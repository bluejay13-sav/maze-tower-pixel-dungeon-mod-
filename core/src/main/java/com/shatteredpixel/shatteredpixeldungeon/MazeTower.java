/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;

//Maze Tower 전용 설정과 수치를 한곳에 모아 둔다.
//수치는 모두 시작값이므로 여기서 바꾸면 된다.
public class MazeTower {

	// ===== 주인공 =====

	//선택할 수 있는 직업: 전사·마법사·도적·사냥꾼 (결투가·성직자는 선택 불가). 해금은 원작대로.
	public static boolean isPlayableClass( HeroClass cls ){
		return cls == HeroClass.WARRIOR || cls == HeroClass.MAGE
				|| cls == HeroClass.ROGUE || cls == HeroClass.HUNTRESS;
	}

	//원작 직업 특성(1~4층 특성 줄)을 쓸지 여부
	public static final boolean ORIGINAL_TALENTS = false;

	//원작 서브클래스(텐구의 가면)를 얻을 수 있는지 여부
	public static final boolean ORIGINAL_SUBCLASS = false;

	//원작 엔딩(아뮬렛을 주운 뒤 머무르기·귀환 길)을 쓸지. false면 아뮬렛을 주우면 그 자리에서 승리
	public static final boolean ORIGINAL_ENDING = false;

	//원작 갑옷 능력(왕의 왕관)을 얻을 수 있는지 여부
	public static final boolean ORIGINAL_ARMOR_ABILITY = false;

	//원작처럼 장착하지 않은 마법사의 지팡이로도 쏠 수 있게 할지 (마탑: 장착해야만 쏜다)
	public static final boolean ORIGINAL_STAFF_ZAP = false;

	//원작 전사의 부서진 인장(방어막)을 시작 장비로 줄지 여부
	public static final boolean WARRIOR_SEAL = true; //(결정) 직업 단계부터 다시 준다

	// ===== 마나 =====

	//최대 마나
	public static final int MANA_MAX = 100;

	//대기(휴식·탐색 포함)로 넘긴 한 턴마다 얻는 마나. 다른 행동으로는 얻지 못한다.
	public static final int MANA_PER_WAIT_TURN = 10;

	// ===== 검 계열 스킬 =====

	//모든 스킬의 마나 비용
	public static final int SKILL_MANA_COST = 30;

	//여러 번 때리는 스킬(연격, 반월베기)의 타격 사이 텀 (초, 화면 연출용)
	public static final float SKILL_HIT_INTERVAL = 0.24f;

	// ===== 마법사와 마법봉 =====

	//마법사 지팡이 스킬(마탄 발사, 합성하지 않은 지팡이)의 피해 = 지팡이 평타 피해의 몇 배 (1 = 100%)
	public static final float MAGE_BOLT_DMG = 1.5f; //23단계: 100% → 150%

	//지팡이 합성 스킬 (명세 16번). 피해는 모두 지팡이 평타 피해의 배율. 비용은 마나 30 + 지팡이 충전 1
	public static final float STAFF_EMPOWERED_BOLT_DMG = 2.0f; //마탄 합성: 강화 마탄
	public static final float STAFF_FIRE_LANCE_DMG     = 1.3f; //화염 폭발 합성: 화염 창
	public static final int   FIRE_LANCE_LENGTH        = 3;    //화염 창: 적 뒤쪽으로 불이 붙는 칸 수 (일직선)
	public static final int   FIRE_LANCE_FIRE          = 2;    //화염 창: 불 양 (원작 화염 폭발 충전 1회와 같음)
	public static final float STAFF_FROST_ARROW_DMG    = 1.3f; //서리 합성: 서리 화살
	public static final float FROSTBITE_TURNS          = 3f;   //혹한 지속
	public static final float STAFF_THUNDER_DMG        = 1.6f; //번개 합성: 낙뢰 (대상)
	public static final int   THUNDER_MAX_TARGETS      = 3;    //낙뢰: 대상 포함 최대 연쇄 수
	public static final int   THUNDER_CHAIN_RANGE      = 1;    //낙뢰: 대상 주변 칸
	public static final float THUNDER_SELF_DRY_MULTI   = 0.5f; //낙뢰가 주인공에게 번졌을 때, 젖지 않았으면 절반 (젖었으면 그대로)
	//지팡이 스킬은 마나(SKILL_MANA_COST, 절약 특성 적용) + 지팡이 충전 1을 쓴다
	//합성하지 않은 마법봉을 쓸 때 드는 마나 (모든 직업)
	public static final int LOOSE_WAND_MANA_COST = 15;

	// ===== 도적과 단검 =====

