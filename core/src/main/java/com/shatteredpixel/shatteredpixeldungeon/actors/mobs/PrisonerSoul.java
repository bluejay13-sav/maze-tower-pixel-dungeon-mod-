/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WraithSprite;
import com.watabou.utils.Random;

//Maze Tower: 죄수의 영혼 (10층 그림자 간수가 '영혼 해방'으로 철창에서 불러낸다).
//원작 감옥 층 경비병(Guard)과 비슷한 체력·공격으로 근접해 달려든다. 경험치·전리품 없음.
//그림은 임시로 원작 혼령(wraith)에 분홍빛 (나중에 감옥 층 잡몹을 만들 때 바뀐다).
public class PrisonerSoul extends Mob {

	{
		spriteClass = SoulSprite.class;

		HP = HT = 40;
		defenseSkill = 10;
		EXP = 0;
		maxLvl = -2;

		state = HUNTING;
		flying = true;

		properties.add(Property.UNDEAD);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(4, 12);
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 7);
	}

	@Override
	public boolean reset() {
		return false;
	}

	public static class SoulSprite extends WraithSprite {
		public SoulSprite(){
			super();
			hardlight(1f, 0.6f, 0.85f); //분홍빛
		}

		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.6f, 0.85f);
		}
	}
}
