/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 2차 특성 효과를 한곳에 모은 클래스.
//원작 코드에는 짧은 호출 한 줄씩만 넣고(Hero, Char, SwordSkill, Mana, Burning), 실제 계산은 여기서 한다.
//
//(2차 특성 개편 A단계) 이름이 같은 특성 13개만 새 수치로 임시 동작한다. 나머지와 무한 연계 방지 규칙은 B·C단계에서 넣는다.
public class BloomEffects {

	//효과 배열 번호: 1레벨 → 0, 2레벨 → 1, 3레벨 → 2. 없거나 0레벨이면 -1 (효과 없음).
	static int lvl(Hero hero, BloomTalent t){
		return BloomTree.level(hero, t) - 1;
	}

	private static boolean isSkill(){
		return SwordSkill.active != null || GladiatorEffects.finisherActive; //검투사 필살기도 스킬
	}

	// =====================================================================
	// 스킬 한 번 사용 동안의 상태 (SwordSkill이 시작/끝에 호출)
	// =====================================================================

	public static class SkillUse {
		public Char target;
		public int manaSpent;
		public boolean wasFullMana;
	}

	public static SkillUse skillUse = null;

	//검·단검·활 스킬 비용 (흐름 각성 +10)
	public static int skillCost(Hero hero, int base){
		return Math.max(0, base + BloomCommon.extraSkillCost(hero, false));
	}

	//지팡이 스킬 비용 (흐름 각성 +15)
	public static int wandSkillCost(Hero hero, int base){
		return Math.max(0, base + BloomCommon.extraSkillCost(hero, true));
	}

	//평타 한 번 시작 (Hero.onAttackComplete): 새 "한 행동"
	public static void beginNormal(Hero hero){
		BloomRules.beginNormal();
		BloomAttack.onActionStart(hero, false);
		BloomCommon.onActionStart(hero, false);
	}

	//마나를 쓰기 직전에 호출
	public static SkillUse beginSkill(Hero hero, Char target, int cost){
		SkillUse u = new SkillUse();
		Mana m = Mana.get(hero);
		u.target = target;
		u.manaSpent = cost;
		u.wasFullMana = m.isFull();
		BloomRules.beginSkill(cost); //스킬 한 번이 새 "한 행동"
		BloomAttack.onActionStart(hero, true); //돌진
		BloomCommon.onActionStart(hero, true); //흐름·거리 벌리기·원소 순환
		BloomSkill.onSkillStart(hero, u.wasFullMana); //집중·과충전
		skillUse = u;
		return u;
	}

	//스킬 타격이 모두 끝난 뒤 호출
	public static void endSkill(Hero hero){
		BloomAttack.flushPostHit(); //밀침·기절 등 타격 뒤 효과
		SkillUse u = skillUse;
		skillUse = null;
		if (u == null) return;

		BloomSkill.endSkill(hero, u); //처형 3레벨 비용 반환
		BloomSkill.onSkillEnd();

		//잔향: 몇 턴 동안 평타 적중 시 마나
		if (lvl(hero, ECHO) >= 0) Buff.prolong(hero, EchoTracker.class, B2_ECHO_TURNS);
		BloomCommon.onSkillEnd(hero); //잔향 각성: 이번 스킬 피해 기억

		//흘리기: 다음에 받는 공격 피해 감소
		if (lvl(hero, PARRY) >= 0) Buff.affect(hero, ParryTracker.class);
	}

	// =====================================================================
	// 명중 (Hero.attackSkill)
	// =====================================================================

	public static float accuracyMulti(Hero hero, Char target){
		float acc = BloomCommon.accuracyMulti(hero); //흐름 3레벨·거리 벌리기 (평타·스킬)
		if (isSkill()) return acc;
		acc *= BloomAttack.accuracyMulti(hero); //확정타
		return acc;
	}

