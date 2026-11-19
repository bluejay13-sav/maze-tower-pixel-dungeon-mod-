/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.Visual;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 역행 중 맵 연출 (명세 21번). 그림은 core/src/main/assets/sprites/maze_retreat_*.png, maze_trace*.png
//  - 금(바닥·벽), 피어나는 분홍 꽃(바닥·벽), 보스가 쓰러진 자리: 기존 타일 위에 겹치는 타일 층
//    (저장하지 않고 화면을 만들 때마다 층의 시드로 같은 자리에 다시 그린다)
//  - 먼지, 꽃이 피어나는 동작, 낙석(경고 그림자 → 떨어지는 돌 → 부서짐): 한 번 재생되는 그림
public class RetreatEffects {

	private static final int SIZE = DungeonTilemap.SIZE;

	public static final String CRACK_FLOOR  = "sprites/maze_retreat_crack_floor.png";  //16×16 ×2
	public static final String CRACK_WALL   = "sprites/maze_retreat_crack_wall.png";   //16×16 ×2
	public static final String FLOWER_FLOOR = "sprites/maze_retreat_flower_floor.png"; //16×16 ×3
	public static final String FLOWER_WALL  = "sprites/maze_retreat_flower_wall.png";  //16×16 ×2
	public static final String FLOWER_BLOOM = "sprites/maze_retreat_flower_bloom.png"; //16×16 ×4
	public static final String DUST         = "sprites/maze_retreat_dust.png";         //16×16 ×6
	public static final String ROCK_WARN    = "sprites/maze_retreat_rock_warn.png";    //16×16 ×2
	public static final String ROCK_FALL    = "sprites/maze_retreat_rock_fall.png";    //16×32 ×4
	public static final String ROCK_HIT     = "sprites/maze_retreat_rock_hit.png";     //16×16 ×3
	public static final String RUBBLE       = "sprites/maze_retreat_rubble.png";       //16×16 ×3

	// ===== 겹치는 타일 층 (GameScene.create에서 역행 중일 때) =====

	public static void addOverlays( GameScene scene ){
		Level level = Dungeon.level;
		int len = level.length();
		int[] crackFloor  = empty(len);
		int[] crackWall   = empty(len);
		int[] flowerFloor = empty(len);
		int[] flowerWall  = empty(len);

		float flowerPct = MazeTower.FLOWER_FLOOR_BASE + (26 - Dungeon.depth) * MazeTower.FLOWER_FLOOR_PER_DEPTH;

		//층의 시드로 정해서, 다시 불러와도 같은 자리에 그린다
		Random.pushGenerator(Dungeon.seedCurDepth() + 0x4D5A);
		for (int i = 0; i < len; i++){
			if (isFloor(level, i)){
				if (Random.Float() < MazeTower.CRACK_FLOOR_PCT) crackFloor[i] = Random.Int(2);
				if (Random.Float() < flowerPct)                 flowerFloor[i] = Random.Int(3);
			} else if (isVisibleWall(level, i)){
				if (Random.Float() < MazeTower.CRACK_WALL_PCT)                       crackWall[i] = Random.Int(2);
				if (Random.Float() < flowerPct * MazeTower.FLOWER_WALL_RATIO)        flowerWall[i] = Random.Int(2);
			}
		}
		Random.popGenerator();

		scene.addCustomTile(new Overlay(CRACK_FLOOR, crackFloor));
		scene.addCustomTile(new Overlay(CRACK_WALL, crackWall));
		scene.addCustomTile(new Overlay(FLOWER_FLOOR, flowerFloor));
		scene.addCustomTile(new Overlay(FLOWER_WALL, flowerWall));

		//보스가 쓰러진 자리 (32×16)
		String trace = bossTrace(Dungeon.depth);
		if (trace != null && level.mazeBossPos >= 0){
			int[] data = empty(len);
			int p = level.mazeBossPos;
			int x = p % level.width();
			if (x + 1 >= level.width()) p--; //오른쪽 끝이면 한 칸 왼쪽에서 시작
			data[p] = 0;
			data[p + 1] = 1;
			scene.addCustomTile(new Overlay(trace, data));
		}
	}

	//층별 보스가 쓰러진 자리 그림. 20층은 20층 기획 뒤에, 25층은 26층 아뮬렛 방으로 대신한다.
	private static String bossTrace( int depth ){
		switch (depth){
			case 5:  return "sprites/maze_trace5_sap.png";
			case 10: return "sprites/maze_trace10_chain.png";
			case 15: return "sprites/maze_trace15_golem.png"; //15층 보스가 정해지면 maze_trace15_crystal.png와 나눈다
			default: return null;
		}
	}

