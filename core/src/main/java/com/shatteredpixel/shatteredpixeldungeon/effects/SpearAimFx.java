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

//Maze Tower: 여제 서리창·화염창이 날아오기 1턴 전의 조준선 (명세 22번, 2026-11-21 결정).
//  조준선: 창에서 주인공 쪽으로 가는 1픽셀 점선(2픽셀 켜짐·2픽셀 꺼짐), 끝 쪽으로 흐르듯 움직인다.
//          강화 마탄의 굵은 보라 궤적과 헷갈리지 않게 가늘게.
//  막힘:   벽·기둥에 막히면 선이 그 앞에서 끊긴다. ((결정, 2026-11-21) 끊긴 자리의 × 표시는 없앰)
//  발밑:   선이 주인공에게 닿을 때만 주인공 발밑에 납작한 고리가 은은하게 깜빡인다.
//경로는 MazeEmpress가 실제 창과 같은 Ballistica로 정해 넘겨 준다.
public class SpearAimFx extends Group {

	private static final float DASH = 2f, GAP = 2f;
	private static final float FLOW_SPEED = 12f; //초당 픽셀
	private static final int RING_POINTS = 16;
	private static final float RING_RX = 5f, RING_RY = 2f;

	private final ArrayList<ColorBlock> dashes = new ArrayList<>();
	private final ColorBlock[] ring = new ColorBlock[RING_POINTS];

	private PointF from, to, foot;
	private boolean blocked;
	private int color;
	private float time = 0;

	public SpearAimFx(){
		for (int i = 0; i < RING_POINTS; i++){
			ring[i] = new ColorBlock(1, 1, 0xFFFFFFFF);
			add(ring[i]);
		}
		visible = false;
	}

	//from: 창끝, to: 선이 끝나는 곳, blocked면 to에 ×, 아니면 foot(주인공 발밑)에 고리
	public void set( PointF from, PointF to, boolean blocked, PointF foot, int color ){
		this.from = from;
		this.to = to;
		this.blocked = blocked;
		this.foot = foot;
		this.color = color;
		visible = true;
		layout();
	}

	public void hide(){
		visible = false;
	}

	@Override
	public void update() {
		super.update();
		if (!visible || from == null) return;
		time += Game.elapsed;
		layout();
	}

	private void layout(){
		float dx = to.x - from.x, dy = to.y - from.y;
		float len = (float)Math.sqrt(dx * dx + dy * dy);
		float ang = (float)(Math.atan2(dy, dx) * 180 / Math.PI);
		float ux = len > 0 ? dx / len : 0, uy = len > 0 ? dy / len : 0;
		float period = DASH + GAP;
		float shift = (time * FLOW_SPEED) % period;
		int need = (int)Math.ceil(len / period) + 1;
		while (dashes.size() < need){
			ColorBlock b = new ColorBlock(DASH, 1, 0xFFFFFFFF);
			b.origin.set(0, 0.5f);
			dashes.add(b);
			addToBack(b);
		}
		for (int i = 0; i < dashes.size(); i++){
			ColorBlock b = dashes.get(i);
			float s = i * period + shift - period;
			float e = Math.min(s + DASH, len);
			s = Math.max(s, 0);
			if (i >= need || e <= s){ b.visible = false; continue; }
			b.visible = true;
			b.size(e - s, 1);
			b.hardlight(color);
			b.angle = ang;
			b.x = from.x + ux * s;
			b.y = from.y + uy * s - 0.5f;
			//끝 쪽으로 갈수록 조금 옅게
			b.alpha(0.95f - 0.35f * (s / Math.max(1f, len)));
		}

		boolean ringOn = !blocked && foot != null;
		//은은하게 깜빡임 (약 1.2초 주기)
		float a = 0.35f + 0.35f * (0.5f + 0.5f * (float)Math.sin(time * 5.2f));
		for (int i = 0; i < RING_POINTS; i++){
			ColorBlock r = ring[i];
			r.visible = ringOn;
			if (!ringOn) continue;
			double t = Math.PI * 2 * i / RING_POINTS;
			r.x = Math.round(foot.x + (float)Math.cos(t) * RING_RX - 0.5f);
			r.y = Math.round(foot.y + (float)Math.sin(t) * RING_RY - 0.5f);
			r.hardlight(color);
			r.alpha(a);
		}
	}
}
