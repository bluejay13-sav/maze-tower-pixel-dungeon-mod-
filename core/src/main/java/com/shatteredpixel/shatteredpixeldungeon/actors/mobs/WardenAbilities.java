/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 10층 그림자 간수(원작 텐구) 2단계 마법 (기획 문서).
//원작 폭탄·불꽃·번개 대신 세 가지만 쓴다.
//  - 봉인 부적: 원작 폭탄과 같은 피해 + 맞은 대상 속박 2턴 (Tengu.BombAbility에서 처리)
//  - 영혼 해방: 맵 곳곳의 부적 철창에서 죄수의 영혼을 불러낸다(최대 4). 불러낸 영혼이 3마리 이상 쓰러지면 다시 쓸 수 있다.
//    철창을 부수면 그 철창에서는 더는 불러내지 못한다.
//  - 만개 광선: 2턴 동안 기를 모아(경고 표시) 무작위 6갈래로 분홍 광선을 쏜다. 벽에 막힌다.
public class WardenAbilities {

	public static final int TALISMAN = 0;
	public static final int SOULS    = 1;
	public static final int BEAM     = 2;

	//간수의 공격(봉인 부적·만개 광선)이 다치게 하지 않는 대상: 간수 편(죄수의 영혼 등 적)과 부적 철창. 주인공(과 아군)만 다친다
	public static boolean spared( Char ch ){
		return com.shatteredpixel.shatteredpixeldungeon.MazeBosses.ENABLED
				&& (ch instanceof SoulCage || ch.alignment == Char.Alignment.ENEMY);
	}

	//주인공을 찾지 못할 때 움직일 옆 칸 (없으면 -1)
	public static int wanderCell( Tengu tengu ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = tengu.pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		return cells.isEmpty() ? -1 : Random.element(cells);
	}

	public static int choose( int used, int last ){
		boolean souls = canReleaseSouls();
		if (used == 0) return TALISMAN;
		if (used == 1) return souls ? SOULS : BEAM;
		ArrayList<Integer> options = new ArrayList<>();
		options.add(TALISMAN);
		options.add(BEAM);
		if (souls) options.add(SOULS);
		int pick = Random.element(options);
		//같은 마법을 연달아 쓰지 않도록 (원작처럼 9/10 확률로 다시 고른다)
		for (int i = 0; i < 3 && pick == last && Random.Int(10) != 0; i++){
			pick = Random.element(options);
		}
		return pick;
	}

	//마법을 쓴다. 봉인 부적을 던졌으면 true (던진 뒤 콜백에서 턴을 넘긴다)
	public static boolean perform( Tengu tengu, int ability ){
		if (ability == TALISMAN){
			if (Tengu.throwBomb(tengu, Dungeon.hero)) return true;
			ability = BEAM;
		}
		if (ability == SOULS && releaseSouls(tengu)) return false;
		startBeam(tengu);
		return false;
	}

	// ===== 영혼 해방 =====

