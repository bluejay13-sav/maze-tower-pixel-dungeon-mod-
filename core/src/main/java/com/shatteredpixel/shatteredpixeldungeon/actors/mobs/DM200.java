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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DM200Sprite;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class DM200 extends Mob {

	{
		spriteClass = DM200Sprite.class;

		HP = HT = 80;
		defenseSkill = 12;

		EXP = 9;
		maxLvl = 17;

		loot = Random.oneOf(Generator.Category.WEAPON, Generator.Category.ARMOR);
		lootChance = 0.2f; //initially, see lootChance()

		properties.add(Property.INORGANIC);
		properties.add(Property.LARGE);

		HUNTING = new Hunting();
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 10, 25 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 20;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 8);
	}

	@Override
	public float lootChance(){
		//each drop makes future drops 1/3 as likely
		// so loot chance looks like: 1/5, 1/15, 1/45, 1/135, etc.
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.DM200_EQUIP.count);
	}

	public Item createLoot() {
		Dungeon.LimitedDrops.DM200_EQUIP.count++;
		//uses probability tables for dwarf city
		if (loot == Generator.Category.WEAPON){
			return Generator.randomWeapon(4, true);
		} else {
			return Generator.randomArmor(4);
		}
	}

	private int ventCooldown = 0;

	{
		//Maze Tower (J7): 자기가 뿜는 마력 증기에 다치지 않는다
		if (MazeTower.J7_VAPOR) immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.class);
	}

	//Maze Tower (J7): 젖은 채 맞으면 기절한다 (봉인 등롱과 같은 규칙: 젖음을 지우고 2턴, 그 뒤 5턴은 다시 기절하지 않음).
	//박힌 채굴 기계(DM201)는 원작 그대로
	@Override
	public void damage( int dmg, Object src ) {
		boolean wet = MazeTower.J7_VAPOR && !(this instanceof DM201)
				&& com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.isWet(this);
		super.damage( dmg, src );
		if (wet && isAlive() && buff(DM100.StunGuard.class) == null
				&& buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class) == null){
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(this, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet.class);
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(this, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, MazeTower.H1_STUN_TURNS);
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(this, DM100.StunGuard.class, MazeTower.H1_STUN_GUARD);
			if (sprite != null && sprite.visible) sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.WARNING, Messages.get(DM200.class, "short_circuit"));
		}
	}

	private static final String VENT_COOLDOWN = "vent_cooldown";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(VENT_COOLDOWN, ventCooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		ventCooldown = bundle.getInt( VENT_COOLDOWN );
	}

	@Override
	protected boolean act() {
		ventCooldown--;
		return super.act();
	}

	public void onZapComplete(){
		zap();
		next();
	}

	private void zap( ){
		spend( TICK );
		ventCooldown = 30;

		Ballistica trajectory = new Ballistica(pos, enemy.pos, Ballistica.STOP_TARGET);

		//Maze Tower (J7): 갉힌 채굴 기계는 원작 독가스 자리에 마력 증기를 뿜는다 (같은 양)
		Class<? extends Blob> gas = MazeTower.J7_VAPOR ? com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.class : ToxicGas.class;
		for (int i : trajectory.subPath(0, trajectory.dist)){
			GameScene.add(Blob.seed(i, 20, gas));
		}
		GameScene.add(Blob.seed(trajectory.collisionPos, 100, gas));

	}

	protected boolean canVent(int target){
		if (ventCooldown > 0) return false;
		PathFinder.buildDistanceMap(target, BArray.not(Dungeon.level.solid, null), Dungeon.level.distance(pos, target)+1);
		//vent can go around blocking terrain, but not through it
		if (PathFinder.distance[pos] == Integer.MAX_VALUE){
			return false;
		}
		return true;
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

				if (distance(enemy) >= 1 && Random.Int(100/distance(enemy)) == 0 && canVent(target)){
					if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
						sprite.zap( enemy.pos );
						return false;
					} else {
						zap();
						return true;
					}

				} else if (getCloser( target )) {
					spend( 1 / speed() );
					return moveSprite( oldPos,  pos );

				} else if (canVent(target)) {
					if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
						sprite.zap( enemy.pos );
						return false;
					} else {
						zap();
						return true;
					}

				} else {
					//attempt to swap targets if the current one can't be reached or vented at
					return handleUnreachableTarget(enemyInFOV, justAlerted);
				}

			}
		}
	}

}
