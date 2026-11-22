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
	public static final boolean BOSS_TRIAL = true;
	//보스전 체험판에서 바로 시작하는 보스층 (레벨은 그 앞 층 건너뛰기와 같다: 5층 레벨 6, 20층 레벨 19)
	public static final int TRIAL_SAP_DEPTH = 5;
	public static final int TRIAL_CITY_DEPTH = 20;
	public static final int TRIAL_HEART_DEPTH = 25; //(2026-11-22) 25층 심장을 진 짐승 (레벨은 24층 건너뛰기와 같음, 결실의 핵 지급)
	//체험판 "아이템 꺼내기" 치트 창(WndTrialItems)의 강화·수량 최대
	public static final int TRIAL_CHEAT_MAX_UPGRADE = 20;
	public static final int TRIAL_CHEAT_MAX_QTY = 99;
	//보스전 체험판 20층 보스로 건너뛸 때 주는 물약·주문서 수 (종류마다), 강화의 주문서 수 (MazeTrial.kit)
	public static final int TRIAL_POTIONS = 5;
	public static final int TRIAL_SCROLLS = 5;
	public static final int TRIAL_UPGRADES = 12;
	//(2026-11-21) 체험판 사냥꾼에게 주는 화살 (5층 / 20층, 시작 화살 100발에 더함), 투창 수 (20층)
	public static final int TRIAL_ARROWS_SAP = 100;
	public static final int TRIAL_ARROWS_CITY = 300;
	public static final int TRIAL_JAVELINS = 3;
	//체험판 아이템 고르기 창(WndTrialItems)의 마법봉 강화 수치, 체험판 지급 반지(모든 종류, 감정됨) 강화 수치
	public static final int TRIAL_WAND_UPGRADE = 1;
	public static final int TRIAL_RING_UPGRADE = 2;
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

	//대기(휴식·탐색 포함)로 넘긴 한 턴마다 얻는 마나. 다른 행동으로는 얻지 못한다.
	public static final int MANA_PER_WAIT_TURN = 10;

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
	//  1티어 단궁 / 2티어 탄궁·장궁 / 3티어 각궁·석궁 / 4티어 정량궁·정령의 활. 힘 요구치는 원작 공식(티어)이라 검 7종과 같다
	//  강화 1당 증가량: 근접 +1/+1, 원거리 +1/+BOW_RANGED_MAX_SCALE[티어]
	public static final int[] BOW_MELEE_MIN  = {0, 1, 2, 3, 4};
	public static final int[] BOW_MELEE_MAX  = {0, 4, 7, 10, 13};
	public static final int[] BOW_RANGED_MIN = {0, 1, 2, 3, 4}; //(결정, 2026-11-21 밸런스) 2,3,4,5 → 1,2,3,4
	public static final int[] BOW_RANGED_MAX = {0, 7, 10, 14, 17}; //(결정, 밸런스) 8,12,16,20 → 7,10,14,17 (원거리 평타 ≈ 같은 힘 검의 75%)
	public static final int[] BOW_RANGED_MAX_SCALE = {0, 1, 2, 3, 4}; //(결정, 밸런스) 2,3,4,5 → 1,2,3,4
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
	//장전 (석궁): 1턴, 다음 원거리 평타 = 평타 100% + 110% = 210%, 명중 +30%
	public static final float SKILL_LOAD_DMG = 2.1f; //(결정, 밸런스) 2.3 → 2.1 (평타 100% + 110%)
	public static final float SKILL_LOAD_ACCURACY = 1.3f;
	//집중 (정량궁): 1턴, 다음 원거리 평타 = 평타 100% + 140% = 240%, 명중 +50%
	public static final float SKILL_FOCUS_DMG = 2.4f; //(결정, 밸런스) 2.8 → 2.4 (평타 100% + 140%)
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

	//--- 기본공격 전용 ---
	//연속 베기: 평타 적중 시 확률로 같은 적에게 추가 평타
	public static final float[] B2_DOUBLE_SLASH_CHANCE = {0.10f, 0.20f, 0.30f};
	//강타: 같은 적에게 N번째 평타마다
	public static final int     B2_HEAVY_HITS          = 3;
	public static final float[] B2_HEAVY_DMG           = {0.30f, 0.45f, 0.45f};
	public static final int     B2_HEAVY_PUSH          = 1;    //3레벨 밀침 칸
	public static final float   B2_HEAVY_AWAKE_STUN    = 1f;   //각성: 기절 턴
	//질풍: 평타 처치 후 다음 평타 공격속도
	public static final int[]   B2_GALE_CHARGES        = {1, 2, 3};
	public static final float   B2_GALE_SPEED          = 0.50f; //공격속도 +50%
	public static final float   B2_GALE_TURNS          = 10f;
	public static final float   B2_GALE_AWAKE_DMG      = 0.50f; //각성: 휩쓸기 (파생)
	//표식 새기기
	public static final int[]   B2_MARK_MAX            = {2, 3, 4};
	public static final float   B2_MARK_DMG_PER        = 0.04f; //표식 1개당 받는 피해
	public static final float   B2_MARK_TURNS          = 3f;
	public static final int     B2_MARK_AWAKE_RANGE    = 2;
	//돌진: 이동 후 다음 피해
	public static final float[] B2_CHARGE_DMG          = {0.10f, 0.20f, 0.30f};
	public static final int     B2_CHARGE_COOLDOWN     = 40;
	public static final float   B2_CHARGE_AWAKE_IGNORE = 0.50f; //각성: 방어력 무시
	//불꽃 칼날
	public static final float[] B2_FLAME_CHANCE        = {0.10f, 0.20f, 0.30f};
	public static final float   B2_FLAME_TURNS         = 3f;
	public static final float   B2_FLAME_AWAKE_DMG     = 0.25f; //각성: 화상 중인 적에게 모든 피해
	//물보라
	public static final float[] B2_SPLASH_CHANCE       = {0.20f, 0.35f, 0.50f};
	public static final float   B2_SPLASH_TURNS        = 3f;
	//마나 흡수
	public static final int[]   B2_MANA_DRAIN          = {2, 3, 5};
	public static final float   B2_DRAIN_AWAKE_IGNORE  = 0.50f; //각성: 가득 찬 동안 방어 무시
	public static final int     B2_DRAIN_AWAKE_COST    = 15;
	//확정타: 빗나간 뒤 다음 평타
	public static final float[] B2_SURE_BONUS          = {0.10f, 0.20f, 0.30f}; //명중률·피해 보정
	public static final int     B2_SURE_COOLDOWN       = 5;
	public static final int     B2_SURE_AWAKE_HITS     = 2;

	//--- 스킬 전용 ---
	//연쇄 (파생)
	public static final int[]   B2_CHAIN_TARGETS       = {1, 1, 2};
	public static final float[] B2_CHAIN_PCT           = {0.30f, 0.45f, 0.60f};
	public static final int     B2_CHAIN_RANGE         = 2;
	public static final int     B2_CHAIN_AWAKE_TARGETS = 3;
	//표식 폭발
	public static final float[] B2_MARK_BURST_PER      = {0.15f, 0.25f, 0.35f};
	public static final int     B2_MARK_BURST_AWAKE_RANGE = 2;
	//집중: 대기·탐색 바로 다음 스킬
	public static final float[] B2_FOCUS_DMG           = {0.25f, 0.40f, 0.55f};
	public static final int     B2_FOCUS_COOLDOWN      = 10;
	public static final float   B2_FOCUS_AWAKE_IGNORE  = 0.50f;
	//처형
	public static final float   B2_EXECUTE_HP          = 0.30f;
	public static final float[] B2_EXECUTE_DMG         = {0.20f, 0.35f, 0.50f};
	public static final float   B2_EXECUTE_REFUND      = 0.50f; //3레벨, 처치 시 비용 반환
	public static final float   B2_EXECUTE_AWAKE_FEAR  = 2f;
	public static final int     B2_EXECUTE_AWAKE_RANGE = 2;
	//과충전: 가득 찬 마나로 쓴 스킬
	public static final float[] B2_OVERCHARGE_DMG      = {0.20f, 0.35f, 0.50f};
	public static final float   B2_OVERCHARGE_AWAKE_DMG = 0.50f; //각성: 3×3 충격파 (파생)
	//충격: 스킬 맞은 적 밀침
	public static final int[]   B2_IMPACT_PUSH         = {1, 1, 2};
	public static final float[] B2_IMPACT_WALL_DMG     = {0.20f, 0.30f, 0.40f};
	public static final float   B2_IMPACT_STUN         = 2f; //3레벨 벽 충돌, 각성 적 충돌
	//서리 베기
	public static final float[] B2_FROST_TURNS         = {2f, 3f, 4f};
	//흘리기
	public static final float[] B2_PARRY_REDUCE        = {0.15f, 0.25f, 0.40f};
	public static final float   B2_PARRY_AWAKE_DMG     = 1.00f; //각성: 반격 (파생)
	//섬광 보법: 원거리 스킬(활 조준 사격·지팡이 스킬) 피해, 쓴 뒤 물러나는 칸
	public static final int[]   B2_FLASH_RANGE         = {1, 1, 2};
	public static final float[] B2_FLASH_DMG           = {0.10f, 0.20f, 0.20f};
	public static final float   B2_FLASH_AWAKE_STUN    = 1f;
	public static final int     B2_FLASH_COOLDOWN      = 10;   //준비되었을 때만 발동, 발동하면 이 턴 동안 다시 쓸 수 없다

	//--- 공용 ---
	//흐름: 평타와 스킬을 번갈아 쓸 때
	public static final float[] B2_FLOW_DMG            = {0.05f, 0.15f, 0.25f};
	public static final float   B2_FLOW_ACC            = 0.80f; //3레벨 명중률
	public static final int     B2_FLOW_AWAKE_STACKS   = 20;
	public static final int     B2_FLOW_AWAKE_COST     = 10;    //각성: 스킬 마나 +10
	public static final int     B2_FLOW_AWAKE_WAND_COST = 15;   //마법봉은 +15
	//잔향: 스킬 뒤 평타 적중 시 마나
	public static final float   B2_ECHO_TURNS          = 3f;
	public static final int[]   B2_ECHO_MANA           = {2, 4, 6};
	public static final float   B2_ECHO_AWAKE_DMG      = 0.30f; //각성: 잔상 (파생)
	public static final int     B2_ECHO_AWAKE_MANA     = 10;
	//기세: 처치마다 스택
	public static final int     B2_MOMENTUM_MAX        = 3;
	public static final float   B2_MOMENTUM_TURNS      = 10f;
	public static final float[] B2_MOMENTUM_DMG        = {0.05f, 0.08f, 0.12f};
	public static final float   B2_MOMENTUM_AWAKE_DMG  = 0.50f; //각성: 2칸 충격파 (파생)
	public static final int     B2_MOMENTUM_AWAKE_RANGE = 2;
	//난전: 인접한 적 1명당
	public static final int     B2_MELEE_MAX           = 4;
	public static final float[] B2_MELEE_DMG           = {0.05f, 0.08f, 0.12f};
	public static final int     B2_MELEE_GUARD_COUNT   = 3;     //각성: 인접 적이 이만큼 이상이면
	public static final float   B2_MELEE_GUARD         = 0.25f; //받는 피해 감소
	//결투: 2칸 안에 적이 하나뿐일 때
	public static final int     B2_DUEL_RANGE          = 2;
	public static final float[] B2_DUEL_DMG            = {0.15f, 0.25f, 0.35f};
	public static final float   B2_DUEL_GUARD          = 0.15f; //3레벨 받는 피해 감소
	public static final float   B2_DUEL_AWAKE_HEAL     = 0.15f; //각성: 처치 시 최대 체력 비율 회복
	//기습
	public static final float[] B2_AMBUSH_DMG          = {0.20f, 0.30f, 0.40f};
	public static final float   B2_AMBUSH_AWAKE_INVIS  = 1f;
	//잔류전기 (전이는 파생)
	public static final float[] B2_SHOCK_CHANCE        = {0.10f, 0.15f, 0.20f};
	public static final float   B2_SHOCK_DMG_FACTOR    = 0.50f; //그 타격 피해에 대한 전격 피해
	//원소 증폭
	public static final float[] B2_ELEMENT_AMP_DMG     = {0.15f, 0.30f, 0.45f};
	public static final float   B2_AMP_AWAKE_WEAKEN    = 2f;
	public static final int     B2_AMP_AWAKE_COOLDOWN  = 3;
	//원소 순환
	public static final int[]   B2_CYCLE_MANA          = {8, 16, 24};
	public static final float   B2_CYCLE_AWAKE_TURNS   = 3f; //각성: 다음 공격이 거는 원소 지속
	//마나 장막
	public static final float   B2_VEIL_HP             = 0.35f; //체력이 이 비율 이하일 때 발동 (25% → 35%)
	public static final int     B2_VEIL_COST           = 15;
	public static final float[] B2_VEIL_REDUCE         = {0.25f, 0.40f, 0.55f};
	public static final int     B2_VEIL_AWAKE_PUSH     = 1;
	//넘치는 힘
	public static final float[] B2_OVERFLOW_DMG        = {0.10f, 0.20f, 0.30f};
	public static final float   B2_OVERFLOW_AWAKE_DMG  = 0.50f; //각성: 마력 파편 (파생)
	public static final int     B2_OVERFLOW_AWAKE_RANGE = 3;
	//명상
	public static final int[]   B2_MEDITATION_MULTI    = {2, 3, 4};
	public static final float   B2_MEDITATION_AWAKE_SHIELD = 0.20f; //최대 체력 비율
	public static final float   B2_MEDITATION_AWAKE_TURNS  = 50f;
	public static final int     B2_MEDITATION_AWAKE_COOLDOWN = 200;

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
		if (cls == HeroClass.HUNTRESS) return new HeroSubClass[]{HeroSubClass.RINGKEEPER, HeroSubClass.TRACKER}; //사냥꾼: 고리지기 / 추적자
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

	// ===== 사냥꾼 3차: 추적자 (배열은 [0레벨, 1레벨, 2레벨, 3레벨, 4레벨]) =====

	//사냥감 지정: 스킬로 맞힌 적을 사냥감으로 (턴), 사냥감에게 기본공격 피해 +
	public static final float[] T3_PREY_TURNS = {0f, 2f, 2f, 2f, 3f};
	public static final float[] T3_PREY_DMG   = {0f, 0.10f, 0.15f, 0.20f, 0.25f}; //(결정, 밸런스) 15/20/25/35% → 10/15/20/25%
	//저격: 활을 든 채 떨어진 칸당 피해 + (최대 T3_SNIPE_MAX_TILES칸)
	public static final float[] T3_SNIPE_PER_TILE = {0f, 0.01f, 0.02f, 0.03f, 0.04f}; //(결정, 밸런스) 2/3/4/5% → 1/2/3/4%
	public static final int     T3_SNIPE_MAX_TILES = 5; //(결정, 밸런스) 8 → 5
	//덫: 사냥감이 다가오면 1칸마다 느려짐 확률, 느려짐 턴
	public static final float[] T3_SNARE_CHANCE = {0f, 0.10f, 0.20f, 0.30f, 0.40f};
	public static final float   T3_SNARE_TURNS  = 2f;
	//마나 흡수: 사냥감을 처치하면 마나 +
	public static final int[]   T3_PREY_MANA = {0, 8, 12, 16, 20};

	// ===== 사냥꾼 3차: 고리지기 =====

	//고리의 맥: 반지 고유 패시브 수치 배율 +
	public static final float[] T3_RING_PULSE = {0f, 0.15f, 0.30f, 0.45f, 0.60f};
	//공명: 두 반지 강화 수치 차이 중 낮은 쪽을 끌어올리는 비율 (내림)
	public static final float[] T3_RESONANCE = {0f, 0.25f, 0.50f, 0.75f, 1.00f};
	//맨손 사냥: 무기를 들지 않았을 때 근접 피해 +
	public static final float[] T3_BARE_HUNT = {0f, 0.03f, 0.05f, 0.08f, 0.10f}; //(결정, 밸런스 1d) 실제 완력의 반지 공식으로 다시 계산: 10/30/50/70% → (1c) 10/20/30/40% → 3/5/8/10%
	//반지 꽃: 처치 시 마나 +, 체력 +
	public static final int[]   T3_RING_BLOOM_MANA = {0, 3, 6, 9, 12};
	public static final int     T3_RING_BLOOM_HP   = 2;
	//반지 패시브 (고리의 맥 배율이 곱해진다)
	public static final float RK_CRIT_PER_ACC   = 0.003f; //정확성: 명중 보너스 1%당 급소 확률
	public static final float RK_CRIT_DMG       = 1.3f;   //정확성: 급소 피해 배율
	public static final float RK_COUNTER_DMG    = 0.30f;  //회피: 흘리기 반격 (맨손 평타 대비)
	public static final int   RK_MOMENTUM_TILES = 3;      //신속: 추진력 최대 칸
	public static final float RK_MOMENTUM_DMG   = 0.06f;  //신속: 칸당 피해 +
	public static final float RK_HEAT_SPEED     = 0.05f;  //분노: 중첩당 공격 속도 +
	public static final int   RK_HEAT_MAX       = 5;
	public static final float RK_HEAT_TURNS     = 10f;
	public static final int   RK_ADVERSITY_NEED  = 400;   //인내: 반지로 줄인 피해 누적이 이만큼 되면
	public static final int   RK_ADVERSITY_ARMOR = 8;     //인내: 최소·최대 방어력 +
	public static final float RK_SPARE_STR_DMG  = 0.02f;  //힘: 남는 힘 1당 맨손 피해 +
	public static final float RK_FLOW_CHARGE    = 0.15f;  //충전: 맨손 명중 시 마법봉 충전
	public static final float RK_STEAL_CHANCE   = 0.05f;  //부유함: 소매치기 확률
	//반지 스킬 (마나 SKILL_MANA_COST)
	public static final float RK_PRESSURE_TURNS = 3f;     //정확성 혈점 찌르기
	public static final int   RK_FRENZY_HITS = 5;         //분노 광란 연타
	public static final float RK_FRENZY_DMG  = 0.32f;    //(결정, 밸런스 1d) 0.40 → 0.32
	public static final float RK_PURIFY_TURNS = 3f;       //원소 정화 면역
	public static final float RK_THROW_DMG = 1.6f;        //완력 던지기 ((결정, 밸런스 1d) 2.0 → 1.6)
	public static final float RK_THROW_COLLIDE_DMG = 0.8f; //던지기: 벽·적에 부딪히면 부딪힌 쪽도 맨손 평타의 이만큼 ((결정, 밸런스 1d) 1.0 → 0.8)
	public static final int   RK_THROW_DIST = 2;
	public static final float RK_THROW_STUN = 1f;
	public static final float RK_BURST_DMG = 0.8f;        //충전 터뜨리기 기본 ((결정, 밸런스 1d) 1.0 → 0.8)
	public static final float RK_BURST_PER_CHARGE = 0.24f; //(결정, 밸런스 1d) 0.30 → 0.24
	public static final int   RK_GOLD_PER_DEPTH = 10;     //금화 주먹: 층 × 이만큼
	public static final float RK_GOLD_DMG = 2.0f;         //(결정, 밸런스 1d) 2.5 → 2.0
	public static final int   RK_VOLLEY_COUNT = 2;        //저격 연속 투척

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
		if (cls == HeroClass.HUNTRESS){ //사냥꾼: 정령 매 하나
			return new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[]{
					new com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk()};
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

	// ===== 사냥꾼 4차: 정령 매 (충전 35%, 원작과 같음) =====
	public static final float   HAWK_TURNS = 20f;                          //매가 머무는 턴
	public static final float[] T4_HAWK_LINGER = {0f, 5f, 10f, 15f, 20f};    //오래 머무는 매: +턴
	public static final int[]   T4_HAWK_EYE = {0, 1, 2, 3, 4};              //매의 눈: 시야 +칸 (4레벨은 함정·비밀문 발견)
	public static final float   HAWK_BLIND = 2f;                            //쪼은 적 실명 턴
	public static final float[] T4_HUNT_SIGNAL = {0f, 0.10f, 0.20f, 0.30f, 0.40f}; //사냥 신호: 다음 기본공격 피해 +
	public static final float   HAWK_SIGNAL_TURNS = 10f;                    //사냥 신호가 남아 있는 턴

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
	public static final float SAP_PHYS_REDUCE     = 0.80f; //물리 피해 감소 (불타거나 쪼그라든 동안은 없음)
	public static final float SAP_HALF_PHYS_BONUS = 0.30f; //(예전 규칙, 지금은 안 씀) 체력 절반 아래 물리 피해 추가
	public static final float SAP_BURN_UNTIL      = 0.40f; //불이 붙으면 체력이 이 비율이 될 때까지 계속 탄다
	public static final float[] SAP_STAGE_HP      = {0.80f, 0.65f, 0.50f}; //체력이 이 아래면 s1 / s2 / s3 (쪼그라든 모습)
	public static final int   SAP_STAGE_STEP_TURNS = 2;    //쪼그라드는 단계 하나에 머무는 턴 (단계를 건너뛰지 않음)
	public static final int   SAP_DROPS           = 3;     //다 쪼그라들면(s4) 흩뿌리는 수액 방울 수
	public static final int   SAP_DROP_RADIUS     = 2;     //방울을 흩뿌리는 범위 (칸)
	public static final int   SAP_DROPS_TO_HEAL   = 2;     //방울을 이만큼 흡수하면 기본 모습으로 돌아온다
	public static final int   BOSS_TORCHES        = 2;     //5층 보스층에 떨어져 있는 횃불 수 (비밀방 제외 무작위 바닥)
	public static final int   SAP_PUDDLE_MIN      = 3;     //내려칠 때 수액 웅덩이 칸 수
	public static final int   SAP_PUDDLE_MAX      = 4;
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
	public static final float WK_ORB_HASTE      = 2f;    //주인공이 구슬을 먹으면 신속 (갱신만, 더하지 않음)
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
	public static final int   EM_TILE_COOLDOWN  = 13;    //(지금은 안 씀) 예전 원소 감응 쿨타임. 이제 기술을 쓸 때마다 타일 수를 확인한다
	public static final float EM_SENSING_TIME   = 0.5f;  //원소 감응에 드는 시간 (턴)
	public static final int   EM_TILE_STEAM_MIN = 14;    //원소 타일 위 주인공 증기 반응 피해 (폭발은 기존 폭발 반응 피해)
	public static final int   EM_TILE_STEAM_MAX = 22;    //(결정, 밸런스) 10~16 → 14~22
	public static final int   EM_STORM_WARN     = 5;     //원소 폭풍 경고 턴
	public static final int   EM_STORM_FIRST_WARN = 3;   //처음 원소 폭풍만 경고 3턴
	public static final int   EM_STORM_TURNS    = 5;     //원소 폭풍 턴
	public static final int   EM_STORM_MIN      = 8;     //폭풍 구역 안 주인공 피해 (턴마다)
	public static final int   EM_STORM_MAX      = 14;    //(결정, 밸런스) 6~10 → 8~14

	// ===== 25층 최종보스: 심장을 진 짐승 (지시서 claude_code_tasks_1122_boss25.md + 1122b_boss25_update.md) =====
	//사슴(짐승) 기본 수치 (사용자 결정: 원작 요그와 같은 체력)
	public static final int   B25_HP            = 1000;
	public static final int   B25_EXP           = 50;
	public static final int   B25_DEF           = 20;    //회피
	public static final int   B25_ACC           = 30;    //명중
	public static final int   B25_DR            = 10;    //방어 0~10
	public static final int   B25_ROOM_W        = 17;    //보스방 안쪽 크기
	public static final int   B25_ROOM_H        = 13;
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
	public static final float SAP_FLOOR_LEVEL_CHANCE = 0.5f; //뿌리 지하 일반 층마다 수액 웅덩이 한 묶음이 생길 확률 (층과 상관없이 같다)
	public static final int   SAP_FLOOR_SIZE_MIN    = 2;     //한 묶음의 칸 수
	public static final int   SAP_FLOOR_SIZE_MAX    = 4;
	public static final float SEAL_GRASS            = 0.20f; //봉인의 회랑 잔디 (원작과 같은 0.20)
	public static final int   HEART_FLOOR_DECO_DENOM = 80;   //탑의 심장 바닥 살점 (작을수록 많다, 원작과 같은 80)
	public static final float HEART_FLOOR_ALT2_CHANCE = 5f;  //탑의 심장 바닥에 작은 혹·핏줄(FLOOR_ALT_2)이 섞이는 확률 % (원작과 같은 5)
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
