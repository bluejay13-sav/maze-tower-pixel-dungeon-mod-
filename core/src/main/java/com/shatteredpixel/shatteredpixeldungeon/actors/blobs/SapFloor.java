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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
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
//밟으면 젖음(물 위처럼, Wet.onWater) + 이동 감속(원작 다리 저림, 1턴씩 갱신). 불이 붙으면 타서 사라진다.
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
				if (ch != null && !ch.flying){
					Buff.prolong(ch, Cripple.class, 1f);
					Buff.affect(ch, Wet.class);
				}
			}
		}
	}

	//물 위에 깔리면 물을 대신한다 (SapPuddle과 같음)
	@Override
	public void seed( Level level, int cell, int amount ) {
		super.seed( level, cell, amount );
		SapPuddle.replaceWater( level, cell );
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
