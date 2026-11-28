/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tangled;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

//Maze Tower (N6): 얽힌 것들. 땅에서 솟았다 파고드는 뿌리 고리들(maze_tangled.png 16×15, 원작 쥐와 같은 칸 배치, 줄 0~2 = 고리 배치가 다른 세 모양).
//(사용자) 셋이 같은 동작으로 보이지 않게 모양을 하나씩 다르게 쓰고, 가만히 있을 때의 박자도 어긋나게 시작한다.
//이어진 것끼리는 바닥 가까이 처진 뿌리 줄로 잇고, 뿌리에 쌓인 피해가 많을수록 줄이 붉어진다(RootLink).
public class TangledSprite extends MobSprite {

	public TangledSprite() {
		super();
		texture( Assets.Sprites.TANGLED );
		build( 0 );
		play( idle );
	}

	private void build( int row ){
		TextureFilm frames = new TextureFilm( texture, 16, 15 );
		int o = Math.max( 0, Math.min( 2, row ) ) * 16;
		idle = new Animation( 2, true );
		idle.frames( frames, o, o, o, o + 1 );
		run = new Animation( 10, true );
		run.frames( frames, o + 6, o + 7, o + 8, o + 9, o + 10 );
		attack = new Animation( 15, false );
		attack.frames( frames, o + 2, o + 3, o + 4, o + 5, o );
		die = new Animation( 10, false );
		die.frames( frames, o + 11, o + 12, o + 13, o + 14 );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		if (ch instanceof Tangled){
			build( ((Tangled) ch).variant );
			//박자를 어긋나게: 가만히 있는 동작을 무작위 칸에서, 빠르기도 조금씩 다르게
			idle.delay = 1f / Random.Float( 1.6f, 2.4f );
			run.delay = 1f / Random.Float( 9f, 11f );
			play( idle );
			curFrame = Random.Int( idle.frames.length );
			frame( idle.frames[curFrame] );
		}
	}

	private final HashMap<Integer, RootLink> roots = new HashMap<>();

	@Override
	public void update() {
		super.update();
		if (!(ch instanceof Tangled) || parent == null) return;
		Tangled t = (Tangled) ch;
		//나보다 id가 큰 짝과의 뿌리만 내가 그린다 (한 줄을 두 번 그리지 않게)
		for (int id : t.links){
			if (id <= t.id() || roots.containsKey( id )) continue;
			Actor a = Actor.findById( id );
			if (!(a instanceof Tangled) || a == null || ((Tangled) a).sprite == null) continue;
			RootLink r = new RootLink( this, ((Tangled) a).sprite, t, (Tangled) a );
			parent.addToBack( r );
			roots.put( id, r );
		}
		for (Integer id : new ArrayList<>( roots.keySet() )){
			if (!t.isAlive() || !t.linkedTo( id )){
				roots.remove( id ).killAndErase();
			}
		}
	}

	//(사용자) 쓰러진 뒤 땅에 오래 남지 않게 (원작 3초)
	@Override
	protected float fadeTime() {
		return MazeTower.N6_FADE_TIME;
	}

	@Override
	public void die() {
		for (RootLink r : roots.values()) r.killAndErase();
		roots.clear();
		super.die();
	}

	//(사용자) 얽힌 것들을 잇는 뿌리는 그 몸의 덩굴처럼: 둘 사이를 굵은 뿌리가 땅에서 솟았다 파고들기를 되풀이하며 잇고,
	//솟은 고리가 천천히 앞으로 흘러가듯 오르내린다. 파고드는 자리엔 흙 둔덕. 위쪽은 밝고 아래쪽은 어두운 둥근 뿌리에 짙은 테두리.
	//뿌리에 쌓인 피해가 많을수록 뿌리빛이 핏빛으로 물든다.
	public static class RootLink extends Group {
		private static final float STEP = 1f, ARCH = 4.5f, SPAN = 18f, FLOW = 0.5f;
		private final CharSprite a, b;
		private final Tangled ta, tb;
		private final Group outlines = new Group(), dirt = new Group(), bodies = new Group();
		private final ArrayList<ColorBlock> outL = new ArrayList<>(), body = new ArrayList<>(), hi = new ArrayList<>(), mounds = new ArrayList<>();
		private float time = Random.Float( 6f );

		public RootLink( CharSprite a, CharSprite b, Tangled ta, Tangled tb ){
			this.a = a; this.b = b; this.ta = ta; this.tb = tb;
			add( outlines ); add( dirt ); add( bodies );
		}

		private static int mix( int c0, int c1, float k ){
			int r = (int) (((c0 >> 16) & 0xFF) + (((c1 >> 16) & 0xFF) - ((c0 >> 16) & 0xFF)) * k);
			int g = (int) (((c0 >> 8) & 0xFF) + (((c1 >> 8) & 0xFF) - ((c0 >> 8) & 0xFF)) * k);
			int bl = (int) ((c0 & 0xFF) + ((c1 & 0xFF) - (c0 & 0xFF)) * k);
			return 0xFF000000 | (r << 16) | (g << 8) | bl;
		}

		private static ColorBlock take( ArrayList<ColorBlock> list, Group g, int i, int w, int h ){
			while (list.size() <= i){ ColorBlock c = new ColorBlock( w, h, 0xFFFFFFFF ); g.add( c ); list.add( c ); }
			return list.get( i );
		}

