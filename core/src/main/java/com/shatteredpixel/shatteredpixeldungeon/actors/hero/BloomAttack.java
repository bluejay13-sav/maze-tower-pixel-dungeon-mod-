/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 2차 기본공격 전용 특성 9개 (연속 베기, 강타, 질풍, 표식 새기기, 돌진, 불꽃 칼날, 물보라, 마나 흡수, 확정타).
//BloomEffects가 원작 코드에서 받은 호출을 여기로 넘긴다. 횟수·파생·마나 규칙은 BloomRules를 거친다.
//돌진은 스킬에도 붙는다(이동한 바로 다음 공격).
public class BloomAttack {

	private static int lvl(Hero hero, BloomTalent t){
		return BloomEffects.lvl(hero, t);
	}

	private static boolean awake(Hero hero, BloomTalent t){
		return BloomTree.awakened(hero, t);
	}

	// =====================================================================
	// 이번 행동의 상태 (평타·스킬이 시작될 때 정한다)
	// =====================================================================

	static boolean chargeActive = false; //돌진: 이동한 바로 다음 공격
	static boolean galeActive   = false; //질풍: 공격속도 증가를 쓴 평타
	static boolean sureActive   = false; //확정타: 빗나간 다음 평타
	static boolean drainPierce  = false; //마나 흡수 각성: 마나가 가득 찬 채 시작한 평타
	static int lastHitDamage    = 0;     //마지막 타격 피해 (질풍 각성 휩쓸기)

	//타격이 끝난 뒤 할 일 (밀침·기절은 피해가 들어간 다음에)
	private static final ArrayList<Runnable> postHit = new ArrayList<>();

	public static void afterHit(Runnable r){
		postHit.add(r);
	}

	static void flushPostHit(){
		ArrayList<Runnable> list = new ArrayList<>(postHit);
		postHit.clear();
		for (Runnable r : list) r.run();
	}

	//평타·스킬 시작 (BloomEffects.beginNormal / beginSkill)
	static void onActionStart(Hero hero, boolean skill){
		postHit.clear();
		chargeActive = false;
		galeActive = false;
		sureActive = false;
		drainPierce = false;
		lastHitDamage = 0;

		//돌진: 이동한 뒤 첫 공격 (평타·스킬)
		BloomBuffs.ChargeReady ready = hero.buff(BloomBuffs.ChargeReady.class);
		if (ready != null){
			ready.detach();
			if (lvl(hero, CHARGE) >= 0 && hero.buff(BloomBuffs.ChargeCooldown.class) == null){
				chargeActive = true;
				Buff.affect(hero, BloomBuffs.ChargeCooldown.class, B2_CHARGE_COOLDOWN);
			}
		}

		if (skill) return;

		//질풍: 공격속도 증가 1회 사용
		BloomBuffs.Gale gale = hero.buff(BloomBuffs.Gale.class);
		if (gale != null && lvl(hero, GALE) >= 0){
			galeActive = true;
			gale.use();
		}

		//확정타: 빗나간 다음 평타
		BloomBuffs.SureStrike sure = hero.buff(BloomBuffs.SureStrike.class);
		if (sure != null && lvl(hero, SURE_STRIKE) >= 0){
			sureActive = true;
			sure.use();
		}

		//마나 흡수 각성
		drainPierce = awake(hero, MANA_DRAIN) && lvl(hero, MANA_DRAIN) >= 0 && Mana.get(hero).isFull();
	}

	// =====================================================================
	// 명중·공격속도·방어 무시
	// =====================================================================

	static float accuracyMulti(Hero hero){
		int s = lvl(hero, SURE_STRIKE);
		if (sureActive && s >= 0) return 1f + B2_SURE_BONUS[s];
		return 1f;
	}

	//평타가 끝나고 쓰는 시간 배율 (Hero.finishAttackComplete)
	public static float attackDelayMulti(Hero hero){
		return galeActive ? 1f / (1f + B2_GALE_SPEED) : 1f;
	}

