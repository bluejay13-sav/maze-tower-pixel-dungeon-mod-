/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower: 석궁 (활, 3티어). 거리별 평타·화살은 단궁(WornShortbow)과 같고, 스킬은 SwordSkill 표: 장전 (다음 원거리 평타 230%)
public class MazeCrossbow extends WornShortbow {

	{
		image = MazeItemSprites.BOW_CROSS;
		tier = 3;
		bones = true;
	}
}
