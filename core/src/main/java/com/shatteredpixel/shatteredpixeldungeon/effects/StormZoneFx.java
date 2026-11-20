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
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

//Maze Tower: 여제 원소 폭풍 구역 연출 (명세 22번). 그림 시트(칸마다 같은 박자로 움직이는 바닥 줄·번개)가 어색해서 코드로 그린다.
//  바닥(캐릭터 아래): 원소 색을 칸마다 조금씩 다른 진하기로 깔고, 구역 가장자리는 밝은 테두리, 안쪽은 어둡게, 천천히 일렁인다.
//  폭풍(캐릭터 위): 불 = 비스듬히 떨어지는 불씨, 냉기 = 흩날리는 눈송이, 번개 = 무작위 칸에 가끔 내리꽂는 들쭉날쭉한 번개 + 번쩍임.
//경고 때는 바닥만 옅게, 폭풍 때는 바닥을 진하게 + 위의 입자.
public class StormZoneFx {

	public static final int FIRE = 1, FROST = 2, SHOCK = 3;

	private static final int[] BASE  = {0, 0xFF5A2A, 0x5AB0FF, 0xFFE040};
	private static final int[] RIM   = {0, 0xFFC070, 0xC8ECFF, 0xFFF8C0};
	private static final int[] DEEP  = {0, 0x5A1810, 0x0E2A50, 0x5A4A08};

	private final Group floor = new Group(), sky = new Group();
	private final ArrayList<Integer> cells;
	private final HashSet<Integer> set;
	private final int element;
	private final boolean storm;

	public StormZoneFx( ArrayList<Integer> cells, int element, boolean storm ){
		this.cells = cells;
		this.set = new HashSet<>(cells);
		this.element = element;
		this.storm = storm;
		int w = Dungeon.level.width();
		for (int c : cells){
			boolean edge = false;
			int[] n4 = {-w, 1, w, -1};
			for (int n : n4) if (!set.contains(c + n)) edge = true;
			float x = (c % w) * DungeonTilemap.SIZE, y = (c / w) * DungeonTilemap.SIZE;
			//안쪽은 어두운 바탕, 그 위에 원소 색 (칸마다 진하기를 다르게)
			Tile t = new Tile(x, y, edge, Random.Float(0.75f, 1.15f), Random.Float(0f, 6.28f));
			floor.add(t);
		}
	}

	//층의 캐릭터 무리 그룹에 붙인다: 바닥은 맨 뒤, 폭풍 입자는 맨 앞
	public void attach( Group mobs ){
		mobs.addToBack(floor);
		if (storm) mobs.add(sky);
		sky.add(new Spawner());
	}

	public void kill(){
		floor.killAndErase();
		sky.killAndErase();
	}

	// ===== 바닥 한 칸 =====
	private class Tile extends Group {
		private final ColorBlock deep, base, rim;
		private final float shade, phase;
		private float time = 0;

		Tile( float x, float y, boolean edge, float shade, float phase ){
			this.shade = shade;
			this.phase = phase;
			deep = new ColorBlock(16, 16, 0xFF000000 | DEEP[element]);
			deep.x = x; deep.y = y;
			add(deep);
			base = new ColorBlock(14, 14, 0xFF000000 | BASE[element]);
			base.x = x + 1; base.y = y + 1;
			add(base);
			rim = new ColorBlock(16, 16, 0xFF000000 | RIM[element]);
			rim.x = x; rim.y = y;
			rim.visible = edge;
			add(rim);
			update();
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float wave = 0.8f + 0.2f * (float)Math.sin(time * 1.8f + phase);
			//경고는 옅게, 폭풍은 진하게. 불은 청록 바닥에서 묻혀 보여서 더 진하게
			float k = (storm ? 1f : 0.7f) * 1.35f; //(결정) 화염·서리·번개 모두 진하게 (청록 바닥에 묻히지 않게)
			deep.alpha(Math.min(0.6f, 0.30f * k * wave));
			base.alpha(Math.min(0.6f, 0.32f * k * shade * wave));
			rim.alpha(Math.min(0.5f, 0.22f * k * wave));
		}
	}

