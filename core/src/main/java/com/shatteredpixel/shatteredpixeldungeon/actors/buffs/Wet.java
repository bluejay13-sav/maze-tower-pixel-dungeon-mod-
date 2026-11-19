/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WaterDropParticle;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

//Maze Tower: 젖음. 물 타일 위에 있는 동안 + 물에서 나온 뒤 2턴.
//다른 효과는 없고 원소 반응 판정에만 쓴다. Char.move에서 물에 들어가면 붙는다.
public class Wet extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	private int left = MazeTower.WET_AFTER_TURNS;

	@Override
	public boolean attachTo( com.shatteredpixel.shatteredpixeldungeon.actors.Char target ) {
		//빙결 상태에서는 젖지 않는다 (물 위에서 얼어 있어도 화염으로 해빙이 일어나게)
		if (target.buff(Frost.class) != null) return false;
		//불타는 대상이 젖으면 화상과 젖음이 함께 사라진다
		if (ElementalReactions.beforeWet(target)) return false;
		return super.attachTo(target);
	}

	public static boolean onWater( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ){
		return ElementalReactions.validPos(ch) && (Dungeon.level.water[ch.pos] || com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SapFloor.at(ch.pos)) && !ch.flying; //Maze Tower: 수액 웅덩이 바닥도 젖는다
	}

	@Override
	public boolean act() {
		//빙결 상태에서는 젖지 않는다
		if (target.buff(Frost.class) != null){
			detach();
			return true;
		}
		if (onWater(target)){
			left = MazeTower.WET_AFTER_TURNS;
		} else {
			left--;
			if (left <= 0){
				detach();
				return true;
			}
		}
		spend(TICK);
		return true;
	}

	//걸려 있는 동안 몸에서 물방울이 떨어진다
	private Emitter drops;

	@Override
	public void fx(boolean on) {
		if (on){
			if (drops == null && target.sprite != null){
				drops = ElementalReactions.statusEmitter(target, WaterDropParticle.FACTORY, 0.2f);
			}
		} else if (drops != null){
			drops.on = false;
			drops = null;
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.OOZE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.3f, 0.6f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (MazeTower.WET_AFTER_TURNS - left) / (float)MazeTower.WET_AFTER_TURNS);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("left", left);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		left = bundle.getInt("left");
	}
}
