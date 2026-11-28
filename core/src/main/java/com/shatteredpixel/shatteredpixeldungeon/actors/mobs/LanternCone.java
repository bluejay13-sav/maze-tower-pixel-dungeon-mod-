/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

//Maze Tower (H1): 앞쪽 부채꼴로만 보는 빛 (봉인 등롱, 나중에 10층 간수의 등불도 같은 코드를 쓴다).
//방향은 8방향 번호: 0 오른쪽, 1 오른쪽 아래, 2 아래, 3 왼쪽 아래, 4 왼쪽, 5 왼쪽 위, 6 위, 7 오른쪽 위 (화면 기준, y가 아래로 늘어남)
public class LanternCone {

	public static final int[][] DIRS = {{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1},{0,-1},{1,-1}};

	//from에서 to를 향하는 가장 가까운 8방향 번호
	public static int octantToward( int from, int to ){
		int w = Dungeon.level.width();
		double a = Math.toDegrees(Math.atan2( (to / w) - (from / w), (to % w) - (from % w) ));
		int o = (int)Math.round( a / 45.0 );
		return ((o % 8) + 8) % 8;
	}

	//from에서 본 cell이 facing 방향의 부채꼴(양옆 half도, range칸) 안인지. 자기 칸은 늘 안
	public static boolean inCone( int from, int cell, int facing, float half, int range ){
		if (cell == from) return true;
		int w = Dungeon.level.width();
		int dx = (cell % w) - (from % w), dy = (cell / w) - (from / w);
		if (Math.max(Math.abs(dx), Math.abs(dy)) > range) return false;
		double a = Math.toDegrees(Math.atan2( dy, dx ));
		double f = facing * 45.0;
		double diff = Math.abs(((a - f) % 360 + 540) % 360 - 180);
		return diff <= half + 0.01;
	}

	//원작 시야(fov)에서 부채꼴 밖을 지운다
	public static void mask( int from, int facing, boolean[] fov, float half, int range ){
		for (int i = 0; i < fov.length; i++){
			if (fov[i] && !inCone(from, i, facing, half, range)) fov[i] = false;
		}
	}
}
