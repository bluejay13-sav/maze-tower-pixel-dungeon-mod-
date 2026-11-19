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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 2차 특성 24개의 효과를 한곳에 모은 클래스.
//원작 코드에는 짧은 호출 한 줄씩만 넣고(Hero, Char, SwordSkill, Mana, Buff, Burning), 실제 계산은 여기서 한다.
public class BloomEffects {

	private static int lvl(Hero hero, BloomTalent t){
		return BloomTree.level(hero, t);
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
		public boolean burstUsed;
		public boolean executeApplied;
	}

	public static SkillUse skillUse = null;

	//스킬 비용 (절약)
	public static int skillCost(Hero hero, int base){
		int t = lvl(hero, THRIFT);
		if (t >= 0) base = Math.round(base * (1f - T_THRIFT_COST[t]));
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

		//처치 시 마나 반환 (날 세우기 2레벨 50%, 처형 2레벨 100%, 큰 쪽)
		if (u.target != null && !u.target.isAlive()){
			float refund = 0;
			if (lvl(hero, SHARPEN) >= 2) refund = Math.max(refund, T_SHARPEN_REFUND);
			if (lvl(hero, EXECUTE) >= 2 && u.executeApplied) refund = Math.max(refund, T_EXECUTE_REFUND);
			int amt = Math.round(u.manaSpent * refund);
			if (amt > 0){
				Mana.get(hero).gain(amt);
				TalentEffects.manaRefund(hero, u.target, amt); //푸른 마탄이 주인공에게
			}
		}

		//흐름: 다음 평타 강화
		int f = lvl(hero, FLOW);
		if (f >= 0) Buff.affect(hero, FlowTracker.class).charges = T_FLOW_ATTACKS[f];

		//잔향: 몇 턴 동안 평타 적중 시 마나
		int e = lvl(hero, ECHO);
		if (e >= 0) Buff.prolong(hero, EchoTracker.class, T_ECHO_TURNS[e]);

		//흘리기: 다음에 받는 공격 피해 감소
		if (lvl(hero, PARRY) >= 0) Buff.affect(hero, ParryTracker.class);
	}

	// =====================================================================
	// 명중 (Hero.attackSkill)
	// =====================================================================

	public static float accuracyMulti(Hero hero, Char target){
		if (isSkill()) return 1f;
		float acc = 1f;
		int p = lvl(hero, PRECISION);
		if (p >= 0) acc *= 1f + T_PRECISION_ACC[p];

		FlowTracker flow = hero.buff(FlowTracker.class);
		if (flow != null && lvl(hero, FLOW) >= 1) acc = Float.POSITIVE_INFINITY;
		return acc;
	}

	//스킬 방어 무시 (집중 2레벨)
	public static boolean skillIgnoresDefense(){
		return isSkill() && skillUse != null && skillUse.focusPrimed
				&& lvl(Dungeon.hero, FOCUS) >= 2;
	}

	// =====================================================================
	// 공격 적중 (Hero.attackProc, 방어 적용 후 피해)
	// =====================================================================

	private static boolean inChain = false; //연쇄로 번진 피해 처리 중
	private static boolean inShadow = false; //그림자검 추가 공격 중

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

	//쏘기가 끝났을 때 (Wand.wandUsed): 처치 시 마나 반환, 흐름·잔향·흘리기
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

