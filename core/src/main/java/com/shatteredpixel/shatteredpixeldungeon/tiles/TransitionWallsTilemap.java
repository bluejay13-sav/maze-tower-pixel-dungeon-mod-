/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.TextureFilm;

import java.util.HashSet;

//Maze Tower (사용자, 2026-11-27): 보스층 출구 둘레에 다음 구역의 벽 윗부분을 은은히 섞어 그리는 층 (MazeTransition, 단계는 TransitionTilemap.stage)
public class TransitionWallsTilemap extends DungeonWallsTilemap {

	private int level;

	//원작 벽 층이 쓰는 skipCells를 비우지 않게 만든 뒤 되돌린다
	public static TransitionWallsTilemap create( String tex, int level ){
		HashSet<Integer> keep = new HashSet<>( skipCells );
		TransitionWallsTilemap t = new TransitionWallsTilemap( tex, level );
		skipCells.clear();
		skipCells.addAll( keep );
		return t;
	}

	private TransitionWallsTilemap( String tex, int level ){
		super();
		this.level = level;
		texture = TextureCache.get( tex );
		tileset = new TextureFilm( texture, SIZE, SIZE );
		alpha( MazeTower.EXIT_BLEND_ALPHAS[level] );
		updateMap();
	}

	//벽 속(아래도 벽인 칸)은 지금 구역 그대로, 바닥 위로 드리운 벽 윗부분만 섞는다
	@Override
	protected int getTileVisual( int pos, int tile, boolean flat ){
		if (DungeonTileSheet.wallStitcheable( tile ) && (pos + mapWidth >= size || DungeonTileSheet.wallStitcheable( map[pos + mapWidth] ))) return -1;
		return super.getTileVisual( pos, tile, flat );
	}

	@Override
	protected com.watabou.noosa.NoosaScript script() {
		return com.watabou.noosa.NoosaScript.get();
	}

	@Override
	protected boolean needsRender( int pos ) {
		int[] st = TransitionTilemap.stage;
		return pos < st.length && st[pos] == level + 1 && super.needsRender( pos );
	}
}
