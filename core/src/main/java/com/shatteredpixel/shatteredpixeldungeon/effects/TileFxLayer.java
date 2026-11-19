/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.MovieClip;

import java.util.HashMap;

//Maze Tower: 블롭(수액 웅덩이·마력 증기·냉기 결정)이 있는 칸마다 반복 그림을 깔고, 사라지면 지운다.
//Blob.use(emitter)에서 emitter.add(new TileFxLayer(this, fx, fps, alpha))로 붙인다.
public class TileFxLayer extends Group {

	private final Blob blob;
	private final BossEffects.Fx fx;
	private final int fps;
	private final float alpha;
	private final HashMap<Integer, MovieClip> clips = new HashMap<>();

	public TileFxLayer( Blob blob, BossEffects.Fx fx, int fps, float alpha ){
		this.blob = blob;
		this.fx = fx;
		this.fps = fps;
		this.alpha = alpha;
	}

	@Override
	public synchronized void update() {
		super.update();
		if (blob.cur == null || Dungeon.level == null) return;
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
				add(c);
				clips.put(i, c);
			} else if (!has && c != null){
				c.killAndErase();
				clips.remove(i);
			}
			if (c != null) c.visible = Dungeon.level.heroFOV[i];
		}
	}
}
