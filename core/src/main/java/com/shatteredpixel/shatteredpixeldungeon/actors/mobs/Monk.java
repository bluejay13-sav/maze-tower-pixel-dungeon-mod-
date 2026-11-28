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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MonkSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Monk extends Mob {
	
	{
		spriteClass = MonkSprite.class;
		
		HP = HT = 70;
		defenseSkill = 30;
		
		EXP = 11;
		maxLvl = 21;
		
		loot = Food.class;
		lootChance = 0.083f;

		properties.add(Property.UNDEAD);
	}
	
	@Override
	public int damageRoll() {
		return mazeFocusDamage( Random.NormalIntRange( 12, 25 ) ); //Maze Tower (L2)
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 30;
	}
	
	@Override
	public float attackDelay() {
		return super.attackDelay()*0.5f;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}
	
	@Override
	public void rollToDropLoot() {
		Imp.Quest.oldProcess( this );
		
		super.rollToDropLoot();
	}
	
	protected float focusCooldown = 0;
	
	@Override
	protected boolean act() {
		boolean result = super.act();
		if (buff(Focus.class) == null && state == HUNTING && focusCooldown <= 0 && buff(Unguarded.class) == null) { //Maze Tower (L2): 무방비 동안은 자세를 다시 잡지 않는다
			Buff.affect( this, Focus.class );
			mazeFocusLeft = com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_FOCUS_TURNS; //Maze Tower (L2): 자세는 2턴 뒤 스스로 풀린다
		}
		return result;
	}
	
	@Override
	protected void spend( float time ) {
		focusCooldown -= time;
		//Maze Tower (L2, 사용자): 집중 자세는 2턴이 지나면 스스로 풀리고, 그 뒤 한 박자 무방비 (반격은 없음)
		Focus f = buff(Focus.class);
		if (f != null && com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_COUNTER){
			mazeFocusLeft -= time;
			if (mazeFocusLeft <= 0){
				f.detach();
				focusCooldown = Random.NormalFloat( 6, 7 );
				Buff.affect( this, Unguarded.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_UNGUARD_TURNS );
			}
		}
		super.spend( time );
	}
	
	@Override
	public void move( int step, boolean travelling) {
		// moving reduces cooldown by an additional 0.67, giving a total reduction of 1.67f.
		// basically monks will become focused notably faster if you kite them.
		if (travelling) focusCooldown -= 0.67f;
		super.move( step, travelling);
	}
	
	@Override
	public int defenseSkill( Char enemy ) {
		mazeLastAttacker = enemy; //Maze Tower (L2): 흘렸을 때 받아칠 상대
		if (buff(Unguarded.class) != null) return 0; //Maze Tower (L2): 무방비 = 반드시 맞음
		if (buff(Focus.class) != null && paralysed == 0 && state != SLEEPING){
			return INFINITE_EVASION;
		}
		return super.defenseSkill( enemy );
	}
	
	@Override
	public String defenseVerb() {
		Focus f = buff(Focus.class);
		if (f == null) {
			return super.defenseVerb();
		} else {
			f.detach();
			if (sprite != null && sprite.visible) {
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1, Random.Float(0.96f, 1.05f));
			}
			focusCooldown = Random.NormalFloat( 6, 7 );
			mazeAfterParry(); //Maze Tower (L2): 받아치고, 그 뒤 한 박자 무방비
			return Messages.get(this, "parried");
		}
	}
	
	//Maze Tower (L2): 집중 자세로 흘린 공격을 곧바로 받아치고(옆 칸의 상대만, 평타 판정, 시간을 쓰지 않음), 자세가 풀린 뒤 한 박자 무방비.
	private Char mazeLastAttacker;
	public float mazeFocusLeft = 0; //Maze Tower (L2): 집중 자세가 스스로 풀리기까지 남은 시간 (저장됨)

	//Maze Tower (L2, 사용자): 집중 자세 동안에도 때리지만 피해가 약하다 (L2_FOCUS_DMG)
	protected int mazeFocusDamage( int dmg ){
		if (com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_COUNTER && buff(Focus.class) != null){
			return Math.round(dmg * com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_FOCUS_DMG);
		}
		return dmg;
	}

	private void mazeAfterParry(){
		if (!com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_COUNTER) return;
		Buff.affect( this, Unguarded.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_UNGUARD_TURNS );
		final Char foe = mazeLastAttacker;
		if (foe == null || !Dungeon.level.adjacent( pos, foe.pos )) return;
		final Monk self = this;
		//상대의 공격 처리가 끝난 바로 다음에 받아친다 (공격 처리 안에서 바로 치지 않게)
		Actor.add( new Actor() {
			{ actPriority = VFX_PRIO; }
			@Override
			protected boolean act() {
				Actor.remove( this );
				if (self.isAlive() && foe.isAlive() && self.paralysed == 0 && self.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class) == null
						&& Dungeon.level.adjacent( self.pos, foe.pos )){
					if (self.sprite instanceof MonkSprite) ((MonkSprite) self.sprite).counter( foe.pos );
					if (self.sprite != null && self.sprite.visible) self.sprite.showStatus( com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE, Messages.get(Monk.class, "counter") );
					self.attack( foe );
				}
				return true;
			}
		} );
	}

	//Maze Tower (L2): 자세가 풀린 한 박자 동안 무방비 (회피 0 = 반드시 맞음, 그림은 검이 튕겨 벌어진 자세)
	public static class Unguarded extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {
		{ type = buffType.NEGATIVE; announced = true; }
		@Override public int icon() { return BuffIndicator.VULNERABLE; }
		@Override public float iconFadePercent() { return Math.max(0, (com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_UNGUARD_TURNS - visualcooldown()) / com.shatteredpixel.shatteredpixeldungeon.MazeTower.L2_UNGUARD_TURNS); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}

	private static String FOCUS_COOLDOWN = "focus_cooldown";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FOCUS_COOLDOWN, focusCooldown );
		bundle.put( "maze_focus_left", mazeFocusLeft ); //Maze Tower (L2)
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		focusCooldown = bundle.getInt( FOCUS_COOLDOWN );
		mazeFocusLeft = bundle.getFloat( "maze_focus_left" ); //Maze Tower (L2)
	}
	
	public static class Focus extends Buff {
		
		{
			type = buffType.POSITIVE;
			announced = true;
		}
		
		@Override
		public int icon() {
			return BuffIndicator.MIND_VISION;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.25f, 1.5f, 1f);
		}

		//Maze Tower (L2): 스스로 풀리기까지 남은 턴
		@Override
		public String iconTextDisplay() {
			return target instanceof Monk ? Integer.toString((int)Math.ceil(Math.max(0, ((Monk) target).mazeFocusLeft))) : "";
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", target instanceof Monk ? (int)Math.ceil(Math.max(0, ((Monk) target).mazeFocusLeft)) : 0);
		}
	}
}
