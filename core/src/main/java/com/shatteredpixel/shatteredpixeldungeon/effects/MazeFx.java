/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//Maze Tower: 다시 그린 우리 이펙트 그림 시트 (2026-11-21, 원본 docs/sprites/fx_redo/, README에 이름·크기·재생 방식).
//게임 사본은 sprites/maze_fx3_*.png, maze_fx4_*.png. 같은 이름으로 덮어쓰면 바뀐다.
//방향이 있는 그림(궤적·찌르기·바람 등)은 오른쪽을 향해 그렸고, 공격 방향으로 돌려 그린다.
//재생: 따로 적지 않은 것은 한 번, 한 프레임 0.075초.
public class MazeFx {

	public static final float DELAY = 0.075f;
	public static final float FAST  = 0.06f; //빙결·해빙

	public enum Fx {
		SLASH      ("fx3_00", 32, 32, 4),
		THRUST     ("fx3_01", 32, 32, 5),
		FLURRY     ("fx3_02", 32, 32, 6), //연격 3타: 타격마다 두 프레임씩
		CRESCENT   ("fx3_03", 32, 32, 5), //반월베기 2타
		PHANTOM    ("fx3_04", 32, 32, 4),
		VITAL      ("fx3_05", 32, 32, 4),
		VITAL_SURPRISE ("fx3_06", 32, 32, 4),
		AIMED_HIT  ("fx3_07", 32, 32, 3),
		STEAM      ("fx3_08", 32, 32, 6),
		HISS       ("fx3_09", 32, 32, 6),
		EXPLOSION  ("fx3_10", 32, 32, 6),
		FREEZE     ("fx3_11", 32, 32, 4),
		STATIC     ("fx3_13", 32, 32, 4), //반복
		WET        ("fx3_14", 32, 32, 4), //반복
		LEAP       ("fx3_16", 32, 32, 6),
		SLAM_WAVE  ("fx3_17", 32, 32, 4),
		ENDURE     ("fx3_18", 32, 32, 4), //반복
		RETRIBUTION("fx3_19", 32, 32, 4),
		THORNS     ("fx3_20", 32, 32, 4),
		CLOBBER    ("fx3_21", 32, 32, 4), //밀쳐내기
		SLAM       ("fx3_22", 32, 32, 5), //메치기
		PARRY      ("fx3_23", 32, 32, 4), //받아치기
		CRUSH      ("fx3_24", 80, 80, 5), //으깨기 5×5
		FURY       ("fx3_25", 32, 32, 5), //분노 폭발
		THAW       ("fx4_00", 32, 32, 6),
		CLEAVE_RING("fx4_01", 32, 32, 4), //연속 베기 원 베기
		HEAVY      ("fx4_02", 32, 32, 3), //강타
		GALE       ("fx4_03", 32, 32, 4), //질풍 바람 줄기
		CHAIN      ("fx4_04", 32, 32, 4), //연쇄
		EXECUTE    ("fx4_05", 32, 32, 3), //처형
		MANA_ORB   ("fx4_06", 32, 32, 6), //마나 반환
		READY_GLOW ("fx4_07", 32, 32, 4), //집중·흐름 준비 (반복)
		SHIELD     ("fx4_08", 32, 32, 4), //마나 장막·흘리기 (반복)
		ECHO       ("fx4_09", 32, 32, 3), //잔향 각성 잔상
		OVERFLOW   ("fx4_14", 32, 32, 4), //넘치는 힘 (반복, 숨 쉬듯)
		CHILL      ("fx4_15", 32, 32, 4); //냉기 눈송이 (반복)

		public final String file;
		public final int w, h, frames;

		Fx( String name, int w, int h, int frames ){
			this.file = "sprites/maze_" + name + ".png";
			this.w = w;
			this.h = h;
			this.frames = frames;
		}
	}

	//frames가 비어 있으면 모든 프레임
	public static MovieClip clip( Fx fx, float delay, boolean loop, int... frames ){
		final MovieClip c = new MovieClip(fx.file);
		TextureFilm film = new TextureFilm(c.texture, fx.w, fx.h);
		Object[] idx;
		if (frames.length == 0){
			idx = new Object[fx.frames];
			for (int i = 0; i < fx.frames; i++) idx[i] = i;
		} else {
			idx = new Object[frames.length];
			for (int i = 0; i < frames.length; i++) idx[i] = frames[i];
		}
		if (!loop){
			c.listener = new MovieClip.Listener() {
				@Override
				public void onComplete(MovieClip.Animation a) {
					c.killAndErase();
				}
			};
		}
		MovieClip.Animation a = new MovieClip.Animation(Math.round(1f / delay), loop).frames(film, idx);
		a.delay = delay;
		c.play(a);
		c.origin.set(fx.w / 2f, fx.h / 2f);
		return c;
	}

	//(cx, cy)를 그림 가운데로, angle만큼 돌려서 (오른쪽을 향해 그린 그림)
	public static MovieClip at( Fx fx, float cx, float cy, float angle, float delay, int... frames ){
		MovieClip c = clip(fx, delay, false, frames);
		c.x = cx - fx.w / 2f;
		c.y = cy - fx.h / 2f;
		c.angle = angle;
		GameScene.effect(c);
		return c;
	}

	public static MovieClip at( Fx fx, float cx, float cy ){
		return at(fx, cx, cy, 0, DELAY);
	}

