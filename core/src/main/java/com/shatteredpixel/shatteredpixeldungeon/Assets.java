/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon;

public class Assets {

	public static class Effects {
		public static final String EFFECTS      = "effects/effects.png";
		//Maze Tower: 2차 특성 이펙트 (휩쓸기·충격파·표식 등, 좌표는 docs/sprites/talent2/README.md)
		public static final String MAZE_FX2     = "sprites/maze_fx2_sheet.png";
		public static final String FIREBALL     = "effects/fireball.png";
		public static final String SPECKS       = "effects/specks.png";
		public static final String SPELL_ICONS  = "effects/spell_icons.png";
		public static final String TEXT_ICONS   = "effects/text_icons.png";
	}

	public static class Environment {
		//Maze Tower: 잔디·장식 겹침 그림. 탑의 심장 줄의 원작 버섯을 검은 잔디로 (역행 중에는 분홍 잔디). 원작: terrain_features.png
		public static final String TERRAIN_FEATURES = "environment/maze_terrain_features.png";
		public static final String TERRAIN_FEATURES_RETURN = "environment/maze_terrain_features_return.png";
		//Maze Tower: 키 큰 풀 윗부분 그림. 탑의 심장 줄의 원작 버섯을 검은 잔디로 (역행 중에는 분홍). 원작: raised_terrain.png
		public static final String RAISED_TERRAIN = "environment/maze_raised_terrain.png";
		public static final String RAISED_TERRAIN_RETURN = "environment/maze_raised_terrain_return.png";

		public static final String VISUAL_GRID          = "environment/visual_grid.png";
		public static final String WALL_BLOCKING        = "environment/wall_blocking.png";
		public static final String OCCLUSION_SHADOWS    = "environment/occlusion_shadows.png";

		//Maze Tower: 마탑 구역 그림 (명세 20번). 원작: tiles_sewers, tiles_prison, tiles_caves, tiles_city, tiles_halls
		public static final String TILES_SEWERS = "environment/maze_tiles_root.png";  //뿌리 지하
		public static final String TILES_PRISON = "environment/maze_tiles_seal.png";  //봉인의 회랑
		public static final String TILES_CAVES  = "environment/maze_tiles_vein.png";  //마력 광맥
		public static final String TILES_CITY   = "environment/maze_tiles_city2.png"; //잊힌 마도시
		//Maze Tower: 16~19층은 그 판의 20층 보스에 따라 (망령 왕 = 무너진 왕도, 여제 = 바랜 마도 대리석). 원본 docs/sprites/regions/make_city_variants4.ps1
		public static final String TILES_CITY_WRAITH  = "environment/maze_tiles_city_wraith.png";
		public static final String TILES_CITY_EMPRESS = "environment/maze_tiles_city_empress.png";
		public static final String TILES_HALLS  = "environment/maze_tiles_heart.png"; //탑의 심장
		public static final String TILES_HALLS_RETURN = "environment/maze_tiles_heart_return.png"; //탑의 심장 (역행 중)
		//Maze Tower (사용자): 21~24층 새 바닥·벽 (돌 판석과 살점 / 역행: 잿갈색 판석과 이끼). 원본 docs/sprites/regions/make_heart_assets.ps1
		public static final String TILES_HALLS2        = "environment/maze_tiles_heart2.png";
		public static final String TILES_HALLS2_RETURN = "environment/maze_tiles_heart2_return.png";
		public static final String HEART_FLOOR           = "environment/maze_heart_floor.png";
		public static final String HEART_FLOOR_RETURN    = "environment/maze_heart_floor_return.png";
		public static final String HEART_WALLFACE        = "environment/maze_heart_wallface.png";
		public static final String HEART_WALLFACE_RETURN = "environment/maze_heart_wallface_return.png";
		public static final String HEART_PULSE           = "environment/maze_heart_pulse.png";

		public static final String TILES_CAVES_CRYSTAL  = "environment/tiles_caves_crystal.png";
		public static final String TILES_CAVES_GNOLL    = "environment/tiles_caves_gnoll.png";

