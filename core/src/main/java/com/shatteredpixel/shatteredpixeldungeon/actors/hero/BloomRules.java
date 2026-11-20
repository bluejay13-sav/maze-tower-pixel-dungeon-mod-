/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.DeviceCompat;

import java.util.EnumMap;

//Maze Tower: 2차 특성의 무한 연계를 막는 공통 규칙 (TALENT2_SPEC.md "무한 연계를 막는 규칙").
//모든 2차 특성 효과는 여기를 거쳐 발동한다.
//
// "한 행동" = 평타 한 번 또는 스킬 한 번과 그로 인해 벌어지는 일 전부.
//  평타는 Hero.onAttackComplete, 스킬은 BloomEffects.beginSkill(검·단검·활 스킬, 지팡이 스킬, 필살기)에서 새 행동이 시작된다.
//  다음 행동이 시작될 때까지(그 사이에 터진 반응·처치 포함)는 같은 행동으로 본다.
//
// 1. 파생 피해(번짐·충격파·잔상·전이·휩쓸기·반격 등)는 derivedDamage로 준다. 평타도 스킬도 아니라 적중 시 특성을 부르지 않는다.
//    그 피해로 죽은 적은 처치로 친다(isHeroKill).
// 2. 한 행동에 특성마다 한 번(trigger). 여러 번 때리는 스킬의 "스킬 적중 시" 효과는 타격마다, 스킬 한 번에 최대 3번(triggerOnHit, B2_MULTIHIT_SKILL_CAP).
//    횟수가 설명에 적힌 특성(연쇄 대상 수, 잔류전기 전이 등)은 그 안에서 따로 센다.
// 3. 추가 평타(연속 베기)는 추가 평타를 만들지 않는다(canExtraAttack). 연속 베기 각성이면 추가 평타가 막힌 특성 효과 1번을 발동시킬 수 있다.
// 4. 특성으로 얻는 마나는 한 행동에 최대 마나의 25%까지(gainMana). 비용 반환은 그 스킬 비용을 넘지 않는다(refund).
// 5. 옮겨 간 상태이상·표식과 특성이 만든 장판은 다시 퍼지지 않는다(Transferable, canSpread).
//
// (결정) 상태를 보고 붙는 피해 배율(넘치는 힘·난전·결투·표식 1개당 피해 등)은 "발동"이 아니라 조건이라 매 타격에 붙는다.
// 확률·자원·밀침·상태이상·추가 공격처럼 무언가를 "일으키는" 효과만 위 횟수 규칙을 따른다.
public class BloomRules {

	public enum Kind { NONE, NORMAL, SKILL }

	//지금 행동
	private static Kind kind = Kind.NONE;
	private static int actionId = 0;
	private static final EnumMap<BloomTalent, Integer> fired = new EnumMap<>(BloomTalent.class);
	private static int manaGained = 0;
	private static int skillCost = 0;
	private static int refunded = 0;
	private static int derivedDepth = 0;
	private static boolean inExtra = false;
	private static int extraAllowance = 0;

	// ===== 행동 시작 =====

	//평타 한 번 (Hero.onAttackComplete)
	public static void beginNormal(){
		begin(Kind.NORMAL, 0);
	}

	//스킬 한 번 (BloomEffects.beginSkill). cost = 실제로 쓴 마나 (비용 반환 상한)
	public static void beginSkill(int cost){
		begin(Kind.SKILL, cost);
	}

	private static void begin(Kind k, int cost){
		kind = k;
		actionId++;
		fired.clear();
		manaGained = 0;
		skillCost = Math.max(0, cost);
		refunded = 0;
		derivedDepth = 0;
		inExtra = false;
		extraAllowance = 0;
	}

	public static Kind kind(){
		return kind;
	}

	public static int actionId(){
		return actionId;
	}

	// ===== 규칙 2: 한 행동에 특성마다 한 번 =====

	//한 행동에 한 번만 일어나는 효과. 처음이면 true.
	public static boolean trigger(BloomTalent t){
		return tryFire(t, 1);
	}

	//"적중 시" 효과. 스킬이면 타격마다 스킬 한 번에 최대 B2_MULTIHIT_SKILL_CAP번, 평타는 한 번.
	public static boolean triggerOnHit(BloomTalent t){
		return tryFire(t, kind == Kind.SKILL ? MazeTower.B2_MULTIHIT_SKILL_CAP : 1);
	}

	//설명에 적힌 횟수만큼 (예: 연쇄 대상 수는 이것 대신 특성 안에서 센다)
	public static boolean triggerUpTo(BloomTalent t, int times){
		return tryFire(t, times);
	}

	private static boolean tryFire(BloomTalent t, int cap){
		if (derivedDepth > 0) return false; //규칙 1: 파생 피해는 특성을 부르지 않는다
		int n = fired.containsKey(t) ? fired.get(t) : 0;
		if (n >= cap){
			//규칙 3 예외: 연속 베기 각성의 추가 평타는 막힌 특성 효과를 한 번 발동시킬 수 있다
			if (inExtra && extraAllowance > 0 && t != BloomTalent.DOUBLE_SLASH){
				extraAllowance--;
				debug("extra-attack allowance used by " + t);
				return true;
			}
			debug("blocked repeat " + t + " (" + n + "/" + cap + ")");
			return false;
		}
		fired.put(t, n + 1);
		return true;
	}

