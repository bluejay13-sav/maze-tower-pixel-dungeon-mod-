/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower: 정령의 활 (활, 4티어). 거리별 평타·화살은 단궁(WornShortbow)과 같고, 스킬은 SwordSkill 표: 꿰뚫기 (벽 관통 180%)
public class MazeSpiritBow extends WornShortbow {

	{
		image = MazeItemSprites.BOW_SPIRIT;
		tier = 4;
		bones = true;
	}
}