		//Maze Tower: 마탑 구역 물 (원작: water0~water4)
		public static final String WATER_SEWERS = "environment/maze_water_root.png";
		public static final String WATER_PRISON = "environment/maze_water_seal.png";
		public static final String WATER_CAVES  = "environment/maze_water_vein.png";
		public static final String WATER_CITY   = "environment/maze_water_city.png";
		//Maze Tower (사용자): 16~20층 물도 그 판의 20층 보스에 따라 (망령 왕 = 짙은 남빛, 여제 = 차분한 라벤더빛 잿빛). 원본 docs/sprites/regions/make_city_water.ps1
		public static final String WATER_CITY_WRAITH  = "environment/maze_water_city_wraith.png";
		public static final String WATER_CITY_EMPRESS = "environment/maze_water_city_empress.png";
		public static final String WATER_HALLS  = "environment/maze_water_heart.png";
		public static final String WATER_HALLS_RETURN = "environment/maze_water_heart_return.png";

		public static final String CARPET           = "environment/custom_tiles/carpet.png";
		public static final String RAT_KING_ROOM    = "environment/custom_tiles/rat_king_room.png";
		public static final String WEAK_FLOOR       = "environment/custom_tiles/weak_floor.png";
		public static final String SEWER_BOSS       = "environment/custom_tiles/sewer_boss.png";
		public static final String PRISON_QUEST     = "environment/custom_tiles/prison_quest.png";
		public static final String PRISON_EXIT      = "environment/custom_tiles/prison_exit.png";
		public static final String CAVES_QUEST      = "environment/custom_tiles/caves_quest.png";
		public static final String CAVES_BOSS       = "environment/custom_tiles/caves_boss.png";
		public static final String CITY_QUEST       = "environment/custom_tiles/city_quest.png";
		public static final String CITY_BOSS        = "environment/custom_tiles/city_boss.png";
		//Maze Tower: 마탑 특수 방 그림 (원작 그림과 칸 배치가 같다)
		public static final String MAZE_CITY_BOSS_WRAITH   = "environment/custom_tiles/maze_city_boss_wraith.png";
		public static final String MAZE_CITY_BOSS_EMPRESS  = "environment/custom_tiles/maze_city_boss_empress.png";
		public static final String MAZE_PRINCE_ROOM        = "environment/custom_tiles/maze_prince_room.png";
		public static final String MAZE_SAGE_ROOM          = "environment/custom_tiles/maze_sage_room.png"; //Maze Tower (사용자): 현자의 서재 가구
		public static final String MAZE_WELL_WATER         = "environment/maze_well_water.png"; //Maze Tower (사용자): 16~19층 망령 왕 판 우물 속 물
		public static final String HALLS_SP         = "environment/custom_tiles/halls_special.png";
		public static final String HALLS_SP_RETURN  = "environment/custom_tiles/maze_halls_special_return.png"; //Maze Tower (사용자): 역행 중 (26층·검은 씨앗 혹 방)
	}
	
	//TODO include other font assets here? Some are platform specific though...
	public static class Fonts {
		public static final String PIXELFONT= "fonts/pixel_font.png";
	}

	public static class Interfaces {
		public static final String ARCS_BG  = "interfaces/arcs1.png";
		public static final String ARCS_FG  = "interfaces/arcs2.png";

		public static final String BANNERS  = "interfaces/banners.png";
		public static final String BADGES   = "interfaces/badges.png";
		public static final String LOCKED   = "interfaces/locked_badge.png";

		public static final String CHROME   = "interfaces/chrome.png";
		public static final String ICONS    = "interfaces/icons.png";
		public static final String STATUS   = "interfaces/status_pane.png";
		public static final String MENU     = "interfaces/menu_pane.png";
		public static final String MENU_BTN = "interfaces/menu_button.png";
		public static final String TOOLBAR  = "interfaces/toolbar.png";
		public static final String SHADOW   = "interfaces/shadow.png";
		public static final String BOSSHP   = "interfaces/boss_hp.png";

		public static final String SURFACE  = "interfaces/surface.png";

		public static final String BUFFS_SMALL      = "interfaces/buffs.png";
		public static final String BUFFS_LARGE      = "interfaces/large_buffs.png";

		public static final String TALENT_ICONS     = "interfaces/talent_icons.png";
		//Maze Tower: 2차(개화) 특성 아이콘 31개 + 분홍 꽃잎 (16×16, 가로 8칸)
		public static final String MAZE_TALENT2_ICONS = "interfaces/maze_talent2_icons.png";
		//Maze Tower: 2차 특성 상태 아이콘 (7×7, 가로 8칸 간격)
		public static final String MAZE_BUFFS2_SMALL = "interfaces/maze_buffs2_small.png";
		//Maze Tower: 만개 각성한 2차 특성 칸을 감싸는 덩굴 (22×28, 특성 칸 20×26보다 사방 1칸 크게)
		public static final String MAZE_AWAKEN_VINE = "interfaces/maze_awaken_vine.png";
		public static final String TALENT_BUTTON    = "interfaces/talent_button.png";

