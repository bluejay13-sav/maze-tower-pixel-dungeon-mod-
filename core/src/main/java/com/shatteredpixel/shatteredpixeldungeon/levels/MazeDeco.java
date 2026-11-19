/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.watabou.utils.Random;

//Maze Tower: 5·10층 보스층은 새 구역 그림의 장식(뿌리 벽·바닥 잔뿌리·창살 창문 등)이 너무 많이 보여서,
//장식 칸 일부를 보통 벽·바닥으로 바꾼다. 원작 층 생성 난수에 영향을 주지 않도록 따로 된 난수를 쓴다.
public class MazeDeco {

	public static void thin( Level level ){
		Random.pushGenerator(Dungeon.seedCurDepth() ^ 0xDEC0L);
		try {
			for (int i = 0; i < level.length(); i++){
				if (level.map[i] == Terrain.WALL_DECO && Random.Float() >= MazeTower.BOSS_WALL_DECO_KEEP){
					level.map[i] = Terrain.WALL;
				} else if (level.map[i] == Terrain.EMPTY_DECO && Random.Float() >= MazeTower.BOSS_FLOOR_DECO_KEEP){
					level.map[i] = Terrain.EMPTY;
				}
			}
		} finally {
			Random.popGenerator();
		}
	}
}