	// ===== 폭풍 입자 =====
	private class Spawner extends Group {
		private float acc = 0, boltTimer = 0.2f;

		@Override
		public void update() {
			super.update();
			if (!storm || cells.isEmpty()) return;
			int w = Dungeon.level.width();
			if (element == SHOCK){
				boltTimer -= Game.elapsed;
				if (boltTimer <= 0){
					boltTimer = Random.Float(0.07f, 0.18f); //(결정) 더 촘촘하게
					strike(cells.get(Random.Int(cells.size())), w);
					if (Random.Int(2) == 0) strike(cells.get(Random.Int(cells.size())), w);
				}
				//잔 정전기 불꽃
				acc += Game.elapsed * cells.size() * 2.5f;
			} else {
				acc += Game.elapsed * cells.size() * (element == FIRE ? 4f : 3.5f);
			}
			while (acc >= 1f){
				acc -= 1f;
				int c = cells.get(Random.Int(cells.size()));
				float x = (c % w) * DungeonTilemap.SIZE + Random.Float(16), y = (c / w) * DungeonTilemap.SIZE + Random.Float(16);
				Fade p = (Fade) recycle(Fade.class);
				if (element == FIRE){
					p.reset(x - 6, y - 18, Random.oneOf(0xFFF0B0, 0xFFE29A, 0xFFA54A, 0xFF6A2E, 0xC83A1E), Random.Float(1.5f, 2.5f), Random.Float(0.4f, 0.6f));
					p.speed.set(Random.Float(10, 18), Random.Float(40, 60));
				} else if (element == FROST){
					p.reset(x, y - 16, Random.oneOf(0xFFFFFF, 0xFFFFFF, 0xDDEEFF, 0xA8D4FF, 0x7FC0FF), Random.Float(1.5f, 2.5f), Random.Float(0.6f, 0.9f));
					p.speed.set(Random.Float(-8, 8), Random.Float(16, 26));
				} else {
					p.reset(x, y, Random.oneOf(0xFFFFFF, 0xFFF29A, 0xD8C24A), 1f, Random.Float(0.12f, 0.25f));
					p.speed.set(Random.Float(-20, 20), Random.Float(-20, 20));
				}
				p.acc.set(0, 0);
			}
		}

		//하늘에서 내리꽂는 번개 한 줄기 + 닿는 칸 번쩍임
		private void strike( int cell, int w ){
			PointF to = DungeonTilemap.tileCenterToWorld(cell);
			PointF from = new PointF(to.x + Random.Float(-10, 10), to.y - 48);
			add(new Lightning(from, to, null));
			ColorBlock flash = new ColorBlock(16, 16, 0xFFFFF4B0);
			flash.x = (cell % w) * DungeonTilemap.SIZE;
			flash.y = (cell / w) * DungeonTilemap.SIZE;
			flash.alpha(0.55f);
			add(flash);
			add(new com.watabou.noosa.tweeners.AlphaTweener(flash, 0f, 0.25f){
				@Override
				protected void onComplete() {
					flash.killAndErase();
					killAndErase();
				}
			});
			for (int i = 0; i < 4; i++){
				Fade p = (Fade) recycle(Fade.class);
				p.reset(to.x, to.y + 4, Random.oneOf(0xFFFFFF, 0xFFF29A), 1f, Random.Float(0.15f, 0.3f));
				p.speed.polar(Random.Float(-3.1f, 0f), Random.Float(20, 45));
				p.acc.set(0, 60);
			}
		}
	}

	//점점 옅어지는 작은 입자
	public static class Fade extends PixelParticle {
		@Override
		public void update() {
			super.update();
			am = Math.max(0f, left / lifespan);
		}
	}
}