		public static final String HERO_ICONS       = "interfaces/hero_icons.png";

		public static final String RADIAL_MENU      = "interfaces/radial_menu.png";

		public static final String CHANGE_ICONS    = "interfaces/change_icons.png";
	}

	//these points to resource bundles, not raw asset files
	public static class Messages {
		public static final String ACTORS   = "messages/actors/actors";
		public static final String ITEMS    = "messages/items/items";
		public static final String JOURNAL  = "messages/journal/journal";
		public static final String LEVELS   = "messages/levels/levels";
		public static final String MISC     = "messages/misc/misc";
		public static final String PLANTS   = "messages/plants/plants";
		public static final String SCENES   = "messages/scenes/scenes";
		public static final String UI       = "messages/ui/ui";
		public static final String WINDOWS  = "messages/windows/windows";
	}

	public static class Music {
		public static final String THEME_1              = "music/theme_1.ogg";
		public static final String THEME_2              = "music/theme_2.ogg";
		public static final String THEME_FINALE         = "music/theme_finale.ogg";

		public static final String SEWERS_1             = "music/sewers_1.ogg";
		public static final String SEWERS_2             = "music/sewers_2.ogg";
		public static final String SEWERS_3             = "music/sewers_3.ogg";
		public static final String SEWERS_TENSE         = "music/sewers_tense.ogg";
		public static final String SEWERS_BOSS          = "music/sewers_boss.ogg";

		public static final String PRISON_1             = "music/prison_1.ogg";
		public static final String PRISON_2             = "music/prison_2.ogg";
		public static final String PRISON_3             = "music/prison_3.ogg";
		public static final String PRISON_TENSE         = "music/prison_tense.ogg";
		public static final String PRISON_BOSS          = "music/prison_boss.ogg";

		public static final String CAVES_1              = "music/caves_1.ogg";
		public static final String CAVES_2              = "music/caves_2.ogg";
		public static final String CAVES_3              = "music/caves_3.ogg";
		public static final String CAVES_TENSE          = "music/caves_tense.ogg";
		public static final String CAVES_BOSS           = "music/caves_boss.ogg";
		public static final String CAVES_BOSS_FINALE    = "music/caves_boss_finale.ogg";

		public static final String CITY_1               = "music/city_1.ogg";
		public static final String CITY_2               = "music/city_2.ogg";
		public static final String CITY_3               = "music/city_3.ogg";
		public static final String CITY_TENSE           = "music/city_tense.ogg";
		public static final String CITY_BOSS            = "music/city_boss.ogg";
		public static final String CITY_BOSS_FINALE     = "music/city_boss_finale.ogg";

		public static final String HALLS_1              = "music/halls_1.ogg";
		public static final String HALLS_2              = "music/halls_2.ogg";
		public static final String HALLS_3              = "music/halls_3.ogg";
		public static final String HALLS_TENSE          = "music/halls_tense.ogg";
		public static final String HALLS_BOSS           = "music/halls_boss.ogg";
		public static final String HALLS_BOSS_FINALE    = "music/halls_boss_finale.ogg";
	}

	public static class Sounds {
		public static final String CLICK    = "sounds/click.mp3";
		public static final String BADGE    = "sounds/badge.mp3";
		public static final String GOLD     = "sounds/gold.mp3";

		public static final String OPEN     = "sounds/door_open.mp3";
		public static final String UNLOCK   = "sounds/unlock.mp3";
		public static final String ITEM     = "sounds/item.mp3";
		public static final String DEWDROP  = "sounds/dewdrop.mp3";
		public static final String STEP     = "sounds/step.mp3";
		public static final String WATER    = "sounds/water.mp3";
		public static final String GRASS    = "sounds/grass.mp3";
		public static final String TRAMPLE  = "sounds/trample.mp3";
		public static final String STURDY   = "sounds/sturdy.mp3";