	//주인공 공격의 방어력 무시 (Char.attack). 돌진 각성·마나 흡수 각성 (집중 각성은 스킬 단계)
	public static int armorAfterIgnore(Hero hero, Char enemy, int dr){
		float ignore = BloomAttack.armorIgnore(hero, isSkill());
		if (isSkill()) ignore = Math.max(ignore, BloomSkill.armorIgnore(hero)); //집중 각성
		return ignore > 0 ? Math.round(dr * (1f - ignore)) : dr;
	}

	// =====================================================================
	// 공격 적중 (Hero.attackProc, 방어 적용 후 피해)
	// =====================================================================

	public static int onHeroAttackProc(Hero hero, Char enemy, int damage){
		return processHit(hero, enemy, damage, isSkill());
	}

	// =====================================================================
	// 지팡이 스킬 (마법사): 합성된 마법봉 발사도 스킬로 친다
	// =====================================================================

	//지금 스킬로 쏘고 있는 지팡이의 마법봉 (쏘는 동안만)
	public static Object staffSkillWand = null;

	//쏘는 순간 (WandMana.onZap): 마나를 쓰기 전에 호출
	public static void beginStaffSkill(Hero hero, Object wand, Char target, int cost){
		beginSkill(hero, target, cost);
		staffSkillWand = wand;
		BloomSkill.markRanged(); //지팡이 스킬은 원거리 스킬 (섬광 보법)
	}

	//쏘기가 끝났을 때 (Wand.wandUsed)
	public static void endStaffSkill(Hero hero, Object wand){
		if (wand == null || staffSkillWand != wand) return;
		staffSkillWand = null;
		endSkill(hero);
	}

	//지팡이 마법봉이 준 피해 (Char.damage): 스킬 특성의 피해 배율과 적중 시 효과
	public static int onStaffSkillDamage(Char target, int dmg, Object src){
		if (staffSkillWand == null || src != staffSkillWand || dmg <= 0) return dmg;
		Hero hero = Dungeon.hero;
		if (hero == null || target == hero || target.alignment == Char.Alignment.ALLY) return dmg;
		return processHit(hero, target, dmg, true);
	}

	//적이 피해를 받을 때 (Char.damage): 표식(모든 피해), 불꽃 칼날 각성(주인공이 준 피해)
	public static int incomingDamage(Char target, int dmg, Object src){
		if (dmg <= 0 || target == Dungeon.hero) return dmg;
		float m = BloomAttack.incomingMulti(target, src, BloomRules.isHeroKill(src));
		return m == 1f ? dmg : Math.round(dmg * m);
	}

	//평타·스킬 적중 한 번 처리
	private static int processHit(Hero hero, Char enemy, int damage, boolean skill){
		if (enemy == null || enemy == hero) return damage;
		if (BloomRules.isDerived()) return damage; //규칙 1: 파생 피해는 평타도 스킬도 아니다
		float bonus = 1f;

		//넘치는 힘: 마나가 가득 찬 동안 모든 공격
		int o = lvl(hero, OVERFLOW);
		if (o >= 0 && Mana.get(hero).isFull()) bonus += B2_OVERFLOW_DMG[o];
		//돌진 (평타·스킬)
		bonus += BloomAttack.anyHitBonus(hero, enemy);
		//공용 (흐름·기세·난전·결투·기습·거리 벌리기)
		bonus += BloomCommon.hitBonus(hero, enemy, skill);

		if (!skill){
			bonus += BloomAttack.normalHitBonus(hero, enemy); //확정타·강타
		} else {
			bonus += BloomSkill.hitBonus(hero, enemy); //집중·처형·과충전·표식 폭발·섬광 보법
		}

		int result = Math.round(damage * bonus);

		//--- 적중 시 효과 ---
		if (!skill){
			//기본공격 전용 특성 (표식·불꽃 칼날·물보라·마나 흡수)
			BloomAttack.onNormalHit(hero, enemy, result);
		} else {
			//스킬 전용 특성 (서리 베기·연쇄·과충전 각성·충격·섬광 보법 각성)
			BloomSkill.onSkillHit(hero, enemy, result);
		}

		//공용 적중 효과 (잔향·원소 순환 각성·넘치는 힘 각성)
		BloomCommon.onHit(hero, enemy, result, skill);

		//잔류전기 (평타·스킬)
		residualShock(hero, enemy, result);

		return result;
	}

