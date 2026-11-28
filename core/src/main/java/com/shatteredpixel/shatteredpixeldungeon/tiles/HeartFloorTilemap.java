/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeRetreat;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;

//Maze Tower (사용자): 21~24층 탑의 심장의 바닥·벽면·핏줄 박동 (명세 18번).
//칸 하나마다 같은 그림을 찍으면 무늬가 칸마다 되풀이되므로, 15×15칸(240×240) 크기의 큰 그림을 칸 자리(x % 15, y % 15)에 맞게 잘라
//원작 바닥 타일 위에 덮어 그린다. 그래서 15칸마다 한 번만 되풀이되고 칸 경계가 보이지 않는다.
// - FLOOR: 바닥(빈 칸·장식 바닥·특수 바닥·풀·함정 자리 등) — 어두운 돌 판석과 군데군데 번진 살점 / 역행 중에는 잿갈색 판석과 틈에서 삐져나온 이끼
// - WALL : 바닥에 맞닿은 솟은 벽면 — 15칸 길이 막돌 띠(줄 0~3 = 원작 솟은 벽 0~3번처럼 양 끝 모양)
// - UNDER: 지형지물 칸(계단·우물·받침대·석상·기둥·솥·돌더미) 밑의 바닥 — 원작 타일 아래에 깔고, 지형지물 그림은 배경이 비어 있어 그 위에 얹힌다(make_region_objects.ps1)
// - PULSE: 핏줄 박동 겹 — 두(3프레임)·텀(1)·근(3)·쉼(18), 한 프레임 1/12초. 핏줄 칸마다 늦음이 달라 박동이 한쪽부터 번진다 (역행 중에는 없음)
//그림 원본 docs/sprites/regions/make_heart_tissue.ps1·make_heart_wall.ps1·make_heart_assets.ps1
public class HeartFloorTilemap extends DungeonTilemap {

	public static final int FLOOR = 0, WALL = 1, PULSE = 2, UNDER = 3;

	private static final int SPAN = 15;          //큰 그림의 칸 수 (가로·세로)
	private static final int PULSE_FRAMES = 25;  //박동 한 번의 프레임 수
	private static final int PULSE_COLS = 5;     //박동 그림에서 한 줄에 놓인 프레임 수
	private static final float PULSE_FPS = 12f;

	private final int kind;
	private int frame = 0;

	private HeartFloorTilemap( int kind, String tex ){
		super( tex );
		this.kind = kind;
		map( Dungeon.level.map, Dungeon.level.width() );
	}

	//이 층에서 새 바닥을 쓰는지: 21~24층 일반 층(HallsLevel)과 25층 탑의 심장 보스층
	public static boolean active(){
		return MazeTower.HEART_NEW_FLOOR && Dungeon.level != null
				&& (Dungeon.level.getClass() == HallsLevel.class || Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel //(사용자) 25층 보스층도
				|| Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.LastLevel); //(사용자) 26층도
	}

	private static boolean retreat(){
		return !MazeRetreat.tiles( Assets.Environment.TILES_HALLS2 ).equals( Assets.Environment.TILES_HALLS2 );
	}

	public static HeartFloorTilemap floor(){
		return new HeartFloorTilemap( FLOOR, retreat() ? Assets.Environment.HEART_FLOOR_RETURN : Assets.Environment.HEART_FLOOR );
	}

	public static HeartFloorTilemap wall(){
		return new HeartFloorTilemap( WALL, retreat() ? Assets.Environment.HEART_WALLFACE_RETURN : Assets.Environment.HEART_WALLFACE );
	}

	//지형지물 밑 바닥: 원작 타일보다 먼저(아래에) 그린다
	public static HeartFloorTilemap under(){
		return new HeartFloorTilemap( UNDER, retreat() ? Assets.Environment.HEART_FLOOR_RETURN : Assets.Environment.HEART_FLOOR );
	}

	public static boolean objectTile( int tile ){
		switch (tile){
			case Terrain.STATUE: case Terrain.STATUE_SP: case Terrain.REGION_DECO: case Terrain.REGION_DECO_ALT:
			case Terrain.ALCHEMY: case Terrain.BARRICADE: case Terrain.WELL: case Terrain.EMPTY_WELL: case Terrain.PEDESTAL:
			case Terrain.ENTRANCE: case Terrain.ENTRANCE_SP: case Terrain.EXIT:
			case Terrain.DOOR: case Terrain.OPEN_DOOR: case Terrain.LOCKED_DOOR: case Terrain.CRYSTAL_DOOR: case Terrain.HERO_LKD_DR: //(사용자) 문 칸 아래쪽 바닥도
			case Terrain.LOCKED_EXIT: case Terrain.UNLOCKED_EXIT:
				return true;
			default:
				return false;
		}
	}

	//역행 중에는 null
	public static HeartFloorTilemap pulse(){
		return retreat() ? null : new HeartFloorTilemap( PULSE, Assets.Environment.HEART_PULSE );
	}

	public static boolean floorTile( int tile ){
		switch (tile){
			case Terrain.EMPTY: case Terrain.EMPTY_DECO: case Terrain.EMPTY_SP:
			case Terrain.GRASS: case Terrain.HIGH_GRASS: case Terrain.FURROWED_GRASS: case Terrain.EMBERS:
			case Terrain.SECRET_TRAP: case Terrain.TRAP: case Terrain.INACTIVE_TRAP:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected int getTileVisual( int pos, int tile, boolean flat ){
		int w = mapWidth;
		int x = pos % w, y = pos / w;
		//(사용자) 물이 든 우물은 안쪽이 반투명이라 밑의 물이 비쳐야 하므로 바닥을 깔지 않는다 (우물 그림은 안쪽 말고는 불투명)
		if (kind == UNDER) return objectTile( tile ) && tile != Terrain.WELL ? (y % SPAN) * SPAN + (x % SPAN) : -1;
		if (kind == FLOOR || kind == PULSE){
			if (!floorTile( tile )) return -1;
			if (kind == FLOOR) return (y % SPAN) * SPAN + (x % SPAN);
			int fx = (frame % PULSE_COLS) * SPAN, fy = (frame / PULSE_COLS) * SPAN;
			return (fy + y % SPAN) * (SPAN * PULSE_COLS) + fx + (x % SPAN);
		}
		//WALL: 원작 솟은 벽(DungeonTileSheet.getRaisedWallTile)과 같은 조건에서, 보통 벽만 (장식 벽·책장·문 위 벽은 원래 그림)
		if (tile != Terrain.WALL && tile != Terrain.SECRET_DOOR) return -1;
		int below = pos + w < size ? map[pos + w] : -1;
		if (below == -1 || DungeonTileSheet.wallStitcheable( below ) || DungeonTileSheet.doorTile( below )) return -1;
		int right = (pos + 1) % w != 0 ? map[pos + 1] : -1;
		int left = pos % w != 0 ? map[pos - 1] : -1;
		int row = 0;
		if (!DungeonTileSheet.wallStitcheable( right )) row += 1;
		if (!DungeonTileSheet.wallStitcheable( left )) row += 2;
		return row * SPAN + (x % SPAN);
	}

	@Override
	public void update(){
		super.update();
		if (kind == PULSE){
			int f = (int)(Game.timeTotal * PULSE_FPS) % PULSE_FRAMES;
			if (f != frame){
				frame = f;
				updateMap();
			}
		}
	}

	@Override
	public void draw(){
		if (kind == PULSE){
			Blending.setLightMode();   //핏줄이 은은하게 빛나도록 더해 그림
			super.draw();
			Blending.setNormalMode();
		} else {
			super.draw();
		}
	}
}