		public static final String HIT              = "sounds/hit.mp3";
		public static final String MISS             = "sounds/miss.mp3";
		public static final String HIT_SLASH        = "sounds/hit_slash.mp3";
		public static final String HIT_STAB         = "sounds/hit_stab.mp3";
		public static final String HIT_CRUSH        = "sounds/hit_crush.mp3";
		public static final String HIT_MAGIC        = "sounds/hit_magic.mp3";
		public static final String HIT_STRONG       = "sounds/hit_strong.mp3";
		public static final String HIT_PARRY        = "sounds/hit_parry.mp3";
		public static final String HIT_ARROW        = "sounds/hit_arrow.mp3";
		public static final String ATK_SPIRITBOW    = "sounds/atk_spiritbow.mp3";
		public static final String ATK_CROSSBOW     = "sounds/atk_crossbow.mp3";
		public static final String HEALTH_WARN      = "sounds/health_warn.mp3";
		public static final String HEALTH_CRITICAL  = "sounds/health_critical.mp3";

		public static final String DESCEND  = "sounds/descend.mp3";
		public static final String EAT      = "sounds/eat.mp3";
		public static final String READ     = "sounds/read.mp3";
		public static final String LULLABY  = "sounds/lullaby.mp3";
		public static final String DRINK    = "sounds/drink.mp3";
		public static final String SHATTER  = "sounds/shatter.mp3";
		public static final String ZAP      = "sounds/zap.mp3";
		public static final String LIGHTNING= "sounds/lightning.mp3";
		public static final String LEVELUP  = "sounds/levelup.mp3";
		public static final String DEATH    = "sounds/death.mp3";
		public static final String CHALLENGE= "sounds/challenge.mp3";
		public static final String CURSED   = "sounds/cursed.mp3";
		public static final String TRAP     = "sounds/trap.mp3";
		public static final String EVOKE    = "sounds/evoke.mp3";
		public static final String TOMB     = "sounds/tomb.mp3";
		public static final String ALERT    = "sounds/alert.mp3";
		public static final String MELD     = "sounds/meld.mp3";
		public static final String BOSS     = "sounds/boss.mp3";
		public static final String BLAST    = "sounds/blast.mp3";
		public static final String PLANT    = "sounds/plant.mp3";
		public static final String RAY      = "sounds/ray.mp3";
		public static final String BEACON   = "sounds/beacon.mp3";
		public static final String TELEPORT = "sounds/teleport.mp3";
		public static final String CHARMS   = "sounds/charms.mp3";
		public static final String MASTERY  = "sounds/mastery.mp3";
		public static final String PUFF     = "sounds/puff.mp3";
		public static final String ROCKS    = "sounds/rocks.mp3";
		public static final String BURNING  = "sounds/burning.mp3";
		public static final String FALLING  = "sounds/falling.mp3";
		public static final String GHOST    = "sounds/ghost.mp3";
		public static final String SECRET   = "sounds/secret.mp3";
		public static final String BONES    = "sounds/bones.mp3";
		public static final String BEE      = "sounds/bee.mp3";
		public static final String DEGRADE  = "sounds/degrade.mp3";
		public static final String MIMIC    = "sounds/mimic.mp3";
		public static final String DEBUFF   = "sounds/debuff.mp3";
		public static final String CHARGEUP = "sounds/chargeup.mp3";
		public static final String GAS      = "sounds/gas.mp3";
		public static final String CHAINS   = "sounds/chains.mp3";
		public static final String SCAN     = "sounds/scan.mp3";
		public static final String SHEEP    = "sounds/sheep.mp3";
		public static final String MINE    = "sounds/mine.mp3";

		public static final String[] all = new String[]{
				CLICK, BADGE, GOLD,

				OPEN, UNLOCK, ITEM, DEWDROP, STEP, WATER, GRASS, TRAMPLE, STURDY,

				HIT, MISS, HIT_SLASH, HIT_STAB, HIT_CRUSH, HIT_MAGIC, HIT_STRONG, HIT_PARRY,
				HIT_ARROW, ATK_SPIRITBOW, ATK_CROSSBOW, HEALTH_WARN, HEALTH_CRITICAL,

				DESCEND, EAT, READ, LULLABY, DRINK, SHATTER, ZAP, LIGHTNING, LEVELUP, DEATH,
				CHALLENGE, CURSED, TRAP, EVOKE, TOMB, ALERT, MELD, BOSS, BLAST, PLANT, RAY, BEACON,
				TELEPORT, CHARMS, MASTERY, PUFF, ROCKS, BURNING, FALLING, GHOST, SECRET, BONES,
				BEE, DEGRADE, MIMIC, DEBUFF, CHARGEUP, GAS, CHAINS, SCAN, SHEEP, MINE
		};
	}