	private static int cagesLeft(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof SoulCage && m.isAlive()) n++;
		return n;
	}

	private static int soulsAlive(){
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof PrisonerSoul && m.isAlive()) n++;
		return n;
	}

	//불러낸 영혼 4마리 중 3마리 이상 쓰러졌으면 (= 살아 있는 영혼 1마리 이하) 다시 쓸 수 있다
	public static boolean canReleaseSouls(){
		return cagesLeft() > 0 && soulsAlive() <= MazeTower.WARDEN_SOULS - MazeTower.WARDEN_SOUL_RECAST_DEAD;
	}

	private static boolean releaseSouls( Tengu tengu ){
		ArrayList<SoulCage> cages = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs) if (m instanceof SoulCage && m.isAlive()) cages.add((SoulCage)m);
		if (cages.isEmpty()) return false;

		int toSpawn = MazeTower.WARDEN_SOULS;
		int spawned = 0;
		int idx = 0;
		while (spawned < toSpawn && idx < toSpawn * 3){
			SoulCage cage = cages.get(idx % cages.size());
			idx++;
			ArrayList<Integer> cells = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				int c = cage.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) cells.add(c);
			}
			if (cells.isEmpty()) continue;
			PrisonerSoul soul = new PrisonerSoul();
			soul.pos = Random.element(cells);
			GameScene.add(soul, 1f);
			if (Dungeon.level.heroFOV[soul.pos]) BossEffects.atCellBottom(BossEffects.Fx.SOUL, soul.pos, 8, false);
			spawned++;
		}
		if (spawned == 0) return false;

		tengu.sprite.zap(tengu.pos);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		tengu.yell(Messages.get(WardenAbilities.class, "souls"));
		return true;
	}

	// ===== 만개 광선 =====

	private static void startBeam( Tengu tengu ){
		//8방향 전부 + 간수 주변 2칸
		int[] dirs = new int[8];
		for (int i = 0; i < 8; i++) dirs[i] = i;
		Buff.append(tengu, BloomBeam.class).setup(tengu.pos, dirs);
		tengu.sprite.zap(tengu.pos);
		if (Dungeon.level.heroFOV[tengu.pos]){
			BossEffects.onChar(BossEffects.Fx.BLOOM_CHARGE, tengu.sprite, 6, false);
			GLog.n(Messages.get(WardenAbilities.class, "bloom_charge"));
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
	}

	//PathFinder.NEIGHBOURS8 순서(왼위, 위, 오른위, 왼, 오른, 왼아래, 아래, 오른아래)의 가로·세로 방향
	private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};
	private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};

	//간수 주변 MazeTower.WARDEN_BEAM_AREA칸 안의 바닥 칸들 (간수 자리 제외)
	private static ArrayList<Integer> areaCells( int origin ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width(), h = Dungeon.level.height();
		int ox = origin % w, oy = origin / w, r = MazeTower.WARDEN_BEAM_AREA;
		for (int y = oy - r; y <= oy + r; y++) for (int x = ox - r; x <= ox + r; x++){
			if (x < 0 || y < 0 || x >= w || y >= h) continue;
			int c = x + y * w;
			if (c == origin || Dungeon.level.solid[c]) continue;
			cells.add(c);
		}
		return cells;
	}

	//한 방향으로 벽 앞까지의 칸들
	private static ArrayList<Integer> beamCells( int origin, int dir ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width(), h = Dungeon.level.height();
		int x = origin % w, y = origin / w;
		while (true){
			x += DX[dir];
			y += DY[dir];
			if (x < 0 || y < 0 || x >= w || y >= h) break;
			int c = x + y * w;
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}
		return cells;
	}

	public static class BloomBeam extends Buff {

		{
			actPriority = MOB_PRIO + 1;
		}

		private int origin;
		private int[] dirs;
		private int timer = MazeTower.WARDEN_BEAM_CHARGE;

		public void setup( int origin, int[] dirs ){
			this.origin = origin;
			this.dirs = dirs;
			mark();
		}

		private void mark(){
			if (dirs == null) return;
			for (int d : dirs){
				for (int c : beamCells(origin, d)){
					GameScene.targetedCell(c, 1f);
				}
			}
			for (int c : areaCells(origin)) GameScene.targetedCell(c, 1f);
		}

		@Override
		public boolean act() {
			if (dirs == null){
				detach();
				return true;
			}
			if (timer > 0){
				timer--;
				if (timer > 0) mark();
				spend(TICK);
				if (timer > 0) return true;
			}
			fire();
			detach();
			return true;
		}

		private void fire(){
			boolean seen = false;
			java.util.HashSet<Integer> hit = new java.util.HashSet<>();
			for (int d : dirs){
				for (int c : beamCells(origin, d)){
					if (Dungeon.level.heroFOV[c]){
						seen = true;
						beamVisual(c, d);
					}
					hit.add(c);
				}
			}
			for (int c : areaCells(origin)){
				if (Dungeon.level.heroFOV[c]) seen = true;
				hit.add(c);
			}
			//간수 편(영혼)·부적 철창은 다치지 않는다
			for (int c : hit){
				Char ch = Actor.findChar(c);
				if (ch != null && !(ch instanceof Tengu) && !spared(ch)){
					int dmg = Random.NormalIntRange(MazeTower.WARDEN_BEAM_MIN + Dungeon.scalingDepth(),
							MazeTower.WARDEN_BEAM_MAX + 2 * Dungeon.scalingDepth());
					ch.damage(dmg, this);
					if (ch == Dungeon.hero && !ch.isAlive()){
						Dungeon.fail(Tengu.class);
					}
				}
			}
			if (seen){
				PointF p = DungeonTilemap.tileCenterToWorld(origin);
				//주변 2칸(5×5)을 덮도록 크게
				MovieClip burst = BossEffects.at(BossEffects.Fx.BLOOM_BURST, p.x, p.y, 10, false);
				float s = (2 * MazeTower.WARDEN_BEAM_AREA + 1) * DungeonTilemap.SIZE / 32f;
				burst.origin.set(16, 16);
				burst.scale.set(s);
				PixelScene.shake(1, 0.3f);
			}
			Sample.INSTANCE.play(Assets.Sounds.RAY);
		}

		//가로 광선(E→0도), 대각 광선(NE 기준). 방향에 맞게 돌린다
		private static void beamVisual( int cell, int dir ){
			int dx = DX[dir], dy = DY[dir];
			boolean diag = dx != 0 && dy != 0;
			MovieClip c = BossEffects.clip(diag ? BossEffects.Fx.BEAM_D : BossEffects.Fx.BEAM_H, 15, false);
			PointF p = DungeonTilemap.tileCenterToWorld(cell);
			c.origin.set(8, 8);
			c.x = p.x - 8;
			c.y = p.y - 8;
			float angle;
			if (!diag){
				angle = dx > 0 ? 0 : (dx < 0 ? 180 : (dy > 0 ? 90 : 270));
			} else {
				//대각 그림은 오른쪽 위(NE)를 향한다고 본다
				if (dx > 0 && dy < 0)      angle = 0;
				else if (dx > 0)           angle = 90;
				else if (dy > 0)           angle = 180;
				else                       angle = 270;
			}
			((Visual)c).angle = angle;
			GameScene.effect(c);
		}

		private static final String ORIGIN = "origin";
		private static final String DIRS   = "dirs";
		private static final String TIMER  = "timer";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(ORIGIN, origin);
			if (dirs != null) bundle.put(DIRS, dirs);
			bundle.put(TIMER, timer);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			origin = bundle.getInt(ORIGIN);
			if (bundle.contains(DIRS)) dirs = bundle.getIntArray(DIRS);
			timer = bundle.getInt(TIMER);
		}
	}
}