	// =====================================================================
	// 평타가 끝난 뒤 (Hero.onAttackComplete)
	// =====================================================================

	//추가 평타로 이어서 처리하면 true (그때는 끝난 뒤 done을 부른다). false면 부른 쪽이 바로 done을 처리한다.
	public static boolean afterNormalAttack(final Hero hero, final Char enemy, boolean hit, final Runnable done){
		return BloomAttack.afterNormalAttack(hero, enemy, hit, done);
	}

	// =====================================================================
	// 처치 (BloomRules.onDeath에서, 주인공이 처치했을 때만)
	// =====================================================================

	//byDerived: 파생 피해로 죽었는지. 처치 특성(기세·질풍·결투·기습·물보라·표식 등)은 C단계에서 넣는다.
	public static void onEnemyKilled(Hero hero, Char enemy, boolean byDerived){
		BloomRules.debug("kill " + enemy.name() + (byDerived ? " (derived)" : ""));
		BloomAttack.onKill(hero, enemy, byDerived);
		BloomSkill.onKill(hero, enemy, byDerived);
		BloomCommon.onKill(hero, enemy, byDerived);
	}

	// =====================================================================
	// 계속 켜져 있는 연출 (상태창이 매 프레임 호출)
	// =====================================================================

	private static <T extends BloomBuffs.Sense> void syncSense(Hero hero, BloomTalent t, Class<T> cls){
		T s = hero.buff(cls);
		if (lvl(hero, t) >= 0 && hero.isAlive()){
			if (s == null) s = Buff.affect(hero, cls);
			if (s != null) s.recheck();
		} else if (s != null){
			s.detach();
		}
	}

	public static void updateHeroFx(Hero hero){
		if (hero == null) return;
		//결투·난전 아이콘 (조건이 맞을 때만 보인다)
		syncSense(hero, DUEL, BloomBuffs.DuelSense.class);
		syncSense(hero, MELEE, BloomBuffs.MeleeSense.class);
		Mana m = hero.buff(Mana.class);
		//흘리기 대기 중: 보호막
		boolean parry = hero.buff(ParryTracker.class) != null && lvl(hero, PARRY) >= 0;
		//집중(대기·탐색 직후) 준비 완료: 은은한 빛
		boolean ready = (lvl(hero, FOCUS) >= 0 && m != null && m.focusReady && hero.buff(BloomBuffs.FocusCooldown.class) == null);
		//넘치는 힘 + 가득 찬 마나: 은은한 푸른 기운
		boolean full = m != null && m.isFull() && lvl(hero, OVERFLOW) >= 0;
		TalentEffects.updateHero(hero, parry, ready, full);
	}

	// =====================================================================
	// 받는 피해 (Hero.defenseProc / Hero.damage)
	// =====================================================================

	public static int onHeroDefense(Hero hero, Char enemy, int damage){
		BloomCommon.onHeroHit(hero); //거리 벌리기: 맞으면 풀린다
		if (damage <= 0) return damage;
		float dmg = damage;

		//흘리기
		ParryTracker pt = hero.buff(ParryTracker.class);
		int p = lvl(hero, PARRY);
		if (pt != null){
			if (p >= 0){
				dmg *= 1f - B2_PARRY_REDUCE[p];
				TalentEffects.parryUsed(hero);
				BloomSkill.parryCounter(hero, enemy); //흘리기 각성: 반격 (파생)
			}
			pt.detach();
		}

		//마나 장막
		int mv = lvl(hero, MANA_VEIL);
		if (mv >= 0 && hero.HP <= hero.HT * B2_VEIL_HP && Mana.get(hero).spend(B2_VEIL_COST)){
			dmg *= 1f - B2_VEIL_REDUCE[mv];
			hero.sprite.showStatus(0x4488FF, "-" + B2_VEIL_COST);
			TalentEffects.manaVeil(hero);
			BloomCommon.veilPush(hero); //마나 장막 각성
		}

		return Math.round(dmg);
	}

