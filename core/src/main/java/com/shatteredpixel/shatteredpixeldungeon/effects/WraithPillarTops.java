/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.FogOfWar;

import java.util.HashSet;

//Maze Tower: 망령 왕의 어둠 속 기둥 윗부분 (명세 22번).
//기둥(벽)의 윗면은 원작에서 기둥 바로 위 칸에 그려지는데, 어둠 속에서는 그 칸(바닥)이 깜깜한 안개에 덮여 기둥이 잘려 보인다.
//그래서 어두운 동안에만 기둥 바로 위 칸의 윗면 그림을 안개 위에 한 번 더 그린다. 기둥 몸통과 같은 밝기로 어둡게 칠한다.
public class WraithPillarTops extends DungeonWallsTilemap {

	private final boolean[] top;

	public WraithPillarTops( CityBossLevel l ){
		super();
		top = new boolean[l.length()];
		for (int c = 0; c < l.length(); c++){
			if (c + l.width() < l.length() && l.isPillar(c + l.width()) && !l.isPillar(c)) top[c] = true;
		}
		updateMap();
	}

	public static void add( GameScene scene ){
		if (!(Dungeon.level instanceof CityBossLevel)) return;
		CityBossLevel l = (CityBossLevel) Dungeon.level;
		if (!l.mazeRoom || l.mazeEmpressRoom) return;
		//원작 벽 그림의 건너뛸 칸 목록은 새로 만들 때 지워지므로, 지우기 전 것을 되돌려 둔다
		HashSet<Integer> skip = new HashSet<>(DungeonWallsTilemap.skipCells);
		WraithPillarTops t = new WraithPillarTops(l);
		DungeonWallsTilemap.skipCells.clear();
		DungeonWallsTilemap.skipCells.addAll(skip);
		//기둥 몸통도 안개 위에 같은 밝기로 (몸통만 안개 아래에 두면 윗부분보다 어둡고 깨져 보였다)
		GameScene.effectOverFog(com.shatteredpixel.shatteredpixeldungeon.tiles.WraithPillarBodies.create(l));
		GameScene.effectOverFog(t);
	}

	@Override
	protected boolean needsRender( int pos ) {
		return top != null && pos < top.length && top[pos] && super.needsRender(pos);
	}

	@Override
	public void update() {
		super.update();
		boolean dark = Dungeon.level instanceof CityBossLevel && ((CityBossLevel) Dungeon.level).wraithDark();
		visible = dark;
		float s = FogOfWar.visitedShade();
		rm = gm = bm = s;
		liftHero(dark);
	}

	//(결정) 어두운 동안 기둥 그림은 안개 위(캐릭터 층보다 위)에 있어서, 주인공이 기둥 바로 아래 칸에 서면 머리가 기둥에 가려 끊겨 보였다.
	//그래서 어두운 동안에는 주인공 그림도 안개 위 층으로 올려(기둥보다 나중에 그려지게) 기둥 앞에 서 있는 것처럼 보이게 한다.
	//밝아지면 원래 캐릭터 층으로 돌려놓는다
	private boolean heroLifted = false;

	private void liftHero( boolean dark ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null) return;
		com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite hs = Dungeon.hero.sprite;
		if (dark == heroLifted || hs.parent == null) return;
		hs.parent.erase(hs);
		if (dark) GameScene.effectOverFog(hs);
		else      GameScene.addMobSpriteBack(hs);
		heroLifted = dark;
	}
}
