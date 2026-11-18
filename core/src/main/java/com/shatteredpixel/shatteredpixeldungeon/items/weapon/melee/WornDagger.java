/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//Maze Tower: 낡은 단검 (도적 시작 무기, 1티어, 단검 계열).
//수치와 기습 피해(기습한 적에게 최대 피해 쪽으로)는 원작 단검(Dagger)과 같다.
//스킬은 급소 찌르기 (SwordSkill). 그림은 임시로 원작 단검.
public class WornDagger extends Dagger {

	{
		image = ItemSpriteSheet.DAGGER;
		tier = 1;
		bones = false;
	}
}