	//평타·스킬 적중 한 번 처리 (검 스킬·평타는 Hero.attackProc, 지팡이 스킬은 Char.damage에서 온다)
	private static int processHit(Hero hero, Char enemy, int damage, boolean skill){
		if (enemy == null || enemy == hero) return damage;
		float dmg = damage;
		float bonus = 1f;

		//--- 피해 배율 ---

		//약점 노리기 (평타·스킬)
		int w = lvl(hero, WEAK_POINT);
		if (w >= 0 && hasNegativeStatus(enemy)) bonus += T_WEAK_POINT_DMG[w];

		//서리 베기 2레벨: 냉각·빙결 적이 받는 피해
		if (lvl(hero, FROST_SLASH) >= 2 && (enemy.buff(Chill.class) != null || enemy.buff(Frost.class) != null || enemy.buff(Frostbite.class) != null)){
			bonus += T_FROST_SLASH_BONUS;
		}

		if (!skill){
			//연격
			int c = lvl(hero, COMBO);
			if (c >= 0){
				ComboTracker ct = Buff.affect(hero, ComboTracker.class);
				if (ct.target != enemy.id()){
					ct.target = enemy.id();
					ct.count = 0;
				}
				ct.count++;
				if (ct.count < T_COMBO_HITS[c]) TalentEffects.comboStack(enemy, ct.count, T_COMBO_HITS[c]);
				if (ct.count >= T_COMBO_HITS[c]){
					bonus += T_COMBO_BONUS[c];
					ct.count = 0;
					TalentEffects.comboBurst(enemy);
				}
			}
			//넘치는 힘
			int o = lvl(hero, OVERFLOW);
			if (o >= 0 && Mana.get(hero).isFull()) bonus += T_OVERFLOW_DMG[o];
			//흐름
			FlowTracker flow = hero.buff(FlowTracker.class);
			if (flow != null){
				bonus += T_FLOW_DMG;
				flow.charges--;
				if (flow.charges <= 0) flow.detach();
			}
		} else {
			SkillUse u = skillUse;
			//날 세우기
			int s = lvl(hero, SHARPEN);
			if (s >= 0) bonus += T_SHARPEN_DMG[s];
			//집중
			int fo = lvl(hero, FOCUS);
			if (fo >= 0 && u != null && u.focusPrimed) bonus += T_FOCUS_DMG[fo];
			//처형
			int ex = lvl(hero, EXECUTE);
			if (ex >= 0 && enemy.HP <= enemy.HT * T_EXECUTE_HP){
				bonus += T_EXECUTE_DMG[ex];
				if (u != null) u.executeApplied = true;
				TalentEffects.execute(enemy); //붉은 섬광
			}
			//마나 감응 2레벨: 가득 찬 마나로 쓴 스킬
			if (lvl(hero, MANA_ATTUNE) >= 2 && u != null && u.wasFullMana) bonus += T_MANA_ATTUNE_FULL_DMG;
			//원소 폭발: 스킬 한 번에 한 번
			int eb = lvl(hero, ELEMENT_BURST);
			if (eb >= 0 && u != null && !u.burstUsed && removeOneStatus(enemy)){
				u.burstUsed = true;
				bonus += T_ELEMENT_BURST_DMG[eb];
				TalentEffects.elementBurst(hero, enemy); //마탄
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
			}
		}

		dmg *= bonus;
		int result = Math.round(dmg);

		//--- 적중 시 효과 ---
		if (!skill){
			//마나 흡수
			int md = lvl(hero, MANA_DRAIN);
			if (md >= 0) Mana.get(hero).gain(T_MANA_DRAIN[md]);
			//잔향
			int ec = lvl(hero, ECHO);
			if (ec >= 0 && hero.buff(EchoTracker.class) != null) Mana.get(hero).gain(T_ECHO_MANA[ec]);
			//불꽃 칼날
			int fb = lvl(hero, FLAME_BLADE);
			if (fb >= 0 && Random.Float() < T_FLAME_BLADE_CHANCE[fb]){
				ignite(hero, enemy, T_FLAME_BLADE_TURNS);
			}
		} else {
			skillOnHitEffects(hero, enemy, result);
			//연쇄
			int ch = lvl(hero, CHAIN);
			if (ch >= 0 && !inChain) chain(hero, enemy, result, ch);
		}

		//잔류전기 (평타·스킬)
		residualShock(hero, enemy, result);

		return result;
	}

	//스킬 적중 시 효과 (연쇄 2레벨이면 번진 대상에도)
	private static void skillOnHitEffects(Hero hero, Char enemy, int damage){
		int fs = lvl(hero, FROST_SLASH);
		if (fs >= 0) chill(hero, enemy, T_FROST_SLASH_TURNS[fs]);
	}