	//급소 찌르기 (낡은 단검): 평타 피해의 1.5배, 기습 상태(원작 surprisedBy)의 적에게는 2.3배
	public static final float SKILL_WORN_DAGGER_DMG = 1.5f;
	public static final float SKILL_WORN_DAGGER_SURPRISE_DMG = 2.3f;
	//도적의 공격 속도 배율 (1.3 = 30% 빠름, 공격에 드는 시간이 1/1.3)
	public static final float ROGUE_ATTACK_SPEED = 1.3f;

	// ===== 사냥꾼과 활 =====

	//낡은 단궁 (1티어): 근접 피해(인접한 적)와 원거리 피해를 따로 가진다. 강화 1당 증가량은 _SCALE
	public static final int BOW_MELEE_MIN = 1, BOW_MELEE_MIN_SCALE = 1;
	public static final int BOW_MELEE_MAX = 4, BOW_MELEE_MAX_SCALE = 1;
	public static final int BOW_RANGED_MIN = 2, BOW_RANGED_MIN_SCALE = 1;
	public static final int BOW_RANGED_MAX = 8, BOW_RANGED_MAX_SCALE = 2;
	//2칸 거리 원거리 평타의 명중 배율 (0.7 = -30%). 3칸 이상은 패널티 없음
	public static final float BOW_CLOSE_RANGE_ACCURACY = 0.7f;
	//조준 사격: 원거리 피해의 1배, 맞으면 1칸 밀쳐냄 (화살 1발)
	public static final float SKILL_AIMED_SHOT_DMG = 1.0f;
	//근접 평타·조준 사격이 밀쳐내는 거리
	public static final int BOW_KNOCKBACK = 1;
	//사냥꾼 시작 화살
	public static final int HUNTRESS_START_ARROWS = 100;
	//적에게 박힌 화살이 그 적이 죽을 때 떨어질 확률 (화살마다)
	public static final float ARROW_RECOVER_CHANCE = 0.3f;
	//투척 무기 분해: 1개당 (티어 × 이 값)발, 저주받은 것은 절반
	public static final int ARROWS_PER_THROWN_TIER = 5;

	// ===== 1차 특성 (발아). 배열은 [0레벨, 1레벨, 2레벨] =====

	//전사
	public static final int[]   T1_HEARTY_HEAL       = {0, 5, 8};      //푸짐한 식사: 체력 33% 이하일 때 먹으면 회복
	public static final float   T1_HEARTY_HP         = 0.334f;
	public static final int     T1_HEARTY_MANA       = 5;              //푸짐한 식사 2: 먹을 때마다 최대 마나
	public static final int     T1_MEAL_MANA_TIMES   = 5;              //식사로 최대 마나가 오르는 최대 횟수
	public static final float   T1_VETERAN_WEAPON_ID = 2f;             //베테랑의 직감: 무기 감정 속도
	public static final int[]   T1_PROVOKED_DMG      = {0, 3, 6};      //분노 자극
	public static final int[]   T1_IRON_WILL         = {0, 2, 5};      //강철의 의지: 인장 방어막 +
	public static final float   T1_GRIT_HP           = 0.2f;           //투지: 체력 20% 이하
	public static final int[]   T1_GRIT_SHIELD       = {0, 2, 5};
	//마법사
	public static final int[]   T1_EMPOWER_MANA      = {0, 10, 20};    //마력이 깃든 식사: 먹을 때마다 최대 마나
	public static final float   T1_SCHOLAR_WAND_ID   = 3f;             //학자의 직감
	public static final int[]   T1_LINGERING_DMG     = {0, 2, 5};      //잔류 마력
	public static final int[]   T1_BACKUP_SHIELD     = {0, 3, 6};      //예비 방어막
	public static final int[]   T1_MANA_CYCLE_TURNS  = {0, 2, 1};      //마나 순환: 가득 찬 채 대기할 턴
	//도적
	public static final int[]   T1_CACHED_RATIONS    = {0, 3, 5};      //은닉한 보존식
	public static final float   T1_THIEF_RING_ID     = 3f;             //도둑의 직감
	public static final int[]   T1_SUCKER_DMG        = {0, 3, 6};      //불시의 타격
	public static final int[]   T1_SHADOW_EVERY      = {0, 2, 1};      //보호의 그림자: 몇 턴마다
	public static final int[]   T1_SHADOW_AMOUNT     = {0, 2, 3};
	public static final int[]   T1_SHADOW_MAX        = {0, 6, 9};
	public static final float[] T1_SILENT_STEPS      = {0f, 0.2f, 0.4f}; //조용한 걸음: 잠든 적이 깰 확률 감소
	//사냥꾼
	public static final int[]   T1_BERRIES           = {0, 5, 8};      //자연의 은혜
	public static final float   T1_SURVIVAL_BOW_ID   = 3f;             //생존자의 직감
	public static final int[]   T1_BRAWL_DMG         = {0, 1, 3};      //격투술: 활 근접 평타
	public static final int[]   T1_NATURE_AID_BARK   = {0, 2, 4};      //자연의 도움: 나무껍질
	public static final int[]   T1_NATURE_AID_TIME   = {0, 3, 5};      //  나무껍질이 1씩 줄어드는 간격(턴)
	public static final int[]   T1_SENSES_SEARCH     = {0, 1, 2};      //숲의 감각: 탐색 범위 +
	public static final int     T1_SENSES_TRACK      = 4;              //숲의 감각 2: 이 거리 안의 가장 가까운 적 1마리를 늘 본다

