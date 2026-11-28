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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.watabou.noosa.TextureFilm;

public class ShieldedSprite extends MobSprite {

	//Maze Tower (J4): 결정 갑옷 광부. 광폭 중에는 마석에서 빛이 새는 넷째 줄(63칸부터)로 그린다
	private static final int BASE_ROW = 21;
	private static final int RAGE_ROW = 63;
	private TextureFilm frames;
	private boolean raging = false;
	
	public ShieldedSprite() {
		super();
		
		texture( Assets.Sprites.BRUTE );
		
		frames = new TextureFilm( texture, 12, 16 );
		
		setRow( BASE_ROW );
		
		play( idle );
	}

	private void setRow( int o ){
		Animation oldIdle = idle, oldRun = run;

		idle = new Animation( 2, true );
		idle.frames( frames, o, o, o, o+1, o, o, o+1, o+1 );
		
		run = new Animation( 12, true );
		run.frames( frames, o+4, o+5, o+6, o+7 );
		
		attack = new Animation( 12, false );
		attack.frames( frames, o+2, o+3 );
		
		die = new Animation( 12, false );
		die.frames( frames, o+8, o+9, o+10 );

		if (curAnim != null){
			if (curAnim == oldIdle) play( idle );
			else if (curAnim == oldRun) play( run );
		}
	}

	@Override
	public void update() {
		super.update();
		boolean r = ch instanceof Brute && ((Brute) ch).mazeRaging();
		if (r != raging){
			raging = r;
			setRow( r ? RAGE_ROW : BASE_ROW );
		}
	}
}
