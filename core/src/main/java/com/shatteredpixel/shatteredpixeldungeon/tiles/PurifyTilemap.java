/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.TextureFilm;

//Maze Tower (O7, 사용자): 25층 탑의 주인을 쓰러뜨리면 줄기 둘레의 바닥이 정화되어 "탑의 심장 역행" 바닥 그림(maze_tiles_heart_return.png)으로 바뀐다.
//땅 그림 층(DungeonTerrainTilemap)과 같은 칸 그림을 역행 바닥 그림으로 그리는 층을 셋 겹친다(진하기 0.35 / 0.7 / 1).
//칸마다 지금 진하기 단계(stage[칸] 0 없음 · 1 · 2 · 3)를 정하면 그 단계의 층에만 그려진다. 단계가 오르며 서서히 바뀌는 것처럼 보이고, 가장자리 칸은 옅게 남아 경계가 부드럽다.
public class PurifyTilemap extends DungeonTerrainTilemap {

	public static int[] stage = new int[0];
	public static final float[] ALPHAS = { 0.6f, 0.85f, 1f }; //(사용자) 더 선명하게 (예전 0.35·0.7·1)

	private final int level;

	//땅 그림 층의 instance(원작이 다른 곳에서 씀)를 덮어쓰지 않게 만든 뒤 되돌린다
	public static PurifyTilemap create( int level ){
		DungeonTerrainTilemap keep = instance;
		PurifyTilemap t = new PurifyTilemap(level);
		instance = keep;
		return t;
	}

	private PurifyTilemap( int level ){
		super();
		this.level = level;
		//Maze Tower (사용자): 새 바닥이면 정화된 바닥도 큰 역행 바닥 그림(maze_heart_floor_return.png)을 칸 자리로 잘라 써 주변과 이어지게
		texture = TextureCache.get( com.shatteredpixel.shatteredpixeldungeon.MazeTower.HEART_NEW_FLOOR ? Assets.Environment.HEART_FLOOR_RETURN : Assets.Environment.TILES_HALLS_RETURN );
		tileset = new TextureFilm( texture, SIZE, SIZE );
		alpha( ALPHAS[level] );
		updateMap();
	}

	@Override
	protected int getTileVisual( int pos, int tile, boolean flat ){
		if (!com.shatteredpixel.shatteredpixeldungeon.MazeTower.HEART_NEW_FLOOR) return super.getTileVisual( pos, tile, flat );
		if (!HeartFloorTilemap.floorTile( tile ) && !HeartFloorTilemap.objectTile( tile )) return -1;
		int w = mapWidth, x = pos % w, y = pos / w;
		return (y % 15) * 15 + (x % 15);
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
