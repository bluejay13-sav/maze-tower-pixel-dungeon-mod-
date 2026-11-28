/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower (W4): 은 송곳니 (활, 5티어, 힘 18). 거리별 평타·화살·사거리는 다른 활과 같다.
//스킬 "숨통 물기"(SwordSkill): 화살 1발 185%(MazeTower.SKILL_THROAT_DMG), 맞는 순간 대상 체력이 최대의 절반보다 낮으면 방어를 무시한다.
//그림 sprites/maze_bow_silver_fang.png (원본 docs/sprites/weapons2/make_tier5.ps1).
public class SilverFang extends WornShortbow {

	{
		image = MazeItemSprites.BOW_FANG; //(사용자 확인) 새 그림
		tier = 5;
		bones = true;
	}
}
