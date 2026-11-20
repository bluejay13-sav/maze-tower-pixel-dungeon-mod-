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
			boolean diag = dx != 0 && dy != 0;
			//대각선에서 테두리를 칸 변에 직접 긋는 쪽 (광선 끝처럼 그쪽 이웃이 하나만 있는 곳). 옆면은 조각 그림이 곧은 선으로 잘라 그린다
			boolean[] edgeRim = {true, true, true, true}; //위·오른쪽·아래·왼쪽
			if (diag){
				//대각선: "/"(오른쪽 위·왼쪽 아래)는 6~50줄, "\"는 51~95줄. 광선 가운데 선에서 몇 줄 떨어졌는지(s = -2~2)와
				//바깥쪽(+)·안쪽(-) 옆이 비었는지(비었으면 곧은 선으로 잘라 테두리)로 고른다
				boolean slash = dx * dy < 0;
				int s = clamp(slash ? ox + oy : -ox + oy, 2);
				//+쪽 이웃 두 칸: "/"는 오른쪽·아래, "\"는 왼쪽·아래. -쪽은 그 반대
				int[] pA = slash ? new int[]{1, 0} : new int[]{-1, 0};
				int[] pB = {0, 1};
				boolean posA = has(set, origin, w, cx + pA[0], cy + pA[1]), posB = has(set, origin, w, cx + pB[0], cy + pB[1]);
				boolean negA = has(set, origin, w, cx - pA[0], cy - pA[1]), negB = has(set, origin, w, cx - pB[0], cy - pB[1]);
				boolean clipPos = !posA && !posB, clipNeg = !negA && !negB;
				//(결정) 광선 폭을 살짝 줄였다: 자르는 선을 줄 사이(반 줄 안쪽)로 둔다. 그래서 바깥 줄이 기둥 옆으로 홀로 빠져나가도
				//양옆이 모두 잘려 보이지 않고(가는 광선이 삐져나가지 않음), 광선 바깥 가장자리는 가장 바깥 줄 칸의 안쪽 절반 지점이다.
				//가장 바깥 바로 안쪽 줄(±1)은 바깥 줄이 있어도 그 가장자리(반 줄 바깥)에서 잘라 광선 옆면을 곧게 맞춘다
				int pv = clipPos ? 1 : (s == 1 ? 2 : 0);
				int nv = clipNeg ? 1 : (s == -1 ? 2 : 0);
				row = (slash ? 6 : 51) + (s + 2) * 9 + pv * 3 + nv;
				//곧은 선으로 자른 쪽은 칸 변에 테두리를 긋지 않는다
				if (clipPos){ edgeRim[sideIndex(pA)] = false; edgeRim[sideIndex(pB)] = false; }
				if (clipNeg){ edgeRim[sideIndex(new int[]{-pA[0], -pA[1]})] = false; edgeRim[sideIndex(new int[]{-pB[0], -pB[1]})] = false; }
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
			for (int si = 0; si < 4; si++){
				int[] d = sides[si];
				if (!edgeRim[si]) continue;
				if (has(set, origin, w, cx + d[0], cy + d[1])) continue;
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

	//그 칸이 광선 안인지 (왕의 칸도 안으로 친다)
	private static boolean has( HashSet<Integer> set, int origin, int w, int x, int y ){
		if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) return false;
		int n = x + y * w;
		return set.contains(n) || n == origin;
	}

	//방향 → 변 번호 (위 0, 오른쪽 1, 아래 2, 왼쪽 3)
	private static int sideIndex( int[] d ){
		if (d[1] == -1) return 0;
		if (d[0] == 1) return 1;
		if (d[1] == 1) return 2;
		return 3;
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