	//방어력에서 무시하는 비율 (돌진 각성, 마나 흡수 각성)
	static float armorIgnore(Hero hero, boolean skill){
		float ignore = 0;
		if (chargeActive && awake(hero, CHARGE)) ignore = Math.max(ignore, B2_CHARGE_AWAKE_IGNORE);
		if (!skill && drainPierce) ignore = Math.max(ignore, B2_DRAIN_AWAKE_IGNORE);
		return ignore;
	}

	// =====================================================================
	// 피해 배율 (Hero.attackProc → BloomEffects.processHit)
	// =====================================================================

	//평타·스킬 모두
	static float anyHitBonus(Hero hero, Char enemy){
		int c = lvl(hero, CHARGE);
		if (chargeActive && c >= 0) return B2_CHARGE_DMG[c];
		return 0;
	}

	//평타만
	static float normalHitBonus(final Hero hero, final Char enemy){
		float bonus = 0;

		//확정타
		int s = lvl(hero, SURE_STRIKE);
		if (sureActive && s >= 0){
			bonus += B2_SURE_BONUS[s];
			if (BloomRules.trigger(SURE_STRIKE)) com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.target(enemy); //조준 과녁
		}

		//강타: 같은 적에게 3번째 평타마다
		int h = lvl(hero, HEAVY_BLOW);
		if (h >= 0){
			BloomBuffs.HeavyCount hc = Buff.affect(hero, BloomBuffs.HeavyCount.class);
			if (hc.target != enemy.id()){
				hc.target = enemy.id();
				hc.count = 0;
			}
			hc.count++;
			if (hc.count >= B2_HEAVY_HITS){
				hc.count = 0;
				bonus += B2_HEAVY_DMG[h];
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.heavyBlow(hero, enemy); //굵은 베기 + 약한 화면 흔들림
				if (BloomRules.triggerOnHit(HEAVY_BLOW)){
					final boolean push = h >= 2;
					final boolean stun = awake(hero, HEAVY_BLOW);
					afterHit(new Runnable() {
						@Override
						public void run() {
							if (!enemy.isAlive()) return;
							if (push) pushAway(hero, enemy, B2_HEAVY_PUSH);
							if (stun && enemy.isAlive()) Buff.affect(enemy, Paralysis.class, B2_HEAVY_AWAKE_STUN);
						}
					});
				}
			}
			com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshHero(); //강타 준비 아이콘
		}
		return bonus;
	}

	// =====================================================================
	// 평타 적중 시 효과 (피해가 정해진 뒤)
	// =====================================================================

	static void onNormalHit(final Hero hero, final Char enemy, int damage){
		lastHitDamage = damage;

		//표식 새기기
		int m = lvl(hero, MARK_CARVE);
		if (m >= 0 && BloomRules.triggerOnHit(MARK_CARVE)){
			BloomBuffs.Mark.add(enemy, B2_MARK_MAX[m], false);
		}

		//불꽃 칼날
		int fb = lvl(hero, FLAME_BLADE);
		if (fb >= 0 && Random.Float() < B2_FLAME_CHANCE[fb] && BloomRules.triggerOnHit(FLAME_BLADE)){
			BloomEffects.ignite(hero, enemy, B2_FLAME_TURNS);
		}

		//물보라: 젖음은 피해가 들어간 뒤 (빙결 반응 순서를 지키려고)
		int sp = lvl(hero, SPLASH);
		if (sp >= 0 && Random.Float() < B2_SPLASH_CHANCE[sp] && BloomRules.triggerOnHit(SPLASH)){
			afterHit(new Runnable() {
				@Override
				public void run() {
					if (!enemy.isAlive()) return;
					Wet w = Buff.affect(enemy, Wet.class);
					if (w != null) w.setLeft((int)B2_SPLASH_TURNS);
				}
			});
		}

		//마나 흡수
		int md = lvl(hero, MANA_DRAIN);
		if (md >= 0 && BloomRules.triggerOnHit(MANA_DRAIN)) BloomRules.gainMana(hero, B2_MANA_DRAIN[md]);
	}

