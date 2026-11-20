/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;

//Maze Tower: 2차 특성이 쓰는 상태(버프). 아이콘은 임시로 원작 아이콘에 색을 입힌다 (E단계에서 buffs2_small.png로 바꾼다).
public class BloomBuffs {

	// ===== 적에게 붙는 것 =====

	//표식: 1개당 받는 피해 +B2_MARK_DMG_PER. 새로 새길 때마다 B2_MARK_TURNS 동안 유지.
	//옮겨 온 표식(transferred)은 다시 옮겨 가지 않는다 (규칙 5).
	public static class Mark extends Buff implements BloomRules.Transferable {
		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		public int stacks = 0;
		public float left = B2_MARK_TURNS;
		public boolean transferred = false;

		@Override public boolean transferred() { return transferred; }

		//표식을 n개 더한다 (max까지). 옮겨 온 것이면 transferred.
		public static Mark add(Char ch, int n, boolean transferred){
			if (!ch.isAlive()) return null;
			Mark m = Buff.affect(ch, Mark.class);
			if (m == null) return null;
			int before = m.stacks;
			if (transferred){
				m.stacks = Math.max(m.stacks, n);
				m.transferred = true;
			} else {
				m.stacks = Math.min(Math.max(m.stacks, 0) + 1, Math.max(n, 1));
			}
			m.left = B2_MARK_TURNS;
			if (ch.sprite != null && m.stacks != before){
				ch.sprite.showStatus(0xE070FF, Messages.get(BloomBuffs.class, "mark_n", m.stacks)); //임시 (E단계: 머리 위 표식 그림)
			}
			return m;
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0){
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override public int icon() { return BuffIndicator.B2_MARK; }
		@Override public String iconTextDisplay() { return Integer.toString(stacks); }
		@Override public String desc() { return Messages.get(this, "desc", stacks, Math.round(stacks * B2_MARK_DMG_PER * 100), (int)Math.ceil(left)); }

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("stacks", stacks);
			bundle.put("left", left);
			bundle.put("transferred", transferred);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt("stacks");
			left = bundle.getFloat("left");
			transferred = bundle.getBoolean("transferred");
		}
	}

	// ===== 주인공에게 붙는 것 =====

	//강타: 같은 적을 때린 횟수 (화면에 보이지 않음)
	public static class HeavyCount extends Buff {
		{ type = buffType.NEUTRAL; }
		public int target = -1;
		public int count = 0;

		//다음 평타가 강타일 때만 아이콘 (강타 준비)
		@Override public int icon() {
			boolean ready = count >= B2_HEAVY_HITS - 1 && BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.HEAVY_BLOW) >= 0;
			return ready ? BuffIndicator.B2_HEAVY : BuffIndicator.NONE;
		}
		@Override public String desc() { return Messages.get(this, "desc"); }

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

	//질풍: 남은 빠른 평타 횟수 (B2_GALE_TURNS 동안)
	public static class Gale extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		public int charges = 1;

		public void use(){
			charges--;
			if (charges <= 0) detach();
		}

