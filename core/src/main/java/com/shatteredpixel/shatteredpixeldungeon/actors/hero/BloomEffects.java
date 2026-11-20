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
		public boolean focusPrimed;
		public boolean executeApplied;
	}

	public static SkillUse skillUse = null;

	//스킬 비용 (흐름 각성은 C·D단계)
	public static int skillCost(Hero hero, int base){
		return Math.max(0, base);
	}

	//마나를 쓰기 직전에 호출
	public static SkillUse beginSkill(Hero hero, Char target, int cost){
		SkillUse u = new SkillUse();
		Mana m = Mana.get(hero);
		u.target = target;
		u.manaSpent = cost;
		u.wasFullMana = m.isFull();
		u.focusPrimed = m.focusReady;
		m.focusReady = false;
		skillUse = u;
		return u;
	}

	//스킬 타격이 모두 끝난 뒤 호출
	public static void endSkill(Hero hero){
		SkillUse u = skillUse;
		skillUse = null;
		if (u == null) return;

		//처형 3레벨: 처형으로 처치하면 비용 50% 반환
		if (u.target != null && !u.target.isAlive() && u.executeApplied && lvl(hero, EXECUTE) >= 2){
			int amt = Math.round(u.manaSpent * B2_EXECUTE_REFUND);
			if (amt > 0){
				Mana.get(hero).gain(amt);
				TalentEffects.manaRefund(hero, u.target, amt); //푸른 마탄이 주인공에게
			}
		}

		//흐름 (임시: 스킬 뒤 다음 평타 강화. 번갈아 쓰기는 C단계)
		if (lvl(hero, FLOW) >= 0) Buff.affect(hero, FlowTracker.class).charges = 1;

		//잔향: 몇 턴 동안 평타 적중 시 마나
		if (lvl(hero, ECHO) >= 0) Buff.prolong(hero, EchoTracker.class, B2_ECHO_TURNS);

		//흘리기: 다음에 받는 공격 피해 감소
		if (lvl(hero, PARRY) >= 0) Buff.affect(hero, ParryTracker.class);
	}

	// =====================================================================
	// 명중 (Hero.attackSkill)
	// =====================================================================

	public static float accuracyMulti(Hero hero, Char target){
		if (isSkill()) return 1f;
		float acc = 1f;
		FlowTracker flow = hero.buff(FlowTracker.class);
		if (flow != null && lvl(hero, FLOW) >= 2) acc *= 1f + B2_FLOW_ACC;
		return acc;
	}

	//스킬 방어 무시 (집중 각성은 C·D단계)
	public static boolean skillIgnoresDefense(){
		return false;
	}

	// =====================================================================
	// 공격 적중 (Hero.attackProc, 방어 적용 후 피해)
	// =====================================================================

	private static boolean inChain = false; //연쇄로 번진 피해 처리 중

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

	//평타·스킬 적중 한 번 처리
	private static int processHit(Hero hero, Char enemy, int damage, boolean skill){
		if (enemy == null || enemy == hero) return damage;
		float bonus = 1f;

		//넘치는 힘: 마나가 가득 찬 동안 모든 공격
		int o = lvl(hero, OVERFLOW);
		if (o >= 0 && Mana.get(hero).isFull()) bonus += B2_OVERFLOW_DMG[o];

		if (!skill){
			//흐름 (임시)
			FlowTracker flow = hero.buff(FlowTracker.class);
			int f = lvl(hero, FLOW);
			if (flow != null){
				if (f >= 0) bonus += B2_FLOW_DMG[f];
				flow.detach();
			}
		} else {
			SkillUse u = skillUse;
			//집중
			int fo = lvl(hero, FOCUS);
			if (fo >= 0 && u != null && u.focusPrimed) bonus += B2_FOCUS_DMG[fo];
			//처형
			int ex = lvl(hero, EXECUTE);
			if (ex >= 0 && enemy.HP <= enemy.HT * B2_EXECUTE_HP){
				bonus += B2_EXECUTE_DMG[ex];
				if (u != null) u.executeApplied = true;
				TalentEffects.execute(enemy); //붉은 섬광
			}
		}

		int result = Math.round(damage * bonus);

		//--- 적중 시 효과 ---
		if (!skill){
			//마나 흡수
			int md = lvl(hero, MANA_DRAIN);
			if (md >= 0) Mana.get(hero).gain(B2_MANA_DRAIN[md]);
			//잔향
			int ec = lvl(hero, ECHO);
			if (ec >= 0 && hero.buff(EchoTracker.class) != null) Mana.get(hero).gain(B2_ECHO_MANA[ec]);
			//불꽃 칼날
			int fb = lvl(hero, FLAME_BLADE);
			if (fb >= 0 && Random.Float() < B2_FLAME_CHANCE[fb]){
				ignite(hero, enemy, B2_FLAME_TURNS);
			}
		} else {
			//서리 베기
			int fs = lvl(hero, FROST_SLASH);
			if (fs >= 0) chill(hero, enemy, B2_FROST_TURNS[fs]);
			//연쇄
			int ch = lvl(hero, CHAIN);
			if (ch >= 0 && !inChain) chain(hero, enemy, result, ch);
		}

		//잔류전기 (평타·스킬)
		residualShock(hero, enemy, result);

		return result;
	}

	private static void chain(Hero hero, Char enemy, int damage, int level){
		int amt = Math.round(damage * B2_CHAIN_PCT[level]);
		if (amt <= 0) return;
		ArrayList<Char> candidates = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (c != enemy && c != hero && c.alignment == Char.Alignment.ENEMY && c.isAlive()
					&& Dungeon.level.distance(enemy.pos, c.pos) <= B2_CHAIN_RANGE
					&& Dungeon.level.heroFOV[c.pos]){
				candidates.add(c);
			}
		}
		inChain = true;
		for (int i = 0; i < B2_CHAIN_TARGETS[level] && !candidates.isEmpty(); i++){
			Char other = Random.element(candidates);
			candidates.remove(other);
			if (other.sprite != null && enemy.sprite != null){
				TalentEffects.chain(enemy, other); //대상 사이 짧은 빛줄기
			}
			other.damage(amt, hero);
		}
		inChain = false;
	}

	// =====================================================================
	// 평타가 끝난 뒤 (Hero.onAttackComplete)
	// =====================================================================

	//추가 평타로 이어서 처리하면 true (그때는 끝난 뒤 done을 부른다). false면 부른 쪽이 바로 done을 처리한다.
	public static boolean afterNormalAttack(final Hero hero, final Char enemy, boolean hit, final Runnable done){
		return false;
	}

	// =====================================================================
	// 계속 켜져 있는 연출 (상태창이 매 프레임 호출)
	// =====================================================================

	public static void updateHeroFx(Hero hero){
		if (hero == null) return;
		Mana m = hero.buff(Mana.class);
		//흘리기 대기 중: 보호막
		boolean parry = hero.buff(ParryTracker.class) != null && lvl(hero, PARRY) >= 0;
		//집중(대기·탐색 직후)·흐름 준비 완료: 은은한 빛
		boolean ready = (lvl(hero, FOCUS) >= 0 && m != null && m.focusReady)
				|| (lvl(hero, FLOW) >= 0 && hero.buff(FlowTracker.class) != null);
		//넘치는 힘 + 가득 찬 마나: 은은한 푸른 기운
		boolean full = m != null && m.isFull() && lvl(hero, OVERFLOW) >= 0;
		TalentEffects.updateHero(hero, parry, ready, full);
	}

	// =====================================================================
	// 받는 피해 (Hero.defenseProc / Hero.damage)
	// =====================================================================

	public static int onHeroDefense(Hero hero, Char enemy, int damage){
		if (damage <= 0) return damage;
		float dmg = damage;

		//흘리기
		ParryTracker pt = hero.buff(ParryTracker.class);
		int p = lvl(hero, PARRY);
		if (pt != null){
			if (p >= 0){
				dmg *= 1f - B2_PARRY_REDUCE[p];
				TalentEffects.parryUsed(hero);
			}
			pt.detach();
		}

		//마나 장막
		int mv = lvl(hero, MANA_VEIL);
		if (mv >= 0 && hero.HP <= hero.HT * B2_VEIL_HP && Mana.get(hero).spend(B2_VEIL_COST)){
			dmg *= 1f - B2_VEIL_REDUCE[mv];
			hero.sprite.showStatus(0x4488FF, "-" + B2_VEIL_COST);
			TalentEffects.manaVeil(hero);
		}

		return Math.round(dmg);
	}

	//받는 모든 피해 (난전·결투는 C단계)
	public static int onHeroDamage(Hero hero, int dmg){
		return dmg;
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
					&& Dungeon.level.adjacent(enemy.pos, c.pos)){
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

	//전격 피해 한 번
	public static void shock(Hero hero, Char target, int dmg){
		if (!target.isAlive()) return;
		target.damage(dmg, new ShockDamage());
		if (target.sprite != null) target.sprite.flash();
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

	//원소 반응 피해 배율 (원소 증폭은 C단계)
	public static float reactionDamageMulti(Hero hero){
		return 1f;
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

	//흐름: 다음 공격 강화
	public static class FlowTracker extends Buff {
		{ type = buffType.POSITIVE; }
		public int charges = 1;

		@Override public int icon() { return BuffIndicator.MOMENTUM; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.5f, 0.8f, 1f); }
		@Override public String iconTextDisplay() { return Integer.toString(charges); }
		@Override public String desc() {
			int f = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, FLOW));
			return Messages.get(this, "desc", Math.round(B2_FLOW_DMG[f] * 100), charges);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("charges", charges);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			charges = bundle.getInt("charges");
		}
	}

	//잔향: 남은 턴
	public static class EchoTracker extends FlavourBuff {
		{ type = buffType.POSITIVE; }

		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.6f, 1f); }
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

		@Override public int icon() { return BuffIndicator.DUEL_GUARD; }
		@Override public String desc() {
			int p = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, PARRY));
			return Messages.get(this, "desc", Math.round(B2_PARRY_REDUCE[p] * 100));
		}
	}
}