	// ===== 연출 =====

	//그림자검: 평타가 끝난 뒤 잔상 일격까지의 텀 (초)
	public static final float SHADOW_BLADE_DELAY = 0.22f;
	//마나 반환 마탄이 날아오는 속도 (원작 마탄은 200)
	public static final float MANA_REFUND_BOLT_SPEED = 55f;

	//피해 배율은 그 무기 평타 피해에 대한 비율 (1.5 = 150%)
	public static final float SKILL_WORN_SHORTSWORD_DMG = 1.50f; //베기
	public static final float SKILL_SHORTSWORD_DMG      = 1.80f; //베기
	public static final float SKILL_RAPIER_DMG          = 1.60f; //찌르기, 반드시 명중
	public static final float SKILL_SCIMITAR_DMG        = 0.80f; //연격, 타격당
	public static final int   SKILL_SCIMITAR_HITS       = 3;
	public static final float SKILL_LONGSWORD_DMG       = 2.10f; //베기
	public static final float SKILL_KATANA_DMG          = 1.15f; //반월베기, 타격당
	public static final int   SKILL_KATANA_HITS         = 2;
	public static final float SKILL_KATANA_ACCURACY     = 1.30f; //명중 +30%
	public static final float SKILL_RUNIC_BLADE_DMG     = 1.90f; //무형검, 방어 무시
	public static final int   SKILL_RUNIC_BLADE_RANGE   = 3;     //무형검: 이 칸 수 안의 적에게 쓸 수 있다 (마탄이 닿아야 함)
	public static final float SKILL_RUNIC_BLADE_ELEMENT_CHANCE = 2f; //속성 특성 발동 확률 배수 (7단계 특성에서 사용)

	// ===== 2차 전직 (개화의 핵) =====

	//5층 보스(구)가 개화의 핵을 떨어뜨리는지
	public static final boolean GOO_DROPS_BLOOM_CORE = true;

	//2차 줄 칸 수 (핵 사용 1칸 + Lv7~11 5칸)
	public static final int BLOOM_SLOTS = 6;

	//2차 선택권·포인트를 주는 레벨 구간 (이 구간에서 레벨업마다 선택권 1, 포인트 1)
	public static final int BLOOM_FIRST_LEVEL = 7;
	public static final int BLOOM_LAST_LEVEL  = 11;

	//한 칸에서 보여 주는 선택지 수
	public static final int BLOOM_CHOICES_PER_SLOT = 3;

	//특성 최대 레벨 (0레벨로 해금, 포인트 1개당 1레벨)
	public static final int BLOOM_MAX_TALENT_LEVEL = 2;

	// ===== 2차 특성 수치 (배열은 [0레벨, 1레벨, 2레벨]) =====

	//기본공격
	public static final float[] T_PRECISION_ACC      = {0.20f, 0.40f, 0.60f};
	public static final int[]   T_COMBO_HITS         = {3, 3, 2};
	public static final float[] T_COMBO_BONUS        = {0.30f, 0.40f, 0.30f};
	public static final float[] T_SHADOW_BLADE_CHANCE= {0.10f, 0.15f, 0.20f};
	public static final float[] T_WEAK_POINT_DMG     = {0.10f, 0.20f, 0.30f};
	public static final int[]   T_LIFE_STEAL_HEAL    = {5, 10, 15};
	public static final float   T_FLOW_DMG           = 0.25f;
	public static final int[]   T_FLOW_ATTACKS       = {1, 1, 2};

