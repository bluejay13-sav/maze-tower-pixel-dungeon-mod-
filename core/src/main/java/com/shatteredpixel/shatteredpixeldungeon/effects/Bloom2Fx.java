/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WindParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.PointF;


//Maze Tower: 2차 특성 개편 그림·연출 (E단계). 그림은 fx2_sheet.png (좌표는 docs/sprites/talent2/README.md).
//  시트 그림: 원형 휩쓸기, 원형 충격파, 표식 터짐, 방어 무시 파편, 조준 과녁, 마나 장막, 마력 파편, 머리 위 표식
//  코드 입자: 강타 굵은 베기, 질풍 바람, 연쇄 하늘색 빛줄기, 과충전 마력 넘침, 충격 충돌, 원소 순환 3색 빛, 표식이 옮겨 붙는 빛
public class Bloom2Fx {

	// =====================================================================
	// fx2_sheet.png 조각
	// =====================================================================

	public enum Sheet {
		SWEEP     (32, 32, new int[][]{{0,0},{34,0},{68,0}}),
		SHOCKWAVE (48, 48, new int[][]{{0,34},{50,34},{100,34}}),
		MARK_POP  (16, 16, new int[][]{{0,84},{18,84},{36,84}}),
		PIERCE    (16, 16, new int[][]{{0,102},{18,102}}),
		TARGET    (16, 16, new int[][]{{0,120}}),
		VEIL      (16, 16, new int[][]{{0,138}}),
		SHARD     ( 5,  5, new int[][]{{0,156}});

		public final int w, h;
		final int[][] frames;

		Sheet(int w, int h, int[][] frames){
			this.w = w;
			this.h = h;
			this.frames = frames;
		}
	}

	//머리 위 표식 1~4개 (x, y, w, h)
	private static final int[][] MARKS = {{0,181,5,5},{7,181,10,5},{19,181,11,9},{32,181,16,9}};

	private static TextureFilm film(Image img, Sheet s){
		TextureFilm f = new TextureFilm(img.texture);
		for (int i = 0; i < s.frames.length; i++){
			int[] p = s.frames[i];
			f.add(i, p[0], p[1], p[0] + s.w, p[1] + s.h);
		}
		return f;
	}

	//한 번 재생하고 사라지는 그림 (그림 한 장짜리면 잠깐 보였다 흐려진다)
	public static Image play(Sheet s, float cx, float cy, int fps){
		final MovieClip c = new MovieClip(Assets.Effects.MAZE_FX2);
		TextureFilm f = film(c, s);
		c.x = cx - s.w / 2f;
		c.y = cy - s.h / 2f;
		if (s.frames.length > 1){
			Object[] idx = new Object[s.frames.length];
			for (int i = 0; i < idx.length; i++) idx[i] = i;
			c.listener = new MovieClip.Listener() {
				@Override
				public void onComplete(MovieClip.Animation a) {
					c.killAndErase();
				}
			};
			c.play(new MovieClip.Animation(fps, false).frames(f, idx));
		} else {
			c.frame(f.get(0));
		}
		GameScene.effect(c);
		if (s.frames.length == 1) fadeOut(c, 0.45f); //한 장짜리는 잠깐 보였다 흐려진다
		return c;
	}

	public static Image playOn(Sheet s, Char ch, int fps){
		if (!seen(ch)) return null;
		PointF p = ch.sprite.destinationCenter();
		return play(s, p.x, p.y, fps);
	}

	public static Image playAtCell(Sheet s, int cell, int fps){
		if (Dungeon.level == null || cell < 0 || !Dungeon.level.heroFOV[cell]) return null;
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		return play(s, p.x, p.y, fps);
	}

	private static void fadeOut(final Image img, final float time){
		if (img.parent == null) return;
		img.parent.add(new Tweener(img, time) {
			@Override
			protected void updateValues(float progress) {
				img.alpha(1f - progress);
			}

			@Override
			protected void onComplete() {
				img.killAndErase();
				killAndErase();
			}
		});
	}

	static boolean seen(Char ch){
		return ch != null && ch.sprite != null && ch.sprite.parent != null && ch.sprite.visible;
	}

	// =====================================================================
	// 시트 그림을 쓰는 연출
	// =====================================================================

