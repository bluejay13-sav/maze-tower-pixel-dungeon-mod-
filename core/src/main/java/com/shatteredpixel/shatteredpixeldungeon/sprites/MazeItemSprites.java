/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

//Maze Tower: 원작 items.png에 없는 아이템 그림 (16×16 PNG 한 장씩).
//그림 파일은 core/src/main/assets/sprites/ 에 있다. 같은 이름으로 덮어쓰면 게임 그림이 바뀐다.
//ItemSprite.frame()이 이 번호를 보면 원작 아이템 그림 대신 이 파일을 그린다.
public class MazeItemSprites {

	//(사용자) 전직의 핵 세 개가 함께 쓰는 은은한 새잎빛 (BloomCore·FullBloomCore·FruitCore의 glowing())
	public static ItemSprite.Glowing coreGlow(){
		ItemSprite.Glowing g = new ItemSprite.Glowing( com.shatteredpixel.shatteredpixeldungeon.MazeTower.CORE_GLOW_COLOR, com.shatteredpixel.shatteredpixeldungeon.MazeTower.CORE_GLOW_PERIOD );
		g.mazeStrength = com.shatteredpixel.shatteredpixeldungeon.MazeTower.CORE_GLOW_STRENGTH;
		return g;
	}

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
	//(K8) 미스릴 파편: 15층 뿌리를 갉는 것이 떨어뜨림 (원본 docs/sprites/mobs/k_draft/make_mithril7.ps1 → make_mithril8.ps1, I1)
	public static final int MITHRIL_SHARD   = BASE + 24;
	//(K8) 마법이 부여된 미스릴 파편, 미스릴 무기 도안
	public static final int MITHRIL_SHARD_ENCHANTED = BASE + 25;
	public static final int MITHRIL_BLUEPRINT       = BASE + 26; //검 계열 도안
	public static final int MITHRIL_BLUEPRINT_BOW   = BASE + 27; //활 계열 도안
	//(M1) 시든 세계수 잎 브로치 (원본 docs/sprites/mobs/m_draft/make_m.ps1)
	public static final int SAGE_BROOCH     = BASE + 28;
	//(사용자) 갑옷 5종 새 그림 (원본 docs/sprites/armor_draft/make_armor2.ps1). 원작 번호(ItemSpriteSheet.ARMOR_*)를 remap이 바꿔 준다
	public static final int ARMOR_CLOTH     = BASE + 29;
	public static final int ARMOR_LEATHER   = BASE + 30;
	public static final int ARMOR_MAIL      = BASE + 31;
	public static final int ARMOR_SCALE     = BASE + 32;
	public static final int ARMOR_PLATE     = BASE + 33;
	//(W1~W4) 5티어 무기 새 그림 (원본 docs/sprites/weapons2/make_tier5.ps1, weapon_14b·15·16·17). 대검은 원작 특대검 번호를 remap이 바꿔 준다
	public static final int SWORD_GREAT     = BASE + 34; //대검 (B안 왕실 보라)
	public static final int SWORD_RESONANCE = BASE + 35; //공명검
	public static final int BOW_REINFORCED  = BASE + 36; //강화 각궁
	public static final int BOW_FANG        = BASE + 37; //은 송곳니

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
			"sprites/maze_arrow_spirit.png",
			"sprites/maze_mithril_shard.png",
			"sprites/maze_mithril_shard_enchanted.png",
			"sprites/maze_mithril_blueprint.png",
			"sprites/maze_mithril_blueprint_bow.png",
			"sprites/maze_sage_brooch.png",
			"sprites/maze_armor_cloth.png",
			"sprites/maze_armor_leather.png",
			"sprites/maze_armor_mail.png",
			"sprites/maze_armor_scale.png",
			"sprites/maze_armor_plate.png",
			"sprites/maze_sword_greatsword.png",
			"sprites/maze_sword_resonance.png",
			"sprites/maze_bow_reinforced_horn.png",
			"sprites/maze_bow_silver_fang.png"
	};

	//(사용자) 원작 그림 번호 중 마탑 그림으로 바꾸는 것 (갑옷 5종). 모든 곳(가방·바닥·도감 등)에서 바뀌게 ItemSprite.frame이 먼저 부른다
	public static int remap( int image ){
		if (image == ItemSpriteSheet.ARMOR_CLOTH)   return ARMOR_CLOTH;
		if (image == ItemSpriteSheet.ARMOR_LEATHER) return ARMOR_LEATHER;
		if (image == ItemSpriteSheet.ARMOR_MAIL)    return ARMOR_MAIL;
		if (image == ItemSpriteSheet.ARMOR_SCALE)   return ARMOR_SCALE;
		if (image == ItemSpriteSheet.ARMOR_PLATE)   return ARMOR_PLATE;
		if (image == ItemSpriteSheet.GREATSWORD)    return SWORD_GREAT; //(W1) 대검
		return image;
	}

	public static boolean has( int image ){
		return image >= BASE && image < BASE + FILES.length;
	}

	//여러 프레임을 이은 그림이면 첫 프레임만 쓴다 (가로 크기, 0이면 그림 전체)
	public static int frameWidth( int image ){
		if (image == TALISMAN) return 8;
		if (image == WARDEN_CHAIN) return 16;
		if (image == MITHRIL_SHARD || image == MITHRIL_SHARD_ENCHANTED) return 16;
		return 0;
	}

	//Maze Tower (K8): 여러 프레임을 차례로 보여 주는 그림 — 한 바퀴 시간(초), 0이면 첫 프레임만
	public static float animPeriod( int image ){
		if (image == MITHRIL_SHARD || image == MITHRIL_SHARD_ENCHANTED) return com.shatteredpixel.shatteredpixeldungeon.MazeTower.MITHRIL_BREATH_PERIOD;
		return 0;
	}

	public static String file( int image ){
		return FILES[image - BASE];
	}
}
