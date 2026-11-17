/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

//Maze Tower: 마나가 가득 찼을 때 주인공 몸에서 천천히 피어오르는 은은한 푸른 기운
public class ManaWispParticle extends PixelParticle {

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((ManaWispParticle)emitter.recycle( ManaWispParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	public ManaWispParticle() {
		super();
		color( 0x4488FF );
		size( 2 );
	}

	public void reset( float x, float y ) {
		revive();
		this.x = x;
		this.y = y;
		left = lifespan = Random.Float( 0.8f, 1.2f );
		speed.set( Random.Float( -1.5f, 1.5f ), -Random.Float( 6, 10 ) );
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		//은은하게: 최대 투명도 0.5, 나타났다 사라진다
		am = (p > 0.5f ? (1 - p) : p) * 1.0f;
	}
}
