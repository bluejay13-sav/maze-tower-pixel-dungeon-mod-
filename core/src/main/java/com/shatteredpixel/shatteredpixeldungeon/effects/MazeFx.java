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

	//(2026-11-21 밤, 피드백 반영) 그림마다 한 프레임 시간을 README대로. 프레임 수는 그림 너비로 센다.
	//삭제: 조준 사격 적중(fx3_07)·메치기(fx3_22)·맨손 타격(fx_hunter_07)·면역 보호막(14)·던지기 부딪힘(15)·흐르는 충전 불꽃(16)
	public enum Fx {
		SLASH      ("fx3_00", 32, 32, 4, 0.06f),
		THRUST     ("fx3_01", 32, 32, 5, 0.05f),
		FLURRY     ("fx3_02", 32, 32, 6), //연격 3타: 타격마다 두 프레임씩
		CRESCENT   ("fx3_03", 32, 32, 5), //반월베기 2타
		PHANTOM    ("fx3_04", 32, 32, 4, 0.05f),
		//(W1·W2·W4) 5티어 무기 스킬 (사용자 확인, 원본 docs/sprites/fx_redo/make_fx_tier5.ps1)
		SMASH      ("fx5t_smash", 32, 32, 5, 0.06f),     //내려찍기: 돌리지 않고 대상 위에 (위에서 아래로 내리그음)
		DISCHARGE  ("fx5t_discharge", 32, 32, 5, 0.06f), //방출: 대상 몸 가운데보다 3칸 아래에서 터지게 그려 둠
		THROAT     ("fx5t_throat", 32, 32, 4, 0.06f),    //숨통 물기: 오른쪽을 향해 그림, 화살 방향으로 돌림
		VITAL      ("fx3_05", 32, 32, 4, 0.05f),
		VITAL_SURPRISE ("fx3_06", 32, 32, 4, 0.05f),
		STEAM      ("fx3_08", 32, 32, 6),
		HISS       ("fx3_09", 32, 32, 6),
		EXPLOSION  ("fx3_10", 32, 32, 6, 0.06f),
		MINE_BURST ("fx_mine_burst", 32, 32, 6, 0.06f), //Maze Tower (J4): 광폭한 광부의 마석 터짐 (폭발 fx3_10을 마석 푸른빛으로, 원본 docs/sprites/mobs/j_draft/make_mineburst.ps1)
		FREEZE     ("fx3_11", 32, 32, 4, FAST),
		STATIC     ("fx3_13", 32, 32, 4), //반복
		WET        ("fx3_14", 32, 32, 4), //반복 (그림에 반투명이 들어 있어 코드에서 흐리게 하지 않는다)
		LEAP       ("fx3_16", 32, 32, 6),
		SLAM_WAVE  ("fx3_17", 32, 32, 4),
		ENDURE     ("fx3_18", 32, 32, 4), //반복
		RETRIBUTION("fx3_19", 32, 32, 4),
		THORNS     ("fx3_20", 32, 32, 4, 0.05f),
		THAW       ("fx4_00", 32, 32, 6, FAST),
		MANA_ORB   ("fx4_06", 32, 32, 6), //마나 반환
		CHILL      ("fx4_15", 32, 32, 4), //냉기 눈송이 (반복)
		//사냥꾼 (docs/sprites/hunter_fx, 2026-11-21)
		H_LOAD     ("fx_hunter_03", 16, 16, 6), //석궁 장전
		H_FOCUS    ("fx_hunter_04", 16, 16, 7), //정량궁 집중
		H_WALL     ("fx_hunter_05", 16, 16, 5), //정령 화살 벽 통과 잔광
		H_CRIT     ("fx_hunter_08", 16, 16, 3), //급소 번쩍임
		H_HAWK     ("fx_hunter_31", 16, 16, 4), //정령 매 날기 (반복)
		H_HAWK_IN  ("fx_hunter_32", 16, 16, 6), //정령 매 소환
		H_HAWK_OUT ("fx_hunter_33", 16, 16, 6), //정령 매 사라짐
		H_HAWK_GLOW("fx_hunter_34", 32, 32, 1); //정령 매 주변 빛 (몸 뒤)

		public final String file;
		public final int w, h, frames;
		public final float delay; //한 프레임 시간

		Fx( String name, int w, int h, int frames ){
			this(name, w, h, frames, DELAY);
		}

		Fx( String name, int w, int h, int frames, float delay ){
			this.file = "sprites/maze_" + name + ".png";
			this.w = w;
			this.h = h;
			this.frames = frames;
			this.delay = delay;
		}

		//그림을 고쳐 프레임 수가 바뀌어도 맞게: 그림 너비로 센다
		public int count( com.watabou.gltextures.SmartTexture tex ){
			int n = tex.width / w;
			return n > 0 ? n : frames;
		}
	}

	//frames가 비어 있으면 모든 프레임
	public static MovieClip clip( Fx fx, float delay, boolean loop, int... frames ){
		final MovieClip c = new MovieClip(fx.file);
		TextureFilm film = new TextureFilm(c.texture, fx.w, fx.h);
		Object[] idx;
		if (frames.length == 0){
			int n = fx.count(c.texture);
			idx = new Object[n];
			for (int i = 0; i < n; i++) idx[i] = i;
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
		return at(fx, cx, cy, 0, fx.delay);
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
		MovieClip c = at(fx, b.x, b.y, ang, fx.delay, frames);
		mirrorIfLeft( c, ang );
		return c;
	}

	//Maze Tower (사용자): 왼쪽을 향해 쓰는 스킬은 그림을 거꾸로 뒤집지 않고 좌우로만 뒤집는다.
	//오른쪽을 향해 그린 그림을 180° 가까이 돌리면 위아래까지 뒤집혀(베기 궤적이 아래에서 위로 등) 어색하므로, 돌린 뒤 위아래를 한 번 더 뒤집는다.
	public static void mirrorIfLeft( MovieClip c, float ang ){
		if (c != null && (ang > 90f || ang < -90f)) c.scale.y = -Math.abs(c.scale.y);
	}

	//Maze Tower (사용자): 돌리지 않는 그림(내려찍기 등)은 공격자가 대상보다 오른쪽이면 좌우로 뒤집는다
	public static void faceFrom( MovieClip c, Char from, Char to ){
		if (c == null || from == null || to == null || from.sprite == null || to.sprite == null) return;
		if (from.sprite.center().x > to.sprite.center().x + 0.5f) c.scale.x = -Math.abs(c.scale.x);
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

	//(결정, 2026-11-21 밤) 반응 이름 글자 그림(fx4_10)은 쓰지 않는다. 예전처럼 코드 글자(ElementalReactions.announce).

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
			int n = fx.count(texture);
			Object[] idx = new Object[n];
			for (int i = 0; i < n; i++) idx[i] = i;
			Animation a = new Animation(Math.round(1f / DELAY), true).frames(film, idx);
			a.delay = DELAY * 1.6f; //상태 동안 반복하는 그림은 조금 느리게
			play(a);
			follow();
		}

		//움직이지 않는 한 프레임만 (열기 중첩 수 등)
		public Loop still( int frame ){
			TextureFilm film = new TextureFilm(texture, fx.w, fx.h);
			play(new Animation(1, false).frames(film, Math.max(0, Math.min(fx.frames - 1, frame))));
			return this;
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
