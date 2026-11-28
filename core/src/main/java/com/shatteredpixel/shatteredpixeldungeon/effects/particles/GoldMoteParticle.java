/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

//Maze Tower (사용자): 결실의 핵이 바닥(또는 가방)에 있을 때, 열매 둘레에서 황금빛 알갱이가 드문드문 천천히 피어오른다.
//좌표는 16×16 그림 기준 (열매는 x 3~12, y 6~14).
public class GoldMoteParticle extends PixelParticle {

	private static final int[] GOLD = {0xF0DC8E, 0xFFEBA8, 0xE6C866};

	private float ox, oy, sx, sy, rise, sway, scl;

	public static Emitter emitter(){
		Emitter e = new ItemFxEmitter();
		e.pos(0, 0, 0, 0);
		e.fillTarget = false; //그림의 왼쪽 위를 기준으로 낸다
		e.pour(FACTORY, MazeTower.FRUIT_MOTE_INTERVAL);
		return e;
	}

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((GoldMoteParticle)emitter.recycle( GoldMoteParticle.class )).reset( x, y, ItemFxEmitter.scaleOf(emitter) );
		}
		@Override
		public boolean lightMode() {
			return false;
		}
	};

	public void reset( float x, float y, float scale ){
		super.reset( x, y, GOLD[Random.Int(GOLD.length)], scale, Random.Float(1.6f, 2.4f) );
		ox = x;
		oy = y;
		scl = scale;
		//열매 가장자리 둘레(안쪽 조금 포함)에서 나온다
		float a = Random.Float((float)Math.PI * 2);
		sx = 7.9f + (float)Math.cos(a) * Random.Float(3.5f, 6.0f);
		sy = 10.3f + (float)Math.sin(a) * Random.Float(3.0f, 4.8f);
		rise = Random.Float(3f, 5f);
		sway = Random.Float(-0.8f, 0.8f);
		place();
	}

	private void place(){
		float t = 1f - left / lifespan; //0 → 1
		x = ox + scl * (sx + sway * (float)Math.sin(t * Math.PI));
		y = oy + scl * (sy - rise * t);
		//나타났다 사라지기 (은은하게)
		am = Math.max(0f, Math.min(1f, Math.min(t / 0.25f, (1f - t) / 0.35f))) * 0.85f;
	}

	@Override
	public void update() {
		super.update();
		if (alive) place();
	}
}