	//스킬
	public static final float[] T_SHARPEN_DMG        = {0.10f, 0.20f, 0.20f};
	public static final float   T_SHARPEN_REFUND     = 0.50f; //2레벨, 처치 시 마나 반환
	public static final float[] T_CHAIN_PCT          = {0.20f, 0.40f, 0.40f};
	public static final int     T_CHAIN_RANGE        = 2;
	public static final float[] T_FOCUS_DMG          = {0.25f, 0.40f, 0.40f};
	public static final float   T_EXECUTE_HP         = 0.30f;
	public static final float[] T_EXECUTE_DMG        = {0.20f, 0.35f, 0.35f};
	public static final float   T_EXECUTE_REFUND     = 1.00f; //2레벨
	public static final int[]   T_ECHO_TURNS         = {2, 3, 3};
	public static final int[]   T_ECHO_MANA          = {3, 5, 8};
	public static final float[] T_PARRY_REDUCE       = {0.15f, 0.25f, 0.35f};

	//마나
	public static final int[]   T_MANA_DRAIN         = {1, 3, 5};
	public static final int[]   T_MANA_ATTUNE_MAX    = {20, 40, 40};
	public static final float   T_MANA_ATTUNE_FULL_DMG = 0.50f; //2레벨
	public static final float[] T_THRIFT_COST        = {0.10f, 0.20f, 0.30f};
	public static final int[]   T_MEDITATION_MULTI   = {2, 3, 4};
	public static final float[] T_OVERFLOW_DMG       = {0.10f, 0.20f, 0.30f};
	public static final float   T_MANA_VEIL_HP       = 0.50f;
	public static final int     T_MANA_VEIL_COST     = 20;
	public static final float[] T_MANA_VEIL_REDUCE   = {0.25f, 0.40f, 0.60f};

	//속성
	public static final float[] T_FLAME_BLADE_CHANCE = {0.10f, 0.20f, 0.30f};
	public static final float   T_FLAME_BLADE_TURNS  = 3f;
	public static final float[] T_FROST_SLASH_TURNS  = {2f, 3f, 4f};
	public static final float   T_FROST_SLASH_BONUS  = 0.20f; //2레벨, 냉각·빙결 적이 받는 피해
	public static final float[] T_SHOCK_CHANCE       = {0.15f, 0.25f, 0.25f};
	public static final float   T_SHOCK_DMG_FACTOR   = 0.50f; //그 타격 피해에 대한 전격 피해 (원작 전격 마법 부여와 같음)
	public static final float[] T_LINGERING_DURATION = {1.50f, 2.00f, 2.00f};
	public static final float   T_LINGERING_REACTION = 0.50f; //2레벨, 원소 반응 피해 (8단계)
	public static final float[] T_ELEMENT_BURST_DMG  = {0.30f, 0.50f, 0.70f};
	public static final float   T_AFFINITY_DURATION  = 0.50f;
	public static final float[] T_AFFINITY_REDUCE    = {0f, 0.20f, 0.40f};

	// ===== 속성과 원소 반응 =====

	//정전기: 전격 피해를 받으면 걸리는 턴 수
	public static final float STATIC_TURNS = 2f;
	//젖음: 물에서 나온 뒤 유지되는 턴 수
	public static final int WET_AFTER_TURNS = 2;
	//빙결: 젖은 적에게 냉기가 닿으면 걸리는 원작 Frost의 턴 수
	public static final float FREEZE_TURNS = 3f;

	//반응 기준 피해 = FLAT + PER_DEPTH × 층 (층이 깊을수록 커진다)
	public static final float REACTION_BASE_FLAT      = 2f;
	public static final float REACTION_BASE_PER_DEPTH = 0.5f;
	public static final float REACTION_STEAM_MULTI     = 1.5f; //증기: 화상 + 냉기. 남은 화상 피해 합계에 곱한다 (기준 피해 아님)
	public static final float REACTION_EXPLOSION_MULTI = 2.5f; //폭발: 정전기 + 화상, 인접 1칸
	public static final float REACTION_THAW_MULTI      = 2.0f; //해빙: 빙결 + 화염. 남은 화상 피해 합계에 곱한다 (기준 피해 아님)

	// ===== 신성한 영감의 물약 =====

	//한 번 마실 때 고른 줄에 주는 포인트
	public static final int DIVINE_POINTS_PER_POTION = 1;
	//줄마다 물약으로 얻을 수 있는 최대 포인트
	public static final int DIVINE_MAX_PER_ROW = 3;

	//1차 포인트를 주는 레벨 구간 (1차 능력은 미완성, 쌓이기만 한다)
	public static final int FIRST_TIER_FIRST_LEVEL = 2;
	public static final int FIRST_TIER_LAST_LEVEL  = 6;

	// ===== 3차 전직 (만개) =====

	//10층 보스(텐구)가 만개의 핵을 떨어뜨리는지
	public static final boolean TENGU_DROPS_FULLBLOOM_CORE = true;

