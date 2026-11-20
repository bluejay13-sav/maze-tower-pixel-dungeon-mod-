/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 2차 공용 특성 13개 (흐름, 잔향, 기세, 난전, 결투, 기습, 거리 벌리기, 잔류전기, 원소 증폭, 원소 순환, 마나 장막, 넘치는 힘, 명상).
//평타·스킬 모두에 붙는다. 잔류전기·마나 장막·명상의 기본 효과는 BloomEffects에 있고, 여기서는 각성 등 나머지를 맡는다.
//횟수·파생·마나 규칙은 BloomRules를 거친다.
public class BloomCommon {

	private static int lvl(Hero hero, BloomTalent t){
		return BloomEffects.lvl(hero, t);
	}

	private static boolean awake(Hero hero, BloomTalent t){
		return BloomTree.awakened(hero, t);
	}

	// =====================================================================
	// 이번 행동의 상태
	// =====================================================================

	static float flowBonus      = 0;     //흐름: 이번 공격에 붙는 피해
	static boolean flowActive   = false; //흐름: 번갈아 쓴 공격 (3레벨 명중)
	static boolean disengage    = false; //거리 벌리기: 물러난 뒤 첫 공격
	static boolean cycleElement = false; //원소 순환 각성: 이번 공격이 무작위 원소를 건다
	static int duelTarget       = -1;    //결투 상대
	static int ambushTarget     = -1;    //기습한 대상
	static int lastDamage       = 0;     //마지막 타격 피해 (기세 각성 충격파)
	static int lastSkillDamage  = 0;     //마지막 스킬의 첫 타격 피해 (잔향 각성 잔상, 행동이 바뀌어도 남는다)
	private static boolean skillDealtDamage = false;

	//평타·스킬 시작 (BloomEffects.beginNormal / beginSkill)
	static void onActionStart(Hero hero, boolean skill){
		flowBonus = 0;
		flowActive = false;
		disengage = false;
		cycleElement = false;
		duelTarget = -1;
		ambushTarget = -1;
		lastDamage = 0;
		if (skill) skillDealtDamage = false;

		//흐름: 평타와 스킬을 번갈아 쓰면 이번 공격이 강해진다. 같은 종류를 연달아 쓰면 처음부터.
		int f = lvl(hero, FLOW);
		if (f >= 0){
			BloomEffects.FlowTracker ft = Buff.affect(hero, BloomEffects.FlowTracker.class);
			int kind = skill ? 2 : 1;
			if (ft.lastKind != 0 && ft.lastKind != kind){
				ft.stacks = awake(hero, FLOW) ? Math.min(B2_FLOW_AWAKE_STACKS, ft.stacks + 1) : 1;
				flowBonus = B2_FLOW_DMG[f] * ft.stacks;
				flowActive = true;
			} else {
				ft.stacks = 0;
			}
			ft.lastKind = kind;
		}

		//거리 벌리기: 물러난 뒤 맞지 않고 하는 첫 공격
		if (hero.buff(BloomBuffs.DisengageReady.class) != null){
			Buff.detach(hero, BloomBuffs.DisengageReady.class);
			if (lvl(hero, DISENGAGE) >= 0 && hero.buff(BloomBuffs.DisengageCooldown.class) == null){
				disengage = true;
				Buff.affect(hero, BloomBuffs.DisengageCooldown.class, B2_DISENGAGE_COOLDOWN);
			}
		}

		//원소 순환 각성: 반응을 일으킨 다음 공격
		if (hero.buff(BloomBuffs.CycleCharge.class) != null){
			Buff.detach(hero, BloomBuffs.CycleCharge.class);
			cycleElement = true;
		}
	}

	// =====================================================================
	// 명중 (평타·스킬 모두)
	// =====================================================================

	static float accuracyMulti(Hero hero){
		float acc = 1f;
		if (flowActive && lvl(hero, FLOW) >= 2) acc *= 1f + B2_FLOW_ACC;
		int d = lvl(hero, DISENGAGE);
		if (disengage && d >= 0) acc *= 1f + B2_DISENGAGE_BONUS[d];
		return acc;
	}

	//스킬 마나 비용 증가 (흐름 각성). wand = 지팡이 스킬
	static int extraSkillCost(Hero hero, boolean wand){
		if (lvl(hero, FLOW) >= 0 && awake(hero, FLOW)) return wand ? B2_FLOW_AWAKE_WAND_COST : B2_FLOW_AWAKE_COST;
		return 0;
	}

