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

//Maze Tower: 여제 강화 마탄의 궤적 경고 (명세 22번). 그림 타일을 칸마다 돌려 붙이면 대각선에서 깨져서 코드로 그린다.
//세 겹 빛줄기(바깥 흐린 보라 · 가운데 보라 · 가는 흰 심)가 여제 쪽에서 멀어질수록 옅어지고, 은은하게 숨 쉬듯 밝아졌다 어두워지며,
//작은 빛 알갱이가 여제 쪽에서 끝 쪽으로 흘러간다. 어떤 각도로도 끊기지 않는다.
public class BoltWarnLine extends Group {

	private static final float SEG = 6f; //옅어지는 단계를 만들려고 줄을 이 길이로 나눈다

	//{두께, 색, 기본 진하기}
	private static final float[][] LAYERS = {
			{7f,   0x7A52E0, 0.16f},
			{3.5f, 0xB08CFF, 0.30f},
			{1.2f, 0xD6E4FF, 0.65f}, //가운데 심은 옅은 푸른빛
	};

	private final PointF a, b;
	private final float len, angle;
	private final ArrayList<ColorBlock> blocks = new ArrayList<>();
	private final ArrayList<Float> baseAlpha = new ArrayList<>();
	private final ColorBlock[] glints = new ColorBlock[3];
	private float time = 0;

	public BoltWarnLine( PointF from, PointF to ){
		a = from;
		b = to;
		float dx = b.x - a.x, dy = b.y - a.y;
		len = (float)Math.sqrt(dx*dx + dy*dy);
		angle = (float)(Math.atan2(dy, dx) * 180 / Math.PI);
		float ux = dx / Math.max(len, 0.001f), uy = dy / Math.max(len, 0.001f);

		int n = Math.max(1, (int)Math.ceil(len / SEG));
		for (float[] layer : LAYERS){
			float t = layer[0];
			for (int i = 0; i < n; i++){
				float s = i * SEG;
				float l = Math.min(SEG + 0.5f, len - s); //이음매가 벌어지지 않게 조금 겹친다
				ColorBlock c = new ColorBlock(l, t, 0xFF000000 | (int)layer[1]);
				//ColorBlock은 1×1을 늘린 것이라 돌리는 중심도 늘리기 전 크기(0.5)로 준다. 그래야 두꺼운 줄도 경로 가운데에 놓인다
				c.origin.set(0, 0.5f);
				c.x = a.x + ux * s;
				c.y = a.y + uy * s - 0.5f;
				c.angle = angle;
				//여제 쪽이 진하고 끝으로 갈수록 옅다
				float fade = 1f - 0.55f * (s / Math.max(len, 1f));
				float al = layer[2] * fade;
				c.alpha(al);
				add(c);
				blocks.add(c);
				baseAlpha.add(al);
			}
		}
		for (int i = 0; i < glints.length; i++){
			ColorBlock g = new ColorBlock(2f, 2f, 0xFFFFFFFF);
			add(g);
			glints[i] = g;
		}
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float pulse = 0.75f + 0.25f * (float)Math.sin(time * 6f);
		for (int i = 0; i < blocks.size(); i++){
			blocks.get(i).alpha(baseAlpha.get(i) * pulse);
		}
		//빛 알갱이: 여제 쪽에서 끝 쪽으로 흘러간다 (0.7초에 한 번, 셋이 엇갈려서)
		for (int i = 0; i < glints.length; i++){
			float p = ((time / 0.7f) + i / (float)glints.length) % 1f;
			glints[i].x = a.x + (b.x - a.x) * p - 1f;
			glints[i].y = a.y + (b.y - a.y) * p - 1f;
			glints[i].alpha(0.8f * (1f - p) * pulse);
		}
	}
}
