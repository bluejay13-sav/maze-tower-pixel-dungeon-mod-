/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

//Maze Tower (H4): 넋 나방 떼 그림 (16×16 × 15, 칸 배치는 원작 파리떼와 같음: 0~5 가만히, 6~9 공격, 10~14 사라짐)
public class SoulMothSprite extends MobSprite {

	public SoulMothSprite() {
		super();

		texture( Assets.Sprites.SOUL_MOTH );

		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 15, true );
		idle.frames( frames, 0, 1, 2, 3, 4, 5 );

		run = new Animation( 15, true );
		run.frames( frames, 0, 1, 2, 3, 4, 5 );

		attack = new Animation( 20, false );
		attack.frames( frames, 6, 7, 8, 9 );

		die = new Animation( 15, false );
		die.frames( frames, 10, 11, 12, 13, 14 );

		play( idle );
	}

	@Override
	public int blood() {
		return 0xFF8FD0FF; //푸른 빛가루
	}
}