	// =====================================================================
	// 피해 배율 (평타·스킬 타격마다)
	// =====================================================================

	static float hitBonus(Hero hero, Char enemy, boolean skill){
		float bonus = flowBonus;

		//기세: 1스택당
		int mo = lvl(hero, MOMENTUM);
		BloomBuffs.Momentum mom = hero.buff(BloomBuffs.Momentum.class);
		if (mo >= 0 && mom != null) bonus += B2_MOMENTUM_DMG[mo] * mom.stacks;

		//난전: 인접한 적 1명당 (최대 4명)
		int me = lvl(hero, MELEE);
		if (me >= 0) bonus += B2_MELEE_DMG[me] * Math.min(B2_MELEE_MAX, adjacentEnemies(hero));

		//결투: 2칸 안에 적이 그 하나뿐일 때
		int du = lvl(hero, DUEL);
		if (du >= 0 && duelOpponent(hero) == enemy){
			bonus += B2_DUEL_DMG[du];
			duelTarget = enemy.id();
		}

		//기습: 나를 알아채지 못한 적
		int am = lvl(hero, AMBUSH);
		if (am >= 0 && enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero, false)){
			bonus += B2_AMBUSH_DMG[am];
			ambushTarget = enemy.id();
		}

		//거리 벌리기
		int d = lvl(hero, DISENGAGE);
		if (disengage && d >= 0) bonus += B2_DISENGAGE_BONUS[d];

