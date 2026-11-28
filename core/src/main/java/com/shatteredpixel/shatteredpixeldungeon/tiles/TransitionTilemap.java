/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.MazeTransition;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.TextureFilm;

//Maze Tower (사용자, 2026-11-27): 보스층 출구 둘레에 다음 구역의 땅 그림을 은은히 섞어 그리는 층 (MazeTransition).
//땅 그림 층과 같은 칸 그림을 다음 구역의 타일 시트로 그리는 층을 진하기마다 하나씩(셋) 겹치고, 칸마다 단계(stage)에 맞는 층에만 그린다.
//big이면 21~24층처럼 큰 바닥 그림을 칸 자리(x%15·y%15)로 잘라 바닥 칸에만 그린다.
public class TransitionTilemap extends DungeonTerrainTilemap {

	public static int[] stage = new int[0];

	private int level;
	private boolean big, skipFloor;

	//땅 그림 층의 instance(원작이 다른 곳에서 씀)를 덮어쓰지 않게 만든 뒤 되돌린다
	public static TransitionTilemap create( String tex, int level, boolean big, boolean skipFloor ){
		DungeonTerrainTilemap keep = instance;
		TransitionTilemap t = new TransitionTilemap( tex, level, big, skipFloor );
		instance = keep;
		return t;
	}

	private TransitionTilemap( String tex, int level, boolean big, boolean skipFloor ){
		super();
		this.level = level;
		this.big = big;
		this.skipFloor = skipFloor;
		texture = TextureCache.get( tex );
		tileset = new TextureFilm( texture, SIZE, SIZE );
		alpha( MazeTower.EXIT_BLEND_ALPHAS[level] );
		updateMap();
	}

	@Override
	public synchronized void updateMap() {
		stage = MazeTransition.stages( Dungeon.level );
		super.updateMap();
	}

	@Override
	protected int getTileVisual( int pos, int tile, boolean flat ){
		boolean floor = HeartFloorTilemap.floorTile( tile );
		if (big){
			if (!floor) return -1;
			int w = mapWidth, x = pos % w, y = pos / w;
			return (y % 15) * 15 + (x % 15);
		}
		if (skipFloor && floor) return -1;
		//벽 속(아래도 벽인 칸)은 지금 구역 그대로: 바닥에서 보이는 벽면만 섞는다 (벽 속 빛깔이 달라 네모가 비쳐 보임)
		if (DungeonTileSheet.wallStitcheable( tile ) && (pos + mapWidth >= size || DungeonTileSheet.wallStitcheable( map[pos + mapWidth] ))) return -1;
		//계단·석상·문 같은 지형지물은 지금 구역 그림 그대로 둔다
		if (HeartFloorTilemap.objectTile( tile )) return -1;
		//(사용자) 가장 진한 칸 몇 개에 하나만 그 구역의 장식 바닥(광맥 조각·금 상감 등)으로, 어떤 곳인지 알아보기 쉽게
		if (tile == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY && pos < stage.length && stage[pos] >= 3 && (pos * 7919 + 13) % MazeTower.EXIT_BLEND_DECO_EVERY == 0)
			return DungeonTileSheet.getVisualWithAlts( DungeonTileSheet.FLOOR_DECO, pos );
		//(사용자) 나머지 바닥은 장식 없는 바닥만 (다른 바닥 칸 그림에도 조각·잔돌이 들어 있어 너무 많아 보임)
		if (tile == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY) return DungeonTileSheet.FLOOR;
		return super.getTileVisual( pos, tile, flat );
	}

	//진하기(alpha)가 먹도록 빛 계산이 있는 그리기를 쓴다 (원작 CustomTilemap과 같음)
	@Override
	protected com.watabou.noosa.NoosaScript script() {
		return com.watabou.noosa.NoosaScript.get();
	}

	@Override
	protected boolean needsRender( int pos ) {
		return pos < stage.length && stage[pos] == level + 1 && super.needsRender( pos );
	}
}
