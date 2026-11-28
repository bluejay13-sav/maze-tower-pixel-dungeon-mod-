/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.FogOfWar;
import com.watabou.noosa.Group;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

//Maze Tower: 망령 왕의 왕좌 (명세 22번). 그림 sprites/maze_wraith_throne.png (16×32 4프레임, 원본 docs/sprites/props/throne_d_flow_sheet.png).
//금빛·푸른빛 왕좌에 영혼 불꽃이 은은하게 흘러내린다. 원작 석상처럼 아래 칸(의자 몸통)은 캐릭터 아래 층에,
//위 칸(등받이 윗부분)은 벽 층(캐릭터 위)에 그린다. 원래 석상 그림은 DungeonTerrainTilemap·DungeonWallsTilemap에서 지운다.
//방이 어두운 동안에는 왕좌 전체를 안개 위에 기둥과 같은 밝기로 한 번 더 그린다.
public class WraithThrone extends MovieClip {

	public static final String TEX = "sprites/maze_wraith_throne.png";
	private static final int FPS = 4;

	private static final int LOWER = 0, UPPER = 1, OVER_FOG = 2;
	private final int part;

	private WraithThrone( int part ){
		super(TEX);
		this.part = part;
		int cell = CityBossLevel.MAZE_WRAITH_SEAT;
		int w = Dungeon.level.width();
		x = (cell % w) * DungeonTilemap.SIZE;
		y = (cell / w) * DungeonTilemap.SIZE;
		Animation a = new Animation(FPS, true);
		if (part == OVER_FOG){
			TextureFilm f = new TextureFilm(texture, 16, 32);
			a.frames(f, 0, 1, 2, 3);
			y -= DungeonTilemap.SIZE;
		} else {
			TextureFilm f = new TextureFilm(texture, 16, 16);
			if (part == UPPER){
				a.frames(f, 0, 1, 2, 3);
				y -= DungeonTilemap.SIZE;
			} else {
				a.frames(f, 4, 5, 6, 7);
			}
		}
		play(a);
	}

	private static boolean here(){
		return CityBossLevel.mazeThroneCell(CityBossLevel.MAZE_WRAITH_SEAT);
	}

	//의자 몸통: 캐릭터 아래 층 (Level.addVisuals)
	public static void addLower( Group g ){
		if (here()) g.add(new WraithThrone(LOWER));
	}

	//등받이 윗부분: 벽 층 (Level.addWallVisuals)
	public static void addUpper( Group g ){
		if (here()) g.add(new WraithThrone(UPPER));
	}

	//어둠 속: 안개 위
	public static void addOverFog(){
		if (here()) GameScene.effectOverFog(new WraithThrone(OVER_FOG));
	}

	//(M3, 지시서) 망령 왕이 브로치를 알아보면 왕좌의 푸른 불꽃이 금빛으로 (sprites/maze_wraith_throne_gold.png, 같은 칸 배치·크기,
	//밝은 푸른 불꽃만 금빛, 원본 docs/sprites/mobs/m_draft/make_throne_gold.ps1)
	public static final String TEX_GOLD = "sprites/maze_wraith_throne_gold.png";
	private boolean gold = false;

	private static boolean recognized(){
		if (Dungeon.hero == null) return false;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SageQuest q = Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SageQuest.class);
		return q != null && q.recognized;
	}

	@Override
	public void update() {
		boolean g = recognized();
		if (g != gold){
			gold = g;
			texture(g ? TEX_GOLD : TEX);
			if (curAnim != null) frame(curAnim.frames[curFrame]); //그림만 바꾸고 지금 칸은 그대로
		}
		super.update();
		if (part == OVER_FOG){
			visible = Dungeon.level instanceof CityBossLevel && ((CityBossLevel) Dungeon.level).wraithDark();
			rm = gm = bm = FogOfWar.visitedShade();
		}
	}
}