	//만개의 핵으로 고를 수 있는 3차 전직 (지금은 전사 검투사 하나만)
	public static HeroSubClass[] fullBloomPaths( HeroClass cls ){
		if (cls == HeroClass.WARRIOR) return new HeroSubClass[]{HeroSubClass.GLADIATOR};
		return new HeroSubClass[0];
	}

	//3차 특성 최대 레벨 (0레벨 = 효과 없음, 1~4레벨)
	public static final int T3_MAX_TALENT_LEVEL = 4;

	// ===== 검투사 (배열은 [0레벨, 1레벨, 2레벨, 3레벨, 4레벨]) =====

	//야성: 적을 처치한 뒤 연속 타격이 유지되는 턴 (0레벨 = 원작 15턴)
	public static final float[] T3_FERAL_TIME = {15f, 25f, 35f, 45f, 60f};

	//향상된 콤보: 레벨별 효과가 켜지는 연속 타격 수
	public static final int T3_COMBO_CLOBBER = 6;   //1레벨: 밀쳐내기 3칸 + 혼란 + 낭떠러지
	public static final int T3_COMBO_PARRY   = 9;   //2레벨: 받아치기가 1턴 동안 모든 공격 반격
	public static final int T3_COMBO_FURY    = 12;  //3레벨: 분노 폭발이 적 방어력 일부 무시
	public static final int T3_COMBO_REFUND  = 15;  //4레벨: 초기화 필살기로 처치하면 절반 돌려받음
	public static final int   T3_CLOBBER_DIST      = 3;
	public static final float T3_CLOBBER_VERTIGO   = 3f;
	public static final float T3_FURY_ARMOR_IGNORE = 0.10f;

	//치명적인 방어: 필살기로 처치하면 인장 재사용 대기 감소(턴), 4레벨은 인장 방어막 + 최대 체력 비율
	public static final int[] T3_LETHAL_COOLDOWN  = {0, 40, 80, 120, 120};
	public static final float T3_LETHAL_SHIELD_HP = 0.10f;

	//기술 연계: 무기 스킬 적중 후 연속 타격 +, 4레벨은 필살기로 처치하면 마나 +
	public static final int[] T3_SKILL_LINK      = {0, 1, 2, 3, 3};
	public static final int   T3_SKILL_LINK_MANA = 45;

	//으깨기 범위 (대상 중심 칸 수. 원작은 주인공 중심 3칸 = 7×7)
	public static final int CRUSH_RADIUS = 2;

	// ===== 4차 전직 (결실) =====

	//20층 보스(드워프 왕)가 결실의 핵을 떨어뜨리는지 (왕의 왕관 대신)
	public static final boolean DWARF_KING_DROPS_FRUIT_CORE = true;

