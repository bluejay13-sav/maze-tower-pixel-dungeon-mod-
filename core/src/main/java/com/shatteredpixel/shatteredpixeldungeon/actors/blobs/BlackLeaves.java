/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (N1): 탑의 심장(21~24층) 바닥의 검은 낙엽. 퍼지지도 사라지지도 않는다.
//그 위에 서 있는 주인공은 마나가 차지 않는다(Mana.turnRegen, "시든 잎" 아이콘). 정화 물약(BlobImmunity 목록)으로 지운다. (사용자) 불에는 타지 않는다.
//새잎 땅(SproutGround) 위에는 쌓이지 않는다. 바닥의 7%(21층)~20%(24층)에 뭉치로 깔리고(N1_LEAF_FRACTION), 검은 잎의 시녀가 순간이동한 자리에 남긴다.
public class BlackLeaves extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		SproutGround sprout = (SproutGround) l.blobs.get( SproutGround.class );
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j * l.width();
				if (cur[cell] <= 0){ off[cell] = 0; continue; }
				//새잎 땅 위에는 쌓이지 않는다 ((사용자) 불에는 타지 않는다)
				if (sprout != null && sprout.volume > 0 && sprout.cur[cell] > 0){
					off[cell] = 0;
					continue;
				}
				off[cell] = cur[cell];
				volume += off[cell];
			}
		}
	}

	public static boolean at( int cell ){
		if (Dungeon.level == null) return false;
		BlackLeaves b = (BlackLeaves) Dungeon.level.blobs.get( BlackLeaves.class );
		return b != null && b.volume > 0 && cell >= 0 && cell < b.cur.length && b.cur[cell] > 0;
	}

	//주인공이 그 위에서 마나를 얻지 못하는지 (날고 있으면 괜찮다, 정화의 면역이면 괜찮다)
	public static boolean blocks( Char ch ){
		return MazeTower.N1_FLOORS && ch != null && !ch.flying && at( ch.pos ) && !ch.isImmune( BlackLeaves.class );
	}

	//한 칸에 깐다 (새잎 땅·벽·물 위에는 깔지 않는다)
	public static void drop( Level level, int cell ){
		if (cell < 0 || cell >= level.length() || level.solid[cell] || level.pit[cell] || level.water[cell]) return;
		if (SproutGround.at( level, cell )) return;
		Blob.seed( cell, 1, BlackLeaves.class, level );
	}

	//층을 만들 때 (HallsLevel.createMobs): 바닥 칸의 N1_LEAF_FRACTION만큼을 3~6칸 뭉치로. 원작 층 생성 난수를 바꾸지 않게 따로 된 난수
	public static void place( Level level ){
		if (!MazeTower.N1_FLOORS) return;
		Random.pushGenerator( Dungeon.seedCurDepth() ^ 0xB1AC4EAFL );
		try {
			boolean[] near = new boolean[level.length()];
			for (int t : new int[]{ level.entrance(), level.exit() }){
				if (t < 0) continue;
				PathFinder.buildDistanceMap( t, BArray.not( level.solid, null ), 2 );
				for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
			}
			int floors = 0;
			for (int i = 0; i < level.length(); i++) if (ok( level, i, near )) floors++;
			float frac = MazeTower.N1_LEAF_FRACTION[ Math.max( 0, Math.min( MazeTower.N1_LEAF_FRACTION.length - 1, Dungeon.depth - 21 ) ) ]; //(사용자) 층이 깊을수록 많이
			int want = Math.round( floors * frac ), placed = 0;
			for (int tries = 0; tries < 400 && placed < want; tries++){
				int start = Random.Int( level.length() );
				if (!ok( level, start, near ) || BlackLeaves.atLevel( level, start )) continue;
				ArrayList<Integer> patch = new ArrayList<>();
				patch.add( start );
				int size = Random.IntRange( MazeTower.N1_LEAF_CLUMP_MIN, MazeTower.N1_LEAF_CLUMP_MAX );
				for (int k = 0; k < 30 && patch.size() < size; k++){
					int c = Random.element( patch ) + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
					if (!patch.contains( c ) && ok( level, c, near )) patch.add( c );
				}
				for (int c : patch){ Blob.seed( c, 1, BlackLeaves.class, level ); placed++; }
			}
		} finally {
			Random.popGenerator();
		}
	}

	private static boolean atLevel( Level level, int cell ){
		BlackLeaves b = (BlackLeaves) level.blobs.get( BlackLeaves.class );
		return b != null && b.cur != null && b.cur[cell] > 0;
	}

	private static boolean ok( Level level, int c, boolean[] near ){
		return c >= 0 && c < level.length() && level.insideMap( c )
				&& (level.map[c] == Terrain.EMPTY || level.map[c] == Terrain.EMPTY_DECO || level.map[c] == Terrain.GRASS) && !near[c];
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add( new TileFxLayer( this, BossEffects.Fx.BLACK_LEAVES, 0, 1f ).connect( BossEffects.Fx.BLACK_LEAVES_CONN ).varied() );
	}

	@Override
	public String tileDesc() {
		return Messages.get( this, "desc", MazeTower.N1_RUSTLE_RANGE, Math.round( (MazeTower.N1_LEAF_DMG - 1f) * 100 ) );
	}

	//(사용자) 주인공이 검은 낙엽 칸에 들어서면 잎 몇 장이 살짝 흩날린다 (Level.occupyCell)
	public static void onStep( Char ch ){
		if (!MazeTower.N1_FLOORS || ch != Dungeon.hero || ch.flying || !at( ch.pos ) || !Dungeon.level.heroFOV[ch.pos]) return;
		CellEmitter.get( ch.pos ).burst( StepLeaf.FACTORY, MazeTower.N1_LEAF_STEP_FX );
		if (MazeTower.N1_RUSTLE) rustle( ch.pos );
		//Maze Tower (O5, 사용자): 25층 탑의 주인은 바스락 소리를 듣고 그 칸으로 낙엽 걸음을 한다
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel) ((com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel) Dungeon.level).rustleCell = ch.pos;
	}

	//(사용자) 바스락 소리: 주인공이 낙엽을 밟으면 둘레 N1_RUSTLE_RANGE칸 안의 적이 그 칸으로 모인다 (잠든 적은 깬다).
	//이미 쫓아오는 적은 그대로 둔다. 원작 경보 함정·사슬 고리와 같은 beckon. 투명이어도 소리는 나지만 적은 그 칸으로 올 뿐이다.
	//(사용자) 소리는 아주 작게, 연달아 밟아도 N1_RUSTLE_SOUND_GAP초에 한 번만 (25층 보스전처럼 낙엽 위를 계속 걸을 때 거슬리지 않게)
	private static long lastSound = 0;
	public static void rustle( int cell ){
		long now = com.watabou.noosa.Game.realTime;
		if (now - lastSound >= (long) (MazeTower.N1_RUSTLE_SOUND_GAP * 1000)){
			lastSound = now;
			com.watabou.noosa.audio.Sample.INSTANCE.play( com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.GRASS, MazeTower.N1_RUSTLE_VOLUME, Random.Float( 0.85f, 1.0f ) );
		}
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray( new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0] )){
			if (m.alignment != Char.Alignment.ENEMY || m.state == m.PASSIVE || m.state == m.HUNTING) continue;
			if (Dungeon.level.distance( m.pos, cell ) > MazeTower.N1_RUSTLE_RANGE) continue;
			m.beckon( cell );
		}
	}

	//흩날리는 잎: 낙엽 그림과 같은 빛깔(검정·흑갈색·빛바랜 어두운 주황·노랑), 조금 떠올랐다 천천히 내려앉으며 작아진다
	public static class StepLeaf extends PixelParticle.Shrinking {
		private static final int[] COLORS = { 0x3E3438, 0x4E3C30, 0x6E4430, 0x7C5434, 0x82703E, 0x6A5432 };
		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((StepLeaf) emitter.recycle( StepLeaf.class )).reset( x, y );
			}
		};
		public StepLeaf() {
			super();
			lifespan = 0.9f;
			acc.set( 0, 22 );
		}
		public void reset( float x, float y ) {
			revive();
			this.x = x;
			this.y = y;
			color( COLORS[Random.Int( COLORS.length )] );
			speed.set( Random.Float( -7, 7 ), Random.Float( -14, -9 ) );
			left = lifespan;
			size = Random.Float( 1.5f, 2.5f );
		}
	}

	//(사용자) 검은 낙엽 위에 선 적(날지 않는 적)은 주는 피해 +10% (Char.attack, 세계수의 열매와 겹침)
	public static boolean empowers( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ){
		return MazeTower.N1_FLOORS && ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
				&& ch.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY && !ch.flying && at( ch.pos );
	}

	//적의 차례마다: 검은 낙엽 위면 표시(아이콘)를 붙이고, 아니면 뗀다 (Mob.act)
	public static void syncMob( com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m ){
		boolean on = empowers( m );
		LeafMight b = m.buff( LeafMight.class );
		if (on && b == null) com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect( m, LeafMight.class );
		else if (!on && b != null) b.detach();
	}

	//검은 낙엽 위의 적 표시 (아이콘만)
	public static class LeafMight extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.POSITIVE; announced = false; }
		@Override public boolean act(){ if (!empowers( target )) detach(); spend( TICK ); return true; }
		@Override public int icon() { return BuffIndicator.WEAPON; }
		@Override public void tintIcon( Image icon ) { icon.hardlight( 0.45f, 0.3f, 0.3f ); }
		@Override public String desc() { return Messages.get( this, "desc", Math.round( (MazeTower.N1_LEAF_DMG - 1) * 100 ) ); }
	}

	//검은 낙엽 위에 서 있는 동안 (아이콘만, Mana.turnRegen이 붙이고 뗀다)
	public static class Withered extends FlavourBuff {
		{ type = buffType.NEGATIVE; announced = false; }
		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon( Image icon ) { icon.hardlight( 0.35f, 0.3f, 0.32f ); }
		@Override public float iconFadePercent() { return 0; }
		@Override public String desc() { return Messages.get( this, "desc" ); }
	}
}