	// =====================================================================
	// 평타가 끝난 뒤 (Hero.onAttackComplete → BloomEffects.afterNormalAttack)
	// =====================================================================

	//연속 베기로 이어서 처리하면 true (그때는 끝난 뒤 done을 부른다)
	static boolean afterNormalAttack(final Hero hero, final Char enemy, boolean hit, final Runnable done){
		flushPostHit();

		//마나 흡수 각성: 방어 무시 평타 뒤 마나 감소
		if (drainPierce && hit){
			drainPierce = false;
			Mana.get(hero).spend(Math.min(Mana.get(hero).cur(), B2_DRAIN_AWAKE_COST));
		}

		//확정타: 평타가 빗나가면 다음 평타 준비 (추가 평타의 빗나감은 세지 않는다)
		int s = lvl(hero, SURE_STRIKE);
		if (!hit && s >= 0 && !BloomRules.inExtraAttack()
				&& hero.buff(BloomBuffs.SureCooldown.class) == null){
			BloomBuffs.SureStrike st = Buff.affect(hero, BloomBuffs.SureStrike.class);
			st.charges = awake(hero, SURE_STRIKE) ? B2_SURE_AWAKE_HITS : 1;
			Buff.affect(hero, BloomBuffs.SureCooldown.class, B2_SURE_COOLDOWN);
		}

		//연속 베기: 같은 적에게 추가 평타
		int d = lvl(hero, DOUBLE_SLASH);
		if (d >= 0 && hit && enemy != null && enemy.isAlive() && BloomRules.canExtraAttack()
				&& Dungeon.level.adjacent(hero.pos, enemy.pos)
				&& Random.Float() < B2_DOUBLE_SLASH_CHANCE[d] && BloomRules.trigger(DOUBLE_SLASH)){
			if (hero.sprite != null && hero.sprite.parent != null){
				//평타가 끝난 뒤 잠깐 텀을 두고 원 베기
				hero.sprite.parent.add(new Delayer(SHADOW_BLADE_DELAY){
					@Override
					protected void onComplete() {
						super.onComplete();
						extraStrike(hero, enemy);
						done.run();
					}
				});
				return true;
			}
			extraStrike(hero, enemy);
		}
		return false;
	}

	private static void extraStrike(final Hero hero, final Char enemy){
		if (!hero.isAlive() || !enemy.isAlive()) return;
		TalentEffects.shadowStrike(hero, enemy); //원작 그림자검 원 베기 연출 재사용
		BloomRules.extraAttack(awake(hero, DOUBLE_SLASH), new java.util.concurrent.Callable<Boolean>() {
			@Override
			public Boolean call() {
				return hero.attack(enemy);
			}
		});
		flushPostHit();
	}

	// =====================================================================
	// 받는 피해 배율 (Char.damage, 주인공이 준 피해일 때)
	// =====================================================================

	//표식: 어떤 피해든(화상·독·폭탄·함정·원소 반응·아군 등 모두) 더 받는다.
	//불꽃 칼날 각성: 주인공이 준 피해만.
	static float incomingMulti(Char target, Object src, boolean byHero){
		Hero hero = Dungeon.hero;
		if (hero == null) return 1f;
		float multi = 1f;
		//표식 1개당 받는 피해 증가
		BloomBuffs.Mark mark = target.buff(BloomBuffs.Mark.class);
		if (mark != null) multi += B2_MARK_DMG_PER * mark.stacks;
		//불꽃 칼날 각성: 화상 중인 적에게 모든 피해
		if (byHero && awake(hero, FLAME_BLADE) && target.buff(Burning.class) != null) multi += B2_FLAME_AWAKE_DMG;
		return multi;
	}

