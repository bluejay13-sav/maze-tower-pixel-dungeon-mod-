/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

//Maze Tower: 반월베기 궤적을 따라 흩날리는 은색 가루
public class SilverParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((SilverParticle)emitter.recycle( SilverParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	public SilverParticle() {
		super();
		size( 1.5f );
		acc.set( 0, 10 );
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		//은색과 흰색 사이에서 조금씩 다르게
		color( Random.oneOf( 0xD8DEE6, 0xC0C8D2, 0xF4F6FA ) );
		left = lifespan = Random.Float( 0.35f, 0.6f );
		speed.polar( Random.Float( (float)Math.PI * 2f ), Random.Float( 6, 16 ) );
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		am = p;
		size( 1.5f * p + 0.5f );
	}
}
