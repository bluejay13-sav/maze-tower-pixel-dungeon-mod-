/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.Random;

import java.util.HashMap;

//Maze Tower: 블롭(수액 웅덩이·마력 증기·냉기 결정)이 있는 칸마다 반복 그림을 깔고, 사라지면 지운다.
//Blob.use(emitter)에서 emitter.add(new TileFxLayer(this, fx, fps, alpha))로 붙인다.
//shaded(): 칸마다 밝기·진하기를 다르게 하고 천천히 일렁이게 해서 명암을 준다 (마력 증기).
public class TileFxLayer extends Group {

	private final Blob blob;
	private final BossEffects.Fx fx;
	private final int fps;
	private final float alpha;
	private boolean shaded = false;
	//connect(connFx): 붙어 있는 수액이 경계 없이 하나로 이어져 보이게 한다.
	//connFx는 이웃 방향(위 1, 오른쪽 2, 아래 4, 왼쪽 8)마다 테두리를 빼고 칸 끝까지 이어지는 모양 + 대각선(오른쪽 위 16, 오른쪽 아래 32, 왼쪽 아래 64, 왼쪽 위 128) 모서리 채움, 256줄 × 프레임 그림이다.
	//칸마다 이웃을 보고 맞는 줄을 고르고, 모든 칸이 같은 박자로 움직이도록 프레임을 직접 고른다.
	private BossEffects.Fx connFx = null;
	private com.watabou.noosa.TextureFilm film;
	private int syncFrame = 0;

	private void sync( MovieClip c, int mask ){ sync( c, mask, syncFrame ); }
	private void sync( MovieClip c, int mask, int frame ){
		if (connFx == null) return;
		if (film == null) film = new com.watabou.noosa.TextureFilm(c.texture, connFx.w, connFx.h);
		c.paused = true;
		c.frame(film.get(mask * connFx.frames + frame));
	}
	private float time = 0;
	private final HashMap<Integer, MovieClip> clips = new HashMap<>();
	//칸마다 [밝기, 진하기, 일렁임 위상]
	private final HashMap<Integer, float[]> shades = new HashMap<>();

	//Maze Tower (사용자): 살펴보기 창(WndInfoCell)의 그림 — 지도에 보이는 그 칸 그대로 (바닥 타일 위에 이웃과 이어지는 모양, varied면 칸마다 정해진 모양 가지)
	public static com.watabou.noosa.Image infoImage( Blob blob, BossEffects.Fx connFx, boolean varied, int i, com.watabou.noosa.Image base ){
		try {
			int w = Dungeon.level.width();
			int mask = 0;
			if (i - w >= 0 && blob.cur[i - w] > 0)                         mask |= 1;
			if ((i % w) < w - 1 && blob.cur[i + 1] > 0)                  mask |= 2;
			if (i + w < blob.cur.length && blob.cur[i + w] > 0)           mask |= 4;
			if ((i % w) > 0 && blob.cur[i - 1] > 0)                      mask |= 8;
			boolean l = (i % w) > 0, r = (i % w) < w - 1;
			if (r && i - w + 1 >= 0 && blob.cur[i - w + 1] > 0)                mask |= 16;
			if (r && i + w + 1 < blob.cur.length && blob.cur[i + w + 1] > 0)   mask |= 32;
			if (l && i + w - 1 < blob.cur.length && blob.cur[i + w - 1] > 0)   mask |= 64;
			if (l && i - w - 1 >= 0 && blob.cur[i - w - 1] > 0)                mask |= 128;
			int frame = varied ? (int)(((((long)i * 2654435761L) & 0xFFFFFFFFL) >>> 16) % connFx.frames) : 0;
			com.watabou.gltextures.SmartTexture bt = base.texture;
			com.watabou.utils.RectF fr = base.frame();
			int S = DungeonTilemap.SIZE;
			com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(S, S, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
			pm.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
			pm.drawPixmap(bt.bitmap, 0, 0, Math.round(fr.left * bt.width), Math.round(fr.top * bt.height), S, S);
			com.watabou.gltextures.SmartTexture st = com.watabou.gltextures.TextureCache.get(connFx.file);
			int idx = mask * connFx.frames + frame, per = st.width / connFx.w;
			pm.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.SourceOver);
			pm.drawPixmap(st.bitmap, 0, 0, (idx % per) * connFx.w, (idx / per) * connFx.h, connFx.w, connFx.h);
			return new com.watabou.noosa.Image(new com.watabou.gltextures.SmartTexture(pm));
		} catch (Exception e){
			return base;
		}
	}

	public TileFxLayer( Blob blob, BossEffects.Fx fx, int fps, float alpha ){
		this.blob = blob;
		this.fx = fx;
		this.fps = fps;
		this.alpha = alpha;
	}

	public TileFxLayer shaded(){
		shaded = true;
		return this;
	}

	public TileFxLayer connect( BossEffects.Fx connFx ){
		this.connFx = connFx;
		return this;
	}

