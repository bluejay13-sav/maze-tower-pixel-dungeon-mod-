/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.LanternLight;
import com.shatteredpixel.shatteredpixeldungeon.items.FullBloomCore;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeBossSprites;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//Maze Tower (I): 10층 그림자 간수 (새로 짠 보스). 몸이 비어 옷과 그림자만 떠다니는 감옥의 형리.
//1단계(감방 미로): 등불을 들고 순찰한다. 등불은 바라보는 방향의 부채꼴(I2_LIGHT_HALF, I2_LIGHT_RANGE)이고 그 빛이 곧 간수의 눈이다
//  (Level.updateFieldOfView → mazeMaskFov). 빛이 닿는 칸은 늘 보이고(LanternLight showAll), 다음 턴에 비출 칸도 칸 가운데의 점으로 미리 보인다.
//  다음 턴의 자리와 방향은 자기 턴이 끝날 때 정해 두고, 다음 턴에는 정해 둔 대로만 움직인다(예고 = 실제).
//  1단계에서는 직접 공격하지 않는다 (가두기·기습·점호는 I3·I4).
//체력이 I1_PHASE2_AT 아래로 내려가면 대봉인실(2단계)로 (WardenBossLevel.toArena). 2단계 기술은 I6~I9에서 (지금은 원작처럼 다가와 때린다).
//속박·마비·잠·공포를 받지 않는다.
public class ShadowWarden extends Mob implements com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.PhaseMarks {

	//Maze Tower (사용자): 보스 체력 바 페이즈 눈금 — 2단계 대봉인실(I1_PHASE2_AT)
	@Override public float[] phaseMarks(){ return new float[]{ MazeTower.I1_PHASE2_AT }; }
	@Override public int turnsLeft(){ return -1; }

	{
		spriteClass = WardenSprite.class;

		HP = HT = MazeTower.I_WARDEN_HP;
		EXP = 20;
		defenseSkill = 15;
		viewDistance = MazeTower.I2_LIGHT_RANGE;

		properties.add(Property.BOSS);
		flying = true;

		immunities.add( Roots.class );
		immunities.add( Paralysis.class );
		immunities.add( Sleep.class );
		immunities.add( MagicalSleep.class );
		immunities.add( Terror.class );
		immunities.add( Dread.class );

		state = WANDERING;
	}

	public int phase = 1;
	public int facing = 2;               //LanternCone 8방향 번호 (2 = 아래)
	private int nextPos = -1;            //다음 턴의 자리 (예고)
	private int nextFacing = 2;          //다음 턴의 방향 (예고)
	private int waypoint = -1, lastWaypoint = -1, waitTurns = 0;

