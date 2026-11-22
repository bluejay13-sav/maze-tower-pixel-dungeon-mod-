/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.HashMap;

//Maze Tower: 25층 최종보스 연출 도우미 (지시서 1122b 1절 공통 규칙).
//  화면 흔들림은 항상 1px, 1프레임. 영웅 피격: 하얗게 번쩍(1프레임) + 맞은 방향으로 1px 움찔.
//  경고 칸: 칸 단위, 안쪽은 옅게·테두리는 진하게. 앞에서부터 한 칸씩 차오르고, 지나간 칸부터 꺼진다.
public class Heart25Fx {

	public static final int BLOOD = 0xC0182A;

	//화면 흔들림: 1px, 1프레임
	public static void shake(){
		PixelScene.shake(1f, 1 / 60f);
	}

	//영웅 피격 연출 (from 쪽에서 맞음)
	public static void heroHit( Char target, int fromCell ){
		if (target == null || target.sprite == null || target.sprite.parent == null) return;
		target.sprite.flash();
		PointF a = DungeonTilemap.tileCenterToWorld(fromCell), b = DungeonTilemap.tileCenterToWorld(target.pos);
		float dx = Math.signum(b.x - a.x), dy = Math.signum(b.y - a.y);
		nudge(target.sprite, dx, dy);
	}

	//1px 움찔하고 돌아온다
	public static void nudge( final Image s, final float dx, final float dy ){
		if (s == null || s.parent == null) return;
		s.x += dx; s.y += dy;
		s.parent.add(new Gizmo(){
			float t = 0.07f;
			@Override
			public void update() {
				t -= Game.elapsed;
				if (t <= 0){ s.x -= dx; s.y -= dy; killAndErase(); }
			}
		});
	}

	public static void blood( int cell, int n ){
		CellEmitter.center(cell).burst(BloodParticle.BURST, n);
	}

	// =====================================================================
	// 경고 칸
	// =====================================================================
	public static class CellWarn extends Group {

		private final HashMap<Integer, Group> cells = new HashMap<>();
		private final HashMap<Integer, ColorBlock[]> blocks = new HashMap<>();
		private final ArrayList<Integer> order = new ArrayList<>();
		private final HashMap<Integer, Float> peak = new HashMap<>();
		private float time = 0;
		private final float stepDelay;
		private boolean fading = false;
		private float fade = 1f;
		public float pulse = 0f; //0이면 맥동 없음

		public CellWarn( ArrayList<Integer> list, float[] alphas, int color, float stepDelay ){
			this.stepDelay = stepDelay;
			for (int i = 0; i < list.size(); i++){
				int c = list.get(i);
				if (cells.containsKey(c)) continue;
				float a = alphas == null ? 1f : alphas[Math.min(i, alphas.length - 1)];
				ColorBlock[] bl = new ColorBlock[5];
				Group g = cellGroup(c, color, bl);
				blocks.put(c, bl);
				g.visible = stepDelay <= 0;
				add(g);
				cells.put(c, g);
				order.add(c);
				peak.put(c, a);
			}
			apply();
		}

		private static Group cellGroup( int cell, int color, ColorBlock[] bl ){
			Group g = new Group();
			PointF p = DungeonTilemap.tileToWorld(cell);
			ColorBlock in = new ColorBlock(16, 16, 0xFF000000 | color);
			in.x = p.x; in.y = p.y;
			g.add(in);
			bl[0] = in;
			int k = 1;
			int[][] edges = {{0,0,16,1},{0,15,16,1},{0,0,1,16},{15,0,1,16}};
			for (int[] e : edges){
				ColorBlock b = new ColorBlock(e[2], e[3], 0xFF000000 | color);
				b.x = p.x + e[0]; b.y = p.y + e[1];
				g.add(b);
				bl[k++] = b;
			}
			return g;
		}

		private void apply(){
			float k = fade * (pulse > 0 ? 0.75f + 0.25f * (float)Math.sin(time * pulse) : 1f);
			for (Integer c : order){
				Group g = cells.get(c);
				if (g == null) continue;
				float a = peak.get(c) * k;
				ColorBlock[] bl = blocks.get(c);
				if (bl == null) continue;
				for (int i = 0; i < bl.length; i++) if (bl[i] != null) bl[i].alpha(i == 0 ? a * 0.22f : a * 0.85f);
			}
		}

		public void off( int cell ){
			Group g = cells.remove(cell);
			blocks.remove(cell);
			if (g != null) g.killAndErase();
		}

		public boolean has( int cell ){ return cells.containsKey(cell); }

		public void fadeOut(){ fading = true; }

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			if (stepDelay > 0){
				for (int i = 0; i < order.size(); i++){
					Group g = cells.get(order.get(i));
					if (g != null && !g.visible && time >= i * stepDelay) g.visible = true;
				}
			}
			if (fading){
				fade -= Game.elapsed / 0.25f;
				if (fade <= 0){ killAndErase(); return; }
			}
			apply();
		}

		public static CellWarn show( ArrayList<Integer> list, float[] alphas, int color, float stepDelay ){
			CellWarn w = new CellWarn(list, alphas, color, stepDelay);
			GameScene.groupEffect(w);
			return w;
		}
	}

	//화면 가장자리 어둡게 조였다 풀리기 (비네트). 영웅 둘레 큰 원 바깥이 어두워진다
	public static class Vignette extends Image {
		private float t = 0;
		private final float life;
		public Vignette( float life ){
			super("sprites/maze_fx20w_dark_hole.png");
			this.life = life;
			alpha(0);
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t >= life || Dungeon.hero == null || Dungeon.hero.sprite == null){ killAndErase(); return; }
			PointF c = Dungeon.hero.sprite.center();
			float r = 150f;
			scale.set(r * 2 / width);
			x = c.x - width / 2f;
			y = c.y - height / 2f;
			origin.set(width / 2f, height / 2f);
			float beat = (float)Math.abs(Math.sin(t * Math.PI * 70f / 60f));
			alpha(0.35f * beat * Math.min(1f, (life - t) / 0.3f));
		}
	}

	public static Emitter emitterAt( int cell ){
		return CellEmitter.center(cell);
	}
}
