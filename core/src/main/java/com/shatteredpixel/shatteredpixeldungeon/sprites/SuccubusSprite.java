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
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.watabou.noosa.TextureFilm;

public class SuccubusSprite extends MobSprite {
	
	public SuccubusSprite() {
		super();
		
		texture( Assets.Sprites.SUCCUBUS );
		
		TextureFilm frames = new TextureFilm( texture, 12, 15 );
		
		idle = new Animation( 8, true );
		idle.frames( frames, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 2, 2, 2, 1 );
		
		run = new Animation( 15, true );
		run.frames( frames, 3, 4, 5, 6, 7, 8 );
		
		attack = new Animation( 12, false );
		attack.frames( frames, 9, 10, 11 );
		
		die = new Animation( 10, false );
		die.frames( frames, 12, 12, 12, 12 ); //Maze Tower (N2): 변신한 채 쓰러지면 앞 두 칸은 변신 모습으로 주저앉고, 마지막 두 칸에서 변신이 풀린다
		
		play( idle );
	}
	
	//Maze Tower (N2, 사용자): 주인공이 이 시녀에게 매혹되면 다른 외형으로 보이고, 그 뒤로는 매혹이 끝나도 그 모습으로 남는다 (같은 칸 배치):
	//전사·도적에게는 여자 외형(maze_succubus_charm_f.png, 사냥꾼 그림 바탕), 사냥꾼·마법사에게는 남자 외형(maze_succubus_charm_m.png, 도적 그림 바탕).
	//두 그림은 크기가 같아 칸 위치가 그대로 맞는다. 바꾼 뒤 지금 칸을 다시 고른다(그림을 바꾸면 한 칸 전체가 아닌 그림 전체가 잠깐 보이므로).
	private Object charmLook = null; //지금 쓰는 그림 (null = 기본)
	private boolean lookShown = false; //한 번이라도 그렸는지 (저장 파일을 불러온 직후 변신 효과가 나지 않게)

	@Override
	public void update() {
		super.update();
		//(사용자) 변신한 채 쓰러지면 쓰러짐 동작의 앞부분은 변신 모습, 마지막 두 칸에서 변신이 풀려 기본 모습(바닥에 내려앉은 모자와 코트)이 된다 (남자·여자 폼 모두)
		if (ch != null && !ch.isAlive()){
			if (charmLook != null && curAnim == die && curFrame >= die.frames.length - 2) mazeRevert();
			return;
		}
		boolean want = com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_CHARM_LOOK && ch != null && ch.isAlive() && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null;
		if (want){
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm c = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.buff( com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm.class );
			want = c != null && c.object == ch.id();
			//(사용자) 한 번 변신하면 매혹이 끝나도 풀리지 않는다
			if (want && ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus) ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus) ch).mazeTransformed = true;
			if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus) ch).mazeTransformed) want = true;
		}
		//(사용자) 전사·도적이 매혹되면 여자 외형, 사냥꾼·마법사가 매혹되면 남자 외형 (도적 그림 바탕)
		Object tex = null;
		if (want){
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass hc = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.heroClass;
			tex = (hc == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.HUNTRESS || hc == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.MAGE)
					? Assets.Sprites.SUCCUBUS_CHARM_M : Assets.Sprites.SUCCUBUS_CHARM_F;
		}
		if (tex != charmLook){
			//(사용자) 변신할 때 몸이 번쩍이고 앞 모습이 잔상으로 남아 커지며 사라지고, 어두운 기운이 피어오른다
			Object old = charmLook;
			charmLook = tex;
			if (lookShown) mazeShift( old != null ? old : Assets.Sprites.SUCCUBUS ); //불러온 직후 처음 그릴 때는 효과 없이
			texture( tex != null ? tex : Assets.Sprites.SUCCUBUS );
			if (curAnim != null && curAnim.frames != null && curFrame < curAnim.frames.length) frame( curAnim.frames[curFrame] );
		}
		lookShown = true;
	}

	//변신하는 순간: 몸이 한 번 번쩍이고, 앞 모습의 지금 칸이 비친 채 커지며 N2_SHIFT_TIME초 만에 사라지고, 어두운 기운이 피어오른다
	private void mazeShift( Object oldTex ){
		if (parent == null || !visible || curAnim == null || curAnim.frames == null || curFrame >= curAnim.frames.length) return;
		final com.watabou.noosa.Image g = new com.watabou.noosa.Image( oldTex );
		TextureFilm film = new TextureFilm( g.texture, 12, 15 );
		g.frame( film.get( indexOf( curAnim.frames[curFrame] ) ) );
		g.x = x; g.y = y; g.flipHorizontal = flipHorizontal;
		g.origin.set( 6, 15 );
		g.alpha( com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_SHIFT_GHOST );
		parent.add( g );
		parent.add( new com.watabou.noosa.tweeners.AlphaTweener( g, 0, com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_SHIFT_TIME ){
			@Override
			protected void updateValues( float progress ) {
				super.updateValues( progress );
				g.scale.set( 1f + progress * 0.4f );
			}
			@Override
			protected void onComplete() {
				super.onComplete();
				g.killAndErase();
			}
		} );
		emitter().burst( ShadowParticle.UP, 10 );
		emitter().burst( ShadowParticle.CURSE, 6 );
		flash();
	}

	//지금 칸(RectF)이 기본 그림에서 몇 번째 칸인지
	private int indexOf( com.watabou.utils.RectF r ){
		return Math.round( r.left * texture.width / 12f );
	}

	//(사용자) 쓰러지다 변신이 풀리는 순간, 원래 모습(모자와 코트를 입은 서 있는 시녀)의 형체가 반쯤 비쳐 살짝 떠오르며 흐려진다
	private void mazeGhost(){
		if (parent == null || !visible) return;
		final com.watabou.noosa.Image g = new com.watabou.noosa.Image( Assets.Sprites.SUCCUBUS );
		g.frame( new TextureFilm( g.texture, 12, 15 ).get( 0 ) );
		g.x = x; g.y = y; g.flipHorizontal = flipHorizontal;
		g.alpha( com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_REVERT_GHOST );
		parent.add( g );
		parent.add( new com.watabou.noosa.tweeners.AlphaTweener( g, 0, 0.7f ){
			@Override
			protected void updateValues( float progress ) {
				super.updateValues( progress );
				g.y = y - progress * 3;
			}
			@Override
			protected void onComplete() {
				super.onComplete();
				g.killAndErase();
			}
		} );
	}

	//(사용자) 쓰러진 모자와 코트가 땅에 오래 남지 않게 빨리 사라진다 (원작 3초)
	@Override
	protected float fadeTime() {
		return com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_FADE_TIME;
	}

	private void mazeRevert(){
		charmLook = null;
		texture( Assets.Sprites.SUCCUBUS );
		if (curAnim != null && curAnim.frames != null && curFrame < curAnim.frames.length) frame( curAnim.frames[curFrame] );
		emitter().burst( ShadowParticle.UP, 10 );
		if (ch != null && !ch.isAlive()) mazeGhost();
	}

	//쓰러짐이 끝났는데 아직 변신 모습이면 풀어 준다
	@Override
	public void onComplete( Animation anim ) {
		if (anim == die && charmLook != null) mazeRevert(); //혹시 남아 있으면
		super.onComplete( anim );
	}

	@Override
	public void die() {
		super.die();
		emitter().burst( Speck.factory( Speck.HEART ), 6 );
		emitter().burst( ShadowParticle.UP, 8 );
	}
}
