/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.MazeRetreat;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rubble;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//Maze Tower: 광폭 (명세 14번). 역행 중 모든 적이 걸린다.
//아뮬렛에서 힘을 받다가 그 힘이 사라져 통제하지 못하는 상태: 공격력 +30%, 도망치지 않는다.
public class RetreatFrenzy extends Buff {

	{
		type = buffType.NEGATIVE; //적에게 붙은 것이므로 주인공 입장에서 위험 표시
		announced = false;
	}

	//Mob.act에서 호출: 역행 중이면 적에게 광폭을 걸고, 도망치던 적은 다시 쫓아온다
	public static void apply( Mob mob ){
		if (!MazeRetreat.active() || mob.alignment != Char.Alignment.ENEMY || mob instanceof Rubble) return;
		if (mob.buff(RetreatFrenzy.class) == null) Buff.affect(mob, RetreatFrenzy.class);
		if (mob.state == mob.FLEEING) mob.state = mob.HUNTING;
	}

	//Char.attack에서 곱하는 공격력 배율
	public static float damageMulti( Char attacker ){
		return attacker.buff(RetreatFrenzy.class) != null ? 1f + MazeTower.BERSERK_DMG : 1f;
	}

	@Override
	public int icon() {
		return BuffIndicator.RAGE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.35f, 0.6f);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round(MazeTower.BERSERK_DMG * 100));
	}
}