	//이번 행동에 이미 발동했는지 (연출 등 확인용)
	public static boolean fired(BloomTalent t){
		return fired.containsKey(t);
	}

	// ===== 규칙 1: 파생 피해 =====

	//파생 피해의 출처. 이 피해로 죽은 적은 처치로 친다. 주인공이 맞아 죽으면 사망 기록을 남긴다.
	public static class Derived implements Hero.Doom {
		public final BloomTalent talent;
		public Derived(BloomTalent talent){ this.talent = talent; }

		@Override
		public void onDeath() {
			Dungeon.fail(this);
			GLog.n(Messages.get(BloomRules.class, "derived_death"));
		}
	}

	public static boolean isDerived(){
		return derivedDepth > 0;
	}

	//파생 피해를 준다. 그동안 일어나는 일은 특성을 부르지 않는다.
	public static void derivedDamage(BloomTalent from, Char target, int dmg){
		if (target == null || !target.isAlive() || dmg <= 0) return;
		derivedDepth++;
		try {
			target.damage(dmg, new Derived(from));
		} finally {
			derivedDepth--;
		}
	}

	//파생 효과(밀침·상태이상 등 피해가 아닌 것)를 실행한다
	public static void derived(Runnable r){
		derivedDepth++;
		try {
			r.run();
		} finally {
			derivedDepth--;
		}
	}

	//주인공이 처치한 것으로 치는 원인 (평타·스킬·지팡이·파생 피해·튄 번개)
	public static boolean isHeroKill(Object src){
		return src == Dungeon.hero
				|| src instanceof Derived
				|| src instanceof BloomEffects.ShockDamage
				|| src instanceof Wand; //마법봉은 주인공만 쏜다
	}

	// ===== 규칙 3: 추가 평타 =====

	public static boolean canExtraAttack(){
		return !inExtra && derivedDepth == 0;
	}

	public static boolean inExtraAttack(){
		return inExtra;
	}

	//추가 평타 한 번을 실행한다. awakened = 연속 베기 각성 (막힌 특성 효과 1번 허용)
	public static boolean extraAttack(boolean awakened, java.util.concurrent.Callable<Boolean> attack){
		if (!canExtraAttack()) return false;
		inExtra = true;
		extraAllowance = awakened ? 1 : 0;
		try {
			return attack.call();
		} catch (Exception e){
			return false;
		} finally {
			inExtra = false;
			extraAllowance = 0;
		}
	}

	// ===== 규칙 4: 마나 =====

	//특성으로 얻는 마나. 한 행동에 최대 마나의 B2_MANA_CAP_PER_ACTION까지. 실제로 얻은 양을 돌려준다.
	public static int gainMana(Hero hero, int amount){
		if (hero == null || amount <= 0) return 0;
		Mana m = Mana.get(hero);
		int cap = Math.round(m.max() * MazeTower.B2_MANA_CAP_PER_ACTION);
		int give = Math.max(0, Math.min(amount, cap - manaGained));
		if (give < amount) debug("mana capped " + amount + " -> " + give + " (this action " + manaGained + "/" + cap + ")");
		if (give <= 0) return 0;
		manaGained += give;
		m.gain(give);
		return give;
	}

	//스킬 비용 반환. 이번 스킬 비용을 넘지 않는다. 마나 상한(규칙 4 앞부분)과는 따로 센다.
	public static int refund(Hero hero, int amount){
		if (hero == null || amount <= 0) return 0;
		int give = Math.max(0, Math.min(amount, skillCost - refunded));
		if (give < amount) debug("refund capped " + amount + " -> " + give + " (cost " + skillCost + ")");
		if (give <= 0) return 0;
		refunded += give;
		Mana.get(hero).gain(give);
		return give;
	}

	// ===== 규칙 5: 옮겨 간 것은 다시 퍼지지 않는다 =====

	//표식·상태이상·장판 중 다른 곳에서 옮겨 오거나 특성이 만든 것
	public interface Transferable {
		boolean transferred();
	}

	public static boolean canSpread(Object o){
		return !(o instanceof Transferable) || !((Transferable) o).transferred();
	}

	// ===== 처치 (Char.die) =====

	//적이 죽을 때. 주인공이 처치한 것이면 처치 특성을 부른다 (기세·질풍·결투·기습·물보라·표식 등, C단계)
	public static void onDeath(Char ch, Object src){
		Hero hero = Dungeon.hero;
		if (hero == null || ch == hero || ch.alignment != Char.Alignment.ENEMY) return;
		if (!isHeroKill(src)) return;
		BloomEffects.onEnemyKilled(hero, ch, src instanceof Derived);
	}

	// ===== 확인용 로그 (디버그 빌드에서만, 터미널) =====

	static void debug(String s){
		if (DeviceCompat.isDebug()) System.out.println("[Bloom] #" + actionId + " " + kind + ": " + s);
	}
}
