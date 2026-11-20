/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 칸 경고 연출 (여제 전기비 등). 그림 한 장을 칸마다 깜빡이는 대신 명암을 섞어 코드로 그린다:
//칸마다 어두운 바탕 → 원소 색 → 밝은 가운데가 겹친 빛 고임이 칸마다 다른 박자로 숨 쉬고, 위로 작은 불꽃이 튄다.
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
		int w = Dungeon.level.width();
		for (int c : cells){
			floor.add(new Tile((c % w) * DungeonTilemap.SIZE, (c / w) * DungeonTilemap.SIZE, Random.Float(0f, 6.28f)));
		}
		sky.add(new Sparks());
	}

	//층의 캐릭터 무리 그룹에 붙인다: 빛 고임은 맨 뒤(바닥 쪽), 불꽃은 맨 앞
	public void attach( Group mobs ){
		mobs.addToBack(floor);
		mobs.add(sky);
	}

	public void kill(){
		floor.killAndErase();
		sky.killAndErase();
	}

	private class Tile extends Group {
		private final ColorBlock deep, base, core;
		private final float phase;
		private float time = 0;

		Tile( float x, float y, float phase ){
			this.phase = phase;
			deep = block(16, x, y, deepColor);
			base = block(12, x + 2, y + 2, baseColor);
			core = block(6, x + 5, y + 5, coreColor);
			update();
		}

		private ColorBlock block( float size, float x, float y, int color ){
			ColorBlock b = new ColorBlock(size, size, 0xFF000000 | color);
			b.x = x; b.y = y;
			add(b);
			return b;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float wave = 0.65f + 0.35f * (float)Math.sin(time * 7f + phase);
			deep.alpha(0.30f);
			base.alpha(0.30f * wave);
			core.alpha(0.40f * wave);
		}
	}

	private class Sparks extends Group {
		private float acc = 0;

		@Override
		public void update() {
			super.update();
			if (cells.isEmpty()) return;
			int w = Dungeon.level.width();
			acc += Game.elapsed * cells.size() * 2.5f;
			while (acc >= 1f){
				acc -= 1f;
				int c = cells.get(Random.Int(cells.size()));
				if (!Dungeon.level.heroFOV[c]) continue;
				StormZoneFx.Fade p = (StormZoneFx.Fade) recycle(StormZoneFx.Fade.class);
				float x = (c % w) * 16 + Random.Float(3, 13), y = (c / w) * 16 + Random.Float(6, 14);
				p.reset(x, y, Random.Int(3) == 0 ? 0xFFFFFF : sparkColor, 1f, Random.Float(0.2f, 0.4f));
				p.speed.set(Random.Float(-6, 6), Random.Float(-30, -16));
				p.acc.set(0, 30);
			}
		}
	}
}
