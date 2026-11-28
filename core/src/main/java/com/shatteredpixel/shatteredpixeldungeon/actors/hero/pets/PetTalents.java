/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;

//Maze Tower (T2, 지시서 claude_code_tasks_1126b_pets): 펫 강화.
//펫마다 고정된 강화 셋 + 원작 영웅적인 에너지(갑옷 충전 소모 감소). 뽑거나 고르지 않고, 4차 포인트로 0→4레벨(사용자: 넷 모두 4레벨까지).
//레벨은 수치만 올리고 각성은 없다. 효과는 쓰는 순간 지금 레벨로 계산하므로 펫이 나와 있는 동안 올려도 바로 반영된다.
//4차 특성 탭에는 고른 펫(hero.armorAbility)의 talents()만 보인다. 펫을 고르기 전에는 4차 줄이 없고 포인트도 쓸 수 없다(레벨로 계산되므로 쌓여 있다).
public class PetTalents {

	public static boolean isPetTalent( Talent t ){
		return t.name().startsWith("PET_");
	}

	//지금 레벨 (0~4). 펫을 고르지 않았거나 다른 펫의 강화면 0
	public static int level( Talent t ){
		if (Dungeon.hero == null) return 0;
		return Math.max(0, Math.min(4, Dungeon.hero.pointsInTalent(t)));
	}

	//레벨별 설명: properties의 "talent.<이름>.desc"에 %1$s~%5$s(0~4레벨 값)를 채운다
	public static String desc( Talent t ){
		String[] v;
		switch (t){
			case PET_BEAK:     v = pct(PET_BEAK_DMG); break;
			case PET_HAWK_EYE: v = num(PET_EYE_VIEW); break;
			case PET_FLAP:     v = join(pct(PET_FLAP_ACC), num(PET_FLAP_TURNS)); break;
			case PET_HIDE:     v = pct(PET_HIDE_BONUS); break;
			case PET_SHARE:    v = pct(PET_SHARE_RATIO); break;   //기본 PET_BEAR_SHARE에 더함
			case PET_ROAR:     v = num(PET_ROAR_TURNS); break;
			case PET_STANCE:   v = num(PET_STANCE_CUT); break;    //기본 PET_GUARD_STANCE_EVERY에서 뺌
			case PET_REQUITE:  v = pct(PET_REQUITE_DMG); break;
			case PET_OATH:     v = pct(PET_OATH_BONUS); break;
			case PET_ESSENCE:  v = join(pct(PET_ESSENCE_BURN), num(PET_ESSENCE_TURNS)); break;
			case PET_SHIFT:    v = num(PET_SHIFT_CUT); break;     //기본 PET_SPELL_SHIFT_CD에서 뺌
			case PET_TOUCH:    v = pct(PET_TOUCH_CHANCE); break;
			default: return Messages.get(Talent.class, t.name() + ".desc");
		}
		return Messages.get(Talent.class, t.name() + ".desc", (Object[]) v);
	}

	private static String[] pct( float[] a ){
		String[] s = new String[a.length];
		for (int i = 0; i < a.length; i++) s[i] = Math.round(a[i] * 100) + "%";
		return s;
	}

	private static String[] num( int[] a ){
		String[] s = new String[a.length];
		for (int i = 0; i < a.length; i++) s[i] = Integer.toString(a[i]);
		return s;
	}

	//앞 배열이 %1~%5, 뒤 배열이 %6~%10
	private static String[] join( String[] a, String[] b ){
		String[] s = new String[a.length + b.length];
		System.arraycopy(a, 0, s, 0, a.length);
		System.arraycopy(b, 0, s, a.length, b.length);
		return s;
	}
}
