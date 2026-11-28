/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

//Maze Tower (사용자): 16~19층 망령 왕 판의 우물 속 물.
//우물 그림의 안쪽은 반투명이라 밑의 물(움직이는 물 그림)이 비쳐 보이는데, 이 판의 물은 아주 짙은 남빛이라 빈 우물처럼 보였다.
//물이 든 우물 칸에만 조금 밝은 물빛을 반쯤 비치게 깐다 (물 그림 위, 우물 그림 아래). 그림 environment/maze_well_water.png,
//원본 docs/sprites/regions/make_well_water.ps1.
public class WellWaterTilemap extends DungeonTilemap {

	public WellWaterTilemap() {
		super( Assets.Environment.MAZE_WELL_WATER );
		map( Dungeon.level.map, Dungeon.level.width() );
	}

	public static boolean active(){
		return Dungeon.level instanceof CityLevel && MazeTower.CITY_BY_BOSS && MazeBosses.ENABLED && !MazeBosses.empress();
	}

	@Override
	protected int getTileVisual( int pos, int tile, boolean flat ) {
		return tile == Terrain.WELL ? 0 : -1;
	}
}
