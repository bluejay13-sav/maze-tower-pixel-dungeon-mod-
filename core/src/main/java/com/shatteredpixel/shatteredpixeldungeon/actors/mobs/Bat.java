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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BatSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class Bat extends Mob {

	{
		spriteClass = BatSprite.class;
		
		HP = HT = 30;
		defenseSkill = 15;
		baseSpeed = 2f;
		
		EXP = 7;
		maxLvl = 15;
		
		flying = true;
		
		loot = PotionOfHealing.class;
		lootChance = 0.1667f; //by default, see lootChance()
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 5, 18 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 16;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	//Maze Tower (J3): 마석 박쥐. 주인공을 물면 마나도 빨아 배에 담는다(최대 J3_BAT_MAX, 저장됨).
	//배에 든 만큼 그림의 배가 푸르게 부풀고, 쓰러지면 그 마나의 절반이 구슬로 주인공에게 돌아간다
	public int storedMana = 0;

	public int bellyRow(){
		return storedMana <= 0 ? 0 : Math.min(3, (storedMana * 3 + MazeTower.J3_BAT_MAX - 1) / MazeTower.J3_BAT_MAX); //최대의 1/3마다 한 단계
	}

	private void updateBelly(){
		if (sprite instanceof BatSprite) ((BatSprite) sprite).belly(bellyRow());
	}

	@Override
	public void die(Object cause) {
		flying = false;
		//Maze Tower (J3): 담아 둔 마나의 일부(J3_BAT_RETURN)를 돌려준다
		int back = (int)(storedMana * MazeTower.J3_BAT_RETURN); //(결정) 절반만 돌려받는다
		if (back > 0 && Dungeon.hero != null && Dungeon.hero.isAlive()){
			Mana m = Mana.get(Dungeon.hero);
			if (m != null){
				m.gain(back);
				if (Dungeon.hero.sprite != null && sprite != null && sprite.visible){
					TalentEffects.manaRefund(Dungeon.hero, this, back);
				}
			}
			storedMana = 0;
		}
		super.die(cause);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		int reg = Math.min( damage - 4, HT - HP );

		if (reg > 0) {
			HP += reg;
			sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(reg), FloatingText.HEALING);
		}

		//Maze Tower (J3): 마나를 빨아 간다
		if (enemy instanceof Hero && enemy.isAlive()){
			Mana m = Mana.get(enemy);
			int drain = m == null ? 0 : Math.min(MazeTower.J3_BAT_DRAIN, Math.min(m.cur(), MazeTower.J3_BAT_MAX - storedMana));
			if (drain > 0){
				m.gain(-drain);
				storedMana += drain;
				if (enemy.sprite != null && enemy.sprite.visible) enemy.sprite.showStatus(0x4488FF, "-" + drain);
				updateBelly();
			}
		}

		return damage;
	}

	@Override
	public String description() {
		String d = super.description();
		if (storedMana > 0) d += "\n\n" + Messages.get(this, "belly", storedMana);
		return d;
	}

	private static final String STORED_MANA = "maze_stored_mana";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STORED_MANA, storedMana );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		storedMana = bundle.getInt( STORED_MANA );
	}
	
	@Override
	public float lootChance(){
		return super.lootChance() * ((7f - Dungeon.LimitedDrops.BAT_HP.count) / 7f);
	}
	
	@Override
	public Item createLoot(){
		Dungeon.LimitedDrops.BAT_HP.count++;
		return super.createLoot();
	}
	
}
