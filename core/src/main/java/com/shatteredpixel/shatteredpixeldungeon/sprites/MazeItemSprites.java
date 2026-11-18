/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

//Maze Tower: 원작 items.png에 없는 아이템 그림 (16×16 PNG 한 장씩).
//그림 파일은 core/src/main/assets/sprites/ 에 있다. 같은 이름으로 덮어쓰면 게임 그림이 바뀐다.
//ItemSprite.frame()이 이 번호를 보면 원작 아이템 그림 대신 이 파일을 그린다.
public class MazeItemSprites {

	//원작 ItemSpriteSheet 번호와 겹치지 않는 큰 번호
	private static final int BASE = 900000;

	public static final int BLOOM_CORE     = BASE;     //개화의 핵 (2차)
	public static final int FULLBLOOM_CORE = BASE + 1; //만개의 핵 (3차)
	public static final int FRUIT_CORE     = BASE + 2; //결실의 핵 (4차)

	private static final String[] FILES = {
			"sprites/maze_bloom_core.png",
			"sprites/maze_fullbloom_core.png",
			"sprites/maze_fruit_core.png"
	};

	public static boolean has( int image ){
		return image >= BASE && image < BASE + FILES.length;
	}

	public static String file( int image ){
		return FILES[image - BASE];
	}
}
