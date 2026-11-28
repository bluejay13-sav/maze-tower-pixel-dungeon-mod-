/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RainbowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;

public abstract class ElementalSprite extends MobSprite {
	
	protected int boltType;
	
	protected abstract int texOffset();
	
	private Emitter particles;
	protected abstract Emitter createEmitter();
	
	public ElementalSprite() {
		super();
		
		int c = texOffset();
		
		texture( Assets.Sprites.ELEMENTAL );
		
		TextureFilm frames = new TextureFilm( texture, 12, 14 );
		
		idle = new Animation( 10, true );
		idle.frames( frames, c+0, c+1, c+2 );
		
		run = new Animation( 12, true );
		run.frames( frames, c+0, c+1, c+3 );
		
		attack = new Animation( 15, false );
		attack.frames( frames, c+4, c+5, c+6 );
		
		zap = attack.clone();
		
		die = new Animation( 15, false );
		die.frames( frames, c+7, c+8, c+9, c+10, c+11, c+12, c+13, c+12 );
		
		play( idle );
	}
	
	@Override
	public void link( Char ch ) {
		super.link( ch );
		
		if (particles == null) {
			particles = createEmitter();
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (particles != null){
			particles.visible = visible;
		}
		mazeTint();
	}
	
	@Override
	public void die() {
		super.die();
		if (particles != null){
			particles.on = false;
		}
	}
	
	@Override
	public void kill() {
		super.kill();
		if (particles != null){
			particles.killAndErase();
		}
	}
	
	//Maze Tower (L3·L4, 사용자): 풀려난 주문은 평소 원작보다 작고 적은 입자를 뿜고(MazeTower.L3_PARTICLE_SIZE·L3_PARTICLE_SPARSE),
	//궁정 마도사에게 증폭되면 원작 입자(원작 크기·개수)를 뿜으며 몸이 아주 조금 커진다(MazeTower.L4_AMP_SCALE). 증폭이 끊기면 다시 작게
	private int shownAmp = -1;

	//(사용자: 입자가 너무 자유분방하지 않게) 원작 입자를 차분하게: 원소 둘레의 좁은 자리에서만 생기고(mazeTint의 particles.pos),
	//천천히 움직인다(MazeTower.L3_PARTICLE_CALM, 불꽃은 천천히 떠오름 L3_PARTICLE_RISE). k = 크기 배율 (평소 L3_PARTICLE_SIZE, 증폭 1).
	//원작 입자는 매 프레임 크기를 다시 정하므로 그 뒤에 곱한다
	private static final float CALM = com.shatteredpixel.shatteredpixeldungeon.MazeTower.L3_PARTICLE_CALM;
	public static class CalmFlame extends FlameParticle {
		public float k = 1;
		public CalmFlame(){ super(); acc.set( 0, -com.shatteredpixel.shatteredpixeldungeon.MazeTower.L3_PARTICLE_RISE ); }
		@Override public void update(){ super.update(); scale.set( scale.x * k ); }
	}
	public static class CalmMagic extends MagicMissile.MagicParticle {
		public float k = 1;
		public CalmMagic(){ super(); speed.scale( CALM ); }
		@Override public void update(){ super.update(); scale.set( scale.x * k ); }
	}
	public static class CalmSpark extends SparkParticle {
		public float k = 1;
		@Override public void update(){ super.update(); scale.set( scale.x * k ); }
	}
	public static class CalmRainbow extends RainbowParticle {
		public float k = 1;
		@Override public void update(){ super.update(); scale.set( scale.x * k ); }
	}
	//type 0 불꽃, 1 푸른 마력, 2 불똥, 3 무지개 빛
	protected static Emitter.Factory calm( final int type, final float k ){
		return new Emitter.Factory(){
			@Override public void emit( Emitter e, int index, float x, float y ){
				switch (type){
					case 0: { CalmFlame p = (CalmFlame)e.recycle( CalmFlame.class ); p.k = k; p.reset( x, y ); break; }
					case 1: { CalmMagic p = (CalmMagic)e.recycle( CalmMagic.class ); p.k = k; p.reset( x, y ); break; }
					case 2: { CalmSpark p = (CalmSpark)e.recycle( CalmSpark.class ); p.k = k; p.resetStatic( x, y ); break; }
					default: { CalmRainbow p = (CalmRainbow)e.recycle( CalmRainbow.class ); p.k = k; p.resetBurst( x, y ); p.speed.scale( CALM ); break; }
				}
			}
			@Override public boolean lightMode(){ return true; }
		};
	}
	private static final float QK = com.shatteredpixel.shatteredpixeldungeon.MazeTower.L3_PARTICLE_SIZE;
	protected static final Emitter.Factory QUIET_FLAME = calm( 0, QK ), AMP_FLAME = calm( 0, 1 );
	protected static final Emitter.Factory QUIET_MAGIC = calm( 1, QK ), AMP_MAGIC = calm( 1, 1 );
	protected static final Emitter.Factory QUIET_SPARK = calm( 2, QK ), AMP_SPARK = calm( 2, 1 );
	protected static final Emitter.Factory QUIET_RAINBOW = calm( 3, QK ), AMP_RAINBOW = calm( 3, 1 );

	//원작 입자와 그 간격 (증폭됐을 때), 작은 입자 (평소). null이면 바꾸지 않는다
	protected Emitter.Factory spellFactory(){ return null; }
	protected Emitter.Factory quietFactory(){ return null; }
	protected float spellInterval(){ return 0.06f; }

	protected void mazeTint(){
		if (!(ch instanceof Elemental)) return;
		boolean amp = ch.isAlive() && ((Elemental) ch).mazeAmpBy != -1;
		float want = amp ? com.shatteredpixel.shatteredpixeldungeon.MazeTower.L4_AMP_SCALE : 1f;
		if (scale.x != want){
			origin.set( width / 2f, height );
			scale.set( want );
		}
		if (particles != null && particles.on && quietFactory() != null && ch.isAlive()){
			int now = amp ? 1 : 0;
			if (now != shownAmp){
				shownAmp = now;
				particles.pos( this, 3, 2, 6, 7 ); //(사용자) 원소가 떠 있는 자리에서만
				if (amp) particles.pour( spellFactory(), spellInterval() * com.shatteredpixel.shatteredpixeldungeon.MazeTower.L4_AMP_DENSE );
				else     particles.pour( quietFactory(), spellInterval() * com.shatteredpixel.shatteredpixeldungeon.MazeTower.L3_PARTICLE_SPARSE );
			}
		}
	}

	public void zap( int cell ) {
		super.zap( cell );
		
		MagicMissile.boltFromChar( parent,
				boltType,
				this,
				cell,
				new Callback() {
					@Override
					public void call() {
						((Elemental)ch).onZapComplete();
					}
				} );
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}
	
	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}
	
