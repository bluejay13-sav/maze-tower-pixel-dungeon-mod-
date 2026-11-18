/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;

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

}
