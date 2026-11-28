/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Point;

//Maze Tower (사용자, 디버그 전용): 층마다 맵 전체를 사진으로 남기는 도구.
//환경 변수 MAZE_MAPSHOTS(저장할 폴더)를 두고 desktop:debug로 켜면, 시작 화면에서 바로 전사로 새 게임을 시작하고
//1층부터 26층까지 차례로 들어가 안개를 걷고(맵 전체가 보이게) 화면에 맵 전체를 맞춘 뒤 한 장씩 저장(층.png)하고 끝낸다.
//빈 저장 칸에만 시작한다(빈 칸이 없으면 하지 않음). 게임 규칙에는 아무 영향이 없다.
public class MazeMapShot {

	private static final int LAST_DEPTH = 26;
	public static boolean active = false;
	public static boolean discarding = false; //사진을 다 찍고 지운 판 (끝날 때 다시 저장하지 않게)
	private static int frames = 0;
	private static boolean framed = false;
	private static String dir = null;

	public static boolean wanted(){
		return DeviceCompat.isDebug() && System.getenv( "MAZE_MAPSHOTS" ) != null;
	}

	//시작 화면에서 부른다
	public static boolean startIfWanted(){
		if (active || !wanted()) return false;
		int slot = GamesInProgress.firstEmpty();
		if (slot == -1) return false;
		dir = System.getenv( "MAZE_MAPSHOTS" );
		active = true;
		GamesInProgress.curSlot = slot;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.initSeed();
		//(디버그) MAZE_MAPSHOTS_RETREAT가 있으면 첫 층에서 역행을 시작해 역행 중 모습으로 찍는다
		if (System.getenv( "MAZE_MAPSHOTS_RETREAT" ) != null) MazeRetreat.pendingBegin = true;
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene( InterlevelScene.class );
		return true;
	}

	//GameScene.create 뒤
	public static void onSceneCreated(){
		frames = 0;
		framed = false;
	}

