/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 사냥꾼 2차 특성 효과 (구현 지시서 D단계). 원작 코드와는 BloomEffects를 거쳐서만 만난다.
//공용 특성은 검·단검을 든 사냥꾼에게도 작동한다.
public class BloomHuntress {

	private static int lvl(Hero hero, BloomTalent t){
		return BloomEffects.lvl(hero, t);
	}

	private static boolean awake(Hero hero, BloomTalent t){
		return BloomTree.awakened(hero, t);
	}

	// =====================================================================
	// 적중 피해 (평타·스킬·덧타격·던지기, 파생 피해는 오지 않는다)
	// =====================================================================

	static int hitDamage(Hero hero, Char enemy, int damage, boolean skill){
		float mult = 1f;

		//(D5) 먼 과녁: 원거리 공격(화살·던지기·지팡이 스킬의 마탄)은 멀수록 세진다
		boolean ranged = rangedNow(hero);
		mult += farBonus(hero, enemy, ranged);
		//(D5) 각성: 활의 최대 사거리에 있는 적에게 입히는 피해는 기습 판정 (첫 일격 등이 본다)
		boolean farSurprise = ranged && atBowMaxRange(hero, enemy);
		if (farSurprise){
			BloomJudge.forcedSurprise = true;
			if (enemy.sprite != null && enemy.sprite.visible) com.shatteredpixel.shatteredpixeldungeon.effects.Surprise.hit(enemy);
			BloomRules.debug("far target: surprise at max range");
		}

		//(D1, 결정) 첫 일격: 모든 기습 판정 (원작 기습·잠든 적·다른 특성이 주는 기습)
		int fl = lvl(hero, H_FIRST_STRIKE);
		if (fl >= 0 && BloomJudge.surprised(enemy, hero)){
			mult += D1_FIRST_STRIKE_DMG[fl];
			BloomRules.debug("first strike +" + Math.round(D1_FIRST_STRIKE_DMG[fl] * 100) + "%");
			if (awake(hero, H_FIRST_STRIKE) && BloomRules.trigger(H_FIRST_STRIKE)) hush(enemy);
		}

		//(D4) 치고 빠지기: 바로 앞 행동에 걸었으면 이번 공격이 강해진다 (평타 한 번, 또는 스킬 한 번의 모든 타격)
		int rl = lvl(hero, H_HIT_AND_RUN);
		if (rl >= 0 && hero.buff(BloomBuffs.HitAndRun.class) != null){
			mult += D4_HIT_AND_RUN_DMG[rl];
			BloomRules.debug("hit and run +" + Math.round(D4_HIT_AND_RUN_DMG[rl] * 100) + "%");
		}

		//(D21) 활대 치기: 활 근접 평타는 늘 강하고, 밀쳐낸 직후의 사격(화살)이 강해진다
		int bl = lvl(hero, H_BOW_BASH);
		if (bl >= 0){
			if (!ranged && !skill && !BloomEffects.inExtraHit() && hero.belongings.thrownWeapon == null && BloomJudge.isNormalAttack()){
				mult += D21_MELEE_DMG;
				BloomRules.debug("bow bash melee +" + Math.round(D21_MELEE_DMG * 100) + "%");
			}
			BloomBuffs.BowBashReady bb = hero.buff(BloomBuffs.BowBashReady.class);
			if (ranged && bb != null && bb.spends >= 1){
				mult += D21_SHOT_DMG[bl];
				bb.used = true;
				BloomRules.debug("bow bash shot +" + Math.round(D21_SHOT_DMG[bl] * 100) + "%");
			}
		}

		BloomJudge.forcedSurprise = false;

		int result = mult == 1f ? damage : Math.round(damage * mult);

		//(D2) 약점: 느려지거나 묶이거나 움직이지 못하는 적에게 치명타 (덧타격은 치명타가 나지 않는다)
		int wl = lvl(hero, H_WEAK_POINT);
		if (wl >= 0 && !BloomEffects.inExtraHit() && (BloomJudge.slowed(enemy) || BloomJudge.immobile(enemy))
				&& Random.Float() < D2_CRIT_CHANCE[wl]){
			result = Math.round(result * D2_CRIT_DMG);
			if (enemy.sprite != null && enemy.sprite.visible){
				enemy.sprite.showStatus(CharSprite.WARNING, Messages.get(BloomHuntress.class, "crit"));
				MazeFx.onChar(MazeFx.Fx.H_CRIT, enemy.sprite);
			}
			BloomRules.debug("weak point crit x" + D2_CRIT_DMG);
			if (awake(hero, H_WEAK_POINT)) extendHold(enemy);
		}
		return result;
	}

	// =====================================================================
	// (D5) 먼 과녁: 멀리 있는 적일수록 원거리 공격(화살, 던지는 무기, 마탄)의 피해가 는다 (칸마다, D5_MAX_TILES칸까지).
	// 각성: 활의 최대 사거리(활마다 D5_BOW_RANGE) 이상 떨어진 적에게 쏜 화살은 기습 판정(반드시 맞고, 첫 일격이 붙는다).
	// =====================================================================

	//(결정) 먼 과녁은 화살에만: 지금 화살을 쏘는 중인지
	private static boolean rangedNow(Hero hero){
		return com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.isShooting();
	}

	private static float farBonus(Hero hero, Char enemy, boolean ranged){
		int l = lvl(hero, H_FAR_TARGET);
		if (l < 0 || !ranged || enemy == null) return 0;
		int d = Math.min(D5_MAX_TILES, Dungeon.level.distance(hero.pos, enemy.pos));
		if (d <= 1) return 0;
		float b = (d - 1) * D5_PER_TILE[l];
		BloomRules.debug("far target " + d + " tiles +" + Math.round(b * 100) + "%");
		return b;
	}

	//(결정) 들고 있는 활의 최대 사거리: 모든 활이 BOW_RANGE칸 (활이 아니면 0)
	static int bowMaxRange(Hero hero){
		return com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.equipped(hero) == null ? 0 : com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.range(); //Maze Tower (S6): 전투 사냥꾼은 절반
	}

