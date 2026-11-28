/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 전사 2차 특성 효과 (구현 지시서 C단계). 원작 코드와는 BloomEffects를 거쳐서만 만난다.
public class BloomWarrior {

	//효과 배열 번호: 1레벨 → 0 ... 없거나 0레벨이면 -1
	private static int lvl(Hero hero, BloomTalent t){
		return BloomEffects.lvl(hero, t);
	}

	private static boolean awake(Hero hero, BloomTalent t){
		return BloomTree.awakened(hero, t);
	}

	// =====================================================================
	// 적중 피해 (평타·스킬·덧타격, 파생 피해는 오지 않는다)
	// =====================================================================

	static int hitDamage(Hero hero, Char enemy, int damage, boolean skill){
		float mult = 1f;

		//(C2) 허점 찌르기: 움직이지 못하는 적(기절·속박·빙결)과 나를 아직 보지 못한 적
		int ol = lvl(hero, W_OPENING);
		if (ol >= 0) rememberHeld(enemy);
		if (ol >= 0 && (BloomJudge.immobile(enemy) || BloomJudge.unaware(enemy, hero))){
			mult += C2_OPENING_DMG[ol];
			BloomRules.debug("opening +" + Math.round(C2_OPENING_DMG[ol] * 100) + "%");
		}

		//(C3) 벼림: 공격하지 않고 보낸 턴만큼 이번 공격(평타 한 번 또는 스킬 한 번의 모든 타격)이 강해진다
		//던지기 등 평타·스킬이 아닌 주인공 공격도 벼림을 쓴다. 행동 밖의 덧타격(메아리 잔상)은 쓰지 않는다
		if (!attackedThisAction && !BloomEffects.inExtraHit()) beginAttack(hero);
		if (honeBonus > 0){
			mult += honeBonus;
			BloomRules.debug("hone +" + Math.round(honeBonus * 100) + "%" + (honeFull ? " (full)" : ""));
		}

		//(C13) 넘치는 잔 각성: 쌓아 둔 피해 (스킬 한 번의 모든 타격)
		if (skill && cupBonus > 0){
			mult += cupBonus;
			BloomRules.debug("overflow cup +" + Math.round(cupBonus * 100) + "%");
		}

		//(C1) 받아치기: 받아친 평타의 피해 증가 (평타 한 번에 한 번)
		int rl = lvl(hero, W_RIPOSTE);
		if (!skill && riposteBoost && rl >= 0){
			riposteBoost = false;
			mult += C1_RIPOSTE_DMG[rl];
			BloomRules.debug("riposte +" + Math.round(C1_RIPOSTE_DMG[rl] * 100) + "%");
		}

		return mult == 1f ? damage : Math.round(damage * mult);
	}

	// =====================================================================
	// (C1) 받아치기
	// =====================================================================

	private static boolean riposteBoost = false;   //이번 평타에 피해 증가가 붙는다 (적중 처리 전까지)
	private static boolean riposteAttack = false;  //이번 평타가 받아친 평타다 (턴 소모 계산까지)

	//적의 공격으로 피가 줄지 않았다: 직후의 평타가 강해진다
	static void onUnhurtByAttack(Hero hero, Char enemy){
		if (lvl(hero, W_RIPOSTE) >= 0){
			Buff.affect(hero, BloomBuffs.RiposteReady.class);
		}
	}

	//평타 시작: 준비가 되어 있으면 이 평타가 받아친 평타다
	static void beginNormal(Hero hero){
		riposteBoost = false;
		riposteAttack = false;
		BloomBuffs.RiposteReady r = hero.buff(BloomBuffs.RiposteReady.class);
		if (r != null){
			r.detach();
			if (lvl(hero, W_RIPOSTE) >= 0){
				riposteBoost = true;
				riposteAttack = true;
			}
		}
	}

	//이번 행동이 공격인지 (onHeroSpend 전에 본다)
	static boolean attackingNow(){
		return attackedThisAction;
	}

	//주인공이 턴을 쓸 때 (Hero.spend). 공격이 끝나며 쓰는 턴이면 attackedThisAction이 켜져 있다.
	static void onHeroSpend(Hero hero, float time){
		boolean attacked = attackedThisAction;
		attackedThisAction = false;
		honeBonus = 0;
		honeFull = false;
		cupBonus = 0;

		//(C21) 발도: 겨눈 검은 다음에 턴을 쓰면 풀린다 (대기하면 armQuickdraw가 다시 겨눈다)
		BloomBuffs.Quickdraw q = hero.buff(BloomBuffs.Quickdraw.class);
		if (q != null) q.detach();

		//(C1) 받아칠 준비는 평타가 아닌 행동을 하면 사라진다 (평타는 시작할 때 이미 준비를 썼다)
		BloomBuffs.RiposteReady r = hero.buff(BloomBuffs.RiposteReady.class);
		if (r != null){
			r.detach();
			BloomRules.debug("riposte lost (other action)");
		}

		//(C3) 벼림: 공격하지 않고 보낸 턴마다 쌓인다. 걷든 기다리든 같다 (신속이면 반 턴씩)
		if (!attacked && time > 0 && lvl(hero, W_HONE) >= 0){
			Buff.affect(hero, BloomBuffs.Hone.class).add(time);
		}

		//(C9) 숨 고르기: 공격하지도 맞지도 않고 보낸 턴에 방어막이 조금 찬다 (상한 있음)
		boolean hurt = hurtSinceSpend;
		hurtSinceSpend = false;
		int bl = lvl(hero, W_BREATHER);
		if (!attacked && !hurt && time > 0 && bl >= 0){
			BloomBuffs.BreatherShield s = Buff.affect(hero, BloomBuffs.BreatherShield.class);
			s.gain(hero.HT * C9_BREATHER_PER_TURN[bl] * time, breatherCap(hero, bl));
		}
	}

	// =====================================================================
	// (C9) 숨 고르기
	// =====================================================================

	private static boolean hurtSinceSpend = false;

	//주인공이 피해를 받았다 (Hero.damage, 방어막에 막혀도)
	static void onHeroHurt(Hero hero){
		hurtSinceSpend = true;
	}

