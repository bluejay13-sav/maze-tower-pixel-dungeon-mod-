/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

import java.util.ArrayList;

//Maze Tower (L1): 짝 잃은 시민 둘 사이의 영혼 끈. 두 손 사이를 아래로 살짝 처진 곡선으로 잇는 아주 옅은 푸른 실과,
//그 위를 천천히 흐르는 빛 알갱이 넷 (움짤 docs/sprites/mobs/l_draft/anim_l1l2.html). 매 프레임 두 그림의 자리로 다시 잇는다.
public class SoulTether extends Group {

	private static final int COLOR = 0xFF9FD8FF, DOT = 0xFFC8ECFF;
	private static final float STEP = 1.5f;   //실을 이루는 점 사이 간격 (픽셀)
	private static final float SAG = 4f;      //가운데가 처지는 정도
	private static final float FLOW = 0.25f;  //빛 알갱이가 한 바퀴 도는 빠르기 (초당)

	private final CharSprite a, b;
	private final ArrayList<ColorBlock> line = new ArrayList<>();
	private final ColorBlock[] dots = new ColorBlock[4];
	private float time = 0;
	public boolean show = true; //둘 다 서 있을 때만 (쓰러져 다시 일어나는 중이면 숨김)

	public SoulTether( CharSprite a, CharSprite b ){
		this.a = a; this.b = b;
		for (int i = 0; i < dots.length; i++){ dots[i] = new ColorBlock(1, 1, DOT); add(dots[i]); }
	}

	@Override
	public void update() {
		super.update();
		if (a.parent == null || b.parent == null){ killAndErase(); return; }
		visible = show && a.visible && b.visible;
		if (!visible) return;
		time += Game.elapsed;
		//손 높이: 그림 위에서 7픽셀쯤
		float x1 = a.x + a.width / 2f, y1 = a.y + 7f, x2 = b.x + b.width / 2f, y2 = b.y + 7f;
		float mx = (x1 + x2) / 2f, my = (y1 + y2) / 2f + SAG + (float)Math.sin(time * 2) * 0.8f;
		float len = (float)Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		int n = Math.max(2, (int)(len / STEP));
		while (line.size() < n){ ColorBlock c = new ColorBlock(1, 1, COLOR); addToBack(c); line.add(c); }
		for (int i = 0; i < line.size(); i++){
			ColorBlock c = line.get(i);
			if (i >= n){ c.visible = false; continue; }
			float t = i / (float)(n - 1);
			c.visible = true;
			c.x = bez(x1, mx, x2, t) - 0.5f; c.y = bez(y1, my, y2, t) - 0.5f;
			c.alpha(0.22f);
		}
		for (int i = 0; i < dots.length; i++){
			float t = (time * FLOW + i / (float)dots.length) % 1f;
			dots[i].x = bez(x1, mx, x2, t) - 0.5f; dots[i].y = bez(y1, my, y2, t) - 0.5f;
			dots[i].alpha(0.55f * (float)Math.sin(Math.PI * t));
		}
	}

	private static float bez( float p0, float p1, float p2, float t ){
		return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
	}
}
