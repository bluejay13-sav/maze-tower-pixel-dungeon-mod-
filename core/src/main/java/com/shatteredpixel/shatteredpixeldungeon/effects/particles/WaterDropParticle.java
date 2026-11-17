/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

//Maze Tower: 젖음 상태에서 몸에서 떨어지는 물방울. (원작 SnowParticle 구조를 따른다)
public class WaterDropParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((WaterDropParticle)emitter.recycle( WaterDropParticle.class )).reset( x, y );
		}
	};

	public WaterDropParticle() {
		super();
		color( 0x66AAFF );
		size( 1.5f );
		acc.set( 0, 40 );
		lifespan = 0.6f;
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		speed.set( Random.Float( -4, 4 ), Random.Float( -2, 4 ) );
		left = lifespan;
	}

	@Override
	public void update() {
		super.update();
		am = left / lifespan;
	}
}
