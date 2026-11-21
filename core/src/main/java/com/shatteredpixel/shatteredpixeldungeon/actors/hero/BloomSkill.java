/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 2차 스킬 전용 특성 9개 (연쇄, 표식 폭발, 집중, 처형, 과충전, 충격, 서리 베기, 흘리기, 섬광 보법).
//스킬 = 검·단검·활 스킬, 지팡이 스킬, 검투사 필살기 (BloomEffects.beginSkill ~ endSkill).
//횟수·파생·마나 규칙은 BloomRules를 거친다.
public class BloomSkill {

	private static int lvl(Hero hero, BloomTalent t){
		return BloomEffects.lvl(hero, t);
	}

	private static boolean awake(Hero hero, BloomTalent t){
		return BloomTree.awakened(hero, t);
	}

	// =====================================================================
	// 스킬 한 번의 상태 (BloomEffects.SkillUse에 붙는 것)
	// =====================================================================

	public static class State {
		boolean focus;                                //집중: 대기·탐색 바로 다음 스킬
		boolean overcharge;                           //과충전: 마나가 가득 찬 채 시작
		final HashSet<Integer> executed = new HashSet<>(); //처형이 붙은 대상
		int burstTarget = -1;                         //표식 폭발로 표식을 터뜨린 대상
		int burstStacks = 0;
		boolean burstTransferred = false;
		boolean impactDone = false;
		boolean flash = false;                        //섬광 보법이 이번 스킬에 발동 (준비되었을 때만)
		boolean flashIn = false;                      //근거리 스킬: 떨어진 적에게 돌진해 들어갔다 (물러나지 않음)
	}

	static State state = null;

	//스킬 시작 (BloomEffects.beginSkill, 마나를 쓰기 직전)
	static void onSkillStart(Hero hero, boolean wasFullMana){
		State s = new State();
		Mana m = Mana.get(hero);

		//집중: 대기·탐색 바로 다음 스킬, 쿨타임 10턴
		if (m.focusReady && lvl(hero, FOCUS) >= 0 && hero.buff(BloomBuffs.FocusCooldown.class) == null){
			s.focus = true;
			Buff.affect(hero, BloomBuffs.FocusCooldown.class, B2_FOCUS_COOLDOWN);
		}
		m.focusReady = false;

		s.overcharge = wasFullMana && lvl(hero, OVERCHARGE) >= 0;
		if (s.overcharge) com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.overcharge(hero); //과충전: 시전할 때 마력 넘침
		state = s;
	}

	static void onSkillEnd(){
		state = null;
	}

	//원거리 스킬로 시작했다 (활 조준 사격: SwordSkill.perform, 지팡이 스킬: BloomEffects.beginStaffSkill)
	public static void markRanged(){
		Hero hero = Dungeon.hero;
		//섬광 보법: 준비되었을 때만 발동하고 쿨타임 10턴
		if (state != null && hero != null && lvl(hero, FLASH_STEP) >= 0
				&& hero.buff(BloomBuffs.FlashCooldown.class) == null){
			state.flash = true;
			Buff.affect(hero, BloomBuffs.FlashCooldown.class, B2_FLASH_COOLDOWN);
		}
	}

