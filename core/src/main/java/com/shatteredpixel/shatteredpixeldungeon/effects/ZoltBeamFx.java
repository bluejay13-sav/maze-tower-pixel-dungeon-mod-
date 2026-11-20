/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

//Maze Tower: 망령 왕 "인간을 죽이는 마법"의 광선 (명세 22번).
//줄마다 광선 그림을 돌려 늘여 그리면 대각선에서 칸 귀퉁이가 비고, 줄이 여러 개로 보였다.
//그래서 맞는 칸마다 그 칸에 들어갈 광선 조각을 칸 그대로(돌리지 않고) 그린다. 조각들은 광선 가운데 선에서 떨어진 거리에 따라
//흰 심 → 옅은 하늘색 → 푸른색 → 짙은 푸른색으로 칠한 한 가지 단면에서 잘라 낸 것이라(`maze_fx20w_zoltraak_beam_cells.png`),
//이어 붙이면 칸이 빈틈없이 채워진 하나의 광선이 된다. 광선 바깥(맞지 않는 칸과 맞닿은 변)에는 짙은 테두리를 긋는다.
//잠깐 심이 숨 쉬듯 굵어졌다 가늘어진 뒤 흐려지며 사라진다.
public class ZoltBeamFx extends Group {

	private static final String TEX = "sprites/maze_fx20w_zoltraak_beam_cells.png";
	private static final int S = DungeonTilemap.SIZE;
	private static final float FRAME = 0.1f;   //한 프레임
	private static final float FADE  = 0.25f;  //흐려지는 시간
	private static final int RIM_COLOR = 0x2F5A8C;

	private final ArrayList<Image> pieces = new ArrayList<>();
	private final ArrayList<Integer> rows = new ArrayList<>();
	private final ArrayList<ColorBlock> rims = new ArrayList<>();
	private final TextureFilm film;
	private float time = 0;
	private int frame = -1;

	//cells: 광선이 지나는 칸, origin: 왕의 칸, (dx, dy): 광선 방향
	public ZoltBeamFx( Collection<Integer> cells, int origin, int dx, int dy ){
		HashSet<Integer> set = new HashSet<>(cells);
		int w = Dungeon.level.width();
		int kx = origin % w, ky = origin / w;
		film = new TextureFilm(TEX, 16, 16);
		for (int c : set){
			int cx = c % w, cy = c / w;
			int ox = cx - kx, oy = cy - ky;
			int row;
			if (dx != 0 && dy != 0){
				//대각선: "/"(오른쪽 위·왼쪽 아래)는 6~10줄, "\"는 11~15줄, 광선 가운데 선에서 몇 줄 떨어졌는지(-2~2)
				if (dx * dy < 0) row = 6 + clamp(ox + oy, 2) + 2;
				else             row = 11 + clamp(-ox + oy, 2) + 2;
			} else if (dy == 0){
				row = clamp(oy, 1) + 1;      //가로 광선: 0~2줄
			} else {
				row = 3 + clamp(ox, 1) + 1;  //세로 광선: 3~5줄
			}
			Image img = new Image(TEX);
			img.frame(film.get(row * 3));
			img.x = cx * S;
			img.y = cy * S;
			add(img);
			pieces.add(img);
			rows.add(row);

			//광선 바깥과 맞닿은 변에 짙은 테두리 (왕의 칸 쪽은 없음)
			int[][] sides = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
			for (int[] d : sides){
				int nx = cx + d[0], ny = cy + d[1];
				int n = nx + ny * w;
				boolean inside = nx >= 0 && ny >= 0 && nx < w && ny < Dungeon.level.height() && (set.contains(n) || n == origin);
				if (inside) continue;
				ColorBlock b;
				if (d[1] == -1)      b = new ColorBlock(S, 2, 0xFF000000 | RIM_COLOR);
				else if (d[1] == 1)  b = new ColorBlock(S, 2, 0xFF000000 | RIM_COLOR);
				else                 b = new ColorBlock(2, S, 0xFF000000 | RIM_COLOR);
				b.x = cx * S + (d[0] == 1 ? S - 2 : 0);
				b.y = cy * S + (d[1] == 1 ? S - 2 : 0);
				add(b);
				rims.add(b);
			}
		}
	}

	private static int clamp( int v, int r ){
		return Math.max(-r, Math.min(r, v));
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		int f = Math.min(2, (int)(time / FRAME));
		if (f != frame){
			frame = f;
			for (int i = 0; i < pieces.size(); i++) pieces.get(i).frame(film.get(rows.get(i) * 3 + f));
		}
		float hold = FRAME * 3;
		if (time > hold + FADE){
			killAndErase();
			return;
		}
		float a = time < hold ? 1f : 1f - (time - hold) / FADE;
		for (Image p : pieces) p.alpha(a);
		for (ColorBlock b : rims) b.alpha(a);
	}
}
