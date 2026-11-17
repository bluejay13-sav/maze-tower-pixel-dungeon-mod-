/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//Maze Tower: 정전기. 전격 피해를 받으면 2턴. 다른 효과는 없고 원소 반응 판정에만 쓴다.
public class StaticCharge extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo(target)){
			ElementalReactions.onStaticApplied(target);
			return true;
		}
		return false;
	}

	@Override
	public int icon() {
		return BuffIndicator.RECHARGING;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 1f, 0.3f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (MazeTower.STATIC_TURNS - visualcooldown()) / MazeTower.STATIC_TURNS);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}
}
