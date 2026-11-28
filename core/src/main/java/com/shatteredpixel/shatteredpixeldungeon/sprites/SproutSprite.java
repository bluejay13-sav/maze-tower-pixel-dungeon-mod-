/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.watabou.noosa.TextureFilm;

//Maze Tower (F6): 기생 싹 그림 (sprites/maze_sprout.png, 16×16 × 7: 0 가만히, 1 떨림, 2·3 뿌리 휘두름, 4·5·6 시듦)
//원본·만든 스크립트 docs/sprites/mobs/f_draft/sprout.png·make_sprout.ps1
public class SproutSprite extends MobSprite {

	//묶을 때 휘두르는 동작은 평타 동작(attack)과 따로 둔다. 같은 동작을 쓰면 묶은 직후 싹의 평타가 끝났다는 신호를 삼켜 게임이 멈춘다.
	private Animation lashAnim;

	public SproutSprite() {
		super();

		texture( "sprites/maze_sprout.png" );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		//가만히 있으면 바닥 같지만 가끔 살짝 떨린다
		idle = new Animation( 3, true );
		idle.frames( frames, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0 );

		run = new Animation( 3, true );
		run.frames( frames, 0, 0, 0, 1 );

		attack = new Animation( 12, false );
		attack.frames( frames, 2, 3, 2, 0 );

		lashAnim = new Animation( 12, false );
		lashAnim.frames( frames, 2, 3, 2, 0 );

		die = new Animation( 8, false );
		die.frames( frames, 4, 5, 6 );

		play( idle );
	}

	//자기 턴이 아닐 때 뿌리를 휘두르는 동작만 (평타 처리를 부르지 않는다)
	public void lash( int cell ){
		if (curAnim == attack) return; //평타 중이면 그 동작을 끊지 않는다
		turnTo( ch.pos, cell );
		play( lashAnim );
	}

	@Override
	public synchronized void onComplete( Animation anim ) {
		if (anim == lashAnim){
			idle();
			return;
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFF7FA35A;
	}
}
