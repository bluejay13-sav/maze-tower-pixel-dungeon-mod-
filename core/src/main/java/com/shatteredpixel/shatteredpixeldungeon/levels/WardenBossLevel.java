/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CellDoor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ChainRing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ChainedSoul;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SealCage;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (I1): 10층 그림자 간수 보스층 (새로 짠 보스). 원작 텐구 보스층(PrisonBossLevel)을 대신한다 (MazeBosses.ENABLED·I_NEW_WARDEN).
//1단계 감방 미로(MAP1): 입구 방 → 짧은 복도 → 감방 9개(3×3, 모양이 조금씩 다름)와 2칸 폭 복도, 철문 5개(문 지형, 빛을 막음).
//  감방 문은 고정 몹(CellDoor), 감방 9개 중 7개에 사슬 고리와 갇힌 영혼. 미로에 들어서면 입구 쪽이 막히고 간수가 감방 사이 빈 터에 나온다.
//2단계 대봉인실(MAP2): 간수 체력이 I1_PHASE2_AT 아래로 내려가면 땅이 무너지며 넓은 방으로 (toArena). 가장자리에 부적 철창 6개(SealCage, 부서지지 않음)와
//  그 앞 바닥의 사슬 고리 6개. 간수를 쓰러뜨리면 입구 쪽 길과 출구가 열린다.
//지도 설계도: docs/sprites/boss10/make_map10.ps1 (글자 지도 map10.txt, 그림 map10_preview.png)
public class WardenBossLevel extends Level {

	{
		color1 = 0x6a723d;
		color2 = 0x88924c;
		viewDistance = 12;
	}

	public enum State { START, PHASE1, PHASE2, WON }
	private State state = State.START;

	private static final int W = 32, H = 32;

	//W 벽 . 바닥 E 입구 D 문 I 철문 c 감방 문 r 사슬 고리 s 갇힌 영혼 g 간수 자리 K 부적 철창 o 철창 앞 고리 X 막힌 벽(이기면 열림) Q 출구 자리
	private static final String[] MAP1 = {
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWW...WWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWW.E.WWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWW...WWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWDWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWW.WWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWW.WWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWW.WWWWWWWWWWWWWWWWWWWWW",
		"WWWWW...W.................WWWWWW",
		"WWWWW...I.................WWWWWW",
		"WWWWW.WWWWW..WWWWW..WWWWW.WWWWWW",
		"WWWWW.Wrs.W..W.srW..Wrs.W.WWWWWW",
		"WWWWW.W...c..W...W..c...W.WWWWWW",
		"WWWWW.WWWWW..WWcWW..WWWWW.WWWWWW",
		"WWWWW..........W..g.......WWWWWW",
		"WWWWW..........I..........WWWWWW",
		"WWWWW.WWcWW..WWWWW..WWWWW.WWWWWW",
		"WWWWW.Wrs.W..W.rsW..Wsr.W.WWWWWW",
		"WWWWW.W...W..c...WIWW...c.WWWWWW",
		"WWWWW.WWWWW..WWWWW..WWWWW.WWWWWW",
		"WWWWW.................W...WWWWWW",
		"WWWWW.................I...WWWWWW",
		"WWWWW.WWcWW..WWWWW..WWWWW.WWWWWW",
		"WWWWW.W...W..Wrs.W..W...W.WWWWWW",
		"WWWWW.W...WWIW...W..c...W.WWWWWW",
		"WWWWW.WWWWW..WWcWW..WWWWW.WWWWWW",
		"WWWWW.....................WWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
	};

