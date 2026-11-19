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
	private boolean connect = false;
	//connect(): 붙어 있는 칸 사이(가로·세로)에 같은 그림을 반 칸 어긋나게 하나 더 깔아 이어져 보이게 한다
	private final HashMap<Integer, MovieClip> bridges = new HashMap<>();
	//connect()일 때는 모든 칸이 같은 박자로 움직이도록 프레임을 직접 고른다 (따로 움직이면 이음매가 깜빡인다)
	private com.watabou.noosa.TextureFilm film;
	private int syncFrame = 0;

	private void sync( MovieClip c ){
		if (!connect) return;
		if (film == null) film = new com.watabou.noosa.TextureFilm(c.texture, fx.w, fx.h);
		c.paused = true;
		c.frame(film.get(syncFrame));
	}
	private float time = 0;
	private final HashMap<Integer, MovieClip> clips = new HashMap<>();
	//칸마다 [밝기, 진하기, 일렁임 위상]
	private final HashMap<Integer, float[]> shades = new HashMap<>();

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

	public TileFxLayer connect(){
		connect = true;
		return this;
	}

	//key = 칸 * 2 + (0 오른쪽, 1 아래)
	private void bridge( int cell, int dir, boolean has, int w ){
		int key = cell * 2 + dir;
		MovieClip b = bridges.get(key);
		if (has && b == null){
			b = BossEffects.clip(fx, fps, true);
			float half = DungeonTilemap.SIZE / 2f;
			b.x = (cell % w) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - fx.w) / 2f + (dir == 0 ? half : 0);
			b.y = (cell / w) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - fx.h) / 2f + (dir == 1 ? half : 0);
			b.alpha(alpha);
			addToBack(b);
			bridges.put(key, b);
		} else if (!has && b != null){
			b.killAndErase();
			bridges.remove(key);
			b = null;
		}
		if (b != null) b.visible = Dungeon.level.heroFOV[cell] || Dungeon.level.heroFOV[cell + (dir == 0 ? 1 : w)];
		if (b != null) sync(b);
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
				c = BossEffects.clip(fx, fps, true);
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
			} else if (!has && c != null){
				c.killAndErase();
				clips.remove(i);
				shades.remove(i);
				c = null;
			}
			if (connect){
				boolean right = has && (i % w) < w - 1 && blob.cur[i + 1] > 0;
				boolean down  = has && i + w < blob.cur.length && blob.cur[i + w] > 0;
				bridge(i, 0, right, w);
				bridge(i, 1, down, w);
			}
			if (c != null){
				c.visible = Dungeon.level.heroFOV[i];
				sync(c);
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