	private static int[] empty( int len ){
		int[] d = new int[len];
		for (int i = 0; i < len; i++) d[i] = -1;
		return d;
	}

	public static boolean isFloor( Level level, int i ){
		return level.passable[i] && !level.solid[i] && !level.pit[i] && !level.water[i]
				&& level.getTransition(i) == null;
	}

	private static boolean isVisibleWall( Level level, int i ){
		int t = level.map[i];
		if (t != Terrain.WALL && t != Terrain.WALL_DECO) return false;
		for (int n : PathFinder.NEIGHBOURS4){
			int c = i + n;
			if (c >= 0 && c < level.length() && level.passable[c]) return true;
		}
		return false;
	}

	public static class Overlay extends CustomTilemap {

		private int[] data;

		public Overlay(){
			//저장하지 않는다 (불러오기용 빈 생성자)
		}

		public Overlay( String texture, int[] data ){
			this.texture = texture;
			this.data = data;
			setRect(0, 0, Dungeon.level.width(), Dungeon.level.height());
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			v.map(data, tileW);
			return v;
		}
	}

	// ===== 한 번 재생되는 그림 =====

	private static float cellX( int cell ){ return (cell % Dungeon.level.width()) * SIZE; }
	private static float cellY( int cell ){ return (cell / Dungeon.level.width()) * SIZE; }

	private static MovieClip clip( String tex, int fw, int fh, int fps, boolean loop, int frames ){
		MovieClip c = new MovieClip(tex);
		TextureFilm film = new TextureFilm(c.texture, fw, fh);
		Object[] idx = new Object[frames];
		for (int i = 0; i < frames; i++) idx[i] = i;
		MovieClip.Animation anim = new MovieClip.Animation(fps, loop).frames(film, idx);
		if (!loop){
			c.listener = new MovieClip.Listener() {
				@Override
				public void onComplete(MovieClip.Animation a) {
					c.killAndErase();
				}
			};
		}
		c.play(anim);
		return c;
	}

	private static boolean visible( int cell ){
		return Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}

	//천장에서 떨어지는 먼지 (장식)
	public static void dust( int cell ){
		if (!visible(cell)) return;
		MovieClip c = clip(DUST, 16, 16, 12, false, 6);
		c.x = cellX(cell);
		c.y = cellY(cell);
		GameScene.effect(c);
	}

	//주인공이 지나간 근처에서 분홍 꽃이 피어난다
	public static void bloom( int cell ){
		if (!visible(cell)) return;
		MovieClip c = clip(FLOWER_BLOOM, 16, 16, 8, false, 4);
		c.x = cellX(cell);
		c.y = cellY(cell);
		GameScene.effect(c);
	}

	//낙석 경고 그림자 (깜빡임). 돌이 떨어질 때 지운다
	public static Visual warn( int cell ){
		MovieClip c = clip(ROCK_WARN, 16, 16, 4, true, 2);
		c.x = cellX(cell);
		c.y = cellY(cell);
		GameScene.effect(c);
		return c;
	}

	//떨어지는 돌(16×32) → 부서짐(16×16)
	public static void rockFall( final int cell ){
		final MovieClip fall = clip(ROCK_FALL, 16, 32, 16, false, 4);
		fall.x = cellX(cell);
		fall.y = cellY(cell) - SIZE; //아래쪽이 그 칸에 닿는다
		fall.listener = new MovieClip.Listener() {
			@Override
			public void onComplete(MovieClip.Animation a) {
				fall.killAndErase();
				MovieClip hit = clip(ROCK_HIT, 16, 16, 12, false, 3);
				hit.x = cellX(cell);
				hit.y = cellY(cell);
				GameScene.effect(hit);
			}
		};
		GameScene.effect(fall);
		CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
	}

	//주인공 주변(거리 range 안)의 바닥 칸들
	public static ArrayList<Integer> floorCellsNear( int center, int range ){
		ArrayList<Integer> cells = new ArrayList<>();
		Level level = Dungeon.level;
		for (int i = 0; i < level.length(); i++){
			if (level.distance(center, i) > range) continue;
			if (level.solid[i] || level.pit[i]) continue;
			cells.add(i);
		}
		return cells;
	}

	public static boolean occupied( int cell ){
		return Actor.findChar(cell) != null;
	}
}
