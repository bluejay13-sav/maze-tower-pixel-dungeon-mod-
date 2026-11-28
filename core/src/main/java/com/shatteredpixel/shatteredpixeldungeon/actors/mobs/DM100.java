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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DM100Sprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

public class DM100 extends Mob {

	private static final float TIME_TO_ZAP	= 1f;
	
	{
		spriteClass = DM100Sprite.class;
		
		HP = HT = 20;
		defenseSkill = 8;
		
		EXP = 6;
		maxLvl = 13;
		
		loot = Generator.Category.SCROLL;
		lootChance = 0.25f;
		
		properties.add(Property.ELECTRIC);
		properties.add(Property.INORGANIC);
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 2, 8 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 11;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack(enemy)
				|| new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos;
	}
	
	//used so resistances can differentiate between melee and magical attacks
	public static class LightningBolt{}

	// ===== Maze Tower (H1): 봉인 등롱 =====
	//앞쪽 부채꼴(양옆 H1_CONE_HALF도, H1_RANGE칸)로만 본다. 빛이 닿는 칸은 안개 위에 늘 보인다(LanternLight).
	//주인공을 보면 그쪽을 향하고, 움직이면 가는 쪽을 향하며, 가만히 못 보는 동안에는 H1_TURN_TURNS턴마다 45도씩 돈다.
	//젖은 채 공격을 받으면 기절하고 젖음이 지워진다 (기절 뒤 잠깐은 다시 기절하지 않음).
	private int facing = com.watabou.utils.Random.Int(8);
	private int turnDir = com.watabou.utils.Random.Int(2) == 0 ? 1 : -1;
	private int turnTimer = 0;
	private com.shatteredpixel.shatteredpixeldungeon.effects.LanternLight light;

	private boolean maze(){ //금고 변종(VaultDM100)은 원작 그대로
		return com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_LANTERN && getClass() == DM100.class;
	}

	//Level.updateFieldOfView 끝에서: 부채꼴 밖을 지운다
	public void mazeMaskFov( boolean[] fov ){
		if (!maze()) return;
		LanternCone.mask(pos, facing, fov, com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_CONE_HALF, com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_RANGE);
	}

	@Override
	protected boolean act() {
		if (!maze()) return super.act();
		int oldPos = pos;
		boolean result = super.act();
		if (!isAlive()) return result;
		if (pos != oldPos){
			facing = LanternCone.octantToward(oldPos, pos);
			turnTimer = 0;
		} else if (enemy != null && enemySeen && enemy.isAlive()){
			facing = LanternCone.octantToward(pos, enemy.pos);
			turnTimer = 0;
		} else if (++turnTimer >= com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_TURN_TURNS){
			turnTimer = 0;
			facing = ((facing + turnDir) % 8 + 8) % 8;
		}
		updateLight();
		return result;
	}

	//빛이 닿는 바닥 칸을 다시 계산해 표시한다
	public void updateLight(){
		if (!maze() || sprite == null || Dungeon.level == null) return;
		if (light == null || light.parent == null){
			light = new com.shatteredpixel.shatteredpixeldungeon.effects.LanternLight();
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupOverFog(light);
		}
		boolean[] fov = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(this, fov); //부채꼴로 잘린 시야
		//(결정) 빛이 닿는 칸은 모두 같은 밝기(H1_LIGHT_ALPHA): 끝자락에 서도 똑같이 발각되므로 옅게 그리지 않는다 (읽히는 위험)
		java.util.HashMap<Integer, Float> cells = new java.util.HashMap<>();
		java.util.HashSet<Integer> near = new java.util.HashSet<>();
		for (int i = 0; i < fov.length; i++){
			if (!fov[i] || Dungeon.level.solid[i]) continue;
			//(결정) 한 번도 보거나 밝힌 적 없는 깜깜한 칸에는 그리지 않는다 (탐험하지 않은 곳에서 빛 모양으로 등롱 위치를 짐작할 수 없게)
			if (!Dungeon.level.heroFOV[i] && !Dungeon.level.visited[i] && !Dungeon.level.mapped[i]) continue;
			cells.put(i, com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_LIGHT_ALPHA);
			//(결정) 시야 밖에서는 등롱 가까이(H1_LIGHT_FOG_RANGE칸 안)의 축소된 범위만 보인다
			if (Dungeon.level.distance(pos, i) <= com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_LIGHT_FOG_RANGE) near.add(i);
		}
		light.set(cells, near);
		int dx = LanternCone.DIRS[facing][0];
		if (dx != 0) sprite.turnTo(pos, pos + dx);
	}

	@Override
	public void damage( int dmg, Object src ) {
		boolean wet = maze() && com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.isWet(this);
		super.damage( dmg, src );
		if (!maze() || !isAlive()) return;
		//맞으면 때린 쪽을 돌아본다 (기습은 맞는 순간에 이미 정해졌다)
		if (src instanceof Char && ((Char)src).pos != pos){
			facing = LanternCone.octantToward(pos, ((Char)src).pos);
			turnTimer = 0;
		}
		//젖은 채 맞으면 기절 (젖음을 지워 연달아 걸리지 않게, 잠깐은 다시 기절하지 않음)
		if (wet && buff(StunGuard.class) == null && buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class) == null){
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(this, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet.class);
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(this, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_STUN_TURNS);
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(this, StunGuard.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.H1_STUN_GUARD);
			if (sprite != null && sprite.visible) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "sputter"));
		}
		updateLight();
	}

	//기절한 뒤 잠깐은 다시 기절하지 않는다 (보이지 않는 표시)
	public static class StunGuard extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		if (light != null){ light.fadeAway(); light = null; } //빛은 서서히 사라진다
	}

	@Override
	public void destroy() {
		super.destroy();
		if (light != null){ light.fadeAway(); light = null; } //빛은 서서히 사라진다
	}

	private static final String FACING = "maze_facing", TURN_DIR = "maze_turn_dir", TURN_TIMER = "maze_turn_timer";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FACING, facing );
		bundle.put( TURN_DIR, turnDir );
		bundle.put( TURN_TIMER, turnTimer );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains(FACING)){
			facing = bundle.getInt( FACING );
			turnDir = bundle.getInt( TURN_DIR );
			turnTimer = bundle.getInt( TURN_TIMER );
		}
	}
	
	@Override
	protected boolean doAttack( Char enemy ) {

		if (Dungeon.level.adjacent( pos, enemy.pos )
				|| new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos != enemy.pos) {
			
			return super.doAttack( enemy );
			
		} else {
			
			spend( TIME_TO_ZAP );

			Invisibility.dispel(this);
			if (hit( this, enemy, true )) {
				int dmg = Random.NormalIntRange(3, 10);
				dmg = Math.round(dmg * AscensionChallenge.statModifier(this));
				enemy.damage( dmg, new LightningBolt() );

				if (enemy.sprite.visible) {
					enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
					enemy.sprite.flash();
				}
				
				if (enemy == Dungeon.hero) {
					
					PixelScene.shake( 2, 0.3f );
					
					if (!enemy.isAlive()) {
						Badges.validateDeathFromEnemyMagic();
						Dungeon.fail( this );
						GLog.n( Messages.get(this, "zap_kill") );
					}
				}
			} else {
				enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
			}
			
			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				sprite.zap( enemy.pos );
				return false;
			} else {
				return true;
			}
		}
	}
	
}
