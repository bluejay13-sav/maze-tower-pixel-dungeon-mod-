/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

import java.util.ArrayList;

//Maze Tower: 망령 왕 "인간을 죽이는 마법"의 범위 경고 (명세 22번).
//예전에는 칸마다 경고 그림을 붙여 칸 사이 이음매가 보이고 지저분했다. 줄마다 하나로 이어진 옅은 푸른 띠를 깔고,
//범위 바깥 가장자리에만 가는 밝은 선을 그어 범위 전체가 한 덩어리로 깔끔하게 보이게 한다. 숨 쉬듯 은은하게 밝아졌다 어두워진다.
public class BeamWarnZone extends Group {

	private static final int FILL_COLOR = 0x6FA8FF;
	private static final int EDGE_COLOR = 0xD2E8FF;
	private static final float FILL_ALPHA = 0.16f;
	private static final float EDGE_ALPHA = 0.55f;

	private final ArrayList<ColorBlock> fills = new ArrayList<>();
	private final ArrayList<ColorBlock> edges = new ArrayList<>();
	private float time = 0;

	//한 줄의 띠 (a~b, 두께 width). 나란한 줄끼리 겹치지 않게 두께는 줄 간격과 같게 준다
	public void band( PointF a, PointF b, float width ){
		fills.add(block(a, b, width, FILL_COLOR));
	}

	//범위 가장자리 선
	public void edge( PointF a, PointF b ){
		edges.add(block(a, b, 1f, EDGE_COLOR));
	}

	private ColorBlock block( PointF a, PointF b, float t, int color ){
		float dx = b.x - a.x, dy = b.y - a.y;
		float len = (float)Math.sqrt(dx*dx + dy*dy);
		ColorBlock c = new ColorBlock(Math.max(len, 1f), t, 0xFF000000 | color);
		//ColorBlock은 1×1을 늘린 것이라 돌리는 중심도 늘리기 전 크기(0.5)로 준다 (줄이 경로 가운데에 놓인다)
		c.origin.set(0, 0.5f);
		c.x = a.x;
		c.y = a.y - 0.5f;
		c.angle = (float)(Math.atan2(dy, dx) * 180 / Math.PI);
		add(c);
		return c;
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float pulse = 0.7f + 0.3f * (float)Math.sin(time * 5f);
		for (ColorBlock c : fills) c.alpha(FILL_ALPHA * pulse);
		for (ColorBlock c : edges) c.alpha(EDGE_ALPHA * pulse);
	}
}