	public static MovieClip onChar( Fx fx, CharSprite s ){
		if (s == null || s.parent == null) return null;
		PointF c = s.center();
		return at(fx, c.x, c.y);
	}

	public static MovieClip onChar( Fx fx, CharSprite s, float delay ){
		if (s == null || s.parent == null) return null;
		PointF c = s.center();
		return at(fx, c.x, c.y, 0, delay);
	}

	public static MovieClip atCell( Fx fx, int cell ){
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		return at(fx, p.x, p.y);
	}

	//공격자 → 대상 방향으로 돌려 대상 위에
	public static MovieClip toward( Fx fx, Char from, Char to, int... frames ){
		if (from.sprite == null || to.sprite == null || to.sprite.parent == null) return null;
		PointF a = from.sprite.center(), b = to.sprite.center();
		float ang = (float)(Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI);
		return at(fx, b.x, b.y, ang, DELAY, frames);
	}

	public static float angle( PointF a, PointF b ){
		return (float)(Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI);
	}

	//a에서 b로 날아가며 재생 (마나 반환 구슬 등). 닿으면 onLand
	public static MovieClip fly( Fx fx, PointF a, PointF b, float time, final Callback onLand ){
		final MovieClip c = clip(fx, Math.max(0.02f, time / fx.frames), false);
		c.listener = null;
		c.x = a.x - fx.w / 2f;
		c.y = a.y - fx.h / 2f;
		c.angle = angle(a, b);
		GameScene.effect(c);
		c.parent.add(new PosTweener(c, new PointF(b.x - fx.w / 2f, b.y - fx.h / 2f), time){
			@Override
			protected void onComplete() {
				c.killAndErase();
				if (onLand != null) onLand.call();
			}
		});
		return c;
	}

	//반응 이름 글자 그림 (fx4_10, 48×16: 0 증기 1 폭발 2 해빙 3 빙결). 한국어일 때만 그림을 띄우고 true
	public static boolean reactionLabel( CharSprite s, String key ){
		if (com.shatteredpixel.shatteredpixeldungeon.messages.Messages.lang()
				!= com.shatteredpixel.shatteredpixeldungeon.messages.Languages.KOREAN) return false;
		int idx;
		switch (key){
			case "steam":     idx = 0; break;
			case "explosion": idx = 1; break;
			case "thaw":      idx = 2; break;
			case "freeze":    idx = 3; break;
			default: return false;
		}
		if (s == null || s.parent == null) return false;
		s.parent.add(new FloatLabel(s, idx));
		return true;
	}

	//원작 FloatingText처럼 머리 위에서 떠오르며 사라진다
	private static class FloatLabel extends com.watabou.noosa.Image {
		private static final float LIFE = 1.0f, RISE = 14f;
		private float left = LIFE;
		private final float baseY;

		FloatLabel( CharSprite s, int idx ){
			super("sprites/maze_fx4_10.png");
			frame(new TextureFilm(texture, 48, 16).get(idx));
			PointF c = s.center();
			x = c.x - 24;
			baseY = s.y - 14;
			y = baseY;
		}

		@Override
		public void update() {
			super.update();
			left -= com.watabou.noosa.Game.elapsed;
			if (left <= 0){
				killAndErase();
				return;
			}
			float p = 1f - left / LIFE;
			y = baseY - RISE * p;
			alpha(p < 0.6f ? 1f : (1f - p) / 0.4f);
		}
	}

	//캐릭터 몸을 따라다니며 반복하는 그림 (상태이상·버프 동안). stop()으로 끝낸다.
	//대상이 죽거나 그림이 사라지면 스스로 멈추고, 대상이 안 보이면 숨는다.
	public static class Loop extends MovieClip {

		private final CharSprite target;
		private final Fx fx;
		private float alphaMax = 1f;
		private boolean breathe = false;
		private float t = 0;

		public Loop( Fx fx, CharSprite target ){
			super(fx.file);
			this.fx = fx;
			this.target = target;
			TextureFilm film = new TextureFilm(texture, fx.w, fx.h);
			Object[] idx = new Object[fx.frames];
			for (int i = 0; i < fx.frames; i++) idx[i] = i;
			Animation a = new Animation(Math.round(1f / DELAY), true).frames(film, idx);
			a.delay = DELAY * 1.6f; //상태 동안 반복하는 그림은 조금 느리게
			play(a);
			follow();
		}

		//숨 쉬듯 진하기가 오르내린다 (넘치는 힘)
		public Loop breathe( float alphaMax ){
			this.breathe = true;
			this.alphaMax = alphaMax;
			return this;
		}

		public Loop alphaMax( float a ){
			this.alphaMax = a;
			alpha(a);
			return this;
		}

		public static Loop on( Fx fx, CharSprite target ){
			if (target == null || target.parent == null) return null;
			Loop l = new Loop(fx, target);
			target.parent.add(l);
			return l;
		}

		private void follow(){
			PointF c = target.center();
			x = c.x - fx.w / 2f;
			y = c.y - fx.h / 2f;
		}

		public void stop(){
			killAndErase();
		}

		@Override
		public void update() {
			super.update();
			if (target.parent == null || (target.ch != null && !target.ch.isAlive())){
				killAndErase();
				return;
			}
			follow();
			visible = target.visible;
			if (breathe){
				t += com.watabou.noosa.Game.elapsed;
				alpha(alphaMax * (0.55f + 0.45f * (float)Math.sin(t * 2.4f)));
			}
		}
	}
}
