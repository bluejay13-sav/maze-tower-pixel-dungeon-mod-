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

	//보스전 체험판 빌드 (MazeTrial): true면 시작 화면 "플레이"가 보스 고르기 창이 되고, 고른 보스층 바로 앞까지 건너뛰어 시작한다.
	//본편(main 브랜치)은 false. 체험판 브랜치(boss-trial)에서만 true로 둔다
	public static final boolean BOSS_TRIAL = false;
	//(사용자) 테스트 버전 빌드: 본판 그대로 1층부터 하되, 배포판에서도 게임 메뉴에 "층 이동"·"아이템 꺼내기" 치트가 보이고
	//마법사·도적을 고르면 2·3차 전직이 아직 준비 중이라고 알린다. 본편(main)은 false, 체험판 브랜치(boss-trial)에서만 true
	public static final boolean TEST_BUILD = true;
	//보스전 체험판에서 19층으로 건너뛸 때 주는 물약·주문서 수 (종류마다), 강화의 주문서 수 (MazeTrial.kit)
	public static final int TRIAL_POTIONS = 5;
	public static final int TRIAL_SCROLLS = 5;
	public static final int TRIAL_UPGRADES = 12;
	//"아이템 꺼내기" 치트 창(WndTrialItems)의 강화·수량 최대 (본판 디버그·체험판)
	public static final int TRIAL_CHEAT_MAX_UPGRADE = 20;
	public static final int TRIAL_CHEAT_MAX_QTY = 99;
	//배포용: 크레딧 화면(AboutScene) 맨 아래 미궁의 마탑 칸. 제작자 이름과 저장소 주소 (비워 두면 그 줄을 숨긴다)
	public static final String MOD_AUTHOR = "bluejay13-sav";
	public static final String MOD_LINK_TEXT = "github.com/bluejay13-sav/...";
	public static final String MOD_LINK = "https://github.com/bluejay13-sav/maze-tower-pixel-dungeon-mod-";
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

	//(A1, 2026-11-24) 턴이 흐를 때마다 얻는 마나. 이동·평타·대기·아이템·마법봉 모두 같다.
	//마나를 써서 공격한 턴(공격형 스킬, 장전·집중, 지팡이 스킬, 반지 스킬)에만 차지 않는다.
	public static final int A1_MANA_PER_TURN = 1; //(결정) 턴마다 1
	//(A1) 대기(휴식·탐색)한 턴에 더 주는 마나. 지금은 덤 없음.
	public static final int A1_MANA_WAIT_BONUS = 0;

	// ===== 검 계열 스킬 =====

	//모든 스킬의 마나 비용
	public static final int SKILL_MANA_COST = 30;

	//여러 번 때리는 스킬(연격, 반월베기)의 타격 사이 텀 (초, 화면 연출용)
	public static final float SKILL_HIT_INTERVAL = 0.24f;
	//(결정, 2026-11-21) 활 속사(각궁)의 화살 사이 텀 (앞 화살이 닿은 뒤 다음 화살을 쏘기까지, 예전엔 위 0.24초)
	public static final float SKILL_RAPID_INTERVAL = 0.05f;

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

	//활 7종: 근접 피해(인접한 적)와 원거리 피해를 따로 가진다. 배열은 [티어] (0은 안 씀)
	//  1티어 단궁 / 2티어 탄궁·장궁 / 3티어 각궁·석궁 / 4티어 정량궁·정령의 활 / 5티어 강화 각궁·은 송곳니(W3·W4). 힘 요구치는 원작 공식(티어)이라 검과 같다
	//  강화 1당 증가량: 근접 +1/+1, 원거리 +1/+BOW_RANGED_MAX_SCALE[티어]
	public static final int[] BOW_MELEE_MIN  = {0, 1, 2, 3, 4, 5}; //(W3·W4) 5티어는 같은 간격으로 이어 붙임 (임시값)
	public static final int[] BOW_MELEE_MAX  = {0, 4, 7, 10, 13, 16};
	public static final int[] BOW_RANGED_MIN = {0, 1, 2, 3, 4, 5}; //(결정, 2026-11-21 밸런스) 2,3,4,5 → 1,2,3,4
	public static final int[] BOW_RANGED_MAX = {0, 7, 10, 14, 17, 21}; //(결정, 밸런스) 8,12,16,20 → 7,10,14,17 (원거리 평타 ≈ 같은 힘 검의 75%)
	public static final int[] BOW_RANGED_MAX_SCALE = {0, 1, 2, 3, 4, 5}; //(결정, 밸런스) 2,3,4,5 → 1,2,3,4
	//2칸 거리 원거리 평타의 명중 배율 (0.7 = -30%). 3칸 이상은 패널티 없음
	public static final float BOW_CLOSE_RANGE_ACCURACY = 0.7f;
	//활 스킬 (원거리 피해 배율, 모두 마나 SKILL_MANA_COST)
	//조준 사격: 단궁 100% / 탄궁 120% / 장궁 140%, 명중 +30%, 밀쳐내기 없음
	public static final float SKILL_AIMED_SHOT_DMG = 1.0f;
	public static final float SKILL_PELLET_AIMED_DMG = 1.2f;
	public static final float SKILL_LONG_AIMED_DMG = 1.4f;
	public static final float SKILL_AIMED_ACCURACY = 1.3f;
	//속사 (각궁): 85% 화살 두 발 (화살 2개)
	public static final int   SKILL_RAPID_HITS = 2;
	public static final float SKILL_RAPID_DMG = 0.85f; //(결정, 밸런스) 0.8 → 0.85
	public static final float SKILL_REINFORCED_RAPID_DMG = 1.0f; //(W3, 사용자) 강화 각궁의 속사: 한 발당 100% (각궁은 SKILL_RAPID_DMG 그대로)
	//(W4) 숨통 물기 (은 송곳니): 화살 1발 185%((사용자) 지시서 170%에서 바꿈), 명중 보정 없음. 맞는 순간 대상 체력이 최대의 절반보다 낮으면 방어 무시 (정확히 절반이면 아님)
	public static final float SKILL_THROAT_DMG = 1.85f;
	//장전 (석궁): 1턴, 다음 원거리 평타 = 평타 100% + 110% = 210%, 명중 +30%
	public static final float SKILL_LOAD_DMG = 2.1f; //(결정, 밸런스) 2.3 → 2.1 (평타 100% + 110%)
	public static final float SKILL_LOAD_ACCURACY = 1.3f;
	//집중 (정량궁): 1턴, 다음 원거리 평타 = 평타 100% + 130% = 230%, 명중 +50%
	public static final float SKILL_FOCUS_DMG = 2.3f; //(사용자) 2.4 → 2.3 (평타 100% + 130%)
	public static final float SKILL_FOCUS_ACCURACY = 1.5f;
	//꿰뚫기 (정령의 활): 벽을 관통하는 화살 200% (보이는 적이면 길이 막혀도 맞힌다)
	public static final float SKILL_PIERCE_DMG = 2.0f; //(결정, 밸런스) 1.8 → 2.0
	//활 근접 평타가 밀쳐내는 거리
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
	//(W1) 5티어 대검 (원작 특대검 Greatsword, 5티어 표준 피해): 내려찍기 = 인접한 적 하나에게 평타의 240% (임시값)
	public static final float SKILL_GREATSWORD_DMG      = 2.40f;
	//(W2) 5티어 공명검 (ResonanceSword): 평타는 5티어 표준의 85%, 방출 = 남은 마나를 전부 써서 200% + (쓴 마나 − 30) × 2.5% (임시값)
	public static final float RESONANCE_SWORD_DMG       = 0.85f; //평타 피해 배율 (5티어 표준 대비)
	public static final float SKILL_DISCHARGE_BASE      = 2.00f; //마나 30을 쓸 때 (최소치, 섬광 보법 각성으로 이어 쓸 때·거울 초상도 이 값)
	public static final float SKILL_DISCHARGE_PER_MANA  = 0.025f; //30을 넘는 마나 1마다 (끊기지 않음)
	public static final float SKILL_DISCHARGE_REFUND    = 0.40f; //방출의 본 타격으로 쓰러뜨리면 쓴 마나(형태 특성 추가분 포함)의 40%를 돌려받음 (소수점 버림, 한 번)
	public static final int   SKILL_RUNIC_BLADE_RANGE   = 3;     //무형검: 이 칸 수 안의 적에게 쓸 수 있다 (마탄이 닿아야 함)
	public static final float SKILL_RUNIC_BLADE_ELEMENT_CHANCE = 2f; //속성 특성 발동 확률 배수 (7단계 특성에서 사용)

	// ===== 2차 전직 (개화의 핵) =====

	//5층 보스(구)가 개화의 핵을 떨어뜨리는지
	public static final boolean GOO_DROPS_BLOOM_CORE = true;

	//2차 줄 칸 수 (핵 사용 1칸 + Lv7~10 4칸)
	public static final int BLOOM_SLOTS = 5;

	//2차 선택권을 주는 레벨 구간 (이 구간에서 레벨업마다 선택권 1)
	public static final int BLOOM_FIRST_LEVEL       = 7;
	public static final int BLOOM_CHOICE_LAST_LEVEL = 10;
	//2차 포인트를 주는 레벨 구간 (BLOOM_FIRST_LEVEL ~ 이 레벨, 레벨업마다 포인트 1)
	public static final int BLOOM_LAST_LEVEL        = 12;
	//개화의 핵을 쓸 때 바로 받는 포인트
	public static final int BLOOM_CORE_POINTS = 1;

	//한 칸에서 보여 주는 선택지 수 (첫 번째 계열이 서로 다른 것으로 뽑는다)
	public static final int BLOOM_CHOICES_PER_SLOT = 3;

	//특성 최대 레벨 (0레벨 = 효과 없음으로 해금, 포인트 1개당 1레벨)
	public static final int BLOOM_MAX_TALENT_LEVEL = 3;

	//만개의 핵을 쓰면 받는 분홍 꽃잎 (각성) 수
	public static final int BLOOM_PETALS = 2;
	//각성할 수 있는 최소 특성 레벨 (0레벨 특성은 각성할 수 없다)
	public static final int BLOOM_AWAKEN_MIN_LEVEL = 1;

	// ===== 2차 특성 수치 (모두 임시값. 배열은 [1레벨, 2레벨, 3레벨]) =====

	//무한 연계 방지
	public static final float B2_MANA_CAP_PER_ACTION = 0.25f; //특성으로 얻는 마나: 한 행동에 최대 마나의 이 비율까지
	public static final int   B2_MULTIHIT_SKILL_CAP  = 3;     //여러 번 때리는 스킬의 "스킬 적중 시" 효과는 스킬당 이 횟수까지

	// 테스트용 허수아비 (디버그 빌드에서만 1층 시작 지점에, TrainingDummy)
	public static final int DUMMY_COUNT       = 3;
	public static final int DUMMY_HP          = 80;
	public static final int DUMMY_REGEN_TURNS = 5;  //맞지 않고 이 턴이 지나면 체력이 다시 가득

	// (C1) 받아치기: 적의 공격으로 피가 줄지 않으면 직후의 평타 피해 + (1/2/3레벨)
	public static final float[] C1_RIPOSTE_DMG       = {0.30f, 0.45f, 0.60f};
	public static final float   C1_RIPOSTE_FREE_CD   = 3f;    //각성: 턴을 쓰지 않는 받아치기 사이의 간격 (턴)

	// (C2) 허점 찌르기: 움직이지 못하는 적(기절·속박·빙결)과 나를 아직 보지 못한 적에게 주는 피해 + (1/2/3레벨)
	public static final float[] C2_OPENING_DMG          = {0.20f, 0.35f, 0.50f};
	public static final float   C2_OPENING_SPREAD_TURNS = 2f;  //각성: 옮겨 가는 상태의 지속 (턴)
	public static final int     C2_OPENING_SPREAD_RANGE = 4;   //각성: 쓰러진 적에서 이 칸 안의 가장 가까운 적에게

	// (C3) 벼림: 공격하지 않고 보낸 턴마다 다음 공격 피해 + (1/2/3레벨), C3_HONE_MAX_TURNS턴까지 (상한 25/35/50%)
	public static final float[] C3_HONE_PER_TURN   = {0.05f, 0.07f, 0.10f};
	public static final int     C3_HONE_MAX_TURNS  = 5;

	// (C4) 번지는 반응: 내 공격이 일으킨 반응(증기·해빙·폭발)의 피해 × 이 비율을 주변 1칸의 적에게 (1/2/3레벨)
	public static final float[] C4_SPREAD_RATIO     = {0.30f, 0.45f, 0.60f};
	public static final float   C4_AWAKE_BURN       = 3f;   //각성: 번진 적에게 묻는 화상 지속 (턴)
	public static final float   C4_AWAKE_CHILL      = 3f;   //각성: 냉기 지속 (턴). 정전기는 원작 STATIC_TURNS

	// (C5) 메아리: 공격형 스킬을 쓴 다음 턴에 푸른 잔상이 같은 적에게 그 스킬을 한 번 더 (덧타격, 본 타격 배율 × 비율)
	public static final float[] C5_ECHO_RATIO       = {0.30f, 0.40f, 0.50f};

	// (C6) 한 번 더: 공격형 스킬(강화된 평타 포함) 타수 +1, 피해는 늘어난 타수에 고르게 나누고 합이 이만큼 는다 (1/2/3레벨)
	public static final float[] C6_ONE_MORE_TOTAL   = {0.10f, 0.15f, 0.20f};

	// (C7) 무거운 일격: 스킬의 마지막 타가 적을 1칸(각성 2칸) 밀친다. 벽·적에 부딪히면 그 타격 피해 × 이 비율 (1/2/3레벨)
	public static final float[] C7_HEAVY_COLLIDE    = {0.30f, 0.45f, 0.60f};
	public static final int     C7_HEAVY_PUSH       = 1;
	public static final int     C7_HEAVY_PUSH_AWAKE = 2;

	// (C8) 달군 날: 스킬에 맞은 적에게 화상. 지속(턴)과 화상 피해 배율 (1/2/3레벨)
	public static final float[] C8_BURN_TURNS       = {3f, 4f, 5f};
	public static final float[] C8_BURN_DMG         = {1.00f, 1.25f, 1.50f};
	public static final int     C8_AWAKE_FIRE       = 2;    //각성: 발밑에 남는 불(원작 Fire)의 양. 작을수록 빨리 꺼진다

	// (C9) 숨 고르기: 공격하지도 맞지도 않고 보낸 턴마다 방어막 (최대 체력 비율, 1/2/3레벨)
	public static final float[] C9_BREATHER_PER_TURN = {0.02f, 0.03f, 0.04f};
	public static final float[] C9_BREATHER_CAP      = {0.10f, 0.15f, 0.20f};

	// (C10) 마지막 겹: 방어막이 깨진 공격의 넘친 피해를 받지 않고, 깨진 뒤 1턴 동안 받는 피해 - (1/2/3레벨)
	public static final float[] C10_LAYER_REDUCE    = {0.20f, 0.30f, 0.40f};
	public static final float   C10_LAYER_TURNS     = 1f;
	public static final int     C10_AWAKE_PUSH      = 1;    //각성: 주변 적을 미는 칸 수

	// (C11) 일대일: 옆 칸의 적이 하나뿐이면 받는 피해 - (1/2/3레벨)
	public static final float[] C11_DUEL_REDUCE     = {0.10f, 0.15f, 0.20f};

	// (C12) 버티는 숨: 쓰러질 피해를 받아도 체력 1로 버틴다 (긴 쿨타임). 버틴 뒤 방어막 = 최대 체력 × (1/2/3레벨)
	public static final float   C12_HOLD_COOLDOWN   = 150f;
	public static final float[] C12_HOLD_SHIELD     = {0.10f, 0.20f, 0.30f};
	public static final float   C12_AWAKE_TURNS     = 1f;   //각성: 버틴 뒤 어떤 피해도 받지 않는 턴

	// (C13) 넘치는 잔: 가득 찬 뒤 넘치는 마나 × 비율 → 방어막 (상한 = 최대 체력 × , 1/2/3레벨)
	public static final float[] C13_CUP_RATIO       = {0.20f, 0.30f, 0.40f};
	public static final float[] C13_CUP_CAP         = {0.10f, 0.15f, 0.20f};
	public static final float   C13_AWAKE_PER_MANA  = 0.005f; //각성: 방어막도 가득이면 넘친 마나 1당 다음 스킬 피해 +
	public static final int     C13_AWAKE_MAX_MANA  = 60;     //각성: 쌓을 수 있는 마나 (60 × 0.5% = +30%)

	// (C14) 마찰: 평타가 맞을 때마다 가진 마법봉 모두의 충전 + (1/2/3레벨)
	public static final float[] C14_FRICTION_CHARGE = {0.05f, 0.08f, 0.12f};
	public static final int     C14_AWAKE_MANA      = 5;    //(결정) 각성: 마법봉이 모두 가득 찬 뒤 평타가 맞을 때마다 마나

	// (C15) 가벼운 발: 해로운 바닥에 들어선 첫 턴은 효과 없음, 그 뒤 그 바닥에서 받는 피해 - (1/2/3레벨)
	public static final float[] C15_FLOOR_REDUCE    = {0.20f, 0.30f, 0.40f};
	public static final float   C15_GUARD_TURNS     = 1f;
	public static final float   C15_TRAP_COOLDOWN   = 50f;  //각성: 함정을 한 번 무시한 뒤 다시 무시할 수 있기까지

	// (C16) 초승달: 검 스킬이 대상 좌우 칸(각성: 둘레 8칸)의 적도 벤다. 본 타격 배율 × 이 비율 (1/2/3레벨)
	public static final float[] C16_CRESCENT_RATIO  = {0.30f, 0.40f, 0.50f};

	// (C17) 섬광 보법: 떨어진 적에게 돌진하며 쓸 수 있는 거리 (적까지의 칸 수, 1/2/3레벨)
	public static final int[]   C17_FLASH_RANGE     = {2, 3, 4};
	public static final int     C17_FLASH_COOLDOWN  = 10;   //(결정) 돌진한 뒤 다시 돌진하기까지. 각성의 이어 쓰기는 쿨타임과 상관없다

	// (C18) 긴 칼날: 검 평타가 대상 바로 뒤 칸의 적도 벤다. 평타 피해 × 이 비율 (1/2/3레벨)
	public static final float[] C18_LONG_EDGE_RATIO = {0.30f, 0.45f, 0.60f};

	// (C19) 피 묻은 날: 검으로 벤 적이 가끔 원작 출혈. 확률과 세기(출혈 수치 = 그 타격 피해 × 세기) (1/2/3레벨)
	public static final float[] C19_BLEED_CHANCE    = {0.20f, 0.25f, 0.30f};
	public static final float[] C19_BLEED_POWER     = {0.20f, 0.30f, 0.40f};

	// (C20) 받아넘기기: 검을 들고 있으면 근접 공격을 쳐낼 확률 (1/2/3레벨)
	public static final float[] C20_DEFLECT_CHANCE  = {0.10f, 0.15f, 0.20f};
	public static final float   C20_STAGGER_TURNS   = 1f;   //각성: 쳐낸 적의 공격이 느려지는 턴
	public static final float   C20_STAGGER_MULTI   = 2f;   //각성: 그동안 공격 시간 배율

	// (C21) 발도: 대기한 뒤 옆 칸으로 들어오는 첫 적을 먼저 벤다. 그 한 칼의 평타 피해 배율 (1/2/3레벨)
	public static final float[] C21_QUICKDRAW_DMG   = {1.10f, 1.25f, 1.40f};

	// ===== 사냥꾼 2차 특성 (구현 지시서 D단계) =====

	// (D1) 첫 일격: 기습한 공격의 피해 + (1/2/3레벨)
	public static final float[] D1_FIRST_STRIKE_DMG = {0.30f, 0.45f, 0.60f};
	public static final int     D1_HUSH_RADIUS      = 4;    //각성: 이 칸 안의 잠든 적은
	public static final float   D1_HUSH_TURNS       = 5f;   //각성: 이 턴 동안 깨어나지 않는다

	// (D2) 약점: 느려지거나 묶이거나 움직이지 못하는 적에게 치명타 확률 (1/2/3레벨)
	public static final float[] D2_CRIT_CHANCE      = {0.15f, 0.25f, 0.35f};
	public static final float   D2_CRIT_DMG         = 1.5f; //치명타 피해 배율
	public static final float   D2_AWAKE_EXTEND     = 1f;   //각성: 치명타가 터지면 느려짐·속박이 이 턴 더

	// (D3) 벌어진 상처: 맞을 때마다 상처 +1 (상한). (결정) 상처가 있는 적을 스킬로 맞히면 터진다:
	// 그 스킬 타격 피해 × 상처 수 × 이 비율 (1/2/3레벨, 가득 5개면 60/90/120%)
	public static final int     D3_WOUND_MAX        = 5;
	public static final float[] D3_BURST_PER_STACK  = {0.12f, 0.18f, 0.24f};

	// (D4) 치고 빠지기: 바로 앞 행동에 걸었으면 이번 공격 피해 + (1/2/3레벨)
	public static final float[] D4_HIT_AND_RUN_DMG  = {0.10f, 0.15f, 0.20f};
	public static final float   D4_FREE_CD          = 3f;   //각성: 턴을 쓰지 않는 이동 사이의 간격 (값진 일격 각성과 같이 씀)

	// (D5) 먼 과녁: 원거리 공격(화살·던지기·마탄)이 1칸보다 먼 칸마다 피해 + (1/2/3레벨), D5_MAX_TILES칸까지
	public static final float[] D5_PER_TILE         = {0.03f, 0.05f, 0.07f};
	public static final int     D5_MAX_TILES        = 8;
	//(결정) 활의 사거리: 모든 활이 이 칸까지 쏜다 (원거리 평타·활 스킬·메아리). 먼 과녁 각성은 이 칸 떨어진 적에게 기습 판정
	public static final int     BOW_RANGE           = 8;

	// (D6) 값진 일격: 다치지 않은 적을 한 번에 쓰러뜨리면 마나 (1/2/3레벨, 규칙 4 상한 안에서)
	public static final int[]   D6_CLEAN_MANA       = {8, 12, 16};

	// (D7) 튀는 일격: 스킬 타격이 가장 가까운 다른 적에게 튄다 (본 타격 배율 × 이 비율, 1/2/3레벨)
	public static final float[] D7_RICOCHET_RATIO   = {0.30f, 0.40f, 0.50f};
	public static final int     D7_RANGE            = 4;    //튈 수 있는 거리 (맞은 적에서)

	// (D8) 새잎 자국: 스킬에 맞은 적의 둘레 빈 바닥에 높은 풀이 자라는 칸 수 (1/2/3레벨)
	public static final int[]   D8_SPROUT_CELLS     = {2, 3, 4};
	public static final float   D8_COOLDOWN         = 30f;  //(결정) 풀을 키운 뒤 다시 키우기까지

	// (D9) 휘감는 덩굴: 스킬에 맞은 적을 원작 속박으로 묶는 턴 (1/2/3레벨)
	public static final float[] D9_ROOT_TURNS       = {1f, 2f, 3f};
	public static final float   D9_COOLDOWN         = 5f;   //(결정) 묶은 뒤 다시 묶기까지

	// (D10) 풀숲의 몸: 높은 풀 속에서 적의 공격 회피 + (1/2/3레벨, 결정: 옆 칸의 적 포함)
	public static final float[] D10_GRASS_EVASION   = {0.15f, 0.25f, 0.35f};
	public static final float   D10_LOST_TURNS      = 2f;   //각성: 놓친 적이 이 턴 동안 나를 다시 알아채지 못한다 (맞으면 알아챔)

	// (D11) 낙법: 최대 체력의 D11_BIG_HIT 이상을 한 번에 받으면 그 피해 - (1/2/3레벨), 1칸(각성 2칸) 구른다
	public static final float   D11_BIG_HIT         = 0.20f;
	public static final float[] D11_REDUCE          = {0.20f, 0.30f, 0.40f};
	public static final int     D11_ROLL            = 1;
	public static final int     D11_ROLL_AWAKE      = 2;

	// (D12) 약초 캐기: 높은 풀을 밟으면 확률로 최대 체력의 일부 회복 (1/2/3레벨)
	public static final float[] D12_HERB_CHANCE     = {0.20f, 0.30f, 0.40f};
	public static final float[] D12_HERB_HEAL       = {0.03f, 0.04f, 0.05f};

	// (D13) 연막 깃: 체력이 최대의 1/4 아래로 떨어지는 순간 연막 + 투명 (1/2/3레벨 투명 턴), 긴 쿨타임
	public static final float   D13_HP_THRESHOLD    = 0.25f;
	public static final float[] D13_INVIS_TURNS     = {2f, 3f, 4f};
	public static final float   D13_COOLDOWN        = 100f;
	public static final float   D13_SMOKE_TURNS     = 2f;   //(결정) 연막이 머무는 턴 (발밑 3×3, 퍼지지 않음)
	public static final int     D13_CONFUSE_RADIUS  = 2;    //각성: 연막 속 = 이 칸 안
	public static final float   D13_CONFUSE_TURNS   = 3f;   //각성: 혼란 턴

	// (D14) 팽팽한 숨: 마나가 가득 찬 동안 회피 + (1/2/3레벨). 각성: 가득 찬 동안 첫 공격을 완전히 피하고 마나 -
	public static final float[] D14_TAUT_EVASION    = {0.10f, 0.18f, 0.25f};
	public static final int     D14_DODGE_MANA      = 20;

	// (D15) 덫 놓기: 덫 피해 = 그 무기 피해 × 비율(파생), 속박 턴, 각성 때 던져 까는 거리
	public static final float[] D15_SNARE_DMG       = {0.60f, 0.80f, 1.00f};
	public static final float[] D15_ROOT_TURNS      = {1f, 2f, 3f};
	public static final int     D15_AWAKE_RANGE     = 2;

	// (D16) 정령의 춤: 걸음마다 더 차는 마나, 각성은 이어 걸은 걸음 수
	public static final int[]   D16_MANA_PER_STEP   = {1, 2, 3};
	public static final int     D16_STRIDE_STEPS    = 5;

	// (D17) 못 박기: 바로 뒤가 벽(각성: 또는 다른 적)인 적을 화살로 맞히면 속박 턴
	public static final float[] D17_PIN_TURNS       = {1f, 2f, 3f};

	// (D18) 박힌 화살: 근접 평타로 터질 때 피해 = 박힐 때의 스킬 타격 피해 × 비율(파생)
	public static final float[] D18_BURST_DMG       = {0.80f, 1.10f, 1.40f};

	// (D19) 화살 줍기: 쓰러진 적이 박힌 화살을 떨굴 확률 + (ARROW_RECOVER_CHANCE에 더함), 각성은 돌아오는 거리
	public static final float[] D19_DROP_BONUS      = {0.15f, 0.25f, 0.35f};
	public static final int     D19_PULL_RANGE      = 3;

	// (D21) 활대 치기: 활 근접 평타 피해 + (레벨과 상관없음), 밀쳐낸 직후 사격 피해 + (1/2/3레벨)
	public static final float   D21_MELEE_DMG       = 0.25f;
	public static final float[] D21_SHOT_DMG        = {0.20f, 0.30f, 0.40f};

	// (F6) 기생 싹: 3층부터 층마다 이 확률로 1개. 체력·경험치·약한 피해, 옆 칸에 들어선 주인공을 묶는 턴과 다시 묶기까지의 간격
	public static final int     F6_SPROUT_MIN_DEPTH    = 3;
	public static final float   F6_SPROUT_LEVEL_CHANCE = 0.75f; //층마다 하나가 나올 확률
	public static final float   F6_SPROUT_EXTRA_CHANCE = 0.35f; //하나가 나온 층에 하나 더 나올 확률
	public static final int     F6_SPROUT_HP           = 8;
	public static final int     F6_SPROUT_EXP          = 2;
	public static final int     F6_SPROUT_DMG_MIN      = 1;
	public static final int     F6_SPROUT_DMG_MAX      = 3;
	public static final float   F6_SPROUT_ROOT_TURNS   = 2f;
	public static final int     F6_SPROUT_COOLDOWN     = 5;

	// (H1) 봉인 등롱 (DM-100 자리): 앞쪽 부채꼴 시야, 가만히 못 볼 때 도는 간격, 젖은 채 맞으면 기절
	public static final boolean H1_LANTERN           = true;
	public static final float   H1_CONE_HALF         = 45f;  //부채꼴 양옆 각도 (모두 90도)
	public static final int     H1_RANGE             = 6;    //보는 거리 (칸)
	public static final int     H1_TURN_TURNS        = 3;    //이 턴마다 45도씩 돈다
	public static final float   H1_STUN_TURNS        = 2f;   //젖은 채 맞으면 기절하는 턴
	public static final float   H1_STUN_GUARD        = 5f;   //기절한 뒤 이 턴 동안은 다시 기절하지 않는다
	public static final float   H1_LIGHT_ALPHA       = 0.09f; //빛이 닿는 칸 표시의 투명도 (모든 칸 같은 밝기)
	public static final int     H1_LIGHT_FOG_RANGE   = 2;     //시야 밖에서는 등롱에서 이 칸 수 안의 빛만 보인다 (축소된 범위)

	//H3 탈옥수 (원작 도둑): 도망치며 지나온 문을 한 번 걸어 잠근다, 잡으면 금화도 떨군다
	public static final boolean H3_THIEF_LOCK        = true;
	public static final int     H3_LOCK_TURNS        = 2;     //(결정) 문이 잠겨 있는 턴. 탈옥수 한 마리는 문 하나를 한 번만 잠근다

	//H4 넋 나방 떼 (원작 파리떼 자리, 6~7층)
	public static final int     H4_MOTH_MANA         = 3;     //쓰러질 때마다 돌려주는 마나

	//H5 봉인술사 (원작 강령술사): 유골을 부르지 않고 봉인 부적을 던진다 (피해·속박은 10층 간수의 부적과 같음)
	public static final boolean H5_SEAL_TALISMAN     = true;
	public static final int     H5_TALISMANS         = 4;     //(결정) 한 마리가 던지는 부적 수, 던질 때마다 로브의 부적이 한 장씩 사라진다
	public static final int     H5_TALISMAN_CD       = 2;     //(결정) 던진 뒤 다시 던지기까지의 턴
	public static final int     H5_TALISMAN_RADIUS   = 1;     //(결정) 터지는 범위 (간수의 부적은 2칸)
	public static final float   H5_BINDER_SPEED      = 0.5f;  //(결정) 봉인술사 이동 속도 배율 (원작의 절반)
	//H6 갇힌 영혼과 사슬 고리 (6~9층)
	public static final float   H6_LEVEL_CHANCE      = 0.5f;  //(제안) 층마다 한 쌍이 놓일 확률
	public static final int     H6_RING_HP           = 15;    //(제안) 사슬 고리 체력 (반드시 맞음)
	public static final int     H6_NOISE_RANGE       = 8;     //(제안) 고리를 칠 때 이 칸 수 안의 적이 모인다
	public static final float   H6_FREED_TURNS       = 30f;   //(제안) 풀려난 영혼이 같이 싸우는 턴
	public static final float   H6_RING_RAISE        = 9f;    //사슬 고리를 벽면에 박힌 것처럼 위로 올려 그리는 픽셀
	//I 10층 그림자 간수 (새로 짠 보스, WardenBossLevel·ShadowWarden). 옛 간수(텐구 바탕)는 MazeBosses.ENABLED여도 쓰지 않는다
	public static final boolean I_NEW_WARDEN         = true;
	public static final int     I_WARDEN_HP          = 200;   //간수 체력 (원작 텐구와 같음)
	public static final float   I1_PHASE2_AT         = 0.5f;  //(결정) 체력이 이 비율 아래로 내려가면 대봉인실(2단계)로
	public static final int     I1_CELLDOOR_HITS     = 2;     //(결정) 감방 문은 피해와 상관없이 이 횟수만큼 맞으면 부서진다 (반드시 맞음)
	public static final int     I2_LIGHT_RANGE       = 9;     //등불 빛이 닿는 칸 수
	public static final float   I2_LIGHT_HALF        = 45f;   //등불 부채꼴의 양옆 각도
	public static final float   I2_LIGHT_ALPHA       = 0.11f; //지금 비추는 칸의 빛 (칸 전체를 옅게)
	public static final boolean I2_PREVIEW           = false; //(결정) 다음 턴에 비출 칸의 예고 점은 그리지 않는다 (빛 자체가 늘 보이고, 가두기는 주인공이 턴을 마칠 때 보이는 빛으로만 판정)
	public static final float   I2_PREVIEW_ALPHA     = 0.30f; //예고 점을 켤 때의 밝기 (칸 가운데의 작은 점)
	public static final int     I3_CAPTURE_MIN       = 6;     //(제안) 빛 안에서 턴을 마쳐 잡힐 때 사슬 피해
	public static final int     I3_CAPTURE_MAX       = 10;
	public static final int     I3_CELL_DMG          = 2;     //(제안) 잠긴 감방 안에 있는 동안 턴마다 받는 피해
	public static final float   I6_PROTECT           = 0.6f;  //(I6) 2단계 영혼의 보호: 받는 물리 피해 감소 (풀려난 영혼에 붙들린 동안은 없음)
	public static final int     I7_SENTENCE_FIRST    = 3;     //(제안) 2단계가 된 뒤 첫 선고까지 (간수 턴)
	public static final int     I7_SENTENCE_CD       = 9;     //(사용자) 줄인 방에 맞춰 12 → 9. 선고가 끝난(풀린) 뒤 다음 선고까지 (간수 턴)
	public static final int     I7_SENTENCE_TURNS    = 20;    //(결정) 선고가 이어지는 턴
	public static final int     I7_PENALTY_MIN       = 5;     //(제안) 선고를 어겼을 때 사슬이 조이는 피해
	public static final int     I7_PENALTY_MAX       = 8;
	public static final float   I7_BIND_TURNS        = 5f;    //(제안) 고리를 끊어 풀려난 영혼이 간수를 붙드는 턴
	public static final int     I8_NET_FIRST         = 5;     //(사용자) 줄인 방에 맞춰 6 → 5. 2단계가 된 뒤 첫 사슬 그물까지 (간수 턴)
	public static final int     I8_NET_CD            = 7;     //(사용자) 줄인 방에 맞춰 9 → 7. 사슬 그물 사이 간격 (간수 턴)
	public static final int     I8_NET_LINES         = 7;     //(결정) 사슬 그물 줄 수 (가로줄·세로줄을 함께, 주인공의 가로줄·세로줄 포함, 같은 방향 줄 사이 1칸 이상)
	public static final float   I8_CHAIN_SPEED       = 2.5f;  //(결정) 끌어내기·사슬 그물의 사슬이 날아가는 속도 배율 (원작 사슬 연출 대비)
	public static final int     I8_NET_DMG_MIN       = 4;     //(제안) 팽팽해진 줄 위의 피해
	public static final int     I8_NET_DMG_MAX       = 8;
	public static final float   I8_NET_CRIPPLE       = 3f;    //(결정) 팽팽해진 줄 위의 느려짐(Cripple) 턴
	public static final int     I8_PULL_FIRST        = 3;     //(사용자) 줄인 방에 맞춰 4 → 3. 2단계가 된 뒤 첫 끌어내기까지 (간수 턴)
	public static final int     I8_PULL_CD           = 5;     //(사용자) 줄인 방에 맞춰 7 → 5. 끌어내기 사이 간격
	public static final int     I8_PULL_MIN_DIST     = 3;     //(제안) 이만큼 떨어져 있어야 끌어낸다
	public static final int     I8_PULL_DMG_MIN      = 5;     //(제안) 끌어내기 피해
	public static final int     I8_PULL_DMG_MAX      = 9;
	public static final float   I8_PULL_ROOT         = 1f;    //(결정) 끌어내기에 맞으면 속박 턴
	public static final int     I9_SEAL_CD           = 12;    //(사용자) 줄인 방에 맞춰 15 → 12. 대봉인 쿨타임 (간수 턴). 안전지대가 하나라도 생긴 뒤부터 돈다
	public static final int     I9_SEAL_CHARGE       = 7;     //(사용자) 줄인 방에 맞춰 9 → 7, 닿지 못해도 쓴다. 대봉인을 모으는 턴 (그동안 간수는 다른 행동을 하지 않는다)
	public static final int     I9_SAFE_SIZE         = 3;     //(결정) 안전지대 한 변 (풀어 준 영혼 하나마다 3×3)

	// ===== J. 11~14층 (마력 광맥) =====
	public static final float   J1_LINGER            = 3f;    //(결정) 마력 증기 안에 있으면 마력독(턴마다 피해 + 스킬 막힘)의 남은 턴을 이만큼 채운다 (나와도 이만큼 이어짐)
	public static final float   J1_BURST_MULTI       = 2f;    //(제안) 터지는 증기 피해 = 원소 반응 기준 피해(2 + 0.5 × 층) × 이 배율
	public static final int     J2_VEINS_MIN         = 2;     //(제안) 11~14층 일반 층마다 살아 있는 광맥 수
	public static final int     J2_VEINS_MAX         = 4;
	public static final int     J2_VEIN_MANA         = 2;     //(제안) 광맥 옆 칸에 있으면 턴마다 더 차는 마나 (광맥 수와 상관없이 한 번)
	public static final int     J3_BAT_DRAIN         = 20;    //(결정) 마석 박쥐가 한 번 물 때 빨아 가는 마나
	public static final float   J3_BAT_RETURN        = 0.5f;  //(결정) 쓰러뜨리면 배에 담긴 마나 중 돌려받는 몫 (내림)
	public static final int     J3_BAT_MAX           = 60;    //(결정) 박쥐 배에 담기는 마나 최대 (그림 줄 0~3 = 0 / 1~20 / 21~40 / 41~60, 최대의 1/3씩)
	public static final int     J4_BURST_MIN         = 12;    //(제안) 광폭 중에 쓰러진 광부의 마석이 터질 때 둘레 1칸 모두가 받는 피해
	public static final int     J4_BURST_MAX         = 24;
	public static final boolean J5_CHARGE            = false; //(결정) 꺼 둠: 주술사는 원작처럼 바로 쏜다. (J5) 마석 주술사가 마법을 쏘기 전 한 박자(1턴) 지팡이를 빛내며 모은다. 그동안 맞으면 주문이 끊긴다
	public static final boolean J6_VAPOR_BITE        = true;  //(결정) 수정 거미는 물 때 원작 독 대신 마력 증기의 지속 피해·스킬 막힘(ManaVapor.VaporBite)을 건다 (원작처럼 물면 물러남)
	public static final int     J6_BITE_TURNS_MIN    = 5;     //(결정) 그 지속 턴 5~6 (턴마다 마력 증기 안과 같은 피해 1 + 층/5)
	public static final int     J6_BITE_TURNS_MAX    = 6;
	public static final boolean J7_VAPOR             = true;  //(J7) 갉힌 채굴 기계(DM-200): 원작 독가스 대신 마력 증기, 젖은 채 맞으면 기절 (봉인 등롱의 H1_STUN_TURNS·H1_STUN_GUARD를 같이 씀)
	//(J9) 땅울림 (13·14층): 바닥에 금이 한 줄로 뻗고(예고) 다음 턴에 그 줄 위 모두가 피해
	public static final boolean J9_TREMOR            = true;  //(사용자 확인) 금 그림 sprites/maze_tremor_crack.png
	public static final int     J9_EVERY_MIN         = 60;    //(결정) 평균 80턴에 한 번: 다음 땅울림까지 60~100턴
	public static final int     J9_EVERY_MAX         = 100;
	public static final int     J9_DMG_MIN           = 10;    //(결정) 줄 위 모두(주인공·적)가 받는 피해, 방어 무시
	public static final int     J9_DMG_MAX           = 16;
	public static final int     J9_NEAR              = 2;     //(제안) 금은 주인공에서 이 칸 수 안의 칸을 지난다
	public static final int     J9_HALF_LEN          = 4;     //(제안) 금이 그 칸에서 양쪽으로 뻗는 최대 칸 수 (최대 9칸)
	//(K) 15층 뿌리를 갉는 것 (새로 짠 보스, GnawerBossLevel·RootGnawer). 모두 시작값
	public static final boolean K_NEW_GNAWER         = true;  //끄면 원작 DM-300 보스층
	public static final int     K_HP                 = 300;   //(결정) 체력
	public static final int     K_ATTACK             = 20;    //명중 (원작 DM-300과 같음)
	public static final int     K_DEFENSE            = 15;    //회피 (원작 DM-300과 같음)
	public static final int     K_MELEE_MIN          = 15;    //땅 위에서 옆 칸을 물 때 (원작 DM-300 근접과 같음)
	public static final int     K_MELEE_MAX          = 25;
	public static final int     K_ARMOR_MAX          = 10;    //방어 0~10 (원작 DM-300과 같음)
	public static final int     K_VEINS              = 19;    //(결정) 광맥 기둥 수 (층을 만들 때 무작위 자리)
	public static final int     K_EMERGE_WARN        = 2;     //(K2, 제안) 솟구치기: 둥근 경고가 뜬 뒤 솟기까지 턴 (경고 범위 반지름보다 1 크게 — 가운데서도 피할 수 있게)
	public static final int     K_EMERGE_RADIUS      = 1;     //(제안) 경고 범위: 주인공 칸 둘레 이 칸 수 (1이면 3×3)
	public static final int     K_EMERGE_DMG_MIN     = 16;    //(제안) 솟구치기 피해 (방어 적용)
	public static final int     K_EMERGE_DMG_MAX     = 26;
	public static final int     K_ABOVE_TURNS        = 4;     //(제안) 솟은 뒤 머리가 땅 위에 있는 턴 (딜 구간)
	public static final int     K_FRESH_TURNS        = 1;     //(제안) 솟은 직후 받는 피해가 큰 턴
	public static final float   K_FRESH_MULTI        = 1.5f;  //(제안) 그 피해 배율
	public static final int     K_UNDER_TURNS        = 2;     //(제안) 땅속으로 들어간 뒤 다음 경고까지 쉬는 턴
	public static final int     K_DASH_CD            = 6;     //(K3, 제안) 땅속 질주를 다시 쓰기까지 (벌레 턴)
	public static final int     K_DASH_DMG_MIN       = 14;    //(제안) 질주 줄 위 피해 (방어 적용) + 옆으로 한 칸 밀침
	public static final int     K_DASH_DMG_MAX       = 22;
	public static final float   K_TRAIL_ALPHA        = 0.35f; //(결정) 굴 자국 그림 진하기
	public static final int     K_GNAW_CD            = 14;    //(K4, 제안) 갉기를 다시 쓰기까지 (벌레 턴). 싸움의 첫 행동은 갉기
	public static final int     K_GNAW_TURNS         = 3;     //(제안) 갉는 턴 (광맥이 한 턴에 한 단계씩 뜯겨 나감)
	public static final float   K_GNAW_BREAK         = 0.12f; //(제안) 갉는 동안 머리가 받은 피해가 최대 체력의 이만큼이면 끊김
	public static final int     K_SHELL_MAX          = 3;     //(제안) 마석 껍질 최대 겹
	public static final float   K_SHELL_REDUCE       = 0.10f; //(제안) 껍질 한 겹마다 받는 피해 감소
	public static final int     K_SPIT_RANGE         = 24;    //(사용자) 마력 토사 부채꼴 길이: 방 끝까지 (주인공 쪽, 양옆 45°)
	public static final float   K_TRAIL_SWEEP        = 0.06f; //(사용자) 굴 자국이 줄을 따라 한 칸씩 생기는 간격(초)
	public static final int     K_TRAIL_ROCKS        = 7;     //(사용자) 땅속 질주의 굴 자국이 한 칸 생길 때 튀는 돌 부스러기 수
	public static final int     K_DIG_ROCKS          = 4;     //(사용자) 굴착: 띠의 칸마다 튀는 돌 부스러기 수 (보스가 가리지 않게 줄임)
	public static final int     K_EMERGE_FPS         = 5;     //(사용자) 솟구침 동작 빠르기 (초당 칸, 처음 9)
	public static final float   K_SPIT_COS           = 0.883f; //(사용자) 마력 토사 부채꼴 폭: 가운데 줄과의 각의 코사인 (0.883 ≈ 양옆 28°, 처음 0.70 ≈ 45°)
	public static final float   K_SPIT_WAVE          = 0.035f; //(사용자, 처음 0.05) 마력 토사 증기가 입에서 한 칸 멀어질 때마다 늦게 생기는 시간(초)
	public static final int     K_SPIT_VAPOR         = 40;    //(제안) 토사가 칸마다 남기는 마력 증기 양
	public static final float   K_MOLT_AT            = 0.5f;  //(K5, 결정) 탈피: 체력이 이 비율에서 멈추고 2단계로
	public static final float   K_MOLT_SHARD_TIME    = 0.65f; //(사용자) 탈피 껍질 조각이 날아가는 시간(초, 처음 0.4)
	public static final float   K_MOLT_FIRE_MULTI    = 1.5f;  //(제안) 탈피한 몸이 받는 화상 피해 배율
	public static final int     K_COLLAPSE_CD        = 10;    //(K6, 사용자) 굴 폭파 간격 (벌레 턴)
	public static final float   K_DIG_FX_STEP        = 0.025f; //(사용자) 굴착: 땅이 들썩이는 이펙트와 굴 자국이 띠를 따라 한 칸씩 생기는 간격(초, 둘이 같은 빠르기)
	public static final int     K_DIGS_FOR_COLLAPSE  = 2;     //(사용자) 2단계에서 굴 폭파를 다시 쓰려면 지난 폭파 뒤 굴착을 이만큼 이상
	public static final int     K_DIG_GAP            = 3;     //(사용자) 굴착과 굴 폭파 사이에 적어도 이만큼 텀 (벌레 턴)
	//(K8) 미스릴 파편 (items/quest/MithrilShard): 15층 보스가 원작 저주받은 쇳조각 대신 떨어뜨린다
	public static final int     K_MITHRIL_MIN        = 2;     //(사용자) 떨어뜨리는 수 (2~3개, 반반)
	public static final int     K_MITHRIL_MAX        = 3;
	//(사용자) 전직의 핵 세 개(개화·만개·결실)는 게임 안에서 은은한 새잎빛이 감돈다 (그림이 아닌 원작 아이템 빛 ItemSprite.Glowing)
	public static final int     CORE_GLOW_COLOR      = 0xB4E67A; //새잎빛
	public static final float   CORE_GLOW_PERIOD     = 2f;    //밝아졌다 흐려지는 데 걸리는 시간(초), 숨 쉬듯 천천히
	public static final float   CORE_GLOW_STRENGTH   = 0.35f; //원작 아이템 빛(마법 부여 무기 등)에 견준 세기 (1 = 원작)
	public static final boolean CORE_BIG_IN_BAG      = true;  //(사용자) 가방 칸에서는 세 핵을 조금 크게 (그림 한 픽셀이 화면 한 칸씩 더 커지는 배율, ui/InventorySlot)
	public static final float   FRUIT_MOTE_INTERVAL  = 0.55f; //(사용자) 결실의 핵 둘레에 황금빛 알갱이가 하나 피어오르는 간격(초), 드문드문 (effects/particles/GoldMoteParticle)
	public static final float   KEY_SLIDE_DELAY      = 0.1f;  //(사용자) PC 대각선 벽 따라 이동: 벽에 막힌 채 방향키 두 개를 이만큼(초) 계속 눌러야 움직인다 (짧게 누르고 떼면 움직이지 않음)
	public static final float   MITHRIL_BREATH_PERIOD = 2f;   //(사용자) 맥 둘레의 마력 빛이 한 번 숨 쉬는 시간(초). 반짝이지 않게 꺼지지는 않음
	public static final int     MITHRIL_VALUE        = 100;   //(제안) 상점에 팔 때 한 개 값
	//(K8) 대장장이와 미스릴 (items/quest/EnchantedMithrilShard·MithrilBlueprint, windows/WndMithrilForge)
	public static final int     K_MITHRIL_FAVOR      = 2000;  //(사용자) 대장장이 퀘스트를 끝내면 호감에 더 주는 몫 = 미스릴 도안 제작 비용
	public static final int     MITHRIL_BLUEPRINT_COST = 2000; //(사용자) 대장장이: 부여된 파편 + 무기 두 종 → 도안 (호감)
	public static final int     MITHRIL_ARMOR_COST   = 1000;  //(제안) 대장장이: 갑옷 + 미스릴 파편 → 미스릴 보강 (호감)
	public static final int     MITHRIL_ARMOR_DR     = 2;     //(사용자) 미스릴 보강 갑옷: 최소·최대 방어 +2 (한 번만)
	public static final float   MITHRIL_ARMOR_EVASION = 1.2f; //(사용자) 미스릴 보강 갑옷: 회피 수치 ×1.2 (곱연산)
	public static final float   MITHRIL_WEAPON_DMG   = 1.2f;  //(제안) 미스릴 무기: 피해 ×1.2 (평타·스킬 모두)
	public static final int     MITHRIL_MANA_CUT     = 5;     //(제안) 미스릴 무기를 들면 스킬 마나 -5
	public static final int     MITHRIL_ENCHANT_ENERGY = 0;   //(사용자) 연금술: 미스릴 파편 1 + 충전의 주문서 1 → 부여된 파편 1, 에너지는 들지 않음

	// ===== L. 16~19층 잡몹 (잊힌 마도시) =====
	//(L2) 왕실 근위(원작 수도승)·근위대장(원작 상급 수도승): 집중 자세 중에 맞으면 흘리고 곧바로 받아친다(평타 판정), 그 뒤 한 박자 무방비(반드시 맞음)
	public static final boolean L2_COUNTER         = true;
	public static final float   L2_UNGUARD_TURNS   = 1f;    //(사용자) 무방비 시간 (그동안 회피 0 = 반드시 맞음)
	public static final float   L2_FOCUS_TURNS     = 2f;    //(사용자) 집중 자세가 스스로 풀리기까지
	public static final float   L2_FOCUS_DMG       = 0.5f;  //(사용자: 약하게, 값은 제안) 집중 자세 동안 근위가 주는 피해 배율
	//(L3) 풀려난 주문(원작 정령): 쓰러질 때 자기 원소를 둘레 한 칸의 모두에게 터뜨린다 — (사용자) 원소 + 작은 피해(원소 반응 기준 피해 × 1, 제안)
	public static final boolean L3_BURST           = true;
	public static final float   L3_BURST_MULTI     = 1f;
	public static final float   L3_CHILL_TURNS     = 3f;    //(제안) 냉기 주문이 묻히는 냉기 시간
	public static final float   L3_SPELL_REACT     = 1.5f;  //(제안) 터진 주문이 옆의 반대 원소 주문에 일으키는 증기 피해 배율 (불↔냉기 증기, 불↔전격은 원소 반응 폭발)
	public static final float   L3_MELEE_DMG       = 0.4f;  //(사용자: 약한 근접, 값은 제안) 바로 옆의 상대에게는 쏘지 못하고 원래 피해(20~25)의 이 배율로만 친다
	public static final int     L3_RANGED_CD_MIN   = 2;     //(제안) 쏘는 간격 (원작 3~5턴, 탄이 주된 피해가 되어 줄임)
	public static final int     L3_RANGED_CD_MAX   = 3;
	//(사용자) 풀려난 주문이 너무 세서 조금씩 낮춤 (원작 체력 60·회피 20·명중 25, 탄 피해 20~25)
	public static final int     L3_HP              = 54;
	public static final int     L3_DEF             = 18;
	public static final int     L3_ACC             = 23;
	public static final int     L3_BOLT_MIN        = 17;
	public static final int     L3_BOLT_MAX        = 22;
	//(L4) 궁정 마도사(원작 흑마법사): 가까운 주문 하나를 증폭(푸른 선), 증폭된 주문은 더 아프다
	public static final boolean L4_AMPLIFY         = true;
	public static final int     L4_RANGE           = 4;     //(제안) 증폭이 닿는 거리 (시선이 닿아야 함)
	public static final float   L4_AMP_DMG         = 1.5f;  //(사용자) 증폭된 주문의 탄 피해 배율 (근접은 그대로, 쓰러질 때 터짐 피해에도)
	public static final float   L4_AMP_SCALE       = 1.1f;  //(사용자) 증폭된 주문은 몸이 아주 조금(1.1배) 커지고 원작 입자(원작 크기·개수)를 뿜는다
	public static final float   L4_AMP_DENSE       = 0.75f; //(사용자) 증폭된 주문이 입자를 뿜는 간격 배율 (원작 간격 × 이 값, 작을수록 많음)
	public static final float   L3_PARTICLE_SIZE   = 0.65f; //(사용자) 풀려난 주문이 평소 뿜는 입자 크기 배율 (원작 입자가 크고 많아서). 증폭되면 원작 그대로
	public static final float   L3_PARTICLE_SPARSE = 2.0f;  //(사용자) 평소 입자를 뿜는 간격 배율 (클수록 적음). 증폭되면 원작 간격
	public static final float   L3_PARTICLE_CALM   = 0.35f; //(사용자) 풀려난 주문의 입자가 움직이는 빠르기 배율 (자유분방하지 않게)
	public static final float   L3_PARTICLE_RISE   = 30f;   //(사용자) 불꽃 입자가 떠오르는 가속 (원작 80)
	//(L5) 마도 석상(원작 골렘): 가장 가까운 아군 하나를 수호(금빛 선), 그 아군이 받을 피해를 모두 대신 받는다(석상 방어 무시)
	public static final boolean L5_GUARD           = true;
	public static final int     L5_RANGE           = 4;     //(제안) 수호가 닿는 거리 (시선이 닿아야 함, 마도사 증폭과 같음)
	//(L6) 거울 초상(새 몹, 18층부터): 2칸 안에서 쓴 공격 스킬을 주인공이 있던 칸에 한 박자 뒤 따라 쓴다
	public static final boolean L6_MIRROR          = true;
	public static final int     L6_RANGE           = 2;     //(사용자) 따라 쓰는 거리, 평소 이만큼 거리를 두고 따라다닌다
	public static final int     L6_HP              = 40;    //(제안) 약하다
	public static final int     L6_DEF             = 18;
	public static final float   L6_SPEED           = 0.8f;  //(사용자) 조금 느리게 (원작 보통 1)
	//(L7) 16~19층 잡몹 비중: 그 판의 20층 보스가 망령 왕이면 시민·근위, 여제면 주문·마도사가 조금 더
	public static final boolean L7_BOSS_BIAS       = true;
	public static final int     L7_BIAS_EXTRA      = 1;     //(제안) 층마다 그쪽 잡몹 두 종을 하나씩 더
	//(M) 현자의 방 퀘스트
	public static final boolean M_SAGE             = true;  //(사용자) 현자·브로치 그림이 정해져 켬 (끄면 5층 비밀방은 원작 쥐 왕, 퀘스트·고정·잎 이벤트 모두 없음). 켜면 쥐 왕 대신 현자
	public static final int     M_FIX_DEPTH        = 15;    //(사용자, 지시서는 6) 퀘스트를 받지 않고 이 층에 들어서면 시드의 20층 보스로 고정
	public static final int     M_LEAVES           = 5;     //(지시서) 망령 왕 머리 위 잎 수
	public static final float   M_LEAF_TURNS       = 2f;    //(사용자) 잎 하나가 차는 턴 (다 차기까지 10턴)
	public static final int     M_NO_RESPAWN_FROM  = 16;    //(사용자) 현자에게 보고해 강화된 브로치: 역행 중 이 층들(16~19층)에는 적이 다시 생기지 않는다
	public static final int     M_NO_RESPAWN_TO    = 19;
	public static final float   M_APPROACH_TURNS   = 8f;    //(지시서) 잎이 다 찬 뒤 왕이 주인공에게 기어 오는 최대 턴 (그 뒤엔 그 자리에서 알아봄)
	//(N1) 탑의 심장(21~24층) 바닥 두 종: 검은 낙엽(마나가 차지 않음)·새잎 땅(마나가 더 참)
	public static final boolean N1_FLOORS          = true;
	public static final float[] N1_LEAF_FRACTION   = { 0.07f, 0.11f, 0.15f, 0.20f }; //(사용자) 21·22·23·24층 바닥 칸 중 검은 낙엽 비율, 뭉치로
	public static final float   F_SNAKE_GRASS_EVASION = 1.5f; //(사용자) 덩굴뱀: 키 큰 풀 속에서 회피 수치 배율 (기습 판정도 받지 않음)
	public static final float   N1_LEAF_DMG        = 1.1f;  //(사용자) 검은 낙엽 위에 선 적이 주는 피해 배율 (세계수의 열매와 겹침)
	public static final int     N1_LEAF_CLUMP_MIN  = 3;
	public static final int     N1_LEAF_CLUMP_MAX  = 6;
	public static final int     N1_LEAF_STEP_FX    = 3;     //(사용자) 주인공이 검은 낙엽을 밟을 때 살짝 흩날리는 잎 입자 수
	public static final boolean N1_RUSTLE          = true;  //(사용자) 낙엽을 밟으면 바스락 소리로 둘레의 적이 그 칸으로 모임 (잠든 적은 깸)
	public static final int     N1_RUSTLE_RANGE    = 5;     //(사용자) 바스락 소리가 닿는 거리
	public static final float   N1_RUSTLE_VOLUME   = 0.2f;  //(사용자) 바스락 소리 크기 (작게, 원작 풀 소리 1.0)
	public static final float   N1_RUSTLE_SOUND_GAP = 0.7f; //(사용자) 연달아 밟아도 이 초에 한 번만 소리가 난다
	public static final int     N1_SPROUT_MANA     = 1;     //(사용자) 새잎 땅 위에서 턴마다 더 차는 마나 (2배)
	//(N2) 검은 잎의 시녀(원작 서큐버스): 시야 안의 스킬마다 쓴 마나만큼 보호막, 가득 차면 터진다
	public static final boolean N2_MAID            = true;
	public static final int     N2_SHIELD_MAX      = 70;    //(사용자) 이만큼 차면 과식으로 터진다 (스킬 하나 30이면 세 번째에)
	public static final float   N2_STUN            = 3f;    //(제안) 터지면 시녀 기절 턴
	public static final int     N2_BURST_MIN       = 12;    //(사용자) 과식으로 터질 때 둘레 1칸의 다른 적이 받는 피해 (값은 제안)
	public static final int     N2_BURST_MAX       = 20;
	//(N7, 사용자) 메아리 매듭: 22~24층 층마다 이만큼, 체력, 이어지는 거리, 끊으면 새잎 땅이 되는 반지름 (값은 제안)
	public static final boolean N7_KNOT            = true;
	public static final int     N7_KNOTS_MIN       = 1;
	public static final int     N7_KNOTS_MAX       = 2;
	public static final int     N7_HP              = 30;
	public static final int     N7_LINK_RANGE      = 5;
	public static final float   N7_SAP_REDUCE      = 0.5f;  //(사용자) 수액: 물리 피해 감소 (불타는 동안 꺼짐)
	public static final int     N7_SAP_FIRE_MULTI  = 2;     //(사용자) 수액: 불타는 동안 화상 피해 배율
	public static final int     N7_BOLT_FIRST      = 2;     //(사용자) 강화 마탄: 처음 이어지고 이 턴 뒤
	public static final int     N7_BOLT_CD         = 4;     //(사용자) 그 뒤 이 턴마다
	public static final int     N7_BOLT_MIN        = 10;    //강화 마탄 피해 (방어 무시, 제안)
	public static final int     N7_BOLT_MAX        = 16;
	public static final int     N7_VEIN_SHIELD     = 15;    //광맥: 마석 껍질 방어막 (제안)
	public static final int     N7_VEIN_REFILL     = 5;     //이 턴마다 다시 채움
	public static final int     N7_BURST_MIN       = 12;    //(사용자) 광맥 메아리를 받은 적이 쓰러지면 둘레 1칸 피해 (제안)
	public static final int     N7_BURST_MAX       = 20;
	//(N8, 사용자) 열매 맺는 가지: 23~24층 층마다 하나, 열매가 맺히기까지·익기까지 턴, 먹은 효과(주는 피해·받는 피해 배율, 턴, 주인공 회복), 먹으러 오는 적의 거리(제안)
	public static final boolean N8_BRANCH          = true;
	public static final int     N8_BRANCHES        = 1;
	public static final int     N8_GROW_TURNS      = 30;
	public static final int     N8_RIPEN           = 8;
	public static final float   N8_DMG             = 1.1f;
	public static final float   N8_TAKEN           = 0.9f;
	public static final int     N8_TURNS           = 10;
	public static final int     N8_HEAL            = 5;
	public static final int     N8_SEEK_RANGE      = 8;
	public static final boolean N8_BRANCH_ART      = true;  //(사용자 확인) 가지 그림 sprites/maze_fruitbranch.png, 작은 열매 sprites/maze_fruit_small.png
	public static final int     N2_LEAF_DROP_MIN   = 2;     //(사용자) 순간이동해 나타난 칸 둘레에 더 까는 낙엽 칸 수
	public static final int     N2_LEAF_DROP_MAX   = 3;
	public static final boolean N2_CHARM_LOOK      = true;  //(사용자) 주인공을 매혹하면 시녀가 다른 외형으로 바뀌고 다시 풀리지 않는다 (전사·도적 → 여자, 사냥꾼·마법사 → 남자)
	public static final float   N2_REVERT_GHOST    = 0.5f;  //(사용자) 변신이 풀릴 때 잠깐 비치는 원래 모습의 진하기
	public static final float   N2_SHIFT_GHOST     = 0.7f;  //(사용자) 변신할 때 앞 모습의 잔상 진하기 (잘 보이게)
	public static final float   N2_SHIFT_TIME      = 0.7f;  //(사용자) 변신 잔상이 커지며 사라지는 시간(초)
	//(N3) 핏줄 눈(원작 악마의 눈): 광선이 지나간 줄이 잠깐 달궈진다, 광선은 검은 낙엽을 태운다 (예고선은 원작처럼 없음, 사용자)
	public static final boolean N3_EYE             = true;
	public static final int     N3_HOT_TURNS       = 2;     //(사용자) 달궈진 줄이 남는 턴
	public static final int     N3_HOT_DMG_MIN     = 6;     //(사용자) 달궈진 칸에 들어서면 받는 피해
	public static final int     N3_HOT_DMG_MAX     = 10;
	public static final float   N3_HOT_MARK_ALPHA  = 0.26f;  //(사용자) 광선 자국이 피해를 주는 칸마다 까는 옅은 핏빛 칸 표시의 진하기
	//(N5) 검은 씨앗 혹(원작 악마 소환진)·검은 싹(원작 리퍼): 행동은 원작 그대로, 이름·그림만. 씨앗 혹은 떠다니지 않는다
	public static final boolean N5_SEED            = true;
	//(N6) 얽힌 것들: 셋이 한 묶음, 한 마리가 받은 피해를 이어진 것 모두가 1/마리수씩 나눠 받고 각자 1/5씩 더 받음
	public static final boolean N6_TANGLED         = true;
	public static final int     N6_GROUP           = 3;     //(사용자) 한 묶음 마리 수
	public static final int     N6_HP              = 50;    //(제안) 한 마리 체력
	public static final int     N6_DMG_MIN         = 12;    //(제안) 근접 피해
	public static final int     N6_DMG_MAX         = 20;
	public static final int     N6_LINK_RANGE      = 3;     //(사용자) 이보다 멀어지면 뿌리가 끊긴다
	public static final float   N6_SHARE           = 1f / 4;  //(사용자) 이어진 것 모두가 각자 받는 몫 (셋이 1/4씩)
	public static final float   N6_FLASH_TIME      = 0.6f;  //피해가 뿌리를 타고 지나간 뒤 뿌리가 핏빛에서 돌아오는 시간(초)
	public static final float   N6_FADE_TIME       = 0.8f;  //(사용자) 쓰러진 뒤 사라지는 시간(초)
	public static final float   N2_FADE_TIME       = 0.8f;  //(사용자) 쓰러진 시녀가 사라지는 시간 (원작 3초, 땅에 오래 남지 않게)
	public static final int     K_COLLAPSE_WARN      = 3;     //(제안) 굴 폭파 경고 턴
	public static final int     K_COLLAPSE_RADIUS    = 1;     //(사용자) 굴 자국과 그 둘레 1칸 (지시서는 2칸)
	public static final int     K_COLLAPSE_DMG_MIN   = 30;    //(제안) 굴 폭파 피해 (방어 적용)
	public static final int     K_COLLAPSE_DMG_MAX   = 45;
	public static final int     K_SWEEP_CD           = 7;     //(사용자) 굴착(예전 꼬리 휩쓸기) 간격
	public static final int     K_SWEEP_WARN         = 2;     //(사용자) 굴착 경고 턴
	public static final float   K_COLLAPSE_SPREAD    = 0.4f;  //(사용자, 처음 0.6) 굴 폭파: 칸마다 무작위로 늦게 무너지는 최대 시간(초)
	public static final int     K_SWEEP_DMG_MIN      = 18;    //(제안) 꼬리 휩쓸기 피해 (방어 적용) + 띠 밖으로 밀침
	public static final int     K_SWEEP_DMG_MAX      = 26;
	public static final int     K_SWEEP_PUSH         = 2;     //(제안) 밀치는 칸 수
	public static final int     K_MULTI_CD           = 14;    //(제안) 연속 솟구치기 간격
	public static final int     K_MULTI_STAY         = 7;     //(제안) 연속 솟구치기 뒤 땅 위에 머무는 턴 (가장 긴 딜 구간)
	//(J8) 새끼 굴벌레·공명 수정 (13·14층, 모두 제안 값)
	public static final boolean J8_ENABLED           = true;  //13·14층에 자연 배치
	public static final int     J8_WORMS_MIN         = 1;     //층마다 새끼 굴벌레 수
	public static final int     J8_WORMS_MAX         = 2;
	public static final int     J8_WORM_HP           = 45;
	public static final int     J8_CHARGE_RANGE      = 6;     //주인공이 같은 직선(8방향) 이 칸 수 안에 있으면 돌진을 예고
	public static final int     J8_CHARGE_CD         = 6;     //돌진 뒤 다시 예고하기까지 턴
	public static final int     J8_CHARGE_DMG_MIN    = 12;    //돌진 길 위의 주인공이 받는 피해
	public static final int     J8_CHARGE_DMG_MAX    = 20;
	public static final float   J8_WALL_STUN         = 1f;    //벽에 박으면 멈추는 턴
	public static final int     J8_GNAW_RANGE        = 6;     //이 칸 수 안의 (주인공이 보는) 살아 있는 광맥을 갉으러 간다
	public static final int     J8_GNAW_TURNS        = 3;     //다 먹기까지 턴 (그동안 무방비)
	public static final float   J8_GNAW_HEAL         = 0.3f;  //다 먹으면 최대 체력의 이만큼 회복
	public static final int     J8_GNAW_SHELL        = 10;    //다 먹으면 생기는 껍질(방어막)
	public static final int     J8_GNAW_MANA         = 15;    //갉는 동안 잡으면 돌려받는 마나 구슬
	public static final int     J8_CRYSTALS          = 1;     //층마다 공명 수정 수
	public static final int     J8_CRYSTAL_HP        = 30;
	public static final int     J8_LINK_RANGE        = 4;     //공명 수정이 이어지는 거리
	public static final int     J8_LINK_MAX          = 4;     //이어지는 적 수
	public static final int     J8_LINK_SHIELD       = 10;    //이어진 적의 방어막 (깨지면 다시 채움)
	public static final float   J8_BREAK_STUN        = 2f;    //공명 수정이 부서지면 이어진 적이 기절하는 턴
	public static final int     I2_INVESTIGATE_LOOK  = 2;     //(결정) 열리거나 부서진 감방 문에 와서 안쪽을 비춰 보는 턴
	public static final int     I2_WAYPOINT_WAIT     = 1;    //순찰 지점에 닿으면 이 턴 동안 멈춰 다음 길을 본다
	public static final int     H5_TALISMAN_RANGE    = 4;  //주인공이 이 칸 수 안에 보이면 던진다 (원작 소환 거리와 같음)

	// (B5) 스킬 형태 특성의 규칙 (구현 지시서 부록 2)
	public static final int   B5_FORM_MAX            = 2;     //2차에서 형태 특성은 이 개수까지만 고른다 (셋째부터 선택지에 안 나옴)
	public static final int   B5_FORM_MANA_COST      = 5;     //형태 특성(1레벨 이상, 켜진 것) 하나마다 스킬 마나 소모 +
	public static final float B5_EXTRA_HIT_MAX       = 0.8f;  //덧타격은 본 타격보다 약하다: 본 타격 배율에 곱하는 비율의 상한

	//(2026-11-24) 예전 2차 특성 29개의 수치는 지웠다. 새 전사·사냥꾼 특성 수치는 C·D 단계에서 넣는다.


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

	//만개의 핵으로 고를 수 있는 3차 전직
	public static HeroSubClass[] fullBloomPaths( HeroClass cls ){
		if (cls == HeroClass.WARRIOR) return new HeroSubClass[]{HeroSubClass.BLADEDANCER, HeroSubClass.DUELMASTER}; //(S1) 검무가 / 결투가 (예전 검투사는 지움)
		if (cls == HeroClass.HUNTRESS) return new HeroSubClass[]{HeroSubClass.BATTLEHUNTER, HeroSubClass.FORESTWARDEN}; //(S1) 전투 사냥꾼 / 숲지기 (예전 고리지기·추적자는 지움)
		return new HeroSubClass[0];
	}

	//3차 특성 최대 레벨 (0레벨 = 효과 없음, 1~4레벨)
	public static final int T3_MAX_TALENT_LEVEL = 4;

	// ===== (지시서 1127) 새 3차 전직 넷. 모두 임시값, 특성 배열은 [0레벨, 1, 2, 3, 4레벨] =====
	// 검무가 (전사, 근접 무기): 걸어 도착한 칸 둘레의 적을 스치며 벤다, 걸을수록 흐름이 쌓인다
	public static final float   T3B_GRAZE_BASE       = 0.30f;  //스침 피해 = 평타 × (이 값 + 흐름 × 흐름 하나의 몫)
	public static final float   T3B_GRAZE_PER_FLOW   = 0.10f;  //흐름 하나가 더하는 스침 피해 (평타 대비)
	public static final int     T3B_FLOW_MAX         = 5;      //흐름 최대
	public static final int     T3B_FLOW_DECAY       = 2;      //걷지 않은 턴마다 줄어드는 흐름
	public static final float[] T3B_FLOW_EDGE        = {0f, 0.02f, 0.04f, 0.06f, 0.08f};   //결 흐름: 흐름 하나의 몫에 더함
	public static final float[] T3B_DANCING_FEET     = {0f, 0.03f, 0.06f, 0.09f, 0.12f};   //춤추는 발: 흐름 하나마다 회피
	public static final float[] T3B_FINISHING_BLADE  = {0f, 0.05f, 0.10f, 0.15f, 0.20f};   //끝맺는 칼: 쓴 흐름 하나마다 스킬 피해
	public static final int[]   T3B_FLOWING_STEP_CD  = {0, 8, 6, 4, 2};                     //이어지는 걸음: 다시 켜지기까지 턴
	// 결투가 (전사, 무기 무관): 적 하나와 결투해 그 적에게 강해지고 다른 적에게 약해진다
	public static final float[] T3B_DUEL_BONUS       = {0.15f, 0.20f, 0.25f, 0.30f, 0.35f}; //정면 승부: 결투 상대 피해 +, 다른 적 피해 − (0레벨 = 기본)
	public static final float[] T3B_DUEL_BONUS_BOSS  = {0.07f, 0.1025f, 0.135f, 0.1675f, 0.20f}; //  결투 상대가 보스일 때
	public static final int     T3B_DUEL_TURNS       = 15;     //결투가 이어지는 턴
	public static final int     T3B_DUEL_COOLDOWN    = 10;     //넘어갈 적이 없어 끝나면 다시 걸기까지
	public static final int     T3B_DUEL_COOLDOWN_WIN = 5;     //  마지막 결투를 이겨서 끝났으면
	public static final float   T3B_DUEL_WIN_HP      = 0.06f;  //승리: 최대 체력의 이만큼 회복
	public static final int     T3B_DUEL_WIN_MANA    = 10;     //승리: 마나
	public static final float[] T3B_LOCKED_EYES      = {0f, 0.06f, 0.12f, 0.18f, 0.24f};   //맞선 눈: 결투 상대에게 받는 피해 −
	public static final float[] T3B_VICTOR_HP        = {0f, 0.02f, 0.04f, 0.06f, 0.08f};   //승자의 숨: 승리 체력 +
	public static final int[]   T3B_VICTOR_MANA      = {0, 5, 10, 15, 20};                  //승자의 숨: 승리 마나 +
	public static final int[]   T3B_LONG_DUEL        = {0, 3, 6, 9, 12};                    //긴 승부: 결투 시간 +턴
	// 전투 사냥꾼 (사냥꾼, 활 전용): 옆 칸에도 화살을 쏘고, 따로 된 단추로 활대 타격, 사거리는 절반
	public static final float[] T3B_POINT_BLANK_1    = {0f, 0.10f, 0.20f, 0.30f, 0.40f};   //코앞 사격: 1칸 화살 피해 +
	public static final float[] T3B_POINT_BLANK_2    = {0f, 0.06f, 0.12f, 0.18f, 0.24f};   //코앞 사격: 2칸 화살 피해 +
	public static final int[]   T3B_RETURN_FIRE      = {0, 2, 3, 4, 5};                     //맞불: 2칸 안의 적을 맞힐 때마다 방어막
	public static final int[]   T3B_RETURN_FIRE_CAP  = {0, 8, 12, 16, 20};                  //맞불: 방어막 상한
	public static final float[] T3B_POINT_BLANK_ACC  = {0f, 0.10f, 0.20f, 0.30f, 0.40f};   //코앞 사격 II: 2칸 안 화살 명중 +
	public static final int[]   T3B_STEP_BACK        = {0, 2, 3, 4, 5};                     //발 빼기: 활대 타격으로 밀쳐낸 적 느려짐 턴
	// 숲지기 (사냥꾼, 무기 무관): 걸은 자리·화살이 떨어진 자리에 높은 풀이 자라고, 풀 속에서 넣는 공격은 기습
	public static final float[] T3B_SPREADING_GROVE  = {0f, 0.15f, 0.30f, 0.45f, 0.60f};   //번지는 숲: 옆 칸에도 함께 자랄 확률
	public static final float[] T3B_LEAF_ARROW       = {0f, 0.08f, 0.16f, 0.24f, 0.32f};   //풀잎 화살: 풀 속 기습 피해 +
	public static final float[] T3B_GROVE_EMBRACE    = {0f, 0.06f, 0.12f, 0.18f, 0.24f};   //숲의 품: 풀 속에서 받는 근접 피해 − (4레벨: 높은 풀이 주인공 시야를 가리지 않음)
	public static final int[]   T3B_ROOT_STEP        = {0, 1, 2, 3, 4};                     //뿌리 걸음: 높은 풀에 들어온 적 느려짐 턴

	// ===== 4차 전직 (결실) =====

	//20층 보스(드워프 왕)가 결실의 핵을 떨어뜨리는지 (왕의 왕관 대신)
	public static final boolean DWARF_KING_DROPS_FRUIT_CORE = true;

	//(T1, 지시서 claude_code_tasks_1126b_pets) 결실의 핵으로 고를 수 있는 4차 = 펫. 직업과 상관없이 같다.
	//예전 4차 능력(영웅의 도약·인내·정령 매 능력)은 지웠다(원작 코드로 되돌림).
	public static com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] fruitPaths( HeroClass cls ){
		return new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[]{
				new com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetHawk(),
				new com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetBear()};
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

	// ===== 4차 펫 (지시서 claude_code_tasks_1126b_pets) =====
	//(사용자) 펫은 20층을 넘긴 뒤 얻으므로 21~24층 몹에 맞춘다. 21~24층 몹(원작 수치): 체력 50~110, 한 대 피해 12~40,
	//명중 30~40, 회피 20~25, 방어 0~16. 그 무렵 주인공은 레벨 21~25, 최대 체력 약 120~140. 수치는 모두 (제안).
	//소환: 갑옷 충전을 이만큼 쓴다(예전 정령 매 능력과 같은 35). 펫이 나와 있으면 능력 단추는 지시가 되고 충전을 쓰지 않는다.
	//(사용자) 펫마다 강화 넷째 칸은 원작 "영웅적인 에너지"(갑옷 충전 소모 12/23/32/40% 감소, 1~4레벨)
	public static final float PET_SUMMON_CHARGE = 35f;

	//(사용자) 기본 수치는 모두 21~24층 몹보다 살짝 강하게 (몹 위쪽: 체력 80~110, 피해 25~40, 명중 36~40, 회피 24~25, 방어 0~16)
	//정령 매 (견제): 회피가 아주 높다(적 명중 30~40이면 약 30%만 맞음)
	public static final int   PET_HAWK_HP      = 100;
	public static final int   PET_HAWK_DEF     = 60;   //회피 (사용자) 방어가 없는 대신 높게
	public static final int   PET_HAWK_ACC     = 60;   //명중
	public static final int   PET_HAWK_DMG_MIN = 20;
	public static final int   PET_HAWK_DMG_MAX = 28;
	public static final int   PET_HAWK_VIEW    = 6;    //매의 시야(주인공 시야에 더해짐)
	public static final float PET_HAWK_SPEED   = 2f;
	public static final int   PET_HAWK_RANGE   = 2;    //(T3) 멀리서 쪼기: 이 칸 안의 가로막히지 않은 적 (사용자 2칸)
	public static final float PET_HAWK_DIVE    = 0.6f; //  쫄 때 적 쪽으로 날아드는 비율 (연출)
	public static final float PET_HAWK_DIVE_TIME = 0.09f; //  날아들기·돌아오기 시간(초)

	//이끼 곰 (수호): 체력·방어가 가장 높다
	public static final int   PET_BEAR_HP      = 180;
	public static final int   PET_BEAR_DEF     = 20;   //회피
	public static final int   PET_BEAR_ACC     = 42;   //명중
	public static final int   PET_BEAR_DMG_MIN = 26;
	public static final int   PET_BEAR_DMG_MAX = 36;
	public static final int   PET_BEAR_DR_MIN  = 6;    //방어
	public static final int   PET_BEAR_DR_MAX  = 16;
	public static final int   PET_BEAR_ROAR_EVERY = 10; //포효 간격(싸우는 동안의 곰 턴)
	public static final int   PET_BEAR_ROAR_RANGE = 2;  //포효가 닿는 거리
	public static final int   PET_BEAR_ROAR_TURNS = 3;  //포효가 시선을 끄는 기본 턴 (강화 PET_ROAR_TURNS가 더함)
	public static final int   PET_BEAR_SHARE_RANGE = 1; //대신 받기: 주인공이 곰의 이 칸 안에 있을 때 (사용자 1칸)
	public static final float PET_BEAR_SHARE   = 0.25f; //대신 받는 기본 비율 (강화 PET_SHARE_RATIO가 더함)

	//마지막 근위 (맞받아침)
	public static final int   PET_GUARD_HP      = 130;
	public static final int   PET_GUARD_DEF     = 30;   //회피
	public static final int   PET_GUARD_ACC     = 48;   //명중
	public static final int   PET_GUARD_DMG_MIN = 28;
	public static final int   PET_GUARD_DMG_MAX = 38;
	public static final int   PET_GUARD_DR_MIN  = 4;    //방어
	public static final int   PET_GUARD_DR_MAX  = 14;
	public static final int   PET_GUARD_STANCE_TURNS = 3; //집중 자세가 이어지는 근위 턴 (공격 하나를 흘리면 풀림)
	public static final int   PET_GUARD_STANCE_EVERY = 8; //집중 자세 기본 간격 (강화 PET_STANCE_CUT이 줄임)
	public static final float PET_GUARD_REQUITE = 1.5f;   //반격 기본 배율 = 근위 평타 × (강화 PET_REQUITE_DMG가 더함)

	//길든 주문 (원소): 적이 노리지 않는다
	public static final int   PET_SPELL_HP    = 110;
	public static final int   PET_SPELL_DEF   = 50;   //회피 (사용자) 방어가 없는 대신 높게
	public static final float PET_SPELL_CHILL = 3f;   //묻히는 냉기의 기본 시간 (정전기는 원작 STATIC_TURNS, 화상은 원작 기본)
	public static final float PET_SPELL_WALL_STEAM = 1.5f; //(사용자) 냉기 주문이 불의 벽에 닿을 때 받는 증기 피해 = 원소 반응 기준 피해 × 이만큼
	public static final int   PET_SPELL_ACC     = 48;   //명중
	public static final int   PET_SPELL_DMG_MIN = 18;    //평타(탄) 피해
	public static final int   PET_SPELL_DMG_MAX = 26;
	public static final int   PET_SPELL_RANGE   = 2;    //(사용자) 평타(탄)가 닿는 거리
	public static final int   PET_SPELL_SKILL_RANGE = 3; //원소 탄(스킬)이 닿는 거리
	public static final int   PET_SPELL_SKILL_EVERY = 5;   //원소 탄(스킬) 간격 (주문 턴)
	public static final float PET_SPELL_SKILL_DMG   = 2.0f; //원소 탄 피해 = 평타 × 이만큼 + 원소
	public static final int   PET_SPELL_SHIFT_CD = 30;  //원소를 다시 바꾸기까지 기본 턴 (강화 PET_SHIFT_CUT이 줄임)
	public static final int   PET_SPELL_LEASH   = 2;    //(사용자) 명령이 없을 때 주인공에게서 이 칸 안에서 돌아다님
	public static final int   PET_LEASH = 1; //(사용자) 정령 매·이끼 곰·마지막 근위가 명령이 없을 때 맴도는 거리 (주인공 둘레)

	//(T2) 펫 강화. 배열은 [0레벨, 1, 2, 3, 4레벨]. (사용자) 넷 모두 4레벨까지(4차 포인트 최대 11 / 다 채우려면 16).
	//(사용자) 0레벨은 아무 효과가 없다: 모든 배열의 0레벨 값은 0이고, 펫의 기본 동작 수치는 위의 기본값(PET_*_BASE 등)이 정한다.
	//정령 매
	public static final float[] PET_BEAK_DMG    = {0f, 0.15f, 0.30f, 0.45f, 0.60f}; //날카로운 부리: 쪼는 피해 +
	public static final int[]   PET_EYE_VIEW    = {0, 1, 2, 3, 4};                  //매의 눈: 매 시야 반경 +
	public static final float[] PET_FLAP_ACC    = {0f, 0.10f, 0.15f, 0.20f, 0.25f}; //성가신 날갯짓: 쫀 적 명중 -
	public static final int[]   PET_FLAP_TURNS  = {0, 2, 3, 4, 5};                  //  그 턴
	//이끼 곰
	public static final float[] PET_HIDE_BONUS  = {0f, 0.15f, 0.30f, 0.45f, 0.60f}; //두꺼운 가죽: 체력·방어 +
	public static final float[] PET_SHARE_RATIO = {0f, 0.05f, 0.10f, 0.15f, 0.20f}; //대신 받기: 대신 받는 비율 + (기본 PET_BEAR_SHARE)
	public static final int[]   PET_ROAR_TURNS  = {0, 1, 2, 3, 4};                  //포효: 시선을 끄는 턴 + (기본 PET_BEAR_ROAR_TURNS)
	//마지막 근위
	public static final int[]   PET_STANCE_CUT   = {0, 1, 2, 3, 4};                  //곧은 자세: 집중 자세 간격 -턴 (기본 PET_GUARD_STANCE_EVERY)
	public static final float[] PET_REQUITE_DMG  = {0f, 0.30f, 0.60f, 0.90f, 1.20f}; //되갚는 칼: 반격 배율 + (기본 PET_GUARD_REQUITE)
	public static final float[] PET_OATH_BONUS   = {0f, 0.15f, 0.30f, 0.45f, 0.60f}; //굳은 맹세: 체력·방어 +
	//길든 주문
	public static final float[] PET_ESSENCE_BURN  = {0f, 0.15f, 0.30f, 0.45f, 0.60f}; //짙은 원소: 화상 피해 +
	public static final int[]   PET_ESSENCE_TURNS = {0, 1, 2, 3, 4};                  //  냉기·정전기 시간 +턴
	public static final int[]   PET_SHIFT_CUT     = {0, 6, 12, 18, 24};               //빠른 변주: 원소 바꾸기 대기 -턴 (기본 PET_SPELL_SHIFT_CD)
	public static final float[] PET_TOUCH_CHANCE  = {0f, 0.10f, 0.20f, 0.30f, 0.40f}; //스며드는 손길: 평타로 원소를 묻힐 확률

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
	public static final float SAP_PHYS_REDUCE     = 0.60f; //(G1) 물리 피해 감소 (불타거나 쪼그라든 동안은 없음)
	public static final float SAP_HALF_PHYS_BONUS = 0.30f; //(예전 규칙, 지금은 안 씀) 체력 절반 아래 물리 피해 추가
	public static final float SAP_BURN_UNTIL      = 0.40f; //불이 붙으면 체력이 이 비율이 될 때까지 계속 탄다
	public static final float[] SAP_STAGE_HP      = {0.80f, 0.65f, 0.50f}; //체력이 이 아래면 s1 / s2 / s3 (쪼그라든 모습)
	public static final int   SAP_STAGE_STEP_TURNS = 2;    //쪼그라드는 단계 하나에 머무는 턴 (단계를 건너뛰지 않음)
	public static final int   SAP_DROPS           = 6;     //(G2) 다 쪼그라들면(s4) 보스방에 흩어지는 수액 방울(몹 SapDroplet) 수
	public static final int   SAP_DROP_BOSS_GAP   = 2;     //(G2) 보스에서 이 칸 수 안에는 방울이 생기지 않는다
	public static final int   SAP_DROP_GAP        = 3;     //(G2) 다른 방울에서 이 칸 수 안에는 생기지 않는다
	public static final int   SAP_DROP_GAP_MIN    = 1;     //(G2) 자리가 모자라면 이 칸 수까지 줄인다
	public static final int   SAP_DROPS_TO_HEAL   = 3;     //(G3) 방울을 이만큼 흡수하면 기본 모습으로 돌아온다 (흡수할 때마다 한 단계 부풂)
	public static final int   BOSS_TORCHES        = 2;     //5층 보스층에 떨어져 있는 횃불 수 (비밀방 제외 무작위 바닥)
	public static final int   SAP_PUDDLE_COUNT    = 6;     //(G4) 힘 모아 내리칠 때 범위 안에 생기는 수액 웅덩이 칸 수
	public static final int   SAP_PUDDLE_RANGE    = 2;     //(G4) 그 범위 (내려치기 경고와 같은 2칸)
	public static final int   SAP_PUDDLE_TURNS    = 30;    //수액 웅덩이 지속
	public static final int   SAP_MOVE_HEAL       = 10;    //수액 칸으로 옮기면 회복

	public static final int   TORCH_FIRE            = 4;   //횃불: 떨어진 자리에 붙는 불의 양

	// ===== 20층 보스 (명세 22번). 정해지지 않은 수치는 원작 드워프 왕(체력 300, 근접 15~25)을 기준으로 잡은 임시값 =====
	//(결정, 2026-11-21 밸런스) 체력은 보스마다 따로: 망령 왕 WK_HP, 여제 EM_HP (예전 공통 B20_HP 300 / 450)
	public static final int   B20_EXP           = 40;
	public static final int   B20_DEF           = 20;    //(결정, 바뀜 22→20) 회피 (여제)
	public static final int   B20_ACC           = 26;    //명중
	public static final int   B20_DR            = 10;    //방어 (0~10)
	public static final float B20_PHASE_DROP    = 0.01f; //단계가 넘어가는 피해는 다음 단계 최대 체력보다 이만큼 낮은 체력에서 멈춘다
	public static final int   B20_DMG_MIN       = 15;    //망령 왕 근접 피해
	public static final int   B20_DMG_MAX       = 25;

	//망령 왕
	public static final float WK_PHASE2_HP      = 0.70f; //체력이 이 아래면 2단계 (어둠)
	public static final float WK_PHASE3_HP      = 0.10f; //(결정, 바뀜 25%→10%) 체력이 이 아래면 3단계 (깨진 왕관, 기어다님)
	public static final int   WK_ORB_INTERVAL   = 4;     //1단계: 이 턴마다 영혼 구슬
	public static final int   WK_ORB_COUNT      = 2;     //한 번에 떨어지는 구슬 수
	public static final float WK_SOUL_ATK       = 0.05f; //영혼 1개당 공격력
	public static final int   WK_SOUL_GUARDS    = 5;     //(결정, 바뀜 6→5) 영혼이 이만큼 이상이면 어두워질 때 근위병 3마리
	public static final int   WK_SOUL_NOWARN    = 11;    //(결정, 바뀜 12→11) 영혼이 이만큼 이상이면 인간을 죽이는 마법 경고 없음
	public static final int   WK_SOUL_RAGE      = 16;    //(결정, 바뀜 18→16) 영혼이 이만큼 이상이면 공격력 추가
	public static final float WK_RAGE_ATK       = 1.0f;  //위의 추가 공격력 (+100%)
	public static final int   WK_SOUL_DEATH     = 30;    //(결정) 영혼이 이만큼 모이면 주인공 즉사
	public static final int   WK_DARK_VIEW      = 1;     //어둠 속 주인공 시야
	public static final int   WK_LAMPS          = 3;     //기억의 등불 수
	public static final int   WK_LAMP_HERO_GAP  = 2;     //등불은 처음 생길 때 주인공 둘레 이 칸 안에는 생기지 않는다
	public static final int   WK_LAMP_GAP       = 4;     //등불끼리 이 칸 수 이상 떨어진다 (서로 3칸 안에는 생기지 않음)
	public static final int   WK_LAMP_TURNS     = 25;    //(결정, 바뀜 50→35→25) 등불이 켜져 있는 턴
	public static final int   WK_HP             = 600;   //(결정, 밸런스) 체력 300 → 600
	public static final int   WK_HP_STRONG      = 900;   //도전 "더 강한 보스" (450 → 900)
	//(결정, 밸런스) 기본 공격 배율을 둘로 나눔 (예전 WK_BASIC_MULTI 0.4 공통)
	public static final float WK_MELEE_MULTI    = 1.33f; //바로 옆 근접 (방어 적용): 15~25 × 1.33 = 20~33 ((결정, 밸런스 1d) 1.1 → 1.33)
	public static final float WK_ARROW_MULTI    = 0.56f; //떨어진 주인공에게 영혼 화살 (방어 무시): 8~14
	public static final float WK_AMBUSH_MULTI   = 1.0f;  //기습 일격 (× WK_AMBUSH_DMG)
	public static final int   WK_DEF            = 17;    //(결정, 바뀜 18→17) 망령 왕 회피
	public static final float WK_COUNTER_MIN    = 0.10f; //(결정, 바뀜 60~70%→10%) 기습 중 푸른 눈을 쳐서 숨은 왕에게 넘어가는 피해 비율 (90% 감소)
	public static final float WK_COUNTER_MAX    = 0.10f;
	public static final float WK_LAMP_STUN      = 3f;    //등불이 켜지면 왕 기절
	public static final int   WK_ZOLT_INTERVAL  = 15;    //졸트라크 쿨타임
	public static final int   WK_ZOLT_CHARGE    = 1;     //(결정, 바뀜 2→1) 졸트라크 기 모으기 턴
	public static final int   WK_ZOLT_DARK_CUT  = 4;     //(결정, 바뀜 2→4) 어둠 속에서는 인간을 죽이는 마법 쿨타임이 이만큼 짧다 (15 → 11)
	public static final int   WK_ZOLT_MIN       = 35;    //(결정, 밸런스) 25 → 35    //졸트라크 피해 (영혼 강화 전)
	public static final int   WK_ZOLT_MAX       = 56;    //(결정, 밸런스) 40 → 56
	public static final int   WK_GUARD_INTERVAL = 7;     //근위병 소환 쿨타임
	public static final int   WK_DARK_GUARDS    = 3;     //어두워질 때 추가 근위병 (영혼 5개 이상)
	public static final int   WK_AMBUSH_INTERVAL = 3;    //기습 쿨타임 (어두운 동안)
	public static final float WK_AMBUSH_ARMOR_IGNORE = 0.5f; //기습 일격은 주인공 방어력의 이만큼을 무시한다
	public static final float WK_AMBUSH_DMG     = 1.3f;  //(결정) 기습 일격 피해 배율 (+30%)
	public static final float WK_ZOLT_ARMOR_IGNORE = 0.7f; //(결정) 인간을 죽이는 마법은 주인공 방어력의 이만큼을 무시한다
	public static final float WK_EYES_ALPHA     = 0.25f; //(결정, 바뀜 35%→25%) 어둠 속 푸른 눈의 진하기 (원작 미믹처럼 알아채기 어렵게)
	//(결정, 2026-11-21) 1→2단계 "어둠이 찾아옴" 연출: 밝은 원이 왕 쪽으로 좁혀지는 시간, 완전히 깜깜한 시간, 마지막 원 반지름(픽셀)
	//(결정, 바뀜) 더 빠르게: 2초 → 1초 → 0.6초, 0.7초 → 0.35초 → 0.2초, 눈 빛남은 0.6배 → 0.5배 (연출 중 주인공은 행동하지 않는다)
	public static final float WK_DARK_SHRINK_TIME = 0.6f;
	public static final float WK_DARK_BLACK_TIME  = 0.2f;
	public static final float WK_DARK_EYE_SPEED   = 0.5f;
	public static final float WK_DARK_LAST_R      = 20f;
	public static final int   WK_TP_ORB_MIN     = 2;     //(결정) 1단계에 맞아서 순간이동할 때: 영혼 구슬에서 이 칸 이상
	public static final int   WK_TP_ORB_MAX     = 4;     //... 이 칸 이내
	public static final float WK_PHASE3_TAKEN   = 1.5f;  //3단계: 받는 평타·스킬 피해
	public static final float WK_CRAWL_SPEED    = 0.7f;  //(결정) 3단계 기어다니는 왕의 이동 속도 (주인공에게서 도망친다)
	//망령 근위병 (원작 드워프 왕 부하 구울을 조금 약하게)
	public static final int   WG_HP             = 40;
	public static final int   WG_DMG_MIN        = 14;    //(결정, 밸런스) 10 → 14
	public static final int   WG_DMG_MAX        = 22;    //(결정, 밸런스) 16 → 22
	public static final int   WG_ACC            = 25;
	public static final int   WG_DEF            = 8;     //(결정, 바뀜 15→8) 회피
	public static final int   WG_DR             = 4;
	public static final float WG_SPEED          = 2f;    //한 턴에 2칸
	public static final int   WG_DARK_SENSE     = 2;     //(2026-11-22 사용자) 어둠 속에서 주인공을 알아채는 거리(칸). 발소리는 듣지 못하고 던진 물건 소리에만 모인다

	//마도시의 여제
	public static final int   EM_HP             = 540;   //(결정, 2026-11-21 밸런스) 체력 300 → 540
	public static final int   EM_HP_STRONG      = 810;   //도전 "더 강한 보스" (450 → 810)
	public static final float EM_PHASE2_HP      = 0.85f;
	public static final float EM_PHASE3_HP      = 0.30f;
	public static final float EM_PETAL_TAKEN    = 0.5f;  //1단계 꽃잎의 보호: 받는 피해
	public static final float EM_PHASE3_TAKEN   = 1.5f;  //3단계 시든 꽃관: 받는 피해
	public static final int   EM_TELEPORT_HITS  = 2;     //(결정, 바뀜 4→2) 피해를 연속으로 이만큼 받으면 순간이동
	public static final int   EM_HIT_RESET_TURNS = 2;    //이 턴 동안 맞지 않으면 연속 횟수가 처음부터
	public static final int   EM_BOLT_INTERVAL  = 3;     //강화 마탄 쿨타임
	public static final int   EM_BOLT_MIN       = 14;    //(결정, 밸런스) 10 → 14
	public static final int   EM_BOLT_MAX       = 22;    //(결정, 밸런스) 16 → 22
	public static final int   EM_RAIN_INTERVAL  = 7;     //전기비 쿨타임
	public static final int   EM_RAIN_CELLS     = 10;    //(결정, 바뀜 5→10) 솟는 칸 수 (주인공 칸 + 둘레 6×6 안 무작위)
	public static final int   EM_RAIN_AREA      = 6;     //전기비 범위: 주인공 둘레 6×6칸
	public static final int   EM_RAIN_RADIUS    = 2;
	public static final int   EM_RAIN_MIN       = 11;    //(결정, 밸런스) 8 → 11
	public static final int   EM_RAIN_MAX       = 17;    //(결정, 밸런스) 12 → 17
	public static final float EM_RAIN_STATIC    = 3f;    //정전기 턴
	public static final int   EM_SPEAR_INTERVAL = 15;    //서리창·화염창 번갈아 (각각 30턴마다)
	public static final int   EM_SPEAR_DELAY_MIN = 3;    //창이 머리 위에 뜬 뒤 날아가기까지 (3~4턴, 무작위)
	public static final int   EM_SPEAR_DELAY_MAX = 4;
	//(결정, 2026-11-21) 서리창·화염창 조준선(조준선·창 겨누기·발밑 표식)을 보여 주는 남은 턴: 날아오기 2턴 전부터
	public static final int   EM_SPEAR_AIM_TURNS = 2;
	public static final int   EM_SPEAR_MIN      = 21;    //(결정, 밸런스) 15 → 21
	public static final int   EM_SPEAR_MAX      = 31;    //(결정, 밸런스) 22 → 31
	public static final float EM_SPEAR_FROSTBITE = 3f;   //서리창 혹한
	public static final float EM_TILE_FILL      = 0.90f; //원소 감응: 타일 칸(132칸)의 90% = 118칸
	public static final float EM_TILE_REFILL    = 0.80f; //(결정, 완화 65%→80%) 이 아래(106칸)면 다시 깐다
	public static final float EM_SENSING_TIME   = 0.5f;  //원소 감응에 드는 시간 (턴)
	public static final int   EM_TILE_STEAM_MIN = 14;    //원소 타일 위 주인공 증기 반응 피해 (폭발은 기존 폭발 반응 피해)
	public static final int   EM_TILE_STEAM_MAX = 22;    //(결정, 밸런스) 10~16 → 14~22
	public static final int   EM_STORM_WARN     = 4;     //원소 폭풍 경고 턴 (2026-11-22 사용자: 5 → 3 → 4)
	public static final int   EM_STORM_FIRST_WARN = 2;   //처음 원소 폭풍만 경고 2턴 (2026-11-22 사용자: 3 → 2)
	public static final int   EM_STORM_TURNS    = 4;     //원소 폭풍 턴 (2026-11-22 사용자: 5 → 3 → 4)
	public static final int   EM_STORM_MIN      = 8;     //폭풍 구역 안 주인공 피해 (턴마다)
	public static final int   EM_STORM_MAX      = 14;    //(결정, 밸런스) 6~10 → 8~14

	// ===== 25층 최종보스: 탑의 주인 "탑의 심장" (기획 docs/boss25_heart_plan.md, 조각 O1~O7) =====
	//켜면 25층이 새 보스방(MasterBossLevel)이 된다. 끄면 지금처럼 원작 25층(또는 HEART_BOSS면 심장을 진 짐승)
	public static final boolean O_MASTER     = true;
	public static final int   O_MASTER_HP    = 1000;  //(결정)
	public static final int   O_MASTER_EXP   = 50;
	public static final int   O_MASTER_DEF   = 20;    //회피 (제안, 지난 25층 짐승과 같음)
	public static final int   O_MASTER_ACC   = 30;    //명중 (제안)
	public static final int   O_MASTER_DR    = 10;    //방어 0~10 (제안)
	public static final int   O_MASTER_DMG_MIN = 20;  //옆 칸 평타: 지팡이로 바닥을 짚음 (제안, O1 임시)
	public static final int   O_MASTER_DMG_MAX = 30;
	public static final int   O_VALVE_HP     = 60;    //심장 판막 체력 (제안)
	public static final float O_VALVE_WARD   = 0.7f;  //(사용자) 판막의 보호: 1·2막에 판막이 하나도 깨져 있지 않으면 주인이 받는 피해 감소 (3막에는 없음)
	//(O4, 사용자) 다시 닫기: 깨진 판막은 이 턴 동안 검게 일렁인 뒤 주인이 순간이동해 O_RECLOSE_TURNS턴 수리, 다시 선 판막 체력 몫
	public static final int   O_RECLOSE_DELAY = 9;   //(사용자, 1.5배) 깨진 뒤 이 턴이 지나면 그루터기가 검게 일렁이기 시작
	public static final int   O_RECLOSE_WARN  = 12;  //(사용자, 1.5배) 일렁이기 시작하고 이 턴을 다 채우면 주인이 순간이동해 수리 시작
	public static final int   O_RECLOSE_GAP   = 12;  //(사용자, 1.5배) 하나를 다 닫은 뒤 이 턴이 지나야 다음 판막이 일렁이기 시작 (그 뒤 다시 O_RECLOSE_WARN턴)
	public static final int   O_HOP_LEN       = 4;   //(사용자) 판막까지 가고 올 때 한 번에 순간이동하는 거리(칸, 대략)
	public static final int   O_HOP_MAX       = 3;   //(사용자) 거리에 따라 최대 이만큼 끊어서 순간이동 (한 턴에 한 번)
	public static final int   O_RECLOSE_TURNS = 6;   //(사용자, 1.5배)
	public static final float O_RECLOSE_HP    = 0.5f;
	//(O3) 1막 기술 (모두 제안, MasterSkills). 한 턴에 하나, 모두 1턴 전에 예고
	public static final int   O_ROOT_SPEAR_CD     = 4;   //뿌리 창: 주인공을 지나는 한 줄 바닥 금 → 검은 뿌리
	public static final int   O_ROOT_SPEAR_MIN    = 16;
	public static final int   O_ROOT_SPEAR_MAX    = 24;
	public static final int   O_ROOT_SPEAR_MINLEN = 5;   //이 칸 수보다 짧은 줄은 고르지 않는다
	public static final int   O_FAN_CD            = 4;   //(사용자) 가지 부채 쿨타임 3 → 4
	public static final int   O_FAN_RANGE         = 4;   //(사용자) 넓힘 3 → 4
	public static final float O_FAN_HALF          = 40f; //(사용자) 부채꼴 양옆 각도: 멀어질수록 넓어지는 삼각형 (정면 1·3·5·7칸)
	public static final int   O_FAN_R2            = 25;  //(사용자) 부채꼴 둥근 거리 (가로² + 세로² 이하): 대각선일 때 먼 귀퉁이 (4,4)를 뺌
	public static final int   O_FAN_MIN           = 14;
	public static final int   O_FAN_MAX           = 20;
	public static final int   O_FAN_PUSH          = 3;   //(사용자) 맞으면 주인에게서 먼 쪽으로 밀침 (칸)
	public static final int   O_FAN_WALL_STUN     = 2;   //(사용자) 밀려서 벽에 부딪히면 기절 턴
	public static final int   O_FAN_WALL_MIN      = 8;   //(사용자) 벽에 부딪히면 추가 피해 (방어 무시, 값은 제안)
	public static final int   O_FAN_WALL_MAX      = 12;
	public static final int   O_ECHO_CD           = 6;   //메아리 기술(판막마다 하나) 쿨타임
	//(사용자) 주인의 기술 중 방어를 전혀 무시하지 않는 것(뿌리 창·가지 채찍·가지 부채·낙엽 걸음·수액 내려치기·뿌리 해일)은 피해의 이 몫(최대 O_SKILL_CHIP_MAX)이 방어를 무시하고 박힌다
	public static final float O_SKILL_CHIP        = 0.2f;
	public static final int   O_SKILL_CHIP_MAX    = 5;
	//(O5, 사용자) 순간이동할 때마다 (1·2막 모두) 낙엽 연출과 함께 내려앉은 칸 둘레(이 칸)에 검은 낙엽을 깐다 (새잎 땅 위에는 안 깔림)
	public static final int   O_HOP_LEAVES        = 1;
	//(O5) 2막 "걷는 나무": 체력이 이 몫 아래로 내려가면(한 번에 건너뛰지 않고 멈춤) 다음 주인 차례에 막이 바뀐다. 주인은 여전히 걷지 않는다(사용자)
	public static final float O_ACT2_AT           = 0.7f;
	public static final float O_ACT2_PAUSE        = 1.7f;  //막 전환 연출 동안 턴이 멈추는 시간(초, 전환 그림 13장 × 초당 8장)
	//(사용자) 가지 채찍: 2막에서 뿌리 창을 대신한다. 주인에게서 주인공을 지나 벽까지 곧은 줄 경고 → 검은 가지가 주인 쪽부터 솟아 피해, 그 칸에 검은 낙엽 (값은 제안)
	public static final int   O_WHIP_CD           = 4;
	public static final int   O_WHIP_MIN          = 16;
	public static final int   O_WHIP_MAX          = 24;
	public static final float O_WHIP_STAGGER      = 0.03f; //주인 쪽부터 한 칸씩 솟는 간격(초)
	//(사용자) 낙엽 걸음: 주인공이 검은 낙엽을 밟아 바스락 소리를 내면, 그 칸으로 순간이동해 온다 (낙엽이 있는 칸으로만). 경고 1턴(그 칸과 둘레 8칸) →
	//내려앉아 둘레 8칸에 검은 가지 (값은 제안). 주인공이 그 칸에 서 있으면 한 칸 밀려난다
	public static final int   O_LEAFWALK_CD       = 7;   //(사용자) 5 → 7
	public static final int   O_LEAFWALK_MIN      = 14;
	public static final int   O_LEAFWALK_MAX      = 20;
	public static final int   O_LEAFWALK_MINDIST  = 3;   //주인공과 이만큼 떨어져 있을 때만 (붙어 있으면 평타·가지 부채)
	public static final int   O_LEAFWALK_WARN     = 2;   //(사용자) 경고 턴 (그동안 주인은 다른 기술 없이 기다림, 머리 위에 남은 턴)
	//(사용자) 낙엽 바람: 방의 세로 또는 가로 절반(무작위) 경고 → 약한 피해 + 검은 낙엽 (값은 제안)
	public static final int   O_WIND_CD           = 12;  //(사용자) 길게 8 → 12
	public static final int   O_WIND_MIN          = 2;   //(사용자) 약하게, 방어 무시
	public static final int   O_WIND_MAX          = 10;
	public static final float O_WIND_LEAVES       = 0.8f;  //(사용자) 바람이 지나간 칸 중 검은 낙엽이 깔리는 몫 (0.35 → 0.8)
	public static final float O_WIND_SWEEP        = 0.45f; //바람이 그 절반의 한쪽 끝에서 다른 끝까지 지나가는 시간(초)
	public static final int   O_LEAF_WARN_COLOR   = 0xC07A3A; //낙엽 걸음·낙엽 바람 경고 칸 색 (빛바랜 낙엽빛)
	public static final float O_LEAF_WARN_ALPHA   = 0.4f;
	//(O6) 3막 "심장": 체력이 이 몫 아래로 내려가면(2막에서 멈춤) 다음 차례에 막이 바뀐다. 주인이 줄기 곁으로 돌아와 그때 살아 있던 판막을 모두 흡수한다
	//(판막은 재가 되고 기믹은 사라지며, 그 메아리 기술만 끝까지 남아 피해 O_ABSORB_MUL배). 깨져 있던 판막은 돌아오지 않고 다시 닫기도 없다. (사용자) 3막에서만 걷는다
	public static final float O_ACT3_AT           = 0.35f;
	public static final float O_ACT3_PAUSE        = 1.6f;  //막 전환 연출 동안 턴이 멈추는 시간(초)
	public static final float O_ABSORB_MUL        = 1.5f;
	//(사용자) 세계수의 열매: 새잎 땅 칸(없으면 줄기 바로 옆 칸)에 맺혀 익고, 먼저 밟는 쪽이 먹는다 (주인공과 주인이 다툼). 값은 모두 제안
	public static final int   O_FRUIT_FIRST       = 4;    //3막이 열리고 첫 열매가 맺히기까지 (주인 턴)
	public static final int   O_FRUIT_RIPEN       = 8;    //맺힌 뒤 익기까지
	public static final int   O_FRUIT_REGROW      = 8;    //누가 먹은 뒤 다음 열매가 맺히기까지
	public static final float O_FRUIT_HEAL        = 0.25f; //주인공이 먹으면: 최대 체력의 이만큼 회복
	public static final int   O_FRUIT_MANA        = 50;    //마나 회복
	public static final int   O_FRUIT_TURNS       = 10;    //그 뒤 이 턴 동안
	public static final float O_FRUIT_DMG         = 1.25f; //주인에게 입히는 피해 배율
	public static final float O_FRUIT_TAKEN       = 0.75f; //받는 피해 배율
	//뿌리 해일: 가장자리에서 좁혀 오는 고리 셋 (줄기 둘레 O_TIDE_SAFE 안은 늘 안전). 고리마다 경고 O_TIDE_WARN턴 → 솟음, 그때 다음 고리 경고. 값은 모두 제안
	public static final int   O_TIDE_CD           = 16;   //해일이 끝난 뒤부터
	public static final int   O_TIDE_FIRST        = 6;    //3막이 열리고 첫 해일까지
	public static final int   O_TIDE_WARN         = 2;
	public static final float[] O_TIDE_BANDS      = {5.5f, 3.5f, 1.5f}; //고리 안쪽 경계 (줄기에서의 거리): 바깥 고리 > 5.5, 가운데 > 3.5, 안쪽 > 1.5
	public static final int   O_TIDE_MIN          = 22;
	public static final int   O_TIDE_MAX          = 32;
	//(사용자 확인) 2막 주인 그림(등에서 가지가 크게 뻗음, 32×30, sprites/maze_b25_master2.png)
	public static final boolean O_MASTER2_ART     = true;
	//(O6, 사용자 확인) 세계수의 열매 그림(sprites/maze_b25_fruit.png)·3막 주인 그림(옹이가 갈라짐, sprites/maze_b25_master3.png)
	public static final boolean O_FRUIT_ART       = true;
	public static final boolean O_MASTER3_ART     = true;
	//(사용자 확인) 솟구치는 뿌리·휩쓰는 가지 그림의 빠르기 (MasterSpikeFx, 미리보기 anim_spikes.html과 같음)
	public static final float O_SPIKE_FPS         = 10f;   //뿌리 창 초당 장 수
	public static final float O_SPEAR_STAGGER     = 0.04f; //뿌리 창이 줄 한쪽 끝부터 한 칸씩 솟는 간격(초)
	public static final float O_WHIP_FPS          = 22f;   //(사용자) 휩쓰는 가지 초당 장 수 (더 빠르게)
	public static final float O_FAN_SWEEP         = 0.22f; //(사용자) 가지 부채가 부채꼴 한쪽 끝에서 다른 쪽 끝까지 휩쓰는 시간(초, 더 빠르게)
	public static final float O_FAN_TAP           = 0.2f;  //지팡이로 바닥을 짚는 순간까지(초): 이때 가지가 솟기 시작
	public static final int   O_SLAM_RADIUS       = 2;   //수액 내려치기: 주인공 둘레 이 칸
	public static final int   O_SLAM_CD           = 10;  //(사용자) 수액 내려치기 쿨타임 (다른 메아리 기술 O_ECHO_CD 6보다 길게, 쓰기 시작한 때부터 셈)
	public static final int   O_SLAM_WARN         = 3;   //(사용자) 수액 내려치기 경고 턴 (그동안 주인은 힘을 모음, 2턴이면 피하지 못해 3으로)
	public static final float O_SLAM_WARN_ALPHA   = 0.4f; //(사용자) 호박빛 경고 칸 진하기 (1이면 테두리 85%·바탕 22%, 지금은 34%·9%)
	public static final int   O_SLAM_MIN          = 14;
	public static final int   O_SLAM_MAX          = 22;
	public static final int   O_SLAM_PUDDLES      = 4;   //남기는 수액 웅덩이 칸 수
	//(사용자) 판막 기믹 범위는 모두 5칸: 판막 둘레 둥글게 5칸(가로·세로 5칸, 대각선은 조금 덜) 안에서 친 공격만 판막에 들어간다 (멀리서 안전하게 쏘는 빈틈을 막음).
	//그 범위는 바닥에 판막 색의 옅은 테두리로 늘 보인다(ValveZone). 어둠 판막의 어둠·꽃잎 판막의 덤 기술도 같은 범위
	public static final int   O_VALVE_ZONE   = 5;
	//(O2) 판막 깨기 — (결정) 그 보스를 이긴 방법으로. 수치는 모두 제안
	public static final float O_SAP_PHYS_REDUCE  = 0.8f;  //(사용자) 수액 판막: 물리 피해(출처가 캐릭터)를 흘리는 몫. 타는 동안은 다 들어감
	public static final float O_SAP_IGNITE_DMG   = 0.6f;  //(사용자) 수액 판막: 불이 새로 붙으면 O_SAP_IGNITE_TURNS턴에 걸쳐 최대 체력의 이만큼 타 들어감 (다시 닫혀 50%면 불만으로 깨짐)
	public static final int   O_SAP_IGNITE_TURNS = 7;
	public static final int   O_LANTERN_RANGE    = 5;     //(사용자) 등불 판막: 앞쪽 부채꼴 빛의 길이(칸, 판막 범위 O_VALVE_ZONE과 맞춤), 양옆은 봉인 등롱과 같은 H1_CONE_HALF
	public static final int   O_LANTERN_BOLT_MIN = 10;    //(사용자) 주인공이 등불 판막의 빛 안에 있으면 주인이 턴마다 떨구는 낙뢰 피해 (제안, 방어 무시)
	public static final int   O_LANTERN_BOLT_MAX = 16;
	public static final int   O_LANTERN_TURN     = 2;     //등불 판막: 이 턴마다 45°씩 돈다 (시계 방향)
	public static final int   O_DARK_RADIUS      = 5;     //(사용자) 어둠 판막: 둘레 이 칸까지 둥글게 어둠 (판막 범위와 같음, 주인공은 자기 칸만 보임, 판막은 닿지 않음)
	public static final int   O_AMBUSH_INTERVAL  = 3;     //(사용자) 주인공이 어둠 속에 있으면 주인이 이 턴마다 기습 (20층 망령 왕과 같은 방식, 피해·방어 무시·되치기는 WK_* 값)
	public static final int   O_DARK_LIGHT_TURNS = 8;     //어둠 판막: 어둠 속 기억의 등불을 켜면 밝은 턴 ((사용자) 꺼지면 어둠 속 다른 자리에 새 등불이 생김)
	public static final float O_PETAL_TAKEN      = 0.5f;  //꽃잎 판막: 받는 피해 배율 (꽃잎의 보호, 20층 여제 1단계처럼 늘)
	//(사용자) 주인공이 꽃잎 판막 둘레 이 칸 안에 있으면 주인이 여제의 강화 마탄·전기비를 턴을 쓰지 않고 덤으로 쓴다 (원래 패턴은 그대로).
	//(결정) "한 턴에 한 기술"의 예외. 읽히게: 20층과 같은 경고 1턴·같은 계산(EM_BOLT_*·EM_RAIN_*), 쿨타임 마탄 EM_BOLT_INTERVAL·전기비 EM_RAIN_INTERVAL, 새 경고는 한 턴에 하나
	public static final int   O_PETAL_ZONE       = 5;     //(사용자) 판막 범위와 같음 (둥글게)
	//(사용자) 판막을 깨도 따로 받는 것은 없고, 깬 판막 수만큼 줄기 둘레가 새잎 땅이 된다 (반지름, 칸)
	public static final float[] O_SPROUT_RINGS   = {1.5f, 3f, 4.5f};
	public static final float O_SPROUT_FADE      = 0.4f;  //(사용자) 새로 돋는 새잎 땅 칸이 서서히 나타나는 시간(초). 이미 자란 칸은 그대로(이웃과 상관없는 모양)
	public static final float O_SPROUT_STEP      = 0.05f; //(사용자) 새잎 땅이 줄기에 가까운 칸부터 한 칸씩 돋는 간격(초), 빛이 줄기 밑동에 닿을 때 시작
	//(O7, 사용자) 마무리: 쓰러짐 그림 시간·줄기에 빛이 모이는 시간·정화 물결이 퍼지는 시간·칸이 다 바뀌는 시간(초), 깔린 새잎 땅보다 더 정화되는 칸, 새잎 땅이 없을 때의 정화 반지름
	public static final float O_FINALE_FALL      = 2.7f;
	public static final float O_FINALE_GATHER    = 0.9f;
	public static final float O_PURIFY_TIME      = 2.0f;
	public static final float O_PURIFY_FADE      = 0.25f; //(사용자) 더 선명하게 바뀌도록 조금 빨리
	public static final float O_PURIFY_EXTRA     = 1.5f;
	public static final float O_PURIFY_MIN_R     = 2.5f;
	public static final float O_WITHER_STEP      = 0.06f; //(사용자) 판막이 다시 닫히면 바깥 고리의 새잎 땅이 바깥 칸부터 한 칸씩 검은 낙엽으로 덮이는 간격(초)
	public static final float O_ZONE_ALPHA       = 0.22f; //(사용자) 판막을 칠 수 있는 범위 테두리의 진하기 (조금 옅게 0.35 → 0.22)
	//(사용자) 판막이 깨질 때 줄기가 자라는 것이 잘 보이게: 깨지고 빛이 흐르기 시작하기까지(초), 뿌리를 타고 판막 → 줄기 밑동까지 흐르는 시간(초),
	//밑동부터 차오르며 자라는 시간(초)
	public static final float O_ROOT_FLOW_DELAY = 0.2f;
	public static final float O_ROOT_FLOW_TIME  = 1.2f;
	//(사용자) 줄기를 따라 시선이 옮겨 가니 자라는 빛은 작게(반지름 픽셀), 나무 꼭대기보다 이만큼(픽셀) 아래까지만 올라감
	public static final float O_GROW_GLOW_RADIUS = 9f;
	public static final int   O_GROW_GLOW_STOP   = 9;
	public static final float O_GROW_TIME        = 0.9f;
	//(사용자) 마무리: 줄기가 한 번에 끝까지 자라고, 둥근 빛 대신 나무 그림(줄기·가지 픽셀) 자체를 은은한 새잎빛 띠가 밑동부터 위로 타고 오른다 (BranchGlow)
	public static final float O_FINALE_GROW_TIME   = 1.8f;   //자라며 빛이 오르는 시간
	public static final float O_FINALE_GLOW_ALPHA  = 0.55f;  //빛 띠 가운데 진하기
	public static final float O_FINALE_GLOW_BAND   = 5f;     //빛 띠 반 너비 (픽셀)
	public static final float O_FINALE_GLOW_TRAIL  = 0.18f;  //빛이 지나간 아래쪽에 남는 옅은 빛
	public static final float O_FINALE_GLOW_FADE   = 0.9f;   //다 오른 뒤 스러지는 시간

	// ===== 25층 최종보스: 심장을 진 짐승 (지시서 claude_code_tasks_1122_boss25.md + 1122b_boss25_update.md) =====
	//사슴(짐승) 기본 수치 (사용자 결정: 원작 요그와 같은 체력)
	//(결정, 2026-11-22 사용자) 25층 짐승은 버그가 많고 완성도가 낮아 꺼 둔다(나중에 다시 개발). false면 원작 25층 보스(탑의 주인 외형)와 원작 굴절광·이식 마법봉, 24층 건너뛰기 숨김
	public static final boolean HEART_BOSS = false;
	//(결정, 2026-11-22 사용자) 보스전(보스층이 잠겨 싸우는 동안)에는 이동 속도를 빠르게 하는 효과가 듣지 않는다
	//  (신속의 물약·반지, 마법 부여, 기세 등). 주인공 이동 속도는 최대 1배. 느려지는 효과는 이것들로 보통 속도까지 상쇄할 수 있다. 공격 속도(분노의 반지 등)는 그대로
	public static final boolean BOSS_NO_SPEEDUP = true;
	public static final int   B25_HP            = 1000;
	public static final int   B25_EXP           = 50;
	public static final int   B25_DEF           = 20;    //회피
	public static final int   B25_ACC           = 30;    //명중
	public static final int   B25_DR            = 10;    //방어 0~10
	//(2026-11-22) 2페이즈 진입 어둠: 화면 전체가 짐승 쪽으로 조여든다 (조여드는 시간·유지 시간은 연출 길이에 대한 비율)
	public static final float B25_DARK_ALPHA    = 0.92f;
	public static final float B25_DARK_R        = 26f;   //가장 좁을 때 짐승 둘레 밝은 원 반지름(픽셀)
	public static final float B25_DARK_IN       = 0.35f;
	public static final float B25_DARK_HOLD     = 0.35f;
	public static final float B25_VIGNETTE_ALPHA = 0.45f; //3페이즈 심장 박동 파동 때 화면 어둡기
	public static final int   B25_ROOM_W        = 17;    //보스방 안쪽 크기
	public static final int   B25_ROOM_H        = 11;    //(결정, 2026-11-22 사용자) 17×13 → 17×11
	//1페이즈: 살점과 오염
	public static final float B25_PHASE2_HP     = 0.70f; //이 아래면 2페이즈
	public static final int   B25_FLESH_START   = 2;     //처음 살점 수
	public static final int   B25_FLESH_INTERVAL = 6;    //이 턴마다 1개
	public static final int   B25_FLESH_MAX     = 4;
	public static final int   B25_FLESH_GAP     = 3;     //영웅·사슴에게서 이 칸 이내에는 생기지 않음
	public static final int   B25_EAT_TURNS     = 3;     //살점 하나를 먹는 턴
	public static final float B25_EAT_HEAL      = 0.05f; //먹으면 최대 체력의 이만큼 회복
	public static final float B25_CONTAM_TIME   = 0.6f;  //오염 번짐 연출 (초)
	//2페이즈: 사슴 상태
	public static final float B25_P2_TAKEN      = 0.10f; //받는 피해 90% 감소
	public static final float B25_BAND          = 0.25f; //의태 처치마다 깎이는 체력·피해로 못 내려가는 구간
	public static final float B25_PHASE3_HP     = 0.10f; //4번째 의태 처치 후 3페이즈 (체력바 눈금)
	public static final int   B25_MIMIC_FIRST   = 5;     //2페이즈 시작 뒤 첫 의태까지
	public static final int   B25_MIMIC_NEXT    = 7;     //사슴으로 돌아온 뒤 다음 의태까지
	public static final int   B25_MIMIC_KILLS   = 4;     //이만큼 처치하면 3페이즈
	public static final int   B25_CHARGE_INTERVAL = 4;   //사슴 돌진 주기 (턴)
	public static final int   B25_CHARGE_MIN    = 42;    //돌진 피해
	public static final int   B25_CHARGE_MAX    = 66;
	public static final float B25_CHARGE_ARMOR  = 0.30f; //방어력의 이만큼만 적용
	public static final int   B25_CHARGE_KNOCK  = 3;     //영웅 넉백 칸
	public static final float B25_CHARGE_STUN   = 1f;    //영웅 기절 턴
	public static final float B25_WALL_STUN     = 1f;    //벽에 부딪힌 사슴 기절 턴
	//의태 영웅 공통
	public static final float B25_TELEGRAPH_ARMOR = 0.50f; //예고 공격은 방어력의 이만큼만
	public static final float B25_AMBUSH_ARMOR  = 0.30f;   //도적 기습
	//전사
	public static final int   MW_HP = 200, MW_ACC = 36, MW_DEF = 16, MW_MIN = 14, MW_MAX = 22, MW_DR = 12;
	public static final int   MW_LEAP_CD = 8, MW_LEAP_FIRST = 2, MW_LEAP_RANGE = 4, MW_LEAP_MIN = 20, MW_LEAP_MAX = 30;
	public static final float MW_RAGE_HP = 0.50f, MW_RAGE_DMG = 0.30f;
	//마법사
	public static final int   MM_HP = 150, MM_ACC = 40, MM_DEF = 12, MM_MIN = 20, MM_MAX = 28, MM_DR = 4;
	public static final int   MM_KEEP_MIN = 3, MM_KEEP_MAX = 4;
	public static final int   MM_THORN_CD = 6, MM_THORN_FIRST = 2, MM_THORN_MIN = 38, MM_THORN_MAX = 52;
	public static final int   MM_BLINK_CD = 10, MM_BLINK_DIST = 4;
	//도적
	public static final int   MR_HP = 120, MR_ACC = 40, MR_DEF = 22, MR_MIN = 8, MR_MAX = 12, MR_DR = 6, MR_HITS = 2;
	public static final float MR_MOVE_SPEED = 2f;   //빠름(이동만)
	public static final int   MR_STEALTH_CD = 10, MR_STEALTH_FIRST = 3, MR_STEALTH_TURNS = 30, MR_STEALTH_SHIFT = 2;
	public static final float MR_STEALTH_SPEED = 2f; //은신 중 이동속도 증가 (배)
	public static final int   MR_AMBUSH_MIN = 24, MR_AMBUSH_MAX = 34;
	public static final int   MR_POISON_CD = 8, MR_POISON_FIRST = 1, MR_POISON_TURNS = 8;
	public static final float MR_POISON_ARMOR = 0.40f; //독바르기: 방어력을 이만큼 더 무시
	public static final int   MR_POISON_STACK = 10;    //(결정, 2026-11-22 사용자) 맞을 때마다 원작 독이 이만큼 쌓인다 (예전 1)
	//사냥꾼
	public static final int   MH_HP = 170, MH_ACC = 42, MH_DEF = 22, MH_MIN = 20, MH_MAX = 27, MH_DR = 6;
	public static final int   MH_KEEP = 4;
	public static final float MH_CLAW_MULTI = 0.60f; //구석에 몰려 발톱으로 싸울 때 (피해 -40%)
	public static final int   MH_PIERCE_CD = 4, MH_PIERCE_MIN = 33, MH_PIERCE_MAX = 45, MH_PIERCE_BACK = 2;
	public static final int   MH_TRAP_CD = 6, MH_TRAP_RANGE = 4, MH_TRAP_COUNT = 17, MH_TRAP_FADE = 3;
	public static final float MH_TRAP_ROOT = 2f;
	//3페이즈 광폭화
	public static final int   B25_P3_TURNS      = 20;    //버티면 쓰러지는 턴
	public static final int   B25_P3_STAGE2     = 16;    //남은 턴이 이 이하면 2단계 (파동·핏줄 바닥)
	public static final int   B25_P3_STAGE3     = 12;    //이 이하면 3단계 (살점 비)
	public static final int   B25_BOUNCE_CD     = 5,  B25_BOUNCE_MIN = 55, B25_BOUNCE_MAX = 75, B25_BOUNCE_KNOCK = 2;
	public static final float B25_BOUNCE_AIM_MIN = 20f, B25_BOUNCE_AIM_MAX = 35f; //첫 돌진 조준 오차 (도)
	public static final float B25_BOUNCE_WALL_MIN = 25f, B25_BOUNCE_WALL_MAX = 65f; //튕김 방향과 벽면 사이 각도
	public static final int   B25_BOUNCE_WARN_AFTER = 5; //튕긴 뒤 경로 경고 칸 수
	public static final float[] B25_BOUNCE_WARN_ALPHA = {1f, 0.78f, 0.58f, 0.40f, 0.25f};
	public static final int   B25_WAVE_CD       = 3,  B25_WAVE_MIN = 40, B25_WAVE_MAX = 55;
	public static final int   B25_VEIN_DMG      = 4;     //핏줄 바닥
	public static final float B25_VEIN_SLOW     = 1f;
	public static final int   B25_RAIN_COUNT    = 3,  B25_RAIN_MIN = 45, B25_RAIN_MAX = 60;
	public static final float B25_P3_ARMOR      = 0.30f; //3페이즈 공격은 방어력의 이만큼만
	//연출
	public static final float B25_BPM           = 48f;   //심장 박동 (실제 시간)
	public static final float B25_BPM_RAGE      = 70f;   //3페이즈
	//마법봉 변경 (25층 지시서 7절): 이식은 맞은 적을 이만큼 매혹 (원작 Charm.DURATION/2 = 5)
	public static final float TRANSFUSION_CHARM = 1f;

	//10층: 부적 넋(봉인술사)의 부적. 예전 간수(텐구 바탕)의 수치는 지웠다
	public static final float TALISMAN_ROOT_TURNS   = 2f;  //봉인 부적: 맞은 대상 속박
	public static final int   CAGE_HP               = 30;  //부적 철창 체력

	//광맥 (11~14층 살아 있는 광맥·15층 보스방 광맥 기둥)
	public static final int   VEIN_HP              = 20;  //광맥 체력
	public static final boolean VEIN_GLOW         = false; //(결정) 광맥 둘레의 빛무리는 끔 (틈 사이로만 빛이 샘)
	public static final float VEIN_GLOW_RADIUS     = 11f;  //광맥에서 새는 옅은 푸른 빛무리의 반지름 (픽셀)
	public static final float VEIN_GLOW_BRIGHT     = 0.28f; //그 밝기
	public static final int   VAPOR_AMOUNT         = 150; //(사용자) 마력 증기: 광맥이 부서진 칸에 뿜는 양. 원작 독가스처럼 퍼지며 옅어진다 (원작 독가스 함정은 300 + 20 × 층)
	public static final float PROP_FADE_TIME       = 0.3f; //부적 철창·광맥·수정 기둥이 부서진 뒤 사라지는 시간(초, 원작 몬스터는 3초)

	// ===== 전직의 핵을 지닌 보스 몸 주위 벚꽃잎 (5층 희미하게 / 10층 조금 선명하게 / 20층 선명하게) =====
	public static final float[] BOSS_PETAL_ALPHA    = {0.35f, 0.6f, 0.9f};   //꽃잎 진하기
	public static final float[] BOSS_PETAL_INTERVAL = {0.35f, 0.22f, 0.13f}; //꽃잎이 나오는 간격(초)
	public static final float   BOSS_PETAL_MOVE_INTERVAL = 0.4f;           //움직이는 동안은 간격에 이 배율 (더 자주)

	// ===== 구역 외형 (28단계, 명세 20번) =====
	public static final float SAP_FLOOR_LEVEL_CHANCE = 0.5f; //뿌리 지하 일반 층마다 수액 웅덩이 한 묶음이 생길 확률 (층과 상관없이 같다)
	public static final int   SAP_FLOOR_SIZE_MIN    = 2;     //한 묶음의 칸 수
	public static final int   SAP_FLOOR_SIZE_MAX    = 4;
	public static final float SEAL_GRASS            = 0.20f; //봉인의 회랑 잔디 (원작과 같은 0.20)
	public static final int   HEART_FLOOR_DECO_DENOM = 80;   //탑의 심장 바닥 살점 (작을수록 많다, 원작과 같은 80)
	public static final float HEART_FLOOR_ALT2_CHANCE = 5f;  //탑의 심장 바닥에 작은 혹·핏줄(FLOOR_ALT_2)이 섞이는 확률 % (원작과 같은 5)
	//(사용자) 16~19층 바닥·벽은 그 판의 20층 보스에 따라 (망령 왕 = 무너진 왕도, 여제 = 바랜 마도 대리석). false면 예전 한 가지 그림
	public static final boolean CITY_BY_BOSS = true;
	//(사용자) 탑의 심장(21~26층)에서 낮은 확률로 나오던 붉은 고리 "두-근" 분위기 이펙트. 없앰
	public static final boolean HEART_REGION_BEAT = false;
	//(사용자) 21~24층 새 바닥·벽: 큰 그림을 칸 자리로 잘라 깔고(15칸마다 되풀이), 핏줄이 두-근 박동 (tiles/HeartFloorTilemap). false면 예전 살결 그림
	public static final boolean HEART_NEW_FLOOR = true;
	//(사용자) 구역이 바뀌는 보스층(10·15·20층) 출구 둘레에 다음 구역의 바닥·벽을 은은히 섞어 그림 (MazeTransition). 출구에 가까울수록 진함
	public static final boolean EXIT_BLEND = true;
	public static final float EXIT_BLEND_RADIUS = 6.5f;     //10·15층: 출구에서 이만큼 칸까지 퍼짐
	public static final float EXIT_BLEND_RADIUS_20 = 11f;   //20층: 위쪽 출구 계단 밑에서 복도를 따라
	public static final float EXIT_BLEND_JITTER = 1.8f;     //칸마다 흔들림 (가장자리가 들쭉날쭉)
	public static final float[] EXIT_BLEND_ALPHAS = { 0.3f, 0.55f, 0.8f }; //단계 1·2·3의 진하기
	public static final float RETREAT_GRID_ALPHA = 0.3f;    //(사용자) 역행 중 21~26층: 원작 칸 격자(바둑판) 진하기 (밝은 바닥에서 네모가 비쳐 보이지 않게)
	public static final int EXIT_BLEND_DECO_EVERY = 6;      //(사용자) 가장 진한 칸 중 이만큼에 하나만 그 구역의 장식 바닥 (나머지는 장식 없는 바닥)
	//(사용자) 적이 밀려나 문 칸을 떠날 때 문을 닫지 않는다 (원작은 몇몇 밀치기가 문을 닫음 → 닫힌 문 너머에서 걸어오는 적에게 매번 기습)
	public static final boolean PUSH_KEEPS_DOOR = true;
	public static final int   REGION_FX_CHANCE      = 150;   //구역 분위기 이펙트: 주인공이 행동할 때마다 1/이 값
	public static final float HEART_BLOOD_DROP_DELAY_MIN = 3f;  //탑의 심장 핏빛 물: 칸마다 방울이 튀는 간격(초, 원작 불꽃은 0~2초)
	public static final float HEART_BLOOD_DROP_DELAY_MAX = 8f;
	public static final float HEART_BLOOD_DROP_ALPHA     = 0.7f; //방울 진하기
	public static final float SEWER_WALL_DECO_KEEP       = 0.35f; //뿌리 지하: 벽 장식(뿌리 단면) 중 남기는 비율 (물이 많은 층에서 너무 많아서)
	public static final float DOOR_DECO_PCT              = 0.33f; //모든 구역: 닫힌 문 중 장식(뿌리·사슬·마석·휘장·가지)이 붙은 문의 비율 (effects/MazeDoorDecor)
	public static final float BOSS20_WALL_DECO_PCT       = 0.30f; //20층 보스방(망령 왕·여제): 아래가 바닥인 벽 앞면 중 새 장식을 그리는 비율 (effects/BossRoomDecor20)
	//베타 테스트용 층 건너뛰기 치트 (명세 8번): 목표 층(각 보스층 바로 앞)과 도착할 때의 레벨
	public static final int[] SKIP_DEPTHS = {4, 9, 14, 19, 24}; //(2026-11-22) 24층(25층 짐승 앞) 추가
	public static final int[] SKIP_LEVELS = {6, 11, 15, 19, 24};
	public static final int   SKIP_FRUIT_DEPTH     = 24; //이 층부터는 결실의 핵도 준다 (25층 짐승 시험용)
	public static final int   SKIP_BLOOM_DEPTH     = 9;  //이 층부터는 건너뛸 때 개화의 핵을 준다
	public static final int   SKIP_FULLBLOOM_DEPTH = 14; //이 층부터는 만개의 핵도 준다 (주문서·물약 가방은 언제나)
	public static final int   CHEAT_STR = 20;            //치트 "힘 20으로" (명세 8번)
	public static final float ROOT_SP_PETAL_CHANCE       = 12f;  //뿌리 지하 특수 바닥 중 벚꽃잎이 있는 그림이 나오는 확률 % (원작 그림 바꾸기 비율 50)
	public static final float BOSS_WALL_DECO_KEEP  = 0.3f; //5·10층 보스층: 벽 장식 중 남기는 비율
	public static final float BOSS_FLOOR_DECO_KEEP = 0.3f; //5·10층 보스층: 바닥 장식 중 남기는 비율

	// ===== 몬스터 전체 세기 =====
	//원작 몬스터 수치에 곱한다(1 = 원작). (결정) 한때 0.8·0.9로 약하게 했다가 원작대로 되돌렸다. 30단계 난이도 조정 때 구역별로 나눌 수 있다.
	public static final float MOB_HP_MULTI  = 1f;    //체력
	public static final float MOB_DMG_MULTI = 1f;    //근접 공격 피해
	public static final float MOB_ACC_MULTI = 1f;    //명중
	//보스 (BOSS 속성). 보스가 부른 몹·움직이지 않는 것(철창·광맥·기둥·돌무더기)은 원작 그대로
	public static final float BOSS_HP_MULTI  = 1f;
	public static final float BOSS_DMG_MULTI = 1f;
	public static final float BOSS_ACC_MULTI = 1f;

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