	public static class Splashes {
		public static final String WARRIOR  = "splashes/warrior.jpg";
		public static final String MAGE     = "splashes/mage.jpg";
		public static final String ROGUE    = "splashes/rogue.jpg";
		public static final String HUNTRESS = "splashes/huntress.jpg";
		public static final String DUELIST  = "splashes/duelist.jpg";
		public static final String CLERIC   = "splashes/cleric.jpg";

		public static final String SEWERS   = "splashes/sewers.jpg";
		public static final String PRISON   = "splashes/prison.jpg";
		public static final String CAVES    = "splashes/caves.jpg";
		public static final String CITY     = "splashes/city.jpg";
		public static final String HALLS    = "splashes/halls.jpg";

		public static class Title {
			public static final String ARCHS         = "splashes/title/archs.png";
			public static final String BACK_CLUSTERS = "splashes/title/back_clusters.png";
			public static final String MID_MIXED     = "splashes/title/mid_mixed.png";
			public static final String FRONT_SMALL   = "splashes/title/front_small.png";
		}
	}

	public static class Sprites {
		public static final String ITEMS        = "sprites/items.png";
		public static final String ITEM_ICONS   = "sprites/item_icons.png";

		public static final String WARRIOR  = "sprites/warrior.png";
		public static final String MAGE     = "sprites/mage.png";
		public static final String ROGUE    = "sprites/rogue.png";
		public static final String HUNTRESS = "sprites/huntress.png";
		public static final String DUELIST  = "sprites/duelist.png";
		public static final String CLERIC   = "sprites/cleric.png";
		public static final String AVATARS  = "sprites/avatars.png";
		public static final String PET      = "sprites/pet.png";
		public static final String AMULET   = "sprites/amulet.png";

