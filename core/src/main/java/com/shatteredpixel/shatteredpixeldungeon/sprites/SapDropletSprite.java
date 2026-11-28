/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.watabou.noosa.TextureFilm;

//Maze Tower (G2): 5층 수액 방울 그림 (sprites/maze_sapdrop.png, 16×16 × 11)
//0~2 가만히(출렁), 3·4 떨림(보스가 향하는 방울), 5~7 터짐, 8~10 탐. 원본·만든 스크립트 docs/sprites/mobs/f_draft/sapdrop.png·make_sapdrop.ps1
public class SapDropletSprite extends MobSprite {

	private Animation twitch, pop, burn;
	private boolean twitching = false;

	public SapDropletSprite() {
		super();

		texture( "sprites/maze_sapdrop.png" );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 4, true );
		idle.frames( frames, 0, 1, 0, 2 );

		run = idle.clone();
		attack = idle.clone();

		twitch = new Animation( 10, true );
		twitch.frames( frames, 3, 0, 4, 0 );

		pop = new Animation( 12, false );
		pop.frames( frames, 5, 6, 7 );

		burn = new Animation( 8, false );
		burn.frames( frames, 8, 9, 10 );

		die = pop;
		play( idle );
	}

	//보스가 이 방울로 향한다 (예고)
	public void twitch( boolean on ){
		twitching = on;
		if (curAnim == die) return;
		play( on ? twitch : idle );
	}

	@Override
	public void idle() {
		if (twitching) play( twitch );
		else super.idle();
	}

	//불에 타서 사라질 때
	public void burnOnDeath(){
		die = burn;
	}

	//터지거나 탄 뒤 남은 그림은 짧게 사라진다 (원작 몹은 3초)
	@Override
	protected float fadeTime() {
		return 0.25f;
	}

	@Override
	public int blood() {
		return 0xFFE0952A;
	}
}
