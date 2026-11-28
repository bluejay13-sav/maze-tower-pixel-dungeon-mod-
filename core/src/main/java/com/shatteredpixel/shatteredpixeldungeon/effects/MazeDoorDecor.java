/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeRetreat;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

//Maze Tower: 구역별 문 장식 (명세 20번). 닫힌 문의 일부(MazeTower.DOOR_DECO_PCT)에만 장식이 붙는다.
//기본 타일셋의 문은 깨끗하고, 고른 문에만 장식 그림(sprites/maze_door_decor.png, 32×96)을 겹쳐 그린다.
//  칸: 0번(x 0~15) 정면 문 (가로 벽의 문, 문 칸 자체에 그린다 → 바닥 쪽 층)
//      1번(x 16~31) 옆 방향 문 (세로 벽의 문, 원작은 문 칸 바로 위 벽 칸에 문짝을 그린다 → 벽 쪽 층)
//  줄(16픽셀씩): 0 뿌리 지하(뿌리), 1 봉인의 회랑(쇠사슬), 2 마력 광맥(마석), 3 잊힌 마도시(보라 휘장),
//               4 탑의 심장(검은 가지), 5 역행 중 탑의 심장(벚꽃 가지)
//문이 열려 있으면(누가 서 있으면) 숨긴다. 저장하지 않고 화면을 만들 때마다 층 시드로 같은 문을 고른다.
public class MazeDoorDecor {

	public static final String DECOR = "sprites/maze_door_decor.png";

	public static void add( GameScene scene ){
		Level level = Dungeon.level;
		if (level == null) return;
		int row = row(MazeRetreat.tiles(level.tilesTex()));
		if (row < 0) return;

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
					scene.addMazeWallOverlay(new Decor(i, i - w, 16, row));
				} else {
					scene.addMazeTerrainOverlay(new Decor(i, i, 0, row));
				}
			}
		} finally {
			Random.popGenerator();
		}
	}

	private static int row( String tex ){
		if (Assets.Environment.TILES_SEWERS.equals(tex))       return 0;
		if (Assets.Environment.TILES_PRISON.equals(tex))       return 1;
		if (Assets.Environment.TILES_CAVES.equals(tex))        return 2;
		if (Assets.Environment.TILES_CITY.equals(tex)
				|| Assets.Environment.TILES_CITY_WRAITH.equals(tex)
				|| Assets.Environment.TILES_CITY_EMPRESS.equals(tex)) return 3;
		if (Assets.Environment.TILES_HALLS.equals(tex) || Assets.Environment.TILES_HALLS2.equals(tex)) return 4;
		if (Assets.Environment.TILES_HALLS_RETURN.equals(tex) || Assets.Environment.TILES_HALLS2_RETURN.equals(tex)) return 5;
		return -1; //대장장이 광산 등 원작 그림을 쓰는 층은 장식 없음
	}

	//문 하나에 겹치는 장식 그림. door = 문 칸, drawAt = 그림을 그릴 칸
	private static class Decor extends Image {
		private final int door;

		Decor( int door, int drawAt, int frameX, int row ){
			super(DECOR);
			this.door = door;
			frame(frameX, row * 16, 16, 16);
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