	private static final String[] MAP2 = {
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWrrrrrrrWWWWWWWWWWWWW",
		"WWWWWWWWWWWWrrrrrrrWWWWWWWWWWWWW",
		"WWWWWWWWWWWWrrrErrrWWWWWWWWWWWWW",
		"WWWWWWWWWWWWrrrrrrrWWWWWWWWWWWWW",
		"WWWWWWWWWWWWrrrrrrrWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWuuuWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWuuuWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWuuuWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWuuuWWWWWWWWWWWWWWW",
		"WWWWWWWWW...K.....K...WWWWWWWWWW",
		"WWWWWWWWW...o.....o...WWWWWWWWWW",
		"WWWWWWWWW.............WWWWWWWWWW",
		"WWWWWWWWW.............WWWWWWWWWW",
		"WWWWWWWWW.............WWWWWWWWWW",
		"WWWWWWWWWKo....g....oKWWWWWWWWWW",
		"WWWWWWWWW.............WWWWWWWWWW",
		"WWWWWWWWW.............WWWWWWWWWW",
		"WWWWWWWWW.............WWWWWWWWWW",
		"WWWWWWWWW...o.....o...WWWWWWWWWW",
		"WWWWWWWWW...K.....K...WWWWWWWWWW",
		"WWWWWWWWWWWWWWpppWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWpppWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWpppppWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWppppppppWWWWWWWWWWWW",
		"WWWWWWWWWWWWpppQpppWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWppppppWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWpppWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
	};

	private static final int ENTRANCE = 10 + 3 * W;
	private static final int SEAL_CELL = 10 + 8 * W;      //미로에 들어서면 막히는 복도 끝
	private static final int MAZE_TOP = 9;                //이 줄 아래로 들어서면 싸움이 시작된다
	private static final int ARENA_HERO = 15 + 22 * W;    //대봉인실로 떨어진 주인공 자리 (사용자: 줄인 방, 아래쪽 가운데)

	//순찰 지점: 2칸 폭 복도가 만나는 곳 (가로 4 × 세로 4)
	private static final int[] PATROL_X = {5, 11, 18, 25}; //(사용자) 줄인 미로 21×19
	private static final int[] PATROL_Y = {9, 15, 21, 27};

	public State state(){ return state; }

	private static int find( String[] map, char ch ){
		for (int y = 0; y < H; y++){ int x = map[y].indexOf(ch); if (x >= 0) return x + y * W; }
		return -1;
	}

	@Override
	public void playLevelMusic() {
		if (state == State.START){
			Music.INSTANCE.end();
		} else if (state == State.WON) {
			Music.INSTANCE.playTracks(PrisonLevel.PRISON_TRACK_LIST, PrisonLevel.PRISON_TRACK_CHANCES, false);
		} else {
			Music.INSTANCE.play(Assets.Music.PRISON_BOSS, true);
		}
	}

	@Override
	public String tilesTex() { return Assets.Environment.TILES_PRISON; }

	@Override
	public String waterTex() { return Assets.Environment.WATER_PRISON; }

