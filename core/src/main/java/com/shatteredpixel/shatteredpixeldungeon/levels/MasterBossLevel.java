/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.HeartValve;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MemoryLamp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.TowerMaster;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (O1): 25층 탑의 주인 "탑의 심장" 보스층 (기획 docs/boss25_heart_plan.md). 원작 요그-드제 방(HallsBossLevel)을 대신한다 (MazeBosses.ENABLED·O_MASTER).
//15×15 둥근 방. 가운데 세계수 어린 줄기(T, 원작 석상 칸: 지나갈 수 없고 조준선을 막음, 시야는 막지 않음, 공격할 수 없음).
//방 가장자리 정삼각형 자리에 심장 판막 셋(1 수액 8시 / 2 등불 4시 / 3 그 판의 20층 보스 12시). 깬 판막 수만큼 줄기가 자란다(그림 단계 0~3).
//주인공이 아래(6시) 입구로 방에 들어서면(9~22줄) 뒤가 막히고 주인이 줄기 바로 아래(g)에 나온다. 이기면 2시 방향 벽이 무너지며 26층 계단 방이 열린다.
//지도 설계도: docs/sprites/boss25/make_map25.ps1 (글자 지도 map25.txt, 그림 map25_preview.png)
public class MasterBossLevel extends Level {

	{
		TowerMaster.clearCurrent(); //(점검) 이 층을 만들거나 불러올 때 예전 판의 주인을 잊는다
		color1 = 0x801500;
		color2 = 0xa68521;
		viewDistance = 12; //(제안) 보스방이 한눈에 읽히게 (원작 25층의 4칸 시야를 쓰지 않음)
	}

	public enum State { START, FIGHT, WON }
	private State state = State.START;

	private static final int W = 32, H = 32;

	//W 벽 . 바닥 E 입구 D 문 X 싸우는 동안 막히는 칸 T 세계수 어린 줄기 g 주인 시작 h 주인공이 들어서는 자리
	//1·2·3 심장 판막 v 줄기에서 판막으로 뻗은 검은 뿌리(바닥) t 보스 횃불 자리 L 기억의 등불 자리 (O2에서 씀)
	//p·r·Q 이기면 열리는 길·방·26층 계단
	private static final String[] MAP = {
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWrrrrWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWrrQrWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWrrrrWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWrrrrWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWpWWWWWWW",
		"WWWWWWWWWWWWW..3..WWWWWpWWWWWWWW",
		"WWWWWWWWWWW..L.v....WWpWWWWWWWWW",
		"WWWWWWWWWW.....v.....pWWWWWWWWWW",
		"WWWWWWWWW......v......WWWWWWWWWW",
		"WWWWWWWWW......v......WWWWWWWWWW",
		"WWWWWWWW..t....v.......WWWWWWWWW",
		"WWWWWWWW.......v.......WWWWWWWWW",
		"WWWWWWWW.......T.......WWWWWWWWW",
		"WWWWWWWW.....vv.vv.....WWWWWWWWW",
		"WWWWWWWW....v..g..v....WWWWWWWWW",
		"WWWWWWWWW.vv.......vv.WWWWWWWWWW",
		"WWWWWWWWW1...........2WWWWWWWWWW",
		"WWWWWWWWWW...........WWWWWWWWWWW",
		"WWWWWWWWWWW.t..h....WWWWWWWWWWWW",
		"WWWWWWWWWWWWW.....WWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWXWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWW.WWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWDWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWW...WWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWW.E.WWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWW...WWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW",
		"WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
	};

	private static final int ROOM_TOP = 8, ROOM_BOTTOM = 22; //방의 줄 (들어서면 싸움이 시작된다)

	public static int find( char ch ){
		for (int y = 0; y < H; y++){ int x = MAP[y].indexOf(ch); if (x >= 0) return x + y * W; }
		return -1;
	}

	public static final int SAPLING = 15 + 15 * W;
	public static final int MASTER_START = 15 + 17 * W;
	private static final int SEAL_CELL = 15 + 23 * W;

	public State state(){ return state; }

	//세계수 어린 줄기 칸인지 (원작 석상 그림 대신 MasterBossLevel.Sapling이 따로 그린다: DungeonTerrainTilemap·DungeonWallsTilemap)
	public static boolean saplingCell( int cell ){
		return cell == SAPLING && Dungeon.level instanceof MasterBossLevel;
	}

	//방 바닥인지 (입구·통로·이기면 열리는 길 제외)
	public boolean isRoomFloor( int c ){
		if (c < 0 || c >= W * H) return false;
		int y = c / W;
		return y >= ROOM_TOP && y <= ROOM_BOTTOM && c != SAPLING && passable[c] && map[c] != Terrain.DOOR;
	}

	//깬 판막 수 = 줄기의 자란 단계 (0~3)
	public int brokenValves(){
		int n = 0;
		for (HeartValve v : valves()) if (v.broken && !v.absorbed) n++; //(O6) 주인이 흡수한 판막은 세지 않는다
		return Math.min(3, n);
	}

	@Override
	public void playLevelMusic() {
		if (state == State.FIGHT){
			Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
		} else if (state == State.START || Statistics.amuletObtained){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(HallsLevel.HALLS_TRACK_LIST, HallsLevel.HALLS_TRACK_CHANCES, false);
		}
	}

	@Override
	public String tilesTex() { return com.shatteredpixel.shatteredpixeldungeon.MazeTower.HEART_NEW_FLOOR ? Assets.Environment.TILES_HALLS2 : Assets.Environment.TILES_HALLS; } //Maze Tower (사용자): 21~24층과 같은 새 바닥·벽

