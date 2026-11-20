/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CityPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.ImpShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.watabou.utils.Rect;

import java.util.ArrayList;
import java.util.HashSet;

public class CityBossLevel extends Level {

	{
		color1 = 0x4b6636;
		color2 = 0xf2f2f2;
	}

	private static int WIDTH = 15;
	private static int HEIGHT = 48;

	private static final Rect entry = new Rect(1, 37, 14, 48);
	private static final Rect arena = new Rect(1, 25, 14, 38);
	private static final Rect end = new Rect(0, 0, 15, 22);

	private static final int bottomDoor = 7 + (arena.bottom-1)*15;
	private static final int topDoor = 7 + arena.top*15;

	public static final int throne;
	private static final int[] pedestals = new int[4];

	static {
		Point c = arena.center();
		throne = c.x + (c.y) * WIDTH;
		pedestals[0] = c.x-3 + (c.y-3) * WIDTH;
		pedestals[1] = c.x+3 + (c.y-3) * WIDTH;
		pedestals[2] = c.x+3 + (c.y+3) * WIDTH;
		pedestals[3] = c.x-3 + (c.y+3) * WIDTH;
	}

	private ImpShopRoom impShop;

	@Override
	public void playLevelMusic() {
		if (locked){
			if (BossHealthBar.isBleeding()){
				Music.INSTANCE.play(Assets.Music.CITY_BOSS_FINALE, true);
			} else {
				Music.INSTANCE.play(Assets.Music.CITY_BOSS, true);
			}
		//if top door isn't unlocked
		} else if (map[topDoor()] == Terrain.LOCKED_DOOR){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(CityLevel.CITY_TRACK_LIST, CityLevel.CITY_TRACK_CHANCES, false);
		}
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CITY;
	}

	private static final String IMP_SHOP = "imp_shop";
	//Maze Tower: 마도시의 여제 보스방으로 지은 층인지 (지을 때 정해 저장. 치트로 보스를 바꿔도 이미 지은 층은 그대로)
	private static final String MAZE_EMPRESS_ROOM = "maze_empress_room";
	private static final String MAZE_ROOM = "maze_room20";
	public boolean mazeEmpressRoom = false;
	//Maze Tower: 마탑 20층 보스방(망령 왕·여제, 13×13 새 모양)으로 지은 층. 예전(9×9 여제 방) 저장 파일은 false
	public boolean mazeRoom = false;

	//Maze Tower: 위쪽 문 자리. 마탑 보스방은 방이 원작보다 2줄 길어서 문이 2칸 위에 있다
	private int topDoor(){
		return mazeRoom ? MAZE_TOP_DOOR : topDoor;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( IMP_SHOP, impShop );
		bundle.put( MAZE_EMPRESS_ROOM, mazeEmpressRoom ); //Maze Tower
		bundle.put( MAZE_ROOM, mazeRoom );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		impShop = (ImpShopRoom) bundle.get( IMP_SHOP );
		mazeEmpressRoom = bundle.getBoolean( MAZE_EMPRESS_ROOM ); //Maze Tower
		mazeRoom = bundle.getBoolean( MAZE_ROOM );
		if (map[topDoor()] != Terrain.LOCKED_DOOR && Imp.Quest.earnedShop() && !impShop.shopSpawned()){
			spawnShop();
		}
	}

