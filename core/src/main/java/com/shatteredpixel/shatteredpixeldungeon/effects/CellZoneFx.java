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
import java.util.Map;

//Maze Tower: 망령 왕 "인간을 죽이는 마법"의 범위 경고와 광선 (명세 22번).
//줄마다 따로 그린 띠·그림은 대각선에서 벽 쪽 세모가 비고, 바깥 줄이 벽에 막히면 테두리가 끊겼다.
//그래서 맞는 칸들을 칸 단위로 빈틈없이 칠하고(한 덩어리), 그 덩어리의 바깥 둘레 전체에 회색빛 테두리를 긋는다.
//(결정) 바깥으로 튀어나온 모서리는 몇 픽셀 둥글게 깎는다. 벽에 막혀 폭이 줄어드는 곳도 테두리가 따라간다.
//광선은 줄마다 명암을 둔다: 가운데 줄이 가장 밝고 바깥 줄로 갈수록 짙은 푸른색, 왕에게서 멀어질수록 조금 옅다.
public class CellZoneFx extends Group {

	public enum Mode { WARN, BEAM }

	private static final int S = DungeonTilemap.SIZE;
	private static final int EDGE_COLOR = 0xA9B3BE; //(결정) 회색빛 테두리

	//광선 줄별 색 (가운데 → 바깥)
	private static final int[] BEAM_LANE_COLORS = { 0xE4F4FF, 0x9ACFFF, 0x5E93DC };
	private static final int WARN_COLOR = 0x6FA8FF;

	private final Mode mode;
	private final ArrayList<ColorBlock> fills = new ArrayList<>();
	private final ArrayList<Float> fillBase = new ArrayList<>();
	private final ArrayList<ColorBlock> edges = new ArrayList<>();
	private float time = 0;

	private final Map<Integer, Integer> lanes;
	private final int origin;
	private final int w;

	//laneOf: 칠할 칸 → 줄 번호(가운데 0, 바깥으로 ±1, ±2), origin: 테두리를 긋지 않을 이웃 칸(왕의 칸)
	public CellZoneFx( Mode mode, Map<Integer, Integer> laneOf, int origin ){
		this.mode = mode;
		this.lanes = laneOf;
		this.origin = origin;
		this.w = Dungeon.level.width();
		int ox = origin % w, oy = origin / w;
		for (Map.Entry<Integer, Integer> en : laneOf.entrySet()){
			int c = en.getKey();
			int cx = c % w, cy = c / w;
			float x = cx * S, y = cy * S;

			int color;
			float base;
			if (mode == Mode.BEAM){
				int k = Math.min(BEAM_LANE_COLORS.length - 1, Math.abs(en.getValue()));
				color = BEAM_LANE_COLORS[k];
				//왕에게서 멀어질수록 조금 옅게
				int d = Math.max(Math.abs(cx - ox), Math.abs(cy - oy));
				base = (k == 0 ? 0.85f : 0.65f) * (1f - 0.3f * Math.min(1f, d / 12f));
			} else {
				color = WARN_COLOR;
				base = 0.16f;
			}

			boolean up = !in(cx, cy - 1), down = !in(cx, cy + 1), left = !in(cx - 1, cy), right = !in(cx + 1, cy);
			//바깥으로 튀어나온 모서리 (두 변이 모두 바깥과 맞닿음)
			boolean tl = up && left, tr = up && right, bl = down && left, br = down && right;

			//칠하기: 둥근 모서리만큼 위·아래 두 줄씩 안으로 들인다 (2픽셀, 1픽셀)
			fillRow(x, y,         tl ? 2 : 0, tr ? 2 : 0, 1, color, base);
			fillRow(x, y + 1,     tl ? 1 : 0, tr ? 1 : 0, 1, color, base);
			fillRow(x, y + 2,     0, 0, S - 4, color, base);
			fillRow(x, y + S - 2, bl ? 1 : 0, br ? 1 : 0, 1, color, base);
			fillRow(x, y + S - 1, bl ? 2 : 0, br ? 2 : 0, 1, color, base);

			//테두리 (칸 안쪽 1픽셀)
			if (up)    edge(x + (tl ? 2 : 0), y, S - (tl ? 2 : 0) - (tr ? 2 : 0), 1);
			if (down)  edge(x + (bl ? 2 : 0), y + S - 1, S - (bl ? 2 : 0) - (br ? 2 : 0), 1);
			if (left)  edge(x, y + (tl ? 2 : 0), 1, S - (tl ? 2 : 0) - (bl ? 2 : 0));
			if (right) edge(x + S - 1, y + (tr ? 2 : 0), 1, S - (tr ? 2 : 0) - (br ? 2 : 0));
			//둥근 모서리의 대각선 한 픽셀
			if (tl) edge(x + 1, y + 1, 1, 1);
			if (tr) edge(x + S - 2, y + 1, 1, 1);
			if (bl) edge(x + 1, y + S - 2, 1, 1);
			if (br) edge(x + S - 2, y + S - 2, 1, 1);
		}
		update();
	}

	//그 칸이 덩어리에 들어가는지 (왕의 칸도 들어간 것으로 쳐서 그쪽엔 테두리·둥근 모서리가 없다)
	private boolean in( int x, int y ){
		if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) return false;
		int c = x + y * w;
		return c == origin || lanes.containsKey(c);
	}

	private void fillRow( float x, float y, int insetL, int insetR, int h, int color, float base ){
		ColorBlock b = block(x + insetL, y, S - insetL - insetR, h, color);
		fills.add(b);
		fillBase.add(base);
	}

	private void edge( float x, float y, float ww, float hh ){
		if (ww <= 0 || hh <= 0) return;
		edges.add(block(x, y, ww, hh, EDGE_COLOR));
	}

	private ColorBlock block( float x, float y, float ww, float hh, int color ){
		ColorBlock b = new ColorBlock(ww, hh, 0xFF000000 | color);
		b.x = x;
		b.y = y;
		add(b);
		return b;
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float fillK, edgeA;
		if (mode == Mode.WARN){
			//숨 쉬듯 은은하게
			float p = 0.7f + 0.3f * (float)Math.sin(time * 5f);
			fillK = p; edgeA = 0.45f * p;
		} else {
			//광선: 잠깐 환하게 번쩍인 뒤 사라진다 (번쩍이는 동안 살짝 일렁임)
			float hold = 0.15f, fade = 0.35f;
			if (time > hold + fade){
				killAndErase();
				return;
			}
			float k = time < hold ? 1f : Math.max(0f, 1f - (time - hold) / fade);
			fillK = k * (0.92f + 0.08f * (float)Math.sin(time * 60f));
			edgeA = 0.7f * k;
		}
		for (int i = 0; i < fills.size(); i++) fills.get(i).alpha(fillBase.get(i) * fillK);
		for (ColorBlock b : edges) b.alpha(edgeA);
	}
}