	@Override
	public String waterTex() { return Assets.Environment.WATER_HALLS; }

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				int t;
				switch (MAP[y].charAt(x)){
					case 'W': case 'p': case 'r': case 'Q': t = Terrain.WALL; break;
					case 'E': t = Terrain.ENTRANCE; break;
					case 'D': t = Terrain.DOOR; break;
					case 'T': t = Terrain.STATUE; break;
					default: t = Terrain.EMPTY; break;
				}
				map[x + y * W] = t;
			}
		}
		//바닥 장식 조금 (방 안만, 판막·줄기 칸 제외)
		for (int y = ROOM_TOP; y <= ROOM_BOTTOM; y++){
			for (int x = 0; x < W; x++){
				char ch = MAP[y].charAt(x);
				if ((ch == '.' || ch == 'v') && Random.Int(8) == 0) map[x + y * W] = Terrain.EMPTY_DECO;
			}
		}
		int entrance = find('E');
		transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));
		return true;
	}

	//판막 셋: 1 수액(8시) / 2 등불(4시) / 3 그 판에서 싸운 20층 보스(망령 왕 또는 여제, 12시)
	@Override
	protected void createMobs() {
		addValve(find('1'), HeartValve.SAP);
		addValve(find('2'), HeartValve.LANTERN);
		boolean empress = MazeBosses.empress();
		HeartValve top = addValve(find('3'), empress ? HeartValve.PETAL : HeartValve.DARK);
		if (!empress){
			//(O2, 사용자) 망령 왕 판: 어둠 속 무작위 자리에 기억의 등불 하나
			int at = lampSpot(top, -1);
			if (at != -1){ MemoryLamp lamp = new MemoryLamp(); lamp.pos = at; mobs.add(lamp); }
		}
		rebuildValves();
	}

	// ===== (O2) 판막 깨기를 돕는 방 =====

	//어둠 판막의 어둠이 지금 덮여 있는지 (깨지지 않았고 기억의 등불이 꺼져 있으면)
	//(점검) 화면 쪽(밀치기 콜백의 Dungeon.observe 등)에서도 불려, 그사이 행동 쪽이 몹을 넣고 빼면 튕길 수 있어 그때는 지난 값을 쓴다
	private volatile boolean lastDarkActive = false;
	public boolean darkActive(){
		HeartValve v = valveOf(HeartValve.DARK);
		if (v == null || v.broken) return lastDarkActive = false;
		try {
			for (Mob m : mobs) if (m instanceof MemoryLamp && ((MemoryLamp) m).lit) return lastDarkActive = false;
		} catch (java.util.ConcurrentModificationException e){
			return lastDarkActive;
		}
		return lastDarkActive = true;
	}

	//판막 셋 (화면 그리기 쪽에서 매 프레임 mobs를 돌면 행동 쪽이 몹을 넣고 뺄 때 충돌해 튕길 수 있어서 따로 들고 있음.
	//층을 만들 때·불러올 때 행동 쪽에서 채우고, 주인이 쓰러지면 비움)
	private volatile HeartValve[] valveCache = null;
	public HeartValve[] valves(){
		HeartValve[] v = valveCache;
		if (v == null){ rebuildValves(); v = valveCache; }
		return v;
	}
	private void rebuildValves(){
		ArrayList<HeartValve> a = new ArrayList<>();
		for (Mob m : mobs.toArray(new Mob[0])) if (m instanceof HeartValve) a.add((HeartValve) m);
		valveCache = a.toArray(new HeartValve[0]);
	}

	public HeartValve valveOf( int kind ){
		for (HeartValve v : valves()) if (v.kind == kind) return v;
		return null;
	}

	//(사용자) 판막 범위: 판막 둘레 둥글게 r칸 안인지 (가로·세로 r칸, 대각선은 조금 덜)
	public static boolean inRadius( int cell, int center, int r ){
		float dx = cell % W - center % W, dy = cell / W - center / W;
		return dx * dx + dy * dy <= (r + 0.5f) * (r + 0.5f);
	}

	//(사용자) 판막을 칠 수 있는 범위(O_VALVE_ZONE칸) 안인지
	public static boolean inZone( int cell, HeartValve v ){
		return v != null && inRadius(cell, v.pos, com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_VALVE_ZONE);
	}

	//어둠 판막 둘레(둥글게 O_DARK_RADIUS칸)의 어둠 자리인지 (벽 제외)
	public boolean darkZone( int cell, HeartValve v ){
		if (v == null || map[cell] == Terrain.WALL) return false;
		return inRadius(cell, v.pos, com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_DARK_RADIUS);
	}

	//어둠 칸인지 (기억의 등불은 주인공이 바로 옆일 때만 보이고 칠 수 있다)
	private boolean inDark( int cell, HeartValve v ){
		if (!darkZone(cell, v)) return false;
		//(사용자) 기억의 등불도 어둠에 가려지고, 주인공이 바로 옆 칸(대각선 포함)에 있을 때만 보인다 (멀리서 위치가 다 보이지 않게)
		Char c = Actor.findChar(cell);
		if (c instanceof MemoryLamp) return Dungeon.hero == null || !adjacent(cell, Dungeon.hero.pos);
		return true;
	}

	//주인공이 지금 어둠 속에 있는지 (주인이 기습한다)
	public boolean heroInDark(){
		return darkActive() && Dungeon.hero != null && darkZone(Dungeon.hero.pos, valveOf(HeartValve.DARK));
	}

	//(사용자) 어둠 속 무작위 자리 (판막 바로 옆·지난 자리 2칸 안·누가 있는 칸 제외). 없으면 -1
	private int lampSpot( HeartValve v, int old ){
		ArrayList<Integer> cand = new ArrayList<>();
		for (int c = 0; c < length(); c++){
			if (!darkZone(c, v) || c == v.pos || !isRoomFloor(c) || adjacent(c, v.pos)) continue;
			if (Actor.findChar(c) != null) continue;
			boolean taken = false;
			for (Mob m : mobs) if (m.pos == c) taken = true;
			if (taken) continue;
			if (old != -1 && distance(c, old) <= 2) continue;
			cand.add(c);
		}
		return cand.isEmpty() ? -1 : Random.element(cand);
	}

	//(사용자) 켜졌던 기억의 등불이 다시 꺼지면: 그 등불은 사라지고 어둠 속 다른 자리에 새 등불이 생긴다 (MemoryLamp.act)
	public void respawnLamp( MemoryLamp old ){
		HeartValve v = valveOf(HeartValve.DARK);
		int was = old.pos;
		old.die(null);
		if (v == null || v.broken) return;
		int at = lampSpot(v, was);
		if (at == -1) at = lampSpot(v, -1);
		if (at == -1) return;
		MemoryLamp lamp = new MemoryLamp();
		lamp.pos = at;
		GameScene.add(lamp);
	}

	//(사용자) 완전한 어둠: 지나온 칸·옆 칸도 탐색되지 않는다 (원작 Dungeon.observe는 주인공 둘레 칸을 늘 본 적 있음으로 하고
	//안개를 주인공 근처만 다시 그려 지나온 자리에 회색 자국이 남았다. 20층 망령 왕의 어둠과 같은 처리)
	public void afterObserve(){
		if (!darkActive()) return;
		HeartValve v = valveOf(HeartValve.DARK);
		for (int c = 0; c < length(); c++){
			if (!inDark(c, v)) continue;
			visited[c] = heroFOV[c] && c != darkEyeCell;
			mapped[c] = false;
		}
		GameScene.updateFog();
	}

	//기습하려는 푸른 눈 칸: 칠 수 있게 시야에 넣되 밝히지 않는다 (FogOfWar)
	public int darkEyeCell = -1;

	//(O2) 어둠 판막: 어둠 칸은 주인공에게 보이지 않는다(자기 칸만). 본 적 있음도 지워 안개가 완전히 검다
	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView(c, fieldOfView);
		if (c != Dungeon.hero) return;
		darkEyeCell = -1;
		if (!darkActive()) return;
		HeartValve v = valveOf(HeartValve.DARK);
		for (int i = 0; i < fieldOfView.length; i++){
			if (i == c.pos || !inDark(i, v)) continue;
			//(사용자) 주인의 기습: 푸른 눈 칸은 칠 수 있게 시야에 넣되 밝히지 않는다 (눈 그림만 안개 위에)
			if (Actor.findChar(i) instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithEyes && adjacent(i, c.pos)){
				fieldOfView[i] = true;
				darkEyeCell = i;
				continue;
			}
			fieldOfView[i] = false;
			visited[i] = false;
			mapped[i] = false;
		}
	}

	//(O2) 판막을 깼다: 깬 수만큼 줄기 둘레가 새잎 땅이 된다 (반지름 O_SPROUT_RINGS, 방 바닥만)
	public void onValveBroken( HeartValve v ){
		//(사용자) 한꺼번에 깔지 않고: 빛이 줄기 밑동에 닿을 때부터 줄기에 가까운 칸부터 한 칸씩 차례로 돋아 퍼진다 (O_SPROUT_STEP초 간격)
		ArrayList<Integer> cells = missingSprouts();
		float start = Game.timeTotal + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_ROOT_FLOW_DELAY + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_ROOT_FLOW_TIME;
		for (int i = 0; i < cells.size(); i++){
			sproutQueue.add(new float[]{ cells.get(i), start + i * com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_SPROUT_STEP });
		}
	}

	//깬 판막 수만큼의 새잎 땅 자리 중 아직 새잎 땅이 아닌 칸 (줄기에 가까운 차례, 같은 거리는 무작위)
	private ArrayList<Integer> missingSprouts(){
		ArrayList<Integer> out = new ArrayList<>();
		float r = sproutRadius();
		if (r < 0) return out;
		int sx = SAPLING % W, sy = SAPLING / W;
		final java.util.HashMap<Integer, Float> dist = new java.util.HashMap<>();
		for (int c = 0; c < length(); c++){
			if (!isRoomFloor(c) || Actor.findChar(c) instanceof HeartValve) continue;
			float dx = c % W - sx, dy = c / W - sy;
			float d2 = dx * dx + dy * dy;
			if (d2 > r * r + 0.01f) continue;
			if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.at(this, c)) continue;
			boolean queued = false;
			for (float[] q : sproutQueue) if ((int) q[0] == c) queued = true;
			if (queued) continue;
			out.add(c);
			dist.put(c, (float)Math.sqrt(d2) + Random.Float(0.6f));
		}
		java.util.Collections.sort(out, (a, b) -> Float.compare(dist.get(a), dist.get(b)));
		return out;
	}

	//(O4, 사용자) 판막이 다시 닫혔다: 세계수가 한 단계 줄고(brokenValves가 줄어 Sapling이 따라감), 넓어졌던 새잎 땅의 바깥 고리는 다시 검은 낙엽으로 덮인다.
	//어둠 판막이면 어둠 속에 기억의 등불이 없을 때 하나 다시 놓는다
	public void onValveRestored( HeartValve v ){
		int n = brokenValves();
		float r = n <= 0 ? -1f : com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_SPROUT_RINGS[Math.min(n, com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_SPROUT_RINGS.length) - 1];
		int sx = SAPLING % W, sy = SAPLING / W;
		//(사용자) 줄어든 고리 밖의 새잎 땅이 바깥 칸부터 한 칸씩 검은 낙엽으로 덮여 간다 (화면 연출, 불러오면 sproutNow가 바로 정리)
		final java.util.HashMap<Integer, Float> dist = new java.util.HashMap<>();
		ArrayList<Integer> out = outsideSprouts(r);
		for (int c : out){ float dx = c % W - sx, dy = c / W - sy; dist.put(c, (float)Math.sqrt(dx * dx + dy * dy) + Random.Float(0.6f)); }
		java.util.Collections.sort(out, (a, b) -> Float.compare(dist.get(b), dist.get(a)));
		float start = Game.timeTotal + 0.3f;
		for (int i = 0; i < out.size(); i++) witherQueue.add(new float[]{ out.get(i), start + i * com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_WITHER_STEP });
		for (float[] q : sproutQueue){ //(점검) 목록의 한 순간 모습을 돌므로 그림 쪽이 넣고 빼도 튕기지 않는다
			int c = (int) q[0];
			float dx = c % W - sx, dy = c / W - sy;
			if (r < 0 || dx * dx + dy * dy > r * r + 0.01f) sproutQueue.remove(q);
		}
		if (heroFOV[SAPLING]){
			CellEmitter.get(SAPLING).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 10);
			CellEmitter.get(SAPLING - W).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 8);
		}
		if (v.kind == HeartValve.DARK){
			boolean has = false;
			for (Mob m : mobs) if (m instanceof MemoryLamp && m.isAlive()) has = true;
			int at = has ? -1 : lampSpot(v, -1);
			if (at != -1){ MemoryLamp lamp = new MemoryLamp(); lamp.pos = at; GameScene.add(lamp); }
		}
	}

	//차례를 기다리는 새잎 땅 칸 {칸, 돋는 시각(Game.timeTotal)}. 화면 연출이라 저장하지 않는다 (불러오면 sproutNow로 다 깔림)
	private static final java.util.concurrent.CopyOnWriteArrayList<float[]> sproutQueue = new java.util.concurrent.CopyOnWriteArrayList<>(); //(점검) 그림 쪽과 행동 쪽이 같이 만지므로

	private void sproutAt( int c, boolean inScene ){
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.at(this, c)) return;
		if (inScene) GameScene.add(Blob.seed(c, 1, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.class));
		else Blob.seed(c, 1, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.class, this); //화면을 만드는 중에는 레벨에만 (그림은 화면이 블롭을 올릴 때)
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves b = (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves) blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.class);
		if (b != null && b.cur != null) b.clear(c);
	}

	//매 프레임 (RootLayer): 차례가 된 칸에 새잎 땅이 돋는다 / 다시 닫혀 줄어든 칸이 검은 낙엽으로 덮인다
	private void tickSprouts(){
		for (float[] q : sproutQueue){ //(점검) 목록의 한 순간 모습을 돌므로 행동 쪽이 넣고 빼도 튕기지 않는다
			if (q[1] > Game.timeTotal || !sproutQueue.remove(q)) continue;
			sproutAt((int) q[0], true);
		}
		for (float[] q : witherQueue){
			if (q[1] > Game.timeTotal || !witherQueue.remove(q)) continue;
			witherAt((int) q[0], true);
		}
	}

	//다시 닫혀 줄어드는 새잎 땅 칸 {칸, 덮이는 시각}. 화면 연출이라 저장하지 않는다
	private static final java.util.concurrent.CopyOnWriteArrayList<float[]> witherQueue = new java.util.concurrent.CopyOnWriteArrayList<>();

	//지금 깬 판막 수의 고리(반지름 r, 음수면 없음) 밖에 남은 새잎 땅 칸
	private ArrayList<Integer> outsideSprouts( float r ){
		ArrayList<Integer> out = new ArrayList<>();
		Blob sg = blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.class);
		if (sg == null || sg.cur == null) return out;
		int sx = SAPLING % W, sy = SAPLING / W;
		for (int c = 0; c < length(); c++){
			if (sg.cur[c] <= 0) continue;
			float dx = c % W - sx, dy = c / W - sy;
			if (r < 0 || dx * dx + dy * dy > r * r + 0.01f) out.add(c);
		}
		return out;
	}

	private float sproutRadius(){

		if (bloomed) return finaleSproutR; //(O7) 주인이 쓰러진 뒤 판막이 사라져도 새잎 땅은 그대로
		int n = brokenValves();
		return n <= 0 ? -1f : com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_SPROUT_RINGS[Math.min(n, com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_SPROUT_RINGS.length) - 1];
	}

	//새잎 땅 한 칸이 시들어 검은 낙엽으로 (그 사이 다시 넓어졌으면 그대로 둔다)
	private void witherAt( int c, boolean inScene ){
		float r = sproutRadius();
		int sx = SAPLING % W, sy = SAPLING / W;
		float dx = c % W - sx, dy = c / W - sy;
		if (r >= 0 && dx * dx + dy * dy <= r * r + 0.01f) return;
		Blob sg = blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.class);
		if (sg == null || sg.cur == null || sg.cur[c] <= 0) return;
		sg.clear(c);
		if (inScene){
			GameScene.add(Blob.seed(c, 1, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.class));
			if (heroFOV[c]) CellEmitter.get(c).burst(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.StepLeaf.FACTORY, 3);
		} else {
			Blob.seed(c, 1, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.class, this);
		}
	}

	//새 화면 (불러온 뒤 등): 기다리던 칸과 빠진 칸을 연출 없이 바로 깔고, 고리 밖에 남은 칸은 바로 낙엽으로
	private void sproutNow(){
		sproutQueue.clear();
		witherQueue.clear();
		for (int c : outsideSprouts(sproutRadius())) witherAt(c, false);
		for (int c : missingSprouts()) sproutAt(c, false);
		//(점검) 새잎 땅·검은 낙엽 블롭을 미리 만들어 둔다: 매 프레임(화면 쪽) 돋고 시드는 칸이 처음으로 블롭을 만들면
		// 행동 쪽이 level.blobs를 도는 중에 새 항목이 생겨 튕길 수 있다 (있는 블롭에 양 0을 심으면 아무것도 바뀌지 않음)
		Blob.seed(SAPLING, 0, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.class, this);
		Blob.seed(SAPLING, 0, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.class, this);
		//(점검) 화면을 만드는 중에 새로 생긴 바닥 블롭도 행동 목록에 넣는다
		Blob sg = blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.class); if (sg != null) Actor.add(sg);
		Blob bl = blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.class); if (bl != null) Actor.add(bl);
	}

	// ===== (O5, 사용자) 검은 낙엽 =====

	//주인공이 마지막으로 낙엽을 밟아 바스락 소리를 낸 칸 (낙엽 걸음이 그곳으로 온다, BlackLeaves.onStep)
	public int rustleCell = -1;

	//한 칸에 검은 낙엽 (방 바닥만, 새잎 땅·줄기·판막 칸 제외)
	public void layLeaves( int c ){
		if (c < 0 || c >= length() || !isRoomFloor(c) || Actor.findChar(c) instanceof HeartValve) return;
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.at(this, c)) return;
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.at(c)) return;
		GameScene.add(Blob.seed(c, 1, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.class));
	}

	//낙엽이 흩날리는 연출 (순간이동·낙엽 걸음·낙엽 바람)
	public void leafBurst( int c, int n ){
		if (c >= 0 && c < length() && heroFOV[c]) CellEmitter.get(c).burst(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.StepLeaf.FACTORY, n);
	}

	private HeartValve addValve( int cell, int kind ){
		HeartValve v = new HeartValve();
		v.kind = kind;
		v.pos = cell;
		mobs.add(v);
		return v;
	}

	@Override
	public Actor addRespawner() { return null; }

	//(결정) 보스층에는 이전 영웅의 유해가 나오지 않는다 (Bones.get). (사용자) 수액 판막용 횃불도 두지 않는다
	@Override
	protected void createItems() {}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = entrance() + i;
			if (passable[cell] && Actor.findChar(cell) == null) candidates.add(cell);
		}
		return candidates.isEmpty() ? -1 : Random.element(candidates);
	}

	private int lastHeroPos = -1;
	public int lastHeroStep = 0;

	@Override
	public void occupyCell( Char ch ) {
		super.occupyCell( ch );
		if (ch == Dungeon.hero){
			int prev = lastHeroPos;
			lastHeroPos = ch.pos;
			if (prev != -1 && prev != ch.pos && adjacent(prev, ch.pos)) lastHeroStep = ch.pos - prev; //주인의 기습: 주인공이 움직인 방향 (뒤에서 덮침)
		}
		//(O6, 사용자) 익은 열매 칸에 먼저 들어선 쪽이 먹는다
		if (state == State.FIGHT && (ch == Dungeon.hero || ch instanceof TowerMaster)){
			TowerMaster tm = TowerMaster.find();
			if (tm != null) tm.onEnterCell(ch);
		}
		if (ch == Dungeon.hero && state == State.START){
			int y = ch.pos / W;
			if (y >= ROOM_TOP && y <= ROOM_BOTTOM) startFight();
		}
	}

	//방에 들어서면: 뒤(입구 쪽 칸)가 막히고 주인이 줄기 바로 아래에 나온다
	private void startFight(){
		state = State.FIGHT;
		seal();
		Statistics.qualifiedForBossChallengeBadge = true;
		set( SEAL_CELL, Terrain.WALL );
		GameScene.updateMap( SEAL_CELL );
		//(사용자) 막히는 칸에 서 있거나 방 밖에 남은 펫·동료는 주인공 곁으로 옮긴다 (20층 보스방과 같은 원작 방식)
		Mob.holdAllies( this, Dungeon.hero.pos );
		Mob.restoreAllies( this, Dungeon.hero.pos, MASTER_START );
		CellEmitter.get( SEAL_CELL ).burst( ShadowParticle.UP, 15 );
		Sample.INSTANCE.play( Assets.Sounds.ROCKS, 0.6f );

		TowerMaster boss = new TowerMaster();
		boss.pos = MASTER_START;
		Char there = Actor.findChar(boss.pos);
		if (there != null){
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8){
				int c = boss.pos + i;
				if (Actor.findChar(c) == null && passable[c] && isRoomFloor(c)) candidates.add(c);
			}
			if (!candidates.isEmpty()){
				int to = Random.element(candidates);
				Actor.add(new Pushing(there, there.pos, to));
				there.pos = to;
			}
		}
		GameScene.add( boss );
		boss.beginFight();
		Dungeon.observe();

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() { Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true); }
		});
	}

	// ===== 이겼을 때 =====

	public void onMasterDeath(){
		state = State.WON;
		unseal();
		boolean seen = false;
		for (int y = 0; y < H; y++){
			for (int x = 0; x < W; x++){
				char ch = MAP[y].charAt(x);
				if (ch != 'p' && ch != 'r' && ch != 'Q') continue;
				int c = x + y * W;
				set(c, Terrain.EMPTY);
				GameScene.updateMap(c);
				if (heroFOV[c]){ CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3); seen = true; }
			}
		}
		set(SEAL_CELL, Terrain.EMPTY);
		GameScene.updateMap(SEAL_CELL);
		int exit = find('Q');
		set(exit, Terrain.EXIT);
		GameScene.updateMap(exit);
		transitions.add(new LevelTransition(this, exit, LevelTransition.Type.REGULAR_EXIT));
		cleanWalls();
		GameScene.updateMap();
		if (seen) Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		PixelScene.shake(1, 0.3f);
		Dungeon.observe();
		startFinale();
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut(5f, new Callback() {
					@Override
					public void call() { Music.INSTANCE.play(Assets.Music.THEME_FINALE, true); }
				});
			}
		});
	}

	// ===== (O7) 마무리: 줄기 둘레가 정화된다 =====
	//(사용자) 주인을 쓰러뜨리면 턴이 멈추고(FinaleHold) 옅은 새잎빛 번쩍임·흔들림 1픽셀. 주인이 완전히 쓰러진 뒤(등 가지가 마르고 옹이가 빠져 굴러감, 쓰러짐 그림 9~12칸)
	//줄기에 새잎빛이 모이고, 줄기 둘레의 바닥이 물결처럼 정화되어(역행 바닥 그림, PurifyLayer) 깔린 새잎 땅보다 1~2칸 더 퍼진다. 줄기는 끝까지 자란다. 저장됨(불러오면 다 된 모습)
	public boolean bloomed = false;

	private void startFinale(){
		if (bloomed) return;
		bloomed = true;
		//(사용자) 정화는 지금 깔린 새잎 땅(깬 판막 수만큼)보다 O_PURIFY_EXTRA칸 더. 새잎 땅은 새로 깔지 않는다
		float sr = sproutRadius();
		finaleSproutR = sr;
		purifyR = Math.max(com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_MIN_R, Math.max(0, sr) + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_EXTRA);
		removeValves();
		//(사용자) 주인이 완전히 쓰러진 뒤(쓰러짐 그림 O_FINALE_FALL초) 줄기에 새잎빛이 모이고(O_FINALE_GATHER초), 그다음 정화 물결이 퍼진다
		fallEnd = Game.timeTotal + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_FALL;
		purifyStart = fallEnd + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GATHER;
		gathered = false;
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(Messages.get(this, "bloom"));
		//쓰러지는 순간: 옅은 새잎빛 번쩍임, 흔들림 1픽셀 (짧게)
		GameScene.flash(0x30401C);
		PixelScene.shake(1, 0.35f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 0.7f);
		float dur = com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_FALL + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GATHER
				+ com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_TIME + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_FADE + 0.6f;
		Actor.add(new FinaleHold(dur));
	}

	// ----- (O7, 사용자) 정화: 줄기 둘레의 바닥이 "탑의 심장 역행" 바닥 그림으로 바뀌어 퍼진다 -----
	public float purifyR = 0;                    //정화 반지름 (칸, 저장됨)
	private float finaleSproutR = -1;            //쓰러질 때의 새잎 땅 반지름 (저장됨, 그 뒤 판막이 사라져도 그대로)

	//(사용자) 주인이 죽으면 판막도 모두 사라진다 (기억의 등불·기습 눈도). 그림자 넋과 함께 서서히 흐려진다
	private void removeValves(){
		for (Mob m : mobs.toArray(new Mob[0])){
			if (!(m instanceof HeartValve) && !(m instanceof MemoryLamp) && !(m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithEyes)) continue;
			if (m instanceof HeartValve){ ((HeartValve) m).broken = true; ((HeartValve) m).updateLight(); } //등불의 빛을 거둠
			if (heroFOV[m.pos]) CellEmitter.get(m.pos).burst(ShadowParticle.UP, 10);
			final com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite s = m.sprite;
			m.destroy();
			if (s != null && s.parent != null) s.parent.add(new com.watabou.noosa.tweeners.AlphaTweener(s, 0, 1f){ @Override protected void onComplete(){ super.onComplete(); s.killAndErase(); } });
			else if (s != null) s.killAndErase();
		}
		valveCache = new HeartValve[0];
		Dungeon.observe();
		GameScene.updateFog();
	}
	private static float fallEnd = -1, purifyStart = -1; //화면 연출 시각 (Game.timeTotal). -1이면 연출 없이 다 된 모습
	private static boolean gathered = false;

	//칸마다 정해진 들쭉날쭉함 (정화된 자리의 가장자리가 둥근 원이 아니라 자연스럽게, 미리보기 anim_finale.html과 같은 식)
	private static float jitter( int x, int y ){
		double h = Math.sin(x * 12.9898 + y * 78.233) * 43758.5453;
		return (float)((h - Math.floor(h) - 0.5) * 0.9);
	}

	//이 칸의 정화 진하기 단계 (0 없음 / 1 옅게 / 2 반 / 3 다), now < 0이면 다 된 모습
	private int purifyStage( int c, float now ){
		if (!bloomed || !isRoomFloor(c) && c != SAPLING) return 0;
		int x = c % W, y = c / W, sx = SAPLING % W, sy = SAPLING / W;
		float d = (float)Math.hypot(x - sx, y - sy) + jitter(x, y);
		if (d > purifyR) return 0;
		int max = d > purifyR - 0.7f ? 1 : d > purifyR - 1.5f ? 2 : 3; //(사용자) 가장자리 두 겹은 옅게 남는다
		if (now < 0 || purifyStart < 0) return max;
		float reach = purifyStart + Math.max(0, d) / purifyR * com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_TIME;
		if (now < reach) return 0;
		float a = Math.min(1f, (now - reach) / com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_FADE);
		int st = a >= 0.99f ? 3 : a >= 0.66f ? 2 : 1;
		return Math.min(st, max);
	}

	//바닥 그림 층 셋(진하기 0.35·0.7·1)과 매 프레임 칸마다 단계를 맞추는 일
	public class PurifyLayer extends Group {
		private final com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap[] maps = new com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap[3];
		PurifyLayer(){
			com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap.stage = new int[length()];
			//(점검) 예전 마무리 연출의 시각이 남아 있으면 (다른 판·다시 들어온 층) 연출 없이 다 된 모습으로
			if (!bloomed || Game.timeTotal > purifyStart + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_TIME + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_PURIFY_FADE + 1f){ purifyStart = -1; fallEnd = -1; gathered = true; }
			for (int i = 0; i < 3; i++){ maps[i] = com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap.create(i); add(maps[i]); }
			if (bloomed && purifyStart < 0){ //불러온 뒤: 다 된 모습
				for (int c = 0; c < length(); c++) com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap.stage[c] = purifyStage(c, -1);
				for (com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap m : maps) m.updateMap();
			}
		}
		@Override
		public void update() {
			super.update();
			if (!bloomed || purifyStart < 0) return;
			float now = Game.timeTotal;
			//완전히 쓰러진 뒤: 줄기에 새잎빛이 모인다
			if (!gathered && now >= fallEnd){
				gathered = true;
				if (heroFOV[SAPLING]){
					com.watabou.utils.PointF p = DungeonTilemap.tileCenterToWorld(SAPLING);
					GameScene.effect(new GatherLight()); //(사용자) 미리보기처럼 줄기 둘레의 은은한 둥근 새잎빛 (별 모양 빛 아님)
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.7f, 1.1f);
				}
			}
			int[] st = com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap.stage;
			for (int c = 0; c < st.length; c++){
				int s = purifyStage(c, now);
				if (s == st[c]) continue;
				if (st[c] == 0 && heroFOV[c]) CellEmitter.get(c).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.LEVEL_SPECIFIC, 3);
				st[c] = s;
				for (com.shatteredpixel.shatteredpixeldungeon.tiles.PurifyTilemap m : maps) m.updateMapCell(c);
			}
		}
	}

	//마무리 연출 동안 턴을 멈춘다 (다른 누구보다 먼저 차례를 받아 연출이 끝날 때까지 넘기지 않음)
	public static class FinaleHold extends Actor {
		{ actPriority = VFX_PRIO; }
		private final float dur;
		FinaleHold( float dur ){ this.dur = dur; }
		@Override
		protected boolean act() {
			final FinaleHold self = this;
			Actor.remove(this);
			if (Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return true;
			GameScene.groupEffect(new Group(){
				float t = 0, leaf = 0;
				@Override public void update(){
					super.update();
					t += Game.elapsed;
					leaf += Game.elapsed;
					//방 곳곳에서 새잎이 흩날린다
					if (leaf > 0.08f && Dungeon.level instanceof MasterBossLevel){
						leaf = 0;
						int c = Random.Int(Dungeon.level.length());
						if (((MasterBossLevel) Dungeon.level).isRoomFloor(c) && Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.LEVEL_SPECIFIC, 2);
					}
					if (t >= dur){ killAndErase(); self.next(); }
				}
			});
			return false;
		}
	}

	//원작 25층처럼: 아뮬렛을 들고 내려가는 계단을 밟으면 승천을 묻는다 (마탑은 26층에서 아뮬렛을 고르므로 보통은 쓰이지 않음)
	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.REGULAR_ENTRANCE
				&& hero.belongings.getItem(Amulet.class) != null
				&& hero.buff(AscensionChallenge.class) == null) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndOptions( new ItemSprite(ItemSpriteSheet.AMULET),
							Messages.get(Amulet.class, "ascent_title"),
							Messages.get(Amulet.class, "ascent_desc"),
							Messages.get(Amulet.class, "ascent_yes"),
							Messages.get(Amulet.class, "ascent_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0){
								Buff.affect(hero, AscensionChallenge.class);
								Statistics.highestAscent = 25;
								MasterBossLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;
		}
		return super.activateTransition(hero, transition);
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER: return Messages.get(HallsLevel.class, "water_name");
			case Terrain.STATUE: return Messages.get(MasterBossLevel.class, "sapling_name");
			default: return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc( int tile ) {
		if (tile == Terrain.STATUE) return Messages.get(MasterBossLevel.class, "sapling_desc");
		return super.tileDesc( tile );
	}

	@Override
	public Group addVisuals () {
		super.addVisuals();
		HallsLevel.addHallsVisuals( this, visuals );
		visuals.add(new PurifyLayer());  //(O7) 주인을 쓰러뜨린 뒤 정화된 바닥 (땅 그림 바로 위)
		visuals.add(new RootLayer());    //줄기에서 판막 셋으로 뻗은 검은 뿌리 (바닥 위, 캐릭터 아래)
		//(사용자) 판막마다 칠 수 있는 5칸 범위의 옅은 테두리
		for (Mob m : mobs) if (m instanceof HeartValve) visuals.add(new ValveZone((HeartValve) m));
		visuals.add(new Sapling(false)); //줄기 아랫부분: 캐릭터 아래
		return visuals;
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		wallVisuals.add(new Sapling(true)); //줄기 윗부분: 캐릭터 위 (원작 석상 윗면처럼)
		return wallVisuals;
	}

	// ===== 줄기가 자라는 연출 (사용자: 판막이 깨질 때마다 자라는 것이 잘 보이게) =====
	//판막이 깨지면 (사용자) 입자 없이 뿌리 자체가 판막 쪽부터 줄기 쪽으로 새잎빛으로 빛나며 흘러가고(O_ROOT_FLOW_TIME, RootLayer),
	//빛이 지나간 뿌리는 재로 바랜다. 작은 갈래는 끝에서 갈라진 자리 쪽으로 빛이 모여든다. 둥근 새잎빛(원작 Halo)이 큰 뿌리를 따라 흐른다(FlowLight).
	//빛이 밑동에 닿으면 새 모습이 밑동부터 위로 차오르듯 자란다(O_GROW_TIME초, 자라는 선 위는 아직 예전 모습). 둥근 새잎빛이 자라는 선을 따라 올라가고(GrowLight),
	//다 자라면 천천히 스러진다. 화면에서만 쓰는 상태라 저장하지 않는다(불러오면 지금 단계로 바로 그린다: 깨진 갈래는 재).
	private static int shownStage = -1;     //다 그려진 단계
	private static int growTo = -1;         //자라는 중인 단계
	private static float growStart = -1;    //자라기 시작한 시각 (Game.timeTotal), -1이면 자라는 중 아님
	private static final float[] flowStart = {-1, -1, -1}; //갈래마다 빛이 흐르기 시작한 시각, -1이면 흐르는 중 아님

	//새 화면 (뿌리 층을 만들 때): 지금 단계로 바로 그린다
	private static void resetFx(){
		shownStage = -1; growStart = -1;
		for (int b = 0; b < 3; b++) flowStart[b] = -1;
	}

	private static int targetStage(){
		if (!(Dungeon.level instanceof MasterBossLevel)) return 0;
		MasterBossLevel ml = (MasterBossLevel) Dungeon.level;
		return ml.bloomed && (purifyStart < 0 || Game.timeTotal >= purifyStart) ? 3 : ml.brokenValves(); //(O7) 정화가 시작되면 끝까지 자란다
	}

	//판막 종류 → 뿌리 갈래 (0 수액 8시 / 1 등불 4시 / 2 20층 12시)
	public static int branchOf( int kind ){
		return kind == HeartValve.SAP ? 0 : kind == HeartValve.LANTERN ? 1 : 2;
	}

	private static boolean branchBroken( int b ){
		if (!(Dungeon.level instanceof MasterBossLevel)) return false;
		if (((MasterBossLevel) Dungeon.level).bloomed) return true; //(O7) 주인이 쓰러지면 판막이 사라지고 뿌리는 재
		for (HeartValve v : ((MasterBossLevel) Dungeon.level).valves()) if (v.broken && branchOf(v.kind) == b) return true;
		return false;
	}

	private static float flowTime(){ return com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_ROOT_FLOW_TIME; }

	//빛이 아직 뿌리를 흐르는 중인지 (흐르는 동안은 줄기가 자라지 않고 기다린다)
	private static boolean anyFlowing(){
		for (int b = 0; b < 3; b++) if (flowStart[b] >= 0 && Game.timeTotal < flowStart[b] + flowTime()) return true;
		return false;
	}

	//줄기 그림(16×32)에서 단계마다 나무 꼭대기 줄 (maze_b25_sapling.png에서 잰 값, 그림을 바꾸면 다시 잰다)
	private static final int[] SAPLING_TOP = {6, 4, 2, 0};

	//자라는 단계의 나무 키 (밑동에서 꼭대기까지 픽셀): 차오르는 선과 빛이 여기까지만 올라간다
	private static int growHeight(){
		return 32 - SAPLING_TOP[Math.max(0, Math.min(3, growTo))];
	}

	//(사용자) 자라는 빛이 줄기의 S자 굴곡을 따라 오르게: 그림의 줄기 가운데 x (make_props25.ps1의 TrunkX와 같은 식).
	//줄기는 그림 줄 27(밑동)에서 줄 10 - 2 × 단계(꼭대기)까지. 그 아래(뿌리)·위(잎)는 끝의 x를 쓴다
	private static float trunkWorldX( float worldY ){
		int top = 10 - 2 * Math.max(0, Math.min(3, growTo));
		float imgTop = (SAPLING / W - 1) * DungeonTilemap.SIZE; //그림(16×32)의 맨 위 (줄기 칸 바로 위 칸)
		float row = Math.max(top, Math.min(27, worldY - imgTop));
		double u = (27 - row) / (double)(27 - top);
		double x = 7.4 + 1.3 * Math.sin(u * Math.PI * 1.25 + 0.3) + 0.5; //줄기 픽셀들의 가운데
		return (SAPLING % W) * DungeonTilemap.SIZE + (float)x;
	}

	//자라는 진행도 0~1 (자라는 중이 아니면 -1)
	private static float growProgress(){
		if (growStart < 0) return -1;
		return Math.min(1f, (Game.timeTotal - growStart) / growDur);
	}

	//매 프레임 (두 그림 조각이 같이 부르므로 여러 번 불려도 같은 결과)
	private static void tickGrowth(){
		int target = targetStage();
		if (shownStage < 0) shownStage = target;
		if (growStart >= 0){
			if (growProgress() >= 1f){
				shownStage = growTo;
				growStart = -1;
			}
			return;
		}
		if (target < shownStage){ shownStage = target; return; } //다시 닫혀 줄어듦 (O4에서 다듬음)
		if (target > shownStage && !anyFlowing()) startGrow(target);
	}

	private static void startGrow( int to ){
		//(사용자) 마무리(주인을 쓰러뜨린 뒤)에는 한 번에 끝까지 자라고, 빛은 나무 그림 자체를 타고 오른다. 판막이 깨질 때는 한 단계씩, 둥근 빛
		boolean finale = Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).bloomed;
		growTo = finale ? to : Math.min(to, shownStage + 1);
		growDur = finale ? com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GROW_TIME : com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_GROW_TIME;
		growStart = Game.timeTotal;
		if (Dungeon.level.heroFOV[SAPLING]){
			Sample.INSTANCE.play(Assets.Sounds.PLANT, 0.9f, 0.9f);
			if (finale) GameScene.groupEffect(new BranchGlow()); else GameScene.effect(new GrowLight());
		}
	}

	private static float growDur = com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_GROW_TIME;

	//(사용자) 나무의 줄기·가지 모양 그대로, 은은한 새잎빛이 밑동부터 위로 타고 오른다: 자라는 단계 그림의 줄마다 한 장씩(32줄)
	//새잎빛 한 빛깔로 칠해 빛을 더하는 방식으로 겹치고, 자라는 선 둘레 띠만 진하게(아래로는 옅은 빛이 남음). 다 오르면 스러진다.
	private static class BranchGlow extends Group {
		private final Image[] rows = new Image[32];
		private final int stage;
		private float fade = -1;
		BranchGlow(){
			stage = growTo;
			float x0 = (SAPLING % W) * DungeonTilemap.SIZE;
			float imgTop = (SAPLING / W - 1) * DungeonTilemap.SIZE;
			for (int r = 0; r < 32; r++){
				Image im = new Image("sprites/maze_b25_sapling.png");
				im.frame(im.texture.uvRect(stage * 16, r, stage * 16 + 16, r + 1));
				im.x = x0; im.y = imgTop + r;
				im.tint(0xB4E67A, 1f);
				im.alpha(0);
				rows[r] = im;
				add(im);
			}
			update();
		}
		@Override
		public void update() {
			super.update();
			float p = growProgress();
			float h = growHeight() * (p < 0 ? 1f : p); //밑동에서 빛 띠 가운데까지 (픽셀)
			float k = 1f;
			if (p < 0 || p >= 1f){
				if (fade < 0) fade = 0;
				fade += Game.elapsed;
				k = 1f - fade / com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GLOW_FADE;
				if (k <= 0){ killAndErase(); return; }
			}
			float a = com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GLOW_ALPHA;
			float band = com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GLOW_BAND;
			float trail = com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GLOW_TRAIL;
			for (int r = 0; r < 32; r++){
				float fromBase = 31 - r; //이 줄이 밑동에서 얼마나 위인지
				float d = Math.abs(fromBase - h);
				float v = Math.max(0f, 1f - d / band);
				if (fromBase < h) v = Math.max(v, trail);
				rows[r].alpha(a * v * k);
			}
		}
		@Override
		public void draw() {
			com.watabou.glwrap.Blending.setLightMode();
			super.draw();
			com.watabou.glwrap.Blending.setNormalMode();
		}
	}

	//판막이 깨진 순간 (HeartValve.die): 그 갈래의 뿌리로 새잎빛이 흐르기 시작한다 (O_ROOT_FLOW_DELAY초 뒤)
	public static void startRootFlow( HeartValve valve ){
		int b = branchOf(valve.kind);
		flowStart[b] = Game.timeTotal + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_ROOT_FLOW_DELAY;
		GameScene.effect(new FlowLight(b));
	}

	//칸마다 {방 영역 x, y (칸, 방 영역 왼쪽 위 = (8,8)), t × 1000} (갈래 0 수액 / 1 등불 / 2 20층). t는 줄기 밑동 0 → 판막 1.
	//docs/sprites/boss25/roots25_path.json에서 옮김 (뿌리 그림을 다시 만들면 같이 옮긴다)
	private static final int[][] ROOT_CELLS = {
		{6,7,69, 7,7,15, 2,8,707, 3,8,663, 4,8,394, 5,8,303, 6,8,148, 7,8,48, 3,9,606, 4,9,500, 5,9,427, 2,10,800, 3,10,712, 1,11,975, 2,11,885, 3,11,795, 2,12,912},
		{7,7,11, 8,7,90, 7,8,472, 8,8,294, 9,8,295, 10,8,521, 11,8,632, 9,9,390, 10,9,498, 11,9,599, 10,10,856, 11,10,717, 12,10,827, 12,11,921, 13,11,993},
		{7,0,998, 7,1,903, 6,2,689, 7,2,789, 8,2,937, 6,3,659, 7,3,600, 8,3,954, 7,4,464, 7,5,303, 8,5,328, 7,6,169, 8,6,273, 7,7,33},
	};
	private static final int ROOT_X0 = 8, ROOT_Y0 = 8;
	//큰 뿌리 곡선 위 점 {x, y (방 영역 픽셀), t × 1000}: 빛이 흐를 때 둥근 새잎빛이 따라가는 자리
	private static final int[][] ROOT_PATH = {
		{116,124,0, 115,125,13, 113,125,29, 111,126,48, 109,127,70, 107,128,94, 105,129,119, 102,130,143, 100,132,168, 97,133,194, 95,134,221, 92,135,249, 89,136,277, 86,138,307, 84,139,336, 81,141,366, 78,143,397, 76,145,428, 73,147,461, 70,149,494, 68,151,527, 65,154,559, 62,156,591, 60,158,623, 57,160,654, 55,162,686, 52,165,717, 50,167,748, 47,169,778, 45,171,807, 43,173,834, 41,175,860, 39,177,887, 37,179,912, 36,181,937, 34,183,959, 32,184,978, 31,186,994},
		{124,124,0, 125,124,13, 127,125,29, 129,125,49, 131,126,71, 134,126,94, 136,127,119, 139,128,145, 141,130,170, 144,131,198, 146,133,228, 149,135,259, 152,137,291, 155,139,323, 157,141,355, 160,143,386, 162,145,416, 165,147,447, 167,149,477, 170,152,507, 172,154,537, 174,156,567, 177,158,597, 179,160,626, 182,162,656, 184,164,686, 187,165,716, 190,167,745, 192,168,773, 195,170,801, 197,172,827, 199,174,853, 201,176,880, 203,178,907, 205,180,933, 206,182,957, 208,184,977, 209,186,994},
		{120,120,0, 120,118,15, 120,116,34, 121,114,58, 121,111,84, 121,108,111, 121,105,139, 121,102,166, 121,100,192, 120,97,218, 120,94,244, 119,91,272, 118,89,299, 118,86,326, 117,83,354, 117,80,380, 117,77,408, 118,74,436, 119,71,464, 120,68,493, 121,65,523, 121,62,551, 122,60,579, 122,56,607, 122,54,635, 121,51,663, 121,48,691, 120,45,719, 120,42,747, 119,39,775, 119,36,802, 119,33,831, 119,30,861, 119,26,891, 119,23,921, 120,20,949, 120,18,973, 120,16,993},
	};

	//뿌리 그림 (sprites/maze_b25_roots.png 720×720, 원본 docs/sprites/boss25/make_roots25.ps1): 가로 = 갈래, 세로 = 상태(0 검은 뿌리 / 1 새잎빛이 흐르는 뿌리 / 2 재).
	//평소에는 칸(16×16)마다 그림 하나(검은 뿌리 또는 재). (사용자) 빛이 흐르는 동안에는 칸 단위로 바꾸면 네모나게 깨져 보여서,
	//그 갈래만 4×4 조각(ROOT_BLOCKS)으로 잘게 나눠 세 상태를 겹쳐 두고 진하기만 바꾼다(흐름이 끝나면 조각은 지운다). 본 적 있는 칸에만.
	public static class RootLayer extends Group {
		private final Image[][] cells = new Image[3][];  //갈래 → 칸마다 그림 하나
		private final int[][] cellOf = new int[3][];
		private final Image[][] blocks = new Image[3][]; //갈래 → 조각 × 3상태 (흐르는 동안만)

		RootLayer(){
			resetFx();
			if (Dungeon.level instanceof MasterBossLevel) ((MasterBossLevel) Dungeon.level).sproutNow();
			for (int b = 0; b < 3; b++){
				int n = ROOT_CELLS[b].length / 3;
				cells[b] = new Image[n];
				cellOf[b] = new int[n];
				for (int i = 0; i < n; i++){
					int lx = ROOT_CELLS[b][i * 3], ly = ROOT_CELLS[b][i * 3 + 1];
					cellOf[b][i] = (lx + ROOT_X0) + (ly + ROOT_Y0) * W;
					Image im = new Image("sprites/maze_b25_roots.png");
					im.x = (lx + ROOT_X0) * DungeonTilemap.SIZE;
					im.y = (ly + ROOT_Y0) * DungeonTilemap.SIZE;
					add(im);
					cells[b][i] = im;
				}
			}
			update();
		}

		private static Image piece( int b, int st, int px, int py, int size ){
			Image im = new Image("sprites/maze_b25_roots.png");
			im.frame(im.texture.uvRect(b * 240 + px, st * 240 + py, b * 240 + px + size, st * 240 + py + size));
			im.x = ROOT_X0 * DungeonTilemap.SIZE + px;
			im.y = ROOT_Y0 * DungeonTilemap.SIZE + py;
			return im;
		}

		private void makeBlocks( int b ){
			int n = ROOT_BLOCKS[b].length / 3;
			blocks[b] = new Image[n * 3];
			for (int i = 0; i < n; i++){
				for (int st = 0; st < 3; st++){
					Image im = piece(b, st, ROOT_BLOCKS[b][i * 3] * 4, ROOT_BLOCKS[b][i * 3 + 1] * 4, 4);
					add(im);
					blocks[b][i * 3 + st] = im;
				}
			}
		}

		private void dropBlocks( int b ){
			if (blocks[b] == null) return;
			for (Image im : blocks[b]) im.killAndErase();
			blocks[b] = null;
		}

		private static boolean seen( int px, int py ){
			int c = (ROOT_X0 + px / 16) + (ROOT_Y0 + py / 16) * W;
			return Dungeon.level.visited[c] || Dungeon.level.mapped[c];
		}

		@Override
		public void update() {
			super.update();
			if (!(Dungeon.level instanceof MasterBossLevel)) return;
			float now = Game.timeTotal, flow = flowTime();
			((MasterBossLevel) Dungeon.level).tickSprouts();
			for (int b = 0; b < 3; b++){
				boolean broken = branchBroken(b);
				float start = flowStart[b];
				if (start >= 0 && now > start + flow + 0.4f){ flowStart[b] = -1; start = -1; }
				boolean flowing = broken && start >= 0;

				//칸 그림: 흐르는 동안은 숨기고, 아니면 검은 뿌리 또는 재
				for (int i = 0; i < cells[b].length; i++){
					Image im = cells[b][i];
					int lx = ROOT_CELLS[b][i * 3], ly = ROOT_CELLS[b][i * 3 + 1];
					int st = broken ? 2 : 0;
					im.frame(im.texture.uvRect(b * 240 + lx * 16, st * 240 + ly * 16, b * 240 + lx * 16 + 16, st * 240 + ly * 16 + 16));
					int c = cellOf[b][i];
					im.visible = !flowing && (Dungeon.level.visited[c] || Dungeon.level.mapped[c]);
				}

				if (!flowing){ dropBlocks(b); continue; }
				if (blocks[b] == null) makeBlocks(b);
				float pulse = 1f - (now - start) / flow;
				for (int i = 0; i < blocks[b].length / 3; i++){
					float ct = ROOT_BLOCKS[b][i * 3 + 2] / 1000f;
					float ash = 0, lit = 0;
					if (now >= start){
						float pass = start + (1f - ct) * flow;
						ash = Math.max(0, Math.min(1, (now - pass) / 0.3f));
						if (now < start + flow) lit = Math.max(0, 1f - Math.abs(ct - pulse) / 0.22f);
					}
					boolean s = seen(ROOT_BLOCKS[b][i * 3] * 4, ROOT_BLOCKS[b][i * 3 + 1] * 4);
					Image n = blocks[b][i * 3], l = blocks[b][i * 3 + 1], a = blocks[b][i * 3 + 2];
					n.visible = s && ash < 1; n.alpha(1f - ash);
					l.visible = s && lit > 0; l.alpha(0.9f * lit);
					a.visible = s && ash > 0; a.alpha(ash);
				}
			}
		}
	}

	//4×4 조각마다 {조각 x, y (방 영역에서 4픽셀 단위), t × 1000}. docs/sprites/boss25/roots25_blocks.txt에서 옮김 (뿌리 그림을 다시 만들면 같이 옮긴다)
	private static final int[][] ROOT_BLOCKS = {
		{27,30,41, 28,30,15, 29,30,1, 24,31,200, 25,31,126, 26,31,93, 27,31,59, 28,31,26, 29,31,4, 23,32,207, 24,32,183, 25,32,144, 26,32,108, 27,32,71, 28,32,44, 10,33,721, 11,33,718, 18,33,370, 19,33,370, 21,33,293, 22,33,267, 23,33,234, 24,33,191, 25,33,158, 26,33,133, 10,34,718, 11,34,700, 12,34,683, 19,34,371, 20,34,346, 21,34,310, 22,34,291, 23,34,298, 11,35,683, 12,35,665, 13,35,644, 18,35,421, 19,35,396, 20,35,363, 21,35,325, 22,35,342, 23,35,346, 12,36,647, 13,36,630, 14,36,603, 15,36,583, 17,36,476, 18,36,446, 19,36,414, 20,36,391, 22,36,387, 23,36,384, 14,37,594, 15,37,572, 16,37,541, 17,37,502, 18,37,469, 21,37,447, 22,37,424, 23,37,415, 14,38,612, 15,38,580, 16,38,553, 17,38,527, 21,38,456, 22,38,451, 23,38,453, 13,39,661, 14,39,635, 15,39,606, 16,39,566, 21,39,468, 22,39,466, 23,39,469, 12,40,714, 13,40,687, 14,40,657, 15,40,633, 10,41,720, 11,41,750, 12,41,741, 13,41,710, 10,42,820, 11,42,785, 12,42,761, 7,43,860, 8,43,860, 9,43,871, 10,43,848, 11,43,796, 12,43,777, 7,44,860, 8,44,891, 9,44,895, 10,44,873, 11,44,797, 12,44,796, 7,45,980, 8,45,953, 9,45,926, 11,45,822, 12,45,815, 7,46,993, 8,46,983, 10,46,873, 11,46,848, 7,47,1000, 9,47,907, 10,47,883, 11,47,869, 8,48,927, 9,48,915, 10,48,897, 11,48,883, 9,49,921, 10,49,913, 9,50,937},
		{30,30,1, 31,30,15, 32,30,48, 33,30,83, 30,31,2, 31,31,20, 32,31,57, 33,31,97, 34,31,131, 35,31,157, 32,32,72, 33,32,107, 34,32,147, 35,32,180, 36,32,213, 37,32,237, 42,32,610, 43,32,622, 44,32,634, 30,33,489, 31,33,466, 32,33,439, 33,33,413, 34,33,326, 35,33,276, 36,33,257, 37,33,262, 38,33,289, 42,33,588, 43,33,608, 44,33,629, 31,34,467, 32,34,450, 33,34,428, 34,34,312, 35,34,318, 36,34,281, 37,34,280, 38,34,314, 39,34,348, 40,34,370, 41,34,504, 42,34,557, 43,34,596, 31,35,474, 38,35,335, 39,35,365, 40,35,391, 41,35,499, 42,35,520, 39,36,389, 40,36,423, 41,36,460, 42,36,473, 40,37,449, 41,37,471, 42,37,499, 43,37,526, 41,38,495, 42,38,525, 43,38,554, 44,38,569, 45,38,550, 42,39,552, 43,39,578, 44,39,605, 45,39,636, 43,40,603, 44,40,624, 45,40,657, 46,40,693, 47,40,715, 42,41,831, 43,41,810, 44,41,784, 45,41,738, 46,41,717, 47,41,739, 48,41,771, 39,42,909, 40,42,891, 41,42,868, 42,42,846, 43,42,822, 44,42,786, 45,42,763, 46,42,738, 47,42,746, 48,42,794, 49,42,823, 40,43,896, 41,43,884, 42,43,866, 47,43,720, 48,43,821, 49,43,847, 50,43,874, 41,44,891, 49,44,869, 50,44,894, 51,44,924, 49,45,860, 50,45,904, 51,45,952, 52,45,973, 51,46,980, 52,46,988},
		{29,3,994, 30,3,994, 29,4,969, 30,4,972, 29,5,934, 30,5,934, 29,6,895, 30,6,888, 31,6,860, 29,7,859, 30,7,859, 29,8,821, 30,8,820, 29,9,790, 30,9,793, 31,9,720, 26,10,694, 29,10,754, 30,10,784, 31,10,835, 26,11,686, 27,11,681, 29,11,710, 30,11,725, 31,11,888, 32,11,923, 33,11,941, 34,11,957, 35,11,966, 25,12,711, 26,12,678, 27,12,669, 29,12,675, 30,12,669, 32,12,943, 33,12,961, 34,12,963, 35,12,966, 25,13,701, 26,13,671, 27,13,649, 28,13,641, 29,13,632, 30,13,630, 31,13,625, 26,14,655, 27,14,635, 28,14,605, 29,14,576, 30,14,591, 31,14,593, 28,15,581, 29,15,555, 30,15,554, 31,15,565, 28,16,558, 29,16,527, 30,16,520, 29,17,475, 30,17,485, 28,18,425, 29,18,436, 30,18,445, 28,19,398, 29,19,400, 30,19,406, 28,20,362, 29,20,362, 30,20,370, 28,21,330, 29,21,322, 30,21,306, 33,21,374, 34,21,374, 28,22,299, 29,22,286, 30,22,278, 33,22,351, 34,22,370, 29,23,249, 30,23,241, 32,23,299, 33,23,319, 28,24,200, 29,24,208, 30,24,207, 31,24,245, 32,24,273, 29,25,175, 30,25,179, 31,25,220, 29,26,132, 30,26,133, 29,27,91, 30,27,94, 29,28,56, 30,28,57, 29,29,19, 30,29,22, 29,30,1, 30,30,2},
	};


	//(사용자) 모든 판막은 둘레 O_VALVE_ZONE칸(둥글게) 안에서 친 공격만 들어간다: 그 범위의 바깥 둘레를 바닥에 판막 색의 옅은 테두리로
	//(수액 호박빛 / 등불 노란빛 / 어둠 넋빛 / 꽃잎 연분홍, 깜빡이지 않음, 본 적 있는 칸만). 판막이 깨져 있는 동안은 숨는다
	public static class ValveZone extends Group {
		private com.shatteredpixel.shatteredpixeldungeon.effects.ZoneEdgeFx edge;
		private final HeartValve valve;

		private static final int[] COLORS = { 0xE0952A, 0xFFE07A, 0x8FC8FF, 0xE8B0C4 };

		ValveZone( HeartValve valve ){
			this.valve = valve;
			if (!(Dungeon.level instanceof MasterBossLevel) || valve == null) return;
			MasterBossLevel l = (MasterBossLevel) Dungeon.level;
			java.util.HashSet<Integer> cells = new java.util.HashSet<>();
			for (int c = 0; c < l.length(); c++){
				if (!inZone(c, valve)) continue;
				if (c == valve.pos || (l.isRoomFloor(c) && c != SAPLING)) cells.add(c);
			}
			edge = new com.shatteredpixel.shatteredpixeldungeon.effects.ZoneEdgeFx(cells, COLORS[Math.max(0, Math.min(3, valve.kind))], com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_ZONE_ALPHA);
			add(edge);
			update();
		}

		@Override
		public void update() {
			super.update();
			if (edge != null && valve != null) edge.visible = !valve.broken && !(Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).bloomed); //(사용자) 주인이 쓰러지면 판막과 함께 사라짐
		}
	}

	//뿌리를 따라 흐르는 둥근 새잎빛 (판막 → 줄기 밑동), 흐름이 끝나면 스러진다
	private static class FlowLight extends com.watabou.noosa.Halo {
		private final int b;
		private float fade = -1;
		FlowLight( int b ){
			super(9, 0xB4E67A, 0.55f);
			this.b = b;
			visible = false;
		}
		@Override
		public void update() {
			super.update();
			float start = flowStart[b], now = Game.timeTotal;
			if (fade < 0 && start >= 0 && now < start){ visible = false; return; }
			float pulse = (start >= 0) ? 1f - (now - start) / flowTime() : 0;
			if (fade < 0 && start >= 0 && pulse > 0){
				int[] path = ROOT_PATH[b];
				int best = 0; float bd = 99;
				for (int i = 0; i < path.length / 3; i++){
					float d = Math.abs(path[i * 3 + 2] / 1000f - pulse);
					if (d < bd){ bd = d; best = i; }
				}
				point(ROOT_X0 * DungeonTilemap.SIZE + path[best * 3] + 0.5f, ROOT_Y0 * DungeonTilemap.SIZE + path[best * 3 + 1] + 0.5f);
				visible = true;
				return;
			}
			//다 흘렀으면 0.3초에 걸쳐 스러진다
			if (fade < 0) fade = 0;
			fade += Game.elapsed;
			float a = 0.55f * (1f - fade / 0.3f);
			if (a <= 0){ killAndErase(); return; }
			alpha(a);
		}
	}

	//자라는 선을 따라 올라가는 둥근 새잎빛, 다 자라면 천천히 스러진다
	//(O7, 사용자) 주인이 완전히 쓰러진 뒤 줄기 둘레에 은은하게 모였다가 정화가 퍼지는 동안 스러지는 둥근 새잎빛 (미리보기 anim_finale.html과 같은 흐름)
	private static class GatherLight extends com.watabou.noosa.Halo {
		GatherLight(){
			super(18, 0xD6F6A4, 0f);
			com.watabou.utils.PointF p = DungeonTilemap.tileCenterToWorld(SAPLING);
			point(p.x, p.y - 2);
		}
		@Override
		public void update() {
			super.update();
			float now = Game.timeTotal;
			float in = Math.min(1f, (now - fallEnd) / com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_FINALE_GATHER);
			float out = 1f - Math.max(0f, (now - purifyStart - 1.5f) / 1.0f);
			float k = Math.max(0f, Math.min(in, out));
			if (now > purifyStart + 2.5f || fallEnd < 0){ killAndErase(); return; }
			alpha(0.45f * k);
		}
	}

	private static class GrowLight extends com.watabou.noosa.Halo {
		private float fade = -1;
		GrowLight(){
			super(com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_GROW_GLOW_RADIUS, 0xB4E67A, 0.5f);
			update();
		}
		@Override
		public void update() {
			super.update();
			float p = growProgress();
			float baseY = (SAPLING / W + 1) * DungeonTilemap.SIZE;
			//(사용자) 그림 칸 맨 위가 아니라 자라는 단계의 실제 나무 꼭대기보다 O_GROW_GLOW_STOP픽셀 아래까지만 올라간다
			float topY = baseY - growHeight() + com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_GROW_GLOW_STOP;
			if (p >= 0 && p < 1f){
				float y = Math.max(topY, baseY - growHeight() * p);
				point(trunkWorldX(y), y);
				alpha(0.5f);
			} else {
				if (fade < 0) fade = 0;
				fade += Game.elapsed;
				point(trunkWorldX(topY), topY);
				float a = 0.5f * (1f - fade / 0.6f);
				if (a <= 0){ killAndErase(); return; }
				alpha(a);
			}
		}
	}

	//세계수 어린 줄기 그림 (sprites/maze_b25_sapling.png 16×32 × 4단계, 원본 docs/sprites/boss25/make_props25.ps1).
	//깬 판막 수만큼 자란다: 줄기가 굵어지고 키가 크고, 새 가지가 돋고, 흰 꽃이 하나씩 더 핀다. 원작 석상처럼 아래 칸은 캐릭터 아래, 위 칸은 캐릭터 위에 그린다.
	//조각마다 그림 둘: 예전 단계(자라는 선 위쪽)와 새 단계(자라는 선 아래쪽)
	public static class Sapling extends Group {
		private final boolean upper;
		private final Image oldImg, newImg;
		private final float x0, y0;

		Sapling( boolean upper ){
			this.upper = upper;
			oldImg = new Image("sprites/maze_b25_sapling.png");
			newImg = new Image("sprites/maze_b25_sapling.png");
			add(oldImg); add(newImg);
			x0 = (SAPLING % W) * DungeonTilemap.SIZE;
			y0 = (SAPLING / W) * DungeonTilemap.SIZE - (upper ? DungeonTilemap.SIZE : 0);
			update();
		}

		//stage 단계의 이 조각에서 아래쪽 rows줄 / 위쪽 rows줄만
		private void show( Image im, int stage, int top, int rows ){
			if (rows <= 0){ im.visible = false; return; }
			im.visible = true;
			int ty = upper ? 0 : 16;
			im.frame(im.texture.uvRect(stage * 16, ty + top, stage * 16 + 16, ty + top + rows));
			im.x = x0;
			im.y = y0 + top;
		}

		@Override
		public void update() {
			super.update();
			tickGrowth();
			float p = growProgress();
			int st = Math.max(0, shownStage);
			if (p < 0){
				show(oldImg, st, 0, 16);
				newImg.visible = false;
				return;
			}
			//자라는 선: 밑동(아래 조각 맨 아래)에서 자라는 단계의 나무 꼭대기까지
			int h = Math.round(growHeight() * p);
			int mine = upper ? Math.max(0, h - 16) : Math.min(h, 16); //이 조각에서 새 모습이 보이는 줄 수 (아래쪽부터)
			show(oldImg, st, 0, 16 - mine);
			show(newImg, growTo, 16 - mine, mine);
		}
	}

	private static final String STATE = "maze_state";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put( STATE, state );
		bundle.put( "maze_rustle", rustleCell );
		bundle.put( "maze_bloomed", bloomed );
		bundle.put( "maze_purify_r", purifyR );
		bundle.put( "maze_finale_sprout_r", finaleSproutR );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		state = bundle.getEnum( STATE, State.class );
		rebuildValves(); //불러온 판막을 다시 들고 있음 (주인이 쓰러진 뒤면 판막이 없어 빈 목록)
		rustleCell = bundle.contains( "maze_rustle" ) ? bundle.getInt( "maze_rustle" ) : -1;
		bloomed = bundle.getBoolean( "maze_bloomed" );
		purifyR = bundle.getFloat( "maze_purify_r" );
		if (bundle.contains( "maze_finale_sprout_r" )) finaleSproutR = bundle.getFloat( "maze_finale_sprout_r" );
	}
}
