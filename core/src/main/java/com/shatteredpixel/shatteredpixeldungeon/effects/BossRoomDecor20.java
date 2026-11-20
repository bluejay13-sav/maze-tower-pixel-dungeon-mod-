/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Image;

//Maze Tower: 20층 보스방 벽 장식 (명세 22번, 그림 docs/sprites/props/p20*_wall_*.png).
//원작 휘장(WALL_DECO) 대신, 방 둘레의 아래가 바닥인 벽(바깥 벽·기둥의 앞면) 중 약 30%(MazeTower.BOSS20_WALL_DECO_PCT)에
//장식을 겹쳐 그린다. 가로로 바로 옆 칸끼리는 붙지 않는다. 칸마다 던전 시드로 정해 저장하지 않아도 늘 같은 자리.
public class BossRoomDecor20 {

	private static final String[] EMPRESS = {
			"sprites/maze_p20e_wall_banner.png", "sprites/maze_p20e_wall_sconce.png",
			"sprites/maze_p20e_wall_niche_red.png", "sprites/maze_p20e_wall_niche_blue.png", "sprites/maze_p20e_wall_niche_yellow.png"
	};
	//휘장·벽등을 조금 더 자주 (3 : 3 : 1 : 1 : 1)
	private static final int[] EMPRESS_WEIGHT = {3, 3, 1, 1, 1};

	private static final String[] WRAITH = {
			"sprites/maze_p20w_wall_banner.png", "sprites/maze_p20w_wall_sconce.png"
	};
	private static final int[] WRAITH_WEIGHT = {1, 1};

	public static void add( GameScene scene ){
		if (!(Dungeon.level instanceof CityBossLevel)) return;
		CityBossLevel l = (CityBossLevel) Dungeon.level;
		if (!l.mazeRoom) return;
		boolean empress = l.mazeEmpressRoom;
		String[] files = empress ? EMPRESS : WRAITH;
		int[] weights = empress ? EMPRESS_WEIGHT : WRAITH_WEIGHT;
		int total = 0;
		for (int w : weights) total += w;

		//가로로 붙지 않게 하면 실제 비율이 줄어드므로 고르는 확률을 조금 올린다: f = p/(1+p) → p = f/(1-f)
		float pct = MazeTower.BOSS20_WALL_DECO_PCT;
		float p = pct / (1f - pct);
		int w = l.width();
		for (int y = CityBossLevel.ROOM_Y - 1; y < CityBossLevel.ROOM_Y + CityBossLevel.ROOM_SIZE; y++){
			boolean prev = false;
			for (int x = 0; x < w; x++){
				int cell = x + y * w;
				boolean pick = false;
				if (frontWall(l, cell)){
					java.util.Random rnd = new java.util.Random(Dungeon.seed ^ 0x20DEC0DEL ^ (cell * 0x9E3779B97F4A7C15L));
					if (!prev && rnd.nextFloat() < p){
						pick = true;
						int roll = rnd.nextInt(total), k = 0;
						while (roll >= weights[k]){ roll -= weights[k]; k++; }
						Image img = new Image(files[k]);
						img.x = x * DungeonTilemap.SIZE;
						img.y = y * DungeonTilemap.SIZE;
						scene.addMazeTerrainOverlay(img);
					}
				}
				prev = pick;
			}
		}
	}

	//아래가 방 바닥인 벽 (바깥 벽·기둥의 앞면)
	private static boolean frontWall( CityBossLevel l, int cell ){
		int below = cell + l.width();
		if (below >= l.length()) return false;
		int t = l.map[cell];
		if (t != Terrain.WALL && t != Terrain.WALL_DECO) return false;
		if (DungeonTileSheet.wallStitcheable(l.map[below])) return false;
		return l.roomFloor(below);
	}
}
