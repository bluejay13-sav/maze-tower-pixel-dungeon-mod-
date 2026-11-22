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
	//사냥꾼 활 7종
	public static final int BOW_SHORT  = BASE + 6;  //단궁
	public static final int BOW_PELLET = BASE + 7;  //탄궁
	public static final int BOW_LONG   = BASE + 8;  //장궁
	public static final int BOW_HORN   = BASE + 9;  //각궁
	public static final int BOW_CROSS  = BASE + 10; //석궁
	public static final int BOW_GREAT  = BASE + 11; //정량궁
	public static final int BOW_SPIRIT = BASE + 12; //정령의 활
	//검 7종 (2026-11-21 새 그림, 원본 docs/sprites/weapons2/weapon_00~06.png)
	public static final int SWORD_WORN      = BASE + 13; //낡은 숏소드
	public static final int SWORD_SHORT     = BASE + 14; //숏소드
	public static final int SWORD_RAPIER    = BASE + 15; //레이피어
	public static final int SWORD_SCIMITAR  = BASE + 16; //시미터
	public static final int SWORD_LONG      = BASE + 17; //롱소드
	public static final int SWORD_KATANA    = BASE + 18; //카타나
	public static final int SWORD_RUNIC     = BASE + 19; //룬 블레이드
	//화살 (2026-11-21, docs/sprites/hunter_fx): 아이템 아이콘, 날아가는 화살(기본·강화·정령). 석궁도 같은 화살
	public static final int ARROW           = BASE + 20;
	public static final int ARROW_SHOT      = BASE + 21;
	public static final int ARROW_LOADED    = BASE + 22;
	public static final int ARROW_SPIRIT    = BASE + 23;

	private static final String[] FILES = {
			"sprites/maze_bloom_core.png",
			"sprites/maze_fullbloom_core.png",
			"sprites/maze_fruit_core.png",
			"sprites/maze_p13_torch.png",
			"sprites/maze_fx10_talisman_fly.png",
			"sprites/maze_fx10_chain_fly.png",
			"sprites/maze_bow_short.png",
			"sprites/maze_bow_pellet.png",
			"sprites/maze_bow_long.png",
			"sprites/maze_bow_horn.png",
			"sprites/maze_bow_cross.png",
			"sprites/maze_bow_great.png",
			"sprites/maze_bow_spirit.png",
			"sprites/maze_sword_worn_shortsword.png",
			"sprites/maze_sword_shortsword.png",
			"sprites/maze_sword_rapier.png",
			"sprites/maze_sword_scimitar.png",
			"sprites/maze_sword_longsword.png",
			"sprites/maze_sword_katana.png",
			"sprites/maze_sword_runic_blade.png",
			"sprites/maze_arrow_item.png",
			"sprites/maze_arrow_shot.png",
			"sprites/maze_arrow_loaded.png",
			"sprites/maze_arrow_spirit.png"
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