	private LanternLight light, preview;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 6, 12 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 10;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 5);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return phase >= 2 && super.canAttack(enemy);
	}

	// ===== 등불 =====

	//from에서 dir 방향으로 비추는 칸 (벽에 가리는 원작 시야 + 부채꼴)
	public static boolean[] cone( int from, int dir ){
		int w = Dungeon.level.width();
		boolean[] fov = new boolean[Dungeon.level.length()];
		if (from < 0) return fov;
		ShadowCaster.castShadow( from % w, from / w, w, fov, Dungeon.level.losBlocking, MazeTower.I2_LIGHT_RANGE );
		LanternCone.mask( from, dir, fov, MazeTower.I2_LIGHT_HALF, MazeTower.I2_LIGHT_RANGE );
		return fov;
	}

	//Level.updateFieldOfView 끝에서: 1단계에는 등불 부채꼴 밖을 보지 못한다
	public void mazeMaskFov( boolean[] fov ){
		if (phase == 1) LanternCone.mask( pos, facing, fov, MazeTower.I2_LIGHT_HALF, MazeTower.I2_LIGHT_RANGE );
	}

	//지금 비추는 칸에 주인공이 있는지 (I3 가두기에서 쓴다)
	public boolean litCell( int cell ){
		return phase == 1 && cone(pos, facing)[cell];
	}

	public void updateLights(){
		if (phase != 1 || sprite == null || Dungeon.level == null){ clearLights(); return; }
		if (light == null || light.parent == null){
			light = new LanternLight( true, 0, 0.6f );
			GameScene.groupOverFog( light );
		}
		if (MazeTower.I2_PREVIEW && (preview == null || preview.parent == null)){
			preview = new LanternLight( true, 5, 1.2f );
			GameScene.groupOverFog( preview );
		}
		HashMap<Integer, Float> now = new HashMap<>(), next = new HashMap<>();
		boolean[] c1 = cone(pos, facing);
		boolean[] c2 = cone(nextPos >= 0 ? nextPos : pos, nextFacing);
		for (int i = 0; i < c1.length; i++){
			if (c1[i] && !Dungeon.level.solid[i]) now.put(i, MazeTower.I2_LIGHT_ALPHA);
			if (c2[i] && !Dungeon.level.solid[i]) next.put(i, MazeTower.I2_PREVIEW_ALPHA);
		}
		light.set(now, new HashSet<Integer>());
		if (preview != null) preview.set(next, new HashSet<Integer>());
		int dx = LanternCone.DIRS[facing][0];
		if (dx != 0) sprite.turnTo(pos, pos + dx);
	}

	public void clearLights(){
		if (light != null){ light.fadeAway(); light = null; }
		if (preview != null){ preview.fadeAway(); preview = null; }
	}

	// ===== 순찰 (1단계) =====

	@Override
	protected boolean act() {
		if (phase != 1 || !(Dungeon.level instanceof WardenBossLevel)){
			clearLights();
			if (state != HUNTING && state != WANDERING) state = HUNTING;
			if (phase >= 2 && Dungeon.level instanceof WardenBossLevel){
				Boolean r = actPhase2();
				if (r != null) return r;
			}
			return super.act();
		}
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}

		//(I3) 주인공이 보이는 빛 안에서 턴을 마쳤으면 붙잡아 감방에 가둔다 (기습당한 직후 한 턴은 여유)
		if (captureCheck()){
			updateLights();
			return false;
		}

		boolean moved = false;
		int oldPos = pos;
		//정해 둔 대로만 움직인다
		if (nextPos >= 0 && nextPos != pos){
			if (Actor.findChar(nextPos) == null && Dungeon.level.passable[nextPos]){
				move(nextPos);
				moved = true;
			}
		}
		facing = nextFacing;
		Dungeon.level.updateFieldOfView(this, fieldOfView);

		planNext();
		updateLights();

		if (moved){
			spend( 1 / speed() );
			return moveSprite( oldPos, pos );
		}
		spend( TICK );
		return true;
	}

	// ===== 가두기 (I3) =====

	private int graceTurns = 0;   //기습당한 직후 한 턴은 비추기만 하고 가두지 않는다

	//붙잡았으면(사슬 연출이 끝난 뒤 이어짐) true: 그 턴은 움직이지 않는다
	private boolean captureCheck(){
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h = Dungeon.hero;
		//(사용자) 문이 열리거나 부서진 감방 안은 안전하지 않다: 잠긴 감방 안에 있을 때만 가두지 않는다
		WardenBossLevel.PrisonCell inCell = h == null ? null : lv.cellOf(h.pos);
		boolean lit = h != null && h.isAlive() && h.invisible <= 0 && cone(pos, facing)[h.pos]
				&& (inCell == null || !lv.doorLocked(inCell));
		if (graceTurns > 0){ graceTurns--; return false; }
		if (!lit) return false;

		//(사용자) 사슬이 날아가 주인공을 휘감아 간수 앞까지 끌어당긴 뒤, 문이 잠긴 가장 가까운 감방에 가둔다
		h.interrupt();
		yell( Messages.get(this, "capture_yell") );
		final int pullTo = pullCell(h);
		if (sprite == null || h.sprite == null || sprite.parent == null){
			chainHit(h, pullTo);
			return false;
		}
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHAINS);
		sprite.turnTo(pos, h.pos);
		sprite.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Chains(sprite.center(), h.sprite.destinationCenter(),
				com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.CHAIN, new com.watabou.utils.Callback() {
			@Override
			public void call() {
				chainHit(h, pullTo);
				next();
			}
		}));
		spend( TICK );
		return true;
	}

	//사슬이 끌어당길 칸: 간수 쪽에서 주인공 쪽으로 가며 처음 나오는 빈칸 (붙어 있거나 빈칸이 없으면 그 자리)
	private int pullCell( Char h ){
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica b =
				new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos, h.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
		if (b.collisionPos != h.pos || b.dist < 2) return h.pos;
		for (int c : b.subPath(1, b.dist)){
			if (c == h.pos) break;
			if (!Dungeon.level.solid[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null) return c;
		}
		return h.pos;
	}

	//사슬이 닿았다: 피해, 끌어당김, 그다음 감방으로
	private void chainHit( final com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h, final int pullTo ){
		chainPull(h, pullTo, MazeTower.I3_CAPTURE_MIN, MazeTower.I3_CAPTURE_MAX, "capture_kill", true);
	}

	//사슬이 주인공에게 닿았다: 피해, 간수 앞까지 끌어당김, (1단계면) 그다음 감방으로
	private void chainPull( final com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h, final int pullTo,
							int min, int max, String killKey, final boolean thenImprison ){
		if (!h.isAlive()) return;
		int dmg = Random.NormalIntRange(min, max);
		h.damage(dmg, this);
		if (!h.isAlive()){
			Dungeon.fail(this);
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get(this, killKey) );
			return;
		}
		if (pullTo != h.pos && h.sprite != null){
			Actor.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Pushing(h, h.pos, pullTo, new com.watabou.utils.Callback() {
				@Override
				public void call() {
					h.pos = pullTo;
					h.sprite.place(pullTo);
					Dungeon.level.occupyCell(h);
					if (thenImprison) imprison(h);
					else { pulledRoot(h); Dungeon.observe(); GameScene.updateFog(); }
				}
			}));
		} else if (thenImprison){
			imprison(h);
		} else {
			pulledRoot(h);
		}
	}

	//(사용자) 2단계 끌어내기에 맞으면 원작 속박 I8_PULL_ROOT턴
	private void pulledRoot( Char h ){
		if (h.isAlive()) com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(h, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, MazeTower.I8_PULL_ROOT);
	}

	private void imprison( com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h ){
		if (!h.isAlive() || !(Dungeon.level instanceof WardenBossLevel)) return;
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		WardenBossLevel.PrisonCell pc = lv.nearestLockedCell(h.pos);
		int to = pc == null ? -1 : lv.freeInside(pc);
		if (to == -1){
			//잠긴 감방이 하나도 남지 않았으면 피해만 준다
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w( Messages.get(this, "capture_nocell") );
			Dungeon.observe();
			GameScene.updateFog();
			return;
		}
		com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation.appear(h, to);
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(h, Imprisoned.class);
		Dungeon.observe();
		GameScene.updateFog();
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get(this, "captured") );
	}

	//(I3) 잠긴 감방 안에 있는 동안 턴마다 피해. 문이 열리거나 부서져 나가면 풀린다
	public static class Imprisoned extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.NEGATIVE; announced = true; }

		@Override
		public boolean act() {
			WardenBossLevel.PrisonCell pc = Dungeon.level instanceof WardenBossLevel ? ((WardenBossLevel) Dungeon.level).cellOf(target.pos) : null;
			if (pc == null || !((WardenBossLevel) Dungeon.level).doorLocked(pc)){
				detach();
				return true;
			}
			target.damage(MazeTower.I3_CELL_DMG, this);
			if (target == Dungeon.hero && !target.isAlive()){
				Dungeon.fail(ShadowWarden.class);
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get(ShadowWarden.class, "cell_kill") );
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() { return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.LOCKED_FLOOR; }

		@Override
		public String desc() { return Messages.get(this, "desc", MazeTower.I3_CELL_DMG); }
	}

	//다음 턴의 자리와 방향을 정한다 (예고)
	// ===== 2단계: 한 턴에 기술 하나 (I7·I8·I9) =====
	//기술을 썼으면 act의 결과(true/false), 원작 몬스터처럼 다가가 때리면 null

	public int pullCD = MazeTower.I8_PULL_FIRST, netCD = MazeTower.I8_NET_FIRST, sealCD = MazeTower.I9_SEAL_CD, sealLeft = 0;
	private int[] pullCells = null;   //끌어내기 조준선 (간수 다음 칸부터 벽 앞까지)
	private int[] netCells = null;    //사슬 그물이 덮는 칸
	private int[] netSegs = null;     //사슬 그물 줄 그림 (시작 칸, 끝 칸) 쌍

	private Boolean actPhase2(){
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h = Dungeon.hero;
		boolean sentenced = h.buff(Sentence.class) != null;

		if (pullCD > 0) pullCD--;
		if (netCD > 0) netCD--;
		if (sentenceCD > 0 && !sentenced) sentenceCD--;
		//(I9, 결정) 대봉인의 쿨타임은 안전지대가 하나라도 생긴 뒤부터 돈다
		if (sealLeft <= 0 && sealCD > 0 && !lv.safeCells.isEmpty()) sealCD--;

		//1) 대봉인을 모으는 중: 다른 행동을 하지 않는다
		if (sealLeft > 0){
			sealLeft--;
			if (sealLeft <= 0) releaseSeal();
			syncFx();
			spend(TICK);
			return true;
		}
		//2) 지난 턴에 예고한 기술을 마저 쓴다
		if (netCells != null){
			fireNet();
			spend(TICK);
			return true;
		}
		if (pullCells != null){
			return firePull();
		}

		//3) 새 기술 하나 (대봉인 > 선고 > 끌어내기 > 사슬 그물)
		if (sealCD <= 0 && !lv.safeCells.isEmpty()){
			startSeal();
			spend(TICK);
			return true;
		}
		if (sentenceCD <= 0 && !sentenced && castSentence()){
			sentenceCD = MazeTower.I7_SENTENCE_CD;
			spend(TICK);
			return true;
		}
		//주인공을 겨누는 기술: 투명이면 쓰지 않고(은신 존중), "걷지 마라" 동안에도 쓰지 않는다(공정함)
		Sentence s = h.buff(Sentence.class);
		boolean canAim = h.isAlive() && h.invisible <= 0 && (s == null || s.kind != Sentence.MOVE);
		if (canAim && pullCD <= 0 && startPull(h)){
			pullCD = MazeTower.I8_PULL_CD;
			spend(TICK);
			return true;
		}
		if (canAim && netCD <= 0 && startNet(lv, h)){
			netCD = MazeTower.I8_NET_CD;
			spend(TICK);
			return true;
		}
		return null;
	}

	// ----- 끌어내기 (I8) -----

	private boolean startPull( Char h ){
		if (Dungeon.level.distance(pos, h.pos) < MazeTower.I8_PULL_MIN_DIST) return false;
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica aim =
				new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos, h.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE);
		if (aim.collisionPos != h.pos) return false;
		//조준선은 주인공을 지나 벽 앞까지 (옆으로 비켜서야 피한다)
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica line =
				new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos, h.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_SOLID);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : line.subPath(1, line.dist)){
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}
		if (cells.isEmpty()) return false;
		pullCells = new int[cells.size()];
		for (int i = 0; i < pullCells.length; i++) pullCells[i] = cells.get(i);
		if (sprite != null) sprite.turnTo(pos, h.pos);
		yell( Messages.get(this, "pull_yell") );
		syncFx();
		return true;
	}

	//예고한 줄을 따라 사슬이 날아간다. 줄 위의 첫 대상이 주인공이면 피해를 주고 간수 앞까지 끌어당긴다
	private boolean firePull(){
		final int[] cells = pullCells;
		pullCells = null;
		syncFx();
		int end = cells[cells.length - 1];
		Char hit = null;
		int hitIdx = -1;
		for (int i = 0; i < cells.length; i++){
			Char ch = Actor.findChar(cells[i]);
			if (ch != null && ch != this){ hit = ch; hitIdx = i; end = cells[i]; break; }
		}
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHAINS);
		if (hit != Dungeon.hero){
			//빗나감: 사슬만 날아가 줄 끝(또는 가로막은 것)에 부딪힌다
			if (sprite != null && sprite.parent != null){
				sprite.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Chains(sprite.center(),
						com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(end),
						com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.CHAIN, null).speed(MazeTower.I8_CHAIN_SPEED));
			}
			spend(TICK);
			return true;
		}
		final com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h = Dungeon.hero;
		int to = h.pos;
		for (int i = 0; i < hitIdx; i++){
			if (!Dungeon.level.solid[cells[i]] && !Dungeon.level.pit[cells[i]] && Actor.findChar(cells[i]) == null){ to = cells[i]; break; }
		}
		final int pullTo = to;
		h.interrupt();
		if (sprite == null || h.sprite == null || sprite.parent == null){
			chainPull(h, pullTo, MazeTower.I8_PULL_DMG_MIN, MazeTower.I8_PULL_DMG_MAX, "pull_kill", false);
			spend(TICK);
			return true;
		}
		sprite.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Chains(sprite.center(), h.sprite.destinationCenter(),
				com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.CHAIN, new com.watabou.utils.Callback() {
			@Override
			public void call() {
				chainPull(h, pullTo, MazeTower.I8_PULL_DMG_MIN, MazeTower.I8_PULL_DMG_MAX, "pull_kill", false);
				next();
			}
		}).speed(MazeTower.I8_CHAIN_SPEED));
		spend(TICK);
		return false;
	}

	// ----- 사슬 그물 (I8) -----

	//(사용자) 방을 벽에서 벽까지 가로지르는 그물: 가로줄과 세로줄을 함께 모두 I8_NET_LINES개.
	//주인공이 선 가로줄·세로줄은 늘 들어가고, 같은 방향의 줄 사이에는 1칸 이상 비운다 (그래서 대각선 한 걸음이면 피할 칸이 있다)
	private boolean startNet( WardenBossLevel lv, Char h ){
		int w = Dungeon.level.width();
		int hx = h.pos % w, hy = h.pos / w;
		if (!lv.isArenaFloor(h.pos)) return false;
		int total = MazeTower.I8_NET_LINES;
		int nRows = total / 2 + (Random.Int(2) == 0 ? 0 : total % 2), nCols = total - nRows;
		ArrayList<Integer> rows = pickLines(WardenBossLevel.AY0, WardenBossLevel.AY1, hy, nRows);
		ArrayList<Integer> cols = pickLines(WardenBossLevel.AX0, WardenBossLevel.AX1, hx, nCols);

		java.util.LinkedHashSet<Integer> cells = new java.util.LinkedHashSet<>();
		ArrayList<Integer> segs = new ArrayList<>();
		for (int y : rows){
			segs.add(WardenBossLevel.AX0 + y * w); segs.add(WardenBossLevel.AX1 + y * w);
			for (int x = WardenBossLevel.AX0; x <= WardenBossLevel.AX1; x++) if (lv.isArenaFloor(x + y * w)) cells.add(x + y * w);
		}
		for (int x : cols){
			segs.add(x + WardenBossLevel.AY0 * w); segs.add(x + WardenBossLevel.AY1 * w);
			for (int y = WardenBossLevel.AY0; y <= WardenBossLevel.AY1; y++) if (lv.isArenaFloor(x + y * w)) cells.add(x + y * w);
		}
		if (cells.isEmpty()) return false;
		netCells = toArray(new ArrayList<>(cells));
		netSegs = toArray(segs);
		yell( Messages.get(this, "net_yell") );
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHAINS, 0.7f, 0.8f);
		syncFx();
		return true;
	}

	//줄이 팽팽해진다: 줄 위의 주인공·아군에게 피해 + 속박 (간수 편인 몬스터는 다치지 않는다)
	private void fireNet(){
		int[] cells = netCells, segs = netSegs;
		netCells = null; netSegs = null;
		syncFx();
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHAINS);
		if (sprite != null && sprite.parent != null && segs != null){
			for (int i = 0; i + 1 < segs.length; i += 2){
				com.watabou.utils.PointF[] p = wallToWall(segs[i], segs[i + 1]);
				sprite.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Chains(p[0], p[1],
						com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.CHAIN, null).speed(MazeTower.I8_CHAIN_SPEED));
			}
		}
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || ch.alignment == Alignment.ENEMY || !ch.isAlive()) continue;
			if (ch instanceof ChainRing || ch instanceof SealCage) continue;
			ch.damage(Random.NormalIntRange(MazeTower.I8_NET_DMG_MIN, MazeTower.I8_NET_DMG_MAX), this);
			if (ch.isAlive()){
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class, MazeTower.I8_NET_CRIPPLE); //(사용자) 그물은 속박 없이 느려짐만
				if (ch.sprite != null) ch.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE, Messages.get(this, "net_status"));
			} else if (ch == Dungeon.hero){
				Dungeon.fail(this);
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get(this, "net_kill") );
			}
		}
	}

	//lo~hi 사이에서 must를 넣고 n개를 고른다. 고른 줄끼리는 2 이상 떨어진다
	private static ArrayList<Integer> pickLines( int lo, int hi, int must, int n ){
		ArrayList<Integer> picks = new ArrayList<>();
		if (n <= 0) return picks;
		picks.add(must);
		ArrayList<Integer> cand = new ArrayList<>();
		for (int i = lo; i <= hi; i++) cand.add(i);
		Random.shuffle(cand);
		for (int c : cand){
			if (picks.size() >= n) break;
			boolean ok = true;
			for (int p : picks) if (Math.abs(p - c) < 2) ok = false;
			if (ok) picks.add(c);
		}
		return picks;
	}

	//줄의 두 끝 칸 → 벽에 닿는 두 점 (칸 가장자리까지 늘린다)
	public static com.watabou.utils.PointF[] wallToWall( int a, int b ){
		int w = Dungeon.level.width(), S = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.SIZE;
		int ax = a % w, ay = a / w, bx = b % w, by = b / w;
		if (ay == by){
			return new com.watabou.utils.PointF[]{ new com.watabou.utils.PointF(Math.min(ax, bx) * S, (ay + 0.5f) * S),
					new com.watabou.utils.PointF((Math.max(ax, bx) + 1) * S, (ay + 0.5f) * S) };
		}
		return new com.watabou.utils.PointF[]{ new com.watabou.utils.PointF((ax + 0.5f) * S, Math.min(ay, by) * S),
				new com.watabou.utils.PointF((ax + 0.5f) * S, (Math.max(ay, by) + 1) * S) };
	}

	private static int[] toArray( ArrayList<Integer> l ){
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) a[i] = l.get(i);
		return a;
	}

	// ----- 대봉인 (I9) -----

	//열쇠를 제 가슴에 꽂고 돌린다. 남은 턴이 다 되면 안전지대 밖의 주인공은 쓰러진다
	private void startSeal(){
		sealLeft = MazeTower.I9_SEAL_CHARGE;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(this, SealCharge.class);
		yell( Messages.get(this, "seal_yell") );
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w( Messages.get(this, "seal_start", MazeTower.I9_SEAL_CHARGE) );
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.UNLOCK, 1f, 0.6f);
		Dungeon.hero.interrupt();
		syncFx();
	}

	private void releaseSeal(){
		sealCD = MazeTower.I9_SEAL_CD;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(this, SealCharge.class);
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h = Dungeon.hero;
		GameScene.flash(0x3A1840);
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.BLAST, 1f, 0.7f);
		com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(1, 0.2f);
		if (h.isAlive() && !lv.isSafe(h.pos)){
			if (h.sprite != null) h.sprite.flash();
			h.die(this);
			if (!h.isAlive()){
				Dungeon.fail(this);
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get(this, "seal_kill") );
			}
		} else if (h.isAlive()){
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p( Messages.get(this, "seal_survived") );
		}
	}

	//(I9) 이식 마법봉에 맞으면 모으던 대봉인이 끊긴다 (WandOfTransfusion.onZap)
	public void onTransfusion(){
		if (sealLeft <= 0) return;
		sealLeft = 0;
		sealCD = MazeTower.I9_SEAL_CD;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(this, SealCharge.class);
		if (sprite != null) sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE, Messages.get(this, "seal_broken_status"));
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p( Messages.get(this, "seal_broken") );
		syncFx();
	}

	//대봉인 남은 턴 (보스 체력바 밑 아이콘 + 숫자)
	public static class SealCharge extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.NEUTRAL; announced = false; }
		@Override public boolean act(){
			if (!(target instanceof ShadowWarden) || ((ShadowWarden) target).sealLeft <= 0) detach();
			spend(TICK);
			return true;
		}
		@Override public int icon(){ return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.TIME; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.85f, 0.69f, 0.29f); }
		@Override public String iconTextDisplay(){ return target instanceof ShadowWarden ? Integer.toString(((ShadowWarden) target).sealLeft) : ""; }
		@Override public String desc(){ return Messages.get(this, "desc", target instanceof ShadowWarden ? ((ShadowWarden) target).sealLeft : 0); }
	}

	// ----- 예고 그림 -----

	private com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.ChainLine pullFx;
	private ArrayList<com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.ChainLine> netFx = new ArrayList<>();
	private com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.SealFloor sealFx;
	private com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.SafeZone safeFx;
	private com.watabou.noosa.particles.Emitter sealEmitter;

	private static final int PULL_COLOR = 0xE0B860, NET_COLOR = 0xB4C4D8;

	//저장된 상태에 맞게 예고 그림을 다시 그린다 (불러온 뒤에도 같게)
	public void syncFx(){
		//끌어내기 조준선
		if (pullCells == null){
			if (pullFx != null){ pullFx.killAndErase(); pullFx = null; }
		} else if (pullFx == null){
			pullFx = new com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.ChainLine(pos, pullCells[pullCells.length - 1], PULL_COLOR);
			GameScene.groupOverFog(pullFx);
		}
		//사슬 그물
		if (netCells == null){
			for (com.watabou.noosa.Group g : netFx) g.killAndErase();
			netFx.clear();
		} else if (netFx.isEmpty() && netSegs != null){
			for (int i = 0; i + 1 < netSegs.length; i += 2){
				com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.ChainLine l =
						new com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.ChainLine(wallToWall(netSegs[i], netSegs[i + 1])[0], wallToWall(netSegs[i], netSegs[i + 1])[1], NET_COLOR); //벽에서 벽까지
				netFx.add(l);
				GameScene.groupOverFog(l);
			}
		}
		//대봉인 바닥 (안전지대 밖의 대봉인실 바닥 전체)
		if (sealLeft <= 0){
			if (sealFx != null){ sealFx.killAndErase(); sealFx = null; }
			if (sealEmitter != null){ sealEmitter.on = false; sealEmitter = null; }
		} else {
			if (sealFx == null && Dungeon.level instanceof WardenBossLevel){
				WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
				ArrayList<Integer> cells = new ArrayList<>();
				for (int c = 0; c < lv.length(); c++) if (lv.isArenaFloor(c) && !lv.isSafe(c)) cells.add(c);
				sealFx = new com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.SealFloor(cells);
				GameScene.groupOverFog(sealFx);
			}
			if (sealFx != null) sealFx.progress(1f - sealLeft / (float) MazeTower.I9_SEAL_CHARGE);
			//몸 둘레가 검게 시든다
			if (sealEmitter == null && sprite != null && sprite.parent != null){
				sealEmitter = sprite.emitter();
				sealEmitter.pour(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 0.12f);
			}
		}
		refreshSafeFx();
	}

	public void refreshSafeFx(){
		if (safeFx != null){ safeFx.killAndErase(); safeFx = null; }
		if (phase < 2 || !isAlive() || !(Dungeon.level instanceof WardenBossLevel)) return;
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		if (lv.safeCells.isEmpty()) return;
		safeFx = new com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.SafeZone(lv.safeCells);
		GameScene.groupOverFog(safeFx);
	}

	private void clearSkillFx(){
		if (pullFx != null){ pullFx.killAndErase(); pullFx = null; }
		for (com.watabou.noosa.Group g : netFx) g.killAndErase();
		netFx.clear();
		if (sealFx != null){ sealFx.killAndErase(); sealFx = null; }
		if (safeFx != null){ safeFx.killAndErase(); safeFx = null; }
		if (sealEmitter != null){ sealEmitter.on = false; sealEmitter = null; }
	}

	// ===== 2단계: 영혼의 보호 (I6)와 선고 (I7) =====

	public int sentenceCD = MazeTower.I7_SENTENCE_FIRST;

	//철창 하나에서 영혼을 끌어내 주인공에게 선고를 내린다. 내렸으면 true
	private boolean castSentence(){
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		ArrayList<SealCage> cages = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof SealCage && !((SealCage) m).open && lv.ringOf((SealCage) m) != null) cages.add((SealCage) m);
		}
		if (cages.isEmpty()) return false;
		SealCage cage = Random.element(cages);
		//영혼은 철창 옆 빈칸에 나타난다
		ArrayList<Integer> spots = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cage.pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) spots.add(c);
		}
		if (spots.isEmpty()) return false;
		PrisonerSoul soul = new PrisonerSoul();
		soul.pos = Random.element(spots);
		//선고를 먼저 걸어 두어야 영혼 그림이 생길 때 철창과 사슬로 이어진다 (PrisonerSoul.SoulSprite.link)
		int kind = Random.Int(3);
		//종류를 먼저 정한 뒤 붙인다 (붙는 순간 머리 위에 뜨는 이름이 종류를 따르도록)
		Sentence s = new Sentence();
		s.set(kind, MazeTower.I7_SENTENCE_TURNS, cage.id(), soul.id());
		if (!s.attachTo(Dungeon.hero)) return false;
		GameScene.add(soul, 1f);
		soul.aggro(Dungeon.hero);
		CellEmitterBurst(soul.pos);
		if (sprite != null) sprite.turnTo(pos, Dungeon.hero.pos);
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHAINS);
		yell( Messages.get(this, "sentence_" + kind) );
		//무엇이 금지인지 헷갈리지 않게: 로그에도 선고 이름 (머리 위 이름은 상태가 붙을 때 원작대로 뜬다)
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w( Messages.get(this, "sentence_given", s.name(), MazeTower.I7_SENTENCE_TURNS) );
		Dungeon.hero.interrupt();
		return true;
	}

	private static void CellEmitterBurst( int cell ){
		if (Dungeon.level.heroFOV[cell]) com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(cell).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 10);
	}

	//영혼의 보호 (2단계 내내, 풀려난 영혼에 붙들린 동안만 꺼짐). 아이콘으로 보인다
	public static class SoulGuard extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.POSITIVE; }
		@Override public boolean act(){ spend(TICK); return true; }
		@Override public int icon(){ return target != null && target.buff(SoulBind.class) != null ? com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE : com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.ARMOR; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.55f, 0.8f, 1f); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round(MazeTower.I6_PROTECT * 100)); }
	}

	//풀려난 영혼이 간수를 붙든다: 움직이지 못하고 영혼의 보호가 꺼진다
	public static class SoulBind extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {
		{ type = buffType.NEGATIVE; announced = true; }
		@Override public boolean attachTo( Char target ){ if (super.attachTo(target)){ target.rooted = true; return true; } return false; }
		@Override public void detach(){ super.detach(); target.rooted = false; }
		@Override public int icon(){ return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.ROOTS; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.55f, 0.8f, 1f); }
		@Override public float iconFadePercent(){ return Math.max(0, (MazeTower.I7_BIND_TURNS - visualcooldown()) / MazeTower.I7_BIND_TURNS); }
		@Override public String iconTextDisplay(){ return Integer.toString((int) visualcooldown()); }
		@Override public String desc(){ return Messages.get(this, "desc", dispTurns()); }
	}

	@Override
	public String description() {
		if (phase >= 2) return Messages.get(this, "desc_p2", Math.round(MazeTower.I6_PROTECT * 100));
		return super.description();
	}

	//선고 (주인공에게): 걷지 마라 / 치지 마라 / 스킬을 쓰지 마라. 막지는 않고, 어기면 사슬이 조여 피해.
	//남은 턴을 버티면 끌려 나온 영혼은 사라지고 그 철창의 사슬도 끊어진다(풀어 준 것이 아님). 철창 앞 고리를 끊으면 영혼이 풀려나 간수를 붙든다.
	public static class Sentence extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.NEUTRAL; announced = true; } //중립: 정화·숨 고르기 각성 등에 지워지거나 막히지 않게

		public static final int MOVE = 0, ATTACK = 1, SKILL = 2;
		public int kind, left, cageId = -1, soulId = -1;

		public void set( int kind, int turns, int cageId, int soulId ){
			this.kind = kind; this.left = turns; this.cageId = cageId; this.soulId = soulId;
		}

		@Override
		public boolean act() {
			if (--left <= 0){
				//버텨 냈다: 영혼은 사라지고 사슬도 끊어진다
				if (Dungeon.level instanceof WardenBossLevel) ((WardenBossLevel) Dungeon.level).endSentence(this, false);
				detach();
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i( Messages.get(ShadowWarden.class, "sentence_over") );
				return true;
			}
			spend(TICK);
			return true;
		}

		//주인공이 금지된 행동을 했을 때 (WardenBossLevel.occupyCell·BloomEffects.beginNormal·Mana.spendForSkill)
		public static void violate( Char hero, int what ){
			if (hero == null) return;
			Sentence s = hero.buff(Sentence.class);
			if (s == null || s.kind != what || !hero.isAlive()) return;
			int dmg = Random.NormalIntRange(MazeTower.I7_PENALTY_MIN, MazeTower.I7_PENALTY_MAX);
			if (com.watabou.utils.DeviceCompat.isDebug()) System.out.println("[Maze Tower] sentence violated: kind=" + what + " dmg=" + dmg + " hp=" + hero.HP + " shield=" + hero.shielding());
			hero.damage(dmg, s);
			if (hero.sprite != null) hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE, Messages.get(ShadowWarden.class, "violate"));
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.CHAINS, 0.8f, 1.3f);
			if (hero == Dungeon.hero && !hero.isAlive()){
				Dungeon.fail(ShadowWarden.class);
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get(ShadowWarden.class, "sentence_kill") );
			}
		}

		@Override
		public int icon() {
			switch (kind){
				case MOVE: return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.CRIPPLE;
				case ATTACK: return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.WEAKNESS;
				default: return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.MAGIC_SLEEP;
			}
		}

		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(1f, 0.45f, 0.45f); }
		@Override public String iconTextDisplay(){ return Integer.toString(left); }
		@Override public String name(){ return Messages.get(this, "name_" + kind); }
		@Override public String desc(){ return Messages.get(this, "desc_" + kind, left, MazeTower.I7_PENALTY_MIN, MazeTower.I7_PENALTY_MAX); }

		private static final String KIND = "kind", LEFT = "left", CAGE = "cage", SOUL = "soul";
		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put(KIND, kind); b.put(LEFT, left); b.put(CAGE, cageId); b.put(SOUL, soulId); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); kind = b.getInt(KIND); left = b.getInt(LEFT); cageId = b.getInt(CAGE); soulId = b.getInt(SOUL); }
	}

	//(결정) 감방 문이 열리거나 부서지면 그 문으로 살피러 간다 (WardenBossLevel.onDoorOpened)
	private int investigateCell = -1, lookTurns = 0;

	public void investigate( int door ){
		if (phase != 1) return;
		investigateCell = door;
		lookTurns = 0;
	}

	private void planNext(){
		WardenBossLevel lv = (WardenBossLevel) Dungeon.level;
		if (investigateCell >= 0){
			if (pos == investigateCell || lookTurns > 0){
				//문에 닿으면 감방 안쪽을 비춰 보고, 다 보면 가까운 순찰 지점으로 돌아간다
				if (lookTurns <= 0) lookTurns = MazeTower.I2_INVESTIGATE_LOOK;
				lookTurns--;
				nextPos = pos;
				WardenBossLevel.PrisonCell pc = lv.cellOfDoor(investigateCell);
				if (pc != null && !pc.inside.isEmpty()){
					int sx = 0, sy = 0, w = Dungeon.level.width();
					for (int c : pc.inside){ sx += c % w; sy += c / w; }
					int center = (sx / pc.inside.size()) + (sy / pc.inside.size()) * w;
					if (center != pos) nextFacing = LanternCone.octantToward(pos, center);
				}
				if (lookTurns <= 0){
					investigateCell = -1;
					lastWaypoint = -1;
					waypoint = lv.nearestWaypoint(pos);
				}
				return;
			}
			int step = stepToward(investigateCell);
			nextPos = step == -1 ? pos : step;
			nextFacing = LanternCone.octantToward(pos, step == -1 ? investigateCell : step);
			return;
		}
		if (waypoint < 0) waypoint = lv.nearestWaypoint(pos);
		if (pos == waypoint){
			if (waitTurns <= 0) waitTurns = MazeTower.I2_WAYPOINT_WAIT;
			waitTurns--;
			if (waitTurns <= 0){
				int nw = lv.nextWaypoint(waypoint, lastWaypoint);
				lastWaypoint = waypoint;
				waypoint = nw;
			}
			//멈춰 서서 다음 길 쪽을 본다
			nextPos = pos;
			if (waypoint != pos) nextFacing = LanternCone.octantToward(pos, waypoint);
			else nextFacing = facing;
			if (waitTurns > 0) return;
		}
		int step = stepToward(waypoint);
		if (step == -1){
			nextPos = pos;
			nextFacing = waypoint != pos ? LanternCone.octantToward(pos, waypoint) : facing;
		} else {
			nextPos = step;
			nextFacing = LanternCone.octantToward(pos, step);
		}
	}

	private int stepToward( int target ){
		if (target < 0 || target == pos) return -1;
		boolean[] pass = Dungeon.level.passable.clone();
		for (Char c : Actor.chars()){
			if (c != this && c.pos >= 0 && c.pos < pass.length) pass[c.pos] = false;
		}
		pass[target] = true;
		return PathFinder.getStep( pos, target, pass );
	}

	// ===== 단계 =====

	@Override
	public void damage( int dmg, Object src ) {
		//(I6) 영혼의 보호: 2단계에는 물리 피해(무기·스킬·던지기 = 캐릭터가 직접 친 것)를 60% 덜 받는다. 풀려난 영혼에 붙들린 동안은 없다
		if (phase >= 2 && src instanceof Char && buff(SoulGuard.class) != null && buff(SoulBind.class) == null){
			dmg = Math.round(dmg * (1f - MazeTower.I6_PROTECT));
		}
		//(결정) 1단계에 맞으면(기습 포함) 때린 쪽을 돌아보고 그 자리에 서서 비춘다
		if (phase == 1 && src instanceof Char && ((Char) src).pos != pos){
			//(결정) 빛 밖에서 맞은 기습이면 그 턴은 비추기만 하고 가두지 않는다 (한 턴 여유). 빛 안에서 다시 치면 가둔다
			if (!cone(pos, facing)[((Char) src).pos]) graceTurns = 1;
			facing = LanternCone.octantToward(pos, ((Char) src).pos);
			nextFacing = facing;
			nextPos = pos;
			updateLights();
		}
		if (phase == 1 && Dungeon.level instanceof WardenBossLevel){
			int threshold = Math.round(HT * MazeTower.I1_PHASE2_AT);
			//1단계에서 한 번에 2단계 선 아래로 떨어지지 않는다
			if (HP - dmg < threshold) dmg = Math.max(0, HP - threshold);
			super.damage(dmg, src);
			if (isAlive() && HP <= threshold){
				phase = 2;
				clearLights();
				((WardenBossLevel) Dungeon.level).toArena(this);
			}
			return;
		}
		super.damage(dmg, src);
	}

	@Override
	public void die( Object cause ) {
		clearLights();
		pullCells = null; netCells = null; netSegs = null; sealLeft = 0;
		clearSkillFx();
		Dungeon.level.mazeBossPos = pos;
		//만개의 핵(3차 전직)을 떨어뜨린다
		if (MazeTower.TENGU_DROPS_FULLBLOOM_CORE && Dungeon.hero.subClass == HeroSubClass.NONE) {
			Dungeon.level.drop( new FullBloomCore(), pos ).sprite.drop();
		}
		GameScene.bossSlain();
		super.die( cause );

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[1] += 2000;

		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) beacon.upgrade();

		yell( Messages.get(this, "defeated") );
		if (Dungeon.level instanceof WardenBossLevel) ((WardenBossLevel) Dungeon.level).onWardenDeath();
	}

	@Override
	public void destroy() {
		super.destroy();
		clearLights();
		clearSkillFx();
	}

	private static final String PHASE = "phase", FACING = "facing", NEXT_POS = "next_pos", NEXT_FACING = "next_facing",
			WAYPOINT = "waypoint", LAST_WAYPOINT = "last_waypoint", WAIT = "wait", GRACE = "grace", INVEST = "investigate", LOOK = "look";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(PHASE, phase);
		bundle.put(FACING, facing);
		bundle.put(NEXT_POS, nextPos);
		bundle.put(NEXT_FACING, nextFacing);
		bundle.put(WAYPOINT, waypoint);
		bundle.put(LAST_WAYPOINT, lastWaypoint);
		bundle.put(WAIT, waitTurns);
		bundle.put(GRACE, graceTurns);
		bundle.put(INVEST, investigateCell);
		bundle.put(LOOK, lookTurns);
		bundle.put("sentence_cd", sentenceCD);
		bundle.put("pull_cd", pullCD);
		bundle.put("net_cd", netCD);
		bundle.put("seal_cd", sealCD);
		bundle.put("seal_left", sealLeft);
		if (pullCells != null) bundle.put("pull_cells", pullCells);
		if (netCells != null){ bundle.put("net_cells", netCells); bundle.put("net_segs", netSegs); }
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt(PHASE);
		facing = bundle.getInt(FACING);
		nextPos = bundle.getInt(NEXT_POS);
		nextFacing = bundle.getInt(NEXT_FACING);
		waypoint = bundle.getInt(WAYPOINT);
		lastWaypoint = bundle.getInt(LAST_WAYPOINT);
		waitTurns = bundle.getInt(WAIT);
		graceTurns = bundle.getInt(GRACE);
		if (bundle.contains(INVEST)){ investigateCell = bundle.getInt(INVEST); lookTurns = bundle.getInt(LOOK); }
		if (bundle.contains("sentence_cd")) sentenceCD = bundle.getInt("sentence_cd");
		if (bundle.contains("pull_cd")){
			pullCD = bundle.getInt("pull_cd");
			netCD = bundle.getInt("net_cd");
			sealCD = bundle.getInt("seal_cd");
			sealLeft = bundle.getInt("seal_left");
		}
		pullCells = bundle.contains("pull_cells") ? bundle.getIntArray("pull_cells") : null;
		netCells = bundle.contains("net_cells") ? bundle.getIntArray("net_cells") : null;
		netSegs = bundle.contains("net_segs") ? bundle.getIntArray("net_segs") : null;
		com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.assignBoss(this);
	}

	// ===== 그림 =====

	//그림자 간수 그림 (지금 그림 유지). 걷는 동작은 원작 몬스터처럼 미끄러지듯 이동
	public static class WardenSprite extends MobSprite {

		public WardenSprite(){
			super();
			TextureFilm f = MazeBossSprites.apply(this, MazeBossSprites.Look.WARDEN);
			//공격 마지막 칸(8번)은 하얗게 빛나는 그림이라 쓰지 않는다
			attack = MazeBossSprites.anim(f, 15, false, 6, 7, 7, 0);
			zap = attack.clone();
			play( idle );
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof ShadowWarden){
				//불러온 뒤 첫 프레임에 등불을 다시 그린다
				final ShadowWarden w = (ShadowWarden) ch;
				com.watabou.noosa.Game.runOnRenderThread(new com.watabou.utils.Callback() {
					@Override
					public void call() { w.updateLights(); if (w.phase >= 2) w.syncFx(); }
				});
			}
		}
	}
}
