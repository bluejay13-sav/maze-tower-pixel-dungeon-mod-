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

	//직업 선택 없이 시작하는 주인공 (내부적으로 원작 직업 하나를 재사용)
	public static final HeroClass HERO = HeroClass.WARRIOR;

	//원작 직업 특성(1~4층 특성 줄)을 쓸지 여부
	public static final boolean ORIGINAL_TALENTS = false;

	//원작 서브클래스(텐구의 가면)를 얻을 수 있는지 여부
	public static final boolean ORIGINAL_SUBCLASS = false;

	//원작 갑옷 능력(왕의 왕관)을 얻을 수 있는지 여부
	public static final boolean ORIGINAL_ARMOR_ABILITY = false;

	//원작 전사의 부서진 인장(방어막)을 시작 장비로 줄지 여부
	public static final boolean WARRIOR_SEAL = false;

	// ===== 마나 =====

	//최대 마나
	public static final int MANA_MAX = 100;

	//대기(휴식·탐색 포함)로 넘긴 한 턴마다 얻는 마나. 다른 행동으로는 얻지 못한다.
	public static final int MANA_PER_WAIT_TURN = 10;

	// ===== 검 계열 스킬 =====

	//모든 스킬의 마나 비용
	public static final int SKILL_MANA_COST = 30;

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

	//1차 포인트를 주는 레벨 구간 (1차 능력은 미완성, 쌓이기만 한다)
	public static final int FIRST_TIER_FIRST_LEVEL = 2;
	public static final int FIRST_TIER_LAST_LEVEL  = 6;

}
