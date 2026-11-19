/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.effects.RetreatEffects;
import com.watabou.noosa.TextureFilm;

//Maze Tower: 무너진 돌무더기 그림 (core/src/main/assets/sprites/maze_retreat_rubble.png, 16×16 ×3).
//남은 체력이 많으면 0번, 2/3 아래 1번, 1/3 아래 2번 (금이 많이 가고 위 돌이 굴러떨어진 모습).
public class RubbleSprite extends MobSprite {

	private int shown = -1;

	public RubbleSprite(){
		super();
		texture(RetreatEffects.RUBBLE);
		setFrames(0);
	}

	private void setFrames( int frame ){
		TextureFilm frames = new TextureFilm(texture, 16, 16);
		idle = new Animation(1, true);
		idle.frames(frames, frame);
		run = idle.clone();
		attack = idle.clone();
		zap = idle.clone();
		die = new Animation(1, false);
		die.frames(frames, 2);
		shown = frame;
		play(idle, true);
	}

	public void updateCracks(){
		updateCracks(true);
	}

	private void updateCracks( boolean flashIt ){
		if (ch == null) return;
		float hp = ch.HP / (float)ch.HT;
		int frame = hp > 2f/3f ? 0 : (hp > 1f/3f ? 1 : 2);
		if (frame != shown && ch.isAlive()) setFrames(frame);
		if (flashIt) flash();
	}

	@Override
	public void link( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ) {
		super.link(ch);
		shown = -1;
		updateCracks(false);
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == die){
			killAndErase();
			return;
		}
		super.onComplete(anim);
	}
}
