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

	//원작 갑옷 능력(왕의 왕관)을 얻을 수 있는지 여부
	public static final boolean ORIGINAL_ARMOR_ABILITY = false;

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

	//마법사 지팡이 스킬(마탄 발사)의 피해 = 지팡이 평타 피해의 몇 배 (1 = 100%)
	public static final float MAGE_BOLT_DMG = 1.0f;
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

}
