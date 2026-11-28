/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MirrorPortrait;
import com.watabou.noosa.TextureFilm;

//Maze Tower (L6): 거울 초상 그림 (sprites/maze_mirror.png 14×16 × 13). 0·1 가만히(흐릿하게 일렁임), 2~5 떠서 움직임,
//6~8 따라 쓰기(빈 얼굴의 액자가 번쩍), 9~12 쓰러짐(액자 조각이 떨어지고 흩어짐). 원본 docs/sprites/mobs/l_draft/make_l5l6.ps1
public class MirrorPortraitSprite extends MobSprite {

	public MirrorPortraitSprite() {
		super();

		texture( Assets.Sprites.MIRROR );

		TextureFilm frames = new TextureFilm( texture, 14, 16 );

		idle = new Animation( 4, true );
		idle.frames( frames, 0, 0, 1, 1 );

		run = new Animation( 8, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 12, false );
		attack.frames( frames, 6, 7, 8, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 9, 10, 11, 12 );

		play( idle );
	}

	@Override
	public void update() {
		super.update();
		//불러온 뒤 남은 따라 쓰기 경고를 다시 그린다
		if (ch instanceof MirrorPortrait && ch.isAlive()){
			MirrorPortrait m = (MirrorPortrait) ch;
			if (m.pendingCell != -1 && m.warnFx == null) m.showWarn();
		}
	}

	@Override
	public int blood() {
		return 0xFFB8B4D0; //흐릿한 잿빛 환영
	}
}
