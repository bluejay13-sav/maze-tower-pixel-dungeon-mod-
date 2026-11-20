/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower: 2차(개화) 특성 30개 (2차 특성 개편, docs/sprites/talent2/TALENT2_SPEC.md). 거리 벌리기는 삭제했다.
//기본공격 전용 9, 스킬 전용 9, 공용 12. 계열은 1~2개이고, 선택지는 첫 번째 계열이 서로 다르게 뽑는다.
//아이콘 번호는 talent2_icons.png (16×16, 가로 8칸) 순서. 31번은 분홍 꽃잎.
public enum BloomTalent {

	//기본공격 전용
	DOUBLE_SLASH  ( 0, Kind.ATTACK, Family.COMBO),                   //연속 베기
	HEAVY_BLOW    ( 1, Kind.ATTACK, Family.COMBO),                   //강타
	GALE          ( 2, Kind.ATTACK, Family.COMBO),                   //질풍
	MARK_CARVE    ( 3, Kind.ATTACK, Family.MARK),                    //표식 새기기
	CHARGE        ( 4, Kind.ATTACK, Family.TACTIC, Family.COMBO),    //돌진
	FLAME_BLADE   ( 5, Kind.ATTACK, Family.ELEMENT),                 //불꽃 칼날
	SPLASH        ( 6, Kind.ATTACK, Family.ELEMENT),                 //물보라
	MANA_DRAIN    ( 7, Kind.ATTACK, Family.MARK, Family.MANA),       //마나 흡수
	SURE_STRIKE   ( 8, Kind.ATTACK, Family.COMBO, Family.TACTIC),    //확정타

	//스킬 전용
	CHAIN         ( 9, Kind.SKILL, Family.MANA, Family.COMBO),       //연쇄
	MARK_BURST    (10, Kind.SKILL, Family.MARK),                     //표식 폭발
	FOCUS         (11, Kind.SKILL, Family.TACTIC),                   //집중
	EXECUTE       (12, Kind.SKILL, Family.MANA, Family.MARK),        //처형
	OVERCHARGE    (13, Kind.SKILL, Family.MANA),                     //과충전
	IMPACT        (14, Kind.SKILL, Family.TACTIC),                   //충격
	FROST_SLASH   (15, Kind.SKILL, Family.ELEMENT),                  //서리 베기
	PARRY         (16, Kind.SKILL, Family.SURVIVAL),                 //흘리기
	FLASH_STEP    (17, Kind.SKILL, Family.TACTIC, Family.COMBO),     //섬광 보법

	//공용
	FLOW          (18, Kind.COMMON, Family.MARK),                    //흐름
	ECHO          (19, Kind.COMMON, Family.MARK, Family.MANA),       //잔향
	MOMENTUM      (20, Kind.COMMON, Family.COMBO),                   //기세
	MELEE         (21, Kind.COMMON, Family.COMBO, Family.SURVIVAL),  //난전
	DUEL          (22, Kind.COMMON, Family.TACTIC, Family.SURVIVAL), //결투
	AMBUSH        (23, Kind.COMMON, Family.TACTIC),                  //기습
	RESIDUAL_SHOCK(25, Kind.COMMON, Family.ELEMENT),                 //잔류전기
	ELEMENT_AMP   (26, Kind.COMMON, Family.ELEMENT),                 //원소 증폭
	ELEMENT_CYCLE (27, Kind.COMMON, Family.ELEMENT, Family.MANA),    //원소 순환
	MANA_VEIL     (28, Kind.COMMON, Family.SURVIVAL),                //마나 장막
	OVERFLOW      (29, Kind.COMMON, Family.MANA),                    //넘치는 힘
	MEDITATION    (30, Kind.COMMON, Family.MANA, Family.SURVIVAL);   //명상

	//분홍 꽃잎 아이콘 번호 (talent2_icons.png)
	public static final int PETAL_ICON = 31;

	public enum Kind { ATTACK, SKILL, COMMON }

	//계열: 연타, 표식, 마력, 원소, 전술, 생존
	public enum Family {
		COMBO, MARK, MANA, ELEMENT, TACTIC, SURVIVAL;

		public String title(){
			return Messages.get(BloomTalent.class, "family_" + name().toLowerCase());
		}
	}

	private final int icon;
	public final Kind kind;
	public final Family[] families;

	BloomTalent(int icon, Kind kind, Family... families){
		this.icon = icon;
		this.kind = kind;
		this.families = families;
	}

	public int icon(){
		return icon;
	}

	//선택지를 뽑을 때 쓰는 계열 (첫 번째 계열)
	public Family family(){
		return families[0];
	}

	public String title(){
		return Messages.get(this, name() + ".title");
	}

	//분류·계열 + 레벨별 효과 + 각성
	public String desc(){
		StringBuilder fam = new StringBuilder();
		for (Family f : families){
			if (fam.length() > 0) fam.append("·");
			fam.append(f.title());
		}
		return Messages.get(BloomTalent.class, "header", Messages.get(BloomTalent.class, "kind_" + kind.name().toLowerCase()), fam.toString())
				+ "\n\n" + Messages.get(this, name() + ".desc")
				+ "\n\n" + Messages.get(BloomTalent.class, "awaken_label") + " " + Messages.get(this, name() + ".awaken");
	}

	//선택 창에서 보여 줄 짧은 설명
	public String shortDesc(){
		return Messages.get(this, name() + ".short");
	}
}