	// =====================================================================
	// 이동·처치
	// =====================================================================

	//주인공이 한 칸 움직였을 때 (Hero.move)
	public static void onMove(Hero hero){
		if (lvl(hero, CHARGE) >= 0 && hero.buff(BloomBuffs.ChargeCooldown.class) == null){
			Buff.affect(hero, BloomBuffs.ChargeReady.class);
		}
	}

	//주인공이 적을 처치했을 때 (BloomRules.onDeath)
	static void onKill(final Hero hero, Char enemy, boolean byDerived){
		boolean normalKill = BloomRules.kind() == BloomRules.Kind.NORMAL && !byDerived;

		//질풍: 평타로 처치하면 다음 평타 공격속도 증가
		int g = lvl(hero, GALE);
		if (g >= 0 && normalKill){
			//각성: 질풍 상태에서 처치하면 인접한 모든 적 휩쓸기 (파생)
			if (galeActive && awake(hero, GALE) && BloomRules.trigger(GALE)){
				int dmg = Math.round(lastHitDamage * B2_GALE_AWAKE_DMG);
				BloomRules.debug("gale sweep " + dmg);
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.galeSweep(hero);
				for (int n : PathFinder.NEIGHBOURS8){
					Char c = Actor.findChar(hero.pos + n);
					if (c != null && c != enemy && c.alignment == Char.Alignment.ENEMY){
						BloomRules.derivedDamage(GALE, c, dmg);
					}
				}
			}
			BloomBuffs.Gale gb = Buff.prolong(hero, BloomBuffs.Gale.class, B2_GALE_TURNS);
			gb.charges = B2_GALE_CHARGES[g];
		}

		//물보라 각성: 젖은 적을 처치하면 그 칸에 물웅덩이 (원작 물 1칸)
		if (awake(hero, SPLASH) && (enemy.buff(Wet.class) != null || Wet.onWater(enemy))){
			Dungeon.level.setCellToWater(false, enemy.pos);
		}

		//표식 새기기 각성: 표식이 가득 찬 적이 죽으면 2칸 안 다른 적에게 옮겨 간다 (옮겨 간 표식은 다시 옮겨 가지 않는다)
		BloomBuffs.Mark mark = enemy.buff(BloomBuffs.Mark.class);
		int m = lvl(hero, MARK_CARVE);
		if (mark != null && awake(hero, MARK_CARVE) && m >= 0 && mark.stacks >= B2_MARK_MAX[m] && BloomRules.canSpread(mark)){
			Char to = nearestEnemy(enemy, B2_MARK_AWAKE_RANGE);
			if (to != null){
				BloomBuffs.Mark.add(to, mark.stacks, true);
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.markTransfer(enemy, to);
			}
		}
	}

	// =====================================================================
	// 도움 함수
	// =====================================================================

	//주인공에게서 먼 쪽으로 밀친다
	static void pushAway(Hero hero, Char enemy, int dist){
		if (!enemy.isAlive() || enemy.properties().contains(Char.Property.IMMOVABLE)) return;
		int dir = enemy.pos - hero.pos;
		Ballistica trajectory = new Ballistica(enemy.pos, enemy.pos + dir, Ballistica.MAGIC_BOLT);
		WandOfBlastWave.throwChar(enemy, trajectory, dist, true, false, hero);
	}

	//dist칸 안에서 가장 가까운 다른 적
	static Char nearestEnemy(Char from, int dist){
		Char best = null;
		int bestD = Integer.MAX_VALUE;
		for (Char c : Actor.chars()){
			if (c == from || !c.isAlive() || c.alignment != Char.Alignment.ENEMY) continue;
			int d = Dungeon.level.distance(from.pos, c.pos);
			if (d <= dist && d < bestD){
				best = c;
				bestD = d;
			}
		}
		return best;
	}
}