	public static class Fire extends ElementalSprite {
		@Override protected Emitter.Factory spellFactory(){ return AMP_FLAME; } //Maze Tower (L3·L4)
		@Override protected Emitter.Factory quietFactory(){ return QUIET_FLAME; }
		
		{
			boltType = MagicMissile.FIRE;
		}
		
		@Override
		protected int texOffset() {
			return 0;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( FlameParticle.FACTORY, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFFFFBB33;
		}
	}
	
	public static class NewbornFire extends ElementalSprite {
		
		{
			boltType = MagicMissile.ELMO;
		}
		
		@Override
		protected int texOffset() {
			return 14;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( ElmoParticle.FACTORY, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFF85FFC8;
		}
	}
	
	public static class Frost extends ElementalSprite {
		@Override protected Emitter.Factory spellFactory(){ return AMP_MAGIC; } //Maze Tower (L3·L4)
		@Override protected Emitter.Factory quietFactory(){ return QUIET_MAGIC; }
		
		{
			boltType = MagicMissile.FROST;
		}
		
		@Override
		protected int texOffset() {
			return 28;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( MagicMissile.MagicParticle.FACTORY, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFF8EE3FF;
		}
	}
	
	public static class Shock extends ElementalSprite {
		@Override protected Emitter.Factory spellFactory(){ return AMP_SPARK; } //Maze Tower (L3·L4)
		@Override protected Emitter.Factory quietFactory(){ return QUIET_SPARK; }
		
		//different bolt, so overrides zap
		@Override
		public void zap( int cell ) {
			super.zap( cell, null );
			
			((Elemental)ch).onZapComplete();
			parent.add( new Beam.LightRay(center(), DungeonTilemap.raisedTileCenterToWorld(cell)));
			Sample.INSTANCE.play( Assets.Sounds.RAY );
		}
		
		@Override
		protected int texOffset() {
			return 42;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( SparkParticle.STATIC, 0.06f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFFFFFF85;
		}
	}
	
	public static class Chaos extends ElementalSprite {
		@Override protected Emitter.Factory spellFactory(){ return AMP_RAINBOW; } //Maze Tower (L3·L4)
		@Override protected Emitter.Factory quietFactory(){ return QUIET_RAINBOW; }
		@Override protected float spellInterval(){ return 0.025f; }

		@Override
		public void zap(int cell) {
			//Maze Tower (L3): 다음에 쓸 원소의 탄을 쏜다 (전격은 빛줄기)
			int el = ch instanceof Elemental.ChaosElemental ? ((Elemental.ChaosElemental) ch).mazeNext : 1;
			if (el == 3){
				zap( cell, null );
				((Elemental)ch).onZapComplete();
				parent.add( new Beam.LightRay(center(), DungeonTilemap.raisedTileCenterToWorld(cell)) );
				Sample.INSTANCE.play( Assets.Sounds.RAY );
			} else {
				boltType = el == 1 ? MagicMissile.FIRE : MagicMissile.FROST;
				super.zap( cell );
			}
		}

		@Override
		protected int texOffset() {
			return 56;
		}
		
		@Override
		protected Emitter createEmitter() {
			Emitter emitter = emitter();
			emitter.pour( RainbowParticle.BURST, 0.025f );
			return emitter;
		}
		
		@Override
		public int blood() {
			return 0xFFE3E3E3;
		}
	}
}
