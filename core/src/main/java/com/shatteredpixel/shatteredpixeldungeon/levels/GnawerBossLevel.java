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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ManaVein;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RootGnawer;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (K1): 15층 "뿌리를 갉는 것" 보스층 (새로 짠 보스). 원작 DM-300 보스층(CavesBossLevel)을 대신한다 (MazeBosses.ENABLED·K_NEW_GNAWER).
//위쪽 입구 방 → 짧은 통로 → 넓은 동굴 방(타원에 울퉁불퉁한 가장자리). 푸른 물 세 곳(w), 광맥 기둥(ManaVein) K_VEINS개를 층을 만들 때 무작위 자리에
//(물 아님, 서로 2칸 이상, 입구 통로 끝에서 3칸 밖, 둘레 8칸에 벽 없음, 왼쪽이나 오른쪽 옆 칸이 비어 있음 — 벌레가 옆에서 갉을 수 있게).
//벌레가 땅속에 있을 때 머무는 대기 칸(Z, 벽 속 한 칸, 주인공이 닿을 수 없음). 방에 들어서면 통로가 막히고 싸움이 시작된다.
//이기면 아래쪽 벽이 무너지며 길(p)과 내려가는 계단(Q)이 열린다. 벌레가 땅속으로 지나간 줄은 굴 자국(trail, K3)으로 남는다.
//지도 설계도: docs/sprites/boss15/make_map15.ps1 (글자 지도 map15.txt, 그림 map15_preview.png)
public class GnawerBossLevel extends Level {

	{
		color1 = 0x534f3e;
		color2 = 0xb9d661;
		viewDistance = 12;
	}

	public enum State { START, FIGHT, WON }
	private State state = State.START;

	private static final int W = 32, H = 32;

	//W 벽 . 바닥 E 입구 w 물 Z 땅속 대기 칸 p 이기면 열리는 길 Q 이기면 열리는 계단
	private static final String[] MAP = {
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WZWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWW...WWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWW.E.WWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWW...WWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWW.WWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWW.WWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWW.WWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWW.WWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWW........WWWWWWWWWWW",
		"WWWWWWWWWW.............WWWWWWWWW",
		"WWWWWWWW...............WWWWWWWWW",
		"WWWWWWW.................WWWWWWWW",
		"WWWWWWW..................WWWWWWW",
		"WWWWWWW..................WWWWWWW",
		"WWWWWW....................WWWWWW",
		"WWWWWW.ww..............ww.WWWWWW",
		"WWWWW..ww..............ww.WWWWWW",
		"WWWWWW.w................w.WWWWWW",
		"WWWWWW....................WWWWWW",
		"WWWWWW...................WWWWWWW",
		"WWWWWWW..................WWWWWWW",
		"WWWWWWWW................WWWWWWWW",
		"WWWWWWWWWW....www.....WWWWWWWWWW",
		"WWWWWWWWWWWW...ww....WWWWWWWWWWW",
		"WWWWWWWWWWWWWW.....WWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWpppWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWpppWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWpppppppWWWWWWWWWWWWW",
		"WWWWWWWWWWWWpppQpppWWWWWWWWWWWWW",
		"WWWWWWWWWWWWpppppppWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
	};

	private static final int ENTRANCE = 15 + 3 * W;
	private static final int SEAL_CELL = 15 + 7 * W;     //방에 들어서면 막히는 통로 칸
	private static final int ROOM_TOP = 9;               //이 줄부터 방 (들어서면 싸움이 시작된다)
	private static final int CORRIDOR_END = 15 + 8 * W;  //통로 끝 (광맥은 여기서 3칸 밖)
	public static final int WAIT_CELL = 1 + 1 * W;       //땅속 대기 칸

	//굴 자국 (K3): 벌레가 땅속으로 지나간 칸 (비트 1 가로, 2 세로). 1단계에는 효과가 없다
	public int[] trail = new int[W * H];
	private TrailLayer trailLayer;

	public void refreshTrail(){
		if (trailLayer != null) trailLayer.rebuild();
	}