	@Override
	protected boolean build() {

		setSize(WIDTH, HEIGHT);

		//entrance room
		Painter.fill(this, entry, Terrain.WALL);
		Painter.fill(this, entry, 1, Terrain.BOOKSHELF);
		Painter.fill(this, entry, 2, Terrain.EMPTY);

		Painter.fill(this, entry.left+3, entry.top+3, 1, 5, Terrain.BOOKSHELF);
		Painter.fill(this, entry.right-4, entry.top+3, 1, 5, Terrain.BOOKSHELF);

		Painter.set(this, entry.left+5, entry.top+1, Terrain.REGION_DECO);
		Painter.set(this, entry.left+7, entry.top+1, Terrain.REGION_DECO);

		Point c = entry.center();

		Painter.fill(this, c.x-1, c.y-2, 3, 1, Terrain.STATUE);
		Painter.fill(this, c.x-1, c.y, 3, 1, Terrain.STATUE);
		Painter.fill(this, c.x-1, c.y+2, 3, 1, Terrain.STATUE);
		Painter.fill(this, c.x, entry.top+1, 1, 6, Terrain.CUSTOM_DECO_EMPTY);

		Painter.set(this, c.x, entry.top, Terrain.DOOR);

		int entrance = c.x + (c.y+2)*width();
		Painter.set(this, entrance, Terrain.ENTRANCE);
		transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));

		//Maze Tower: 마탑 보스면 원작 드워프 왕 옥좌방 대신 망령 왕 / 마도시의 여제 보스방 (명세 22번)
		mazeRoom = com.shatteredpixel.shatteredpixeldungeon.MazeBosses.ENABLED;
		mazeEmpressRoom = mazeRoom && com.shatteredpixel.shatteredpixeldungeon.MazeBosses.empress();
		if (mazeRoom){
			buildMazeRoom();
		} else {

		//DK's throne room
		Painter.fillDiamond(this, arena, 1, Terrain.EMPTY);

		Painter.fill(this, arena, 5, Terrain.CUSTOM_DECO_EMPTY);
		Painter.fill(this, arena, 6, Terrain.CUSTOM_DECO);

		c = arena.center();
		Painter.set(this, c.x-3, c.y, Terrain.STATUE);
		Painter.set(this, c.x-4, c.y, Terrain.STATUE);
		Painter.set(this, c.x+3, c.y, Terrain.STATUE);
		Painter.set(this, c.x+4, c.y, Terrain.STATUE);

		Painter.set(this, pedestals[0], Terrain.PEDESTAL);
		Painter.set(this, pedestals[1], Terrain.PEDESTAL);
		Painter.set(this, pedestals[2], Terrain.PEDESTAL);
		Painter.set(this, pedestals[3], Terrain.PEDESTAL);

		Painter.fill(this, c.x, c.y+2, 1, 4, Terrain.CUSTOM_DECO_EMPTY);

		Painter.set(this, c.x, arena.top, Terrain.LOCKED_DOOR);

		}

		//exit hallway
		Painter.fill(this, end, Terrain.CHASM);
		Painter.fill(this, end.left+4, end.top+5, 7, 18, Terrain.EMPTY);
		Painter.fill(this, end.left+4, end.top+5, 7, 4, Terrain.EXIT);

		int exitCell = end.left+7 + (end.top+8)*width();
		LevelTransition exit = new LevelTransition(this, exitCell, LevelTransition.Type.REGULAR_EXIT);
		exit.set(end.left+4, end.top+4, end.left+4+6, end.top+4+4);
		transitions.add(exit);

		impShop = new ImpShopRoom();
		impShop.set(end.left+3, end.top+12, end.left+11, end.top+20);
		Painter.set(this, impShop.center(), Terrain.PEDESTAL);

		Painter.set(this, impShop.left+2, impShop.top, Terrain.STATUE);
		Painter.set(this, impShop.left+6, impShop.top, Terrain.STATUE);

		//Maze Tower: 마탑 보스방은 이 두 줄이 방의 위쪽 벽·첫 줄이다
		if (!mazeRoom){
			Painter.fill(this, end.left+5, end.bottom+1, 5, 1, Terrain.EMPTY);
			Painter.fill(this, end.left+6, end.bottom+2, 3, 1, Terrain.EMPTY);
		}

		impShop.paint(this);
		new CityPainter().paint(this, null);

		//Maze Tower: 원작은 20층이면 아래가 바닥인 벽마다 휘장(WALL_DECO)을 건다(확률 1/(21-층) = 100%).
		//마탑 보스방은 원작 휘장 대신 새 장식(effects/BossRoomDecor20)을 그리므로 방 둘레의 휘장을 지운다.
		if (mazeRoom) clearMazeRoomWallDeco();

		//pillars last, no deco on these
		Painter.fill(this, end.left+1, end.top+2, 2, 2, Terrain.WALL);
		Painter.fill(this, end.left+1, end.top+7, 2, 2, Terrain.WALL);
		Painter.fill(this, end.left+1, end.top+12, 2, 2, Terrain.WALL);
		Painter.fill(this, end.left+1, end.top+17, 2, 2, Terrain.WALL);

		Painter.fill(this, end.right-3, end.top+2, 2, 2, Terrain.WALL);
		Painter.fill(this, end.right-3, end.top+7, 2, 2, Terrain.WALL);
		Painter.fill(this, end.right-3, end.top+12, 2, 2, Terrain.WALL);
		Painter.fill(this, end.right-3, end.top+17, 2, 2, Terrain.WALL);

		CustomTilemap customVisuals = new CustomGroundVisuals();
		customVisuals.setRect(0, 0, width(), height());
		customTiles.add(customVisuals);

		customVisuals = new CustomTerrainVisuals();
		customVisuals.setRect(0, 0, width(), height());
		customTerrain.add(customVisuals);

		customVisuals = new CustomWallVisuals();
		customVisuals.setRect(0, 0, width(), height());
		customWalls.add(customVisuals);

		return true;
	}

	// ===== Maze Tower: 20층 마탑 보스방 (명세 22번) =====
	//방을 감싸는 13×13의 맨 왼쪽 위 칸을 (행 0, 열 0)으로 둔다. 13×13은 층의 x 1~13, y 24~36에 놓인다.
	//(원작 옥좌방보다 2줄 길어서 방 위쪽 벽을 2칸 올렸다: 위쪽 문 (7,23), 아래쪽 문은 원작 그대로 (7,37))
	//옥좌·왕좌는 (6,6). 잔디·물은 만들지 않는다(보스층은 원래 칠하지 않음).
	public static final int ROOM_SIZE = 13;
	public static final int ROOM_X = 1, ROOM_Y = 24;
	public static final int MAZE_TOP_DOOR = 7 + 23*15;
	public static final int MAZE_THRONE = (ROOM_X + 6) + (ROOM_Y + 6)*15;

	//여제 방: 가운데 11×11 + 네 변 가운데 5칸을 1칸씩 확장 (141칸), 한 칸짜리 기둥 8개
	public static final int[][] EMPRESS_PILLARS = {
			{3,3},{3,6},{3,9}, {6,3},{6,9}, {9,3},{9,6},{9,9}
	};
	//망령 왕 방: 13×13에서 귀퉁이를 깎은 모양 (153칸), 한 칸짜리 기둥 8개
	public static final int[][] WRAITH_PILLARS = {
			{3,3},{3,9},{9,3},{9,9}, {2,6},{10,6},{6,2},{6,10}
	};

	//(행, 열)이 방 안인지 (기둥 칸도 방 안으로 친다)
	public static boolean inRoomShape( boolean empress, int r, int c ){
		if (r < 0 || c < 0 || r >= ROOM_SIZE || c >= ROOM_SIZE) return false;
		if (empress){
			if (r == 0 || r == 12)  return c >= 4 && c <= 8;
			if (r <= 3 || r >= 9)   return c >= 1 && c <= 11;
			return true;
		} else {
			if (r == 0 || r == 12)  return c >= 3 && c <= 9;
			if (r == 1 || r == 11)  return c >= 1 && c <= 11;
			return true;
		}
	}

	public static boolean isPillarRC( boolean empress, int r, int c ){
		for (int[] p : (empress ? EMPRESS_PILLARS : WRAITH_PILLARS)){
			if (p[0] == r && p[1] == c) return true;
		}
		return false;
	}

	public static int cellOf( int r, int c ){
		return (ROOM_X + c) + (ROOM_Y + r) * 15;
	}

	//칸 → {행, 열} (13×13 밖이면 null)
	public static int[] rcOf( int cell ){
		int x = cell % 15 - ROOM_X, y = cell / 15 - ROOM_Y;
		if (x < 0 || y < 0 || x >= ROOM_SIZE || y >= ROOM_SIZE) return null;
		return new int[]{y, x};
	}

	//방 바닥 칸 (기둥 제외)
	public boolean roomFloor( int cell ){
		int[] rc = rcOf(cell);
		return rc != null && inRoomShape(mazeEmpressRoom, rc[0], rc[1]) && !isPillarRC(mazeEmpressRoom, rc[0], rc[1]);
	}

	public boolean isPillar( int cell ){
		int[] rc = rcOf(cell);
		return rc != null && inRoomShape(mazeEmpressRoom, rc[0], rc[1]) && isPillarRC(mazeEmpressRoom, rc[0], rc[1]);
	}

	//방의 맨 끝자락 칸: 바깥 벽과 맞닿은 칸 (대각선 포함). 기둥은 바깥 벽이 아니다
	public boolean roomEdge( int cell ){
		int[] rc = rcOf(cell);
		if (rc == null || !inRoomShape(mazeEmpressRoom, rc[0], rc[1])) return false;
		for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++){
			if (!inRoomShape(mazeEmpressRoom, rc[0] + dr, rc[1] + dc)) return true;
		}
		return false;
	}

	//벽(바깥 벽·기둥)과 맞닿은 칸 (대각선 포함)
	public boolean touchesWall( int cell ){
		int[] rc = rcOf(cell);
		if (rc == null) return true;
		for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++){
			int r = rc[0] + dr, c = rc[1] + dc;
			if (!inRoomShape(mazeEmpressRoom, r, c) || isPillarRC(mazeEmpressRoom, r, c)) return true;
		}
		return false;
	}

	//방 바닥 칸 목록 (기둥 제외)
	public java.util.ArrayList<Integer> roomFloorCells(){
		java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
		for (int r = 0; r < ROOM_SIZE; r++) for (int c = 0; c < ROOM_SIZE; c++){
			if (inRoomShape(mazeEmpressRoom, r, c) && !isPillarRC(mazeEmpressRoom, r, c)) list.add(cellOf(r, c));
		}
		return list;
	}

	public int mazeThrone(){
		return MAZE_THRONE;
	}

	private void buildMazeRoom(){
		for (int r = 0; r < ROOM_SIZE; r++){
			for (int c = 0; c < ROOM_SIZE; c++){
				if (!inRoomShape(mazeEmpressRoom, r, c)) continue;
				Painter.set(this, cellOf(r, c), isPillarRC(mazeEmpressRoom, r, c) ? Terrain.WALL : Terrain.EMPTY);
			}
		}
		Painter.set(this, MAZE_TOP_DOOR, Terrain.LOCKED_DOOR);
	}

	//방 둘레(바깥 벽 + 기둥)의 원작 휘장(WALL_DECO)을 평범한 벽으로. 새 장식은 effects/BossRoomDecor20이 겹쳐 그린다
	private void clearMazeRoomWallDeco(){
		for (int y = ROOM_Y - 1; y <= ROOM_Y + ROOM_SIZE; y++){
			for (int x = 0; x < 15; x++){
				int cell = x + y * width();
				if (map[cell] == Terrain.WALL_DECO) map[cell] = Terrain.WALL;
			}
		}
	}


	//returns a random pedestal that doesn't already have a summon inbound on it
	public int getSummoningPos(){
		Mob king = getKing();
		HashSet<DwarfKing.Summoning> summons = king.buffs(DwarfKing.Summoning.class);
		ArrayList<Integer> positions = new ArrayList<>();
		for (int pedestal : pedestals) {
			boolean clear = true;
			for (DwarfKing.Summoning s : summons) {
				if (s.getPos() == pedestal) {
					clear = false;
					break;
				}
			}
			if (clear) {
				positions.add(pedestal);
			}
		}
		if (positions.isEmpty()){
			return -1;
		} else {
			return Random.element(positions);
		}
	}

	private Mob getKing(){
		for (Mob m : mobs){
			if (m instanceof DwarfKing) return m;
		}
		return null;
	}

	@Override
	protected void createMobs() {
	}

	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int pos;
				do {
					pos = randomRespawnCell(null);
				} while (pos == entrance());
				for (Item i : bonesItems) {
					drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = entrance() + i;
			if (passable[cell]
					&& Actor.findChar(cell) == null
					&& (!Char.hasProp(ch, Char.Property.LARGE) || openSpace[cell])){
				candidates.add(cell);
			}
		}

		if (candidates.isEmpty()){
			return -1;
		} else {
			return Random.element(candidates);
		}
	}

	@Override
	public boolean invalidHeroPos(int tile) {
		//hero cannot be above top door if it is locked
		if (map[topDoor()] == Terrain.LOCKED_DOOR && tile <= topDoor()){
			return true;
		}
		return super.invalidHeroPos(tile);
	}

	@Override
	public void occupyCell( Char ch ) {
		//Maze Tower: 주인공이 아래쪽 문을 지나 들어오면 방을 잠그고 보스가 나온다
		if (!(mazeEmpressRoom && !mazeRoom) //예전 저장 파일의 9×9 여제 방은 보스가 없어 잠그지 않는다
				&& map[bottomDoor] != Terrain.LOCKED_DOOR && map[topDoor()] == Terrain.LOCKED_DOOR
				&& ch.pos < bottomDoor && ch == Dungeon.hero) {
			seal();
		}

		super.occupyCell( ch );

		//Maze Tower: 마탑 보스방 (명세 22번)
		if (mazeRoom){
			if (ch == Dungeon.hero){
				int prev = lastHeroPos;
				if (prev != -1 && prev != ch.pos && adjacent(prev, ch.pos)) lastHeroStep = ch.pos - prev;
				lastHeroPos = ch.pos;
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithKing.noise(ch.pos); //근위병은 소리를 따라간다
				if (prev != -1 && prev != ch.pos) com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ElementTile.onHeroLeft(prev); //밟은 원소 타일은 사라진다
				com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ElementTile.onHeroEnter(ch); //원소 상태로 반대 원소 타일에 들어서면 반응
			}
			com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithKing.onOccupy(ch); //영혼 구슬
		}
	}

	//Maze Tower: 주인공이 마지막으로 움직인 방향 (망령 왕 기습: 주인공 뒤 칸)
	private int lastHeroPos = -1;
	private int lastHeroStep = 0;

	public int lastHeroStep(){
		return lastHeroStep;
	}

	@Override
	public void seal() {
		super.seal();
		Statistics.qualifiedForBossChallengeBadge = true;

		//moves intelligent allies with the hero, preferring closer pos to entrance door
		int doorPos = pointToCell(new Point(arena.left + arena.width()/2, arena.bottom));
		Mob.holdAllies(this, doorPos);
		Mob.restoreAllies(this, Dungeon.hero.pos, doorPos);

		Mob boss;
		if (mazeRoom){
			//Maze Tower: 마탑 20층 보스 (망령 왕 / 마도시의 여제), 옥좌·왕좌 칸에
			boss = mazeEmpressRoom ? new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MazeEmpress()
					: new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithKing();
			boss.state = boss.HUNTING;
			boss.pos = MAZE_THRONE;
			GameScene.add( boss );
		} else {
			boss = new DwarfKing();
			boss.state = boss.WANDERING;
			boss.pos = pointToCell(arena.center());
			GameScene.add( boss );
			boss.beckon(Dungeon.hero.pos);
		}

		if (heroFOV[boss.pos]) {
			boss.notice();
			boss.sprite.alpha( 0 );
			boss.sprite.parent.add( new AlphaTweener( boss.sprite, 1, 0.1f ) );
		}

		set( bottomDoor, Terrain.LOCKED_DOOR );
		GameScene.updateMap( bottomDoor );
		Dungeon.observe();

		Heap h = heaps.get(bottomDoor);
		if (h != null){
			for (Item i : h.items){
				drop(i, bottomDoor-width()).sprite.drop(bottomDoor);
			}
			h.destroy();
		}

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(Assets.Music.CITY_BOSS, true);
			}
		});
	}

	@Override
	public void unseal() {
		super.unseal();

		set( bottomDoor, Terrain.DOOR );
		GameScene.updateMap( bottomDoor );

		set( topDoor(), Terrain.DOOR );
		GameScene.updateMap( topDoor() );

		if (Imp.Quest.earnedShop()) {
			spawnShop();
		}
		Dungeon.observe();

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut(5f, new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.end();
					}
				});
			}
		});
	}

	private void spawnShop(){
		while (impShop.spacesNeeded() >= 7*(impShop.height()-2)){
			impShop.bottom++;
		}
		impShop.spawnShop(this);
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER:
				return Messages.get(CityLevel.class, "water_name");
			case Terrain.HIGH_GRASS:
				return Messages.get(CityLevel.class, "high_grass_name");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(CityLevel.class, "region_deco_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public String tileDesc(int tile) {
		switch (tile) {
			case Terrain.ENTRANCE:
			case Terrain.ENTRANCE_SP:
				return Messages.get(CityLevel.class, "entrance_desc");
			case Terrain.EXIT:
				return Messages.get(CityLevel.class, "exit_desc");
			case Terrain.WALL_DECO:
			case Terrain.EMPTY_DECO:
				return Messages.get(CityLevel.class, "deco_desc");
			case Terrain.STATUE:
			case Terrain.STATUE_SP:
				return Messages.get(CityLevel.class, "statue_desc");
			case Terrain.BOOKSHELF:
				return Messages.get(CityLevel.class, "bookshelf_desc");
			case Terrain.REGION_DECO:
			case Terrain.REGION_DECO_ALT:
				return Messages.get(CityLevel.class, "region_deco_desc");
			default:
				return super.tileDesc( tile );
		}
	}

	@Override
	public Group addVisuals( ) {
		super.addVisuals();
		CityLevel.addCityVisuals(this, visuals);
		return visuals;
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		CityLevel.addCityWallVisuals( this, wallVisuals );
		return wallVisuals;
	}

	public static class CustomGroundVisuals extends CustomTilemap {

		{
			texture = com.shatteredpixel.shatteredpixeldungeon.MazeBosses.cityBossTex(); //Maze Tower: 보스에 맞춘 보스방 그림
			tileW = 15;
			tileH = 48;
		}

		private static final int STAIR_ROWS = 7;

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];

			int[] map = Dungeon.level.map;

			int stairsTop = -1;

			//upper part of the level, mostly demon halls tiles
			for (int i = tileW; i < tileW*22; i++){

				if (map[i] == Terrain.EXIT && stairsTop == -1){
					stairsTop = i;
				}

				//pillars
				if (map[i] == Terrain.WALL && map[i-tileW] == Terrain.CHASM){
					data[i] = 13*8 + 6;
					data[++i] = 13*8 + 7;
				} else if (map[i] == Terrain.WALL && map[i-tileW] == Terrain.WALL){
					data[i] = 14*8 + 6;
					data[++i] = 14*8 + 7;
				} else if (i > tileW && map[i] == Terrain.CHASM && map[i-tileW] == Terrain.WALL) {
					data[i] = 15*8 + 6;
					data[++i] = 15*8 + 7;

				//imp's pedestal
				} else if (map[i] == Terrain.PEDESTAL) {
					data[i] = 12*8 + 5;

				//skull piles
				} else if (map[i] == Terrain.STATUE) {
					data[i] = 15*8 + 5;

				//ground tiles
				} else if (map[i] == Terrain.EMPTY || map[i] == Terrain.EMPTY_DECO
						|| map[i] == Terrain.EMBERS || map[i] == Terrain.GRASS
						|| map[i] == Terrain.HIGH_GRASS || map[i] == Terrain.FURROWED_GRASS){

					//final ground stiching with city tiles
					if (i/tileW == 21){
						data[i] = 11*8 + 0;
						data[++i] = 11*8 + 1;
						data[++i] = 11*8 + 2;
						data[++i] = 11*8 + 3;
						data[++i] = 11*8 + 4;
						data[++i] = 11*8 + 5;
						data[++i] = 11*8 + 6;
					} else {

						//regular ground tiles
						if (map[i - 1] == Terrain.CHASM) {
							data[i] = 12 * 8 + 1;
						} else if (map[i + 1] == Terrain.CHASM) {
							data[i] = 12 * 8 + 3;
						} else if (map[i] == Terrain.EMPTY_DECO) {
							data[i] = 12 * 8 + 4;
						} else {
							data[i] = 12 * 8 + 2;
						}
					}

					//otherwise no tile here
				} else {
					data[i] = -1;
				}
			}

			//custom for stairs
			for (int i = 0; i < STAIR_ROWS; i++){
				for (int j = 0; j < 7; j++){
					data[stairsTop+j] = (i+4)*8 + j;
				}
				stairsTop += tileW;
			}

			//lower part: statues, pedestals, and carpets
			for (int i = tileW*22; i < tileW * tileH; i++){

				//pedestal spawners
				if (map[i] == Terrain.PEDESTAL){
					data[i] = 13*8 + 4;

				//statues
				} else if (map[i] == Terrain.STATUE) {

					//throne statues
					if (i < tileW*32){
						//facing left
						if (i%tileW > 7) {
							data[i] = 10 * 8 + 7;

							i++;
							data[i] = 11 * 8 + 7;

						} else {
							data[i] = 8 * 8 + 7;
							i++;
							data[i] = 9 * 8 + 7;
						}
					} else {

						//regular statues facing left
						if (i % tileW > 7) {
							data[i] = 15 * 8 + 4;
						} else {
							data[i] = -1;
						}

					}

				//carpet tiles, uses both terrain types for compatibility with pre-v4.0 saves
				} else if (map[i] == Terrain.CUSTOM_DECO_EMPTY || map[i] == Terrain.EMPTY_SP) {
					//top row of DK's throne
					if ((map[i + 1] == Terrain.CUSTOM_DECO_EMPTY && map[i + tileW] == Terrain.CUSTOM_DECO_EMPTY)
						|| (map[i + 1] == Terrain.EMPTY_SP && map[i + tileW] == Terrain.EMPTY_SP)) {
						data[i] = 13 * 8 + 1;
						data[++i] = 13 * 8 + 2;
						data[++i] = 13 * 8 + 3;

					//mid row of DK's throne
					}else if (map[i + 1] == Terrain.CUSTOM_DECO) {
						data[i] = 14 * 8 + 1;
						data[++i] = 14 * 8 + 2;
						data[++i] = 14 * 8 + 3;

					//bottom row of DK's throne
					} else if ((map[i+1] == Terrain.CUSTOM_DECO_EMPTY && map[i-tileW] == Terrain.CUSTOM_DECO_EMPTY)
						|| (map[i+1] == Terrain.EMPTY_SP && map[i-tileW] == Terrain.EMPTY_SP) ){
						data[i] = 15*8 + 1;
						data[++i] = 15*8 + 2;
						data[++i] = 15*8 + 3;

					//otherwise entrance carpets
					} else if ((map[i-tileW] != Terrain.CUSTOM_DECO_EMPTY && map[i-tileW] != Terrain.EMPTY_SP) || map[i-2*tileW] == Terrain.CUSTOM_DECO){
						data[i] = 13*8 + 0;
					} else if (map[i+tileW] != Terrain.CUSTOM_DECO_EMPTY && map[i+tileW] != Terrain.EMPTY_SP){
						data[i] = 15*8 + 0;
					} else {
						data[i] = 14*8 + 0;
					}

				//otherwise no tile here
				} else {

					data[i] = -1;
				}
			}

			v.map( data, tileW );
			return v;
		}

		@Override
		public boolean allowWater(int tileX, int tileY) {
			if (super.allowWater(tileX, tileY)){
				return true;
			} else {
				//allow curtain tails
				int i = tileX + tileY*tileW;
				int[] map = Dungeon.level.map;
				return i > tileW*32 && (map[i] == Terrain.EMPTY || map[i] == Terrain.EMPTY_DECO) && map[i-tileW] == Terrain.WALL_DECO;
			}
		}

		@Override
		public String name(int tileX, int tileY) {
			int cell = (this.tileX + tileX) + (this.tileY + tileY)*tileW;

			//demon halls tiles
			if (cell < Dungeon.level.width*22){
				if (Dungeon.level.map[cell] == Terrain.STATUE){
					return Messages.get(HallsLevel.class, "statue_name");
				}

			//DK arena tiles
			} else {
				if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
					return Messages.get(CityBossLevel.class, "throne_name");
				} else if (Dungeon.level.map[cell] == Terrain.PEDESTAL){
					return Messages.get(CityBossLevel.class, "summoning_name");
				}
			}

			return super.name(tileX, tileY);
		}

		@Override
		public String desc(int tileX, int tileY) {
			int cell = (this.tileX + tileX) + (this.tileY + tileY)*tileW;

			//demon halls tiles
			if (cell < Dungeon.level.width*22){
				if (Dungeon.level.map[cell] == Terrain.EXIT){
					return Messages.get(HallsLevel.class, "exit_desc");
				} else if (Dungeon.level.map[cell] == Terrain.STATUE){
					return Messages.get(HallsLevel.class, "statue_desc");
				} else if (Dungeon.level.map[cell] == Terrain.EMPTY_DECO){
					return "";
				}

			//DK arena tiles
			} else {
				if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
					return Messages.get(CityBossLevel.class, "throne_desc");
				} else if (Dungeon.level.map[cell] == Terrain.PEDESTAL){
					return Messages.get(CityBossLevel.class, "summoning_desc");
				}
			}

			return super.desc(tileX, tileY);
		}
	}

	public static class CustomTerrainVisuals extends CustomTilemap {

		{
			texture = com.shatteredpixel.shatteredpixeldungeon.MazeBosses.cityBossTex(); //Maze Tower: 보스에 맞춘 보스방 그림
			tileW = 15;
			tileH = 48;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			int[] map = Dungeon.level.map;

			//upper part of the level, skull statues
			for (int i = tileW; i < tileW*22; i++){
				if (map[i] == Terrain.STATUE) {
					data[i] = 15*8 + 5;
				} else {
					data[i] = -1;
				}
			}

			//lower part: statues and banners
			for (int i = tileW*22; i < tileW * tileH; i++){

				if (map[i] == Terrain.STATUE) {

					//throne statues
					if (i < tileW*32){
						//facing left
						if (i%tileW > 7) {
							data[i] = 10 * 8 + 7;

							i++;
							data[i] = 11 * 8 + 7;

						} else {
							data[i] = 8 * 8 + 7;
							i++;
							data[i] = 9 * 8 + 7;
						}
					} else {

						//regular statues facing left
						if (i % tileW > 7) {
							data[i] = 15 * 8 + 4;
						} else {
							data[i] = -1;
						}

					}

				//banners along walls
				} else if (i < tileW*32 && map[i] == Terrain.WALL_DECO && map[i+tileW] != Terrain.WALL) {
					data[i] = 6*8 + 7;
				} else if (i < tileW*32 && (map[i] == Terrain.EMPTY || map[i] == Terrain.EMPTY_DECO) && map[i-tileW] == Terrain.WALL_DECO) {
					data[i] = 7*8 + 7;

				//otherwise no tile here
				} else {

					data[i] = -1;
				}
			}

			v.map( data, tileW );
			return v;
		}

		@Override
		public Image image(int tileX, int tileY) {
			return null;
		}
	}

	public static class CustomWallVisuals extends CustomTilemap {
		{
			texture = com.shatteredpixel.shatteredpixeldungeon.MazeBosses.cityBossTex(); //Maze Tower: 보스에 맞춘 보스방 그림
			tileW = 15;
			tileH = 48;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];

			int[] map = Dungeon.level.map;

			int shadowTop = -1;

			//upper part of the level, mostly demon halls tiles
			for (int i = tileW; i < tileW*21; i++) {

				if (map[i] == Terrain.EXIT && shadowTop == -1){
					shadowTop = i - tileW*4;
				}

				//pillars
				if (map[i] == Terrain.CHASM && map[i+tileW] == Terrain.WALL) {
					data[i] = 12*8 + 6;
					data[++i] = 12*8 + 7;
				} else if (map[i] == Terrain.WALL && map[i-tileW] == Terrain.CHASM) {
					data[i] = 13 * 8 + 6;
					data[++i] = 13 * 8 + 7;

				//skull tops
				} else if (map[i+tileW] == Terrain.STATUE) {
					data[i] = 14*8 + 5;

				//otherwise no tile here
				} else {
					data[i] = -1;
				}
			}

			//custom shadow  for stairs
			for (int i = 0; i < 8; i++){
				if (i < 4){
					data[shadowTop] = i*8 + 0;
					data[shadowTop+1] = data[shadowTop+2] = data[shadowTop+3] = data[shadowTop+4] =
							data[shadowTop+5] = data[shadowTop+6] = i*8 + 1;
					data[shadowTop+7] = i*8 + 2;
				} else {
					int j = i - 4;
					data[shadowTop] = j*8 + 3;
					data[shadowTop+1] = data[shadowTop+2] = data[shadowTop+3] = data[shadowTop+4] =
							data[shadowTop+5] = data[shadowTop+6] = j*8 + 4;
					data[shadowTop+7] = j*8 + 5;
				}

				shadowTop += tileW;
			}

			//lower part. Statues and DK's throne
			for (int i = tileW*21; i < tileW * tileH; i++){

				//Statues that need to face left instead of right
				if (map[i] == Terrain.STATUE && i%tileW > 7){
					data[i-tileW] = 14*8 + 4;
				} else if (map[i] == Terrain.CUSTOM_DECO){
					data[i-tileW] = 13*8 + 5;
				}

				//always no tile here (as the above statements are modifying previous tiles)
				data[i] = -1;
			}

			v.map( data, tileW );
			return v;
		}
	}
}
