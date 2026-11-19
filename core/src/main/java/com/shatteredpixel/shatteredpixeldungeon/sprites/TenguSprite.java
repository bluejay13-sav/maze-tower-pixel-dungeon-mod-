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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

public class TenguSprite extends MobSprite {
	
	public TenguSprite() {
		super();
		
		texture( Assets.Sprites.TENGU );
		
		TextureFilm frames = new TextureFilm( texture, 14, 16 );
		
		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1 );
		
		run = new Animation( 15, false );
		run.frames( frames, 2, 3, 4, 5, 0 );
		
		attack = new Animation( 15, false );
		attack.frames( frames, 6, 7, 7, 0 );
		
		zap = attack.clone();
		
		die = new Animation( 8, false );
		die.frames( frames, 8, 9, 10, 10, 10, 10, 10, 10 );
		//Maze Tower: 그림자 간수 그림 (원작처럼 이동 동작은 반복하지 않는다: 순간이동이 동작 끝을 기다림)
		if (com.shatteredpixel.shatteredpixeldungeon.MazeBosses.ENABLED){
			TextureFilm f = MazeBossSprites.apply(this, MazeBossSprites.Look.WARDEN);
			run = MazeBossSprites.anim(f, 15, false, 2, 3, 4, 5, 0);
			//공격 마지막 칸(8번)은 하얗게 빛나는 그림이라 쓰지 않는다
			attack = MazeBossSprites.anim(f, 15, false, 6, 7, 7, 0);
			zap = attack.clone();
		}
		
		play( run );
		isMoving = true;
	}

	@Override
	public void play(Animation anim) {
		if (isMoving && anim != run){
			synchronized (this){
				isMoving = false;
				notifyAll();
			}
		}
		super.play(anim);
	}

	@Override
	public void move( int from, int to ) {
		
		place( to );
		
		play( run );
		turnTo( from , to );

		isMoving = true;

		if (Dungeon.level.water[to]) {
			GameScene.ripple( to );
		}

	}

	@Override
	public void update() {
		if (paused && !curAnim.looped){
			updateAnimation();
		}
		super.update();
	}

	@Override
	public void attack( int cell ) {
		if (!Dungeon.level.adjacent( cell, ch.pos )) {

			((MissileSprite)parent.recycle( MissileSprite.class )).
				reset( this, cell, new TenguShuriken(), new Callback() {
					@Override
					public void call() {
						//Maze Tower: 사슬 맞음
						if (com.shatteredpixel.shatteredpixeldungeon.MazeBosses.ENABLED){
							com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.atCell(
									com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.CHAIN_HIT, cell, 15, false);
						}
						ch.onAttackComplete();
					}
				} );
			
			zap( ch.pos );
			
		} else {
			
			super.attack( cell );
			
		}
	}
	
	@Override
	public void onComplete( Animation anim ) {
		if (anim == run) {
			synchronized (this){
				isMoving = false;
				idle();

				notifyAll();
			}
		} else {
			super.onComplete( anim );
		}
	}
	
	public static class TenguShuriken extends Item {
		{
			image = ItemSpriteSheet.SHURIKEN;
			if (com.shatteredpixel.shatteredpixeldungeon.MazeBosses.ENABLED) image = MazeItemSprites.WARDEN_CHAIN; //Maze Tower: 사슬
		}
	}
}