	//(사용자) 벌레가 지나간 줄에 굴 자국을 더한다. 줄을 따라 한 칸씩(K_TRAIL_SWEEP초 간격) 서서히 생긴다
	public void addTrail( ArrayList<Integer> cells, int bit ){ addTrail(cells, bit, MazeTower.K_TRAIL_SWEEP); }

	//step: 줄을 따라 한 칸씩 생기는 간격(초). 굴착은 땅이 들썩이는 이펙트와 같은 빠르기(K_DIG_FX_STEP)
	public void addTrail( ArrayList<Integer> cells, int bit, float step ){ addTrail(cells, bit, step, MazeTower.K_TRAIL_ROCKS); }

	//rocks: 굴 자국이 한 칸 생길 때 튀는 돌 부스러기 수
	public void addTrail( ArrayList<Integer> cells, int bit, float step, int rocks ){
		java.util.HashMap<Long, Float> delays = new java.util.HashMap<>();
		for (int i = 0; i < cells.size(); i++){
			int c = cells.get(i);
			if ((trail[c] & bit) == 0){ delays.put(c * 4L + bit, i * step); if (trailLayer != null) trailLayer.rocks.put(c * 4L + bit, rocks); }
			trail[c] |= bit;
		}
		if (trailLayer != null){ trailLayer.pending.putAll(delays); trailLayer.rebuild(); }
	}

	@Override
	public com.watabou.noosa.Group addVisuals() {
		super.addVisuals();
		trailLayer = new TrailLayer(this);
		visuals.add(trailLayer);
		return visuals;
	}

	//굴 자국 그림 (sprites/maze_gnawer_tunnel.png 16x16 x 2: 0 가로, 1 세로, K_TRAIL_ALPHA로 옅게). 본 적 있는 칸에만
	public static class TrailLayer extends com.watabou.noosa.Group {
		private final GnawerBossLevel lv;
		private final java.util.ArrayList<com.watabou.noosa.Image> imgs = new java.util.ArrayList<>();
		private final java.util.ArrayList<Integer> cellsOf = new java.util.ArrayList<>();
		//새로 생기는 자국: (칸 × 4 + 비트) → 나타나기까지 남은 시간. 나타난 뒤 0.3초에 걸쳐 진해진다
		final java.util.HashMap<Long, Float> pending = new java.util.HashMap<>();
		final java.util.HashMap<Long, Integer> rocks = new java.util.HashMap<>();
		private final java.util.ArrayList<Long> keyOf = new java.util.ArrayList<>();
		private final java.util.HashMap<Long, Float> grow = new java.util.HashMap<>();

		TrailLayer( GnawerBossLevel lv ){ this.lv = lv; rebuild(); }

		public void rebuild(){
			for (com.watabou.noosa.Image im : imgs) im.killAndErase();
			imgs.clear(); cellsOf.clear(); keyOf.clear();
			for (int c = 0; c < lv.trail.length; c++){
				for (int bit = 0; bit < 2; bit++){
					if ((lv.trail[c] & (1 << bit)) == 0) continue;
					com.watabou.noosa.Image im = new com.watabou.noosa.Image("sprites/maze_gnawer_tunnel.png", bit * 16, 0, 16, 16);
					com.watabou.utils.PointF p = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileToWorld(c);
					im.x = p.x; im.y = p.y;
					im.alpha(MazeTower.K_TRAIL_ALPHA);
					add(im); imgs.add(im); cellsOf.add(c); keyOf.add(c * 4L + (1 << bit));
				}
			}
			update();
		}