	//결실의 핵으로 고를 수 있는 4차 능력 (전사: 영웅의 도약 / 인내)
	public static com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] fruitPaths( HeroClass cls ){
		if (cls == HeroClass.WARRIOR){
			return new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[]{
					new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap(),
					new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure()};
		}
		return new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[0];
	}

	//4차 포인트: 결실의 핵을 쓸 때 + Lv21~25에 1씩
	public static final int T4_CORE_POINTS = 3;
	public static final int T4_FIRST_LEVEL = 21;
	public static final int T4_LAST_LEVEL  = 25;

	//특성 줄마다 레벨로 얻는 기본 포인트 수 (별 개수). 1차 Lv2~6, 3차 Lv13~20, 4차 핵 + Lv21~25
	public static int tierRegularPoints( int tier ){
		switch (tier){
			case 1: return FIRST_TIER_LAST_LEVEL - FIRST_TIER_FIRST_LEVEL + 1;
			case 3: return 8;
			case 4: return T4_CORE_POINTS + T4_LAST_LEVEL - T4_FIRST_LEVEL + 1;
			default: return 0;
		}
	}

	// ===== 4차 특성 (배열은 [0레벨, 1레벨, 2레벨, 3레벨, 4레벨], 높은 레벨이 아래 레벨을 대체) =====

	//영웅의 도약
	public static final int LEAP_MAX_DIST = 5;
	public static final int     T4_BODY_SLAM_FLAT  = 5;                              //몸통 박치기 기본 피해
	public static final float[] T4_BODY_SLAM_ARMOR = {0f, 0.50f, 0.75f, 1.00f, 1.50f}; //+ 방어력 비율
	public static final int     T4_IMPACT_PUSH     = 2;                              //충격파 밀침 칸
	public static final float[] T4_IMPACT_VULN     = {0f, 0.25f, 0.50f, 0.75f, 1.00f}; //취약 확률
	public static final float   T4_IMPACT_VULN_TURNS = 5f;
	public static final float[] T4_DOUBLE_JUMP     = {0f, 0.16f, 0.30f, 0.40f, 0.50f}; //3턴 안에 다시 도약하면 충전 감소
	public static final float   T4_DOUBLE_JUMP_TURNS = 3f;

	//인내
	public static final float[] T4_RETRIB_TOTAL = {1.00f, 1.20f, 1.50f, 1.80f, 2.10f}; //지속적인 응징: 추가 피해 총 배율
	public static final int[]   T4_RETRIB_HITS  = {1, 2, 3, 3, 3};                   //나눠 주는 횟수
	public static final float[] T4_SHRUG_REDUCE = {0.50f, 0.55f, 0.60f, 0.65f, 0.70f}; //무시하기: 피해 감소
	public static final float   T4_ENDURE_AFTER = 10f; //인내가 끝난 뒤 추가 피해를 줄 수 있는 턴
	public static final float[] T4_THORNS       = {0f, 0.06f, 0.09f, 0.12f, 0.15f};   //가시갑옷: 받은 피해 중 주변 적에게

	// ===== 두 갈래 엔딩과 역행 (명세 14번, 21번) =====

	//붕괴 제한: 층에 들어설 때마다 남은 턴이 새로 시작. 26층 150턴, 한 층 내려갈 때마다 4.4턴씩 줄어 1층 40턴
	public static final int   COLLAPSE_TURNS_TOP    = 150;
	public static final int   COLLAPSE_TURNS_BOTTOM = 40;
	public static int collapseTurns( int depth ){
		float step = (COLLAPSE_TURNS_TOP - COLLAPSE_TURNS_BOTTOM) / 25f;
		return Math.round(COLLAPSE_TURNS_TOP - (26 - depth) * step);
	}

	//낙석 (붕괴 턴이 0이 된 뒤): 처음 10턴마다 2개, 10턴마다 간격 1턴씩 짧아져 최소 4턴, 20턴마다 1개씩 늘어 최대 4개. 간격은 층 배율로 나눈다
	public static final float ROCK_START_INTERVAL = 10f;
	public static final float ROCK_SPEEDUP_EVERY  = 10f;
	public static final float ROCK_MIN_INTERVAL   = 4f;
	public static final int   ROCK_RANGE          = 3;   //주인공 주변 몇 칸 안에 떨어지는지
	public static final int   ROCK_COUNT_START    = 2;   //한 번에 떨어지는 돌 수 (처음)
	public static final int   ROCK_COUNT_MAX      = 4;   //최대
	public static final float ROCK_COUNT_UP_EVERY = 20f; //붕괴 뒤 이 턴마다 1개씩 늘어난다
	public static final float ROCK_WARN_TURNS     = 1f;  //경고 그림자가 먼저 보이는 턴
	//낙석 피해 = 맞은 대상 최대 체력의 25% + 30 (방어 무시: 방어력으로 줄지 않는다)
	public static final float ROCK_DMG_HP_PCT     = 0.25f;
	public static final int   ROCK_DMG_FLAT       = 30;
	//아래층일수록 낙석이 잦다: 빈도 배율 = 1 + (26 - 층) × 0.06 → 26층 1배, 1층 2.5배
	public static final float COLLAPSE_DEPTH_SCALE = 0.06f;
	public static float rockFrequency( int depth ){
		return 1f + (26 - depth) * COLLAPSE_DEPTH_SCALE;
	}

	//적 광폭화: 역행 중 모든 적. 공격력 +30%, 도망치지 않음
	public static final float BERSERK_DMG = 0.30f;

	//역행 중 층에 들어설 때 적을 다시 깐다: 그 층 원래 적 수(mobLimit)의 이 비율까지 (원작 승천보다 적게)
	public static final float RETREAT_MOB_FILL = 0.6f;

	//막힌 길: 돌무더기 (층마다 1~2개, 길을 완전히 막지 않는 칸에만)
	public static final int RUBBLE_HP  = 100; //회피 불가 (반드시 맞는다)
	public static final int RUBBLE_MIN = 1;
	public static final int RUBBLE_MAX = 2;

	//아뮬렛의 저주 (원작 부적의 저주를 바꿈): 느려짐(원작 6스택), 명중률 감소, 적 광폭화, 적을 죽이면 약해짐
	public static final float CURSE_ACC_PER_STACK  = 0.04f; //스택마다 명중률 감소
	public static final int   CURSE_ACC_MAX_STACKS = 8;     //→ 최대 32%
	public static final int   CURSE_SLOW_STACKS    = 6;     //이 스택부터 느려짐 (원작과 같음)

	//역행 맵 연출
	public static final float CRACK_FLOOR_PCT        = 0.08f;
	public static final float CRACK_WALL_PCT         = 0.12f;
	public static final float DUST_CHANCE            = 1f/20f; //매 턴 주인공 근처에 먼지
	public static final float FLOWER_FLOOR_BASE      = 0.02f;  //바닥 꽃 비율 = 2% + (26 - 층) × 1%
	public static final float FLOWER_FLOOR_PER_DEPTH = 0.01f;
	public static final float FLOWER_WALL_RATIO      = 0.5f;   //벽 꽃 비율 = 바닥 꽃 비율의 절반
	public static final float BLOOM_CHANCE           = 0.15f;  //주인공이 움직일 때 근처에서 꽃이 피어나는 확률


	// ===== 보스 (24단계, MazeBosses) =====

	//5층 썩은 수액 덩어리
	public static final float SAP_PHYS_REDUCE     = 0.80f; //물리 피해 감소 (횃불을 맞기 전, 체력 절반 위)
	public static final float SAP_HALF_PHYS_BONUS = 0.30f; //체력 절반 아래: 물리 피해를 더 받는다
	public static final int   SAP_PUDDLE_MIN      = 3;     //내려칠 때 수액 웅덩이 칸 수
	public static final int   SAP_PUDDLE_MAX      = 4;
	public static final int   SAP_PUDDLE_TURNS    = 30;    //수액 웅덩이 지속
	public static final int   SAP_MOVE_HEAL       = 10;    //수액 칸으로 옮기면 회복

	public static final int   TORCH_FIRE            = 4;   //횃불: 떨어진 자리에 붙는 불의 양

	//10층 그림자 간수
	public static final float TALISMAN_ROOT_TURNS   = 2f;  //봉인 부적: 맞은 대상 속박
	public static final int   WARDEN_SOULS          = 4;   //영혼 해방: 불러내는 죄수의 영혼 수
	public static final int   WARDEN_SOUL_RECAST_DEAD = 3; //이만큼 쓰러지면 다시 쓸 수 있다
	public static final int   CAGE_HP               = 30;  //부적 철창 체력
	public static final int   WARDEN_BEAMS          = 8;   //만개 광선 갈래 수 (8방향 전부)
	public static final int   WARDEN_BEAM_CHARGE    = 2;   //만개 광선 기 모으기 턴
	public static final int   WARDEN_BEAM_AREA      = 2;   //만개 광선: 간수 주변 이 칸 수 안도 모두 맞는다
	public static final int   WARDEN_LOST_TURNS     = 2;   //주인공을 이 턴 동안 못 찾으면 무작위로 돌아다닌다
	public static final int   WARDEN_IDLE_CD_MIN    = 5;   //순간이동 직후 몫을 다 쓴 뒤에도 이 턴~MAX턴마다 스킬을 쓴다 (순간이동 직후엔 원작처럼 더 자주)
	public static final int   WARDEN_IDLE_CD_MAX    = 8;
	public static final int   WARDEN_BEAM_MIN       = 5;   //만개 광선 피해 = MIN + 층 ~ MAX + 2×층 (원작 폭탄과 같음)
	public static final int   WARDEN_BEAM_MAX       = 10;

	//15층 마도 골렘 / 수정 거인
	public static final int   VEIN_HP              = 20;  //광맥 체력
	public static final int   VEIN_COUNT           = 15;  //마도 골렘 보스방에 무작위로 까는 광맥 수
	public static final int   VAPOR_TURNS          = 15;  //마력 증기 지속
	public static final int   VAPOR_MANA_DRAIN     = 10;  //마력 증기 안에서 턴당 마나 감소
	public static final float GOLEM_WET_STUN       = 5f;  //마도 골렘이 젖으면 기절
	public static final float GOLEM_STATIC_STUN    = 3f;  //마도 골렘의 정전기가 끝날 때 기절
	public static final int   FROST_CRYSTAL_TURNS  = 20;  //냉기 결정 지속
	public static final float FROST_CRYSTAL_CHILL  = 2f;  //결정 위에 서 있으면 냉기
	public static final int   CRYSTAL_MELT_TURNS   = 50;  //수정 거인: 불에 녹은 수정이 다시 자라기까지
	public static final int   CRYSTAL_WATER_TILES  = 14;  //수정 거인 보스방에 숨은 물 칸 수
	public static final int   CRYSTAL_FIRE_PYLONS  = 2;   //수정 거인: 수정 기둥을 이만큼 부수기 전까지 화상 면역
	public static final float CRYSTAL_MELT_DMG_TAKEN = 1.3f; //수정 거인: 수정이 녹은 동안 받는 피해 배율 (방어력 0과 함께)
	public static final float CRYSTAL_PILLAR_DMG_TAKEN = 0.3f; //수정 거인: 수정 기둥을 CRYSTAL_FIRE_PYLONS개 부수기 전까지 받는 피해 배율 (70% 감소)
	public static final float PROP_FADE_TIME       = 0.3f; //부적 철창·광맥·수정 기둥이 부서진 뒤 사라지는 시간(초, 원작 몬스터는 3초)

	// ===== 전직의 핵을 지닌 보스 몸 주위 벚꽃잎 (5층 희미하게 / 10층 조금 선명하게 / 20층 선명하게) =====
	public static final float[] BOSS_PETAL_ALPHA    = {0.35f, 0.6f, 0.9f};   //꽃잎 진하기
	public static final float[] BOSS_PETAL_INTERVAL = {0.35f, 0.22f, 0.13f}; //꽃잎이 나오는 간격(초)
	public static final float   BOSS_PETAL_MOVE_INTERVAL = 0.4f;           //움직이는 동안은 간격에 이 배율 (더 자주)

	// ===== 구역 외형 (28단계, 명세 20번) =====
	public static final int   SAP_FLOOR_PATCHES_PER_DEPTH = 1; //뿌리 지하 수액 웅덩이: 층 × 이만큼 묶음 (1층 1 ~ 4층 4)
	public static final int   SAP_FLOOR_PATCHES_MAX = 4;
	public static final int   SAP_FLOOR_SIZE_MIN    = 2;     //한 묶음의 칸 수
	public static final int   SAP_FLOOR_SIZE_MAX    = 4;
	public static final float SEAL_GRASS            = 0.12f; //봉인의 회랑 잔디 (원작 0.20, 뿌리 지하는 0.20 그대로)
	public static final int   HEART_FLOOR_DECO_DENOM = 55;   //탑의 심장 바닥 살점 (작을수록 많다, 원작 80)
	public static final float HEART_FLOOR_ALT2_CHANCE = 12f; //탑의 심장 바닥에 작은 혹·핏줄(FLOOR_ALT_2)이 섞이는 확률 % (원작 5)
	public static final int   REGION_FX_CHANCE      = 150;   //구역 분위기 이펙트: 주인공이 행동할 때마다 1/이 값

	// ===== 몬스터 전체 세기 =====
	//원작 몬스터 수치에 곱한다. 30단계 난이도 조정 때 구역별로 나눌 수 있다.
	public static final float MOB_HP_MULTI  = 0.8f;  //체력
	public static final float MOB_DMG_MULTI = 0.8f;  //근접 공격 피해
	public static final float MOB_ACC_MULTI = 0.9f;  //명중
	//보스 (BOSS 속성). 보스가 부른 몹·움직이지 않는 것(철창·광맥·기둥·돌무더기)은 원작 그대로
	public static final float BOSS_HP_MULTI  = 0.8f;
	public static final float BOSS_DMG_MULTI = 0.8f;
	public static final float BOSS_ACC_MULTI = 0.9f;

	//적 편 몬스터의 세기 배율: 보스면 bossMulti, 잡몹이면 mobMulti, 보스 부하·움직이지 않는 것·그 밖은 1
	public static float mobStatMulti( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch, float mobMulti, float bossMulti ){
		if (!(ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob)) return 1f;
		if (ch.alignment != com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY) return 1f;
		if (com.shatteredpixel.shatteredpixeldungeon.actors.Char.hasProp(ch, com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.BOSS)) return bossMulti;
		if (com.shatteredpixel.shatteredpixeldungeon.actors.Char.hasProp(ch, com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.BOSS_MINION)
				|| com.shatteredpixel.shatteredpixeldungeon.actors.Char.hasProp(ch, com.shatteredpixel.shatteredpixeldungeon.actors.Char.Property.IMMOVABLE)){
			return 1f;
		}
		return mobMulti;
	}

	// ===== 적에게 둘러싸였을 때 =====
	public static final int   CROWD_ENEMIES      = 3;   //주인공을 노리는 적이 이만큼 이상이면
	public static final float CROWD_ATTACK_SPEED = 3f;  //적의 공격 동작을 이 배율로 빠르게 (연출만, 턴 계산은 같다)

	//주인공을 노리는 적(쫓는 중, 주인공 시야 안)이 CROWD_ENEMIES 이상일 때 그 적들의 공격 동작 속도
	public static float crowdAttackSpeed( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ){
		if (!(ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob)
				|| ch.alignment != com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY
				|| Dungeon.hero == null || Dungeon.level == null){
			return 1f;
		}
		int n = 0;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0])){
			if (m.isAlive() && m.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY
					&& m.state == m.HUNTING && Dungeon.level.heroFOV[m.pos]){
				n++;
			}
		}
		return n >= CROWD_ENEMIES ? CROWD_ATTACK_SPEED : 1f;
	}

}