		public static final String RAT      = "sprites/maze_rat.png"; //Maze Tower (F): 뿌리 지하 잡몹 외형 (원본 docs/sprites/mobs/f_draft/rat.png)
		public static final String BRUTE    = "sprites/maze_brute.png"; //Maze Tower (J4): 갱도에 갇힌 광부 (줄 0 광부, 1 결정 갑옷 광부, 2 광폭한 광부, 3 광폭한 결정 갑옷 광부, 원본 docs/sprites/mobs/j_draft/brute.png)
		public static final String SPINNER  = "sprites/maze_spinner.png"; //Maze Tower (J6): 수정 거미 (새로 그림, 원본 docs/sprites/mobs/j_draft/spinner.png ← make_spinner_new.ps1)
		public static final String DM300    = "sprites/dm300.png";
		public static final String WRAITH   = "sprites/maze_wraith.png"; //Maze Tower: 혼령(검은 수의에 푸른 넋빛 눈)·저주받은 영혼(핏빛 수의) 외형 (원본 docs/sprites/mobs/h_draft/wraith.png)
		public static final String UNDEAD   = "sprites/undead.png";
		public static final String KING     = "sprites/king.png";
		public static final String PIRANHA  = "sprites/piranha.png";
		public static final String EYE      = "sprites/maze_eye.png"; //Maze Tower (N3): 핏줄 눈
		public static final String GNOLL    = "sprites/maze_gnoll.png"; //Maze Tower (F): 뿌리 지하 잡몹 외형 (원본 docs/sprites/mobs/f_draft/gnoll.png)
		public static final String CRAB     = "sprites/maze_crab.png"; //Maze Tower (F): 뿌리 지하 잡몹 외형 (원본 docs/sprites/mobs/f_draft/crab.png)
		public static final String GOO      = "sprites/goo.png";
		public static final String SWARM    = "sprites/maze_swarm.png"; //Maze Tower (F): 뿌리 지하 잡몹 외형 (원본 docs/sprites/mobs/f_draft/swarm.png)
		public static final String SKELETON = "sprites/maze_skeleton.png"; //Maze Tower (H2): 묶인 유골 외형 (원본 docs/sprites/mobs/h_draft/skeleton.png, 둘째 줄 금고 변종은 원작)
		public static final String SHAMAN   = "sprites/maze_shaman.png"; //Maze Tower (J5): 마석 주술사 (줄 0~2 판초 색 붉은 약화·푸른 취약·보라 저주, 줄 3 금고 변종은 원작, 원본 docs/sprites/mobs/j_draft/shaman.png ← make_j5j6.ps1)
		public static final String THIEF    = "sprites/maze_thief.png"; //Maze Tower (H3): 탈옥수 외형 (원본 docs/sprites/mobs/h_draft/thief.png, 둘째 줄 노상강도는 원작)
		public static final String SOUL_MOTH = "sprites/maze_moth.png"; //Maze Tower (H4): 넋 나방 떼 (원본 docs/sprites/mobs/h_draft/moth.png, 칸 배치는 파리떼와 같음)
		public static final String TENGU    = "sprites/tengu.png";
		public static final String SHEEP    = "sprites/sheep.png";
		public static final String KEEPER   = "sprites/shopkeeper.png";
		public static final String BAT      = "sprites/maze_bat.png"; //Maze Tower (J3): 마석 박쥐 (줄 0~3 = 배에 든 마나, 원본 docs/sprites/mobs/j_draft/bat.png)
		public static final String ELEMENTAL= "sprites/maze_elemental.png"; //Maze Tower (L3): 풀려난 주문 (원작 칸 배치, 갓 태어난 불의 정령 줄은 원작)
		public static final String MONK     = "sprites/maze_monk.png"; //Maze Tower (L2): 왕실 근위(줄 0)·근위대장(줄 1), 새로 그림 18x18 x 15 (원본 docs/sprites/mobs/l_draft/make_l1l2.ps1)
		public static final String WARLOCK  = "sprites/maze_warlock.png"; //Maze Tower (L4): 궁정 마도사 (원작 칸 배치)
		public static final String MIRROR   = "sprites/maze_mirror.png"; //Maze Tower (L6): 거울 초상
		public static final String SAGE     = "sprites/maze_sage.png"; //Maze Tower (M1): 숨어 사는 현자
		public static final String SAGE_LEAF = "sprites/maze_sage_leaf.png"; //Maze Tower (M3): 망령 왕 머리 위 잎 (5×5 × 2: 빈 잎, 찬 잎)
		public static final String GOLEM    = "sprites/maze_golem.png"; //Maze Tower (L5): 수호 석상 (원작 칸 배치)
		public static final String STATUE   = "sprites/maze_statue.png"; //Maze Tower: (사용자) 석상도 새 갑옷 색 (원본 docs/sprites/mobs/p_draft/make_p2.ps1)
		public static final String SUCCUBUS = "sprites/maze_succubus.png"; //Maze Tower (N2): 검은 잎의 시녀 (원작 칸 배치)
		public static final String SUCCUBUS_CHARM_F = "sprites/maze_succubus_charm_f.png"; //Maze Tower (N2): 전사·도적이 매혹된 동안 보이는 여자 외형 (같은 크기)
		public static final String SUCCUBUS_CHARM_M = "sprites/maze_succubus_charm_m.png"; //Maze Tower (N2): 사냥꾼·마법사가 매혹된 동안 보이는 남자 외형 (같은 크기)
		public static final String SCORPIO  = "sprites/scorpio.png";
		public static final String FISTS    = "sprites/yog_fists.png";
		public static final String YOG      = "sprites/yog.png";
		public static final String LARVA    = "sprites/larva.png";
		public static final String GHOST    = "sprites/ghost.png";
		public static final String MAKER    = "sprites/wandmaker.png";
		public static final String TROLL    = "sprites/blacksmith.png";
		public static final String IMP      = "sprites/imp.png";
		public static final String RATKING  = "sprites/ratking.png";
		public static final String BEE      = "sprites/maze_bee.png"; //Maze Tower: (사용자) 수액벌 (원본 docs/sprites/mobs/p_draft/make_p_draft.ps1)
		public static final String MIMIC    = "sprites/mimic.png";
		public static final String ROT_LASH = "sprites/maze_rot_lasher.png"; //Maze Tower: (사용자) 검은 가시 덩굴
		public static final String ROT_HEART= "sprites/maze_rot_heart.png"; //Maze Tower: (사용자) 검은 씨방
		public static final String GUARD    = "sprites/maze_guard.png"; //Maze Tower (H5): 사슬 옥졸 외형 (원본 docs/sprites/mobs/h_draft/guard.png, 공격 3장을 6장으로 늘림)
		public static final String WARDS    = "sprites/wards.png";
		public static final String GUARDIAN = "sprites/guardian.png";
		public static final String SLIME    = "sprites/maze_slime.png"; //Maze Tower (F): 뿌리 지하 잡몹 외형 (원본 docs/sprites/mobs/f_draft/slime.png)
		public static final String SNAKE    = "sprites/maze_snake.png"; //Maze Tower (F): 뿌리 지하 잡몹 외형 (원본 docs/sprites/mobs/f_draft/snake.png)
		public static final String NECRO    = "sprites/maze_necromancer.png"; //Maze Tower (H5): 봉인술사 외형 (원본 docs/sprites/mobs/h_draft/necromancer_new.png, 둘째 줄 영혼 술사는 원작)
		public static final String SOUL       = "sprites/maze_soul.png"; //Maze Tower (H6): 갇힌 영혼·10층 죄수의 영혼 (원본 docs/sprites/mobs/h_draft/soul.png)
		public static final String SEAL_CAGE  = "sprites/maze_sealcage.png"; //Maze Tower (I1): 10층 2단계 부적 철창 16×22 × 2 (원본 docs/sprites/boss10/make_sealcage.ps1)
		public static final String CELL_DOOR  = "sprites/maze_celldoor.png"; //Maze Tower (I1): 10층 감방 문 (원본 docs/sprites/boss10/celldoor.png)
		public static final String CHAIN_RING = "sprites/maze_chainring.png"; //Maze Tower (H6): 사슬 고리 (원본 docs/sprites/mobs/h_draft/chainring.png)
		public static final String TALISMAN_SPIRIT = "sprites/maze_talisman_spirit.png"; //Maze Tower (H7): 부적 넋 (원본 docs/sprites/mobs/h_draft/talisman_spirit.png, 줄 0~4 = 부적 4~0장)
		public static final String GHOUL    = "sprites/maze_ghoul.png"; //Maze Tower (L1): 짝 잃은 시민 (원작 구울을 푸른빛 망자로 다시 칠함, 칸 배치 같음, 원본 make_l1l2.ps1)
		public static final String RIPPER   = "sprites/maze_ripper.png"; //Maze Tower (N5): 검은 싹
		public static final String SPAWNER  = "sprites/maze_spawner.png"; //Maze Tower (N5): 검은 씨앗 혹
		public static final String TANGLED  = "sprites/maze_tangled.png"; //Maze Tower (N6): 얽힌 것들 (줄 0~2 = 세 모양)
		public static final String DM100    = "sprites/maze_dm100.png"; //Maze Tower (H1): 봉인 등롱 외형 (원본 docs/sprites/mobs/h_draft/lantern.png, 둘째 줄 금고 변종은 원작)
		public static final String PYLON    = "sprites/pylon.png";
		public static final String DM200    = "sprites/maze_dm200.png"; //Maze Tower (J7): 갉힌 채굴 기계 (첫 줄 새로 그림, 둘째 줄 박힌 채굴 기계·금고 변종은 원작, 원본 docs/sprites/mobs/j_draft/dm200.png ← make_dm200_new.ps1)
		public static final String LOTUS    = "sprites/lotus.png";
		public static final String NINJA_LOG        = "sprites/ninja_log.png";
		public static final String SPIRIT_HAWK      = "sprites/spirit_hawk.png";
		public static final String SENTRY           = "sprites/sentry.png";
		public static final String CRYSTAL_WISP     = "sprites/crystal_wisp.png";
		public static final String CRYSTAL_GUARDIAN = "sprites/crystal_guardian.png";
		public static final String CRYSTAL_SPIRE    = "sprites/crystal_spire.png";
		public static final String GNOLL_GUARD      = "sprites/gnoll_guard.png";
		public static final String GNOLL_SAPPER     = "sprites/gnoll_sapper.png";
		public static final String GNOLL_GEOMANCER  = "sprites/gnoll_geomancer.png";
		public static final String FUNGAL_SPINNER   = "sprites/fungal_spinner.png";
		public static final String FUNGAL_SENTRY    = "sprites/fungal_sentry.png";
		public static final String FUNGAL_CORE      = "sprites/fungal_core.png";
		public static final String VAULT_TOKENS_DOOR= "sprites/vault_tokens_door.png";
		public static final String VAULT_MIRROR     = "sprites/vault_mirror.png";
		public static final String VAULT_BOSS_ELEMENTAL= "sprites/vault_boss_elemental.png";
	}
}