	private static void chain(Hero hero, Char enemy, int damage, int level){
		int amt = Math.round(damage * T_CHAIN_PCT[level]);
		if (amt <= 0) return;
		ArrayList<Char> candidates = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (c != enemy && c != hero && c.alignment == Char.Alignment.ENEMY && c.isAlive()
					&& Dungeon.level.distance(enemy.pos, c.pos) <= T_CHAIN_RANGE
					&& Dungeon.level.heroFOV[c.pos]){
				candidates.add(c);
			}
		}
		if (candidates.isEmpty()) return;
		Char other = Random.element(candidates);

		inChain = true;
		if (other.sprite != null && enemy.sprite != null){
			TalentEffects.chain(enemy, other); //대상 사이 짧은 빛줄기
		}
		other.damage(amt, hero);
		if (level >= 2 && other.isAlive()){
			skillOnHitEffects(hero, other, amt);
			residualShock(hero, other, amt);
		}
		inChain = false;
	}

	// =====================================================================
	// 평타가 끝난 뒤 (Hero.onAttackComplete)
	// =====================================================================

	//그림자검이 발동해 텀을 두고 이어서 처리하면 true를 돌려준다. 그때는 끝난 뒤 done을 부른다.
	//false면 부른 쪽이 바로 done을 처리한다.
	public static boolean afterNormalAttack(final Hero hero, final Char enemy, boolean hit, final Runnable done){
		if (enemy == null) return false;

		//생명탈취: 평타로 처치
		int ls = lvl(hero, LIFE_STEAL);
		if (ls >= 0 && hit && !enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY){
			int heal = Math.min(T_LIFE_STEAL_HEAL[ls], hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				TalentEffects.lifeSteal(hero);
			}
		}

		//그림자검: 평타 한 번 더
		int sb = lvl(hero, SHADOW_BLADE);
		if (sb >= 0 && hit && !inShadow && enemy.isAlive() && Random.Float() < T_SHADOW_BLADE_CHANCE[sb]){
			//평타가 끝난 뒤 잠깐 텀을 두고 잔상 일격
			if (hero.sprite != null && hero.sprite.parent != null){
				hero.sprite.parent.add(new Delayer(SHADOW_BLADE_DELAY){
					@Override
					protected void onComplete() {
						super.onComplete();
						if (!shadowStrike(hero, enemy, done)) done.run();
					}
				});
				return true;
			}
			return shadowStrike(hero, enemy, done);
		}
		return false;
	}

	//잔상 일격 한 번. 또 그림자검이 발동해 이어지면 true.
	private static boolean shadowStrike(Hero hero, Char enemy, Runnable done){
		if (!hero.isAlive() || !enemy.isAlive()) return false;
		TalentEffects.shadowStrike(hero, enemy);
		inShadow = true;
		boolean hit2 = hero.attack(enemy);
		inShadow = false;
		return afterNormalAttack(hero, enemy, hit2, done);
	}

	// =====================================================================
	// 계속 켜져 있는 연출 (상태창이 매 프레임 호출)
	// =====================================================================

	public static void updateHeroFx(Hero hero){
		if (hero == null) return;
		Mana m = hero.buff(Mana.class);
		//흘리기 대기 중: 보호막
		boolean parry = hero.buff(ParryTracker.class) != null && lvl(hero, PARRY) >= 0;
		//집중(대기·탐색 직후)·흐름(스킬 직후) 준비 완료: 은은한 빛
		boolean ready = (lvl(hero, FOCUS) >= 0 && m != null && m.focusReady)
				|| (lvl(hero, FLOW) >= 0 && hero.buff(FlowTracker.class) != null);
		//넘치는 힘·마나 감응 + 가득 찬 마나: 은은한 푸른 기운
		boolean full = m != null && m.isFull()
				&& (lvl(hero, OVERFLOW) >= 0 || lvl(hero, MANA_ATTUNE) >= 0);
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
			if (p >= 0) dmg *= 1f - T_PARRY_REDUCE[p];
			if (p >= 0) TalentEffects.parryUsed(hero);
			pt.detach();
		}

		//마나 장막
		int mv = lvl(hero, MANA_VEIL);
		if (mv >= 0 && hero.HP <= hero.HT * T_MANA_VEIL_HP && Mana.get(hero).spend(T_MANA_VEIL_COST)){
			dmg *= 1f - T_MANA_VEIL_REDUCE[mv];
			hero.sprite.showStatus(0x4488FF, "-" + T_MANA_VEIL_COST);
			TalentEffects.manaVeil(hero);
		}

		return Math.round(dmg);
	}

	//원소 친화 1~2레벨: 화상·냉각(·정전기) 중 받는 모든 피해 감소
	public static int onHeroDamage(Hero hero, int dmg){
		int a = lvl(hero, ELEMENT_AFFINITY);
		if (a >= 1 && dmg > 0 && hasElementalStatus(hero)){
			return Math.round(dmg * (1f - T_AFFINITY_REDUCE[a]));
		}
		return dmg;
	}

	//원소 친화: 내가 받는 화상·냉각(·정전기) 지속 감소 (Hero.resist, Burning.reignite)
	//Hero.resist용: resist는 지속시간뿐 아니라 피해에도 쓰이므로(Char.damage), 피해를 주는 화상은 뺀다.
	//화상 지속 감소는 Burning.reignite에서 heroIncomingDuration으로 따로 처리한다.
	public static float heroResistFactor(Hero hero, Class<?> effect){
		if (effect == Burning.class) return 1f;
		return heroIncomingDuration(hero, effect);
	}

	public static float heroIncomingDuration(Hero hero, Class<?> effect){
		if (lvl(hero, ELEMENT_AFFINITY) >= 0 && isElementalStatus(effect)){
			return T_AFFINITY_DURATION;
		}
		return 1f;
	}

	// =====================================================================
	// 마나 (Mana)
	// =====================================================================

	public static int bonusMaxMana(Hero hero){
		int a = lvl(hero, MANA_ATTUNE);
		return a >= 0 ? T_MANA_ATTUNE_MAX[a] : 0;
	}

	public static int waitManaMulti(Hero hero){
		int m = lvl(hero, MEDITATION);
		return m >= 0 ? T_MEDITATION_MULTI[m] : 1;
	}

	// =====================================================================
	// 속성 (상태이상 걸기)
	// =====================================================================

	//잔류원소: 내가 건 상태이상 지속 배율
	public static float inflictDuration(Hero hero, float base){
		int l = lvl(hero, LINGERING_ELEMENT);
		return l >= 0 ? base * T_LINGERING_DURATION[l] : base;
	}

	public static void ignite(Hero hero, Char enemy, float turns){
		if (!enemy.isAlive()) return;
		Buff.affect(enemy, Burning.class).reignite(enemy, inflictDuration(hero, turns));
		if (enemy.sprite != null) enemy.sprite.emitter().burst(FlameParticle.FACTORY, 6);
	}

	public static void chill(Hero hero, Char enemy, float turns){
		if (!enemy.isAlive()) return;
		Buff.prolong(enemy, Chill.class, inflictDuration(hero, turns));
		if (enemy.sprite != null) enemy.sprite.emitter().burst(SnowParticle.FACTORY, 6);
	}

	private static void residualShock(Hero hero, Char enemy, int damage){
		int r = lvl(hero, RESIDUAL_SHOCK);
		if (r < 0 || damage <= 0) return;

		float chance = T_SHOCK_CHANCE[r];
		if (SwordSkill.active != null) chance *= SwordSkill.active.elementChanceMulti; //룬 블레이드 무형검
		boolean onWater = Wet.onWater(enemy);
		if (!(r >= 2 && onWater) && Random.Float() >= chance) return;

		int shockDmg = Math.max(1, Math.round(damage * T_SHOCK_DMG_FACTOR));

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

	//전격 피해 한 번 (8단계에서 정전기 상태이상을 여기서 건다)
	public static void shock(Hero hero, Char target, int dmg){
		if (!target.isAlive()) return;
		target.damage(dmg, new ShockDamage());
		if (target.sprite != null) target.sprite.flash();
	}

	//전격 피해의 출처 표시용
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

	//잔류원소 2레벨: 원소 반응 피해 +50%
	public static float reactionDamageMulti(Hero hero){
		if (hero != null && lvl(hero, LINGERING_ELEMENT) >= 2) return 1f + T_LINGERING_REACTION;
		return 1f;
	}

	public static boolean hasElementalStatus(Char ch){
		for (Buff b : ch.buffs()){
			if (isElementalStatus(b.getClass())) return true;
		}
		return false;
	}

	//원소 폭발: 상태이상 하나를 지운다 (속성 상태이상 우선)
	private static boolean removeOneStatus(Char ch){
		for (Buff b : ch.buffs()){
			if (isElementalStatus(b.getClass())){
				b.detach();
				return true;
			}
		}
		for (Buff b : ch.buffs()){
			if (b.type == Buff.buffType.NEGATIVE && b instanceof FlavourBuff){
				b.detach();
				return true;
			}
		}
		return false;
	}

	// =====================================================================
	// 추적용 버프 (화면에 보이지 않음)
	// =====================================================================

	//연격: 같은 적을 때린 횟수
	public static class ComboTracker extends Buff {
		{ type = buffType.NEUTRAL; }
		public int target = -1;
		public int count = 0;

		//상태 아이콘: 쌓인 횟수 / 필요 횟수
		@Override public int icon() { return count > 0 ? BuffIndicator.COMBO : BuffIndicator.NONE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.85f, 0.3f); }
		@Override public String iconTextDisplay() { return Integer.toString(count); }
		@Override public String desc() { return Messages.get(this, "desc", count, comboNeed()); }
		private static int comboNeed(){ int c = Dungeon.hero == null ? -1 : lvl(Dungeon.hero, COMBO); return c >= 0 ? T_COMBO_HITS[c] : T_COMBO_HITS[0]; }

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("target", target);
			bundle.put("count", count);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			target = bundle.getInt("target");
			count = bundle.getInt("count");
		}
	}

	//흐름: 남은 강화 평타 수
	public static class FlowTracker extends Buff {
		{ type = buffType.POSITIVE; }
		public int charges = 1;

		//상태 아이콘: 남은 강화 평타 수
		@Override public int icon() { return BuffIndicator.MOMENTUM; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.5f, 0.8f, 1f); }
		@Override public String iconTextDisplay() { return Integer.toString(charges); }
		@Override public String desc() {
			int f = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, FLOW));
			return Messages.get(this, f >= 1 ? "desc_sure" : "desc", Math.round(T_FLOW_DMG * 100), charges);
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

		//상태 아이콘: 남은 턴
		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.6f, 1f); }
		@Override public float iconFadePercent() { return Math.max(0, (T_ECHO_TURNS[2] - visualcooldown()) / T_ECHO_TURNS[2]); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() {
			int e = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, ECHO));
			return Messages.get(this, "desc", T_ECHO_MANA[e], dispTurns());
		}
	}

	//흘리기: 다음 피격 1회
	public static class ParryTracker extends Buff {
		{ type = buffType.POSITIVE; }

		//상태 아이콘: 다음 피격 1회 피해 감소
		@Override public int icon() { return BuffIndicator.DUEL_GUARD; }
		@Override public String desc() {
			int p = Dungeon.hero == null ? 0 : Math.max(0, lvl(Dungeon.hero, PARRY));
			return Messages.get(this, "desc", Math.round(T_PARRY_REDUCE[p] * 100));
		}
	}

	//무기를 들고 있는지 (평타 판정 보조)
	static boolean hasWeapon(Hero hero){
		KindOfWeapon w = hero.belongings.attackingWeapon();
		return w != null;
	}
}
