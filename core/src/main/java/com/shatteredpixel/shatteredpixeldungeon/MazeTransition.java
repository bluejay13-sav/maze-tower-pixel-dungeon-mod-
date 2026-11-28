/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TransitionTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TransitionWallsTilemap;
import com.watabou.noosa.Tilemap;

import java.util.ArrayList;

//Maze Tower (사용자, 2026-11-27): 구역이 바뀌는 보스층(10·15·20층)의 출구 둘레에 다음 구역의 바닥·벽을 은은히 섞어 그려
//다음 층이 어떤 곳일지 미리 짐작할 수 있게 한다. 출구에 가까울수록 진하고, 칸마다 조금씩 흔들려 가장자리가 들쭉날쭉하다.
//그림만 바꾼다(지형·규칙은 그대로). 끄기 MazeTower.EXIT_BLEND.
//  10층(봉인의 회랑) → 마력 광맥 바닥·벽   15층(마력 광맥) → 그 판의 잊힌 마도시 바닥·벽   20층(잊힌 마도시) → 탑의 심장 21~24층 바닥·벽
public class MazeTransition {

	//출구 쪽 가운데 칸과 퍼지는 반지름
	private static class Spec {
		final float cx, cy, r;
		Spec( float cx, float cy, float r ){ this.cx = cx; this.cy = cy; this.r = r; }
	}

	private static Spec spec( Level l ){
		if (!MazeTower.EXIT_BLEND || l == null) return null;
		if (l instanceof WardenBossLevel){
			WardenBossLevel.State s = ((WardenBossLevel) l).state();
			//1단계 감방 미로는 같은 자리가 미로라서, 대봉인실로 바뀐 뒤부터
			if (s != WardenBossLevel.State.PHASE2 && s != WardenBossLevel.State.WON) return null;
			return new Spec( 15, 28, MazeTower.EXIT_BLEND_RADIUS );
		}
		if (l instanceof GnawerBossLevel) return new Spec( 15, 29, MazeTower.EXIT_BLEND_RADIUS );
		if (l instanceof CityBossLevel && ((CityBossLevel) l).mazeRoom && MazeTower.HEART_NEW_FLOOR){
			return new Spec( 7, 11, MazeTower.EXIT_BLEND_RADIUS_20 );
		}
		return null;
	}

	//이 층에 섞임 층을 만들어 둘지 (10층은 1단계에도 만들어 두고, 대봉인실이 된 뒤부터 그린다)
	public static boolean wanted( Level l ){
		if (!MazeTower.EXIT_BLEND || l == null) return false;
		return l instanceof WardenBossLevel || l instanceof GnawerBossLevel
				|| (l instanceof CityBossLevel && ((CityBossLevel) l).mazeRoom && MazeTower.HEART_NEW_FLOOR);
	}

	//20층 위쪽 출구 복도를 원작 데몬 홀 그림 대신 지형 그림(잊힌 마도시 새 타일)으로 그릴지 (CityBossLevel.CustomGroundVisuals)
	public static boolean cityUpper(){
		return MazeTower.EXIT_BLEND && MazeTower.HEART_NEW_FLOOR && Dungeon.level instanceof CityBossLevel && ((CityBossLevel) Dungeon.level).mazeRoom;
	}

	//20층 출구 계단 그림이 덮는 칸 (섞임을 그리지 않는다)
	private static boolean cityStairs( int x, int y ){
		return x >= 4 && x <= 10 && y <= 11;
	}

	private static boolean deepWall( Level l, int c ){
		int[] map = l.map;
		if (!com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.wallStitcheable( map[c] )) return false;
		int w = l.width(), x = c % w, y = c / w;
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++){
			int xx = x + dx, yy = y + dy;
			if (xx < 0 || yy < 0 || xx >= w || yy >= l.height()) continue;
			if (!com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.wallStitcheable( map[xx + yy * w] )) return false;
		}
		return true;
	}

	private static float jitter( int x, int y ){
		double h = Math.sin( x * 12.9898 + y * 78.233 ) * 43758.5453;
		return (float)((h - Math.floor( h )) - 0.5);
	}

	//칸마다 진하기 단계 0(없음)·1·2·3
	public static int[] stages( Level l ){
		int[] st = new int[l.length()];
		Spec s = spec( l );
		if (s == null) return st;
		int w = l.width();
		boolean city = l instanceof CityBossLevel;
		for (int c = 0; c < st.length; c++){
			int x = c % w, y = c / w;
			if (city && cityStairs( x, y )) continue;
			//바닥에 닿지 않은 벽 속(깜깜한 곳)에는 그리지 않는다 (구역마다 벽 속 빛깔이 조금 달라 네모가 비쳐 보임)
			if (deepWall( l, c )) continue;
			float d = (float)Math.hypot( x - s.cx, y - s.cy ) + jitter( x, y ) * MazeTower.EXIT_BLEND_JITTER;
			float k = 1f - d / s.r;
			st[c] = k > 0.55f ? 3 : k > 0.3f ? 2 : k > 0.08f ? 1 : 0;
		}
		return st;
	}

	//다음 구역의 타일 시트
	public static String sheetTex( Level l ){
		if (l instanceof WardenBossLevel) return MazeRetreat.tiles( Assets.Environment.TILES_CAVES );
		if (l instanceof GnawerBossLevel){
			if (MazeTower.CITY_BY_BOSS && MazeBosses.ENABLED)
				return MazeBosses.empress() ? Assets.Environment.TILES_CITY_EMPRESS : Assets.Environment.TILES_CITY_WRAITH;
			return Assets.Environment.TILES_CITY;
		}
		return MazeRetreat.tiles( Assets.Environment.TILES_HALLS2 );
	}

	//20층: 바닥은 21~24층처럼 큰 바닥 그림을 칸 자리로 잘라 쓴다
	public static String bigTex( Level l ){
		if (!(l instanceof CityBossLevel)) return null;
		boolean ret = !MazeRetreat.tiles( Assets.Environment.TILES_HALLS2 ).equals( Assets.Environment.TILES_HALLS2 );
		return ret ? Assets.Environment.HEART_FLOOR_RETURN : Assets.Environment.HEART_FLOOR;
	}

	//땅 그림 층 (GameScene이 땅 그림·보스방 그림 위에 얹는다)
	public static ArrayList<Tilemap> terrainLayers(){
		ArrayList<Tilemap> out = new ArrayList<>();
		Level l = Dungeon.level;
		if (!wanted( l )) return out;
		String big = bigTex( l );
		for (int i = 0; i < 3; i++) out.add( TransitionTilemap.create( sheetTex( l ), i, false, big != null ) );
		if (big != null) for (int i = 0; i < 3; i++) out.add( TransitionTilemap.create( big, i, true, false ) );
		return out;
	}

	//벽 윗부분 층 (GameScene이 벽 그림 위에 얹는다)
	public static ArrayList<Tilemap> wallLayers(){
		ArrayList<Tilemap> out = new ArrayList<>();
		Level l = Dungeon.level;
		if (!wanted( l )) return out;
		for (int i = 0; i < 3; i++) out.add( TransitionWallsTilemap.create( sheetTex( l ), i ) );
		return out;
	}
}
