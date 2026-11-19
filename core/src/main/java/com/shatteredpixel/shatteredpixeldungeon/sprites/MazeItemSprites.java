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
	public static final int BOSS_TORCH     = BASE + 3; //5층 보스방 횃불
	public static final int TALISMAN       = BASE + 4; //10층 그림자 간수의 봉인 부적 (날아가는 그림 첫 프레임)
	public static final int WARDEN_CHAIN   = BASE + 5; //10층 그림자 간수가 던지는 사슬 (날아가는 그림 첫 프레임)

	private static final String[] FILES = {
			"sprites/maze_bloom_core.png",
			"sprites/maze_fullbloom_core.png",
			"sprites/maze_fruit_core.png",
			"sprites/maze_p13_torch.png",
			"sprites/maze_fx10_talisman_fly.png",
			"sprites/maze_fx10_chain_fly.png"
	};

	public static boolean has( int image ){
		return image >= BASE && image < BASE + FILES.length;
	}

	//여러 프레임을 이은 그림이면 첫 프레임만 쓴다 (가로 크기, 0이면 그림 전체)
	public static int frameWidth( int image ){
		if (image == TALISMAN) return 8;
		if (image == WARDEN_CHAIN) return 16;
		return 0;
	}

	public static String file( int image ){
		return FILES[image - BASE];
	}
}
