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
		emitter.add(new SoulLayer(this));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	//(결정) 영혼 느낌: 구슬 그림(maze_fx20w_soul_orb)을 반쯤 비치게 하고 위아래로 둥실 떠다니며 깜빡이게,
	//아래에 옅은 푸른 빛무리, 위로 흐릿한 넋 가닥(작은 빛 알갱이)이 살랑이며 피어올라 사라진다.
	public static class SoulLayer extends com.watabou.noosa.Group {

		private final Blob blob;
		private final java.util.HashMap<Integer, com.watabou.noosa.MovieClip> orbs = new java.util.HashMap<>();
		private final java.util.HashMap<Integer, com.watabou.noosa.ColorBlock> glows = new java.util.HashMap<>();
		private final java.util.HashMap<Integer, Float> phases = new java.util.HashMap<>();
		private float time = 0, wispAcc = 0;

		public SoulLayer( Blob blob ){
			this.blob = blob;
		}

		@Override
		public synchronized void update() {
			super.update();
			if (blob.cur == null || Dungeon.level == null) return;
			time += com.watabou.noosa.Game.elapsed;
			int w = Dungeon.level.width();
			java.util.ArrayList<Integer> live = new java.util.ArrayList<>();
			for (int i = 0; i < blob.cur.length; i++){
				com.watabou.noosa.MovieClip orb = orbs.get(i);
				if (blob.cur[i] > 0 && orb == null){
					com.watabou.noosa.ColorBlock g = new com.watabou.noosa.ColorBlock(10, 4, 0xFF7FC8FF);
					add(g);
					glows.put(i, g);
					orb = BossEffects.clip(BossEffects.Fx.WK_SOUL_ORB, 6, true);
					add(orb);
					orbs.put(i, orb);
					phases.put(i, com.watabou.utils.Random.Float(0f, 6.28f));
				} else if (blob.cur[i] <= 0 && orb != null){
					orb.killAndErase();
					glows.remove(i).killAndErase();
					orbs.remove(i);
					phases.remove(i);
					continue;
				}
				if (orb == null) continue;
				live.add(i);
				float ph = phases.get(i);
				float bob = (float)Math.sin(time * 2.2f + ph) * 1.5f;
				float x = (i % w) * com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.SIZE;
				float y = (i / w) * com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.SIZE;
				orb.x = x;
				orb.y = y - 3 + bob;
				orb.alpha(0.62f + 0.22f * (float)Math.sin(time * 3.7f + ph * 2f));
				com.watabou.noosa.ColorBlock g = glows.get(i);
				g.x = x + 3;
				g.y = y + 11;
				g.alpha(0.16f + 0.08f * (float)Math.sin(time * 2.2f + ph));
				boolean seen = Dungeon.level.heroFOV[i];
				orb.visible = g.visible = seen;
			}
			//넋 가닥: 보이는 구슬에서 위로 피어오른다
			wispAcc += com.watabou.noosa.Game.elapsed * live.size() * 6f;
			while (wispAcc >= 1f && !live.isEmpty()){
				wispAcc -= 1f;
				int c = live.get(com.watabou.utils.Random.Int(live.size()));
				if (!Dungeon.level.heroFOV[c]) continue;
				com.shatteredpixel.shatteredpixeldungeon.effects.StormZoneFx.Fade p =
						(com.shatteredpixel.shatteredpixeldungeon.effects.StormZoneFx.Fade) recycle(com.shatteredpixel.shatteredpixeldungeon.effects.StormZoneFx.Fade.class);
				float x = (c % w) * 16 + 8 + com.watabou.utils.Random.Float(-3, 3);
				float y = (c / w) * 16 + 4 + com.watabou.utils.Random.Float(-2, 2);
				p.reset(x, y, com.watabou.utils.Random.oneOf(0xFFFFFF, 0xCFEFFF, 0x8FD0FF, 0x6FA8E8), 1f, com.watabou.utils.Random.Float(0.6f, 1.0f));
				p.speed.set(com.watabou.utils.Random.Float(-4, 4), com.watabou.utils.Random.Float(-14, -8));
				p.acc.set(com.watabou.utils.Random.Float(-6, 6), -4);
			}
		}
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
