/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 뿌리 지하(1~4층)의 수액 웅덩이 바닥 (명세 20번). 사라지지 않는다.
//밟으면 이동 감속(원작 느려짐, 1턴씩 갱신). (결정) 젖지 않는다. 불이 붙으면 이어진 웅덩이로 불이 번지며(Fire) 타서 사라진다.
//5층 보스가 남기는 수액(SapPuddle)과는 다른 것이다. 그림: maze_sap_puddle.png (16×16 2프레임 반복)
public class SapFloor extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		Fire fire = (Fire) l.blobs.get(Fire.class);
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				if (cur[cell] <= 0){
					off[cell] = 0;
					continue;
				}
				//불에 타서 사라진다
				if (fire != null && fire.volume > 0 && fire.cur[cell] > 0){
					off[cell] = 0;
					continue;
				}
				off[cell] = cur[cell];
				volume += off[cell];

				Char ch = Actor.findChar(cell);
				if (ch != null && !ch.flying && !ch.isImmune(getClass())){ //Maze Tower (E2): 정화 물약의 면역
					Buff.prolong(ch, Cripple.class, 1f);
				}
			}
		}
	}

	//물 위에 깔리면 물을 대신한다 (SapPuddle과 같음)
	@Override
	public void seed( Level level, int cell, int amount ) {
		if (SapPuddle.onDoor( level, cell )) return; //(사용자) 문 칸에는 수액이 생기지 않는다 (수액 채집꾼이 문간에서 쓰러져도)
		super.seed( level, cell, amount );
		SapPuddle.replaceWater( level, cell );
	}

	//이어지는 모양 번호 (TileFxLayer.connect와 같은 계산: 위1·오른쪽2·아래4·왼쪽8 + 대각선 16·32·64·128)
	public static int connMask( int i ){
		SapFloor s = Dungeon.level == null ? null : (SapFloor) Dungeon.level.blobs.get(SapFloor.class);
		if (s == null) return 0;
		int[] cur = s.cur; int w = Dungeon.level.width(); int mask = 0;
		if (i - w >= 0 && cur[i - w] > 0)                         mask |= 1;
		if ((i % w) < w - 1 && cur[i + 1] > 0)                  mask |= 2;
		if (i + w < cur.length && cur[i + w] > 0)                 mask |= 4;
		if ((i % w) > 0 && cur[i - 1] > 0)                      mask |= 8;
		boolean l = (i % w) > 0, r = (i % w) < w - 1;
		if (r && i - w + 1 >= 0 && cur[i - w + 1] > 0)                mask |= 16;
		if (r && i + w + 1 < cur.length && cur[i + w + 1] > 0)        mask |= 32;
		if (l && i + w - 1 < cur.length && cur[i + w - 1] > 0)        mask |= 64;
		if (l && i - w - 1 >= 0 && cur[i - w - 1] > 0)                mask |= 128;
		return mask;
	}

	//살펴보기 창(WndInfoCell)의 그림: 원작처럼 지도에 보이는 그 칸 그대로 (바닥 타일 위에 수액 웅덩이)
	public static com.watabou.noosa.Image infoImage( int cell, com.watabou.noosa.Image base ){
		try {
			com.watabou.gltextures.SmartTexture bt = base.texture;
			com.watabou.utils.RectF f = base.frame();
			int S = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.SIZE;
			com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(S, S, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
			pm.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.None);
			pm.drawPixmap(bt.bitmap, 0, 0, Math.round(f.left * bt.width), Math.round(f.top * bt.height), S, S);
			BossEffects.Fx fx = BossEffects.Fx.ROOT_SAP_CONN;
			com.watabou.gltextures.SmartTexture st = com.watabou.gltextures.TextureCache.get(fx.file);
			int idx = connMask(cell) * fx.frames, per = st.width / fx.w;
			pm.setBlending(com.badlogic.gdx.graphics.Pixmap.Blending.SourceOver);
			pm.drawPixmap(st.bitmap, 0, 0, (idx % per) * fx.w, (idx / per) * fx.h, fx.w, fx.h);
			return new com.watabou.noosa.Image(new com.watabou.gltextures.SmartTexture(pm));
		} catch (Exception e){
			return base;
		}
	}

	public static boolean at( int cell ){
		if (Dungeon.level == null) return false;
		SapFloor s = (SapFloor) Dungeon.level.blobs.get(SapFloor.class);
		return s != null && s.volume > 0 && cell >= 0 && cell < s.cur.length && s.cur[cell] > 0;
	}

	//층을 만들 때 수액 웅덩이를 깐다 (SewerLevel.createMobs). 층과 상관없이 SAP_FLOOR_LEVEL_CHANCE 확률로 한 묶음, 묶음당 2~4칸
	public static void place( Level level ){
		//원작 층 생성의 난수 흐름을 바꾸지 않도록 따로 된 난수를 쓴다 (같은 시드면 원작과 같은 층)
		Random.pushGenerator(Dungeon.seedCurDepth() ^ 0x5A9F100DL);
		try {
			placeInner(level);
		} finally {
			Random.popGenerator();
		}
	}

	private static void placeInner( Level level ){
		int patches = Random.Float() < MazeTower.SAP_FLOOR_LEVEL_CHANCE ? 1 : 0;
		if (patches <= 0) return;

		//입구·출구 근처에는 두지 않는다
		boolean[] near = new boolean[level.length()];
		for (int t : new int[]{level.entrance(), level.exit()}){
			if (t < 0) continue;
			PathFinder.buildDistanceMap(t, BArray.not(level.solid, null), 2);
			for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
		}

		for (int p = 0; p < patches; p++){
			int start = -1;
			for (int tries = 0; tries < 200 && start == -1; tries++){
				int c = Random.Int(level.length());
				if (ok(level, c, near)) start = c;
			}
			if (start == -1) continue;

			ArrayList<Integer> patch = new ArrayList<>();
			patch.add(start);
			int size = Random.IntRange(MazeTower.SAP_FLOOR_SIZE_MIN, MazeTower.SAP_FLOOR_SIZE_MAX);
			for (int tries = 0; tries < 30 && patch.size() < size; tries++){
				int from = Random.element(patch);
				int c = from + PathFinder.NEIGHBOURS4[Random.Int(4)];
				if (!patch.contains(c) && ok(level, c, near)) patch.add(c);
			}
			for (int c : patch){
				Blob.seed(c, 1, SapFloor.class, level);
			}
		}
	}

	private static boolean ok( Level level, int c, boolean[] near ){
		return c >= 0 && c < level.length() && level.insideMap(c)
				&& level.map[c] == Terrain.EMPTY && !near[c];
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new TileFxLayer(this, BossEffects.Fx.ROOT_SAP, 3, 0.9f).connect(BossEffects.Fx.ROOT_SAP_CONN));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
