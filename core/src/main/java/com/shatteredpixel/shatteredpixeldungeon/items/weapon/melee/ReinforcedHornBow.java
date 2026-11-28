/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower (W3): 강화 각궁 (활, 5티어, 힘 18). 거리별 평타·화살·사거리는 다른 활과 같다.
//스킬은 각궁과 같은 속사(이름·이펙트·동작이 같고 발당 피해만 MazeTower.SKILL_REINFORCED_RAPID_DMG).
//그림 sprites/maze_bow_reinforced_horn.png (원본 docs/sprites/weapons2/make_tier5.ps1).
public class ReinforcedHornBow extends WornShortbow {

	{
		image = MazeItemSprites.BOW_REINFORCED; //(사용자 확인) 새 그림
		tier = 5;
		bones = true;
	}
}
