/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

//Maze Tower (N3): 핏줄 눈의 광선이 지나간 줄. 잠깐(N3_HOT_TURNS) 달궈져 있고, 그 칸에 들어서는 누구든(날지 않으면) 피해를 받는다.
//칸마다 남은 턴을 담고 턴마다 하나씩 줄어든다. 퍼지지 않는다. 바닥에는 광선 방향 그대로 곧은 달궈진 줄이 그려진다(Layer, maze_hot_trail_line.png).
public class HotTrail extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		int cell;
		//(사용자) 턴이 끝날 때 발밑이 달궈진 칸이면 데인다 (들어설 때와 합쳐 한 턴에 한 번)
		if (MazeTower.N3_EYE){
			for (Char ch : com.shatteredpixel.shatteredpixeldungeon.actors.Actor.chars().toArray( new Char[0] )){
				if (ch.isAlive() && !ch.flying && ch.pos >= 0 && ch.pos < cur.length && cur[ch.pos] > 0) hurt( ch );
			}
		}
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j * l.width();
				off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;
				volume += off[cell];
			}
		}
		//광선 줄도 같이 한 턴씩 줄어든다
		for (int[] b : new ArrayList<>( beams )){ b[2]--; if (b[2] <= 0) beams.remove( b ); }
	}

	//광선이 지나간 칸을 달군다. 광선이 닿은 차례에 바로 하나 줄어드므로 1을 더 넣어, 주인공이 N3_HOT_TURNS번 움직이는 동안 남게 한다
	public static void heat( Level level, int cell ){
		if (!MazeTower.N3_EYE || cell < 0 || cell >= level.length() || level.solid[cell]) return;
		int turns = MazeTower.N3_HOT_TURNS + 1;
		HotTrail h = Blob.seed( cell, turns, HotTrail.class, level );
		if (h != null && h.cur[cell] > turns) h.cur[cell] = turns;
		//광선에 막 맞은 대상은 이번 턴에는 자국에 데지 않는다 (광선 피해와 겹치지 않게)
		Char ch = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar( cell );
		if (ch != null) lastBurn.put( ch.id(), turnOf() );
	}

	//대상마다 마지막으로 데인 턴 (한 턴에 한 번만 데게)
	private static final java.util.HashMap<Integer, Integer> lastBurn = new java.util.HashMap<>();
	private static int turnOf(){ return (int) Math.floor( com.shatteredpixel.shatteredpixeldungeon.actors.Actor.now() + 0.001f ); }

	public static boolean at( int cell ){
		if (Dungeon.level == null) return false;
		HotTrail h = (HotTrail) Dungeon.level.blobs.get( HotTrail.class );
		return h != null && h.volume > 0 && cell >= 0 && cell < h.cur.length && h.cur[cell] > 0;
	}

	//칸에 들어설 때 (Level.occupyCell)
	public static void onEnter( Char ch ){
		if (!MazeTower.N3_EYE || ch == null || ch.flying || !ch.isAlive() || !at( ch.pos )) return;
		hurt( ch );
	}

	private static void hurt( Char ch ){
		Integer last = lastBurn.get( ch.id() );
		int now = turnOf();
		if (last != null && last == now) return;
		lastBurn.put( ch.id(), now );
		ch.damage( Random.NormalIntRange( MazeTower.N3_HOT_DMG_MIN, MazeTower.N3_HOT_DMG_MAX ), new Burn() );
		if (Dungeon.level.heroFOV[ch.pos]){
			CellEmitter.get( ch.pos ).burst( HeatParticle.FACTORY, 6 );
			if (ch.sprite != null) ch.sprite.flash();
		}
		if (ch == Dungeon.hero && !ch.isAlive()){
			Dungeon.fail( Burn.class );
			GLog.n( Messages.get( HotTrail.class, "ondeath" ) );
		}
	}

	public static class Burn {}

	//(사용자) 광선 자국은 칸마다 끊어 그리지 않고 광선 방향 그대로 곧은 한 줄로 그린다: 광선 한 줄마다 [첫 칸, 마지막 칸, 남은 턴]
	public final java.util.concurrent.CopyOnWriteArrayList<int[]> beams = new java.util.concurrent.CopyOnWriteArrayList<>(); //(점검) 그림 쪽이 매 프레임 돌므로 겹쳐도 튕기지 않게

	//광선 한 줄을 기록한다 (Eye.deathGaze, 달궈진 칸을 다 정한 뒤). 칸들과 같은 턴만큼 남는다
	public static void addBeam( Level level, int from, int to ){
		if (!MazeTower.N3_EYE || from < 0 || to < 0) return;
		HotTrail h = (HotTrail) level.blobs.get( HotTrail.class );
		if (h == null) return;
		h.beams.add( new int[]{ from, to, MazeTower.N3_HOT_TURNS + 1 } );
	}

	private static final String BEAMS = "maze_beams";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		int[] flat = new int[beams.size() * 3];
		for (int i = 0; i < beams.size(); i++) System.arraycopy( beams.get( i ), 0, flat, i * 3, 3 );
		bundle.put( BEAMS, flat );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		beams.clear();
		if (bundle.contains( BEAMS )){
			int[] flat = bundle.getIntArray( BEAMS );
			for (int i = 0; i + 2 < flat.length; i += 3) beams.add( new int[]{ flat[i], flat[i + 1], flat[i + 2] } );
		}
	}

	//광선 한 줄을 곧게 그리는 층: 조각 그림(maze_hot_trail_line.png: 0 가운데, 1 시작 끝, 2 마지막 끝)을 광선 방향으로 돌려 첫 칸에서 마지막 칸까지 이어 붙인다. (사용자) 끝은 둥글게 맺고 검정 테두리.
	//양 끝은 반 칸쯤 더 나간다. (사용자) 움직이지 않는 그림이고, 사라질 때는 한꺼번에 옅어지지 않고 밝은 심부터 식어 어두워진 뒤(마지막 턴) 그을린 자국만 남았다 스러진다. 주인공이 보는 칸 위의 조각만 보인다.
	public static class Layer extends Group {
		private final HotTrail blob;
		private final HashMap<int[], ArrayList<Image>> drawn = new HashMap<>();
		private final HashMap<Image, Integer> cellOf = new HashMap<>();
		private final HashMap<int[], float[]> heat = new HashMap<>(); //줄마다 [달궈진 정도 1→0, 사라지는 중 진하기 1→0]
		private TextureFilm film;
		//(사용자) 피해를 주는 칸을 알기 쉽게: 달궈진 칸마다 바닥에 옅은 핏빛 칸 표시(줄 아래에 깔림). 남은 턴이 마지막 하나면 함께 옅어진다
		private final Group marks = new Group();
		private final HashMap<Integer, com.watabou.noosa.ColorBlock> markOf = new HashMap<>();

		public Layer( HotTrail blob ){
			this.blob = blob;
			add( marks );
		}

		private void updateMarks(){
			int S = DungeonTilemap.SIZE, w = Dungeon.level.width();
			for (int c = 0; c < blob.cur.length; c++){
				com.watabou.noosa.ColorBlock m = markOf.get( c );
				if (blob.cur[c] <= 0){
					if (m != null){ m.killAndErase(); markOf.remove( c ); }
					continue;
				}
				if (m == null){
					m = new com.watabou.noosa.ColorBlock( S - 2, S - 2, 0xFF8A1424 );
					m.x = (c % w) * S + 1;
					m.y = (c / w) * S + 1;
					marks.add( m );
					markOf.put( c, m );
				}
				m.visible = Dungeon.level.heroFOV[c];
				m.alpha( blob.cur[c] <= 1 ? 0.12f : MazeTower.N3_HOT_MARK_ALPHA );
			}
		}

		@Override
		public synchronized void update() {
			super.update();
			if (Dungeon.level == null || blob.cur == null) return; //(점검) 자국이 다 식은 뒤 저장·불러오면 cur가 비어 있어 튕기던 것
			updateMarks();
			float dt = Game.elapsed;
			for (int[] b : blob.beams) if (!drawn.containsKey( b )){ drawn.put( b, build( b ) ); heat.put( b, new float[]{ 1f, 1f } ); }
			for (int[] b : new ArrayList<>( drawn.keySet() )){
				float[] st = heat.get( b );
				boolean gone = !blob.beams.contains( b );
				//남은 턴이 마지막 하나가 되면 식기 시작한다(약 0.8초에 걸쳐 어두워짐), 줄이 끝나면 그을린 자국이 0.5초에 걸쳐 스러진다
				float target = (gone || b[2] <= 1) ? 0f : 1f;
				if (st[0] > target) st[0] = Math.max( target, st[0] - dt / 0.8f );
				if (gone) st[1] -= dt / 0.5f;
				if (st[1] <= 0){
					for (Image im : drawn.remove( b )){ cellOf.remove( im ); im.killAndErase(); }
					heat.remove( b );
					continue;
				}
				float h = st[0];
				for (Image im : drawn.get( b )){
					Integer c = cellOf.get( im );
					im.visible = c != null && c >= 0 && c < Dungeon.level.length() && Dungeon.level.heroFOV[c];
					//밝은 빛이 식으면 검붉게: 밝은 심일수록 많이 어두워지고 그을린 바깥은 거의 그대로
					im.rm = 0.30f + 0.70f * h; im.gm = 0.18f + 0.82f * h; im.bm = 0.22f + 0.78f * h;
					im.am = st[1];
				}
			}
		}

		private ArrayList<Image> build( int[] b ){
			ArrayList<Image> list = new ArrayList<>();
			int w = Dungeon.level.width(), S = DungeonTilemap.SIZE;
			float x0 = (b[0] % w) * S + S / 2f, y0 = (b[0] / w) * S + S / 2f;
			float x1 = (b[1] % w) * S + S / 2f, y1 = (b[1] / w) * S + S / 2f;
			float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt( dx * dx + dy * dy );
			float ux = len > 0 ? dx / len : 1, uy = len > 0 ? dy / len : 0;
			float ext = 13f; //끝 조각은 칸 가운데에서 둥글게 맺으므로 반 칸 남짓 더 나가게 둔다
			float sx = x0 - ux * ext, sy = y0 - uy * ext, total = len + ext * 2;
			int n = Math.max( 2, Math.round( total / S ) );
			float sp = total / n;
			float angle = (float) Math.toDegrees( Math.atan2( uy, ux ) );
			for (int k = 0; k < n; k++){
				float cx = sx + ux * sp * (k + 0.5f), cy = sy + uy * sp * (k + 0.5f);
				Image im = new Image( BossEffects.Fx.HOT_TRAIL_LINE.file );
				if (film == null) film = new TextureFilm( im.texture, S, S );
				im.frame( film.get( k == 0 ? 1 : (k == n - 1 ? 2 : 0) ) );
				im.origin.set( S / 2f, S / 2f );
				im.scale.x = sp / S;
				im.angle = angle;
				im.x = cx - S / 2f;
				im.y = cy - S / 2f;
				add( im );
				list.add( im );
				cellOf.put( im, (int)(cx / S) + (int)(cy / S) * w );
			}
			return list;
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
			//(사용자) 땅이 달궈진 것처럼 바닥에 곧은 줄이 남는다 (Layer)
		emitter.add( new Layer( this ) );
	}

	@Override
	public String tileDesc() {
		return Messages.get( this, "desc" );
	}

	//달궈진 줄에서 피어오르는 붉은 열기 (작은 점이 천천히 떠오르며 작아진다)
	public static class HeatParticle extends PixelParticle.Shrinking {
		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((HeatParticle) emitter.recycle( HeatParticle.class )).reset( x, y );
			}
			@Override
			public boolean lightMode() { return true; }
		};
		public HeatParticle() {
			super();
			lifespan = 0.7f;
		}
		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			color( ColorMath.random( 0xB02040, 0xFF7040 ) );
			speed.set( Random.Float( -2, 2 ), Random.Float( -14, -8 ) );
			left = lifespan;
			size = Random.Float( 1.5f, 2.5f );
		}
	}
}
