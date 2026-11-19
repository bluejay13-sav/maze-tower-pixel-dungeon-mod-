/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

//Maze Tower: 마탑 보스 (24단계, 명세 19번 + 기획 문서 "마탑 보스 외형과 패턴 초안").
//  5층 썩은 수액 덩어리 (원작 구)     10층 그림자 간수 (원작 텐구)
//  15층 마도 골렘 또는 수정 거인 (원작 DM-300, 판마다 무작위, 던전 시드를 따름)
//  20층 망령 왕 또는 마도시의 여제 (원작 드워프 왕, 판마다 무작위, 지금은 외형·이름만)
//  25층 탑의 주인 (원작 요그, 지금은 외형·이름만)
public class MazeBosses {

	//마탑 보스 외형·패턴을 쓸지 (false면 원작 보스 그대로)
	public static final boolean ENABLED = true;

	//15층: 수정 거인이 나오는 판인지 (아니면 마도 골렘). 같은 시드면 같은 보스.
	//치트로 바꾼 보스 (저장되지 않음, 시험용). null이면 시드대로
	public static Boolean force15 = null;
	public static Boolean force20 = null;

	public static boolean crystalGiant(){
		if (ENABLED && force15 != null) return force15;
		return ENABLED && new java.util.Random(Dungeon.seed ^ 0x15C0FFEEL).nextBoolean();
	}

	//20층: 마도시의 여제가 나오는 판인지 (아니면 망령 왕). 같은 시드면 같은 보스.
	public static boolean empress(){
		if (ENABLED && force20 != null) return force20;
		return ENABLED && new java.util.Random(Dungeon.seed ^ 0x20E3B2E5L).nextBoolean();
	}

	//보스 이름·설명 문자열 키 ("name" / "desc"). 마탑 보스가 아니면 null (원작 글)
	//대사(yell)는 원작 그대로 두고 27단계 글 바꾸기 때 다시 쓴다.
	public static String textKey( Object boss, String what ){
		if (!ENABLED) return null;
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo
				|| boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu
				|| boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa){
			return "maze_" + what;
		}
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300){
			return (crystalGiant() ? "maze_crystal_" : "maze_golem_") + what;
		}
		//수정 거인 판의 기둥은 수정 기둥
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon && crystalGiant()){
			return "maze_crystal_" + what;
		}
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing){
			return (empress() ? "maze_empress_" : "maze_wraith_") + what;
		}
		return null;
	}
}
