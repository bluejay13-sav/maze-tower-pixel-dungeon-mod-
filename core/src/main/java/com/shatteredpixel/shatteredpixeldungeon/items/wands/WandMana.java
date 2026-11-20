/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomEffects;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.SproutEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Maze Tower: 마법봉·지팡이와 마나.
//  - 지팡이에 합성된 마법봉으로 쏘는 것 = 마법사 스킬: 마나(스킬 비용, 절약 적용) + 원작 충전
//    처음 받은 지팡이의 기본 마탄은 지팡이 평타 피해의 MAGE_BOLT_DMG (마탄 발사).
//    직접 합성한 막대는 StaffSkills의 합성 스킬 (강화 마탄·화염 창·서리 화살·낙뢰, 나머지는 원작 효과)
//  - 합성하지 않은 마법봉: 마나 15 + 원작 충전 (모든 직업)
//원작 Wand.tryToZap에서 마나가 충분한지 보고, 실제로 쏘는 순간(Wand.zapper) 마나를 쓴다.
public class WandMana {

	public static MagesStaff equippedStaff( Hero hero ){
		if (hero == null) return null;
		KindOfWeapon w = hero.belongings.weapon();
		return w instanceof MagesStaff ? (MagesStaff)w : null;
	}

	//이 마법봉이 주인공이 든 지팡이에 합성된 것인지
	public static boolean isStaffWand( Hero hero, Wand wand ){
		MagesStaff staff = equippedStaff(hero);
		return staff != null && staff.imbuedWand() == wand;
	}

	public static int manaCost( Hero hero, Wand wand ){
		if (!SwordSkill.heroUsesSkills(hero)) return 0;
		return isStaffWand(hero, wand) ? BloomEffects.wandSkillCost(hero, MazeTower.SKILL_MANA_COST) : MazeTower.LOOSE_WAND_MANA_COST; //흐름 각성 +15
	}

	//쏘기 전에: 마나가 모자라면 막는다
	public static boolean canZap( Hero hero, Wand wand ){
		int cost = manaCost(hero, wand);
		if (cost <= 0 || Mana.get(hero).cur() >= cost) return true;
		GLog.w(Messages.get(WandMana.class, "no_mana", cost));
		if (isStaffWand(hero, wand)) ActionIndicator.shake();
		return false;
	}

	//실제로 쏘는 순간: 지팡이 스킬이면 2차 스킬 특성을 시작하고, 마나를 쓴다
	public static void onZap( Hero hero, Wand wand, int targetCell ){
		int cost = manaCost(hero, wand);
		if (isStaffWand(hero, wand) && SwordSkill.heroUsesSkills(hero)){
			BloomEffects.beginStaffSkill(hero, wand, Actor.findChar(targetCell), cost);
			SproutEffects.onSkillUsed(hero); //1차 특성: 투지
		}
		if (cost > 0) Mana.get(hero).spend(cost);
	}

	//쏘기가 끝났을 때 (Wand.wandUsed)
	public static void afterZap( Wand wand ){
		BloomEffects.endStaffSkill(Dungeon.hero, wand);
	}

	// ===== 이름과 설명 =====

	//스킬 이름: 기본 마법봉은 '마탄 발사', 다른 마법봉은 '(마법봉 이름) 발사'
	public static String skillName( Wand wand ){
		//23단계: 합성한 막대에 따라 스킬 이름이 바뀐다 (StaffSkills)
		MagesStaff staff = equippedStaff(Dungeon.hero);
		if (staff != null && staff.imbuedWand() == wand){
			switch (StaffSkills.kind(staff)){
				case EMPOWERED_BOLT: return Messages.get(WandMana.class, "skill_empowered");
				case FIRE_LANCE:     return Messages.get(WandMana.class, "skill_fire_lance");
				case FROST_ARROW:    return Messages.get(WandMana.class, "skill_frost_arrow");
				case THUNDER:        return Messages.get(WandMana.class, "skill_thunder");
				default: break;
			}
		}
		if (wand instanceof WandOfMagicMissile) return Messages.get(WandMana.class, "skill_mm");
		return Messages.get(WandMana.class, "skill_wand", wand.name());
	}

	//지팡이 설명에 붙는 스킬 설명
	public static String staffSkillDesc( Hero hero, MagesStaff staff ){
		Wand w = staff.imbuedWand();
		if (w == null) return "";
		String effect;
		switch (StaffSkills.kind(staff)){
			case BASE_BOLT:
				effect = Messages.get(WandMana.class, "staff_mm_effect", pct(MazeTower.MAGE_BOLT_DMG)); break;
			case EMPOWERED_BOLT:
				effect = Messages.get(WandMana.class, "staff_empowered_effect", pct(MazeTower.STAFF_EMPOWERED_BOLT_DMG)); break;
			case FIRE_LANCE:
				effect = Messages.get(WandMana.class, "staff_fire_lance_effect", pct(MazeTower.STAFF_FIRE_LANCE_DMG), MazeTower.FIRE_LANCE_LENGTH); break;
			case FROST_ARROW:
				effect = Messages.get(WandMana.class, "staff_frost_arrow_effect", pct(MazeTower.STAFF_FROST_ARROW_DMG), Math.round(MazeTower.FROSTBITE_TURNS)); break;
			case THUNDER:
				effect = Messages.get(WandMana.class, "staff_thunder_effect", pct(MazeTower.STAFF_THUNDER_DMG), MazeTower.THUNDER_MAX_TARGETS); break;
			default:
				effect = Messages.get(WandMana.class, "staff_wand_effect"); break;
		}
		return Messages.get(WandMana.class, "staff_desc", skillName(w), SwordSkill.manaCost(hero), effect);
	}

	private static int pct( float multi ){
		return Math.round(multi * 100);
	}

	//합성하지 않은 마법봉 설명에 붙는 마나 안내
	public static String looseWandDesc( Hero hero, Wand wand ){
		if (!SwordSkill.heroUsesSkills(hero) || isStaffWand(hero, wand)) return "";
		return Messages.get(WandMana.class, "loose_desc", MazeTower.LOOSE_WAND_MANA_COST);
	}

	//마탄 발사: 처음 받은 지팡이의 기본 마탄 피해를 지팡이 평타 피해의 MAGE_BOLT_DMG(150%)로
	public static int magicMissileDamage( Char owner, Wand wand, int original ){
		if (!(owner instanceof Hero)) return original;
		Hero hero = (Hero)owner;
		MagesStaff staff = equippedStaff(hero);
		if (staff == null || staff.imbuedWand() != wand || !SwordSkill.heroUsesSkills(hero)) return original;
		return Math.round(staff.damageRoll(hero) * MazeTower.MAGE_BOLT_DMG);
	}
}
