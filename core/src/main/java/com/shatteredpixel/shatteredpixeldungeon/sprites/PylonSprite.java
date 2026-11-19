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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;

public class PylonSprite extends MobSprite {

	private Animation activeIdle;

	public PylonSprite() {
		super();

		perspectiveRaise = 5/16f; //1 pixel less
		renderShadow = false;

		texture( Assets.Sprites.PYLON );

		TextureFilm frames = new TextureFilm( texture, 10, 20 );

		idle = new Animation( 1, false );
		idle.frames( frames, 0 );

		activeIdle = new Animation( 1, false );
		activeIdle.frames( frames, 1 );

		run = idle.clone();

		attack = idle.clone();

		die = new Animation( 1, false );
		die.frames( frames, 2 );

		//Maze Tower: 수정 거인 보스방의 수정 기둥 (빛나는 상태는 밝게)
		if (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GolemAbilities.crystal()){
			texture("sprites/maze_p16_pillar.png");
			TextureFilm f = new TextureFilm( texture, 12, 22 );
			idle = new Animation( 1, false );
			idle.frames( f, 0 );
			activeIdle = idle.clone();
			run = idle.clone();
			attack = idle.clone();
			die = new Animation( 20, false ); //부서지면 빠르게 사라진다
			die.frames( f, 0 );
		}

		play( idle );
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		if (ch instanceof Pylon && ch.alignment == Char.Alignment.ENEMY){
			activate();
		}
		renderShadow = false;
	}

	@Override
	public void place(int cell) {
		if (parent != null) parent.bringToFront(this);
		super.place(cell);
	}

	public void activate(){
		idle = activeIdle.clone();
		idle();
	}

	@Override
	public void play(Animation anim) {
		if (anim == die){
			turnTo(ch.pos, ch.pos+1); //always face right to merge with custom tiles
			emitter().burst(BlastParticle.FACTORY, 20);
			Sample.INSTANCE.play(Assets.Sounds.BLAST);
		}
		super.play(anim);
	}

	//Maze Tower: 수정 기둥은 부서지면 빠르게 사라진다
	@Override
	protected float fadeTime() {
		if (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GolemAbilities.crystal()) return com.shatteredpixel.shatteredpixeldungeon.MazeTower.PROP_FADE_TIME;
		return super.fadeTime();
	}

	@Override
	public void onComplete(Animation anim) {
		if (anim == attack){
			flash();
		}
		super.onComplete(anim);
	}

	@Override
	public int blood() {
		return 0xFFFFFF88;
	}
}
