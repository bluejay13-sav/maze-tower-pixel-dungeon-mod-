/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;

//Maze Tower (B5): 2차 특성들이 함께 쓰는 판정. 구현 지시서 부록 2의 '공용 고리'를 본다.
//특성은 서로를 모르고, 특정 몹 클래스도 보지 않는다. 공용 상태(기절·속박·빙결·느려짐 등)만 본다.
public class BloomJudge {

	// =====================================================================
	// 움직이지 못하는 적: 기절(원작 Paralysis)·속박(Roots)·빙결(Frost)
	// =====================================================================

	public static boolean immobile(Char ch){
		if (ch == null) return false;
		return ch.paralysed > 0
				|| ch.buff(Paralysis.class) != null
				|| ch.buff(Roots.class) != null
				|| ch.buff(Frost.class) != null;
	}

	// =====================================================================
	// 느려진 적: 감속(Slow)·냉기(Chill)·혹한(Frostbite)·느려짐(원작 Cripple)
	// =====================================================================

	public static boolean slowed(Char ch){
		if (ch == null) return false;
		return ch.buff(Slow.class) != null
				|| ch.buff(Chill.class) != null
				|| ch.buff(Frostbite.class) != null
				|| ch.buff(Cripple.class) != null;
	}

	// =====================================================================
	// 나를 아직 보지 못한 적: 적이 주인공을 보았는지로만 본다 (잠듦, 아직 못 봄, 시야 밖, 주인공이 투명).
	// 원작 기습 판정(Mob.surprisedBy)에서 "주인공이 기습할 수 있는가"를 뺀 것이다.
	// 다른 특성이 주는 기습 판정(먼 과녁 각성 등)은 여기에 들어오지 않는다.
	// =====================================================================

	public static boolean unaware(Char ch, Hero hero){
		if (!(ch instanceof Mob) || hero == null) return false;
		Mob m = (Mob)ch;
		if (m.mazeNoSurprise()) return false; //(F5) 풀 속 덩굴뱀
		return m.state == m.SLEEPING || m.surprisedBy(hero, false);
	}

	// =====================================================================
	// 기습 판정: 원작 기습(Mob.surprisedBy: 나를 못 봄·시야 밖·내가 투명 등, 주인공이 기습할 수 있을 때)과
	// 잠든 적, 그리고 다른 특성이 주는 기습 판정(forcedSurprise, 예: 먼 과녁 각성)을 모두 포함한다.
	// =====================================================================

	public static boolean forcedSurprise = false;

	public static boolean surprised(Char ch, Hero hero){
		if (!(ch instanceof Mob) || hero == null) return false;
		Mob m = (Mob)ch;
		if (m.mazeNoSurprise()) return false; //(F5) 풀 속 덩굴뱀
		return forcedSurprise || m.state == m.SLEEPING || m.surprisedBy(hero, true);
	}

	// =====================================================================
	// 이 공격은 평타 판정인가 스킬 판정인가
	// 스킬: 검·단검·활 스킬(덧타격 포함), 강화된 평타, 지팡이 스킬.
	// 파생: 특성·반응이 만든 피해(BloomRules.Derived). 평타도 스킬도 아니다.
	// 평타: 그 밖의 주인공 공격 (발도처럼 평타 판정으로 정한 것 포함).
	// 주인공의 공격 처리 중(Hero.attackProc 등)에 부른다.
	// =====================================================================

	public enum AttackKind { NORMAL, SKILL, DERIVED }

	public static AttackKind attackKind(){
		if (BloomRules.isDerived()) return AttackKind.DERIVED;
		if (BloomEffects.isSkill() || BloomEffects.staffSkillWand != null) return AttackKind.SKILL;
		return AttackKind.NORMAL;
	}

	public static boolean isSkillAttack(){
		return attackKind() == AttackKind.SKILL;
	}

	public static boolean isNormalAttack(){
		return attackKind() == AttackKind.NORMAL;
	}

	// =====================================================================
	// 적의 공격으로 피가 줄지 않았다: 빗나감(회피), 방어막이 전부 막음, 쳐냄이 모두 해당한다.
	// 방어막이 일부만 막아 체력이 줄었으면 해당하지 않는다.
	// 적의 공격(Char.attack) 한 번이 끝날 때마다 판정한다 (afterEnemyAttack).
	// =====================================================================

	private static float unhurtTime = -1;
	private static int unhurtFrom = -1;

	//Char.attack: 적이 주인공을 공격한 뒤
	public static void afterEnemyAttack(Hero hero, Char attacker, boolean unhurt){
		if (hero == null || !hero.isAlive()) return;
		if (unhurt){
			unhurtTime = Actor.now();
			unhurtFrom = attacker.id();
			BloomRules.debug("unhurt by " + attacker.name());
			BloomEffects.onUnhurtByAttack(hero, attacker);
		} else {
			unhurtTime = -1;
			unhurtFrom = -1;
		}
	}

	//가장 최근에 받은 적의 공격이 피를 줄이지 않았는지 (그 뒤 다른 적의 공격에 피가 줄면 false)
	public static boolean lastAttackUnhurt(){
		return unhurtTime >= 0;
	}

	//가장 최근에 피를 줄이지 않은 공격을 한 적 (없으면 -1)
	public static int lastUnhurtAttacker(){
		return unhurtFrom;
	}

	public static void reset(){
		unhurtTime = -1;
		unhurtFrom = -1;
	}
}
