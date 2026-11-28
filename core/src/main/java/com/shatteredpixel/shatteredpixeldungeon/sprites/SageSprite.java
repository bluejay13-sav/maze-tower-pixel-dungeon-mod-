/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

//Maze Tower (M1): 숨어 사는 현자 그림 (사용자 확인, sprites/maze_sage.png 18×20 × 4: 0 가만히, 1 눈 깜빡, 2·3 말함). 오른쪽을 비스듬히 본다.
//은백·연보라·자색 로브, 깊게 눌러쓴 두건 그늘 속에 두 눈만 반짝이고 살빛이 은은하게 비친다. 비스듬한 자색 허리띠, 앞으로 내린 손에 새잎이 든 금 테 유리 등.
//원본 docs/sprites/mobs/m_draft/make_sage_w.ps1
public class SageSprite extends MobSprite {

	public SageSprite() {
		super();

		texture( Assets.Sprites.SAGE );
		TextureFilm frames = new TextureFilm( texture, 18, 20 );

		idle = new Animation( 4, true );
		idle.frames( frames, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 0, 2, 0 );

		run = new Animation( 20, true );
		run.frames( frames, 0 );

		die = new Animation( 20, false );
		die.frames( frames, 0 );

		play( idle );
	}
}