	static int breatherCap(Hero hero, int l){
		return Math.max(1, Math.round(hero.HT * C9_BREATHER_CAP[l]));
	}

	// =====================================================================
	// (C10) 마지막 겹: 방어막이 깨질 때 그 공격의 넘친 피해를 받지 않는다 (방어막이 어디서 왔든).
	// 깨진 뒤 1턴 동안 받는 피해가 준다(레벨). 각성: 깨지는 순간 주변의 적을 한 칸씩 밀쳐낸다(원작 밀침, 부딪히면 원작 피해·기절).
	// =====================================================================

	static int afterShield(Hero hero, int before, int after, int dmg){
		int l = lvl(hero, W_LAST_LAYER);
		if (l >= 0 && before > 0 && after <= 0){
			BloomRules.debug("last layer: shield broke, overflow " + dmg + " -> 0");
			Buff.affect(hero, BloomBuffs.LastLayer.class, C10_LAYER_TURNS);
			if (hero.sprite != null) hero.sprite.emitter().burst(LeafParticle.GENERAL, 6);
			if (awake(hero, W_LAST_LAYER)) lastLayerPush(hero);
			dmg = 0;
		}
		dmg = holdBreath(hero, dmg); //(C12) 버티는 숨
		BloomHuntress.smokeFeather(hero, dmg); //(D13) 연막 깃: 체력이 4분의 1 아래로 떨어지는 순간
		return dmg;
	}

	// =====================================================================
	// (C12) 버티는 숨: 쓰러질 피해를 받아도 체력 1로 버틴다 (긴 쿨타임, 층과 상관없음).
	// 버틴 뒤 방어막(원작 Barrier, 레벨). 각성: 버틴 뒤 1턴 동안 어떤 피해도 받지 않는다.
	// =====================================================================

