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

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WebParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

public class Web extends Blob {

	{
		//acts before the hero, to ensure terrain is adjusted correctly
		actPriority = HERO_PRIO+1;
	}
	
	@Override
	protected void evolve() {

		int cell;

		Level l = Dungeon.level;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				off[cell] = cur[cell] > 0 ? cur[cell] - 1 : 0;

				volume += off[cell];

				if (off[cell] == 0 && cur[cell] > 0){
					cellsToFlagUpdate.add(cell);
				}
			}
		}
	}

	@Override
	public void seed(Level level, int cell, int amount) {
		super.seed(level, cell, amount);
		level.updateCellFlags(cell);
	}

	//affects characters as they step on it. See Level.OccupyCell and Level.PressCell
	public static void affectChar( Char ch ){
		Buff.prolong( ch, Roots.class, Roots.DURATION );
		//Maze Tower (J6): 수정 실. 주인공이 걸려 있는(실에 묶인) 동안은 마나가 차지 않는다 (Mana.turnRegen)
		if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero){
			Buff.affect( ch, CrystalThread.class );
		}
	}

	//Maze Tower (J6): 수정 실에 걸림. 실이 건 속박(Roots)이 풀리면 함께 사라진다
	public static class CrystalThread extends Buff {
		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		public static boolean blocks( Char ch ){
			return ch.buff(CrystalThread.class) != null && ch.buff(Roots.class) != null;
		}

		@Override
		public boolean act() {
			if (target.buff(Roots.class) == null) detach();
			spend( TICK );
			return true;
		}

		@Override
		public int icon() {
			return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.RECHARGING;
		}

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ) {
			icon.hardlight( 0.45f, 0.45f, 0.55f ); //(제안) 광맥의 기운(푸른 충전 아이콘)을 잿빛으로: 마나가 멈춤
		}

		@Override
		public String desc() {
			Roots r = target.buff(Roots.class);
			return Messages.get(this, "desc", r == null ? 0 : (int) r.visualcooldown());
		}
	}
	
	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		
		emitter.pour( WebParticle.FACTORY, 0.25f );
	}

	@Override
	public void clear(int cell) {
		super.clear(cell);
		if (cur == null) return;
		Dungeon.level.updateCellFlags(cell);
	}

	@Override
	public void fullyClear() {
		super.fullyClear();
		Dungeon.level.buildFlagMaps();
	}

	@Override
	public void onBuildFlagMaps(Level l) {
		if (volume > 0){
			for (int i=0; i < l.length(); i++) {
				onUpdateCellFlags(l, i);
			}
		}
	}

	@Override
	public void onUpdateCellFlags(Level l, int cell) {
		if (volume > 0 && cur[cell] > 0) {
			l.solid[cell] = true;
			l.flamable[cell] = true;
			//openSpace will be updated as part of updating flags in Level
		}
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
