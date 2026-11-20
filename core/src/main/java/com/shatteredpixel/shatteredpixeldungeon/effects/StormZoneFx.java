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
//  바닥(캐릭터 아래): (결정, 바뀜) 칸을 칠하지 않고 구역의 바깥 테두리만 진한 원소 색 줄(+ 안쪽 밝은 줄)로, 숨 쉬듯 일렁인다.
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
		//(결정, 바뀜) 칸을 칠하면 원소 타일과 겹쳐 헷갈려서, 구역의 바깥 테두리만 진한 색 줄로 그린다
		int w = Dungeon.level.width();
		floor.add(new Border(w));
	}

	//층의 캐릭터 무리 그룹에 붙인다: 테두리는 맨 뒤(바닥 쪽), 폭풍 입자는 맨 앞
	public void attach( Group mobs ){
		mobs.addToBack(floor);
		if (storm) mobs.add(sky);
		sky.add(new Spawner());
	}

	public void kill(){
		floor.killAndErase();
		sky.killAndErase();
	}

	// ===== 구역 테두리: 바깥은 진한 원소 색 2픽셀, 그 안쪽에 밝은 1픽셀 =====
	private class Border extends Group {
		private final ArrayList<ColorBlock> outer = new ArrayList<>(), inner = new ArrayList<>();
		private float time = 0;

		Border( int w ){
			for (int c : cells){
				float x = (c % w) * DungeonTilemap.SIZE, y = (c / w) * DungeonTilemap.SIZE;
				if (!set.contains(c - w)) line(x, y, 16, 2, x, y + 2, 16, 1);           //위
				if (!set.contains(c + w)) line(x, y + 14, 16, 2, x, y + 13, 16, 1);     //아래
				if (!set.contains(c - 1)) line(x, y, 2, 16, x + 2, y, 1, 16);           //왼쪽
				if (!set.contains(c + 1)) line(x + 14, y, 2, 16, x + 13, y, 1, 16);     //오른쪽
			}
			update();
		}

		private void line( float ox, float oy, float ow, float oh, float ix, float iy, float iw, float ih ){
			ColorBlock o = new ColorBlock(ow, oh, 0xFF000000 | BASE[element]);
			o.x = ox; o.y = oy;
			add(o);
			outer.add(o);
			ColorBlock i = new ColorBlock(iw, ih, 0xFF000000 | RIM[element]);
			i.x = ix; i.y = iy;
			add(i);
			inner.add(i);
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			//경고는 천천히 숨 쉬듯, 폭풍은 진하게 빠르게
			float wave = storm ? 0.85f + 0.15f * (float)Math.sin(time * 6f) : 0.6f + 0.4f * (float)Math.sin(time * 3f);
			float k = storm ? 1f : 0.8f;
			for (ColorBlock o : outer) o.alpha(0.85f * k * wave);
			for (ColorBlock i : inner) i.alpha(0.55f * k * wave);
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