	private static int holdBreath(Hero hero, int dmg){
		if (dmg < hero.HP || hero.HP <= 0) return dmg;
		int l = lvl(hero, W_HOLD_BREATH);
		if (l < 0 || hero.buff(BloomBuffs.HoldBreathCooldown.class) != null) return dmg;
		int held = hero.HP - 1;
		BloomRules.debug("hold breath: " + dmg + " -> " + held);
		Buff.affect(hero, BloomBuffs.HoldBreathCooldown.class, C12_HOLD_COOLDOWN);
		int shield = Math.max(1, Math.round(hero.HT * C12_HOLD_SHIELD[l]));
		Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).setShield(shield);
		if (awake(hero, W_HOLD_BREATH)) Buff.affect(hero, BloomBuffs.HoldBreathGuard.class, C12_AWAKE_TURNS);
		if (hero.sprite != null){
			hero.sprite.emitter().burst(LeafParticle.GENERAL, 12);
			hero.sprite.showStatusWithIcon(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
					Integer.toString(shield), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.SHIELDING);
		}
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(BloomBuffs.HoldBreathCooldown.class, "held"));
		return held;
	}

	// =====================================================================
	// (C13) 넘치는 잔: 마나가 가득 찬 뒤에 넘치는 마나가 방어막으로 바뀐다 (상한 있음).
	// 각성: 방어막도 가득이면 넘치는 마나가 다음 스킬의 피해로 쌓인다 (상한 있음).
	// =====================================================================

	private static float cupBonus = 0; //이번 스킬에 붙는 피해 증가 (넘치는 잔 각성)

	static int cupCap(Hero hero, int l){
		return Math.max(1, Math.round(hero.HT * C13_CUP_CAP[l]));
	}

	static void onManaOverflow(Hero hero, int overflow){
		int l = lvl(hero, W_OVERFLOW_CUP);
		if (l < 0 || overflow <= 0 || !hero.isAlive()) return;
		BloomBuffs.CupShield s = hero.buff(BloomBuffs.CupShield.class);
		int cap = cupCap(hero, l);
		if (s == null || s.shielding() < cap){
			if (s == null) s = Buff.affect(hero, BloomBuffs.CupShield.class);
			s.gain(overflow * C13_CUP_RATIO[l], cap);
		} else if (awake(hero, W_OVERFLOW_CUP)){
			Buff.affect(hero, BloomBuffs.CupCharge.class).add(overflow);
		}
	}

	//스킬 시작 (BloomEffects.beginSkill): 쌓인 피해를 이 스킬에 쓴다
	static void beginSkill(Hero hero){
		cupBonus = 0;
		BloomBuffs.CupCharge c = hero.buff(BloomBuffs.CupCharge.class);
		if (c == null) return;
		if (lvl(hero, W_OVERFLOW_CUP) >= 0 && awake(hero, W_OVERFLOW_CUP)) cupBonus = c.bonus();
		c.detach();
	}

	private static void lastLayerPush(final Hero hero){
		for (final Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive() || m.alignment != Char.Alignment.ENEMY || !Dungeon.level.adjacent(hero.pos, m.pos)) continue;
			if (com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.pushingExistsForChar(m)) continue;
			BloomRules.derived(new Runnable() {
				@Override
				public void run() {
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica traj =
							new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, m.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET);
					traj = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(traj.collisionPos, traj.path.get(traj.path.size() - 1),
							com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
					com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(m, traj, C10_AWAKE_PUSH, true, true, hero);
				}
			});
		}
		if (hero.sprite != null && hero.sprite.visible){
			com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.BlastWave.blast(hero.pos, 0.8f);
		}
	}

	// =====================================================================
	// (C11) 일대일: 내 옆 칸의 적이 하나뿐이면 받는 피해가 준다.
	// 각성: 그때 그 적의 공격을 피하면 바로 반격한다 (평타 판정, 턴을 쓰지 않는다).
	// =====================================================================

	//옆 칸의 적 수
	static int adjacentEnemies(Hero hero){
		int n = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY && Dungeon.level.adjacent(hero.pos, m.pos)) n++;
		}
		return n;
	}

	// =====================================================================
	// (C14) 마찰: 평타로 적을 맞힐 때마다 가진 마법봉 모두의 충전이 조금 찬다.
	// 각성: 마법봉이 모두 가득 찬 뒤 넘치는 충전은 마나로 돌아온다 (규칙 4: 한 행동에 최대 마나의 25%까지).
	// =====================================================================

	static void friction(Hero hero, boolean hit){
		int l = lvl(hero, W_FRICTION);
		if (l < 0 || !hit) return;
		java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand> wands =
				hero.belongings.getAllItems(com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand.class);
		if (wands.isEmpty()) return;
		float amt = C14_FRICTION_CHARGE[l];
		boolean allFull = true;
		for (com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand w : wands){
			if (w.curCharges < w.maxCharges){
				w.gainCharge(amt);
				allFull = false;
			}
		}
		if (!allFull){
			BloomRules.debug("friction: wands +" + amt);
			return;
		}
		if (!awake(hero, W_FRICTION)) return;
		//(결정) 넘친 충전은 평타 한 대마다 마나 C14_AWAKE_MANA로 돌아온다 (마법봉 수·특성 레벨과 상관없이)
		int got = BloomRules.gainMana(hero, C14_AWAKE_MANA);
		BloomRules.debug("friction overflow -> mana +" + got);
	}

	// =====================================================================
	// (C18) 긴 칼날 (검 전용 공격): 검 평타가 대상 바로 뒤 칸의 적도 약하게 벤다(평타 판정 덧타격).
	// 각성: 뒤 칸에 적이 없으면 그 칼이 대상에게 한 번 더 들어간다.
	// =====================================================================

	static void longEdge(Hero hero, Char enemy, boolean hit){
		int l = lvl(hero, W_LONG_EDGE);
		if (l < 0 || !hit || enemy == null || !Dungeon.level.adjacent(hero.pos, enemy.pos)) return;
		int w = Dungeon.level.width();
		int dx = Integer.signum(enemy.pos % w - hero.pos % w);
		int dy = Integer.signum(enemy.pos / w - hero.pos / w);
		int behind = enemy.pos + dx + dy * w;
		Char back = (behind >= 0 && behind < Dungeon.level.length()) ? com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(behind) : null;
		boolean backEnemy = back != null && back != hero && back.isAlive() && back.alignment == Char.Alignment.ENEMY;

		BloomEffects.SkillHit h = new BloomEffects.SkillHit();
		h.hero = hero;
		h.dmgMulti = 1f;
		h.accMulti = 1f;
		h.skillKey = "long_edge";
		if (backEnemy){
			if (back.sprite != null && back.sprite.visible){
				com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.toward(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.THRUST, hero, back);
			}
			BloomEffects.extraHit(h, back, C18_LONG_EDGE_RATIO[l]);
			BloomRules.debug("long edge -> behind " + back.name());
		} else if (awake(hero, W_LONG_EDGE) && enemy.isAlive()){
			BloomEffects.extraHit(h, enemy, C18_LONG_EDGE_RATIO[l]);
			BloomRules.debug("long edge -> again on target");
		}
	}

	// =====================================================================
	// (C19) 피 묻은 날 (검 전용 공격): 검으로 벤 적이 가끔 피를 흘린다(원작 출혈). 레벨: 확률과 세기.
	// 각성: 피 흘리는 적을 스킬로 치면 남은 출혈이 한꺼번에 터져 피해로 들어간다(파생 피해, 맞힌 뒤에).
	// =====================================================================

	static void bloodyEdge(final Hero hero, final Char enemy, int damage, boolean skill){
		int l = lvl(hero, W_BLOODY_EDGE);
		if (l < 0 || damage <= 0) return;
		final com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding b =
				enemy.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class);
		//각성: 이 타격 전부터 피를 흘리던 적을 스킬로 쳤다 → 남은 출혈을 한꺼번에
		if (skill && b != null && awake(hero, W_BLOODY_EDGE) && BloomRules.triggerOnHit(W_BLOODY_EDGE)){
			BloomEffects.afterHit(new Runnable() {
				@Override
				public void run() {
					if (!enemy.isAlive() || enemy.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class) != b) return;
					int burst = b.totalIncomingDMG();
					b.detach();
					if (enemy.sprite != null && enemy.sprite.visible){
						enemy.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle.BURST, 12);
					}
					BloomRules.debug("bloody edge burst " + burst);
					BloomRules.derivedDamage(W_BLOODY_EDGE, enemy, burst);
				}
			});
			return;
		}
		if (com.watabou.utils.Random.Float() >= C19_BLEED_CHANCE[l]) return;
		if (!BloomRules.triggerOnHit(W_BLOODY_EDGE)) return;
		float bleed = Math.max(1f, damage * C19_BLEED_POWER[l]);
		Buff.affect(enemy, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding.class).set(bleed);
		BloomRules.debug("bloody edge bleed " + bleed);
	}

	// =====================================================================
	// (C20) 받아넘기기 (검 전용 생존): 검을 들고 있으면 근접 공격을 가끔 쳐낸다 (피가 줄지 않은 공격).
	// 각성: 쳐낸 적은 자세가 무너져 한 턴 동안 공격 속도가 느려진다.
	// =====================================================================

	static boolean tryDeflect(Hero hero, Char enemy){
		int l = lvl(hero, W_DEFLECT);
		if (l < 0 || enemy == null || !hero.isAlive() || !Dungeon.level.adjacent(hero.pos, enemy.pos)) return false;
		if (hero.paralysed > 0 || hero.buff(Frost.class) != null) return false;
		if (com.watabou.utils.Random.Float() >= C20_DEFLECT_CHANCE[l]) return false;
		if (hero.sprite != null && hero.sprite.visible){
			hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEUTRAL,
					com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(BloomBuffs.Staggered.class, "deflect"));
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.HIT_PARRY, 1f, com.watabou.utils.Random.Float(0.96f, 1.05f));
		}
		if (awake(hero, W_DEFLECT) && enemy.isAlive()){
			Buff.prolong(enemy, BloomBuffs.Staggered.class, C20_STAGGER_TURNS);
		}
		BloomRules.debug("deflect " + enemy.name() + (awake(hero, W_DEFLECT) ? " (staggered)" : ""));
		return true;
	}

	// =====================================================================
	// (C21) 발도 (검 전용 유틸): 대기하면 검을 겨눈다. 다음 내 턴 전에 옆 칸으로 들어오는 첫 적을 먼저 벤다.
	// 이 한 칼은 평타 판정(마나 없음, 평타 특성이 붙고 스킬 형태는 붙지 않음, 턴을 쓰지 않음).
	// 각성: 그 한 칼이 적의 방어력을 꿰뚫는다 (그 적에게만, 뒤로 나아가지 않음).
	// =====================================================================

	private static boolean quickdrawPierce = false;

	//대기할 때 (Mana.onWait, onHeroSpend 다음): 검을 겨눈다. 다음에 턴을 쓰면 풀린다(onHeroSpend)
	static void armQuickdraw(Hero hero){
		if (lvl(hero, W_QUICKDRAW) < 0) return;
		Buff.affect(hero, BloomBuffs.Quickdraw.class);
	}

	static void onMobEnterCell(Hero hero, Char ch){
		if (hero == null || ch == null || ch == hero || !(ch instanceof Mob)) return;
		BloomBuffs.Quickdraw q = hero.buff(BloomBuffs.Quickdraw.class);
		if (q == null) return;
		int l = lvl(hero, W_QUICKDRAW);
		if (l < 0){ q.detach(); return; }
		if (!ch.isAlive() || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.adjacent(hero.pos, ch.pos)) return;
		if (!Dungeon.level.heroFOV[ch.pos] || ch.invisible > 0) return;
		if (!hero.isAlive() || hero.paralysed > 0 || hero.buff(Frost.class) != null || hero.isCharmedBy(ch)) return;
		q.detach();
		final Hero fh = hero;
		BloomRules.debug("quickdraw -> " + ch.name());
		if (hero.sprite != null && hero.sprite.visible){
			hero.sprite.attack(ch.pos, new com.watabou.utils.Callback() {
				@Override public void call() { fh.sprite.idle(); }
			});
			if (awake(hero, W_QUICKDRAW) && ch.sprite != null){
				com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.toward(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.PHANTOM, hero, ch); //검기
			}
		}
		quickdrawPierce = awake(hero, W_QUICKDRAW);
		try {
			BloomEffects.beginNormal(hero);
			boolean h = hero.attack(ch, C21_QUICKDRAW_DMG[l], 0, 1f);
			BloomEffects.afterNormalAttack(hero, ch, h, new Runnable() { @Override public void run() {} });
		} finally {
			quickdrawPierce = false;
			endCounter();
		}
	}

	//(C21) 발도 각성: 방어력을 꿰뚫는다
	static int armorAfterIgnore(Hero hero, Char enemy, int dr){
		return quickdrawPierce ? 0 : dr;
	}

	// =====================================================================
	// (C15) 가벼운 발: 해로운 바닥(불, 원작 해로운 가스, 전기, 냉기, 수액 웅덩이, 마력 증기)에 들어선 첫 턴에는
	// 그 효과를 받지 않는다. 그 뒤 그 바닥에 서 있는 동안 바닥이 주는 피해가 준다.
	// 각성: 함정을 밟아도 한 번은 발동하지 않는다 (쿨타임).
	// =====================================================================

	@SuppressWarnings("unchecked")
	static final Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob>[] HARMFUL_FLOORS = new Class[]{
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ConfusionGas.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StenchGas.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SapPuddle.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SapFloor.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.class,
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SpitVapor.class
	};

	static boolean onHarmfulFloor(int cell){
		for (Class<? extends com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob> c : HARMFUL_FLOORS){
			if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.volumeAt(cell, c) > 0) return true;
		}
		return false;
	}

	static void onHeroEnterCell(Hero hero, int cell){
		if (lvl(hero, W_LIGHT_FEET) < 0 || !onHarmfulFloor(cell)) return;
		Buff.prolong(hero, BloomBuffs.LightFeet.class, C15_GUARD_TURNS);
		BloomRules.debug("light feet: first turn on harmful floor");
	}

	static boolean blockTrap(Hero hero, int cell){
		if (lvl(hero, W_LIGHT_FEET) < 0 || !awake(hero, W_LIGHT_FEET)) return false;
		if (hero.buff(BloomBuffs.LightFeetTrapCooldown.class) != null) return false;
		Buff.affect(hero, BloomBuffs.LightFeetTrapCooldown.class, C15_TRAP_COOLDOWN);
		if (hero.sprite != null) hero.sprite.emitter().burst(LeafParticle.GENERAL, 8);
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(BloomBuffs.LightFeetTrapCooldown.class, "dodged"));
		BloomRules.debug("light feet: trap skipped");
		return true;
	}

	//바닥이 주는 피해인지 (장판 그 자체, 또는 장판이 거는 화상·부식)
	private static boolean floorDamage(Object src){
		return src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob
				|| src instanceof Burning
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
	}

	//받는 모든 피해 (Hero.damage → BloomEffects.onHeroDamage)
	static int reduceIncoming(Hero hero, int dmg, Object src){
		if (dmg <= 0) return dmg;
		//(C15) 가벼운 발: 첫 턴은 바닥 피해 없음, 그 뒤 그 바닥에 서 있는 동안 덜 받는다
		int fl = lvl(hero, W_LIGHT_FEET);
		if (fl >= 0 && floorDamage(src)){
			if (hero.buff(BloomBuffs.LightFeet.class) != null){
				BloomRules.debug("light feet guard: " + dmg + " -> 0");
				return 0;
			}
			if (onHarmfulFloor(hero.pos)) dmg = Math.round(dmg * (1f - C15_FLOOR_REDUCE[fl]));
		}
		float mult = 1f;
		//(C12) 버티는 숨 각성: 버틴 뒤 1턴 동안 어떤 피해도 받지 않는다
		if (hero.buff(BloomBuffs.HoldBreathGuard.class) != null){
			BloomRules.debug("hold breath guard: " + dmg + " -> 0");
			return 0;
		}
		int ll = lvl(hero, W_LAST_LAYER);
		if (ll >= 0 && hero.buff(BloomBuffs.LastLayer.class) != null) mult -= C10_LAYER_REDUCE[ll];
		int dl = lvl(hero, W_ONE_ON_ONE);
		if (dl >= 0 && adjacentEnemies(hero) == 1) mult -= C11_DUEL_REDUCE[dl];
		if (mult >= 1f) return dmg;
		int r = Math.round(dmg * Math.max(0f, mult));
		BloomRules.debug("incoming " + dmg + " -> " + r);
		return r;
	}

	//상태 아이콘: 조건이 맞는 동안만 보인다 (매 프레임, StatusPane)
	static void updateOneOnOne(Hero hero){
		if (hero == null || Dungeon.level == null) return;
		boolean on = lvl(hero, W_ONE_ON_ONE) >= 0 && hero.isAlive() && adjacentEnemies(hero) == 1;
		BloomBuffs.OneOnOne b = hero.buff(BloomBuffs.OneOnOne.class);
		if (on && b == null) Buff.affect(hero, BloomBuffs.OneOnOne.class);
		else if (!on && b != null) b.detach();
	}

	private static boolean countering = false;

	static void onEnemyAttackDone(final Hero hero, Char enemy, boolean hit){
		if (hit || countering || enemy == null || !enemy.isAlive() || !hero.isAlive()) return;
		if (lvl(hero, W_ONE_ON_ONE) < 0 || !awake(hero, W_ONE_ON_ONE)) return;
		if (!Dungeon.level.adjacent(hero.pos, enemy.pos) || adjacentEnemies(hero) != 1) return;
		if (hero.paralysed > 0 || hero.buff(Frost.class) != null || hero.isCharmedBy(enemy)) return;
		countering = true;
		try {
			BloomRules.debug("one-on-one counter -> " + enemy.name());
			BloomEffects.beginNormal(hero);
			if (hero.sprite != null && hero.sprite.visible){
				hero.sprite.attack(enemy.pos, new com.watabou.utils.Callback() {
					@Override public void call() { hero.sprite.idle(); }
				});
			}
			boolean h = hero.attack(enemy);
			BloomEffects.afterNormalAttack(hero, enemy, h, new Runnable() { @Override public void run() {} });
		} finally {
			endCounter();
			countering = false;
		}
	}

	//반격이 끝나면 평타 한 번 동안의 상태를 비운다 (반격은 턴을 쓰지 않으므로 Hero.spend가 지우지 않는다)
	static void endCounter(){ //Maze Tower (S2): 검무가의 스침도 쓴다
		attackedThisAction = false;
		honeBonus = 0;
		honeFull = false;
		riposteBoost = false;
		riposteAttack = false;
	}

	//(결정) 각성: 방어막이 상한까지 차 있는 동안 다음에 걸리는 나쁜 상태이상 하나를 막는다.
	//한 번 막으면 방어막을 모두 잃은 뒤(방어막 상태가 사라진 뒤) 다시 상한까지 채워야 또 막는다.
	//(판 전체에 걸린 상태 - 아뮬렛의 저주·붕괴·잃어버린 가방·미뤄 둔 피해·배고픔은 막지 않는다)
	static boolean blockHeroDebuff(Hero hero, Buff b){
		int l = lvl(hero, W_BREATHER);
		if (l < 0 || !awake(hero, W_BREATHER)) return false;
		if (hero.sprite == null) return false; //저장 파일을 불러오는 중 (상태이상을 다시 붙이는 중)에는 막지 않는다
		BloomBuffs.BreatherShield s = hero.buff(BloomBuffs.BreatherShield.class);
		if (s == null || s.wardUsed() || s.shielding() < breatherCap(hero, l)) return false;
		if (b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TowerCollapse
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity.DeferedDamage
				|| b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger) return false;
		s.useWard();
		BloomRules.debug("breather ward blocked " + b.getClass().getSimpleName());
		if (hero.sprite != null) hero.sprite.emitter().burst(LeafParticle.GENERAL, 8);
		return true;
	}

	// =====================================================================
	// (C3) 벼림
	// =====================================================================

	private static boolean attackedThisAction = false; //이번 행동이 공격이다 (턴을 쓸 때 벼림을 쌓지 않는다)
	private static float honeBonus = 0;                //이번 공격에 붙는 피해 증가
	private static boolean honeFull = false;           //이번 공격이 가득 벼린 공격이다

	//공격 시작 (평타·스킬): 쌓인 벼림을 이 공격에 쓴다. 빗나가도 쓰인다.
	static void beginAttack(Hero hero){
		attackedThisAction = true;
		honeBonus = 0;
		honeFull = false;
		BloomBuffs.Hone h = hero.buff(BloomBuffs.Hone.class);
		if (h == null) return;
		int l = lvl(hero, W_HONE);
		if (l >= 0){
			honeBonus = h.turns() * C3_HONE_PER_TURN[l];
			honeFull = h.full();
		}
		h.detach();
	}

	//가득 벼린 공격은 빗나가지 않는다 (각성)
	static boolean sureHit(Hero hero){
		return honeFull && attackedThisAction && awake(hero, W_HONE);
	}

	//평타의 턴 소모. 받아치기 각성이면 턴을 쓰지 않는다 (간격 C1_RIPOSTE_FREE_CD).
	static float normalAttackDelay(Hero hero, float delay){
		boolean was = riposteAttack;
		riposteAttack = false;
		riposteBoost = false;
		if (was && awake(hero, W_RIPOSTE) && hero.buff(BloomBuffs.RiposteFreeCooldown.class) == null){
			Buff.affect(hero, BloomBuffs.RiposteFreeCooldown.class, C1_RIPOSTE_FREE_CD);
			BloomRules.debug("riposte free (no turn)");
			return 0f;
		}
		return delay;
	}

	// =====================================================================
	// (C2) 허점 찌르기 각성: 움직이지 못하는 적을 쓰러뜨리면 걸려 있던 기절·속박·빙결이
	// 가장 가까운 적에게 짧게 옮겨 간다. 옮겨 간 상태는 다시 옮겨 가지 않는다(규칙 5).
	// =====================================================================

	//원작의 기절·빙결은 피해를 받으면 풀린다. 그래서 쓰러뜨린 일격의 피해가 들어가기 전(적중 처리 때)의 상태를 기억해 둔다.
	private static int heldId = -1;
	private static boolean heldPara, heldRoots, heldFrost, heldSpread;

	private static void rememberHeld(Char enemy){
		heldId = enemy.id();
		heldPara = enemy.paralysed > 0 || enemy.buff(Paralysis.class) != null;
		heldRoots = enemy.buff(Roots.class) != null;
		heldFrost = enemy.buff(Frost.class) != null;
		heldSpread = enemy.buff(BloomBuffs.OpeningSpread.class) != null;
	}

	static void onEnemyKilled(Hero hero, Char dead){
		if (lvl(hero, W_OPENING) < 0 || !awake(hero, W_OPENING)) return;
		boolean para, roots, frost, spread;
		if (dead.id() == heldId){
			para = heldPara; roots = heldRoots; frost = heldFrost; spread = heldSpread;
		} else {
			para = dead.paralysed > 0 || dead.buff(Paralysis.class) != null;
			roots = dead.buff(Roots.class) != null;
			frost = dead.buff(Frost.class) != null;
			spread = dead.buff(BloomBuffs.OpeningSpread.class) != null;
		}
		heldId = -1;
		if (!para && !roots && !frost) return;
		if (spread) return; //옮겨 받은 상태로 쓰러진 적 (규칙 5)

		Char to = nearestEnemy(dead);
		if (to == null){
			BloomRules.debug("opening spread: no enemy within " + C2_OPENING_SPREAD_RANGE);
			return;
		}
		if (!BloomRules.trigger(W_OPENING)) return;

		float t = C2_OPENING_SPREAD_TURNS;
		if (para)  Buff.prolong(to, Paralysis.class, t);
		if (roots) Buff.prolong(to, Roots.class, t);
		if (frost) Buff.prolong(to, Frost.class, t);
		Buff.prolong(to, BloomBuffs.OpeningSpread.class, t);
		if (to.sprite != null && to.sprite.visible) to.sprite.emitter().burst(LeafParticle.GENERAL, 6);
		BloomRules.debug("opening spread -> " + to.name() + (para ? " stun" : "") + (roots ? " roots" : "") + (frost ? " frost" : ""));
	}

	// =====================================================================
	// (C4) 번지는 반응: 내 공격이 원소 반응을 일으키면 주변 한 칸의 적도 그 피해의 일부를 받는다.
	// 각성: 번진 적에게 그 반응을 일으킨 원소 하나가 묻는다. 번져서 일어난 반응은 다시 번지지 않는다.
	// =====================================================================

	private static boolean spreading = false;

	static void onReactionDamage(final Hero hero, Char ch, int dmg, final int trigger){
		if (hero == null || ch == null || ch == hero || dmg <= 0) return;
		if (ch.alignment != Char.Alignment.ENEMY) return;
		final int l = lvl(hero, W_SPREAD);
		if (l < 0){
			if (BloomTree.ownedLevel(hero, W_SPREAD) == 0) BloomRules.debug("spread: talent is level 0 (no effect)");
			return;
		}
		if (spreading){ BloomRules.debug("spread: skipped (caused by a spread)"); return; }
		if (!BloomEffects.heroAttackInProgress()){ BloomRules.debug("spread: skipped (not my attack)"); return; }
		if (!BloomRules.triggerOnHit(W_SPREAD)){ BloomRules.debug("spread: skipped (limit per action)"); return; }

		java.util.ArrayList<Char> near = new java.util.ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m != ch && m.isAlive() && m.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.distance(ch.pos, m.pos) <= 1) near.add(m);
		}
		if (near.isEmpty()){ BloomRules.debug("spread: no enemy next to " + ch.name()); return; }

		final int share = Math.max(1, Math.round(dmg * C4_SPREAD_RATIO[l]));
		final boolean awakened = awake(hero, W_SPREAD);
		BloomRules.debug("spread " + dmg + " x" + C4_SPREAD_RATIO[l] + " -> " + share + " to " + near.size());
		spreading = true;
		try {
			for (final Char c : near){
				if (c.sprite != null && c.sprite.visible){
					c.sprite.emitter().burst(trigger == ElementalReactions.TRIGGER_FIRE ? FlameParticle.FACTORY
							: trigger == ElementalReactions.TRIGGER_COLD ? SnowParticle.FACTORY : SparkParticle.FACTORY, 5);
				}
				BloomRules.derivedDamage(W_SPREAD, c, share);
				if (awakened && c.isAlive()){
					//묻은 원소로 일어난 반응도 파생이며, spreading이 켜져 있어 다시 번지지 않는다
					BloomRules.derived(new Runnable() {
						@Override
						public void run() {
							if (trigger == ElementalReactions.TRIGGER_FIRE)      Buff.affect(c, Burning.class).reignite(c, C4_AWAKE_BURN);
							else if (trigger == ElementalReactions.TRIGGER_COLD) Buff.affect(c, Chill.class, C4_AWAKE_CHILL);
							else                                                 Buff.prolong(c, StaticCharge.class, STATIC_TURNS);
						}
					});
				}
			}
		} finally {
			spreading = false;
		}
	}

	// =====================================================================
	// (C5) 메아리: 공격형 스킬을 쓴 다음 턴에 푸른 잔상이 같은 적에게 그 스킬을 한 번 더 쓴다(덧타격).
	// 적이 닿지 않는 곳으로 갔으면 사라진다. 각성: 잔상이 한 턴 더 남아 그 턴의 내 평타 한 번을 따라 친다.
	// =====================================================================

	//공격형 스킬이 끝났다 (강화형: 장전·집중으로 쏜 평타는 제외)
	static void onSwordSkillFinished(Hero hero, SwordSkill skill, Char target){
		if (lvl(hero, W_ECHO) < 0 || skill.charge >= 0 || skill.chargedShot) return;
		if (target == null || !target.isAlive() || target == hero) return;
		Buff.detach(hero, BloomBuffs.Echo.class); //잔상은 하나만 (새 스킬이 덮는다)
		BloomBuffs.Echo e = Buff.affect(hero, BloomBuffs.Echo.class);
		e.set(skill, target);
		BloomRules.debug("echo set " + skill.nameKey + " -> " + target.name());
	}

	//잔상이 닿을 수 있는지 (주인공 자리에서, 그 스킬의 사거리로)
	static boolean echoReaches(Hero hero, Char target, SwordSkill s){
		if (target == null || !target.isAlive() || target.invisible > 0) return false;
		if (s.ranged){
			if (!Dungeon.level.heroFOV[target.pos]) return false;
			if (!com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.inRange(hero.pos, target.pos)) return false;
			return s.pierce || com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.clearShot(hero.pos, target.pos);
		}
		if (s.reach <= 1) return Dungeon.level.adjacent(hero.pos, target.pos);
		return Dungeon.level.distance(hero.pos, target.pos) <= s.reach
				&& new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, target.pos,
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT).collisionPos == target.pos;
	}

	//잔상이 스킬을 다시 쓴다 (Echo.act). 맞힌 것이 하나라도 있으면 true
	static boolean echoReplay(Hero hero, Char target, SwordSkill s){
		int l = lvl(hero, W_ECHO);
		if (l < 0 || !echoReaches(hero, target, s)) return false;
		com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects.echoGhost(hero, target);
		com.shatteredpixel.shatteredpixeldungeon.effects.SkillEffects.hit(s.nameKey, hero, target, 0);
		BloomEffects.SkillHit from = echoFrom(hero, s);
		SwordSkill prev = SwordSkill.active;
		SwordSkill.active = s; //스킬 판정 (무형검 방어 무시 등)
		boolean any = false;
		try {
			for (int i = 0; i < s.hits && target.isAlive(); i++){
				any |= BloomEffects.extraHit(from, target, C5_ECHO_RATIO[l]);
			}
		} finally {
			SwordSkill.active = prev;
		}
		BloomEffects.flushPostHit();
		BloomRules.debug("echo replay " + s.nameKey + " x" + s.hits + " -> " + target.name());
		return any;
	}

	private static BloomEffects.SkillHit echoFrom(Hero hero, SwordSkill s){
		BloomEffects.SkillHit h = new BloomEffects.SkillHit();
		h.hero = hero;
		h.dmgMulti = s.dmgPerHit;
		h.accMulti = s.accMulti;
		h.ranged = s.ranged;
		h.skillKey = s.nameKey;
		h.baseHits = s.hits;
		return h;
	}

	//(C5 각성) 잔상이 남아 있는 턴에 넣은 평타를 따라 친다 (평타 판정의 덧타격, 세기는 잔상의 피해 비율)
	static void afterNormalAttack(Hero hero, Char enemy){
		BloomBuffs.Echo e = hero.buff(BloomBuffs.Echo.class);
		if (e == null || !e.following() || enemy == null || !enemy.isAlive()) return;
		int l = lvl(hero, W_ECHO);
		e.detach();
		if (l < 0 || !awake(hero, W_ECHO)) return;
		com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects.echoGhost(hero, enemy);
		BloomEffects.SkillHit h = new BloomEffects.SkillHit();
		h.hero = hero;
		h.dmgMulti = 1f;
		h.accMulti = 1f;
		h.ranged = com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.equipped(hero) != null
				&& !Dungeon.level.adjacent(hero.pos, enemy.pos);
		h.skillKey = "echo_follow";
		BloomEffects.extraHit(h, enemy, C5_ECHO_RATIO[l]);
		BloomRules.debug("echo follow -> " + enemy.name());
	}

	// =====================================================================
	// (C6) 한 번 더: 공격형 스킬(강화된 평타 포함)의 타수가 하나 는다. 늘어난 타도 본 타격이다.
	// 피해는 늘어난 타수에 고르게 나누고, 합은 C6_ONE_MORE_TOTAL만큼 는다. 각성: 늘어난 마지막 타는 반드시 적중.
	// =====================================================================

	static SwordSkill oneMore(Hero hero, SwordSkill s){
		int l = lvl(hero, W_ONE_MORE);
		if (l < 0 || s.charge >= 0) return s;
		int n = s.hits + 1;
		float scale = s.hits * (1f + C6_ONE_MORE_TOTAL[l]) / n;
		SwordSkill r = s.withHits(n, s.dmgPerHit * scale, scale);
		r.sureLast = awake(hero, W_ONE_MORE);
		BloomRules.debug("one more: " + s.nameKey + " " + s.hits + "x" + s.dmgPerHit + " -> " + n + "x" + r.dmgPerHit + (r.sureLast ? " (sure last)" : ""));
		return r;
	}

	// =====================================================================
	// (C7) 무거운 일격: 스킬의 마지막 타가 둥근 충격과 함께 적을 한 칸(각성 두 칸) 밀친다.
	// 밀린 적이 벽이나 다른 적에 부딪히면 그 타격 피해 × C7_HEAVY_COLLIDE (부딪힌 적도 같은 피해, 파생).
	// 밀침을 받지 않는 적(움직이지 않는 적·묶인 적)은 밀리지도 부딪히지도 않는다. 밀침은 원작 처리(WandOfBlastWave.throwChar).
	// =====================================================================

	static void onSkillHitDone(BloomEffects.SkillHit h){
		heatedEdge(h); //(C8) 달군 날
		crescent(h);   //(C16) 초승달
		heavy(h);      //(C7) 무거운 일격 (밀치기는 마지막에)
	}

	// =====================================================================
	// (C16) 초승달 (검 전용 형태): 검 스킬의 궤적이 반원으로 넓어져 대상 좌우 칸의 적도 약하게 벤다(덧타격).
	// 무형검은 마탄이 맞은 적의 좌우. 여러 번 치는 스킬이어도 스킬 한 번에 한 번(각성도 같다).
	// 각성: 맞은 적의 둘레 여덟 칸을 모두 벤다 (주인공이 선 칸은 뺀다).
	// =====================================================================

	private static void crescent(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		int l = lvl(hero, W_CRESCENT);
		Char t = h.target;
		if (l < 0 || !h.hit || t == null) return;
		if (!BloomRules.trigger(W_CRESCENT)) return;
		int w = Dungeon.level.width();
		int center = t.pos;
		java.util.ArrayList<Integer> cells = new java.util.ArrayList<>();
		if (awake(hero, W_CRESCENT)){
			for (int o : com.watabou.utils.PathFinder.NEIGHBOURS8) cells.add(center + o);
		} else {
			int dx = h.dirX(), dy = h.dirY();
			if (dx == 0 && dy == 0) return;
			int px, py;
			if (dx == 0 || dy == 0){ px = dy; py = dx; }  //가로·세로: 수직 방향
			else                   { px = dx; py = -dy; } //대각선: 반대 대각선
			cells.add(center + px + py * w);
			cells.add(center - px - py * w);
		}
		int n = 0;
		for (int c : cells){
			if (c < 0 || c >= Dungeon.level.length() || c == hero.pos || !Dungeon.level.adjacent(center, c)) continue;
			Char e = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c);
			if (e == null || e == t || e == hero || !e.isAlive() || e.alignment != Char.Alignment.ENEMY) continue;
			if (e.sprite != null && e.sprite.visible){
				com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.toward(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.SLASH, hero, e);
			}
			BloomEffects.extraHit(h, e, C16_CRESCENT_RATIO[l]);
			n++;
		}
		BloomRules.debug("crescent: " + n + " side target(s)" + (awake(hero, W_CRESCENT) ? " (full circle)" : ""));
	}

	// =====================================================================
	// (C8) 달군 날: 스킬의 궤적이 불꽃으로 바뀌고 맞은 적에게 화상을 묻힌다 (레벨: 화상의 피해와 지속).
	// 각성: 스킬에 맞은 적의 발밑에 불길(원작 Fire)이 잠깐 남는다. 원작 불처럼 풀에 옮겨 붙을 수 있다.
	// =====================================================================

	private static void heatedEdge(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		int l = lvl(hero, W_HEATED_EDGE);
		final Char t = h.target;
		if (l < 0 || t == null) return;
		if (t.sprite != null && t.sprite.visible) t.sprite.emitter().burst(FlameParticle.FACTORY, h.hit ? 8 : 4); //불꽃 궤적
		if (!h.hit || !t.isAlive() || !BloomRules.triggerOnHit(W_HEATED_EDGE)) return;
		Burning b = Buff.affect(t, Burning.class);
		b.mazeDmgMulti = Math.max(b.mazeDmgMulti, C8_BURN_DMG[l]);
		b.reignite(t, C8_BURN_TURNS[l]);
		if (awake(hero, W_HEATED_EDGE)){
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(
					com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(t.pos, C8_AWAKE_FIRE,
							com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire.class));
		}
		BloomRules.debug("heated edge: burn " + C8_BURN_TURNS[l] + "t x" + C8_BURN_DMG[l] + (awake(hero, W_HEATED_EDGE) ? " + fire" : ""));
	}

	private static void heavy(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		int l = lvl(hero, W_HEAVY);
		if (l < 0 || !h.hit || h.index != h.baseHits - 1) return;
		final Char t = h.target;
		if (t == null || !t.isAlive()) return;
		if (!BloomRules.trigger(W_HEAVY)) return;

		if (t.sprite != null && t.sprite.visible){
			com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.BlastWave.blast(t.pos, 0.6f);
		}
		if (t.rooted || t.properties().contains(Char.Property.IMMOVABLE)
				|| com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.pushingExistsForChar(t)){
			BloomRules.debug("heavy: target can't be pushed");
			return;
		}

		//원작 밀침과 같은 방향·거리 계산 (주인공 → 적 방향으로)
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica traj =
				new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, t.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET);
		traj = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(traj.collisionPos, traj.path.get(traj.path.size() - 1),
				com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
		int power = awake(hero, W_HEAVY) ? C7_HEAVY_PUSH_AWAKE : C7_HEAVY_PUSH;
		int bossPower = t.properties().contains(Char.Property.BOSS) ? (power + 1) / 2 : power;

		int dist = Math.min(traj.dist, bossPower);
		boolean collided = traj.dist < bossPower; //벽에 막힘
		Char blocker = null;
		int land = dist;
		for (int i = 1; i <= dist; i++){
			Char c = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(traj.path.get(i));
			if (c != null){
				blocker = c;
				collided = true;
				land = i - 1;
				break;
			}
		}
		//밀려 떨어질 칸이 구덩이면, 섬광 보법 각성의 "쓰러뜨렸다"로 친다 (실제 낙사는 밀리는 연출이 끝난 뒤)
		if (land > 0 && Dungeon.level.pit[traj.path.get(land)] && !t.flying){
			BloomSkill.markFalling(t);
			BloomRules.debug("heavy: target will fall into a chasm");
		}

		com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(t, traj, power, true, false, hero);

		if (collided){
			int dmg = Math.max(1, Math.round(h.damage * C7_HEAVY_COLLIDE[l]));
			BloomRules.debug("heavy: collided" + (blocker != null ? " with " + blocker.name() : " with wall") + " dmg " + dmg);
			com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(1, 0.1f);
			BloomRules.derivedDamage(W_HEAVY, t, dmg);
			if (blocker != null && blocker != hero && blocker.alignment == Char.Alignment.ENEMY){
				BloomRules.derivedDamage(W_HEAVY, blocker, dmg);
			}
		} else {
			BloomRules.debug("heavy: pushed " + dist);
		}
	}

	//쓰러진 적에서 C2_OPENING_SPREAD_RANGE칸 안, 주인공이 보는 가장 가까운 적
	private static Char nearestEnemy(Char from){
		Char best = null;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m == from || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[m.pos]) continue;
			int d = Dungeon.level.distance(from.pos, m.pos);
			if (d > C2_OPENING_SPREAD_RANGE || d >= bestD) continue;
			best = m;
			bestD = d;
		}
		return best;
	}
}
