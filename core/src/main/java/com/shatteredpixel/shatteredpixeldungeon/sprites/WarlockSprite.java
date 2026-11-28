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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Warlock;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

public class WarlockSprite extends MobSprite {
	
	public WarlockSprite() {
		super();
		
		texture( Assets.Sprites.WARLOCK );
		
		TextureFilm frames = new TextureFilm( texture, 12, 15 );
		
		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( 15, true );
		run.frames( frames, 0, 2, 3, 4 );
		
		attack = new Animation( 12, false );
		attack.frames( frames, 0, 5, 6 );
		
		zap = attack.clone();
		
		die = new Animation( 15, false );
		die.frames( frames, 0, 7, 8, 8, 9, 10 );
		
		play( idle );
	}
	
	public void zap( int cell ) {

		super.zap( cell );

		MagicMissile.boltFromChar( parent,
				MagicMissile.SHADOW,
				this,
				cell,
				new Callback() {
					@Override
					public void call() {
						((Warlock)ch).onZapComplete();
					}
				} );
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}
	
	//Maze Tower (L4): 증폭하는 주문과 잇는 푸른 선 (공명 수정의 실과 같은 그림: 아주 옅은 실 위로 빛 알갱이가 마도사에서 주문 쪽으로 흐름)
	private com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ResonanceCrystal.ThreadLine ampLine;
	private int ampId = -1;

	@Override
	public void update() {
		super.update();
		if (!(ch instanceof Warlock)) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental e = ((Warlock) ch).mazeAmpElemental();
		boolean ok = ch.isAlive() && e != null && e.isAlive() && e.sprite != null && e.sprite.parent != null && e.mazeAmpBy == ch.id();
		if (!ok || (ampLine != null && ampId != e.id())){
			if (ampLine != null){ ampLine.killAndErase(); ampLine = null; ampId = -1; }
		}
		if (ok && (ampLine == null || ampLine.parent == null)){
			ampLine = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ResonanceCrystal.ThreadLine( this, e.sprite );
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupEffect( ampLine );
			ampId = e.id();
		}
	}

	@Override
	public void kill() {
		if (ampLine != null){ ampLine.killAndErase(); ampLine = null; }
		super.kill();
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}
}