	private static boolean atBowMaxRange(Hero hero, Char enemy){
		if (enemy == null || lvl(hero, H_FAR_TARGET) < 0 || !awake(hero, H_FAR_TARGET)) return false;
		if (!com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.isShooting()) return false;
		int r = bowMaxRange(hero);
		return r > 0 && Dungeon.level.distance(hero.pos, enemy.pos) >= r;
	}

	//(D5 각성) 최대 사거리의 화살은 기습처럼 반드시 맞는다 (Char.hit)
	static boolean farSureHit(Hero hero, Char defender){
		return atBowMaxRange(hero, defender);
	}

	// =====================================================================
	// (D6) 값진 일격: 다치지 않은 적을 한 번에 쓰러뜨리면 마나를 돌려받는다.
	// 각성: 한 번에 쓰러뜨리면 바로 다음 공격이 턴을 쓰지 않는다 (간격·한 번에 하나는 치고 빠지기 각성과 같이).
	// =====================================================================

	private static int cleanCandidate = -1; //이번 타격을 받기 전에 체력이 가득했던 적
	private static boolean cleanKillPending = false;

	//주인공 공격의 적중 처리 (피해가 들어가기 직전)
	static void markCleanCandidate(Char enemy){
		cleanCandidate = (enemy != null && enemy.HP >= enemy.HT) ? enemy.id() : -1;
	}

	static void onEnemyKilled(Hero hero, Char enemy, boolean byDerived){
		int l = lvl(hero, H_CLEAN_KILL);
		if (l < 0 || byDerived || enemy.id() != cleanCandidate) return;
		cleanCandidate = -1;
		int got = BloomRules.gainMana(hero, D6_CLEAN_MANA[l]);
		if (hero.sprite != null && got > 0) com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects.manaRefund(hero, enemy, got);
		BloomRules.debug("clean kill: mana +" + got);
		if (awake(hero, H_CLEAN_KILL)) cleanKillPending = true;
	}

	// =====================================================================
	// 스킬 타격이 끝났다 (B4의 한 곳): 사냥꾼 형태 특성
	// =====================================================================

	static void onSkillHitDone(BloomEffects.SkillHit h){
		ricochet(h);    //(D7) 튀는 일격
		bindingVine(h); //(D9) 휘감는 덩굴
		sproutTrail(h); //(D8) 새잎 자국
		embedArrow(h);  //(D18) 박힌 화살
	}

	// =====================================================================
	// (D9) 휘감는 덩굴 (형태): 스킬의 궤적을 덩굴이 따라가 맞은 적을 짧게 묶는다(원작 속박, 스킬 한 번에 한 번).
	// 속박을 받지 않는 적은 묶이지 않는다. 각성: 묶인 적 옆의 적 하나에게도 덩굴이 뻗어 함께 묶인다.
	// =====================================================================

	private static void bindingVine(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		int l = lvl(hero, H_BINDING_VINE);
		Char t = h.target;
		if (l < 0 || !h.hit || t == null || !t.isAlive()) return;
		if (hero.buff(BloomBuffs.VineCooldown.class) != null) return; //(결정) 쿨타임
		if (!BloomRules.trigger(H_BINDING_VINE)) return;
		Buff.affect(hero, BloomBuffs.VineCooldown.class, D9_COOLDOWN);
		root(t, D9_ROOT_TURNS[l]);
		BloomRules.debug("binding vine " + D9_ROOT_TURNS[l] + "t -> " + t.name());
		if (!awake(hero, H_BINDING_VINE)) return;
		for (int o : com.watabou.utils.PathFinder.NEIGHBOURS8){
			Char e = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(t.pos + o);
			if (e == null || e == hero || !e.isAlive() || e.alignment != Char.Alignment.ENEMY) continue;
			root(e, D9_ROOT_TURNS[l]);
			if (t.sprite != null && e.sprite != null && t.sprite.parent != null && (t.sprite.visible || e.sprite.visible)){
				com.shatteredpixel.shatteredpixeldungeon.effects.Beam.LightRay ray =
						new com.shatteredpixel.shatteredpixeldungeon.effects.Beam.LightRay(t.sprite.center(), e.sprite.center());
				ray.hardlight(0.45f, 0.85f, 0.35f); //덩굴빛
				t.sprite.parent.add(ray);
			}
			BloomRules.debug("binding vine spreads -> " + e.name());
			break;
		}
	}

