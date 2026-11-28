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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ThiefSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Thief extends Mob {
	
	public Item item;
	
	{
		spriteClass = ThiefSprite.class;
		
		HP = HT = 20;
		defenseSkill = 12;
		
		EXP = 5;
		maxLvl = 11;

		loot = Random.oneOf(Generator.Category.RING, Generator.Category.ARTIFACT);
		lootChance = 0.03f; //initially, see lootChance()

		WANDERING = new Wandering();
		FLEEING = new Fleeing();

		properties.add(Property.UNDEAD);
	}

	private static final String ITEM = "item";

	//Maze Tower (H3): 문을 한 번 잠갔는지 (탈옥수 한 마리는 문 하나를 한 번만 잠근다)
	private boolean mazeLocked = false;
	private static final String MAZE_LOCKED = "maze_locked";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( ITEM, item );
		bundle.put( MAZE_LOCKED, mazeLocked );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		item = (Item)bundle.get( ITEM );
		mazeLocked = bundle.getBoolean( MAZE_LOCKED );
	}

	//Maze Tower (H3): 잠근 문 때문에 주인공 쪽에서 길이 끊겨도 계속 멀어지게 한다.
	//원작 도망은 주인공 자리에서 길을 따라 거리를 재므로 문이 잠기면 거리를 못 재고 멈춰 버린다 → 잠근 문을 지나갈 수 있는 칸으로 치고 다시 잰다.
	@Override
	protected boolean getFurther( int target ) {
		if (super.getFurther( target )) return true;
		if (rooted || target == pos) return false;
		boolean[] pass = null;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ThiefLock.at(i)){
				if (pass == null) pass = Dungeon.level.passable.clone();
				pass[i] = true;
			}
		}
		if (pass == null) return false;
		int step = Dungeon.flee( this, target, pass, fieldOfView, true );
		if (step == -1 || !Dungeon.level.passable[step]) return false;
		move( step );
		return true;
	}

	//Maze Tower (H3): 도망치며 문을 지나면 뒤에서 문을 닫아 잠깐(H3_LOCK_TURNS턴) 걸어 잠근다.
	//문에 물건이 놓여 있으면 원작대로 문이 닫히지 않아 잠기지 않는다. 문 칸에 누가 있어도 잠그지 않는다.
	@Override
	public void move( int step, boolean travelling ) {
		int from = pos;
		super.move( step, travelling );
		if (!com.shatteredpixel.shatteredpixeldungeon.MazeTower.H3_THIEF_LOCK || mazeLocked
				|| state != FLEEING || pos == from || !isAlive()) return;
		if (Dungeon.level.map[from] != com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR) return; //물건이 놓이면 열린 채 남는다
		if (Dungeon.level.heaps.get(from) != null || com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(from) != null) return;
		mazeLocked = true;
		com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(from, com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR);
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateMap(from);
		//게임 시간에 등록해야 턴이 흐른다 (GameScene.add)
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add( com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(from,
				com.shatteredpixel.shatteredpixeldungeon.MazeTower.H3_LOCK_TURNS, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ThiefLock.class) );
		if (Dungeon.level.heroFOV[from]){
			com.watabou.noosa.audio.Sample.INSTANCE.play( com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.UNLOCK );
			//(결정) 문 위에 "잠김!"만 띄운다
			com.watabou.utils.PointF p = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(from);
			com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.show(p.x, p.y, from, Messages.get(Thief.class, "locked"),
					com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.WARNING);
		}
		Dungeon.observe();
	}

	@Override
	public float speed() {
		if (item != null) return (5*super.speed())/6;
		else return super.speed();
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 1, 10 );
	}

	@Override
	public float attackDelay() {
		return super.attackDelay()*0.5f;
	}

	@Override
	public float lootChance() {
		//each drop makes future drops 1/3 as likely
		// so loot chance looks like: 1/33, 1/100, 1/300, 1/900, etc.
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.THEIF_MISC.count);
	}

	@Override
	public void rollToDropLoot() {
		if (item != null) {
			Dungeon.level.drop( item, pos ).sprite.drop();
			//updates position
			if (item instanceof Honeypot.ShatteredPot) ((Honeypot.ShatteredPot)item).dropPot( this, pos );
			item = null;
		}
		//Maze Tower (H3): 잡으면 훔친 것에 더해 제 주머니의 금화도 떨군다
		if (com.shatteredpixel.shatteredpixeldungeon.MazeTower.H3_THIEF_LOCK && Dungeon.level.heaps != null){
			Dungeon.level.drop( new Gold().random(), pos ).sprite.drop();
		}
		super.rollToDropLoot();
	}

	@Override
	public Item createLoot() {
		Dungeon.LimitedDrops.THEIF_MISC.count++;
		return super.createLoot();
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 3);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		
		if (alignment == Alignment.ENEMY && item == null
				&& enemy instanceof Hero && steal( (Hero)enemy )) {
			state = FLEEING;
		}

		return damage;
	}

	@Override
	public int defenseProc(Char enemy, int damage) {
		if (state == FLEEING) {
			Dungeon.level.drop( new Gold(), pos ).sprite.drop();
		}

		return super.defenseProc(enemy, damage);
	}

	protected boolean steal( Hero hero ) {

		Item toSteal = hero.belongings.randomUnequipped();

		if (toSteal != null && !toSteal.unique && toSteal.level() < 1 ) {

			GLog.w( Messages.get(Thief.class, "stole", toSteal.name()) );
			if (!toSteal.stackable) {
				Dungeon.quickslot.convertToPlaceholder(toSteal);
			}
			Item.updateQuickslot();

			item = toSteal.detach( hero.belongings.backpack );
			if (item instanceof Honeypot){
				item = ((Honeypot)item).shatter(this, this.pos);
			} else if (item instanceof Honeypot.ShatteredPot) {
				((Honeypot.ShatteredPot)item).pickupPot(this);
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public String description() {
		String desc = super.description();

		if (item != null) {
			desc += Messages.get(this, "carries", item.name() );
		}

		return desc;
	}
	
	private class Wandering extends Mob.Wandering {
		
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			super.act(enemyInFOV, justAlerted);
			
			//if an enemy is just noticed and the thief posses an item, run, don't fight.
			if (state == HUNTING && item != null){
				state = FLEEING;
			}
			
			return true;
		}
	}

	private class Fleeing extends Mob.Fleeing {
		@Override
		protected void escaped() {
			if (item != null
					&& !Dungeon.level.heroFOV[pos]
					&& Dungeon.level.distance(Dungeon.hero.pos, pos) >= 6) {

				int count = 32;
				int newPos;
				do {
					newPos = Dungeon.level.randomRespawnCell( Thief.this );
					if (count-- <= 0) {
						break;
					}
				} while (newPos == -1 || Dungeon.level.heroFOV[newPos] || Dungeon.level.distance(newPos, pos) < (count/3));

				if (newPos != -1) {

					pos = newPos;
					sprite.place( pos );
					sprite.visible = Dungeon.level.heroFOV[pos];
					if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);

				}

				if (item != null) GLog.n( Messages.get(Thief.class, "escapes", item.name()));
				item = null;
				state = WANDERING;
			} else {
				state = WANDERING;
			}
		}
	}
}