	//(결정) 섬광 보법 근거리: 떨어진 적에게 근거리 스킬을 쓰면 그 적 바로 앞 칸으로 돌진한 뒤 벤다.
	//돌진할 수 있는 거리는 물러나는 칸 수와 같다 (1·2레벨 1칸 → 2칸 떨어진 적, 3레벨 2칸 → 3칸 떨어진 적).
	//가는 길(직선)에 벽·적·낭떠러지·함정이 있으면 못 한다. 준비되었을 때만. 돌진할 칸, 못 하면 -1
	public static int dashInCell(Hero hero, Char enemy){
		int fl = lvl(hero, FLASH_STEP);
		if (fl < 0 || hero.buff(BloomBuffs.FlashCooldown.class) != null) return -1;
		if (hero.rooted || Dungeon.level.adjacent(hero.pos, enemy.pos)) return -1;
		if (Dungeon.level.distance(hero.pos, enemy.pos) > B2_FLASH_RANGE[fl] + 1) return -1;
		Ballistica b = new Ballistica(hero.pos, enemy.pos, Ballistica.PROJECTILE);
		if (b.collisionPos != enemy.pos) return -1;
		int land = -1;
		for (int i = 1; i < b.path.size(); i++){
			int c = b.path.get(i);
			if (c == enemy.pos) break;
			if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Actor.findChar(c) != null) return -1;
			land = c;
		}
		if (land == -1 || !Dungeon.level.adjacent(land, enemy.pos)) return -1;
		return land;
	}

	//근거리 스킬로 돌진했다 (SwordSkill.perform, beginSkill 뒤). 피해 증가·각성 기절은 원거리와 같다
	public static void markDashIn(){
		Hero hero = Dungeon.hero;
		if (state == null || hero == null || lvl(hero, FLASH_STEP) < 0) return;
		state.flash = true;
		state.flashIn = true;
		Buff.affect(hero, BloomBuffs.FlashCooldown.class, B2_FLASH_COOLDOWN);
	}

	// =====================================================================
	// 피해 배율 (스킬 타격마다)
	// =====================================================================

	static float hitBonus(Hero hero, Char enemy){
		State s = state;
		if (s == null) return 0;
		float bonus = 0;

		//집중
		int fo = lvl(hero, FOCUS);
		if (s.focus && fo >= 0) bonus += B2_FOCUS_DMG[fo];

		//처형
		int ex = lvl(hero, EXECUTE);
		if (ex >= 0 && enemy.HP <= enemy.HT * B2_EXECUTE_HP){
			bonus += B2_EXECUTE_DMG[ex];
			if (s.executed.add(enemy.id())){
				TalentEffects.execute(enemy); //붉은 섬광
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.slash(hero, enemy, 0xFF3030); //붉은 일격
			}
		}

		//과충전
		int oc = lvl(hero, OVERCHARGE);
		if (s.overcharge && oc >= 0) bonus += B2_OVERCHARGE_DMG[oc];

		//표식 폭발: 스킬 타격마다 표식 1개를 터뜨린다. 터질 때마다 1스택씩 사라진다.
		//(여러 번 때리는 스킬은 타격마다 하나씩, 스킬 한 번에 최대 B2_MULTIHIT_SKILL_CAP개)
		int mb = lvl(hero, MARK_BURST);
		BloomBuffs.Mark mark = enemy.buff(BloomBuffs.Mark.class);
		if (mb >= 0 && mark != null && mark.stacks > 0 && BloomRules.triggerOnHit(MARK_BURST)){
			bonus += B2_MARK_BURST_PER[mb];
			if (s.burstTarget != enemy.id()){
				s.burstTarget = enemy.id();
				s.burstStacks = 0;
			}
			s.burstStacks++;
			s.burstTransferred = mark.transferred;
			mark.stacks--;
			mark.refreshOverhead();
			com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.markPop(enemy); //표식 터짐
			if (mark.stacks <= 0) mark.detach();
		}

		//섬광 보법: 원거리 스킬 피해
		int fl = lvl(hero, FLASH_STEP);
		if (fl >= 0 && s.flash) bonus += B2_FLASH_DMG[fl];

		return bonus;
	}

	//원거리 스킬 (활 조준 사격, 지팡이 스킬). 검·단검 스킬과 필살기는 아니다.
	static boolean rangedSkill(){
		return (SwordSkill.active != null && SwordSkill.active.ranged) || BloomEffects.staffSkillWand != null;
	}

	//방어 무시 비율 (집중 각성)
	static float armorIgnore(Hero hero){
		State s = state;
		if (s != null && s.focus && awake(hero, FOCUS)) return B2_FOCUS_AWAKE_IGNORE;
		return 0;
	}

	// =====================================================================
	// 스킬 적중 시 효과 (피해가 정해진 뒤)
	// =====================================================================

	static void onSkillHit(final Hero hero, final Char enemy, int damage){
		State s = state;

		//서리 베기 (각성: 냉각 대신 혹한)
		int fs = lvl(hero, FROST_SLASH);
		if (fs >= 0 && BloomRules.triggerOnHit(FROST_SLASH)){
			if (awake(hero, FROST_SLASH)){
				final float turns = B2_FROST_TURNS[fs];
				//혹한은 피해가 들어간 뒤에 건다 (빙결 반응 순서)
				BloomAttack.afterHit(new Runnable() {
					@Override
					public void run() {
						if (!enemy.isAlive()) return;
						Buff.affect(enemy, Frostbite.class, turns);
						if (enemy.sprite != null) enemy.sprite.emitter().burst(SnowParticle.FACTORY, 8);
					}
				});
			} else {
				BloomEffects.chill(hero, enemy, B2_FROST_TURNS[fs]);
			}
		}

		//연쇄 (번진 피해는 파생)
		int ch = lvl(hero, CHAIN);
		if (ch >= 0 && BloomRules.triggerOnHit(CHAIN)) chain(hero, enemy, damage, ch);

		//과충전 각성: 대상 주변 3×3 충격파 (파생, 스킬 한 번에 한 번)
		if (s != null && s.overcharge && awake(hero, OVERCHARGE) && BloomRules.trigger(OVERCHARGE)){
			int dmg = Math.round(damage * B2_OVERCHARGE_AWAKE_DMG);
			for (int n : PathFinder.NEIGHBOURS8){
				Char c = Actor.findChar(enemy.pos + n);
				if (c != null && c != hero && c.alignment == Char.Alignment.ENEMY){
					BloomRules.derivedDamage(OVERCHARGE, c, dmg);
				}
			}
			com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.shockwave(enemy, 1); //대상 주변 3×3 충격파
		}

		//충격: 맞은 적을 밀친다 (스킬 한 번에 한 번, 피해가 들어간 뒤)
		int im = lvl(hero, IMPACT);
		if (im >= 0 && s != null && !s.impactDone && BloomRules.trigger(IMPACT)){
			s.impactDone = true;
			final int lv = im;
			final int hitDmg = damage;
			final boolean wake = awake(hero, IMPACT);
			BloomAttack.afterHit(new Runnable() {
				@Override
				public void run() {
					impact(hero, enemy, lv, hitDmg, wake);
				}
			});
		}

		//섬광 보법 각성: 지정한 대상 1턴 기절 (스킬 한 번에 한 번)
		if (awake(hero, FLASH_STEP) && lvl(hero, FLASH_STEP) >= 0 && s != null && s.flash
				&& BloomEffects.skillUse != null && BloomEffects.skillUse.target == enemy
				&& BloomRules.trigger(FLASH_STEP)){
			BloomAttack.afterHit(new Runnable() {
				@Override
				public void run() {
					if (enemy.isAlive()) Buff.affect(enemy, Paralysis.class, B2_FLASH_AWAKE_STUN);
				}
			});
		}
	}

	private static void chain(Hero hero, Char enemy, int damage, int level){
		int amt = Math.round(damage * B2_CHAIN_PCT[level]);
		if (amt <= 0) return;
		ArrayList<Char> candidates = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (c != enemy && c != hero && c.alignment == Char.Alignment.ENEMY && c.isAlive()
					&& Dungeon.level.distance(enemy.pos, c.pos) <= B2_CHAIN_RANGE
					&& Dungeon.level.heroFOV[c.pos]){
				candidates.add(c);
			}
		}
		int targets = awake(hero, CHAIN) ? B2_CHAIN_AWAKE_TARGETS : B2_CHAIN_TARGETS[level];
		for (int i = 0; i < targets && !candidates.isEmpty(); i++){
			Char other = Random.element(candidates);
			candidates.remove(other);
			if (other.sprite != null && enemy.sprite != null){
				com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.chain(enemy, other); //하늘색 빛줄기
			}
			BloomRules.derivedDamage(CHAIN, other, amt);
		}
	}

	//충격: 주인공 반대쪽으로 밀친다. 벽에 부딪히면 추가 피해(파생), 3레벨은 2턴 기절.
	//각성: 다른 적과 부딪히면 둘 다 2턴 기절.
	private static void impact(Hero hero, Char enemy, int lv, int hitDmg, boolean wake){
		if (!enemy.isAlive() || enemy.properties().contains(Char.Property.IMMOVABLE)) return;
		int dir = enemy.pos - hero.pos;
		if (dir == 0) return;
		Ballistica traj = new Ballistica(enemy.pos, enemy.pos + dir, Ballistica.WONT_STOP);
		int want = B2_IMPACT_PUSH[lv];
		if (enemy.properties().contains(Char.Property.BOSS)) want = (want + 1) / 2; //원작 밀침과 같이 보스는 절반

		int steps = 0;
		boolean wall = false;
		Char bumped = null;
		for (int i = 1; i < traj.path.size() && steps < want; i++){
			int next = traj.path.get(i);
			if (Dungeon.level.solid[next] || !Dungeon.level.passable[next] && !Dungeon.level.avoid[next]){
				wall = Dungeon.level.solid[next];
				break;
			}
			Char c = Actor.findChar(next);
			if (c != null){
				bumped = c;
				break;
			}
			steps++;
		}
		//밀려날 칸이 있으면 원작 밀침 (부딪힘 피해는 여기서 따로 준다)
		if (steps > 0){
			//원작 밀침은 보스를 절반만 밀어서, 위에서 이미 줄인 칸 수를 그대로 쓰도록 두 배로 넘긴다
			int power = enemy.properties().contains(Char.Property.BOSS) ? steps * 2 : steps;
			WandOfBlastWave.throwChar(enemy, new Ballistica(enemy.pos, traj.path.get(steps), Ballistica.WONT_STOP), power, true, false, hero);
		} else if (steps == 0 && !wall && bumped == null){
			return;
		}

		if (wall){
			BloomRules.derivedDamage(IMPACT, enemy, Math.max(1, Math.round(hitDmg * B2_IMPACT_WALL_DMG[lv])));
			if (lv >= 2 && enemy.isAlive()) Buff.affect(enemy, Paralysis.class, B2_IMPACT_STUN);
			PixelScene.shake(1, 0.15f);
			com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.impact(enemy.pos); //벽 충돌
		} else if (bumped != null && wake && bumped.alignment == Char.Alignment.ENEMY){
			if (enemy.isAlive()) Buff.affect(enemy, Paralysis.class, B2_IMPACT_STUN);
			if (bumped.isAlive()) Buff.affect(bumped, Paralysis.class, B2_IMPACT_STUN);
			PixelScene.shake(1, 0.15f);
			com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.impact(bumped.pos); //적끼리 충돌
		}
	}

	// =====================================================================
	// 스킬이 끝난 뒤 (BloomEffects.endSkill)
	// =====================================================================

	static void endSkill(Hero hero, BloomEffects.SkillUse u){
		State s = state;
		//처형 3레벨: 처형으로 처치하면 비용 50% 반환 (비용을 넘지 않는다)
		if (s != null && u.target != null && !u.target.isAlive() && s.executed.contains(u.target.id()) && lvl(hero, EXECUTE) >= 2){
			int amt = BloomRules.refund(hero, Math.round(u.manaSpent * B2_EXECUTE_REFUND));
			if (amt > 0) TalentEffects.manaRefund(hero, u.target, amt); //푸른 마탄이 주인공에게
		}

		//섬광 보법: 원거리 스킬을 쓴 뒤 대상 반대쪽으로 1~2칸 물러난다 (물러날 수 있을 때만)
		int fl = lvl(hero, FLASH_STEP);
		if (s != null && s.flash && !s.flashIn && fl >= 0 && u.target != null && hero.isAlive()){
			int cell = retreatCell(hero, u.target.pos, B2_FLASH_RANGE[fl]);
			if (cell != hero.pos) dash(hero, cell);
		}
	}

	// =====================================================================
	// 처치 (BloomRules.onDeath)
	// =====================================================================

	static void onKill(Hero hero, Char enemy, boolean byDerived){
		State s = state;
		if (s == null || BloomRules.kind() != BloomRules.Kind.SKILL) return;

		//처형 각성: 처형으로 처치하면 주변 2칸 적 2턴 공포
		if (!byDerived && s.executed.contains(enemy.id()) && awake(hero, EXECUTE) && BloomRules.trigger(EXECUTE)){
			for (Char c : Actor.chars()){
				if (c != enemy && c.isAlive() && c.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance(enemy.pos, c.pos) <= B2_EXECUTE_AWAKE_RANGE){
					Buff.affect(c, Terror.class, B2_EXECUTE_AWAKE_FEAR).object = hero.id();
				}
			}
		}

		//표식 폭발 각성: 표식을 가진 채(또는 이번 스킬로 터뜨린 채) 죽으면 주변 2칸 적에게 표식이 옮겨 간다
		if (awake(hero, MARK_BURST) && lvl(hero, MARK_BURST) >= 0){
			int stacks = 0;
			boolean transferred = false;
			BloomBuffs.Mark mark = enemy.buff(BloomBuffs.Mark.class);
			//남은 표식 + 이번 스킬로 터뜨린 표식 (터지기 전 개수)
			if (mark != null){
				stacks = mark.stacks;
				transferred = mark.transferred;
			}
			if (s.burstTarget == enemy.id()){
				stacks += s.burstStacks;
				transferred = transferred || s.burstTransferred;
			}
			if (stacks > 0 && !transferred){
				for (Char c : Actor.chars()){
					if (c != enemy && c.isAlive() && c.alignment == Char.Alignment.ENEMY
							&& Dungeon.level.distance(enemy.pos, c.pos) <= B2_MARK_BURST_AWAKE_RANGE){
						BloomBuffs.Mark.add(c, stacks, true);
						com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.markTransfer(enemy, c);
					}
				}
			}
		}
	}

	// =====================================================================
	// 흘리기 각성: 흘린 공격의 적에게 반격 (Hero.defenseProc → BloomEffects.onHeroDefense)
	// =====================================================================

	static void parryCounter(Hero hero, Char enemy){
		if (enemy == null || !enemy.isAlive() || !awake(hero, PARRY)) return;
		int dmg = hero.damageRoll();
		BloomRules.derivedDamage(PARRY, enemy, dmg);
		com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx.slash(hero, enemy, 0xFFFFFF); //베기 궤적
	}

	// =====================================================================
	// 섬광 보법: 원거리 스킬 뒤 물러나기
	// =====================================================================

	//대상 반대쪽으로 최대 n칸. 막히면(벽·적·낭떠러지·함정 등) 그 앞에서 멈춘다. 못 움직이면 지금 칸.
	static int retreatCell(Hero hero, int from, int n){
		int w = Dungeon.level.width();
		int dx = Integer.signum(hero.pos % w - from % w);
		int dy = Integer.signum(hero.pos / w - from / w);
		if (dx == 0 && dy == 0) return hero.pos;
		int cell = hero.pos;
		for (int i = 0; i < n; i++){
			int x = cell % w + dx, y = cell / w + dy;
			if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) break;
			int next = x + y * w;
			if (!Dungeon.level.passable[next] || Dungeon.level.avoid[next] || Actor.findChar(next) != null) break;
			cell = next;
		}
		return cell;
	}

	public static void dash(Hero hero, int cell){
		int from = hero.pos;
		hero.sprite.interruptMotion();
		hero.move(cell, false);
		hero.sprite.place(cell);
		Dungeon.level.occupyCell(hero);
		Dungeon.observe();
		TalentEffects.flashStep(hero, from); //임시 잔상 (E단계)
	}
}