	//받는 모든 피해 (난전 각성, 결투 3레벨)
	public static int onHeroDamage(Hero hero, int dmg){
		if (dmg <= 0) return dmg;
		float m = BloomCommon.incomingMulti(hero);
		return m == 1f ? dmg : Math.round(dmg * m);
	}

	//내가 받는 상태이상 지속 (예전 원소 친화 자리, 지금은 없음)
	public static float heroResistFactor(Hero hero, Class<?> effect){
		return 1f;
	}

	public static float heroIncomingDuration(Hero hero, Class<?> effect){
		return 1f;
	}

	// =====================================================================
	// 마나 (Mana)
	// =====================================================================

	public static int bonusMaxMana(Hero hero){
		return 0;
	}

	public static int waitManaMulti(Hero hero){
		int m = lvl(hero, MEDITATION);
		return m >= 0 ? B2_MEDITATION_MULTI[m] : 1;
	}

	// =====================================================================
	// 속성 (상태이상 걸기)
	// =====================================================================

	public static void ignite(Hero hero, Char enemy, float turns){
		if (!enemy.isAlive()) return;
		Buff.affect(enemy, Burning.class).reignite(enemy, turns);
		if (enemy.sprite != null) enemy.sprite.emitter().burst(FlameParticle.FACTORY, 6);
	}

	public static void chill(Hero hero, Char enemy, float turns){
		if (!enemy.isAlive()) return;
		Buff.prolong(enemy, Chill.class, turns);
		if (enemy.sprite != null) enemy.sprite.emitter().burst(SnowParticle.FACTORY, 6);
	}

	private static void residualShock(Hero hero, Char enemy, int damage){
		int r = lvl(hero, RESIDUAL_SHOCK);
		if (r < 0 || damage <= 0) return;

		float chance = B2_SHOCK_CHANCE[r];
		if (SwordSkill.active != null) chance *= SwordSkill.active.elementChanceMulti; //룬 블레이드 무형검
		boolean onWater = Wet.onWater(enemy);
		if (!(r >= 2 && onWater) && Random.Float() >= chance) return;
		if (!BloomRules.triggerOnHit(RESIDUAL_SHOCK)) return; //규칙 2

		int shockDmg = Math.max(1, Math.round(damage * B2_SHOCK_DMG_FACTOR));

		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		if (hero.sprite != null && enemy.sprite != null){
			arcs.add(new Lightning.Arc(hero.sprite.center(), enemy.sprite.center()));
		}
		shock(hero, enemy, shockDmg);

		//인접한 1명에게 전이 (적뿐 아니라 옆에 있는 주인공·아군에게도 튈 수 있다)
		ArrayList<Char> near = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (c != enemy && c.isAlive()
					&& Dungeon.level.adjacent(enemy.pos, c.pos)
					&& !(c == hero && BloomTree.awakened(hero, RESIDUAL_SHOCK))){ //각성: 나에게는 튀지 않는다
				near.add(c);
			}
		}
		if (!near.isEmpty()){
			Char other = Random.element(near);
			if (other.sprite != null && enemy.sprite != null){
				arcs.add(new Lightning.Arc(enemy.sprite.center(), other.sprite.center()));
			}
			shock(hero, other, shockDmg);
		}

