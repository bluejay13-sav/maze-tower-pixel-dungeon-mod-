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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

public class BuffIcon extends Image {

	private static TextureFilm smallFilm;
	private static final int SML_SIZE = 7;

	private static TextureFilm largeFilm;
	private static final int LRG_SIZE = 16;

	private final boolean large;

	public BuffIcon(Buff buff, boolean large){
		super( large ? Assets.Interfaces.BUFFS_LARGE : Assets.Interfaces.BUFFS_SMALL );
		this.large = large;
		refresh(buff);
	}

	public BuffIcon(int icon, boolean large){
		super( large ? Assets.Interfaces.BUFFS_LARGE : Assets.Interfaces.BUFFS_SMALL );
		this.large = large;
		refresh(icon);
	}

	public void refresh(Buff buff){
		refresh(buff.icon());
		buff.tintIcon(this);
	}

	//Maze Tower: 2차 특성 상태 아이콘 (작은 것 buffs2_small.png, 큰 것은 그 특성 아이콘)
	private static TextureFilm mazeLargeFilm;

	public void refresh(int icon){
		if (icon >= BuffIndicator.MAZE2){
			int i = icon - BuffIndicator.MAZE2;
			if (large){
				texture(Assets.Interfaces.MAZE_TALENT2_ICONS);
				if (mazeLargeFilm == null) mazeLargeFilm = new TextureFilm(texture, LRG_SIZE, LRG_SIZE);
				frame(mazeLargeFilm.get(BuffIndicator.B2_LARGE[i]));
			} else {
				texture(Assets.Interfaces.MAZE_BUFFS2_SMALL);
				frame(i * 8, 0, SML_SIZE, SML_SIZE);
			}
			return;
		}
		//원작 아이콘으로 돌아올 때를 위해 원작 그림으로 되돌린다
		texture(large ? Assets.Interfaces.BUFFS_LARGE : Assets.Interfaces.BUFFS_SMALL);
		if (large){
			if (largeFilm == null) largeFilm = new TextureFilm(texture, LRG_SIZE, LRG_SIZE);
			frame(largeFilm.get(icon));
		} else {
			if (smallFilm == null ) smallFilm = new TextureFilm(texture, SML_SIZE, SML_SIZE);
			frame(smallFilm.get(icon));
		}
	}

}
