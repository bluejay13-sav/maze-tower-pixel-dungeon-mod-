/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;

//Maze Tower: 3차 전직 검투사(만개)의 규칙과 특성 4개 (명세 12번).
//원작 Combo(연속 타격)와 필살기는 그대로 쓰고, 원작 코드에는 호출 한 줄씩만 넣는다.
public class GladiatorEffects {

	static int pts(Hero hero, Talent t){
		return hero == null ? 0 : Math.min(T3_MAX_TALENT_LEVEL, hero.pointsInTalent(t));
	}

	public static boolean isGladiator(Hero hero){
		return hero != null && hero.subClass == HeroSubClass.GLADIATOR;
	}

	// ===== 야성 =====

	//적을 처치한 뒤 연속 타격이 유지되는 턴
	public static float killComboTime(Hero hero){
		return T3_FERAL_TIME[pts(hero, Talent.CLEAVE)];
	}

	// ===== 향상된 콤보 =====

	public static boolean clobberEmpowered(Hero hero, int count){
		return pts(hero, Talent.ENHANCED_COMBO) >= 1 && count >= T3_COMBO_CLOBBER;
	}

	public static boolean parryEmpowered(Hero hero, int count){
		return pts(hero, Talent.ENHANCED_COMBO) >= 2 && count >= T3_COMBO_PARRY;
	}

	public static boolean furyEmpowered(Hero hero, int count){
		return pts(hero, Talent.ENHANCED_COMBO) >= 3 && count >= T3_COMBO_FURY;
	}

	public static boolean refundEmpowered(Hero hero, int count){
		return pts(hero, Talent.ENHANCED_COMBO) >= 4 && count >= T3_COMBO_REFUND;
	}

	//분노 폭발이 적 방어력 일부를 무시하는 중 (Char.attack에서 방어력에 곱한다)
	private static boolean furyIgnoring = false;

	public static int reduceDefense(int dr){
		if (furyIgnoring) return Math.round(dr * (1f - T3_FURY_ARMOR_IGNORE));
		return dr;
	}

	// ===== 필살기 = 스킬 공격 =====

	//필살기 타격 중이면 true. 2차 스킬 특성(날 세우기·집중·처형 등)이 이 값을 보고 스킬로 처리한다.
	public static boolean finisherActive = false;

	//필살기 시작 (분노 폭발은 첫 타격에서만): 2차 스킬 특성 시작(마나 0), 1차 투지
	public static void onFinisherStart(Hero hero, Char enemy){
		BloomEffects.beginSkill(hero, enemy, 0);
		SproutEffects.onSkillUsed(hero);
	}

	//필살기 타격 한 번 (hero.attack 앞뒤)
	public static void beginFinisherHit(Hero hero, Combo.ComboMove move, int count){
		finisherActive = true;
		furyIgnoring = move == Combo.ComboMove.FURY && furyEmpowered(hero, count);
	}

	public static void endFinisherHit(){
		finisherActive = false;
		furyIgnoring = false;
	}

	//필살기가 끝났을 때
	//startCount: 필살기를 쓰기 직전의 연속 타격, resets: 연속 타격을 초기화하는 필살기인지
	public static void onFinisherEnd(Hero hero, Char enemy, int startCount, boolean resets, boolean killed){
		BloomEffects.endSkill(hero); //흐름·잔향·흘리기 (쓴 마나 0이라 마나 반환은 0)

		if (!killed) return;

		//기술 연계 4레벨: 필살기로 처치하면 마나
		if (pts(hero, Talent.SKILL_LINK) >= 4){
			Mana.get(hero).gain(T3_SKILL_LINK_MANA);
			hero.sprite.showStatus(0x66AAFF, "+" + T3_SKILL_LINK_MANA);
		}

		//향상된 콤보 4레벨: 초기화되는 필살기로 처치하면 연속 타격 절반을 돌려받는다
		if (resets && refundEmpowered(hero, startCount)){
			int back = startCount / 2;
			if (back > 0){
				Buff.affect(hero, Combo.class).mazeRestore(back, killComboTime(hero));
			}
		}
	}

	// ===== 치명적인 방어 =====

	//필살기로 처치했을 때 (원작 자리에서 호출)
	public static void onFinisherKill(Hero hero){
		int lvl = pts(hero, Talent.LETHAL_DEFENSE);
		if (lvl <= 0) return;
		Buff.affect(hero, BrokenSeal.WarriorShield.class).reduceCooldownTurns(T3_LETHAL_COOLDOWN[lvl]);
	}

	//4레벨: 인장 방어막 + 최대 체력의 10% (원작 공식에 더한다)
	public static int sealShieldBonus(Hero hero){
		if (pts(hero, Talent.LETHAL_DEFENSE) < 4) return 0;
		return (int)(hero.HT * T3_LETHAL_SHIELD_HP);
	}

	// ===== 무기 스킬의 연속 타격 =====

	private static boolean skillLanded = false;

	//무기 스킬 시작
	public static void onSkillStart(){
		skillLanded = false;
	}

	//무기 스킬 타격 한 번마다: 맞히면 연속 타격 +1
	public static void onSkillHit(Hero hero, Char enemy, boolean hit, boolean wasEnemy){
		if (!hit || !isGladiator(hero) || !wasEnemy) return;
		skillLanded = true;
		Buff.affect(hero, Combo.class).hit(enemy);
	}

	public static boolean wasEnemy(Char enemy){
		return enemy.alignment == Char.Alignment.ENEMY
				|| (enemy instanceof Mimic && enemy.alignment == Char.Alignment.NEUTRAL);
	}

	//무기 스킬 타격이 모두 끝난 뒤: 기술 연계
	public static void onSkillEnd(Hero hero){
		boolean landed = skillLanded;
		skillLanded = false;
		if (!landed || !isGladiator(hero)) return;
		int lvl = pts(hero, Talent.SKILL_LINK);
		if (lvl <= 0) return;
		Combo combo = hero.buff(Combo.class);
		if (combo == null) return;
		combo.mazeAddCount(T3_SKILL_LINK[lvl]);
		GLog.p(Messages.get(Combo.class, "combo", combo.getComboCount()));
	}
}