		if (!arcs.isEmpty() && hero.sprite != null && hero.sprite.parent != null){
			hero.sprite.parent.addToFront(new Lightning(arcs, null));
		}
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
	}

	//전격 피해 한 번. 특성이 만든 피해라 파생 피해로 처리한다 (적중 시 특성을 부르지 않음, 처치는 인정)
	public static void shock(Hero hero, final Char target, final int dmg){
		if (!target.isAlive()) return;
		BloomRules.derived(new Runnable(){ @Override public void run(){ target.damage(dmg, new ShockDamage()); } });
		//원작 번개(전격 마법 부여·번개 마법봉)처럼: 번쩍 + 스파크
		if (target.sprite != null){
			target.sprite.flash();
			target.sprite.centerEmitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle.FACTORY, 3);
		}
	}

	//전격 피해의 출처. 주인공이 튄 번개에 맞아 죽으면 사망 기록을 남긴다.
	public static class ShockDamage implements Hero.Doom {
		@Override
		public void onDeath() {
			Dungeon.fail(this);
			GLog.n(Messages.get(BloomEffects.class, "shock_death"));
		}
	}

	// =====================================================================
	// 상태이상 판정
	// =====================================================================

	public static boolean hasNegativeStatus(Char ch){
		for (Buff b : ch.buffs()){
			if (b.type == Buff.buffType.NEGATIVE) return true;
		}
		return false;
	}

	public static boolean isElementalStatus(Class<?> c){
		return c == Burning.class || c == Chill.class || c == Frost.class || c == StaticCharge.class;
	}

	//원소 반응 피해 배율 (원소 증폭)
	public static float reactionDamageMulti(Hero hero){
		return hero == null ? 1f : BloomCommon.reactionDamageMulti(hero);
	}

	//원소 반응이 일어났을 때 (ElementalReactions.announce): 원소 순환, 원소 증폭 각성
	public static void onReaction(Char ch){
		Hero hero = Dungeon.hero;
		if (hero == null || ch == null || ch == hero) return;
		BloomCommon.onReaction(hero, ch);
	}

	public static boolean hasElementalStatus(Char ch){
		for (Buff b : ch.buffs()){
			if (isElementalStatus(b.getClass())) return true;
		}
		return false;
	}

	// =====================================================================
	// 추적용 버프
	// =====================================================================

	//흐름: 마지막 공격 종류(1 평타, 2 스킬)와 번갈아 쓴 횟수(각성 중첩)
	public static class FlowTracker extends Buff {
		{ type = buffType.POSITIVE; }
		public int lastKind = 0;
		public int stacks = 0;

		//다음 공격을 다른 종류로 하면 붙는 피해
		private int nextPct(){
			Hero h = Dungeon.hero;
			int f = h == null ? -1 : lvl(h, FLOW);
			if (f < 0) return 0;
			int next = BloomTree.awakened(h, FLOW) ? Math.min(B2_FLOW_AWAKE_STACKS, stacks + 1) : 1;
			return Math.round(B2_FLOW_DMG[f] * next * 100);
		}

		@Override public int icon() { return lastKind == 0 ? BuffIndicator.NONE : BuffIndicator.B2_FLOW; }
		@Override public String iconTextDisplay() { return stacks > 0 ? Integer.toString(stacks) : ""; }
		@Override public String desc() {
			return Messages.get(this, lastKind == 2 ? "desc_skill" : "desc_normal", nextPct(), stacks);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("last_kind", lastKind);
			bundle.put("stacks", stacks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			lastKind = bundle.getInt("last_kind");
			stacks = bundle.getInt("stacks");
		}
	}

	//잔향: 남은 턴. skillDamage = 방금 쓴 스킬의 첫 타격 피해 (각성 잔상, 0이면 공격 스킬이 아님)
	public static class EchoTracker extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		public int skillDamage = 0;

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("skill_damage", skillDamage);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			skillDamage = bundle.getInt("skill_damage");
		}

		@Override public int icon() { return BuffIndicator.B2_ECHO; }
		@Override public float iconFadePercent() { return Math.max(0, (B2_ECHO_TURNS - visualcooldown()) / B2_ECHO_TURNS); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() {
			int e = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, ECHO));
			return Messages.get(this, "desc", B2_ECHO_MANA[e], dispTurns());
		}
	}

	//흘리기: 다음 피격 1회
	public static class ParryTracker extends Buff {
		{ type = buffType.POSITIVE; }

		@Override public int icon() { return BuffIndicator.B2_PARRY; }
		@Override public String desc() {
			int p = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, PARRY));
			return Messages.get(this, "desc", Math.round(B2_PARRY_REDUCE[p] * 100));
		}
	}
}
