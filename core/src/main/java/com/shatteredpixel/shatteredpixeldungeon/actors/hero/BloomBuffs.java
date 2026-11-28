/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;

//Maze Tower: 2차 특성이 쓰는 상태(버프). (2026-11-24) 예전 특성을 지워서 지금은 섬광 보법 쿨타임만 있다.
public class BloomBuffs {

	//섬광 보법 쿨타임
	public static class FlashCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.HASTE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (C17_FLASH_COOLDOWN - visualcooldown()) / C17_FLASH_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(C1) 받아치기 준비: 적의 공격으로 피가 줄지 않았다. 직후의 평타가 강해진다.
	//평타를 하면 쓰이고, 다른 행동(이동·대기·아이템·스킬 등 턴을 쓰는 것)을 하면 사라진다 (Hero.spend → BloomEffects.onHeroSpend).
	public static class RiposteReady extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.DUEL_GUARD; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); } //새잎빛
		@Override public String desc() {
			int l = Math.max(0, BloomTree.level((Hero)target, BloomTalent.W_RIPOSTE) - 1);
			String s = Messages.get(this, "desc", Math.round(C1_RIPOSTE_DMG[l] * 100));
			if (BloomTree.awakened((Hero)target, BloomTalent.W_RIPOSTE)){
				s += "\n\n" + (target.buff(RiposteFreeCooldown.class) == null
						? Messages.get(this, "desc_free")
						: Messages.get(this, "desc_free_wait"));
			}
			return s;
		}
	}

	//(C5) 메아리: 공격형 스킬을 쓴 다음 턴에 푸른 잔상이 같은 적에게 그 스킬을 한 번 더 쓴다.
	//각성이면 한 턴 더 남아(following) 그 턴의 평타 한 번을 따라 친다 (BloomWarrior.afterNormalAttack).
	public static class Echo extends Buff {
		{ type = buffType.POSITIVE; }

		private int targetId = -1;
		private String key = "slash";
		private int hits = 1;
		private float dmg = 1f, acc = 1f;
		private boolean ignoreDefense = false, ranged = false, pierce = false;
		private int reach = 1;
		private boolean following = false;

		public void set(com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill s, com.shatteredpixel.shatteredpixeldungeon.actors.Char t){
			targetId = t.id();
			key = s.nameKey; hits = s.hits; dmg = s.dmgPerHit; acc = s.accMulti;
			ignoreDefense = s.ignoreDefense; ranged = s.ranged; pierce = s.pierce; reach = s.reach;
			following = false;
			spend(TICK); //다음 턴 (주인공이 한 번 행동한 뒤)
		}

		public boolean following(){
			return following;
		}

		@Override
		public boolean act() {
			Hero hero = (Hero)target;
			if (following){ //각성: 따라 칠 턴이 지났다
				detach();
				return true;
			}
			com.shatteredpixel.shatteredpixeldungeon.actors.Actor a = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findById(targetId);
			boolean done = false;
			if (a instanceof com.shatteredpixel.shatteredpixeldungeon.actors.Char && hero.isAlive()){
				done = BloomWarrior.echoReplay(hero, (com.shatteredpixel.shatteredpixeldungeon.actors.Char)a,
						com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill.echoOf(key, hits, dmg, acc, ignoreDefense, ranged, reach, pierce));
			}
			if (!done) BloomRules.debug("echo faded (out of reach)");
			if (BloomTree.awakened(hero, BloomTalent.W_ECHO) && BloomTree.level(hero, BloomTalent.W_ECHO) >= 1){
				following = true;
				spend(TICK);
			} else {
				detach();
			}
			return true;
		}

		@Override public int icon() { return BuffIndicator.DUEL_SPIN; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.45f, 0.70f, 1f); } //푸른 잔상
		@Override public String desc() {
			return Messages.get(this, following ? "desc_follow" : "desc");
		}

		private static final String TARGET = "target", KEY = "key", HITS = "hits", DMG = "dmg", ACC = "acc",
				IGNORE = "ignore", RANGED = "ranged", PIERCE = "pierce", REACH = "reach", FOLLOW = "following";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) {
			super.storeInBundle(b);
			b.put(TARGET, targetId); b.put(KEY, key); b.put(HITS, hits); b.put(DMG, dmg); b.put(ACC, acc);
			b.put(IGNORE, ignoreDefense); b.put(RANGED, ranged); b.put(PIERCE, pierce); b.put(REACH, reach); b.put(FOLLOW, following);
		}
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) {
			super.restoreFromBundle(b);
			targetId = b.getInt(TARGET); key = b.getString(KEY); hits = b.getInt(HITS); dmg = b.getFloat(DMG); acc = b.getFloat(ACC);
			ignoreDefense = b.getBoolean(IGNORE); ranged = b.getBoolean(RANGED); pierce = b.getBoolean(PIERCE); reach = b.getInt(REACH);
			following = b.getBoolean(FOLLOW);
		}
	}

	//(D3) 벌어진 상처: 적에게 쌓이는 상처 (D3_WOUND_MAX까지)
	public static class OpenWound extends Buff {
		{ type = buffType.NEGATIVE; announced = false; }
		private int stacks = 0;
		public boolean transferred = false; //각성으로 옮겨 붙은 상처 (터져도 다시 옮겨 가지 않는다, 규칙 5)
		public boolean bursting = false;    //이번 타격에 터지는 중 (저장하지 않음)
		public void add(int n){ stacks = Math.min(D3_WOUND_MAX, stacks + n); }
		public boolean full(){ return stacks >= D3_WOUND_MAX; }
		public int stacks(){ return stacks; }
		@Override public int icon() { return BuffIndicator.BLEEDING; }
		@Override public void tintIcon(Image icon) { if (full()) icon.hardlight(1f, 0.3f, 0.3f); }
		@Override public String iconTextDisplay() { return Integer.toString(stacks); }
		@Override public String desc() { return Messages.get(this, "desc", stacks, D3_WOUND_MAX); }
		private static final String STACKS = "stacks", TRANSFERRED = "transferred";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) { super.storeInBundle(b); b.put(STACKS, stacks); b.put(TRANSFERRED, transferred); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) { super.restoreFromBundle(b); stacks = b.getInt(STACKS); transferred = b.getBoolean(TRANSFERRED); }
	}

	//(D4) 치고 빠지기: 바로 앞 행동에 걸었다 (다음 공격이 강해진다)
	public static class HitAndRun extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.MOMENTUM; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); }
		@Override public String desc() {
			int l = Math.max(0, BloomTree.level((Hero)target, BloomTalent.H_HIT_AND_RUN) - 1);
			return Messages.get(this, "desc", Math.round(D4_HIT_AND_RUN_DMG[l] * 100));
		}
	}

	//(D4) 치고 빠지기 각성: 공격한 직후의 이동 한 번은 턴을 쓰지 않는다
	public static class FreeStep extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.HASTE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); }
		@Override public String desc() { return Messages.get(this, "desc"); }
	}

	//(D6) 값진 일격 각성: 한 번에 쓰러뜨렸다. 바로 다음 공격은 턴을 쓰지 않는다
	public static class FreeAttack extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.UPGRADE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); }
		@Override public String desc() { return Messages.get(this, "desc"); }
	}

	//턴을 쓰지 않는 특성 효과(치고 빠지기·값진 일격 각성)의 간격: 한 번에 하나만
	public static class FreeActionCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.HASTE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public String iconTextDisplay() { return Integer.toString((int)Math.ceil(visualcooldown())); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(D8) 새잎 자국 쿨타임
	public static class SproutCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.HERB_HEALING; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (D8_COOLDOWN - visualcooldown()) / D8_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)Math.ceil(visualcooldown())); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(D9) 휘감는 덩굴 쿨타임
	public static class VineCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.ROOTS; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (D9_COOLDOWN - visualcooldown()) / D9_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)Math.ceil(visualcooldown())); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(D13) 연막 깃 쿨타임
	public static class SmokeCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.INVISIBLE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (D13_COOLDOWN - visualcooldown()) / D13_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)Math.ceil(visualcooldown())); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(D21) 활대 치기: 방금 활로 밀쳐냈다. 바로 다음 사격(원거리 평타 한 번 또는 활 스킬 한 번)이 강해진다.
	//다른 행동을 하면 사라진다 (밀쳐낸 그 평타의 턴 다음, 한 번 더 턴을 쓰면 끝).
	public static class BowBashReady extends Buff {
		{ type = buffType.POSITIVE; }
		public boolean used = false; //이번 사격에 썼다 (턴을 쓰면 사라짐)
		public int spends = 0;       //밀쳐낸 뒤 쓴 턴 수
		@Override public int icon() { return BuffIndicator.WEAPON; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); }
		@Override public String desc() {
			int l = Math.max(0, BloomTree.level((Hero)target, BloomTalent.H_BOW_BASH) - 1); //레벨 1~3 → 칸 0~2
			return Messages.get(this, "desc", Math.round(D21_SHOT_DMG[l] * 100));
		}
		private static final String USED = "used", SPENDS = "spends";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) { super.storeInBundle(b); b.put(USED, used); b.put(SPENDS, spends); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) { super.restoreFromBundle(b); used = b.getBoolean(USED); spends = b.getInt(SPENDS); }
	}

	//(D18) 박힌 화살: 활 스킬의 화살이 박혀 있다 (근접 평타로 치면 터진다)
	public static class EmbeddedArrow extends Buff {
		{ type = buffType.NEGATIVE; announced = false; }
		public int dmg = 1;      //박힐 때의 타격 피해 (터지는 피해의 바탕)
		public float angle = 0;  //꽂힌 그림의 각도
		private com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.EmbeddedArrowFx arrowFx;
		@Override public int icon() { return BuffIndicator.BLEEDING; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.62f, 1f, 0.5f); }
		@Override public String desc() { return Messages.get(this, "desc"); }
		@Override
		public void fx(boolean on) {
			if (on && arrowFx == null) arrowFx = com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.embeddedArrow(target, angle);
			else if (!on && arrowFx != null){ arrowFx.killAndErase(); arrowFx = null; }
		}
		private static final String DMG = "dmg", ANGLE = "angle";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) { super.storeInBundle(b); b.put(DMG, dmg); b.put(ANGLE, angle); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) { super.restoreFromBundle(b); dmg = b.getInt(DMG); angle = b.getFloat(ANGLE); }
	}

	//(D16) 정령의 춤 각성: 다섯 걸음을 이어 걸었다. 다음 공격은 반드시 맞는다 (공격하면 사라짐)
	public static class SpiritStride extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.MOMENTUM; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.45f, 0.75f, 1f); }
	}

	//(D10) 풀숲의 몸 각성: 주인공을 놓쳐 잠깐 다시 알아채지 못한다 (보이지 않는 표시, Mob.Wandering)
	public static class LostTrack extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
	}

	//(D1) 첫 일격 각성: 소리 없는 일격 - 잠든 채로 남는다 (보이지 않는 표시)
	public static class Hushed extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
	}

	//(C20) 받아넘기기 각성: 쳐내진 적의 자세가 무너져 공격이 느려진다 (Mob.attackDelay)
	public static class Staggered extends FlavourBuff {
		{ type = buffType.NEGATIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.DAZE; }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(C21) 발도: 검을 겨누고 있다 (대기하면 붙고, 다음에 턴을 쓰면 풀린다)
	public static class Quickdraw extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.WEAPON; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.6f, 0.8f, 1f); }
		@Override public String desc() {
			Hero hero = (Hero)target;
			int l = Math.max(0, BloomTree.level(hero, BloomTalent.W_QUICKDRAW) - 1);
			String s = Messages.get(this, "desc", Math.round(C21_QUICKDRAW_DMG[l] * 100));
			if (BloomTree.awakened(hero, BloomTalent.W_QUICKDRAW)) s += "\n\n" + Messages.get(this, "desc_awake");
			return s;
		}
	}

	//(C15) 가벼운 발: 해로운 바닥에 들어선 첫 턴. 그 바닥들과 그 바닥이 거는 상태에 면역
	public static class LightFeet extends FlavourBuff {
		{
			type = buffType.POSITIVE;
			for (Class<?> c : BloomWarrior.HARMFUL_FLOORS) immunities.add(c);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge.class);
		}
		@Override public int icon() { return BuffIndicator.LEVITATION; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(C15) 가벼운 발 각성: 함정을 한 번 무시한 뒤의 쿨타임
	public static class LightFeetTrapCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.LEVITATION; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (C15_TRAP_COOLDOWN - visualcooldown()) / C15_TRAP_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(C12) 버티는 숨 쿨타임
	public static class HoldBreathCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.HEART; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (C12_HOLD_COOLDOWN - visualcooldown()) / C12_HOLD_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(C12) 버티는 숨 각성: 버틴 뒤 잠깐 어떤 피해도 받지 않는다
	public static class HoldBreathGuard extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.HEART; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.55f, 0.95f, 0.45f); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//(C13) 넘치는 잔 방어막: 넘친 마나로 찬다. 저절로 줄지 않는다
	public static class CupShield extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff {
		{ type = buffType.POSITIVE; }
		private float partial = 0;

		public void gain(float amount, int cap){
			if (shielding() >= cap){ partial = 0; return; }
			partial += amount;
			int whole = (int)partial;
			if (whole > 0){
				partial -= whole;
				incShield(Math.min(whole, cap - shielding()));
			}
		}

		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.65f, 1f); } //푸른빛 (마나)
		@Override public String iconTextDisplay() { return Integer.toString(shielding()); }
		@Override public String desc() {
			Hero hero = (Hero)target;
			int l = Math.max(0, BloomTree.level(hero, BloomTalent.W_OVERFLOW_CUP) - 1);
			return Messages.get(this, "desc", shielding(), BloomWarrior.cupCap(hero, l));
		}
		private static final String PARTIAL = "partial";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) { super.storeInBundle(b); b.put(PARTIAL, partial); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) { super.restoreFromBundle(b); partial = b.getFloat(PARTIAL); }
	}

	//(C13) 넘치는 잔 각성: 다음 스킬에 쌓인 피해 (넘친 마나의 양)
	public static class CupCharge extends Buff {
		{ type = buffType.POSITIVE; }
		private int mana = 0;
		public void add(int amount){ mana = Math.min(C13_AWAKE_MAX_MANA, mana + amount); BuffIndicator.refreshHero(); }
		public float bonus(){ return mana * C13_AWAKE_PER_MANA; }
		@Override public int icon() { return BuffIndicator.UPGRADE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.65f, 1f); }
		@Override public String iconTextDisplay() { return Math.round(bonus() * 100) + "%"; }
		@Override public String desc() { return Messages.get(this, "desc", Math.round(bonus() * 100), Math.round(C13_AWAKE_MAX_MANA * C13_AWAKE_PER_MANA * 100)); }
		private static final String MANA = "mana";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) { super.storeInBundle(b); b.put(MANA, mana); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) { super.restoreFromBundle(b); mana = b.getInt(MANA); }
	}

	//(C10) 마지막 겹: 방어막이 깨진 뒤 잠깐 받는 피해가 준다
	public static class LastLayer extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.9f, 0.9f, 0.6f); }
		@Override public float iconFadePercent() { return Math.max(0, (C10_LAYER_TURNS - visualcooldown()) / C10_LAYER_TURNS); }
		@Override public String desc() {
			int l = Math.max(0, BloomTree.level((Hero)target, BloomTalent.W_LAST_LAYER) - 1);
			return Messages.get(this, "desc", Math.round(C10_LAYER_REDUCE[l] * 100), dispTurns());
		}
	}

	//(C11) 일대일: 옆 칸의 적이 하나뿐인 동안 보이는 표시 (저장하지 않아도 매 프레임 다시 붙는다)
	public static class OneOnOne extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.DUEL_GUARD; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.9f, 0.75f, 0.4f); }
		@Override public String desc() {
			Hero hero = (Hero)target;
			int l = Math.max(0, BloomTree.level(hero, BloomTalent.W_ONE_ON_ONE) - 1);
			String s = Messages.get(this, "desc", Math.round(C11_DUEL_REDUCE[l] * 100));
			if (BloomTree.awakened(hero, BloomTalent.W_ONE_ON_ONE)) s += "\n\n" + Messages.get(this, "desc_awake");
			return s;
		}
	}

	//(C9) 숨 고르기 방어막: 공격하지도 맞지도 않은 턴마다 찬다. 저절로 줄지 않는다. 다 깎이면 사라진다.
	public static class BreatherShield extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff {
		{ type = buffType.POSITIVE; }

		private float partial = 0; //1보다 작은 몫
		private boolean wardUsed = false; //(각성) 나쁜 상태이상을 이미 한 번 막았다. 방어막을 모두 잃으면(이 상태가 사라지면) 풀린다

		public boolean wardUsed(){ return wardUsed; }
		public void useWard(){ wardUsed = true; BuffIndicator.refreshHero(); }

		//amount만큼(소수 포함) 채운다. cap을 넘지 않는다
		public void gain(float amount, int cap){
			if (shielding() >= cap){ partial = 0; return; }
			partial += amount;
			int whole = (int)partial;
			if (whole > 0){
				partial -= whole;
				incShield(Math.min(whole, cap - shielding()));
			}
		}

		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(Image icon) {
			if (wardUsed) icon.hardlight(0.45f, 0.6f, 0.4f);  //각성 막기를 썼다: 흐린 새잎빛
			else          icon.hardlight(0.55f, 0.95f, 0.45f); //새잎빛
		}
		@Override public String iconTextDisplay() { return Integer.toString(shielding()); }
		@Override public String desc() {
			Hero hero = (Hero)target;
			int l = Math.max(0, BloomTree.level(hero, BloomTalent.W_BREATHER) - 1);
			String s = Messages.get(this, "desc", shielding(), BloomWarrior.breatherCap(hero, l));
			if (BloomTree.awakened(hero, BloomTalent.W_BREATHER)){
				s += "\n\n" + Messages.get(this, "desc_awake");
				s += "\n" + Messages.get(this, wardUsed ? "ward_used" : shielding() >= BloomWarrior.breatherCap(hero, l) ? "ward_ready" : "ward_wait");
			}
			return s;
		}

		private static final String PARTIAL = "partial", WARD = "ward_used";
		@Override public void storeInBundle(com.watabou.utils.Bundle b) { super.storeInBundle(b); b.put(PARTIAL, partial); b.put(WARD, wardUsed); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle b) { super.restoreFromBundle(b); partial = b.getFloat(PARTIAL); wardUsed = b.getBoolean(WARD); }
	}

	//(C3) 벼림: 공격하지 않고 보낸 턴 (C3_HONE_MAX_TURNS까지). 다음 공격(평타·스킬)이 시작될 때 쓰인다.
	public static class Hone extends Buff {
		{ type = buffType.POSITIVE; }

		private float turns = 0;

		public void add(float time){
			turns = Math.min(C3_HONE_MAX_TURNS, turns + time);
		}

		//다 채운 턴 수
		public int turns(){
			return (int)Math.floor(turns + 0.001f);
		}

		public boolean full(){
			return turns() >= C3_HONE_MAX_TURNS;
		}

		@Override public int icon() { return BuffIndicator.WEAPON; }
		@Override public void tintIcon(Image icon) {
			if (full()) icon.hardlight(0.55f, 0.95f, 0.45f); //가득: 새잎빛
			else        icon.hardlight(0.75f, 0.75f, 0.75f);
		}
		@Override public float iconFadePercent() { return Math.max(0, 1f - turns / C3_HONE_MAX_TURNS); }
		@Override public String iconTextDisplay() { return Integer.toString(turns()); }
		@Override public String desc() {
			Hero hero = (Hero)target;
			int l = Math.max(0, BloomTree.level(hero, BloomTalent.W_HONE) - 1);
			String s = Messages.get(this, "desc", turns(), C3_HONE_MAX_TURNS, Math.round(turns() * C3_HONE_PER_TURN[l] * 100));
			if (BloomTree.awakened(hero, BloomTalent.W_HONE)) s += "\n\n" + Messages.get(this, "desc_awake");
			return s;
		}

		private static final String TURNS = "turns";
		@Override public void storeInBundle(com.watabou.utils.Bundle bundle) { super.storeInBundle(bundle); bundle.put(TURNS, turns); }
		@Override public void restoreFromBundle(com.watabou.utils.Bundle bundle) { super.restoreFromBundle(bundle); turns = bundle.getFloat(TURNS); }
	}

	//(C2) 허점 찌르기 각성으로 상태를 옮겨 받은 적 (보이지 않는 표시). 이 적이 쓰러지면 다시 옮겨 가지 않는다 (규칙 5)
	public static class OpeningSpread extends FlavourBuff implements BloomRules.Transferable {
		{ type = buffType.NEUTRAL; }
		@Override public boolean transferred() { return true; }
	}

	//(C1) 받아치기 각성: 턴을 쓰지 않는 받아치기 사이의 간격
	public static class RiposteFreeCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.DUEL_GUARD; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (C1_RIPOSTE_FREE_CD - visualcooldown()) / C1_RIPOSTE_FREE_CD); }
		@Override public String iconTextDisplay() { return Integer.toString((int)Math.ceil(visualcooldown())); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}
}