		@Override
		public void update() {
			super.update();
			if (a.parent == null || b.parent == null || !ta.isAlive() || !tb.isAlive()){ killAndErase(); return; }
			visible = a.visible || b.visible;
			if (!visible) return;
			time += Game.elapsed;
			float x1 = a.x + a.width / 2f, y1 = a.y + a.height - 1.5f, x2 = b.x + b.width / 2f, y2 = b.y + b.height - 1.5f;
			float len = (float) Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) );
			//(사용자) 징검다리처럼 띄엄띄엄 보이지 않게: 픽셀 칸에 딱 맞춰 끊김 없이 이어진 한 줄로 그린다
			int n = Math.max( 2, (int) (len / 0.5f) );
			float humps = Math.max( 1, Math.round( len / SPAN ) );
			//(사용자) 피해가 뿌리를 타고 지나가면 뿌리가 잠깐 핏빛으로 번쩍였다 0.6초에 걸쳐 돌아온다
			ta.shareFlash = Math.max( 0f, ta.shareFlash - Game.elapsed / MazeTower.N6_FLASH_TIME );
			tb.shareFlash = Math.max( 0f, tb.shareFlash - Game.elapsed / MazeTower.N6_FLASH_TIME );
			float heat = Math.max( ta.shareFlash, tb.shareFlash );
			//(사용자) 몸통 고리와 같은 색 단계: 윗줄은 가운데 색, 아랫줄은 그늘 색, 왼쪽 위 빛을 받는 오르막 면의 윗줄만 밝은 색
			int cLit = mix( 0xFF86685A, 0xFFD06070, heat ) & 0xFFFFFF, cTop = mix( 0xFF5E463E, 0xFF9A1C2A, heat ) & 0xFFFFFF, cMid = mix( 0xFF3E2C2A, 0xFF6A1420, heat ) & 0xFFFFFF;
			HashMap<Long, Integer> px = new HashMap<>(); // 5 빛 받는 위, 4 위, 3 아래(그늘), 2 흙 둔덕, 1 테두리
			int prevX = Integer.MIN_VALUE, prevY = 0;
			boolean wasUp = false;
			for (int i = 0; i < n; i++){
				float t = i / (float) (n - 1);
				float bx = x1 + (x2 - x1) * t, by = y1 + (y2 - y1) * t;
				//솟은 높이: 양 끝(몸 쪽)에서 0, 사이에서 고리가 앞으로 흘러가며 오르내림
				float s = (float) Math.sin( Math.PI * (humps * t - time * FLOW) );
				float lift = Math.abs( s ) * ARCH * (float) Math.min( 1, Math.sin( Math.PI * t ) * 2.5f );
				boolean up = lift > 1.0f;
				int gx = Math.round( bx ), gy = Math.round( by );
				if (up != wasUp && i > 0){ for (int d = -1; d <= 1; d++) put( px, gx + d, gy, 2 ); }
				wasUp = up;
				if (!up){ prevX = Integer.MIN_VALUE; continue; }   //땅속으로 들어간 곳은 보이지 않는다
				int x = gx, y = Math.round( by - lift );
				//가파른 곳에서 칸이 벌어지지 않게 사이를 메운다
				if (prevX != Integer.MIN_VALUE && Math.abs( x - prevX ) <= 1 && Math.abs( y - prevY ) > 1){
					int step = y > prevY ? 1 : -1;
					for (int yy = prevY + step; yy != y; yy += step){ put( px, x, yy, 4 ); put( px, x, yy + 1, 3 ); }
				}
				boolean lit = prevX != Integer.MIN_VALUE && ((x2 >= x1 && y < prevY) || (x2 < x1 && y > prevY));
				put( px, x, y, lit ? 5 : 4 ); put( px, x, y + 1, 3 );
				prevX = x; prevY = y;
			}
			//테두리: 뿌리 칸 둘레의 빈 칸
			for (Long k : new ArrayList<>( px.keySet() )){
				int v = px.get( k ); if (v < 3) continue;
				int x = (int) (k >> 32), y = (int) (long) k;
				int[][] nb = { {1,0}, {-1,0}, {0,1}, {0,-1} };
				for (int[] d : nb){ long kk = key( x + d[0], y + d[1] ); if (!px.containsKey( kk )) px.put( kk, 1 ); }
			}
			int i = 0;
			for (java.util.Map.Entry<Long, Integer> e : px.entrySet()){
				int x = (int) (e.getKey() >> 32), y = (int) (long) e.getKey();
				int v = e.getValue();
				ColorBlock c = take( body, bodies, i++, 1, 1 );
				c.visible = true; c.alpha( 1f );
				c.hardlight( v == 5 ? cLit : v == 4 ? cTop : v == 3 ? cMid : v == 2 ? 0x36282A : 0x120A0C );
				c.x = x; c.y = y;
			}
			for (int j = i; j < body.size(); j++) body.get( j ).visible = false;
		}

		private static long key( int x, int y ){ return ((long) x << 32) | (y & 0xFFFFFFFFL); }
		private static void put( HashMap<Long, Integer> px, int x, int y, int v ){
			long k = key( x, y );
			Integer old = px.get( k );
			if (old == null || old < v) px.put( k, v );
		}
	}
}
