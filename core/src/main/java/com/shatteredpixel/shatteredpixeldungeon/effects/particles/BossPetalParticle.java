/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.Random;

//Maze Tower: 전직의 핵을 지닌 보스 몸 주위에 흩날리는 벚꽃잎.
//5층(개화의 핵) 희미하게, 10층(만개의 핵) 조금 선명하게, 20층(결실의 핵) 선명하게.
//몸 주위에 늘 조금씩 흩날리고, 움직이는 동안에는 더 자주 나온다. 보스가 쓰러지면 멈춘다.
public class BossPetalParticle extends PixelParticle {

	public static final int FAINT  = 0;
	public static final int MEDIUM = 1;
	public static final int VIVID  = 2;

	private static final int PALE = 0xFFD6EC;
	private static final int PINK = 0xFF8FCB;

	private int level;
	private float baseAlpha;
	private float swayPhase;

	public static final Emitter.Factory[] FACTORIES = {factory(FAINT), factory(MEDIUM), factory(VIVID)};

	private static Emitter.Factory factory( final int level ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((BossPetalParticle)emitter.recycle( BossPetalParticle.class )).reset( x, y, level );
			}
			@Override
			public boolean lightMode() {
				return false;
			}
		};
	}

	public void reset( float x, float y, int level ){
		this.level = level;
		float life = Random.Float(1.4f, 2.2f);
		int color = ColorMath.interpolate(PALE, PINK, Random.Float());
		float size = (level == VIVID && Random.Int(2) == 0) || (level == MEDIUM && Random.Int(4) == 0) ? 2f : 1f;
		super.reset( x, y, color, size, life );
		baseAlpha = MazeTower.BOSS_PETAL_ALPHA[level];
		swayPhase = Random.Float(0f, 6.28f);
		speed.set( Random.Float(-4f, 4f), Random.Float(2f, 6f) ); //천천히 떨어진다
	}

	@Override
	public void update() {
		super.update();
		if (!alive) return;
		float p = 1f - left / lifespan;
		//살랑살랑 좌우로 흔들리며 나타났다 사라진다
		x += (float)Math.sin(p * 9f + swayPhase) * 0.15f;
		float fade = Math.min(1f, Math.min(p / 0.2f, (1f - p) / 0.3f));
		am = Math.max(0f, fade) * baseAlpha;
	}

	//보스 그림에 붙이는 이미터. 보스가 살아 있고 보일 때만 나오고, 움직이는 동안 더 자주 나온다.
	public static class Petals extends Emitter {

		private final CharSprite sprite;
		private final float baseInterval;

		public Petals( CharSprite sprite, int level ){
			this.sprite = sprite;
			this.baseInterval = MazeTower.BOSS_PETAL_INTERVAL[level];
			autoKill = false;
			pos( sprite );
			pour( FACTORIES[level], baseInterval );
		}

		@Override
		public void update() {
			if (sprite.parent == null || !sprite.alive){
				killAndErase();
				return;
			}
			on = sprite.ch != null && sprite.ch.isAlive() && sprite.visible;
			//움직일 때는 더 자주
			interval = sprite.isMoving ? baseInterval * MazeTower.BOSS_PETAL_MOVE_INTERVAL : baseInterval;
			super.update();
		}
	}
}
