/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;

//Maze Tower: 만개의 핵이 바닥(또는 가방)에 있을 때, 벚꽃잎이 나선을 따라 나무를 휘감으며 올라간다.
//길: 아래 앞 → 오른쪽으로 올라감 → 나무 뒤(흐리게) → 왼쪽에서 나와 → 위쪽 앞. 나무 꼭대기보다 높이 가지 않는다.
//먼저 나간(멀리 간) 꽃잎일수록 밝은 분홍·흰빛이고, 막 나온 꽃잎(꼬리)은 흐린 연보라. 마법 입자가 조금 섞인다.
//좌표는 16×16 그림 기준 (나무 가운데 x 7.5, 밑동 y 13.5, 꼭대기 y 3).
public class BloomPetalParticle extends PixelParticle {

	private static final float CENTER_X = 8f;
	private static final float BOTTOM_Y = 13.5f;
	private static final float TOP_Y    = 3.5f;

	private static final int TAIL  = 0xB9A6E0; //흐린 연보라
	private static final int PINK  = 0xFF9FD6;
	private static final int HEAD  = 0xFFE8F4; //밝은 분홍·흰빛
	private static final int[] MAGIC = {0xB07CFF, 0x8FD8FF, 0xFFD86B}; //보라·하늘·금빛

	private float ox, oy;        //그림 왼쪽 위 (입자를 낼 때의 위치)
	private float radiusScale;   //1이면 바깥 나선, 작으면 나무 가운데를 지나간다
	private float phase;         //마법 입자는 나선의 다른 자리에서 시작
	private boolean magic;
	private int magicColor;

	//만개의 핵 그림 위에 붙일 이미터 (Item.emitter()에서 돌려준다)
	public static Emitter emitter(){
		Emitter e = new Emitter();
		e.pos(0, 0, 0, 0);
		e.fillTarget = false; //그림의 왼쪽 위를 기준으로 낸다
		e.pour(FACTORY, 0.32f);  //듬성듬성
		return e;
	}

	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((BloomPetalParticle)emitter.recycle( BloomPetalParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return false;
		}
	};

	public void reset( float x, float y ){
		magic = Random.Int(5) == 0;
		float life = magic ? Random.Float(1.0f, 1.6f) : Random.Float(2.4f, 2.9f);
		super.reset( x, y, TAIL, magic ? 1f : (Random.Int(3) == 0 ? 1.5f : 1f), life );
		ox = x;
		oy = y;
		radiusScale = Random.Int(4) == 0 ? Random.Float(0.25f, 0.5f) : Random.Float(0.85f, 1.05f);
		phase = magic ? Random.Float(0.1f, 0.6f) : 0f;
		magicColor = MAGIC[Random.Int(MAGIC.length)];
		place();
	}

	private void place(){
		float t = Math.min(1f, phase + (1f - left / lifespan) * (1f - phase)); //0 → 1
		double theta = t * Math.PI * 2;                              //한 바퀴
		float r = (3.5f + 3.2f * (float)Math.sin(Math.PI * t)) * radiusScale; //밑동·꼭대기는 좁고 가운데는 넓게
		float front = (float)Math.cos(theta);                        //+ 앞, - 뒤

		x = ox + CENTER_X + r * (float)Math.sin(theta);
		y = oy + BOTTOM_Y - t * (BOTTOM_Y - TOP_Y) + front * 0.8f;  //앞쪽이 살짝 아래로 (원근)

		//색: 꼬리(막 나옴) 연보라 → 분홍 → 머리 쪽 흰빛
		if (magic){
			color(magicColor);
		} else if (t < 0.5f){
			color(ColorMath.interpolate(TAIL, PINK, t * 2f));
		} else {
			color(ColorMath.interpolate(PINK, HEAD, (t - 0.5f) * 2f));
		}

		//나타났다 사라지기, 나무 뒤에서는 흐리게
		float lifeP = 1f - left / lifespan;
		float fade = Math.min(1f, Math.min(lifeP / 0.12f, (1f - lifeP) / 0.18f));
		float depth = front < -0.2f ? 0.3f : 1f;
		am = Math.max(0f, fade) * depth * (magic ? 0.8f : 0.9f);
	}

	@Override
	public void update() {
		super.update();
		if (alive) place();
	}
}
