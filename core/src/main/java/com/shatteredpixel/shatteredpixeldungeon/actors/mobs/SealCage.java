/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower (I1): 10층 2단계 대봉인실 가장자리의 부적 철창. 지나갈 수 없고 부서지지 않는다.
//그림은 예전 간수전의 부적 철창(SoulCage) 그대로. 철창 앞 바닥에 사슬 고리(ChainRing)가 박혀 있다.
//open: 1단계에서 풀어 준 영혼의 철창은 열린 채로 나타난다 (I6에서 그림을 다르게)
public class SealCage extends SoulCage {

	{
		spriteClass = CageSprite16.class;
	}

	public boolean open = false;

	//칸 위로 솟은 키 큰 새장 모양 (16×22 × 2: 0 영혼이 갇힘, 1 열림·빈 철창). 그림 sprites/maze_sealcage.png
	public static class CageSprite16 extends com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite {
		private com.watabou.noosa.TextureFilm film;
		public CageSprite16(){
			super();
			texture( com.shatteredpixel.shatteredpixeldungeon.Assets.Sprites.SEAL_CAGE );
			film = new com.watabou.noosa.TextureFilm( texture, 16, 22 );
			setFrame( false );
		}
		public void setFrame( boolean open ){
			idle = new Animation( 1, true );
			idle.frames( film, open ? 1 : 0 );
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation( 20, false );
			die.frames( film, open ? 1 : 0 );
			play( idle );
		}
		@Override
		public void link( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ) {
			super.link(ch);
			if (ch instanceof SealCage && ((SealCage) ch).open) setFrame( true );
		}
	}

	//(I7) 철창이 비었다: 선고를 버텨 끌려 나온 영혼이 사라졌거나, 고리를 끊어 영혼이 풀려났다. 다시 선고에 쓰이지 않는다
	public void empty(){
		open = true;
		if (sprite instanceof CageSprite16) ((CageSprite16) sprite).setFrame(true);
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return true;
	}

	@Override
	public String description() {
		return Messages.get(this, open ? "desc_open" : "desc");
	}

	private static final String OPEN = "open";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OPEN, open);
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle(bundle);
		open = bundle.getBoolean(OPEN);
	}
}
