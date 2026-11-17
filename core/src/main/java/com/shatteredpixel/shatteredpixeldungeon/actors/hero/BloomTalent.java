/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower: 2차 특성 24개.
//아이콘은 임시로 원작 특성 아이콘 번호를 재사용한다 (Assets.Interfaces.TALENT_ICONS).
//효과는 7단계에서 구현한다.
public enum BloomTalent {

	//기본공격
	PRECISION(137),        //정밀타격
	COMBO(16),             //연격
	SHADOW_BLADE(87),      //그림자검
	WEAK_POINT(152),       //약점 노리기
	LIFE_STEAL(46),        //생명탈취
	FLOW(138),             //흐름

	//스킬
	SHARPEN(14),           //날 세우기
	CHAIN(18),             //연쇄
	FOCUS(130),            //집중
	EXECUTE(7),            //처형
	ECHO(23),              //잔향
	PARRY(15),             //흘리기

	//마나
	MANA_DRAIN(47),        //마나 흡수
	MANA_ATTUNE(44),       //마나 감응
	THRIFT(54),            //절약
	MEDITATION(39),        //명상
	OVERFLOW(45),          //넘치는 힘
	MANA_VEIL(51),         //마나 장막

	//속성
	FLAME_BLADE(53),       //불꽃 칼날
	FROST_SLASH(50),       //서리 베기
	RESIDUAL_SHOCK(22),    //잔류전기
	LINGERING_ELEMENT(34), //잔류원소
	ELEMENT_BURST(49),     //원소 폭발
	ELEMENT_AFFINITY(111); //원소 친화

	private final int icon;

	BloomTalent(int icon){
		this.icon = icon;
	}

	public int icon(){
		return icon;
	}

	public String title(){
		return Messages.get(this, name() + ".title");
	}

	public String desc(){
		return Messages.get(this, name() + ".desc");
	}

	//0레벨 효과만 짧게
	public String shortDesc(){
		return Messages.get(this, name() + ".short");
	}
}
