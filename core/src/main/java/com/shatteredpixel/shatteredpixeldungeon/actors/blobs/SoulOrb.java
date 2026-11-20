/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower: 영혼 구슬 (20층 망령 왕 1단계, 명세 22번). 그림 maze_fx20w_soul_orb.png (반복).
//왕이 먼저 닿으면 흡수해 영혼 +1, 주인공이 먼저 밟으면 사라지고 주인공은 신속 2턴(CityBossLevel.occupyCell → WraithKing.onOccupy).
//구슬은 스스로 사라지지 않는다. 떨어질 칸 예고는 Warn (maze_fx20w_soul_warn.png).
public class SoulOrb extends Blob {

	@Override
	protected void evolve() {
		keep(this);
	}

	static void keep( Blob b ){
		Level l = Dungeon.level;
		int cell;
		for (int i = b.area.left; i < b.area.right; i++){
			for (int j = b.area.top; j < b.area.bottom; j++){
				cell = i + j*l.width();
				b.off[cell] = b.cur[cell];
				b.volume += b.off[cell];
			}
		}
	}

	public static boolean at( int cell ){
		return at(SoulOrb.class, cell);
	}

	static boolean at( Class<? extends Blob> c, int cell ){
		Blob s = Dungeon.level.blobs.get(c);
		return s != null && s.volume > 0 && cell >= 0 && cell < s.cur.length && s.cur[cell] > 0;
	}

	public static void remove( Class<? extends Blob> c, int cell ){
		Blob s = Dungeon.level.blobs.get(c);
		if (s != null) s.clear(cell);
	}

	public static void removeAll( Class<? extends Blob> c ){
		Blob s = Dungeon.level.blobs.get(c);
		if (s != null && s.cur != null){
			for (int i = 0; i < s.cur.length; i++) if (s.cur[i] > 0) s.clear(i);
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new TileFxLayer(this, BossEffects.Fx.WK_SOUL_ORB, 6, 1f));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	//구슬이 떨어질 칸 예고 (1턴)
	public static class Warn extends Blob {

		@Override
		protected void evolve() {
			keep(this);
		}

		public static boolean at( int cell ){
			return SoulOrb.at(Warn.class, cell);
		}

		@Override
		public void use( BlobEmitter emitter ) {
			super.use( emitter );
			emitter.add(new TileFxLayer(this, BossEffects.Fx.WK_SOUL_WARN, 4, 0.9f));
		}

		@Override
		public String tileDesc() {
			return Messages.get(this, "desc");
		}
	}
}
