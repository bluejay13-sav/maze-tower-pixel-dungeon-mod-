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

		@Override public int icon() { return BuffIndicator.MARK; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.9f, 0.4f, 1f); }
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

		@Override public int icon() { return BuffIndicator.HASTE; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.5f, 1f, 0.8f); }
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

		@Override public int icon() { return BuffIndicator.TARGETED; }
		@Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.4f, 0.3f); }
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
		@Override public int icon() { return BuffIndicator.TARGETED; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_SURE_COOLDOWN - visualcooldown()) / B2_SURE_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	//돌진: 이동해서 다음 공격이 강해지는 상태
	public static class ChargeReady extends Buff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.MOMENTUM; }
		@Override public void tintIcon(Image icon) { icon.hardlight(1f, 0.85f, 0.4f); }
		@Override public String desc() {
			int c = Math.max(0, BloomEffects.lvl(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, BloomTalent.CHARGE));
			return Messages.get(this, "desc", Math.round(B2_CHARGE_DMG[c] * 100));
		}
	}

	//돌진 쿨타임
	public static class ChargeCooldown extends FlavourBuff {
		{ type = buffType.NEUTRAL; }
		@Override public int icon() { return BuffIndicator.MOMENTUM; }
		@Override public void tintIcon(Image icon) { icon.hardlight(0.35f, 0.35f, 0.35f); }
		@Override public float iconFadePercent() { return Math.max(0, (B2_CHARGE_COOLDOWN - visualcooldown()) / B2_CHARGE_COOLDOWN); }
		@Override public String iconTextDisplay() { return Integer.toString((int)visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}
}
