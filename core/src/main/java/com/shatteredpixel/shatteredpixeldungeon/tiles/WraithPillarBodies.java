/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;

//Maze Tower: 망령 왕의 어둠 속 기둥 몸통 (명세 22번).
//기둥 몸통을 안개 아래에 두면 부드러운 안개 가장자리 때문에 윗부분(effects/WraithPillarTops, 안개 위)보다 훨씬 어둡고
//아래로 갈수록 흐려져 깨져 보였다. 그래서 어두운 동안에는 기둥 칸을 깜깜하게 두고 몸통도 안개 위에 같은 밝기로 그린다.
public class WraithPillarBodies extends DungeonTerrainTilemap {

	private final boolean[] body;

	public WraithPillarBodies( CityBossLevel l ){
		super();
		body = new boolean[l.length()];
		for (int c = 0; c < l.length(); c++){
			body[c] = l.isPillar(c) && !CityBossLevel.mazeThroneCell(c);
		}
		updateMap();
	}

	//원작 지형 그림(instance)을 이것으로 바꿔 버리지 않게 되돌린다
	public static WraithPillarBodies create( CityBossLevel l ){
		DungeonTerrainTilemap keep = instance;
		WraithPillarBodies b = new WraithPillarBodies(l);
		instance = keep;
		return b;
	}

	@Override
	protected boolean needsRender( int pos ) {
		return body != null && pos < body.length && body[pos] && super.needsRender(pos);
	}

	@Override
	public void update() {
		super.update();
		visible = Dungeon.level instanceof CityBossLevel && ((CityBossLevel) Dungeon.level).wraithDark();
		rm = gm = bm = FogOfWar.visitedShade();
	}
}
