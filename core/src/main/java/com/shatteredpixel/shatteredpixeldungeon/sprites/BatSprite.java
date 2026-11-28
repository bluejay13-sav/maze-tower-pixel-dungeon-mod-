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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat;
import com.watabou.noosa.TextureFilm;

public class BatSprite extends MobSprite {

	//Maze Tower (J3): 마석 박쥐. 그림 줄 0~3 = 배에 든 마나(빈 배 → 가득 부푼 배)
	private int belly = -1;
	private TextureFilm frames;

	public BatSprite() {
		super();

		texture( Assets.Sprites.BAT );

		frames = new TextureFilm( texture, 15, 15 );

		belly(0);

		play( idle );
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		if (ch instanceof Bat) belly(((Bat) ch).bellyRow());
	}

	//Maze Tower (J3): 배의 줄을 바꾼다. 지금 돌던 동작은 새 줄로 이어서 돈다
	public void belly( int row ){
		if (row == belly) return;
		belly = row;
		Animation oldIdle = idle, oldRun = run;
		int o = row * 8; //한 줄에 8칸 (128 / 15)

		idle = new Animation( 8, true );
		idle.frames( frames, o, o+1 );

		run = new Animation( 12, true );
		run.frames( frames, o, o+1 );

		attack = new Animation( 12, false );
		attack.frames( frames, o+2, o+3, o, o+1 );

		die = new Animation( 12, false );
		die.frames( frames, o+4, o+5, o+6 );

		if (curAnim != null){
			if (curAnim == oldIdle) play( idle );
			else if (curAnim == oldRun) play( run );
		}
	}
}
