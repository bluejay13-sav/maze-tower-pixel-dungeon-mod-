/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Camera;
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
		//(2026-11-22) 원작 근접 피격처럼 맞은 쪽 반대로 피가 튐 (기술 피해는 Char.attack을 거치지 않아 피가 안 나왔음)
		if (fromCell != target.pos) target.sprite.bloodBurstA(a, Math.max(1, target.HT / 3));
		else target.sprite.bloodBurstA(new PointF(b.x, b.y - 8), Math.max(1, target.HT / 3));
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

	//(2026-11-22) 화면 전체를 덮는 어둠: 부드러운 구멍(maze_fx20w_dark_hole.png) + 그 바깥을 덮는 검은 판 4장.
	//  예전에는 구멍 그림 하나만 키워 써서 그림 밖(화면 가장자리)은 어두워지지 않았다. 안개 위에 그린다.
	public static abstract class ScreenDark extends Group {
		private static final float HOLE_TEX = 128f;  //그림 크기
		private static final float HOLE_MID = 42f;   //그림 안 부드러운 가장자리의 가운데 반지름
		private static final float BIG = 4000f;
		private final Image hole;
		private final ColorBlock[] sides = new ColorBlock[4];
		protected final float screenR; //화면 전체를 덮을 만큼 큰 반지름
		protected float t = 0;

		public ScreenDark(){
			SmartTexture tx = TextureCache.get("sprites/maze_fx20w_dark_hole.png");
			tx.filter(Texture.LINEAR, Texture.LINEAR);
			hole = new Image(tx);
			add(hole);
			for (int i = 0; i < 4; i++){ sides[i] = new ColorBlock(1, 1, 0xFF000000); add(sides[i]); }
			Camera c = Camera.main;
			float w = c == null ? 400 : c.width / c.zoom, h = c == null ? 300 : c.height / c.zoom;
			screenR = (float)Math.sqrt(w * w + h * h) + 40f;
			set(new PointF(0, 0), screenR, 0);
		}

		//center 둘레 반지름 r 바깥을 alpha만큼 어둡게
		protected void set( PointF center, float r, float alpha ){
			float s = Math.max(r, 0.01f) / HOLE_MID;
			hole.scale.set(s);
			float size = HOLE_TEX * s;
			hole.x = center.x - size / 2f;
			hole.y = center.y - size / 2f;
			sides[0].size(BIG * 2, BIG); sides[0].x = hole.x - BIG;  sides[0].y = hole.y - BIG;
			sides[1].size(BIG * 2, BIG); sides[1].x = hole.x - BIG;  sides[1].y = hole.y + size;
			sides[2].size(BIG, size);    sides[2].x = hole.x - BIG;  sides[2].y = hole.y;
			sides[3].size(BIG, size);    sides[3].x = hole.x + size; sides[3].y = hole.y;
			hole.alpha(alpha);
			for (ColorBlock b : sides) b.alpha(alpha);
		}

		protected static float smooth( float p ){
			p = Math.max(0, Math.min(1, p));
			return p * p * (3 - 2 * p);
		}
	}

	//심장 박동 파동(3페이즈): 박동에 맞춰 영웅 둘레 바깥이 살짝 어두워졌다 풀림 (화면 전체)
	public static class Vignette extends ScreenDark {
		private final float life;
		public Vignette( float life ){
			this.life = life;
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t >= life || Dungeon.hero == null || Dungeon.hero.sprite == null){ killAndErase(); return; }
			float beat = (float)Math.abs(Math.sin(t * Math.PI * 70f / 60f));
			set(Dungeon.hero.sprite.center(), 70f, MazeTower.B25_VIGNETTE_ALPHA * beat * Math.min(1f, (life - t) / 0.3f));
		}
	}

	//발밑 납작한 고리 (점으로 된 타원이 퍼지며 사라짐). 착지·재구성 때
	public static void footRing( Image s ){
		if (s == null || s.parent == null) return;
		ellipse(s.parent, s.x + s.width() / 2f, s.y + s.height() - 1, 4, 12, 0.35f, 0xFFF0C0C8, 14);
	}

	public static void cellRing( int cell, float maxR, float life ){
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		Group g = new Group();
		GameScene.groupEffect(g);
		ellipse(g, p.x, p.y + 5, 3, maxR, life, 0xFFC0182A, 16);
	}

	//점 n개로 된 납작한 타원(가로:세로 = 2:1)이 r0 → r1로 퍼지며 흐려진다
	public static void ellipse( final Group parent, final float cx, final float cy, final float r0, final float r1, final float life, int color, int n ){
		final ColorBlock[] dots = new ColorBlock[n];
		for (int i = 0; i < n; i++){ dots[i] = new ColorBlock(1, 1, color); parent.add(dots[i]); }
		parent.add(new Gizmo(){
			float t = 0;
			@Override public void update(){
				t += Game.elapsed;
				float k = Math.min(1f, t / life);
				float r = r0 + (r1 - r0) * k;
				for (int i = 0; i < dots.length; i++){
					double a = Math.PI * 2 * i / dots.length;
					dots[i].x = cx + (float)Math.cos(a) * r;
					dots[i].y = cy + (float)Math.sin(a) * r / 2f;
					dots[i].alpha(1f - k);
				}
				if (k >= 1){ for (ColorBlock d : dots) d.killAndErase(); killAndErase(); }
			}
		});
	}

	//포효: 머리 쪽에서 점으로 된 동그란 소리 고리 세 겹이 차례로 퍼짐 (분홍빛)
	public static void roarRings( final Group host, final float cx, final float cy ){
		//(주의) 안쪽 Gizmo에서 "parent"는 Gizmo 자신의 부모 칸이라, 지운 뒤엔 null이 된다. 그래서 host라는 이름을 쓴다
		for (int k = 0; k < 3; k++){
			final int kk = k;
			host.add(new Gizmo(){
				float wait = 0.15f * kk;
				@Override public void update(){
					wait -= Game.elapsed;
					if (wait <= 0){
						killAndErase();
						final ColorBlock[] dots = new ColorBlock[20];
						for (int i = 0; i < dots.length; i++){ dots[i] = new ColorBlock(1, 1, 0xFFFFA3C7); host.add(dots[i]); }
						host.add(new Gizmo(){
							float t = 0;
							@Override public void update(){
								t += Game.elapsed;
								float q = Math.min(1f, t / 0.55f);
								float r = 6 + 26 * q;
								for (int i = 0; i < dots.length; i++){
									double a = Math.PI * 2 * i / dots.length;
									dots[i].x = cx + (float)Math.cos(a) * r;
									dots[i].y = cy + (float)Math.sin(a) * r;
									dots[i].alpha(1f - q);
								}
								if (q >= 1){ for (ColorBlock d : dots) d.killAndErase(); killAndErase(); }
							}
						});
					}
				}
			});
		}
	}

	//2페이즈 진입: 화면 가장자리부터 어둠이 짐승 쪽으로 조여들어 짐승 둘레만 밝게 남았다가(잠시 유지), 다시 풀린다
	public static class DarkEdges extends ScreenDark {
		private final float life;
		private final com.watabou.noosa.Visual focus;
		public DarkEdges( com.watabou.noosa.Visual focus, float life ){
			this.focus = focus;
			this.life = life;
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (t >= life || focus == null || focus.parent == null){ killAndErase(); return; }
			float k = t / life;
			float in = MazeTower.B25_DARK_IN, hold = MazeTower.B25_DARK_HOLD;
			float minR = MazeTower.B25_DARK_R;
			float r, a = MazeTower.B25_DARK_ALPHA;
			if (k < in){
				float p = smooth(k / in);
				r = screenR + (minR - screenR) * p;
				a *= Math.min(1f, k / in * 2f);
			} else if (k < in + hold){
				r = minR;
			} else {
				float p = smooth((k - in - hold) / (1f - in - hold));
				r = minR + (screenR - minR) * p;
				a *= 1f - p;
			}
			set(focus.center(), r, a);
		}
	}

	public static Emitter emitterAt( int cell ){
		return CellEmitter.center(cell);
	}
}