		@Override public int icon() { return BuffIndicator.B2_GALE; }
		@Override public String iconTextDisplay() { return Integer.toString(charges); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_GALE_TURNS - visualcooldown()) / B2_GALE_TURNS); }
		@Override public String desc() { return Messages.get(this, "desc", Math.round(B2_GALE_SPEED * 100), charges, dispTurns()); }

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

	//확정타: 남은 강화 평타
	public static class SureStrike extends Buff {
		{ type = buffType.POSITIVE; }
		public int charges = 1;

		public void use(){
			charges--;
			if (charges <= 0) detach();
		}

		@Override public int icon() { return BuffIndicator.B2_SURE; }
		@Override public String iconTextDisplay() { return Integer.toString(charges); }
		@Override public String desc() {
			int s = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.SURE_STRIKE));
			return Messages.get(this, "desc", Math.round(B2_SURE_BONUS[s] * 100), charges);
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

	//확정타 쿨타임
	public static class SureCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.B2_SURE; }
		@Override public void tintIcon(Image icon) { icon.brightness(0.4f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_SURE_COOLDOWN - visualcooldown()) / B2_SURE_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//돌진: 이동해서 다음 공격이 강해지는 상태
	public static class ChargeReady extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.B2_CHARGE; }
		@Override public String desc() {
			int c = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.CHARGE));
			return Messages.get(this, "desc", Math.round(B2_CHARGE_DMG[c] * 100));
		}
	}

	//돌진 쿨타임
	public static class ChargeCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.B2_CHARGE; }
		@Override public void tintIcon(Image icon) { icon.brightness(0.4f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_CHARGE_COOLDOWN - visualcooldown()) / B2_CHARGE_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//집중 쿨타임
	public static class FocusCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.B2_FOCUS; }
		@Override public void tintIcon(Image icon) { icon.brightness(0.4f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_FOCUS_COOLDOWN - visualcooldown()) / B2_FOCUS_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	// ===== 공용 특성 (C3) =====

	//기세: 처치마다 1스택 (최대 B2_MOMENTUM_MAX), B2_MOMENTUM_TURNS 동안 처치하지 못하면 사라진다
	public static class Momentum extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		public int stacks = 0;

		@Override public int icon() { return BuffIndicator.B2_MOMENTUM; }
		@Override public String iconTextDisplay() { return Integer.toString(stacks); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_MOMENTUM_TURNS - visualcooldown()) / B2_MOMENTUM_TURNS); }
		@Override public String desc() {
			int m = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.MOMENTUM));
			return Messages.get(this, "desc", stacks, Math.round(stacks * B2_MOMENTUM_DMG[m] * 100), dispTurns());
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("stacks", stacks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt("stacks");
		}
	}

	//거리 벌리기: 물러나서 다음 공격이 강해지는 상태 (맞으면 풀린다)
	public static class DisengageReady extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.B2_DISENGAGE; }
		@Override public String desc() {
			int d = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.DISENGAGE));
			return Messages.get(this, "desc", Math.round(B2_DISENGAGE_BONUS[d] * 100));
		}
	}

	//거리 벌리기 쿨타임
	public static class DisengageCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.B2_DISENGAGE; }
		@Override public void tintIcon(Image icon) { icon.brightness(0.4f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_DISENGAGE_COOLDOWN - visualcooldown()) / B2_DISENGAGE_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//원소 순환 각성: 다음 공격이 무작위 원소를 건다
	public static class CycleCharge extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.B2_CYCLE; }
		@Override public String desc() { return Messages.get(this, "desc"); }
	}

	//원소 증폭 각성 쿨타임 (화면에 보이지 않음)
	public static class AmpCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
	}

	//명상 각성 쿨타임
	public static class MeditationCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.B2_MEDITATION; }
		@Override public float iconFadePercent() { return Math.max(0, (B2_MEDITATION_AWAKE_COOLDOWN - visualcooldown()) / B2_MEDITATION_AWAKE_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//명상 각성: B2_MEDITATION_AWAKE_TURNS 동안 유지되는 방어막
	public static class MeditationShield extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff {
		{ type = buffType.POSITIVE; }
		public float left = B2_MEDITATION_AWAKE_TURNS;

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0 || shielding() <= 0){
				detach();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.5f, 0.7f, 1f); }
		@Override public String iconTextDisplay() { return Integer.toString(shielding()); }
		@Override public String desc() { return Messages.get(this, "desc", shielding(), (int)Math.ceil(left)); }

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat("left");
		}
	}

	//거리 벌리기 각성: 물러난 칸에 남는 잔상. 밟은 적은 1턴 속박되고 잔상은 사라진다. 다시 퍼지지 않는다 (규칙 5).
	public static class Afterimage extends com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob {
		@Override
		protected void evolve() {
			int cell;
			for (int i = area.left; i < area.right; i++){
				for (int j = area.top; j < area.bottom; j++){
					cell = i + j * com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.width();
					if (cur[cell] <= 0){
						off[cell] = 0;
						continue;
					}
					Char ch = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY){
						Buff.affect(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, B2_DISENGAGE_AWAKE_ROOT);
						off[cell] = 0;
						continue;
					}
					off[cell] = cur[cell] - 1;
					volume += off[cell];
				}
			}
		}

		@Override
		public void use(com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter emitter) {
			super.use(emitter);
			//임시 (E단계: fx2_sheet 잔상 칸 그림)
			emitter.pour(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.LIGHT), 0.6f);
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}

	//섬광 보법 쿨타임
	public static class FlashCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.HASTE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_FLASH_COOLDOWN - visualcooldown()) / B2_FLASH_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//결투·난전: 조건이 맞는 동안만 아이콘이 보인다. 매 턴 다시 본다 (BloomEffects.updateHeroFx가 붙인다).
	public static abstract class Sense extends Buff {
		{ type = buffType.POSITIVE; }
		protected int value = 0; //결투: 1/0, 난전: 인접한 적 수

		protected abstract int compute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero);

		@Override
		public boolean act() {
			recheck();
			spend(TICK);
			return true;
		}

		public void recheck(){
			if (!(target instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero)) return;
			int v = compute((com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero) target);
			if (v != value){
				value = v;
				BuffIndicator.refreshHero();
			}
		}
	}

	public static class DuelSense extends Sense {
		@Override protected int compute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero) {
			return BloomCommon.duelOpponent(hero) != null ? 1 : 0;
		}
		@Override public int icon() { return value > 0 ? BuffIndicator.B2_DUEL : BuffIndicator.NONE; }
		@Override public String desc() {
			int d = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.DUEL));
			return Messages.get(this, "desc", Math.round(B2_DUEL_DMG[d] * 100));
		}
	}

	public static class MeleeSense extends Sense {
		@Override protected int compute(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero) {
			return Math.min(B2_MELEE_MAX, BloomCommon.adjacentEnemies(hero));
		}
		@Override public int icon() { return value > 0 ? BuffIndicator.B2_MELEE : BuffIndicator.NONE; }
		@Override public String iconTextDisplay() { return Integer.toString(value); }
		@Override public String desc() {
			int m = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.MELEE));
			return Messages.get(this, "desc", value, Math.round(value * B2_MELEE_DMG[m] * 100));
		}
	}
}