	//질풍 각성: 주인공 둘레 원형 휩쓸기
	public static void galeSweep(Char hero){
		if (!seen(hero)) return;
		playOn(Sheet.SWEEP, hero, 18);
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);
	}

	//과충전·기세 각성: 원형 충격파. tiles = 퍼지는 반경(칸)
	public static void shockwave(Char center, float tiles){
		if (!seen(center)) return;
		Image c = playOn(Sheet.SHOCKWAVE, center, 15);
		if (c != null){
			float scale = (tiles * 2 + 1) * DungeonTilemap.SIZE / (float) Sheet.SHOCKWAVE.w;
			c.origin.set(Sheet.SHOCKWAVE.w / 2f, Sheet.SHOCKWAVE.h / 2f);
			c.scale.set(scale);
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 1.3f);
	}

	//표식 폭발: 표식이 터짐
	public static void markPop(Char enemy){
		playOn(Sheet.MARK_POP, enemy, 18);
	}

	//방어 무시: 갑옷 파편 (돌진·마나 흡수·집중 각성)
	public static void pierce(Char enemy){
		playOn(Sheet.PIERCE, enemy, 12);
	}

	//확정타: 조준 과녁
	public static void target(Char enemy){
		playOn(Sheet.TARGET, enemy, 1);
	}

	//마나 장막
	public static void veil(Char hero){
		playOn(Sheet.VEIL, hero, 1);
	}

	//넘치는 힘 각성: 주인공에게서 대상까지 날아가는 마력 파편
	public static void shard(Char from, Char to){
		if (!seen(from) || to == null || to.sprite == null) return;
		final Image img = new Image(Assets.Effects.MAZE_FX2);
		img.frame(film(img, Sheet.SHARD).get(0));
		PointF a = from.sprite.center(), b = to.sprite.center();
		img.x = a.x - Sheet.SHARD.w / 2f;
		img.y = a.y - Sheet.SHARD.h / 2f;
		GameScene.effect(img);
		float dist = (float)Math.hypot(b.x - a.x, b.y - a.y);
		img.parent.add(new PosTweener(img, new PointF(b.x - Sheet.SHARD.w / 2f, b.y - Sheet.SHARD.h / 2f), Math.max(0.08f, dist / 220f)) {
			@Override
			protected void onComplete() {
				super.onComplete();
				img.killAndErase();
				killAndErase();
			}
		});
	}

	// =====================================================================
	// 머리 위 표식 (1·2개 가로, 3개 세모, 4개 사다리꼴). Mark.fx에서 켜고 끈다.
	// =====================================================================

	public static class MarkOverhead extends Image {
		private final CharSprite sprite;
		private int stacks = -1;

		public MarkOverhead(CharSprite sprite){
			super(Assets.Effects.MAZE_FX2);
			this.sprite = sprite;
		}

		public void stacks(int n){
			n = Math.max(1, Math.min(4, n));
			if (n == stacks) return;
			stacks = n;
			int[] r = MARKS[n - 1];
			frame(r[0], r[1], r[2], r[3]);
		}

		@Override
		public void update() {
			super.update();
			if (sprite == null || sprite.parent == null){
				killAndErase();
				return;
			}
			visible = sprite.visible && stacks > 0;
			x = sprite.x + (sprite.width() - width()) / 2f;
			y = sprite.y - height() - 1;
			PixelScene.align(this);
		}
	}

	public static MarkOverhead markOverhead(CharSprite s){
		if (s == null || s.parent == null) return null;
		MarkOverhead m = new MarkOverhead(s);
		s.parent.add(m);
		return m;
	}

	// =====================================================================
	// 코드 입자 연출
	// =====================================================================

	//강타: 굵은 베기 + 약한 화면 흔들림
	public static void heavyBlow(Char hero, Char enemy){
		if (!seen(enemy)) return;
		PointF e = enemy.sprite.destinationCenter();
		float a = hero != null && hero.sprite != null
				? (float)Math.atan2(e.y - hero.sprite.center().y, e.x - hero.sprite.center().x) : 0;
		new SlashTrail(9f, 4.5f, a - 1.9f, 2.6f, 0f, 0.09f, 0.22f)
				.trailColor(0xFFE2A8)
				.show(enemy.sprite.parent, e);
		PixelScene.shake(1, 0.15f);
	}

	//베기 궤적 (흘리기 각성 반격, 잔향 각성 잔상)
	public static void slash(Char hero, Char enemy, int color){
		if (!seen(enemy)) return;
		PointF e = enemy.sprite.destinationCenter();
		float a = hero != null && hero.sprite != null
				? (float)Math.atan2(e.y - hero.sprite.center().y, e.x - hero.sprite.center().x) : 0;
		new SlashTrail(8f, 2.5f, a - 1.6f, 2.2f, 0f, 0.08f, 0.2f)
				.trailColor(color)
				.show(enemy.sprite.parent, e);
	}

	//연쇄: 맞은 적에서 번진 적에게 빛 조각이 빠르게 날아가 꽂힌다 (공격하는 느낌)
	public static void chain(Char from, Char to){
		if (!seen(from) || !seen(to)) return;
		pathStrike(from.sprite.parent, from.sprite.center(), to.sprite.center());
	}

	//표식이 옮겨 붙는 길: 보라색이 섞인 입자가 차례로 지나간다
	public static void markTransfer(Char from, Char to){
		if (!seen(from) || !seen(to)) return;
		pathDust(from.sprite.parent, from.sprite.center(), to.sprite.center(), MARK_DUST_COLORS);
	}

	// ===== 특성으로 다른 적에게 번지는 길 =====

	//표식 전이 입자 색: 흰색·밝은 회색·회색·검정 + 보라 두 가지
	private static final int[] MARK_DUST_COLORS = {0xFFFFFF, 0xC8C8C8, 0x8C8C8C, 0x202020, 0xB070FF, 0xD8A8FF};
	//거리 1칸(16px)마다 입자 수, 최소·최대
	private static final float DUST_PER_TILE = 2.5f;
	private static final int   DUST_MIN = 5, DUST_MAX = 11;
	//입자 크기 (픽셀)
	private static final float DUST_SIZE = 2f;
	//처음 진하기 (0~1, 사라지며 옅어진다)
	private static final float DUST_ALPHA = 1f;
	//출발점에서 도착점까지 입자가 차례로 나타나는 시간 (방향이 보이게)
	private static final float DUST_TRAVEL = 0.14f;

	public static void pathDust(Group parent, final PointF from, final PointF to, final int[] colors){
		if (parent == null) return;
		float dist = (float)Math.hypot(to.x - from.x, to.y - from.y);
		final int count = Math.max(DUST_MIN, Math.min(DUST_MAX, Math.round(dist / DungeonTilemap.SIZE * DUST_PER_TILE)));
		parent.add(new Group(){
			float t = 0;
			int spawned = 0;
			@Override
			public void update() {
				super.update();
				t += Game.elapsed;
				int want = Math.min(count, (int)Math.ceil(t / DUST_TRAVEL * count));
				for (; spawned < want; spawned++){
					float p = (spawned + 0.5f) / count;
					float x = from.x + (to.x - from.x) * p + com.watabou.utils.Random.Float(-1.5f, 1.5f);
					float y = from.y + (to.y - from.y) * p + com.watabou.utils.Random.Float(-1.5f, 1.5f);
					add(fadingDot(x, y, colors[com.watabou.utils.Random.Int(colors.length)], DUST_SIZE, DUST_ALPHA,
							com.watabou.utils.Random.Float(0.3f, 0.45f),
							com.watabou.utils.Random.Float(-4, 4), com.watabou.utils.Random.Float(-6, -2))); //살짝 떠오르며 사라진다
				}
				if (spawned >= count && t > DUST_TRAVEL + 0.6f) killAndErase();
			}
		});
	}

	//연쇄 입자 색: 흰색·하늘색 두 가지·밝은 회색, 가끔 회색·검정
	private static final int[] STRIKE_COLORS = {0xFFFFFF, 0xFFFFFF, 0x7FD4FF, 0xB8E8FF, 0x7FD4FF, 0xC8C8C8, 0x8C8C8C, 0x202020};
	//빛 조각이 날아가는 시간 (짧을수록 날카롭다)
	private static final float STRIKE_TIME = 0.10f;
	//날아가며 남기는 꼬리 입자 간격 (px)
	private static final float STRIKE_TRAIL_STEP = 2.5f;
	//꽂힐 때 튀는 입자 수
	private static final int   STRIKE_BURST = 7;

	//연쇄: 앞머리(밝은 2px)가 빠르게 날아가며 꼬리 입자를 남기고, 닿는 순간 입자가 사방으로 튄다
	public static void pathStrike(Group parent, final PointF from, final PointF to){
		if (parent == null) return;
		final float dx = to.x - from.x, dy = to.y - from.y;
		final float dist = (float)Math.max(1, Math.hypot(dx, dy));
		parent.add(new Group(){
			float t = 0;
			float trailed = 0; //꼬리를 남긴 거리
			boolean hit = false;
			@Override
			public void update() {
				super.update();
				t += Game.elapsed;
				float p = Math.min(1f, t / STRIKE_TIME);
				float travelled = dist * p;
				//꼬리: 지나온 자리에 일정 간격으로
				for (; trailed <= travelled; trailed += STRIKE_TRAIL_STEP){
					float q = trailed / dist;
					float x = from.x + dx * q + com.watabou.utils.Random.Float(-0.6f, 0.6f);
					float y = from.y + dy * q + com.watabou.utils.Random.Float(-0.6f, 0.6f);
					add(fadingDot(x, y, STRIKE_COLORS[com.watabou.utils.Random.Int(STRIKE_COLORS.length)], 1.5f, 0.9f,
							com.watabou.utils.Random.Float(0.15f, 0.25f),
							-dx / dist * 12f, -dy / dist * 12f)); //날아가는 반대쪽으로 살짝 흘러간다
				}
				//앞머리: 밝은 흰색·하늘색 2px
				if (!hit){
					add(fadingDot(from.x + dx * p, from.y + dy * p, com.watabou.utils.Random.Int(2) == 0 ? 0xFFFFFF : 0xB8E8FF, 2.5f, 1f, 0.05f, 0, 0));
				}
				//꽂힘: 사방으로 튀는 입자
				if (p >= 1f && !hit){
					hit = true;
					for (int i = 0; i < STRIKE_BURST; i++){
						float a = com.watabou.utils.Random.Float(0, 6.283f);
						float sp = com.watabou.utils.Random.Float(25f, 45f);
						add(fadingDot(to.x, to.y, STRIKE_COLORS[com.watabou.utils.Random.Int(STRIKE_COLORS.length)], 1.5f, 1f,
								com.watabou.utils.Random.Float(0.18f, 0.3f), (float)Math.cos(a) * sp, (float)Math.sin(a) * sp));
					}
				}
				if (hit && t > STRIKE_TIME + 0.5f) killAndErase();
			}
		});
	}

	//사라지며 옅어지는 점 하나
	private static com.watabou.noosa.particles.PixelParticle fadingDot(float x, float y, int color, float size, final float alpha,
	                                                                  float life, float vx, float vy){
		com.watabou.noosa.particles.PixelParticle d = new com.watabou.noosa.particles.PixelParticle(){
			@Override
			public void update() {
				super.update();
				alpha(Math.max(0, left / lifespan) * alpha);
			}
		};
		d.reset(x, y, color, size, life);
		d.speed.set(vx, vy);
		return d;
	}

	//과충전: 시전할 때 마력이 넘쳐흐름
	public static void overcharge(Char hero){
		if (!seen(hero)) return;
		hero.sprite.centerEmitter().burst(MagicMissile.MagicParticle.FACTORY, 12);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.5f, 1.4f);
	}

	//충격: 벽·적 충돌
	public static void impact(int cell){
		if (Dungeon.level == null || cell < 0 || !Dungeon.level.heroFOV[cell]) return;
		CellEmitter.center(cell).burst(Speck.factory(Speck.ROCK), 4);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.5f, 1.4f);
	}

	// =====================================================================
	// 계속 켜져 있는 연출 (상태창이 매 프레임: BloomEffects.updateHeroFx)
	// =====================================================================

	private static CharSprite lastSprite;
	private static Emitter galeWind;   //질풍: 몸 주위 바람 줄기
	private static Emitter cycleGlow;  //원소 순환 각성: 무기에 원소 3색 빛

	public static void updateHero(Char hero, boolean gale, boolean cycle){
		if (hero == null || hero.sprite == null || hero.sprite.parent == null) return;
		if (hero.sprite != lastSprite){
			lastSprite = hero.sprite;
			galeWind = null;
			cycleGlow = null;
		}
		boolean alive = hero.isAlive();

		if (gale && alive && galeWind == null){
			galeWind = hero.sprite.emitter();
			galeWind.pour(WindParticle.FACTORY, 0.25f);
		} else if ((!gale || !alive) && galeWind != null){
			galeWind.on = false;
			galeWind = null;
		}

		if (cycle && alive && cycleGlow == null){
			cycleGlow = hero.sprite.centerEmitter();
			cycleGlow.pour(new Emitter.Factory() {
				int n = 0;
				@Override
				public void emit(Emitter emitter, int index, float x, float y) {
					switch (n++ % 3){
						case 0: FlameParticle.FACTORY.emit(emitter, index, x, y); break;
						case 1: SnowParticle.FACTORY.emit(emitter, index, x, y); break;
						default: SparkParticle.STATIC.emit(emitter, index, x, y); break;
					}
				}
			}, 0.18f);
		} else if ((!cycle || !alive) && cycleGlow != null){
			cycleGlow.on = false;
			cycleGlow = null;
		}
	}
}