	//varied(): connFx의 프레임을 움직임 대신 "모양 가지"로 쓴다. 칸마다 정해진 하나를 고른다(같은 칸은 늘 같은 모양, 검은 낙엽·새잎 땅 - 무늬가 되풀이되어 보이지 않게)
	private boolean varied = false;
	public TileFxLayer varied(){
		varied = true;
		return this;
	}
	private int variantOf( int cell ){
		return (int)(((((long)cell * 2654435761L) & 0xFFFFFFFFL) >>> 16) % connFx.frames);
	}

	//(사용자, 25층 새잎 땅) solo(): 이웃과 상관없이 늘 사방이 이어진 모양(번호 255)을 쓴다 → 옆에 새 칸이 생겨도 이미 있던 칸의 새싹·빛은 그대로.
	//fadeIn(초): 새로 생긴 칸이 그만큼에 걸쳐 서서히 나타난다 (레이어를 만들 때 이미 있던 칸은 바로)
	private boolean solo = false;
	public TileFxLayer solo(){ solo = true; return this; }
	private float fadeIn = 0;
	private final java.util.HashMap<Integer, Float> born = new java.util.HashMap<>();
	public TileFxLayer fadeIn( float seconds ){ fadeIn = seconds; return this; }

	//valueFade(full): 칸의 남은 값이 full보다 적으면 그만큼 옅게 그린다 (핏줄 눈의 달궈진 줄이 식어 가는 모습)
	private int fadeFull = 0;
	public TileFxLayer valueFade( int full ){
		fadeFull = full;
		return this;
	}

	@Override
	public synchronized void update() {
		super.update();
		if (blob.cur == null || Dungeon.level == null) return;
		time += Game.elapsed;
		syncFrame = (int)(time * fps) % fx.frames;
		int w = Dungeon.level.width();
		for (int i = 0; i < blob.cur.length; i++){
			boolean has = blob.cur[i] > 0;
			MovieClip c = clips.get(i);
			if (has && c == null){
				c = BossEffects.clip(connFx != null ? connFx : fx, fps, true);
				//칸보다 큰 그림은 칸 가운데에 맞춘다
				c.x = (i % w) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - fx.w) / 2f;
				c.y = (i / w) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - fx.h) / 2f;
				c.alpha(alpha);
				if (shaded){
					float[] s = new float[]{ Random.Float(0.55f, 1.25f), Random.Float(0.6f, 1f), Random.Float(0f, 6.28f) };
					shades.put(i, s);
					c.brightness(s[0]);
					//색도 칸마다 조금씩 (보라 쪽 / 하늘 쪽)
					c.rm *= Random.Float(0.8f, 1.15f);
					c.gm *= Random.Float(0.9f, 1.1f);
					c.bm *= Random.Float(0.85f, 1.15f);
				}
				add(c);
				clips.put(i, c);
				if (fadeIn > 0 && time > 0.05f){ born.put(i, time); c.alpha(0); }
			} else if (!has && c != null){
				c.killAndErase();
				clips.remove(i);
				shades.remove(i);
				c = null;
			}
			if (c != null){
				c.visible = Dungeon.level.heroFOV[i];
				if (fadeFull > 0) c.alpha( alpha * Math.min( 1f, blob.cur[i] / (float) fadeFull ) );
				if (connFx != null){
					int mask = 0;
					if (i - w >= 0 && blob.cur[i - w] > 0)                         mask |= 1; //위
					if ((i % w) < w - 1 && blob.cur[i + 1] > 0)                  mask |= 2; //오른쪽
					if (i + w < blob.cur.length && blob.cur[i + w] > 0)           mask |= 4; //아래
					if ((i % w) > 0 && blob.cur[i - 1] > 0)                      mask |= 8; //왼쪽
					//대각선 (양옆이 모두 이어질 때만 모서리를 채운다)
					boolean l = (i % w) > 0, r = (i % w) < w - 1;
					if (r && i - w + 1 >= 0 && blob.cur[i - w + 1] > 0)                mask |= 16;  //오른쪽 위
					if (r && i + w + 1 < blob.cur.length && blob.cur[i + w + 1] > 0)   mask |= 32;  //오른쪽 아래
					if (l && i + w - 1 < blob.cur.length && blob.cur[i + w - 1] > 0)   mask |= 64;  //왼쪽 아래
					if (l && i - w - 1 >= 0 && blob.cur[i - w - 1] > 0)                mask |= 128; //왼쪽 위
					if (solo) mask = 255;
					if (varied) sync(c, mask, variantOf(i)); else sync(c, mask);
				}
				Float b0 = born.get(i);
				if (b0 != null){
					float k = (time - b0) / fadeIn;
					if (k >= 1f){ born.remove(i); c.alpha(alpha); } else c.alpha(alpha * Math.max(0f, k));
				}
				if (shaded){
					float[] s = shades.get(i);
					//남은 양이 적을수록 옅어지고, 칸마다 다른 박자로 천천히 일렁인다
					float density = Math.min(1f, 0.35f + blob.cur[i] / 10f);
					float wave = 0.8f + 0.2f * (float)Math.sin(time * 1.6f + s[2]);
					c.alpha(alpha * s[1] * density * wave);
				}
			}
		}
	}
}
