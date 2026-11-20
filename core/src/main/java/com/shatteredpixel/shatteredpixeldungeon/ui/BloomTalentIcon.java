/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

//Maze Tower: 2차(개화) 특성 아이콘. talent2_icons.png (16×16, 가로 8칸, 31번은 분홍 꽃잎)
public class BloomTalentIcon extends Image {

	private static TextureFilm film;
	private static final int SIZE = 16;

	public BloomTalentIcon( BloomTalent talent ){
		this(talent.icon());
	}

	public BloomTalentIcon( int icon ){
		super( Assets.Interfaces.MAZE_TALENT2_ICONS );
		if (film == null) film = new TextureFilm(texture, SIZE, SIZE);
		frame(film.get(icon));
	}

	//분홍 꽃잎 (남은 각성)
	public static BloomTalentIcon petal(){
		return new BloomTalentIcon(BloomTalent.PETAL_ICON);
	}
}