	private void applyMap( String[] src ){
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				char ch = src[y].charAt(x);
				int t;
				switch (ch){
					case 'W': case 'X': case 'p': case 'u': case 'Q': t = Terrain.WALL; break; //p·u·Q: 간수를 쓰러뜨리면 열리는 아래쪽 길·위쪽 길과 계단
					case 'E': t = Terrain.ENTRANCE; break;
					case 'D': case 'I': t = Terrain.DOOR; break;
					default: t = Terrain.EMPTY; break;
				}
				map[x + y * W] = t;
			}
		}
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		applyMap(MAP1);
		transitions.add(new LevelTransition(this, ENTRANCE, LevelTransition.Type.REGULAR_ENTRANCE));
		return true;
	}

	//감방 문, 사슬 고리와 갇힌 영혼
	@Override
	protected void createMobs() {
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				char ch = MAP1[y].charAt(x);
				int cell = x + y * W;
				if (ch == 'c'){
					CellDoor d = new CellDoor();
					d.pos = cell;
					mobs.add(d);
				} else if (ch == 'r'){
					int soulCell = MAP1[y].charAt(x + 1) == 's' ? cell + 1 : cell - 1;
					ChainRing ring = new ChainRing();
					ring.pos = cell;
					ChainedSoul soul = new ChainedSoul();
					soul.pos = soulCell;
					ring.soulId = soul.id();
					soul.ringId = ring.id();
					mobs.add(ring);
					mobs.add(soul);
				}
			}
		}
	}

	@Override
	public Actor addRespawner() { return null; }

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				for (Item i : bonesItems) {
					drop(i, ENTRANCE + W).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = entrance() + i; //2단계 뒤에는 위쪽 새 방의 계단
			if (passable[cell] && Actor.findChar(cell) == null) candidates.add(cell);
		}
		return candidates.isEmpty() ? -1 : Random.element(candidates);
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell( ch );
		if (ch == Dungeon.hero && state == State.START && ch.pos / W >= MAZE_TOP){
			startFight();
		}
	}

	//미로에 들어서면: 입구 쪽 복도가 막히고 간수가 감방 사이 빈 터에 나온다
	private void startFight(){
		state = State.PHASE1;
		seal();
		Statistics.qualifiedForBossChallengeBadge = true;
		set( SEAL_CELL, Terrain.WALL );
		GameScene.updateMap( SEAL_CELL );
		CellEmitter.get( SEAL_CELL ).burst( ShadowParticle.UP, 15 );

		ShadowWarden w = new ShadowWarden();
		w.pos = find(MAP1, 'g');
		if (Actor.findChar(w.pos) != null){
			for (int n : PathFinder.NEIGHBOURS8){
				if (passable[w.pos + n] && Actor.findChar(w.pos + n) == null){ w.pos += n; break; }
			}
		}
		GameScene.add( w );
		BossHealthBar.assignBoss( w );
		w.yell( Messages.get(w, "notice") );
		Dungeon.observe();

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() { Music.INSTANCE.play(Assets.Music.PRISON_BOSS, true); }
		});
	}

	// ===== 감방 (I3) =====

	//감방 하나: 문 칸과 안쪽 칸들 (MAP1에서 계산)
	public static class PrisonCell {
		public int door;
		public ArrayList<Integer> inside = new ArrayList<>();
	}
	private static final int CELL_W = 5, CELL_H = 4; //감방 블록 크기 (벽 포함)
	private ArrayList<PrisonCell> prisonCells;

	public ArrayList<PrisonCell> prisonCells(){
		if (prisonCells != null) return prisonCells;
		prisonCells = new ArrayList<>();
		int[][] blocks = {{6,11},{13,11},{20,11},{6,17},{13,17},{20,17},{6,23},{13,23},{20,23}}; //(사용자) 줄인 미로: 감방 블록 5×4
		for (int[] b : blocks){
			PrisonCell pc = new PrisonCell();
			for (int y = b[1]; y < b[1] + CELL_H; y++) for (int x = b[0]; x < b[0] + CELL_W; x++){
				if (MAP1[y].charAt(x) == 'c') pc.door = x + y * W;
			}
			//문에서 블록 안쪽으로 이어진 바닥을 따라 채운다 (벽·문을 넘지 않음)
			ArrayList<Integer> open = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS4){
				int c = pc.door + n, cx = c % W, cy = c / W;
				if (cx >= b[0] && cx < b[0] + CELL_W && cy >= b[1] && cy < b[1] + CELL_H && "rs.".indexOf(MAP1[cy].charAt(cx)) >= 0) open.add(c);
			}
			while (!open.isEmpty()){
				int c = open.remove(0);
				if (pc.inside.contains(c)) continue;
				pc.inside.add(c);
				for (int n : PathFinder.NEIGHBOURS4){
					int d = c + n, dx = d % W, dy = d / W;
					if (dx >= b[0] && dx < b[0] + CELL_W && dy >= b[1] && dy < b[1] + CELL_H && "rs.".indexOf(MAP1[dy].charAt(dx)) >= 0 && !pc.inside.contains(d)) open.add(d);
				}
			}
			prisonCells.add(pc);
		}
		return prisonCells;
	}

	//그 문의 감방
	public PrisonCell cellOfDoor( int door ){
		for (PrisonCell pc : prisonCells()) if (pc.door == door) return pc;
		return null;
	}

	//그 칸이 있는 감방 (없으면 null, 1단계에서만)
	public PrisonCell cellOf( int pos ){
		if (state != State.PHASE1) return null;
		for (PrisonCell pc : prisonCells()) if (pc.inside.contains(pos)) return pc;
		return null;
	}


	//감방 안의 사슬 고리가 부서져 영혼이 풀려나면: 그 감방의 문을 열어 준다. 1단계 감방 영혼이면 true
	public boolean onSoulFreed( ChainedSoul soul ){
		PrisonCell pc = cellOf(soul.pos);
		if (pc == null) return false;
		Char d = Actor.findChar(pc.door);
		if (d instanceof CellDoor) ((CellDoor) d).openBySoul();
		return true;
	}

	//(결정) 감방 문이 열리거나 부서지면 간수가 순찰을 멈추고 그 문으로 살피러 온다 (CellDoor.die·openBySoul에서)
	public static void onDoorOpened( int door ){
		if (!(Dungeon.level instanceof WardenBossLevel) || ((WardenBossLevel) Dungeon.level).state != State.PHASE1) return;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof ShadowWarden) ((ShadowWarden) m).investigate(door);
		}
	}

	//(결정) 감방 문은 한 번 열리거나 부서지면 그대로 남는다. 문이 아직 잠겨 있는지
	public boolean doorLocked( PrisonCell pc ){
		return Actor.findChar(pc.door) instanceof CellDoor;
	}

	//(결정) 간수에게 잡히면 문이 아직 잠겨 있는 가장 가까운 감방으로 (없으면 null)
	public PrisonCell nearestLockedCell( int from ){
		PrisonCell best = null; int bestD = Integer.MAX_VALUE;
		for (PrisonCell pc : prisonCells()){
			if (!doorLocked(pc)) continue;
			int d = Math.round(trueDistance(from, pc.door));
			if (d < bestD){ bestD = d; best = pc; }
		}
		return best;
	}

	//감방 안의 빈 칸 (문에서 가장 먼 칸부터)
	public int freeInside( PrisonCell pc ){
		int best = -1;
		for (int c : pc.inside){
			if (Actor.findChar(c) != null) continue;
			if (best == -1 || trueDistance(pc.door, c) > trueDistance(pc.door, best)) best = c;
		}
		return best;
	}

	// ===== 순찰 지점 =====

	public int nearestWaypoint( int from ){
		int best = -1;
		for (int x : PATROL_X) for (int y : PATROL_Y){
			int c = x + y * W;
			if (best == -1 || trueDistance(from, c) < trueDistance(from, best)) best = c;
		}
		return best;
	}

	//(결정) 순찰 길은 매번 같다: 16곳을 한 바퀴 도는 정해진 순서 (주인공이 길을 익혀 등 뒤를 노릴 수 있게)
	//칸 번호(가로 0~3, 세로 0~3): 간수가 나오는 (2,1)에서 시작해 오른쪽 위 → 왼쪽 위 → 가운데 → 왼쪽 아래 → 오른쪽 아래 → 다시 (2,1)
	private static final int[][] ROUTE = {{2,1},{3,1},{3,0},{2,0},{1,0},{0,0},{0,1},{1,1},{1,2},{0,2},{0,3},{1,3},{2,3},{3,3},{3,2},{2,2}};

	public int nextWaypoint( int from, int came ){
		for (int i = 0; i < ROUTE.length; i++){
			if (PATROL_X[ROUTE[i][0]] + PATROL_Y[ROUTE[i][1]] * W == from){
				int[] n = ROUTE[(i + 1) % ROUTE.length];
				return PATROL_X[n[0]] + PATROL_Y[n[1]] * W;
			}
		}
		return nearestWaypoint(from);
	}

	// ===== 2단계: 땅이 무너지며 대봉인실로 =====

	public void toArena( ShadowWarden w ){
		state = State.PHASE2;
		Dungeon.hero.interrupt();

		//(I6) 간수가 허리의 열쇠를 꽂아 돌린다: 봉인이 풀리며 땅이 무너진다
		w.yell( Messages.get(ShadowWarden.class, "key_yell") );
		GLog.w( Messages.get(ShadowWarden.class, "key_turn") );
		Sample.INSTANCE.play( Assets.Sounds.UNLOCK );

		//땅이 무너지는 연출 (흔들림 1픽셀, 짧게)
		PixelScene.shake( 1, 0.4f );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS );
		for (int n : PathFinder.NEIGHBOURS9){
			int c = Dungeon.hero.pos + n;
			if (c >= 0 && c < length()) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
		}

		//바닥의 물건은 모아 두었다가 방이 바뀐 뒤 다시 흩어 놓는다
		ArrayList<Item> stored = new ArrayList<>();
		for (Heap heap : heaps.valueList()){
			stored.addAll(heap.items);
			heap.destroy();
		}

		//간수와 주인공 말고는 모두 치운다 (감방 문·고리·영혼·소환된 것)
		for (Mob m : mobs.toArray(new Mob[0])){
			if (m == w) continue;
			m.destroy();
			if (m.sprite != null) m.sprite.killAndErase();
		}
		for (com.shatteredpixel.shatteredpixeldungeon.plants.Plant p : plants.valueList()) plants.remove(p.pos);

		applyMap(MAP2);
		//(사용자) 2단계 지도에서는 올라가는 계단이 위쪽 가운데 새 방에 있다 (간수를 쓰러뜨리면 그 방으로 가는 길이 열린다)
		transitions.clear();
		transitions.add(new LevelTransition(this, find(MAP2, 'E'), LevelTransition.Type.REGULAR_ENTRANCE));
		buildFlagMaps();
		cleanWalls();
		BArray.setFalse(visited);
		BArray.setFalse(mapped);
		for (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob blob : blobs.values()) blob.fullyClear();
		traps.clear();
		addVisuals();
		GameScene.resetMap();

		//철창과 그 앞의 고리
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				char ch = MAP2[y].charAt(x);
				if (ch == 'K'){
					SealCage cage = new SealCage();
					cage.pos = x + y * W;
					GameScene.add(cage);
				} else if (ch == 'o'){
					ChainRing ring = new ChainRing();
					ring.pos = x + y * W;
					ring.raised = false;
					GameScene.add(ring);
				}
			}
		}

		//주인공과 간수 자리
		Dungeon.hero.pos = ARENA_HERO;
		Dungeon.hero.sprite.interruptMotion();
		Dungeon.hero.sprite.place(Dungeon.hero.pos);
		Camera.main.snapTo(Dungeon.hero.sprite.center());
		w.pos = find(MAP2, 'g');
		w.sprite.interruptMotion();
		w.sprite.place(w.pos);
		w.state = w.HUNTING;

		for (Item i : stored){
			int c;
			do { c = randomArenaCell(); } while (c == -1);
			drop(i, c);
		}

		//(I6) 영혼의 보호: 2단계 내내 받는 물리 피해가 줄어든다
		Buff.affect(w, ShadowWarden.SoulGuard.class);

		Dungeon.observe();
		GameScene.flash(0x80FFFFFF);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
	}

	// ===== 2단계: 선고와 철창 (I7) =====

	//2단계에서 고리를 끊어 풀어 준 영혼 수 (I9 대봉인: 0이면 대봉인의 쿨타임이 돌지 않는다)
	public int arenaFreed = 0;

	//대봉인실 안쪽 (MAP2)
	public static final int AX0 = 9, AX1 = 21, AY0 = 13, AY1 = 23; //(사용자) 줄인 대봉인실 13×11

	//대봉인실의 바닥 칸인지 (철창·벽·닫힌 길 제외)
	public boolean isArenaFloor( int c ){
		int x = c % W, y = c / W;
		if (x < AX0 || x > AX1 || y < AY0 || y > AY1) return false;
		return ".go".indexOf(MAP2[y].charAt(x)) >= 0;
	}

	//(I9) 대봉인을 피할 안전지대: (결정) 2단계에서 고리를 끊어 풀어 준 영혼 하나마다 3×3 하나 (저장됨)
	public ArrayList<Integer> safeCells = new ArrayList<>();

	//(사용자) 안전지대는 영혼을 풀어 준 그 철창 앞에 생긴다: 철창 → 고리 방향으로 한 칸 더 들어간 칸을 가운데로 한 3×3 (방 바닥만).
	//철창이 방 가장자리에 고르게 있어 어디서든 가깝다(줄인 방에서 가장 먼 곳도 9칸 안).
	private void addSafeZone( SealCage cage, ChainRing ring ){
		int n = MazeTower.I9_SAFE_SIZE;
		int rx = ring.pos % W, ry = ring.pos / W;
		int cx = rx + Integer.signum(rx - cage.pos % W), cy = ry + Integer.signum(ry - cage.pos / W);
		int top = (cx - n / 2) + (cy - n / 2) * W;
		for (int dy = 0; dy < n; dy++) for (int dx = 0; dx < n; dx++){
			int c = top + dx + dy * W;
			if (isArenaFloor(c) && !safeCells.contains(c)) safeCells.add(c);
		}
		for (Mob m : mobs){
			if (m instanceof ShadowWarden) ((ShadowWarden) m).refreshSafeFx();
		}
		if (heroFOV[cx + cy * W]) CellEmitter.get(cx + cy * W).burst(Speck.factory(Speck.LIGHT), 8);
	}

	public boolean isSafe( int c ){
		return safeCells.contains(c);
	}

	//철창 앞의 고리 (철창 바로 옆 칸, 살아 있는 것)
	public ChainRing ringOf( SealCage cage ){
		for (Mob m : mobs){
			if (m instanceof ChainRing && !((ChainRing) m).raised && m.isAlive() && distance(m.pos, cage.pos) <= 1) return (ChainRing) m;
		}
		return null;
	}

	private SealCage cageOf( ChainRing ring ){
		for (Mob m : mobs){
			if (m instanceof SealCage && distance(m.pos, ring.pos) <= 1) return (SealCage) m;
		}
		return null;
	}

	//지금 선고가 걸린 철창의 고리인지 (그 고리만 끊을 수 있다)
	public boolean sentenceRing( ChainRing ring ){
		if (state != State.PHASE2 || Dungeon.hero == null) return false;
		ShadowWarden.Sentence s = Dungeon.hero.buff(ShadowWarden.Sentence.class);
		if (s == null) return false;
		SealCage cage = cageOf(ring);
		return cage != null && cage.id() == s.cageId;
	}

	//선고가 끝났다. freed = 고리를 끊어 풀어 줌, false = 남은 턴을 버팀(영혼은 그냥 사라지고 사슬도 끊어진다)
	public void endSentence( ShadowWarden.Sentence s, boolean freed ){
		Actor soul = Actor.findById(s.soulId);
		if (soul instanceof Char && ((Char) soul).isAlive()){
			Char c = (Char) soul;
			if (c.sprite != null && heroFOV[c.pos]) CellEmitter.get(c.pos).burst(freed ? Speck.factory(Speck.LIGHT) : ShadowParticle.UP, 8);
			c.destroy();
			if (c.sprite != null) c.sprite.die();
		}
		Actor cage = Actor.findById(s.cageId);
		if (cage instanceof SealCage){
			((SealCage) cage).empty();
			if (!freed){
				ChainRing ring = ringOf((SealCage) cage);
				if (ring != null){
					ring.destroy();
					if (ring.sprite != null) ring.sprite.die();
					if (heroFOV[ring.pos]) Sample.INSTANCE.play(Assets.Sounds.CHAINS, 0.8f, 0.7f);
				}
			}
		}
	}

	//선고가 걸린 철창 앞의 고리를 끊었다: 선고가 풀리고, 끌려 나온 영혼이 풀려나 간수를 붙든다
	public void onArenaRingBroken( ChainRing ring ){
		ShadowWarden.Sentence s = Dungeon.hero.buff(ShadowWarden.Sentence.class);
		SealCage cage = cageOf(ring);
		if (s == null || cage == null || cage.id() != s.cageId) return;
		Actor soul = Actor.findById(s.soulId);
		int from = soul instanceof Char ? ((Char) soul).pos : cage.pos;
		endSentence(s, true);
		s.detach();
		arenaFreed++;
		addSafeZone(cage, ring);
		for (Mob m : mobs.toArray(new Mob[0])){
			if (m instanceof ShadowWarden && m.isAlive()){
				Buff.prolong(m, ShadowWarden.SoulBind.class, MazeTower.I7_BIND_TURNS);
				if (m.sprite != null){
					m.sprite.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Chains(
							from, m.pos, com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.CHAIN, null));
					m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
					m.sprite.showStatus(CharSprite.POSITIVE, Messages.get(ShadowWarden.class, "bound_status"));
				}
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.CHAINS);
		GLog.p( Messages.get(ShadowWarden.class, "soul_binds") );
	}

	private int randomArenaCell(){
		for (int tries = 0; tries < 200; tries++){
			int c = Random.Int(length());
			if (passable[c] && Actor.findChar(c) == null && MAP2[c / W].charAt(c % W) == '.') return c;
		}
		return ARENA_HERO;
	}

	// ===== 이겼을 때 =====

	//Maze Tower (디버그 전용, MazeMapShot): 대봉인실로 바꾸고 출구를 연 모습으로 (맵 사진용, 게임에서는 부르지 않음)
	public void mazeShotOpen(){
		applyMap(MAP2);
		state = State.PHASE2;
		onWardenDeath();
	}

	public void onWardenDeath(){
		state = State.WON;
		unseal();
		//남은 선고와 끌려 나온 영혼은 사라진다
		ShadowWarden.Sentence s = Dungeon.hero.buff(ShadowWarden.Sentence.class);
		if (s != null){ endSentence(s, false); s.detach(); }
		int open = find(MAP2, 'X');
		if (open >= 0){ set(open, Terrain.EMPTY); GameScene.updateMap(open); }
		//(사용자) 방 아래쪽 가운데의 벽이 무너지며 세 칸 너비의 길이 열리고, 그 끝에 내려가는 계단이 있다. 위쪽 두 철창 가운데에서도 입구로 가는 길이 열린다
		boolean pathSeen = false;
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				if ("pu".indexOf(MAP2[y].charAt(x)) < 0) continue; //아래쪽 길(p)과 위 철창 가운데에서 입구로 가는 길(u)
				int c = x + y * W;
				set(c, Terrain.EMPTY);
				GameScene.updateMap(c);
				if (heroFOV[c]){ CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3); pathSeen = true; }
			}
		}
		cleanWalls();
		GameScene.updateMap(); //벽 그림이 이웃 칸을 보고 다시 그려지게
		if (pathSeen){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(1, 0.3f);
		}
		int exit = find(MAP2, 'Q');
		if (exit >= 0){
			set(exit, Terrain.EXIT);
			GameScene.updateMap(exit);
			transitions.add(new LevelTransition(this, exit, LevelTransition.Type.REGULAR_EXIT));
			//Maze Tower (사용자): 계단 위 보라 그림자 입자는 없앰
		}
		Dungeon.observe();
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut(5f, new Callback() {
					@Override
					public void call() { Music.INSTANCE.end(); }
				});
			}
		});
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		PrisonLevel.addPrisonVisuals(this, visuals);
		return visuals;
	}

	private static final String STATE = "maze_state";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( STATE, state );
		bundle.put( "arena_freed", arenaFreed );
		int[] sc = new int[safeCells.size()];
		for (int i = 0; i < sc.length; i++) sc[i] = safeCells.get(i);
		bundle.put( "safe_cells", sc );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		state = bundle.getEnum( STATE, State.class );
		if (bundle.contains("arena_freed")) arenaFreed = bundle.getInt("arena_freed");
		safeCells.clear();
		if (bundle.contains("safe_cells")) for (int c : bundle.getIntArray("safe_cells")) safeCells.add(c);
	}
}
