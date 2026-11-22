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
		//(결정) 젖음은 원소 반응용 표시일 뿐이라 "나쁜 상태이상"이 아니다.
		//나쁜 상태이상이면 원작에서 중립 몬스터(놀 추방자·석상)가 적으로 바뀌고, 잠든 몬스터가 깨고, 상점 주인이 떠난다.
		type = buffType.NEUTRAL;
		announced = true;
	}

	private int left = MazeTower.WET_AFTER_TURNS;

	//Maze Tower: 2차 특성 물보라 - 물 밖에서 남은 턴을 늘린다
	public void setLeft( int turns ){
		left = Math.max(left, turns);
	}

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
	private com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Loop drops; //(2026-11-21) 다시 그린 반복 그림

	@Override
	public void fx(boolean on) {
		if (on){
			if (drops == null && target.sprite != null){
				drops = com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Loop.on(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.WET, target.sprite);
			}
		} else if (drops != null){
			drops.stop();
			drops = null;
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.WET; //(2026-11-21) 새 아이콘
	}

	@Override
	public void tintIcon(Image icon) {
		icon.resetColor();
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
