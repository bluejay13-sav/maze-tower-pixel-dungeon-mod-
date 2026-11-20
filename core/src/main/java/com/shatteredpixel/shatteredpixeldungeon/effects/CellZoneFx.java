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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

//Maze Tower: 망령 왕 "인간을 죽이는 마법"의 범위 경고와 광선 (명세 22번).
//줄마다 따로 그린 띠·그림은 대각선에서 벽 쪽 세모가 비고, 바깥 줄이 벽에 막히면 테두리가 끊겼다.
//그래서 맞는 칸들을 칸 단위로 빈틈없이 칠하고(한 덩어리), 그 덩어리의 바깥 둘레 전체에 흰 테두리를 긋는다.
//벽에 막혀 폭이 줄어드는 곳도 테두리가 그대로 따라가 자연스럽게 이어진다. 왕이 서 있는 칸 쪽에는 테두리를 긋지 않는다.
public class CellZoneFx extends Group {

	public enum Mode { WARN, BEAM }

	private static final int[][] SIDES = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; //위·오른쪽·아래·왼쪽

	private final Mode mode;
	private final ArrayList<ColorBlock> fills = new ArrayList<>();
	private final ArrayList<ColorBlock> cores = new ArrayList<>();
	private final ArrayList<ColorBlock> edges = new ArrayList<>();
	private float time = 0;

	//cells: 칠할 칸, core: 더 밝게 칠할 가운데 줄 칸(없으면 null), origin: 테두리를 긋지 않을 이웃 칸(왕의 칸)
	public CellZoneFx( Mode mode, Collection<Integer> cells, Collection<Integer> core, int origin ){
		this.mode = mode;
		HashSet<Integer> set = new HashSet<>(cells);
		int w = Dungeon.level.width();
		float s = DungeonTilemap.SIZE;
		float e = mode == Mode.BEAM ? 2f : 1f; //테두리 두께
		int fill = mode == Mode.BEAM ? 0x7FC4FF : 0x6FA8FF;
		for (int c : set){
			float x = (c % w) * s, y = (c / w) * s;
			fills.add(block(x, y, s, s, fill));
			if (core != null && core.contains(c)) cores.add(block(x, y, s, s, 0xEAF6FF));
			int cx = c % w, cy = c / w;
			for (int[] d : SIDES){
				int nx = cx + d[0], ny = cy + d[1];
				int n = nx + ny * w;
				if (nx >= 0 && ny >= 0 && nx < w && ny < Dungeon.level.height()){
					if (set.contains(n) || n == origin) continue;
				}
				//칸 안쪽에 붙여 긋는다 (이웃 칸을 덮지 않게)
				if (d[1] == -1)     edges.add(block(x, y, s, e, 0xFFFFFF));
				else if (d[1] == 1) edges.add(block(x, y + s - e, s, e, 0xFFFFFF));
				else if (d[0] == -1) edges.add(block(x, y, e, s, 0xFFFFFF));
				else                 edges.add(block(x + s - e, y, e, s, 0xFFFFFF));
			}
		}
		update();
	}

	private ColorBlock block( float x, float y, float w, float h, int color ){
		ColorBlock b = new ColorBlock(w, h, 0xFF000000 | color);
		b.x = x;
		b.y = y;
		add(b);
		return b;
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float fillA, coreA, edgeA;
		if (mode == Mode.WARN){
			//숨 쉬듯 은은하게
			float p = 0.7f + 0.3f * (float)Math.sin(time * 5f);
			fillA = 0.16f * p; coreA = 0f; edgeA = 0.6f * p;
		} else {
			//광선: 잠깐 환하게 번쩍인 뒤 사라진다
			float hold = 0.15f, fade = 0.35f;
			float k = time < hold ? 1f : Math.max(0f, 1f - (time - hold) / fade);
			fillA = 0.6f * k; coreA = 0.45f * k; edgeA = 0.95f * k;
			if (time > hold + fade){
				killAndErase();
				return;
			}
		}
		for (ColorBlock b : fills) b.alpha(fillA);
		for (ColorBlock b : cores) b.alpha(coreA);
		for (ColorBlock b : edges) b.alpha(edgeA);
	}
}