		@Override
		public void update() {
			super.update();
			float dt = com.watabou.noosa.Game.elapsed;
			for (java.util.Map.Entry<Long, Float> e : pending.entrySet()) e.setValue(e.getValue() - dt);
			for (java.util.Map.Entry<Long, Float> e : grow.entrySet()) e.setValue(Math.min(1f, e.getValue() + dt / 0.3f));
			for (int i = 0; i < imgs.size(); i++){
				int c = cellsOf.get(i);
				long key = keyOf.get(i);
				com.watabou.noosa.Image im = imgs.get(i);
				im.visible = lv.visited[c] || lv.mapped[c];
				Float wait = pending.get(key);
				if (wait != null){
					if (wait > 0){ im.alpha(0); continue; }
					pending.remove(key);
					grow.put(key, 0f);
					//굴이 생길 때 돌 부스러기 ((사용자) 노란 먼지는 뺌)
					int n = rocks.containsKey(key) ? rocks.remove(key) : MazeTower.K_TRAIL_ROCKS;
					if (lv.heroFOV[c]){
						if (n > 0) com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(c).burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.ROCK), n);
					}
				}
				Float g = grow.get(key);
				if (g != null){
					im.alpha(MazeTower.K_TRAIL_ALPHA * g);
					if (g >= 1f) grow.remove(key);
				}
			}
		}
	}

	public State state(){ return state; }

	private static int find( char ch ){
		for (int y = 0; y < H; y++){ int x = MAP[y].indexOf(ch); if (x >= 0) return x + y * W; }
		return -1;
	}

	//방 바닥인지 (입구 방·통로·대기 칸 제외)
	public boolean isRoomFloor( int c ){
		if (c < 0 || c >= W * H) return false;
		return c / W >= ROOM_TOP && c / W <= 25 && (map[c] == Terrain.EMPTY || map[c] == Terrain.WATER
				|| map[c] == Terrain.GRASS || map[c] == Terrain.HIGH_GRASS || map[c] == Terrain.FURROWED_GRASS || map[c] == Terrain.EMBERS);
	}

	@Override
	public void playLevelMusic() {
		if (state == State.START){
			Music.INSTANCE.end();
		} else if (state == State.WON) {
			Music.INSTANCE.playTracks(CavesLevel.CAVES_TRACK_LIST, CavesLevel.CAVES_TRACK_CHANCES, false);
		} else {
			Music.INSTANCE.play(Assets.Music.CAVES_BOSS, true);
		}
	}

	@Override
	public String tilesTex() { return Assets.Environment.TILES_CAVES; }

	@Override
	public String waterTex() { return Assets.Environment.WATER_CAVES; }

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int t;
				switch (MAP[y].charAt(x)){
					case 'W': case 'p': case 'Q': t = Terrain.WALL; break;
					case 'E': t = Terrain.ENTRANCE; break;
					case 'w': t = Terrain.WATER; break;
					default: t = Terrain.EMPTY; break; //바닥과 대기 칸
				}
				map[x + y * W] = t;
			}
		}
		transitions.add(new LevelTransition(this, ENTRANCE, LevelTransition.Type.REGULAR_ENTRANCE));
		return true;
	}

	//광맥 기둥 K_VEINS개를 무작위 자리에 (규칙은 위)
	@Override
	protected void createMobs() {
		ArrayList<Integer> placed = new ArrayList<>();
		for (int tries = 0; tries < 4000 && placed.size() < MazeTower.K_VEINS; tries++){
			int c = Random.IntRange(3, W - 4) + Random.IntRange(ROOM_TOP, 27) * W;
			if (!veinSpot(c, placed)) continue;
			placed.add(c);
			ManaVein v = new ManaVein();
			v.pos = c;
			mobs.add(v);
		}
	}

	//광맥을 놓을 수 있는 칸인지 (지도 기준 규칙 + 다른 광맥에서 2칸 이상)
	private boolean veinSpot( int c, ArrayList<Integer> others ){
		int x = c % W, y = c / W;
		if (y < 1 || y > H - 2 || x < 1 || x > W - 2) return false;
		if (MAP[y].charAt(x) != '.' || !isRoomFloor(c)) return false;
		if (Math.max(Math.abs(x - 15), Math.abs(y - 8)) < 3) return false;
		if (MAP[y].charAt(x - 1) != '.' && MAP[y].charAt(x + 1) != '.') return false;
		for (int n : PathFinder.NEIGHBOURS8) if (MAP[(c + n) / W].charAt((c + n) % W) == 'W') return false;
		for (int o : others) if (Math.max(Math.abs(o % W - x), Math.abs(o / W - y)) < 2) return false;
		return true;
	}

	//(사용자) 보스전 중에는 광맥이 깨질 때마다 다른 빈 자리에 하나씩 다시 자란다 (깨진 칸·주인공 둘레 1칸 제외)
	public void onVeinBroken( int broken ){
		if (state != State.FIGHT) return;
		ArrayList<Integer> others = new ArrayList<>();
		for (Mob m : mobs) if (m instanceof ManaVein && m.isAlive() && m.pos != broken) others.add(m.pos);
		ArrayList<Integer> cand = new ArrayList<>();
		for (int c = 0; c < W * H; c++){
			if (c == broken || !freeFloor(c) || !veinSpot(c, others)) continue;
			if (Dungeon.hero != null && Dungeon.level.distance(c, Dungeon.hero.pos) <= 1) continue;
			cand.add(c);
		}
		if (cand.isEmpty()) return;
		ManaVein v = new ManaVein();
		v.pos = Random.element(cand);
		GameScene.add(v);
		if (heroFOV[v.pos]){
			CellEmitter.get(v.pos).burst(Speck.factory(Speck.ROCK), 4);
			CellEmitter.center(v.pos).burst(Speck.factory(Speck.BLUE_LIGHT), 4);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.5f, 1.2f);
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
			int cell = ENTRANCE + i;
			if (passable[cell] && Actor.findChar(cell) == null) candidates.add(cell);
		}
		return candidates.isEmpty() ? -1 : Random.element(candidates);
	}

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell( ch );
		if (ch == Dungeon.hero && state == State.START && ch.pos / W >= ROOM_TOP){
			startFight();
		}
	}

	//방에 들어서면: 통로가 막히고 벌레가 땅속(대기 칸)에서 깨어난다
	private void startFight(){
		state = State.FIGHT;
		seal();
		Statistics.qualifiedForBossChallengeBadge = true;
		set( SEAL_CELL, Terrain.WALL );
		GameScene.updateMap( SEAL_CELL );
		CellEmitter.get( SEAL_CELL ).burst( Speck.factory(Speck.ROCK), 6 );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS );
		PixelScene.shake( 1, 0.3f );

		RootGnawer g = new RootGnawer();
		g.pos = WAIT_CELL;
		GameScene.add( g );
		BossHealthBar.assignBoss( g );
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w( Messages.get(RootGnawer.class, "notice") );
		Dungeon.observe();

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() { Music.INSTANCE.play(Assets.Music.CAVES_BOSS, true); }
		});
	}

	//빈 방 바닥 칸 (솟을 자리 등)
	public boolean freeFloor( int c ){
		return isRoomFloor(c) && passable[c] && !solid[c] && Actor.findChar(c) == null;
	}

	// ===== 이겼을 때 =====

	public void onGnawerDeath(){
		state = State.WON;
		unseal();
		boolean seen = false;
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				if (MAP[y].charAt(x) != 'p') continue;
				int c = x + y * W;
				set(c, Terrain.EMPTY);
				GameScene.updateMap(c);
				if (heroFOV[c]){ CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3); seen = true; }
			}
		}
		set(SEAL_CELL, Terrain.EMPTY);
		GameScene.updateMap(SEAL_CELL);
		cleanWalls();
		GameScene.updateMap();
		if (seen){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(1, 0.3f);
		}
		int exit = find('Q');
		set(exit, Terrain.EXIT);
		GameScene.updateMap(exit);
		transitions.add(new LevelTransition(this, exit, LevelTransition.Type.REGULAR_EXIT));
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

	private static final String STATE = "maze_state";
	private static final String TRAIL = "maze_trail";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( STATE, state );
		bundle.put( TRAIL, trail );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		state = bundle.getEnum( STATE, State.class );
		if (bundle.contains(TRAIL)) trail = bundle.getIntArray(TRAIL);
	}
}
