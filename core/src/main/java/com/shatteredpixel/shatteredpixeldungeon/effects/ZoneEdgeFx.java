/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;

import java.util.ArrayList;
import java.util.Set;

//Maze Tower (O2): 바닥에 칸 덩어리의 바깥 둘레만 옅게 긋는 테두리 (채우지 않음). 숨은 규칙의 범위를 보여 줄 때
//(예: 25층 등불 판막 "4칸 안에서 때려야 들어간다"). 테두리는 칸 안쪽 1픽셀, 바깥으로 튀어나온 모서리는 2픽셀 둥글게 (CellZoneFx와 같은 꼴).
//본 적 있는 칸의 테두리만 보인다. 깜빡이지 않는다.
public class ZoneEdgeFx extends Group {

	private static final int S = DungeonTilemap.SIZE;

	private final Set<Integer> cells;
	private final int w;
	private final ArrayList<ColorBlock> edges = new ArrayList<>();
	private final ArrayList<Integer> edgeCell = new ArrayList<>();
	private float alpha;

	public ZoneEdgeFx( Set<Integer> cells, int color, float alpha ){
		this.cells = cells;
		this.alpha = alpha;
		this.w = Dungeon.level.width();
		for (int c : cells){
			int cx = c % w, cy = c / w;
			float x = cx * S, y = cy * S;
			boolean up = !in(cx, cy - 1), down = !in(cx, cy + 1), left = !in(cx - 1, cy), right = !in(cx + 1, cy);
			boolean tl = up && left, tr = up && right, bl = down && left, br = down && right;
			if (up)    edge(c, x + (tl ? 2 : 0), y, S - (tl ? 2 : 0) - (tr ? 2 : 0), 1, color);
			if (down)  edge(c, x + (bl ? 2 : 0), y + S - 1, S - (bl ? 2 : 0) - (br ? 2 : 0), 1, color);
			if (left)  edge(c, x, y + (tl ? 2 : 0), 1, S - (tl ? 2 : 0) - (bl ? 2 : 0), color);
			if (right) edge(c, x + S - 1, y + (tr ? 2 : 0), 1, S - (tr ? 2 : 0) - (br ? 2 : 0), color);
			if (tl) edge(c, x + 1, y + 1, 1, 1, color);
			if (tr) edge(c, x + S - 2, y + 1, 1, 1, color);
			if (bl) edge(c, x + 1, y + S - 2, 1, 1, color);
			if (br) edge(c, x + S - 2, y + S - 2, 1, 1, color);
		}
		update();
	}

	private boolean in( int x, int y ){
		if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) return false;
		return cells.contains(x + y * w);
	}

	private void edge( int cell, float x, float y, float ww, float hh, int color ){
		if (ww <= 0 || hh <= 0) return;
		ColorBlock b = new ColorBlock(ww, hh, 0xFF000000 | color);
		b.x = x;
		b.y = y;
		add(b);
		edges.add(b);
		edgeCell.add(cell);
	}

	public void alpha( float a ){ alpha = a; }

	@Override
	public void update() {
		super.update();
		if (Dungeon.level == null) return;
		for (int i = 0; i < edges.size(); i++){
			int c = edgeCell.get(i);
			ColorBlock b = edges.get(i);
			b.visible = Dungeon.level.visited[c] || Dungeon.level.mapped[c];
			b.alpha(alpha);
		}
	}
}
