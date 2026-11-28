/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;
import static com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent.*;

//Maze Tower: 섬광 보법 (전사 검 전용 형태, 구현 지시서 C17).
//검 스킬(무형검 포함)을 떨어진 적에게 쓰면 그 적 바로 앞 칸으로 돌진해 쓴다. 레벨은 쓸 수 있는 거리를 늘린다.
//각성: 돌진해 쓴 스킬로 적을 쓰러뜨리면, 마나 없이 다른 적에게 한 번 더 돌진해 그 스킬을 쓴다. 쓰러뜨리는 동안 계속 이어진다.
public class BloomSkill {

	private static int lvl(Hero hero, BloomTalent t){
		return BloomEffects.lvl(hero, t);
	}

	private static boolean awake(Hero hero, BloomTalent t){
		return BloomTree.awakened(hero, t);
	}

	// =====================================================================
	// 스킬 한 번의 상태
	// =====================================================================

	public static class State {
		boolean flash = false; //이번 스킬이 돌진해 쓴 스킬이다
	}

	static State state = null;

	//스킬 시작 (BloomEffects.beginSkill, 마나를 쓰기 직전)
	static void onSkillStart(Hero hero){
		state = new State();
		fallingId = -1;
	}

	//돌진할 칸. 못 하면 -1. 적까지 C17_FLASH_RANGE칸 안, 가는 길(직선)에 벽·적·낭떠러지·함정이 없어야 한다.
	//(결정) 돌진한 뒤 C17_FLASH_COOLDOWN턴 동안은 돌진하지 못한다
	public static int dashInCell(Hero hero, Char enemy){
		if (hero.buff(BloomBuffs.FlashCooldown.class) != null) return -1;
		return dashCell(hero, enemy);
	}

	//쿨타임을 보지 않는 돌진 칸 (각성의 이어 쓰기)
	private static int dashCell(Hero hero, Char enemy){
		int fl = lvl(hero, FLASH_STEP);
		if (fl < 0 || enemy == null) return -1;
		if (hero.rooted || Dungeon.level.adjacent(hero.pos, enemy.pos)) return -1;
		if (Dungeon.level.distance(hero.pos, enemy.pos) > C17_FLASH_RANGE[fl]) return -1;
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

	//검 스킬로 돌진했다 (SwordSkill.launch, beginSkill 뒤)
	public static void markDashIn(){
		Hero hero = Dungeon.hero;
		if (state == null || hero == null || lvl(hero, FLASH_STEP) < 0) return;
		state.flash = true;
		Buff.affect(hero, BloomBuffs.FlashCooldown.class, C17_FLASH_COOLDOWN);
	}

	//(결정) 스킬에 맞은 대상이 밀치기(무거운 일격 등)로 구덩이에 떨어질 참이면, 각성의 "쓰러뜨렸다"로 친다.
	//밀리는 연출이 스킬이 끝난 뒤에야 끝나 실제 낙사는 그 뒤에 일어나므로, 밀칠 때 미리 표시해 둔다.
	private static int fallingId = -1;

	public static void markFalling(Char ch){
		if (ch != null) fallingId = ch.id();
	}

	//(지금 사양에는 돌진 피해 증가가 없다. 예전 사양의 자리만 남겨 둔다)
	static float hitBonus(Hero hero, Char enemy){
		return 0;
	}

	static void onSkillHit(final Hero hero, final Char enemy){
	}

	static void endSkill(Hero hero, BloomEffects.SkillUse u){
		state = null;
	}

	// =====================================================================
	// 각성: 이어서 돌진할 다음 적
	// =====================================================================

	public static class Chain {
		public Char target;
		public int dashIn; //-1이면 이미 붙어 있어 돌진 없이 쓴다
	}

	//스킬이 끝날 때(SwordSkill.finish, endSkill 전) 부른다. 이어서 쓸 적이 있으면 돌려준다.
	public static Chain chainTarget(Hero hero){
		State s = state;
		BloomEffects.SkillUse u = BloomEffects.skillUse;
		int falling = fallingId;
		fallingId = -1;
		if (s == null || !s.flash || u == null || u.target == null) return null;
		boolean down = !u.target.isAlive() || u.target.id() == falling;
		if (!down || !hero.isAlive()) return null;
		if (lvl(hero, FLASH_STEP) < 0 || !awake(hero, FLASH_STEP)) return null;
		if (hero.paralysed > 0) return null;
		Chain best = null;
		int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (!m.isAlive() || m.id() == falling || m.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[m.pos]) continue;
			if (m.invisible > 0 || hero.isCharmedBy(m)) continue;
			int d = Dungeon.level.distance(hero.pos, m.pos);
			if (d >= bestD) continue;
			int dash = -1;
			if (!Dungeon.level.adjacent(hero.pos, m.pos)){
				dash = dashCell(hero, m); //(결정) 각성의 이어 쓰기는 쿨타임과 상관없다
				if (dash == -1) continue;
			}
			best = new Chain();
			best.target = m;
			best.dashIn = dash;
			bestD = d;
		}
		if (best != null) BloomRules.debug("flash chain -> " + best.target.name() + (best.dashIn == -1 ? " (adjacent)" : ""));
		return best;
	}

	// =====================================================================
	// 돌진
	// =====================================================================

	public static void dash(Hero hero, int cell){
		int from = hero.pos;
		hero.sprite.interruptMotion();
		hero.move(cell, false);
		hero.sprite.place(cell);
		Dungeon.level.occupyCell(hero);
		Dungeon.observe();
		TalentEffects.flashStep(hero, from);
	}
}