	//GameScene.update마다
	public static void onUpdate( GameScene scene ){
		if (!active || Dungeon.level == null) return;
		frames++;
		//(디버그) MAZE_MAPSHOTS_DEPTHS="10,15,20"이면 그 층만 찍는다
		if (frames == 2 && !wantedDepth( Dungeon.depth )){ next(); return; }
		//(디버그) MAZE_MAPSHOTS_OPEN이 있으면 보스층 출구를 연 모습으로 (적은 치움)
		if (frames == 2 && System.getenv( "MAZE_MAPSHOTS_OPEN" ) != null) openExit();
		//(디버그) MAZE_MAPSHOTS_ROOMS가 있으면 맵 전체 대신 특수 방·비밀 방만 하나씩 확대해 찍는다 (층_방이름_번호.png)
		if (System.getenv( "MAZE_MAPSHOTS_ROOMS" ) != null){
			if (frames == 3 && roomList == null){
				scene.mazeHideFog();
				if (PixelScene.uiCamera != null) PixelScene.uiCamera.x = -100000;
				roomList = new java.util.ArrayList<>();
				if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel){
					for (com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room r : ((com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel) Dungeon.level).rooms()){
						if (r instanceof com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.SpecialRoom) roomList.add( r );
					}
				}
				roomIdx = -1; roomWait = 0;
				nextRoom();
			} else if (roomList != null && roomWait > 0){
				roomWait--;
				if (roomWait == 0) shootRoom = true;
			}
			return;
		}
		//(디버그) MAZE_MAPSHOTS_VIEW가 있으면 안개·확대를 그대로 둔 보통 게임 화면 (주인공 둘레)을 찍는다
		if (frames == 3 && !framed && System.getenv( "MAZE_MAPSHOTS_VIEW" ) != null){
			if (System.getenv( "MAZE_MAPSHOTS_NOFOG" ) != null) scene.mazeHideFog();
			if (PixelScene.uiCamera != null) PixelScene.uiCamera.x = -100000;
			framed = true;
			return;
		}
		if (frames == 3 && !framed){
			//안개를 걷고, 화면 글·버튼은 화면 밖으로, 맵 전체가 화면에 들어오게
			scene.mazeHideFog();
			if (PixelScene.uiCamera != null) PixelScene.uiCamera.x = -100000;
			int mw = Dungeon.level.width() * DungeonTilemap.SIZE, mh = Dungeon.level.height() * DungeonTilemap.SIZE;
			int sw = Game.width, sh = Game.height;
			float z = Math.min( sw / (float) mw, sh / (float) mh );
			Camera.main.zoom( z );
			Camera.main.snapTo( mw / 2f, mh / 2f );
			framed = true;
		}
	}

	//GameScene.draw 뒤 (그린 화면을 읽는다)
	public static void afterDraw(){
		if (active && System.getenv( "MAZE_MAPSHOTS_ROOMS" ) != null){ afterDrawRoom(); return; }
		if (!active || !framed || frames < 6 || Dungeon.level == null) return;
		try {
			int fbW = Gdx.graphics.getBackBufferWidth(), fbH = Gdx.graphics.getBackBufferHeight();
			int mw = Dungeon.level.width() * DungeonTilemap.SIZE, mh = Dungeon.level.height() * DungeonTilemap.SIZE;
			Point a = Camera.main.cameraToScreen( 0, 0 );
			Point b = Camera.main.cameraToScreen( mw, mh );
			float sx = fbW / (float) Game.width, sy = fbH / (float) Game.height;
			int x0 = Math.max( 0, Math.round( a.x * sx ) ), y0 = Math.max( 0, Math.round( a.y * sy ) );
			int x1 = Math.min( fbW, Math.round( b.x * sx ) ), y1 = Math.min( fbH, Math.round( b.y * sy ) );
			int w = Math.max( 1, x1 - x0 ), h = Math.max( 1, y1 - y0 );
			Pixmap full = Pixmap.createFromFrameBuffer( 0, 0, fbW, fbH );
			Pixmap crop = new Pixmap( w, h, Pixmap.Format.RGBA8888 );
			crop.setBlending( Pixmap.Blending.None );
			crop.drawPixmap( full, 0, 0, x0, fbH - y1, w, h );
			//화면에는 늘 불투명하게 보이므로 사진도 불투명하게 (반투명 겹침 그림이 화면 버퍼의 알파를 낮춰 사진에서 비쳐 보이던 것)
			java.nio.ByteBuffer px = crop.getPixels();
			for (int i = 3; i < px.limit(); i += 4) px.put( i, (byte) 0xFF );
			PixmapIO.writePNG( Gdx.files.absolute( dir + "/" + Dungeon.depth + ".png" ), crop, 1, true );
			full.dispose(); crop.dispose();
		} catch (Exception e){
			Game.reportException( e );
		}
		next();
	}

	//(디버그) 특수 방 사진 찍기
	private static java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room> roomList = null;
	private static int roomIdx = -1, roomWait = 0;
	private static boolean shootRoom = false;
	private static final float ROOM_ZOOM = 3f;

	private static void nextRoom(){
		roomIdx++;
		if (roomIdx >= roomList.size()){ roomList = null; next(); return; }
		com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room r = roomList.get( roomIdx );
		Camera.main.zoom( ROOM_ZOOM );
		Camera.main.snapTo( (r.left + r.right + 1) / 2f * DungeonTilemap.SIZE, (r.top + r.bottom + 1) / 2f * DungeonTilemap.SIZE );
		roomWait = 4;
	}

	private static boolean afterDrawRoom(){
		if (!shootRoom || roomList == null || roomIdx < 0 || roomIdx >= roomList.size()) return roomList != null;
		shootRoom = false;
		com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room r = roomList.get( roomIdx );
		try {
			int fbW = Gdx.graphics.getBackBufferWidth(), fbH = Gdx.graphics.getBackBufferHeight();
			int S = DungeonTilemap.SIZE;
			Point a = Camera.main.cameraToScreen( (r.left - 1) * S, (r.top - 1) * S );
			Point b = Camera.main.cameraToScreen( (r.right + 2) * S, (r.bottom + 2) * S );
			float sx = fbW / (float) Game.width, sy = fbH / (float) Game.height;
			int x0 = Math.max( 0, Math.round( a.x * sx ) ), y0 = Math.max( 0, Math.round( a.y * sy ) );
			int x1 = Math.min( fbW, Math.round( b.x * sx ) ), y1 = Math.min( fbH, Math.round( b.y * sy ) );
			int w = Math.max( 1, x1 - x0 ), h = Math.max( 1, y1 - y0 );
			Pixmap full = Pixmap.createFromFrameBuffer( 0, 0, fbW, fbH );
			Pixmap crop = new Pixmap( w, h, Pixmap.Format.RGBA8888 );
			crop.setBlending( Pixmap.Blending.None );
			crop.drawPixmap( full, 0, 0, x0, fbH - y1, w, h );
			java.nio.ByteBuffer px = crop.getPixels();
			for (int i = 3; i < px.limit(); i += 4) px.put( i, (byte) 0xFF );
			String name = Dungeon.depth + "_" + r.getClass().getSimpleName() + "_" + System.currentTimeMillis() % 100000;
			PixmapIO.writePNG( Gdx.files.absolute( dir + "/" + name + ".png" ), crop, 1, true );
			full.dispose(); crop.dispose();
		} catch (Exception e){
			Game.reportException( e );
		}
		nextRoom();
		return true;
	}

	private static boolean wantedDepth( int d ){
		String s = System.getenv( "MAZE_MAPSHOTS_DEPTHS" );
		if (s == null) return true;
		for (String p : s.split( "," )) if (p.trim().equals( Integer.toString( d ) )) return true;
		return false;
	}

	private static void openExit(){
		Level l = Dungeon.level;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : l.mobs.toArray( new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0] )){
			if (m.sprite != null) m.sprite.killAndErase();
			m.destroy();
		}
		if (l instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) ((com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) l).mazeShotOpen();
		if (l instanceof com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel) ((com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel) l).onGnawerDeath();
		GameScene.updateMap();
	}

	private static void next(){
		framed = false; frames = -1000;
		int d = Dungeon.depth + 1;
		String only = System.getenv( "MAZE_MAPSHOTS_DEPTHS" );
		if (only != null) while (d <= LAST_DEPTH && !wantedDepth( d )) d++;
		if (d > LAST_DEPTH){
			active = false;
			//(디버그) 사진을 찍으려고 만든 판은 지운다 (저장 칸을 채우지 않게)
			Dungeon.deleteGame( GamesInProgress.curSlot, true );
			GamesInProgress.delete( GamesInProgress.curSlot );
			discarding = true;
			Gdx.app.exit();
			return;
		}
		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = d;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1;
		Game.switchScene( InterlevelScene.class );
	}
}
