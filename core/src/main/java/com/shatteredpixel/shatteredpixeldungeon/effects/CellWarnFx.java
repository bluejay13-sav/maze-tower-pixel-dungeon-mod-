/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Halo;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 칸 경고 연출 (여제 전기비). 그림 한 장을 칸마다 깜빡이는 대신 명암을 섞어 코드로 그린다.
//(결정, 바뀜) 노란 원소 타일(네모)과 헷갈리지 않게 둥근 모양: 칸마다 어두운 바깥 고리 → 원소 색 → 밝은 가운데가 겹친
//둥근 빛 고임이 칸마다 다른 박자로 숨 쉬고, 불꽃이 자주 튀며, 가끔 작은 번개가 칸 안에서 번쩍인다.
public class CellWarnFx {

	private final Group floor = new Group(), sky = new Group();
	private final ArrayList<Integer> cells;
	private final int deepColor, baseColor, coreColor, sparkColor;

	public CellWarnFx( ArrayList<Integer> cells, int deepColor, int baseColor, int coreColor, int sparkColor ){
		this.cells = cells;
		this.deepColor = deepColor;
		this.baseColor = baseColor;
		this.coreColor = coreColor;
		this.sparkColor = sparkColor;
		for (int c : cells){
			floor.add(new Glow(DungeonTilemap.tileCenterToWorld(c), Random.Float(0f, 6.28f)));
		}
		sky.add(new Sparks());
	}

	//층의 캐릭터 무리 그룹에 붙인다: 빛 고임은 맨 뒤(바닥 쪽), 불꽃·번개는 맨 앞
	public void attach( Group mobs ){
		mobs.addToBack(floor);
		mobs.add(sky);
	}

	public void kill(){
		floor.killAndErase();
		sky.killAndErase();
	}

	private class Glow extends Group {
		private final Halo deep, base, core;
		private final float phase;
		private float time = 0;

		Glow( PointF c, float phase ){
			this.phase = phase;
			deep = halo(c, 9f, deepColor);
			base = halo(c, 6.5f, baseColor);
			core = halo(c, 3.5f, coreColor);
			update();
		}

		private Halo halo( PointF c, float r, int color ){
			Halo h = new Halo(r, color, 1f);
			h.point(c.x, c.y);
			add(h);
			return h;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float wave = 0.6f + 0.4f * (float)Math.sin(time * 8f + phase);
			deep.alpha(0.55f);
			base.alpha(0.55f * wave);
			core.alpha(0.75f * wave);
		}
	}

	private class Sparks extends Group {
		private float acc = 0, arcTimer = 0.1f;

		@Override
		public void update() {
			super.update();
			if (cells.isEmpty()) return;
			int w = Dungeon.level.width();
			//가끔 칸 안에서 작은 번개가 번쩍
			arcTimer -= Game.elapsed;
			if (arcTimer <= 0){
				arcTimer = Random.Float(0.08f, 0.2f);
				int c = cells.get(Random.Int(cells.size()));
				if (Dungeon.level.heroFOV[c]){
					PointF m = DungeonTilemap.tileCenterToWorld(c);
					PointF a = new PointF(m.x + Random.Float(-6, 6), m.y + Random.Float(-6, 6));
					PointF b = new PointF(m.x + Random.Float(-6, 6), m.y + Random.Float(-6, 6));
					add(new Lightning(a, b, null));
				}
			}
			//튀는 불꽃
			acc += Game.elapsed * cells.size() * 6f;
			while (acc >= 1f){
				acc -= 1f;
				int c = cells.get(Random.Int(cells.size()));
				if (!Dungeon.level.heroFOV[c]) continue;
				StormZoneFx.Fade p = (StormZoneFx.Fade) recycle(StormZoneFx.Fade.class);
				PointF m = DungeonTilemap.tileCenterToWorld(c);
				p.reset(m.x + Random.Float(-4, 4), m.y + Random.Float(-3, 4), Random.Int(3) == 0 ? 0xFFFFFF : sparkColor, 1f, Random.Float(0.15f, 0.35f));
				p.speed.polar(Random.Float(-3.14f, 0f), Random.Float(18, 40));
				p.acc.set(0, 50);
			}
		}
	}
}
