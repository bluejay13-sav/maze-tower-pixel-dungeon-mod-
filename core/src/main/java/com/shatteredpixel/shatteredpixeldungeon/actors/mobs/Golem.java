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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GolemSprite;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Golem extends Mob {
	
	{
		spriteClass = GolemSprite.class;
		
		HP = HT = 120;
		defenseSkill = 15;
		
		EXP = 12;
		maxLvl = 22;

		loot = Random.oneOf(Generator.Category.WEAPON, Generator.Category.ARMOR);
		lootChance = 0.2f; //initially, see lootChance()

		properties.add(Property.INORGANIC);
		properties.add(Property.LARGE);

		WANDERING = new Wandering();
		HUNTING = new Hunting();
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 25, 30 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 28;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 12);
	}

	@Override
	public float lootChance() {
		//each drop makes future drops 1/3 as likely
		// so loot chance looks like: 1/5, 1/15, 1/45, 1/135, etc.
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.GOLEM_EQUIP.count);
	}

	@Override
	public void rollToDropLoot() {
		Imp.Quest.oldProcess( this );
		super.rollToDropLoot();
	}

	public Item createLoot() {
		Dungeon.LimitedDrops.GOLEM_EQUIP.count++;
		//uses probability tables for demon halls
		if (loot == Generator.Category.WEAPON){
			return Generator.randomWeapon(5, true);
		} else {
			return Generator.randomArmor(5);
		}
	}


	// ===== Maze Tower (L5): 마도 석상은 가장 가까운 아군 하나를 수호한다 (금빛 선) =====
	//시선이 닿는 4칸(L5_RANGE) 안의, 다른 석상이 수호하지 않는 가장 가까운 아군(석상 제외). 그 아군이 받을 피해는 모두 석상이 대신 받는다
	//(석상 방어 무시, Char.damage → mazeGuardianOf). 시선이 끊기거나 멀어지면 선도 끊긴다(매 차례, 피해를 받는 순간에도 다시 확인)
	public int mazeGuardTarget = -1;

	public static boolean mazeCanGuard( Golem g, Char c ){
		return com.shatteredpixel.shatteredpixeldungeon.MazeTower.L5_GUARD && g.isAlive() && c != null && c.isAlive()
				&& c instanceof Mob && !(c instanceof Golem) && g.alignment == c.alignment
				&& Dungeon.level.distance( g.pos, c.pos ) <= com.shatteredpixel.shatteredpixeldungeon.MazeTower.L5_RANGE
				&& new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica( g.pos, c.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE ).collisionPos == c.pos;
	}

	public Char mazeGuarded(){
		Actor a = mazeGuardTarget == -1 ? null : Actor.findById( mazeGuardTarget );
		return a instanceof Char ? (Char) a : null;
	}

	//c를 지금 수호하는 석상 (없으면 null)
	public static Golem mazeGuardianOf( Char c ){
		if (c == null || !(c instanceof Mob) || c instanceof Golem || Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m instanceof Golem && ((Golem) m).mazeGuardTarget == c.id() && mazeCanGuard( (Golem) m, c )) return (Golem) m;
		}
		return null;
	}

	private void mazeGuard(){
		Char cur = mazeGuarded();
		if (cur != null && mazeCanGuard( this, cur )) return;
		mazeGuardTarget = -1;
		Mob best = null; int bestD = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!mazeCanGuard( this, m )) continue;
			if (mazeGuardianOf( m ) != null) continue;
			int d = Dungeon.level.distance( pos, m.pos );
			if (d < bestD){ bestD = d; best = m; }
		}
		if (best != null) mazeGuardTarget = best.id();
	}

	//수호하는 아군 대신 받는 피해: 석상 방어 무시, 석상이 번쩍이고 아군 둘레에 금빛
	public void mazeTakeGuarded( int dmg, Object src, Char ally ){
		if (sprite != null && sprite.visible) sprite.flash();
		if (ally.sprite != null && ally.sprite.visible){
			ally.sprite.emitter().burst( com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory( com.shatteredpixel.shatteredpixeldungeon.effects.Speck.LIGHT ), 3 );
		}
		damage( dmg, src );
	}

	private boolean teleporting = false;
	private int selfTeleCooldown = 0;
	private int enemyTeleCooldown = 0;

	private static final String TELEPORTING = "teleporting";
	private static final String SELF_COOLDOWN = "self_cooldown";
	private static final String ENEMY_COOLDOWN = "enemy_cooldown";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TELEPORTING, teleporting);
		bundle.put(SELF_COOLDOWN, selfTeleCooldown);
		bundle.put(ENEMY_COOLDOWN, enemyTeleCooldown);
		bundle.put( "maze_guard", mazeGuardTarget ); //Maze Tower (L5)
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		teleporting = bundle.getBoolean( TELEPORTING );
		selfTeleCooldown = bundle.getInt( SELF_COOLDOWN );
		enemyTeleCooldown = bundle.getInt( ENEMY_COOLDOWN );
		if (bundle.contains( "maze_guard" )) mazeGuardTarget = bundle.getInt( "maze_guard" ); //Maze Tower (L5)
	}

	@Override
	protected boolean act() {
		selfTeleCooldown--;
		mazeGuard(); //Maze Tower (L5)
		enemyTeleCooldown--;
		if (teleporting){
			((GolemSprite)sprite).teleParticles(false);
			if (Actor.findChar(target) == null && Dungeon.level.openSpace[target]) {
				ScrollOfTeleportation.appear(this, target);
				selfTeleCooldown = 30;
			} else {
				target = Dungeon.level.randomDestination(this);
			}
			teleporting = false;
			spend(TICK);
			return true;
		}
		return super.act();
	}

	public void onZapComplete(){
		teleportEnemy();
		next();
	}

	public void teleportEnemy(){
		spend(TICK);

		int bestPos = enemy.pos;
		for (int i : PathFinder.NEIGHBOURS8){
			if (Dungeon.level.passable[pos + i]
				&& Actor.findChar(pos+i) == null
				&& Dungeon.level.trueDistance(pos+i, enemy.pos) > Dungeon.level.trueDistance(bestPos, enemy.pos)){
				bestPos = pos+i;
			}
		}

		if (enemy.buff(MagicImmune.class) != null){
			bestPos = enemy.pos;
		}

		if (bestPos != enemy.pos){
			ScrollOfTeleportation.appear(enemy, bestPos);
			if (enemy instanceof Hero){
				((Hero) enemy).interrupt();
				Dungeon.observe();
				GameScene.updateFog();
			}
		}

		enemyTeleCooldown = 20;
	}

	private boolean canTele(int target){
		if (enemyTeleCooldown > 0) return false;
		PathFinder.buildDistanceMap(target, BArray.not(Dungeon.level.solid, null), Dungeon.level.distance(pos, target)+1);
		//zaps can go around blocking terrain, but not through it
		if (PathFinder.distance[pos] == Integer.MAX_VALUE){
			return false;
		}
		return true;
	}

	private class Wandering extends Mob.Wandering{

		@Override
		protected boolean continueWandering() {
			enemySeen = false;

			int oldPos = pos;
			if (target != -1 && getCloser( target )) {
				spend( 1 / speed() );
				return moveSprite( oldPos, pos );
			} else if (!Dungeon.bossLevel() && target != -1 && target != pos && selfTeleCooldown <= 0) {
				((GolemSprite)sprite).teleParticles(true);
				teleporting = true;
				spend( 2*TICK );
			} else {
				target = randomDestination();
				spend( TICK );
			}

			return true;
		}
	}

	private class Hunting extends Mob.Hunting{

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (!enemyInFOV || canAttack(enemy)) {
				return super.act(enemyInFOV, justAlerted);
			} else {

				if (handleRecentAttackers()){
					return act( true, justAlerted );
				}

				enemySeen = true;
				target = enemy.pos;

				int oldPos = pos;

				if (distance(enemy) >= 1 && Random.Int(100/distance(enemy)) == 0
						&& !Char.hasProp(enemy, Property.IMMOVABLE) && canTele(target)){
					if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
						sprite.zap( enemy.pos );
						return false;
					} else {
						teleportEnemy();
						return true;
					}

				} else if (getCloser( target )) {
					spend( 1 / speed() );
					return moveSprite( oldPos,  pos );

				} else if (!Char.hasProp(enemy, Property.IMMOVABLE) && canTele(target)) {
					if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
						sprite.zap( enemy.pos );
						return false;
					} else {
						teleportEnemy();
						return true;
					}

				} else {
					//attempt to swap targets if the current one can't be reached or teleported
					return handleUnreachableTarget(enemyInFOV, justAlerted);
				}

			}
		}
	}

}
