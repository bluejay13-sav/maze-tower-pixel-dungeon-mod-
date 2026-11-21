/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower: 장궁 (활, 2티어). 거리별 평타·화살은 단궁(WornShortbow)과 같고, 스킬은 SwordSkill 표: 조준 사격 140%
public class Longbow extends WornShortbow {

	{
		image = MazeItemSprites.BOW_LONG;
		tier = 2;
		bones = true;
	}
}
