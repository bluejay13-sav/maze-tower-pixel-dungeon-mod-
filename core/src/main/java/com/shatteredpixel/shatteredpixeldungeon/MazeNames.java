/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

//Maze Tower: 캐릭터 이름 (명세 17번).
//영웅 선택 화면에서 시작할 때 정하고(HeroSelectScene), 영웅에 저장된다(Hero.mazeName).
//비워 두면 직업 이름을 쓴다. 영웅 정보 창, 랭킹, 사망·엔딩 메시지, NPC가 부르는 이름에 보인다.
//나중에 익스트랙션의 캐릭터 보관함·무덤에서도 쓴다(지금은 저장만).
public class MazeNames {

	public static final int MAX_LENGTH = 12;

	//새 게임을 시작할 때 쓸 이름 (Dungeon.init에서 영웅에게 옮긴다)
	public static String pending = "";

	//앞뒤 공백을 지우고, 한글·영어·숫자·공백만 남기고, 최대 12글자
	public static String clean( String text ){
		if (text == null) return "";
		StringBuilder sb = new StringBuilder();
		for (char c : text.trim().toCharArray()){
			boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
					|| (c >= '가' && c <= '힣')   //한글 음절
					|| (c >= 'ㄱ' && c <= 'ㆎ')   //한글 자모
					|| c == ' ';
			if (ok) sb.append(c);
			if (sb.length() >= MAX_LENGTH) break;
		}
		return sb.toString().trim();
	}
}
