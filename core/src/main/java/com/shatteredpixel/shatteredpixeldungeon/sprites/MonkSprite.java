/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk;
import com.watabou.noosa.TextureFilm;

//Maze Tower (L2): 왕실 근위 그림 (원작 수도승 자리를 새로 그림, sprites/maze_monk.png 18x18 x 15, 줄 0 왕실 근위 / 줄 1 근위대장).
//칸: 0·1 가만히(검을 다리 옆에 곧게), 2~5 걷기, 6 치켜듦, 7 베기, 8·9 집중 자세(검을 얼굴 앞에 곧게 세움, 9는 칼끝 빛),
//10 받아쳐 찌르기(반격), 11 무방비(검이 튕겨 벌어진 자세), 12~14 쓰러짐. 원본 docs/sprites/mobs/l_draft/make_l1l2.ps1.
//집중 자세·무방비 중에는 가만히 있는 모습이 그 자세로 바뀐다(update).
public class MonkSprite extends MobSprite {

	protected Animation baseIdle, stance, counter, unguarded;

	public MonkSprite() {
		this( 0 );
	}

	protected MonkSprite( int row ) {
		this( Assets.Sprites.MONK, row );
	}

	//Maze Tower (펫 마지막 근위): 같은 칸 배치의 다른 그림 (sprites/maze_pet_guard.png)
	protected MonkSprite( String tex, int row ) {
		super();

		texture( tex );

		TextureFilm frames = new TextureFilm( texture, 18, 18 );
		int o = row * 15;

		idle = new Animation( 3, true );
		idle.frames( frames, o, o, o + 1, o + 1 );
		baseIdle = idle;

		run = new Animation( 10, true );
		run.frames( frames, o + 2, o + 3, o + 4, o + 5 );

		//원작 수도승처럼 공격이 빠르다 (공격 시간이 절반)
		attack = new Animation( 15, false );
		attack.frames( frames, o + 6, o + 7, o + 7, o );

		stance = new Animation( 3, true );
		stance.frames( frames, o + 8, o + 8, o + 9 );

		counter = new Animation( 12, false );
		counter.frames( frames, o + 10, o + 10, o + 10 );

		unguarded = new Animation( 1, true );
		unguarded.frames( frames, o + 11 );

		die = new Animation( 10, false );
		die.frames( frames, o + 11, o + 12, o + 13, o + 14 );

		play( idle );
	}

	//받아쳐 찌르기 (공격 처리와 상관없는 동작만, 끝나면 가만히)
	public void counter( int cell ){
		turnTo( ch.pos, cell );
		play( counter );
	}

	@Override
	public void update() {
		super.update();
		if (ch instanceof Monk && ch.isAlive()){
			Animation want = ch.buff(Monk.Unguarded.class) != null ? unguarded
					: ch.buff(Monk.Focus.class) != null ? stance : baseIdle;
			if (idle != want){
				boolean idling = curAnim == idle;
				idle = want;
				if (idling) play( idle );
			}
		}
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == counter){
			idle();
			return;
		}
		super.onComplete( anim );
	}
}
