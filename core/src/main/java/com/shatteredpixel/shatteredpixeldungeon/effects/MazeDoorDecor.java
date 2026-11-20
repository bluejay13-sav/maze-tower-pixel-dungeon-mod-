/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

//Maze Tower: 뿌리 지하 문 장식 (명세 20번). 닫힌 문의 일부(MazeTower.DOOR_DECO_PCT)에만 뿌리가 감겨 있다.
//기본 타일셋의 문은 깨끗하고, 고른 문에만 뿌리 그림(sprites/maze_door_roots.png)을 겹쳐 그린다.
//  0번 칸(x 0~15): 정면 문 (가로 벽의 문, 문 칸 자체에 그린다 → 바닥 쪽 층)
//  1번 칸(x 16~31): 옆 방향 문 (세로 벽의 문, 원작은 문 칸 바로 위 벽 칸에 문짝을 그린다 → 벽 쪽 층)
//문이 열려 있으면(누가 서 있으면) 숨긴다. 저장하지 않고 화면을 만들 때마다 층 시드로 같은 문을 고른다.
public class MazeDoorDecor {

	public static final String ROOTS = "sprites/maze_door_roots.png"; //32×16, 정면·옆 2칸

	public static void add( GameScene scene ){
		Level level = Dungeon.level;
		if (level == null || !Assets.Environment.TILES_SEWERS.equals(level.tilesTex())) return;

		int w = level.width();
		Random.pushGenerator(Dungeon.seedCurDepth() + 0x4D44); //원작 층 생성과 상관없는 따로 된 난수
		try {
			for (int i = 0; i < level.length(); i++){
				int t = level.map[i];
				if (t != Terrain.DOOR && t != Terrain.OPEN_DOOR) continue;
				if (Random.Float() >= MazeTower.DOOR_DECO_PCT) continue;
				boolean sideways = i + w < level.length() && DungeonTileSheet.wallStitcheable(level.map[i + w]);
				if (sideways){
					if (i - w < 0) continue;
					scene.addMazeWallOverlay(new Roots(i, i - w, 16));
				} else {
					scene.addMazeTerrainOverlay(new Roots(i, i, 0));
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	//문 하나에 겹치는 뿌리 그림. door = 문 칸, drawAt = 그림을 그릴 칸
	private static class Roots extends Image {
		private final int door;

		Roots( int door, int drawAt, int frameX ){
			super(ROOTS);
			this.door = door;
			frame(frameX, 0, 16, 16);
			x = (drawAt % Dungeon.level.width()) * DungeonTilemap.SIZE;
			y = (drawAt / Dungeon.level.width()) * DungeonTilemap.SIZE;
		}

		@Override
		public void update() {
			super.update();
			//닫힌 문일 때만 (열린 문·부서진 문에는 없음)
			visible = Dungeon.level != null && door < Dungeon.level.length() && Dungeon.level.map[door] == Terrain.DOOR;
		}
	}
}