		return bonus;
	}

	// =====================================================================
	// 적중 시 효과 (피해가 정해진 뒤)
	// =====================================================================

	static void onHit(final Hero hero, final Char enemy, int damage, boolean skill){
		lastDamage = damage;
		if (skill && !skillDealtDamage){
			skillDealtDamage = true;
			lastSkillDamage = damage;
		}

		//잔향 (평타 적중 시 마나), 각성: 방금 쓴 스킬의 잔상 (파생) 또는 마나 10
		int ec = lvl(hero, ECHO);
		if (!skill && ec >= 0 && hero.buff(BloomEffects.EchoTracker.class) != null && BloomRules.triggerOnHit(ECHO)){
			BloomRules.gainMana(hero, B2_ECHO_MANA[ec]);
			if (awake(hero, ECHO)){
				BloomEffects.EchoTracker et = hero.buff(BloomEffects.EchoTracker.class);
				if (et.skillDamage > 0){
					BloomRules.derivedDamage(ECHO, enemy, Math.max(1, Math.round(et.skillDamage * B2_ECHO_AWAKE_DMG)));
					com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.slash(hero, enemy, 0x9FB8FF); //방금 쓴 스킬의 잔상 (푸른 베기 궤적)
				} else {
					BloomRules.gainMana(hero, B2_ECHO_AWAKE_MANA);
				}
			}
		}

		//원소 순환 각성: 무작위 원소 (화상·냉각·정전기)
		if (cycleElement && BloomRules.trigger(ELEMENT_CYCLE)){
			cycleElement = false;
			final int pick = Random.Int(3);
			BloomAttack.afterHit(new Runnable() {
				@Override
				public void run() {
					if (!enemy.isAlive()) return;
					if (pick == 0)      BloomEffects.ignite(hero, enemy, B2_CYCLE_AWAKE_TURNS);
					else if (pick == 1) BloomEffects.chill(hero, enemy, B2_CYCLE_AWAKE_TURNS);
					else                Buff.prolong(enemy, StaticCharge.class, B2_CYCLE_AWAKE_TURNS);
				}
			});
		}

		//넘치는 힘 각성: 마나가 가득 찬 동안 공격마다 마력 파편 (파생, 한 행동에 한 번)
		if (awake(hero, OVERFLOW) && lvl(hero, OVERFLOW) >= 0 && Mana.get(hero).isFull() && BloomRules.trigger(OVERFLOW)){
			ArrayList<Char> near = new ArrayList<>();
			for (Char c : Actor.chars()){
				if (c.isAlive() && c.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance(hero.pos, c.pos) <= B2_OVERFLOW_AWAKE_RANGE
						&& Dungeon.level.heroFOV[c.pos]){
					near.add(c);
				}
			}
			if (!near.isEmpty()){
				final Char t = Random.element(near);
				final int dmg = Math.max(1, Math.round(damage * B2_OVERFLOW_AWAKE_DMG));
				BloomRules.derivedDamage(OVERFLOW, t, dmg);
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.shard(hero, t); //마력 파편
			}
		}
	}

	//스킬이 끝났을 때: 잔향에 이번 스킬 피해를 적어 둔다 (BloomEffects.endSkill)
	static void onSkillEnd(Hero hero){
		BloomEffects.EchoTracker et = hero.buff(BloomEffects.EchoTracker.class);
		if (et != null) et.skillDamage = skillDealtDamage ? lastSkillDamage : 0;
	}

	// =====================================================================
	// 처치 (BloomRules.onDeath)
	// =====================================================================

	static void onKill(Hero hero, Char enemy, boolean byDerived){
		//기세: 처치마다 1스택 (최대 3), 10턴 동안 처치하지 못하면 사라진다
		int mo = lvl(hero, MOMENTUM);
		if (mo >= 0){
			BloomBuffs.Momentum m = hero.buff(BloomBuffs.Momentum.class);
			boolean wasMax = m != null && m.stacks >= B2_MOMENTUM_MAX;
			m = Buff.prolong(hero, BloomBuffs.Momentum.class, B2_MOMENTUM_TURNS);
			//각성: 가득 찬 상태로 처치하면 2칸 충격파 (파생), 기세는 절반으로
			if (wasMax && awake(hero, MOMENTUM) && BloomRules.trigger(MOMENTUM)){
				int dmg = Math.max(1, Math.round(lastDamage * B2_MOMENTUM_AWAKE_DMG));
				for (Char c : Actor.chars()){
					if (c != enemy && c.isAlive() && c.alignment == Char.Alignment.ENEMY
							&& Dungeon.level.distance(hero.pos, c.pos) <= B2_MOMENTUM_AWAKE_RANGE){
						BloomRules.derivedDamage(MOMENTUM, c, dmg);
					}
				}
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.shockwave(hero, B2_MOMENTUM_AWAKE_RANGE); //2칸 충격파
				PixelScene.shake(1, 0.2f);
				m.stacks = m.stacks / 2;
			} else {
				m.stacks = Math.min(B2_MOMENTUM_MAX, m.stacks + 1);
			}
		}

		//결투 각성: 결투 상대를 처치하면 최대 체력 15% 회복
		if (!byDerived && enemy.id() == duelTarget && awake(hero, DUEL) && lvl(hero, DUEL) >= 0 && BloomRules.trigger(DUEL)){
			int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * B2_DUEL_AWAKE_HEAL));
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}

		//기습 각성: 기습으로 처치하면 1턴 투명
		if (!byDerived && enemy.id() == ambushTarget && awake(hero, AMBUSH) && lvl(hero, AMBUSH) >= 0 && BloomRules.trigger(AMBUSH)){
			Buff.affect(hero, Invisibility.class, B2_AMBUSH_AWAKE_INVIS);
		}
	}

	// =====================================================================
	// 받는 피해
	// =====================================================================

	//공격을 받았을 때 (Hero.defenseProc → BloomEffects.onHeroDefense): 거리 벌리기가 풀린다
	static void onHeroHit(Hero hero){
		Buff.detach(hero, BloomBuffs.DisengageReady.class);
	}

	//받는 모든 피해 배율 (Hero.damage → BloomEffects.onHeroDamage): 난전 각성, 결투 3레벨
	static float incomingMulti(Hero hero){
		float m = 1f;
		if (awake(hero, MELEE) && lvl(hero, MELEE) >= 0 && adjacentEnemies(hero) >= B2_MELEE_GUARD_COUNT) m *= 1f - B2_MELEE_GUARD;
		if (lvl(hero, DUEL) >= 2 && duelOpponent(hero) != null) m *= 1f - B2_DUEL_GUARD;
		return m;
	}

	//마나 장막 각성: 장막이 발동하면 인접한 적을 1칸 밀친다
	static void veilPush(Hero hero){
		if (!awake(hero, MANA_VEIL)) return;
		for (int n : PathFinder.NEIGHBOURS8){
			Char c = Actor.findChar(hero.pos + n);
			if (c != null && c.alignment == Char.Alignment.ENEMY){
				BloomAttack.pushAway(hero, c, B2_VEIL_AWAKE_PUSH);
			}
		}
	}

	// =====================================================================
	// 이동 (Hero.move): 거리 벌리기
	// =====================================================================

	static void onMove(Hero hero, int from){
		if (lvl(hero, DISENGAGE) < 0 || hero.buff(BloomBuffs.DisengageCooldown.class) != null) return;
		//붙어 있던 적에게서 멀어졌는지
		boolean awayFromAny = false;
		for (Char c : Actor.chars()){
			if (c.isAlive() && c.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.adjacent(from, c.pos) && !Dungeon.level.adjacent(hero.pos, c.pos)){
				awayFromAny = true;
				break;
			}
		}
		if (!awayFromAny) return;
		Buff.affect(hero, BloomBuffs.DisengageReady.class);
		//각성: 물러난 칸에 잔상 (밟은 적은 1턴 속박, 다시 퍼지지 않는 장판)
		if (awake(hero, DISENGAGE)){
			Blob.seed(from, B2_AFTERIMAGE_TURNS, BloomBuffs.Afterimage.class, Dungeon.level);
		}
	}

	// =====================================================================
	// 원소 반응 (ElementalReactions → BloomEffects.onReaction)
	// =====================================================================

	static void onReaction(Hero hero, Char ch){
		//원소 순환: 마나 (한 행동에 한 번), 각성: 다음 공격이 무작위 원소
		int cy = lvl(hero, ELEMENT_CYCLE);
		if (cy >= 0 && BloomRules.trigger(ELEMENT_CYCLE)){
			BloomRules.gainMana(hero, B2_CYCLE_MANA[cy]);
			if (awake(hero, ELEMENT_CYCLE)) Buff.affect(hero, BloomBuffs.CycleCharge.class);
		}
		//원소 증폭 각성: 반응이 일어난 적 2턴 약화 (쿨타임 3턴)
		if (awake(hero, ELEMENT_AMP) && lvl(hero, ELEMENT_AMP) >= 0 && ch.isAlive()
				&& ch.alignment == Char.Alignment.ENEMY && hero.buff(BloomBuffs.AmpCooldown.class) == null){
			Buff.affect(ch, Weakness.class, B2_AMP_AWAKE_WEAKEN);
			Buff.affect(hero, BloomBuffs.AmpCooldown.class, B2_AMP_AWAKE_COOLDOWN);
		}
	}

	static float reactionDamageMulti(Hero hero){
		int a = lvl(hero, ELEMENT_AMP);
		return a >= 0 ? 1f + B2_ELEMENT_AMP_DMG[a] : 1f;
	}

	// =====================================================================
	// 명상 각성: 마나가 가득 찬 상태로 대기 (Mana.onWait, 회복 전)
	// =====================================================================

	static void onWaitAtFull(Hero hero){
		if (!awake(hero, MEDITATION) || lvl(hero, MEDITATION) < 0) return;
		if (hero.buff(BloomBuffs.MeditationCooldown.class) != null) return;
		BloomBuffs.MeditationShield sh = Buff.affect(hero, BloomBuffs.MeditationShield.class);
		sh.setShield(Math.round(hero.HT * B2_MEDITATION_AWAKE_SHIELD));
		sh.left = B2_MEDITATION_AWAKE_TURNS;
		Buff.affect(hero, BloomBuffs.MeditationCooldown.class, B2_MEDITATION_AWAKE_COOLDOWN);
	}

	// =====================================================================
	// 도움 함수
	// =====================================================================

	static int adjacentEnemies(Hero hero){
		int n = 0;
		for (int o : PathFinder.NEIGHBOURS8){
			Char c = Actor.findChar(hero.pos + o);
			if (c != null && c.isAlive() && c.alignment == Char.Alignment.ENEMY) n++;
		}
		return n;
	}

	//2칸 안의 적이 하나뿐이면 그 적, 아니면 null
	static Char duelOpponent(Hero hero){
		Char only = null;
		for (Char c : Actor.chars()){
			if (c == hero || !c.isAlive() || c.alignment != Char.Alignment.ENEMY) continue;
			if (Dungeon.level.distance(hero.pos, c.pos) <= B2_DUEL_RANGE){
				if (only != null) return null;
				only = c;
			}
		}
		return only;
	}
}