	private static void root(Char ch, float turns){
		Buff.prolong(ch, Roots.class, turns);
		if (ch.sprite != null && ch.sprite.visible){
			ch.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 6);
		}
	}

	// =====================================================================
	// (D10) 풀숲의 몸: 높은 풀 속에 있는 동안 원거리 공격(옆 칸이 아닌 적의 공격)을 더 잘 피한다.
	// 각성: 풀 속에서 한 턴 가만히 있으면(대기·탐색) 나를 쫓던 적이 나를 놓친다.
	// =====================================================================

	static boolean inTallGrass(Hero hero){
		int t = Dungeon.level.map[hero.pos];
		return t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HIGH_GRASS
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.FURROWED_GRASS; //사냥꾼이 들어서면 헤쳐나간 풀이 된다
	}

	//(결정) 바로 앞(옆 칸)의 적이 하는 공격에도 붙는다
	static float evasionMulti(Hero hero, Char enemy){
		float m = 1f;
		int l = lvl(hero, H_GRASS_BODY);
		if (l >= 0 && enemy != null && inTallGrass(hero)) m *= 1f + D10_GRASS_EVASION[l];
		//(D14) 팽팽한 숨: 마나가 가득 찬 동안 공격을 더 잘 피한다
		int tl = lvl(hero, H_TAUT_BREATH);
		if (tl >= 0 && Mana.get(hero).isFull()) m *= 1f + D14_TAUT_EVASION[tl];
		return m;
	}

	// =====================================================================
	// (D13) 연막 깃: 체력이 4분의 1 아래로 처음 떨어지면 발밑에서 연막(원작 SmokeScreen)이 터지고 잠깐 투명해진다 (긴 쿨타임).
	// 각성: 연막 속(둘레 2칸)의 적이 잠깐 혼란(원작 Vertigo)에 걸린다.
	// =====================================================================

	static void smokeFeather(Hero hero, int dmg){
		int l = lvl(hero, H_SMOKE_FEATHER);
		if (l < 0 || dmg <= 0) return;
		int threshold = Math.round(hero.HT * D13_HP_THRESHOLD);
		int after = hero.HP - dmg;
		if (hero.HP <= threshold || after > threshold || after <= 0) return; //4분의 1 선을 막 넘어 떨어지는 순간만
		if (hero.buff(BloomBuffs.SmokeCooldown.class) != null) return;
		Buff.affect(hero, BloomBuffs.SmokeCooldown.class, D13_COOLDOWN);
		Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.class, D13_INVIS_TURNS[l]);
		//(결정) 연막은 발밑과 바로 둘레(3×3)에만, 2턴 동안 (퍼지지 않음, 밝은 빛깔)
		for (int o : com.watabou.utils.PathFinder.NEIGHBOURS9){
			int c = hero.pos + o;
			if (!Dungeon.level.solid[c]){
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(
						com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(c, Math.round(D13_SMOKE_TURNS),
								com.shatteredpixel.shatteredpixeldungeon.actors.blobs.FeatherSmoke.class));
			}
		}
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.PUFF);
		BloomRules.debug("smoke feather: invisible " + D13_INVIS_TURNS[l]);
		if (awake(hero, H_SMOKE_FEATHER)){
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.isAlive() && m.alignment == Char.Alignment.ENEMY && Dungeon.level.distance(hero.pos, m.pos) <= D13_CONFUSE_RADIUS){
					Buff.prolong(m, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo.class, D13_CONFUSE_TURNS);
				}
			}
		}
	}

	// =====================================================================
	// (D14) 팽팽한 숨 각성: 마나가 가득 찬 동안 받는 첫 공격(Char.attack)을 완전히 피하고, 대신 마나가 조금 빠진다.
	// (마나가 빠져 가득이 아니게 되므로, 다시 가득 차야 또 피한다)
	// =====================================================================

	static boolean tautDodge(Hero hero, Char enemy){
		int l = lvl(hero, H_TAUT_BREATH);
		if (l < 0 || !awake(hero, H_TAUT_BREATH) || enemy == null || !hero.isAlive()) return false;
		Mana m = Mana.get(hero);
		if (!m.isFull()) return false;
		m.spend(D14_DODGE_MANA);
		if (hero.sprite != null && hero.sprite.visible){
			hero.sprite.showStatus(CharSprite.NEUTRAL, hero.defenseVerb());
		}
		BloomRules.debug("taut breath: dodged, mana -" + D14_DODGE_MANA);
		return true;
	}

	static void onHeroWait(Hero hero){
		walkStreak = 0; //(D16) 대기·탐색하면 이어 걷기가 끊긴다
		if (lvl(hero, H_GRASS_BODY) < 0 || !awake(hero, H_GRASS_BODY) || !inTallGrass(hero)) return;
		int n = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive() || m.alignment != Char.Alignment.ENEMY || m.state != m.HUNTING) continue;
			if (Char.hasProp(m, Char.Property.BOSS) || Char.hasProp(m, Char.Property.MINIBOSS)) continue;
			m.mazeLoseTrack(D10_LOST_TURNS);
			n++;
		}
		if (n > 0 && hero.sprite != null) hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 6);
		BloomRules.debug("grass body: " + n + " pursuer(s) lost me");
	}

	// =====================================================================
	// (D7) 튀는 일격 (형태): 스킬 타격이 빛줄기가 되어 대상에서 가장 가까운 다른 적 하나에게 튄다(덧타격, 약하게).
	// 각성: 한 번 더 튀어 셋째 적까지 닿는다. (덧타격이라 다른 형태를 부르지 않고 화살을 쓰지 않는다)
	// =====================================================================

	private static void ricochet(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		int l = lvl(hero, H_RICOCHET);
		if (l < 0 || !h.hit || h.target == null) return;
		if (!BloomRules.triggerOnHit(H_RICOCHET)) return;
		java.util.ArrayList<Char> hitList = new java.util.ArrayList<>();
		hitList.add(h.target);
		Char from = h.target;
		int bounces = awake(hero, H_RICOCHET) ? 2 : 1;
		for (int i = 0; i < bounces; i++){
			Char next = nearestOther(from, hitList, hero);
			if (next == null) break;
			if (from.sprite != null && next.sprite != null && from.sprite.parent != null && (from.sprite.visible || next.sprite.visible)){
				com.shatteredpixel.shatteredpixeldungeon.effects.Beam.LightRay ray =
						new com.shatteredpixel.shatteredpixeldungeon.effects.Beam.LightRay(from.sprite.center(), next.sprite.center());
				ray.hardlight(0.7f, 1f, 0.6f); //새잎빛
				from.sprite.parent.add(ray);
			}
			BloomEffects.extraHit(h, next, D7_RICOCHET_RATIO[l]);
			BloomRules.debug("ricochet -> " + next.name());
			hitList.add(next);
			from = next;
		}
	}

	//from에서 가장 가까운 (이미 맞은 적이 아닌) 적. D7_RANGE칸 안, 주인공이 보는 적
	private static Char nearestOther(Char from, java.util.ArrayList<Char> exclude, Hero hero){
		Char best = null;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (exclude.contains(m) || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[m.pos]) continue;
			int d = Dungeon.level.distance(from.pos, m.pos);
			if (d > D7_RANGE || d >= bestD) continue;
			best = m;
			bestD = d;
		}
		return best;
	}

	// =====================================================================
	// (D8) 새잎 자국 (형태): 스킬에 맞은 적의 둘레 빈 바닥에 새잎빛과 함께 원작 높은 풀이 자란다 (스킬 한 번에 한 번).
	// 풀을 둘 수 없는 칸(물·함정·문·벽 등)은 건너뛴다. 각성: 내 발밑에도 풀이 자란다.
	// =====================================================================

	private static boolean canGrow(int cell){
		if (cell < 0 || cell >= Dungeon.level.length()) return false;
		int t = Dungeon.level.map[cell];
		return t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY_DECO
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.GRASS;
	}

	private static void grow(int cell){
		com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell, com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HIGH_GRASS);
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateMap(cell);
		if (Dungeon.level.heroFOV[cell]){
			com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(cell).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 4);
		}
	}

	private static void sproutTrail(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		int l = lvl(hero, H_SPROUT_TRAIL);
		if (l < 0 || !h.hit || h.target == null) return;
		if (hero.buff(BloomBuffs.SproutCooldown.class) != null) return; //(결정) 쿨타임
		if (!BloomRules.trigger(H_SPROUT_TRAIL)) return;
		Buff.affect(hero, BloomBuffs.SproutCooldown.class, D8_COOLDOWN);
		java.util.ArrayList<Integer> cells = new java.util.ArrayList<>();
		for (int o : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = h.target.pos + o;
			if (c != hero.pos && canGrow(c)) cells.add(c);
		}
		java.util.Collections.shuffle(cells, new java.util.Random(Random.Long()));
		int n = Math.min(D8_SPROUT_CELLS[l], cells.size());
		for (int i = 0; i < n; i++) grow(cells.get(i));
		if (awake(hero, H_SPROUT_TRAIL) && canGrow(hero.pos)) grow(hero.pos);
		if (n > 0) com.shatteredpixel.shatteredpixeldungeon.Dungeon.observe();
		BloomRules.debug("sprout trail: " + n + " grass" + (awake(hero, H_SPROUT_TRAIL) ? " + under me" : ""));
	}

	// =====================================================================
	// (D11) 낙법: 한 번에 큰 피해(최대 체력의 D11_BIG_HIT 이상)를 받으면 그 일부를 줄이고, 때린 적 반대쪽으로 한 칸 구른다.
	// 구를 칸이 없거나 때린 적이 없으면(함정 등) 피해만 줄인다. 각성: 두 칸을 구른다.
	// =====================================================================

	static int breakfall(final Hero hero, int dmg, Object src){
		int l = lvl(hero, H_BREAKFALL);
		if (l < 0 || dmg <= 0 || dmg < hero.HT * D11_BIG_HIT) return dmg;
		int reduced = Math.round(dmg * (1f - D11_REDUCE[l]));
		BloomRules.debug("breakfall " + dmg + " -> " + reduced);
		if (src instanceof Char && src != hero && !hero.rooted){
			final Char from = (Char)src;
			final int power = awake(hero, H_BREAKFALL) ? D11_ROLL_AWAKE : D11_ROLL;
			com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica traj =
					new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(from.pos, hero.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET);
			traj = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(traj.collisionPos, traj.path.get(traj.path.size() - 1),
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
			if (traj.dist > 0 && !com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.pushingExistsForChar(hero)){
				com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(hero, traj, power, false, false, from);
				BloomRules.debug("breakfall roll " + power);
			}
		}
		if (hero.sprite != null) hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 5);
		return reduced;
	}

	// =====================================================================
	// (D12) 약초 캐기: 높은 풀을 밟으면 가끔 체력을 조금 회복한다 (사냥꾼이 이미 헤쳐나간 풀은 아님).
	// 각성: 체력이 가득하면 회복 대신 방어막(원작 Barrier)이 된다.
	// =====================================================================

	static void onHeroEnterCell(Hero hero, int cell){
		int l = lvl(hero, H_HERB_PICK);
		if (l < 0 || Dungeon.level.map[cell] != com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HIGH_GRASS) return;
		if (Random.Float() >= D12_HERB_CHANCE[l]) return;
		int amt = Math.max(1, Math.round(hero.HT * D12_HERB_HEAL[l]));
		if (hero.HP < hero.HT){
			int heal = Math.min(amt, hero.HT - hero.HP);
			hero.HP += heal;
			if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal),
					com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
			BloomRules.debug("herb pick: heal " + heal);
		} else if (awake(hero, H_HERB_PICK)){
			Buff.affect(hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class).incShield(amt);
			if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amt),
					com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.SHIELDING);
			BloomRules.debug("herb pick: shield " + amt);
		} else {
			return;
		}
		if (hero.sprite != null) hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 6);
	}

	// =====================================================================
	// (D15) 덫 놓기: 던지는 무기 하나를 발밑에 꽂아 덫으로 만든다 (던지는 무기의 행동 "덫 놓기", 1턴, 화살은 안 됨).
	// 덫(SnareTrap)을 적이 밟으면 그 무기 피해 × 비율(파생)을 받고 원작 속박. 무기는 그 칸에 떨어진다. 주인공은 밟아도 안 터진다.
	// 각성: 발밑이 아니라 1~2칸 떨어진 자리를 골라 던져 깐다.
	// =====================================================================

	public static boolean canSetTrap(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon w){
		return hero != null && w != null && lvl(hero, H_SET_TRAP) >= 0;
	}

	public static void setTrap(final Hero hero, final com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon w){
		if (awake(hero, H_SET_TRAP)){
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.selectCell(new com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.Listener() {
				@Override
				public void onSelect(Integer cell) {
					if (cell == null) return;
					if (hasSnare(cell)){
						com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(BloomBuffs.class, "snare_already"));
						return;
					}
					int d = Dungeon.level.distance(hero.pos, cell);
					if (d > D15_AWAKE_RANGE || !canPlant(cell) || (d > 0 && !Dungeon.level.heroFOV[cell])
							|| d > 0 && new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, cell,
								com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE).collisionPos != (int)cell){
						com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(BloomBuffs.class, "snare_bad_cell"));
						return;
					}
					plant(hero, w, cell);
				}
				@Override
				public String prompt() {
					return Messages.get(BloomBuffs.class, "snare_prompt");
				}
			});
		} else {
			if (hasSnare(hero.pos)){
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(BloomBuffs.class, "snare_already"));
				return;
			}
			if (!canPlant(hero.pos)){
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(BloomBuffs.class, "snare_bad_cell"));
				return;
			}
			plant(hero, w, hero.pos);
		}
	}

	private static boolean hasSnare(int cell){
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap s =
				(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap)Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap.class);
		return s != null && s.weapons.containsKey(cell);
	}

	private static boolean canPlant(int cell){
		if (cell < 0 || cell >= Dungeon.level.length()) return false;
		if (Dungeon.level.solid[cell] || !Dungeon.level.passable[cell] || Dungeon.level.pit[cell]) return false;
		if (Dungeon.level.water[cell]) return false;
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap s =
				(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap)Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap.class);
		if (s != null && s.weapons.containsKey(cell)) return false; //한 칸에 덫 하나 (이미 놓은 곳에는 못 놓는다)
		if (Dungeon.level.traps.get(cell) != null) return false; //원작 함정이 있는 칸도 안 된다
		Char c = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell);
		return c == null || c == Dungeon.hero;
	}

	private static void plant(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon w, int cell){
		com.shatteredpixel.shatteredpixeldungeon.items.Item one = w.detach(hero.belongings.backpack);
		if (one == null) return;
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(
				com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(cell, 1, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap.class));
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap s =
				(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap)Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap.class);
		s.plant(cell, one);
		if (hero.sprite != null) hero.sprite.operate(cell);
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.TRAP);
		BloomRules.debug("set trap at " + cell + " with " + one.name());
		hero.spendAndNext(1f);
		com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
	}

	//적이 칸에 들어섰다 (Level.occupyCell): 덫이 터진다
	static void onMobEnterCell(Hero hero, Char ch){
		if (hero == null || ch == null || ch == hero || !(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || ch.flying) return; //나는 적은 밟지 않는다 (원작 함정처럼)
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap s =
				(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap)Dungeon.level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SnareTrap.class);
		if (s == null || !s.weapons.containsKey(ch.pos)) return;
		int cell = ch.pos;
		com.shatteredpixel.shatteredpixeldungeon.items.Item item = s.spring(cell);
		int l = Math.max(0, lvl(hero, H_SET_TRAP));
		int dmg = 1;
		if (item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon){
			dmg = Math.max(1, Math.round(((com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon)item).damageRoll(hero) * D15_SNARE_DMG[l]));
		}
		if (ch.sprite != null && ch.sprite.visible){
			ch.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 8);
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.TRAP);
		}
		BloomRules.debug("snare sprung by " + ch.name() + " dmg " + dmg);
		BloomRules.derivedDamage(H_SET_TRAP, ch, dmg);
		if (ch.isAlive()) Buff.prolong(ch, Roots.class, D15_ROOT_TURNS[l]);
		if (item != null) Dungeon.level.drop(item, cell).sprite.drop();
	}

	// =====================================================================
	// (D16) 정령의 춤: 걸을 때마다 마나가 조금 더 찬다 (원래 턴 회복에 더해서).
	// 각성: 다섯 걸음을 이어 걸은 뒤의 첫 공격은 반드시 맞는다.
	// =====================================================================

	private static int walkStreak = 0;

	private static void spiritDanceStep(Hero hero){
		int l = lvl(hero, H_SPIRIT_DANCE);
		if (l < 0) return;
		Mana.get(hero).gain(D16_MANA_PER_STEP[l]);
	}

	//걸음 수 세기 (onHeroSpend에서): 걸은 행동이면 +1, 아니면 처음부터
	private static void spiritDanceSpend(Hero hero, boolean walked, boolean attacked){
		if (attacked) Buff.detach(hero, BloomBuffs.SpiritStride.class); //첫 공격에 쓰였다
		if (lvl(hero, H_SPIRIT_DANCE) < 0 || !awake(hero, H_SPIRIT_DANCE)){
			walkStreak = 0;
			return;
		}
		walkStreak = walked ? walkStreak + 1 : 0;
		if (walkStreak >= D16_STRIDE_STEPS && hero.buff(BloomBuffs.SpiritStride.class) == null){
			Buff.affect(hero, BloomBuffs.SpiritStride.class);
			BloomRules.debug("spirit dance: next attack always hits");
		}
	}

	static boolean strideSureHit(Hero hero){
		return hero.buff(BloomBuffs.SpiritStride.class) != null && lvl(hero, H_SPIRIT_DANCE) >= 0;
	}

	// =====================================================================
	// (D17) 못 박기 (활 전용): 바로 뒤가 벽인 적을 화살로 맞히면 벽에 박혀 잠깐 묶인다(원작 속박).
	// 각성: 뒤가 벽이 아니라 다른 적이어도 둘을 함께 꿰어 묶는다.
	// =====================================================================

	//주인공 → 적 방향으로 적 바로 뒤 칸
	private static int behindCell(Hero hero, Char enemy){
		int w = Dungeon.level.width();
		int dx = (enemy.pos % w) - (hero.pos % w), dy = (enemy.pos / w) - (hero.pos / w);
		int c = enemy.pos + Integer.signum(dx) + Integer.signum(dy) * w;
		return (c < 0 || c >= Dungeon.level.length() || c == enemy.pos) ? -1 : c;
	}

	static void pin(Hero hero, Char enemy){
		int l = lvl(hero, H_PIN);
		if (l < 0 || enemy == null || !enemy.isAlive() || !rangedNow(hero) || BloomEffects.inExtraHit()) return;
		int behind = behindCell(hero, enemy);
		if (behind < 0) return;
		Char other = null;
		if (!Dungeon.level.solid[behind]){
			if (!awake(hero, H_PIN)) return;
			Char c = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(behind);
			if (!(c instanceof Mob) || c.alignment != Char.Alignment.ENEMY || !c.isAlive()) return;
			other = c;
		}
		if (!BloomRules.trigger(H_PIN)) return;
		Buff.prolong(enemy, Roots.class, D17_PIN_TURNS[l]);
		if (other != null) Buff.prolong(other, Roots.class, D17_PIN_TURNS[l]);
		if (enemy.sprite != null && enemy.sprite.visible){
			com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.center(other != null ? other.pos : behind)
					.burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.DUST), 4);
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.HIT_ARROW, 1f, 0.8f);
		}
		BloomRules.debug("pin " + D17_PIN_TURNS[l] + "t -> " + enemy.name() + (other != null ? " + " + other.name() : " (wall)"));
	}

	// =====================================================================
	// (D18) 박힌 화살 (활 전용 형태): 활 스킬의 화살이 빛나며 적에게 박혀 남는다(한 적에 하나, 스킬 한 번에 한 번).
	// 그 적을 근접 평타로 치면 화살이 터져 박힐 때의 타격 피해 × 비율(파생)을 주고 화살 하나를 돌려받는다.
	// 각성: 그 적이 밀쳐지면(벽에 부딪히는 것 포함, WandOfBlastWave.throwChar) 내가 치지 않아도 터진다.
	// =====================================================================

	private static Char pendingBurst = null; //근접 평타가 맞은 박힌 적 (평타가 끝난 뒤 터뜨림)
	private static int pendingAction = -1;  //그 평타의 행동 번호 (다른 행동에서 잘못 터지지 않게)

	private static void embedArrow(BloomEffects.SkillHit h){
		Hero hero = h.hero;
		Char t = h.target;
		if (lvl(hero, H_EMBED_ARROW) < 0 || !h.ranged || !h.hit || t == null || !t.isAlive()) return;
		if (t.buff(BloomBuffs.EmbeddedArrow.class) != null) return; //한 적에 하나
		if (!BloomRules.trigger(H_EMBED_ARROW)) return;
		BloomBuffs.EmbeddedArrow e = Buff.affect(t, BloomBuffs.EmbeddedArrow.class);
		e.dmg = Math.max(1, h.damage);
		com.watabou.utils.Point a = Dungeon.level.cellToPoint(h.fromCell), b = Dungeon.level.cellToPoint(t.pos);
		e.angle = 135 - (float)(Math.atan2(b.x - a.x, b.y - a.y) / Math.PI * 180); //MissileSprite와 같은 방향 계산
		e.fx(false); e.fx(true); //붙을 때 이미 그려진 그림을 새 각도로 다시
		BloomRules.debug("embed arrow in " + t.name() + " (" + e.dmg + ")");
	}

	//Hero.attackProc 끝: 활 근접 평타가 박힌 적을 맞혔다
	static void markEmbedHit(Hero hero, Char enemy){
		if (enemy == null || enemy.buff(BloomBuffs.EmbeddedArrow.class) == null || lvl(hero, H_EMBED_ARROW) < 0) return;
		if (!BloomJudge.isNormalAttack() || rangedNow(hero) || BloomEffects.inExtraHit() || hero.belongings.thrownWeapon != null) return;
		pendingBurst = enemy;
		pendingAction = BloomRules.actionId();
	}

	//평타가 끝난 뒤 (밀쳐낸 뒤라도 터진다)
	static void afterNormalAttack(Hero hero, Char enemy){
		Char t = pendingBurst;
		pendingBurst = null;
		if (t != null && t == enemy && pendingAction == BloomRules.actionId()) burstEmbed(hero, t);
	}

	//각성: 박힌 적이 밀쳐지는 순간 (WandOfBlastWave.throwChar)
	public static void onCharThrown(Char ch){
		Hero hero = Dungeon.hero;
		if (hero == null || ch == null || ch.buff(BloomBuffs.EmbeddedArrow.class) == null) return;
		if (lvl(hero, H_EMBED_ARROW) < 0 || !awake(hero, H_EMBED_ARROW)) return;
		burstEmbed(hero, ch);
	}

	private static void burstEmbed(Hero hero, Char t){
		BloomBuffs.EmbeddedArrow e = t.buff(BloomBuffs.EmbeddedArrow.class);
		if (e == null) return;
		int l = Math.max(0, lvl(hero, H_EMBED_ARROW));
		int dmg = Math.max(1, Math.round(e.dmg * D18_BURST_DMG[l]));
		e.detach();
		//박혀 있던 화살 하나는 죽을 때 떨어지는 몫(Arrow.Stuck)에서 빼서 돌려준다
		com.shatteredpixel.shatteredpixeldungeon.items.Arrow.Stuck st = t.buff(com.shatteredpixel.shatteredpixeldungeon.items.Arrow.Stuck.class);
		if (st != null && st.count > 0) st.count--;
		com.shatteredpixel.shatteredpixeldungeon.items.Item a = new com.shatteredpixel.shatteredpixeldungeon.items.Arrow().quantity(1);
		if (!a.collect()) Dungeon.level.drop(a, hero.pos).sprite.drop();
		if (t.sprite != null && t.sprite.visible){
			t.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.GENERAL, 8);
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.BLAST, 0.6f, 1.4f);
		}
		BloomRules.debug("embedded arrow burst on " + t.name() + " dmg " + dmg);
		BloomRules.derivedDamage(H_EMBED_ARROW, t, dmg);
		com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
	}

	// =====================================================================
	// (D19) 화살 줍기 (활 전용): 떨어진 화살 위를 걸어 지나면 턴을 쓰지 않고 줍는다.
	// 주인공 그림이 그 칸에 닿은 뒤(Hero.onMotionComplete) 원작 줍기처럼 가방으로 들어가는 그림(GameScene.pickUp)과 함께.
	// 쓰러진 적이 박혀 있던 화살을 더 자주 떨군다(Arrow.recover 확률 +).
	// 각성: 걸을 때마다 세 칸 안의 보이는 떨어진 화살을 그 자리에서 줍는다(원작 줍기 그림, 당겨 오는 연출 없음).
	// =====================================================================

	private static boolean walkedForPickup = false; //이번 이동이 걸음인지 (도착했을 때 본다)

	public static float arrowDropBonus(){
		Hero hero = Dungeon.hero;
		if (hero == null) return 0;
		int l = lvl(hero, H_ARROW_PICKUP);
		return l < 0 ? 0 : D19_DROP_BONUS[l];
	}

	private static boolean isArrow(com.shatteredpixel.shatteredpixeldungeon.items.Item it){
		return it.getClass() == com.shatteredpixel.shatteredpixeldungeon.items.Arrow.class; //날아가는 그림용 Shot 제외
	}

	//가방에 넣고 원작 줍기 그림·소리. 가방이 차면 그 칸에 다시 둔다
	private static void collectArrow(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.Item it, int from){
		int n = it.quantity();
		if (!it.collect(hero.belongings.backpack)){
			Dungeon.level.drop(it, from).sprite.drop();
			return;
		}
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.pickUp(it, from);
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.ITEM);
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i(Messages.get(BloomBuffs.class, "arrows_picked", n));
		com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
		BloomRules.debug("arrow pickup +" + n);
	}

	//주인공 그림이 칸에 닿았다 (Hero.onMotionComplete)
	public static void onHeroArrived(final Hero hero){
		boolean walked = walkedForPickup;
		walkedForPickup = false;
		if (hero == null || lvl(hero, H_ARROW_PICKUP) < 0) return;

		//발밑의 화살
		com.shatteredpixel.shatteredpixeldungeon.items.Heap here = Dungeon.level.heaps.get(hero.pos);
		if (here != null && here.type == com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP){
			for (com.shatteredpixel.shatteredpixeldungeon.items.Item it : here.items.toArray(new com.shatteredpixel.shatteredpixeldungeon.items.Item[0])){
				if (!isArrow(it)) continue;
				here.remove(it);
				collectArrow(hero, it, hero.pos);
			}
		}

		//각성: 세 칸 안의 보이는 화살을 그 자리에서 줍는다 (걸었을 때만)
		if (!walked || !awake(hero, H_ARROW_PICKUP) || hero.sprite == null || hero.sprite.parent == null) return;
		for (com.shatteredpixel.shatteredpixeldungeon.items.Heap heap : Dungeon.level.heaps.valueList()){
			if (heap.pos == hero.pos || heap.type != com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type.HEAP) continue;
			if (!Dungeon.level.heroFOV[heap.pos] || Dungeon.level.distance(hero.pos, heap.pos) > D19_PULL_RANGE) continue;
			final int from = heap.pos;
			for (com.shatteredpixel.shatteredpixeldungeon.items.Item it : heap.items.toArray(new com.shatteredpixel.shatteredpixeldungeon.items.Item[0])){
				if (!isArrow(it)) continue;
				heap.remove(it);
				collectArrow(hero, it, from); //그 자리에서 바로 가방으로 (원작 줍기 그림)
			}
		}
	}

	// =====================================================================
	// (D21) 활대 치기 (활 전용): 활의 근접 평타 피해가 오르고(hitDamage), 밀쳐낸 직후의 사격이 강해진다(BowBashReady).
	// 각성: 밀쳐낸 뒤 나도 한 칸 물러선다 (물러설 칸이 없거나 구덩이면 그대로).
	// =====================================================================

	//활 근접 평타가 적을 밀쳐낸다 (WornShortbow.knockback, 밀치기 직전)
	public static void onBowKnockback(final Hero hero, Char enemy){
		if (hero == null || enemy == null || lvl(hero, H_BOW_BASH) < 0) return;
		BloomBuffs.BowBashReady bb = Buff.affect(hero, BloomBuffs.BowBashReady.class);
		bb.used = false;
		bb.spends = 0;
		BloomRules.debug("bow bash: next shot is stronger");
		if (!awake(hero, H_BOW_BASH) || hero.rooted || com.shatteredpixel.shatteredpixeldungeon.effects.Pushing.pushingExistsForChar(hero)) return;
		int w = Dungeon.level.width();
		int dx = Integer.signum((hero.pos % w) - (enemy.pos % w)), dy = Integer.signum((hero.pos / w) - (enemy.pos / w));
		int back = hero.pos + dx + dy * w;
		if (back < 0 || back >= Dungeon.level.length() || back == hero.pos) return;
		if (Dungeon.level.pit[back] || Dungeon.level.solid[back] || !Dungeon.level.passable[back]
				|| com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(back) != null) return;
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica traj =
				new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, back, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
		if (traj.dist < 1) return;
		com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(hero, traj, 1, false, false, enemy);
		BloomRules.debug("bow bash: step back");
	}

	private static void bowBashSpend(Hero hero){
		BloomBuffs.BowBashReady bb = hero.buff(BloomBuffs.BowBashReady.class);
		if (bb == null) return;
		if (bb.used || ++bb.spends >= 2) bb.detach(); //쏘았거나, 밀쳐낸 다음 행동이 사격이 아니었다
	}

	// =====================================================================
	// (D3) 벌어진 상처: 내 공격에 맞을 때마다 적에게 상처가 쌓인다(상한). 가득한 적을 스킬로 맞히면 터져 큰 피해.
	// 각성: 터질 때 상처의 절반이 가장 가까운 적에게 옮겨 붙는다 (옮겨 붙은 상처가 터지면 다시 옮겨 가지 않는다).
	// =====================================================================

	static void openWound(final Hero hero, final Char enemy, int damage, boolean skill){
		final int l = lvl(hero, H_OPEN_WOUND);
		if (l < 0 || damage <= 0 || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY) return;
		BloomBuffs.OpenWound w = enemy.buff(BloomBuffs.OpenWound.class);
		//(결정) 상처가 있는 적을 스킬로 맞혔다 → 맞힌 뒤에 터진다 (상처가 많을수록 세다)
		if (skill && w != null && w.stacks() > 0 && !w.bursting){
			w.bursting = true;
			final BloomBuffs.OpenWound fw = w;
			final int stacks = w.stacks();
			final int dmg = Math.max(1, Math.round(damage * stacks * D3_BURST_PER_STACK[l]));
			BloomEffects.afterHit(new Runnable() {
				@Override
				public void run() {
					boolean spread = awake(hero, H_OPEN_WOUND) && !fw.transferred;
					fw.detach();
					if (enemy.sprite != null && enemy.sprite.visible){
						enemy.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle.BURST, 14);
					}
					BloomRules.debug("open wound burst " + stacks + " stack(s) -> " + dmg);
					int at = enemy.pos;
					BloomRules.derivedDamage(H_OPEN_WOUND, enemy, dmg);
					if (spread && stacks / 2 > 0) spreadWound(enemy, at, stacks / 2);
				}
			});
			return;
		}
		if (!BloomRules.triggerOnHit(H_OPEN_WOUND)) return;
		Buff.affect(enemy, BloomBuffs.OpenWound.class).add(1);
	}

	private static void spreadWound(Char from, int at, int stacks){
		Char best = null;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m == from || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			int d = Dungeon.level.distance(at, m.pos);
			if (d < bestD){ best = m; bestD = d; }
		}
		if (best == null) return;
		BloomBuffs.OpenWound w = Buff.affect(best, BloomBuffs.OpenWound.class);
		w.add(stacks);
		w.transferred = true;
		if (best.sprite != null && best.sprite.visible){
			best.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle.FACTORY, 6);
		}
		BloomRules.debug("open wound spread -> " + best.name());
	}

	// =====================================================================
	// (D4) 치고 빠지기: 바로 앞 행동에 걸었다면 이번 공격이 강해진다.
	// 각성: 공격한 직후의 이동 한 번이 턴을 쓰지 않는다 (간격 D4_FREE_CD, 턴을 쓰지 않는 효과는 한 번에 하나).
	// =====================================================================

	private static boolean movedThisAction = false;

	static void onHeroMoved(){
		movedThisAction = true;
		if (Dungeon.hero != null) spiritDanceStep(Dungeon.hero); //(D16) 정령의 춤
		walkedForPickup = true; //(D19) 화살 줍기: 도착하면 줍는다
	}

	static boolean movedNow(){
		return movedThisAction;
	}

	//주인공이 턴을 쓸 때. 실제로 쓸 시간을 돌려준다
	static float onHeroSpend(Hero hero, float time, boolean attacked, boolean moved){
		movedThisAction = false;
		cleanCandidate = -1;
		spiritDanceSpend(hero, moved && !attacked, attacked); //(D16) 정령의 춤 각성
		bowBashSpend(hero); //(D21) 활대 치기

		//(D6) 값진 일격 각성: 한 번에 쓰러뜨린 바로 다음 행동이 공격이면 턴을 쓰지 않는다
		BloomBuffs.FreeAttack fa = hero.buff(BloomBuffs.FreeAttack.class);
		if (fa != null){
			fa.detach();
			if (attacked && hero.buff(BloomBuffs.FreeActionCooldown.class) == null){
				Buff.affect(hero, BloomBuffs.FreeActionCooldown.class, D4_FREE_CD);
				BloomRules.debug("clean kill: free attack");
				time = 0;
			}
		}
		if (cleanKillPending){
			cleanKillPending = false;
			if (lvl(hero, H_CLEAN_KILL) >= 0 && hero.buff(BloomBuffs.FreeActionCooldown.class) == null){
				Buff.affect(hero, BloomBuffs.FreeAttack.class);
			}
		}

		int l = lvl(hero, H_HIT_AND_RUN);
		if (l < 0){
			Buff.detach(hero, BloomBuffs.HitAndRun.class);
			Buff.detach(hero, BloomBuffs.FreeStep.class);
			return time;
		}

		boolean walked = moved && !attacked;

		//각성: 공격한 직후의 이동 한 번은 턴을 쓰지 않는다
		BloomBuffs.FreeStep free = hero.buff(BloomBuffs.FreeStep.class);
		if (free != null){
			free.detach();
			if (walked && hero.buff(BloomBuffs.FreeActionCooldown.class) == null){
				Buff.affect(hero, BloomBuffs.FreeActionCooldown.class, D4_FREE_CD);
				BloomRules.debug("hit and run: free step");
				time = 0;
			}
		}

		//걸었으면 다음 공격이 강해진다. 그 밖의 행동을 하면 풀린다
		if (walked) Buff.affect(hero, BloomBuffs.HitAndRun.class);
		else        Buff.detach(hero, BloomBuffs.HitAndRun.class);

		if (attacked && awake(hero, H_HIT_AND_RUN) && hero.buff(BloomBuffs.FreeActionCooldown.class) == null
				&& hero.buff(BloomBuffs.FreeAttack.class) == null){ //턴을 쓰지 않는 효과는 한 번에 하나
			Buff.affect(hero, BloomBuffs.FreeStep.class);
		}
		return time;
	}

	// =====================================================================
	// (D1) 첫 일격 각성: 그 공격은 소리가 나지 않아 주변의 적을 깨우지 않는다.
	// 둘레 D1_HUSH_RADIUS칸 안의 잠든 적이 D1_HUSH_TURNS턴 동안 깨어나지 않는다 (Mob.Sleeping → wakeChance 0).
	// =====================================================================

	private static void hush(Char struck){
		int n = 0;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m == struck || !m.isAlive() || m.state != m.SLEEPING) continue;
			if (Dungeon.level.distance(struck.pos, m.pos) > D1_HUSH_RADIUS) continue;
			Buff.prolong(m, BloomBuffs.Hushed.class, D1_HUSH_TURNS);
			n++;
		}
		BloomRules.debug("first strike hush: " + n + " sleeper(s)");
	}

	//잠든 적이 깨어날 확률 (Mob.Sleeping)
	static float wakeChance(Mob mob, float chance){
		return mob.buff(BloomBuffs.Hushed.class) != null ? 0f : chance;
	}

	// =====================================================================
	// (D2) 약점 각성: 치명타가 터지면 걸려 있던 느려짐·속박이 한 턴 더 이어진다.
	// =====================================================================

	@SuppressWarnings("unchecked")
	private static final Class<? extends FlavourBuff>[] HOLDS = new Class[]{
			Slow.class, Cripple.class, Chill.class, Frostbite.class, Roots.class
	};

	private static void extendHold(Char enemy){
		for (Class<? extends FlavourBuff> c : HOLDS){
			FlavourBuff b = enemy.buff(c);
			if (b != null) Buff.prolong(enemy, c, b.cooldown() + D2_AWAKE_EXTEND);
		}
	}
}
