/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;

public class GridTileMap extends DungeonTilemap {

	public GridTileMap() {
		super( Assets.Environment.VISUAL_GRID );

		map( Dungeon.level.map, Dungeon.level.width() );
		//Maze Tower (사용자): 역행 중 21~26층의 밝은 바닥에서는 칸 격자(한 칸 건너 옅은 바둑판)가 네모난 명암으로 비쳐 보여 옅게
		if (com.shatteredpixel.shatteredpixeldungeon.MazeRetreat.active() && HeartFloorTilemap.active()) alpha( com.shatteredpixel.shatteredpixeldungeon.MazeTower.RETREAT_GRID_ALPHA );
	}

	//진하기(alpha)가 먹도록 빛 계산이 있는 그리기 (원작 CustomTilemap과 같음)
	@Override
	protected com.watabou.noosa.NoosaScript script() {
		return com.watabou.noosa.NoosaScript.get();
	}

	private int gridSetting = -1;

	@Override
	public synchronized void updateMap() {
		gridSetting = SPDSettings.visualGrid();
		super.updateMap();
	}

	@Override
	protected int getTileVisual(int pos, int tile, boolean flat) {
		if (gridSetting == -1 || (pos % mapWidth) % 2 != (pos / mapWidth) % 2){
			return -1;
		} else if (DungeonTileSheet.floorTile(tile) || tile == Terrain.HIGH_GRASS || tile == Terrain.FURROWED_GRASS) {
			return gridSetting;
		} else if (DungeonTileSheet.doorTile(tile)){
			if (DungeonTileSheet.wallStitcheable(map[pos - mapWidth])){
				return 12 + gridSetting;
			} else if ( tile == Terrain.OPEN_DOOR){
				return 8 + gridSetting;
			} else {
				return 4 + gridSetting;
			}
		} else {
			return -1;
		}
	}

}
