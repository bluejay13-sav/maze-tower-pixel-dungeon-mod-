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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetedCell;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.RatSkull;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.CursedWand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ElementalSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public abstract class Elemental extends Mob {

	{
		HP = HT = MazeTower.L3_HP; //Maze Tower (사용자): 원작 60에서 조금 낮춤
		defenseSkill = MazeTower.L3_DEF; //원작 20
		
		EXP = 10;
		maxLvl = 20;
		
		flying = true;
	}

	protected boolean summonedALly;

	// ===== Maze Tower (L3·L4): 풀려난 주문 =====
	//원소: 0 없음, 1 불, 2 냉기, 3 전격 (뒤틀린 주문은 다음에 쓸 원소)
	public int mazeElement(){ return 0; }

	//L4 궁정 마도사가 이 주문을 증폭하는 중이면 그 마도사의 id (-1 = 아님)
	public int mazeAmpBy = -1;

	public boolean mazeAmplified(){
		if (mazeAmpBy == -1) return false;
		Actor a = Actor.findById( mazeAmpBy );
		if (!(a instanceof Warlock) || !((Warlock) a).isAlive() || !Warlock.mazeCanAmplify( (Warlock) a, this )){ mazeAmpBy = -1; return false; }
		return true;
	}

	//원소를 묻힌다 (원작 상태이상을 붙이므로 원소 반응도 그대로 일어난다)
	public static void mazeApply( Char ch, int element ){
		if (ch == null || !ch.isAlive()) return;
		switch (element){
			case 1: Buff.affect( ch, Burning.class ).reignite( ch ); break;
			case 2: Buff.prolong( ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill.class, MazeTower.L3_CHILL_TURNS ); break;
			case 3: Buff.prolong( ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge.class, MazeTower.STATIC_TURNS ); break;
		}
	}

	//(L3) 쓰러질 때 자기 원소를 둘레 한 칸의 모두(주인공·적)에게 터뜨린다: 작은 피해 + 원소. 증폭된 주문은 더 아프다
	protected void mazeBurst(){
		int el = mazeElement();
		if (!MazeTower.L3_BURST || el == 0 || summonedALly || alignment == Alignment.ALLY) return;
		float multi = MazeTower.L3_BURST_MULTI * (mazeAmplified() ? MazeTower.L4_AMP_DMG : 1f);
		int dmg = Math.max(1, Math.round(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.baseDamage() * multi));
		boolean seen = Dungeon.level.heroFOV[pos];
		for (int n : PathFinder.NEIGHBOURS9){
			int c = pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (Dungeon.level.heroFOV[c]){
				if (el == 1) CellEmitter.get(c).burst( com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle.FACTORY, 6 );
				else if (el == 2) CellEmitter.get(c).burst( com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle.FACTORY, 6 );
				else CellEmitter.get(c).burst( com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle.FACTORY, 6 );
			}
			Char ch = Actor.findChar( c );
			if (ch == null || ch == this || !ch.isAlive()) continue;
			//옆의 다른 주문과는 원소끼리 바로 반응한다 (주문은 반대 원소를 받지 않으므로): 불↔냉기 = 증기, 불↔전격 = 폭발
			if (ch instanceof Elemental && ((Elemental) ch).mazeElement() != 0 && ((Elemental) ch).mazeElement() != el){
				int other = ((Elemental) ch).mazeElement();
				ch.damage( dmg, new SpellBurst() );
				if (!ch.isAlive()) continue;
				if ((el == 1 && other == 2) || (el == 2 && other == 1)){
					com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.steamOn( ch, Math.round( dmg * MazeTower.L3_SPELL_REACT ) );
				} else if ((el == 1 && other == 3) || (el == 3 && other == 1)){
					com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.explodeAt( ch.pos );
				}
				continue;
			}
			ch.damage( dmg, new SpellBurst() );
			mazeApply( ch, el );
		}
		if (seen){
			Sample.INSTANCE.play( el == 1 ? Assets.Sounds.BURNING : el == 2 ? Assets.Sounds.SHATTER : Assets.Sounds.LIGHTNING );
			com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake( 1, 0.1f );
		}
	}

	//(L3) 풀려난 주문이 터지며 휘말림 (사망 기록)
	public static class SpellBurst implements Hero.Doom {
		@Override
		public void onDeath() {
			Dungeon.fail( this );
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( Messages.get( Elemental.class, "burst_kill" ) );
		}
	}
	
	@Override
	public int damageRoll() {
		if (!summonedALly) {
			return MazeTower.L3_BURST ? Math.max( 1, Math.round( Random.NormalIntRange(20, 25) * MazeTower.L3_MELEE_DMG ) ) : Random.NormalIntRange(20, 25); //Maze Tower (L3, 사용자): 바로 옆에서는 약한 근접만 (탄이 주된 피해)
		} else {
			int regionScale = Math.max(2, (1 + Dungeon.scalingDepth()/5));
			return Random.NormalIntRange(5*regionScale, 5 + 5*regionScale);
		}
	}
	
	@Override
	public int attackSkill( Char target ) {
		if (!summonedALly) {
			return MazeTower.L3_ACC; //Maze Tower (사용자): 원작 25
		} else {
			int regionScale = Math.max(2, (1 + Dungeon.scalingDepth()/5));
			return 5 + 5*regionScale;
		}
	}

	public void setSummonedALly(){
		summonedALly = true;
		//sewers are prison are equivalent, otherwise scales as normal (2/2/3/4/5)
		int regionScale = Math.max(2, (1 + Dungeon.scalingDepth()/5));
		defenseSkill = 5*regionScale;
		HT = 15*regionScale;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 5);
	}
	
	protected int rangedCooldown = Random.NormalIntRange( MazeTower.L3_RANGED_CD_MIN, MazeTower.L3_RANGED_CD_MAX ); //Maze Tower (L3): 원작 3~5
	
	@Override
	protected boolean act() {
		if (state == HUNTING){
			rangedCooldown--;
		}
		
		return super.act();
	}

	@Override
	public void die(Object cause) {
		flying = false;
		mazeBurst(); //Maze Tower (L3)
		super.die(cause);
	}
	
	@Override
	protected boolean canAttack( Char enemy ) {
		if (super.canAttack(enemy)){
			return true;
		} else {
			return rangedCooldown < 0 && new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT ).collisionPos == enemy.pos;
		}
	}
	
	protected boolean doAttack( Char enemy ) {
		
		//Maze Tower (L3, 사용자): 바로 옆에 붙어 있어도 쿨타임이 돌아오면 탄(스킬)을 쓴다. 쿨타임 동안은 약한 근접
		if ((Dungeon.level.adjacent( pos, enemy.pos ) && !MazeTower.L3_BURST)
				|| rangedCooldown > 0
				|| new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT ).collisionPos != enemy.pos) {
			
			return super.doAttack( enemy );
			
		} else {
			
			if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
				sprite.zap( enemy.pos );
				return false;
			} else {
				zap();
				return true;
			}
		}
	}
	
	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		meleeProc( enemy, damage );
		
		return damage;
	}
	
	protected void zap() {
		spend( 1f );

		Invisibility.dispel(this);
		Char enemy = this.enemy;
		if (hit( this, enemy, true )) {
			//Maze Tower (L3·L4, 사용자): 풀려난 주문은 쏘는 탄이 주된 피해다(원작은 원소만 묻힘). 증폭되면 탄 피해가 커진다
			if (!summonedALly && MazeTower.L3_BURST){
				int dmg = Random.NormalIntRange( MazeTower.L3_BOLT_MIN, MazeTower.L3_BOLT_MAX ); //(사용자) 조금 낮춤
				if (mazeAmplified()) dmg = Math.round( dmg * MazeTower.L4_AMP_DMG );
				dmg = Math.max( 0, dmg - enemy.drRoll() );
				enemy.damage( dmg, this );
			}
			if (enemy.isAlive()) rangedProc( enemy );
			
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
		}

		rangedCooldown = Random.NormalIntRange( MazeTower.L3_RANGED_CD_MIN, MazeTower.L3_RANGED_CD_MAX ); //Maze Tower (L3)
	}
	
	public void onZapComplete() {
		zap();
		next();
	}
	
	@Override
	public boolean add( Buff buff ) {
		if (harmfulBuffs.contains( buff.getClass() )) {
			damage( Random.NormalIntRange( HT/2, HT * 3/5 ), buff );
			return false;
		} else {
			return super.add( buff );
		}
	}
	
	protected abstract void meleeProc( Char enemy, int damage );
	protected abstract void rangedProc( Char enemy );
	
	protected ArrayList<Class<? extends Buff>> harmfulBuffs = new ArrayList<>();
	
	private static final String COOLDOWN = "cooldown";
	private static final String SUMMONED_ALLY = "summoned_ally";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( COOLDOWN, rangedCooldown );
		bundle.put( SUMMONED_ALLY, summonedALly);
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( COOLDOWN )){
			rangedCooldown = bundle.getInt( COOLDOWN );
		}
		summonedALly = bundle.getBoolean( SUMMONED_ALLY );
		if (summonedALly){
			setSummonedALly();
		}
	}
	
	public static class FireElemental extends Elemental {
		@Override public int mazeElement(){ return 1; } //Maze Tower (L3)
		
		{
			spriteClass = ElementalSprite.Fire.class;
			
			loot = PotionOfLiquidFlame.class;
			lootChance = 1/8f;
			
			properties.add( Property.FIERY );
			
			harmfulBuffs.add( com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class );
			harmfulBuffs.add( Chill.class );
		}
		
		@Override
		protected void meleeProc( Char enemy, int damage ) {
			if (Random.Int( 2 ) == 0 && !Dungeon.level.water[enemy.pos]) {
				Buff.affect( enemy, Burning.class ).reignite( enemy );
				if (enemy.sprite.visible) Splash.at( enemy.sprite.center(), sprite.blood(), 5);
			}
		}
		
		@Override
		protected void rangedProc( Char enemy ) {
			if (!Dungeon.level.water[enemy.pos]) {
				Buff.affect( enemy, Burning.class ).reignite( enemy, 4f );
			}
			if (enemy.sprite.visible) Splash.at( enemy.sprite.center(), sprite.blood(), 5);
		}
	}
	
	//used in wandmaker quest, a fire elemental with lower ACC/EVA/DMG, no on-hit fire
	// and a unique 'fireball' style ranged attack, which can be dodged
	public static class NewbornFireElemental extends FireElemental {
		@Override public int mazeElement(){ return 0; } //Maze Tower (L3): 퀘스트의 갓 태어난 불의 정령은 터지지 않는다
		
		{
			spriteClass = ElementalSprite.NewbornFire.class;

			defenseSkill = 12;
			
			properties.add(Property.MINIBOSS);
		}

		private int targetingPos = -1;

		@Override
		protected boolean act() {
			//fire a charged attack instead of any other action, as long as it is possible to do so
			if (targetingPos != -1 && state == HUNTING){
				//account for bolt hitting walls, in case position suddenly changed
				targetingPos = new Ballistica( pos, targetingPos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET ).collisionPos;
				if (sprite != null && (sprite.visible || Dungeon.level.heroFOV[targetingPos])) {
					sprite.zap( targetingPos );
					return false;
				} else {
					zap();
					return true;
				}
			} else {

				if (state != HUNTING){
					targetingPos = -1;
				}

				return super.act();
			}
		}

		@Override
		protected boolean canAttack( Char enemy ) {
			if (super.canAttack(enemy)){
				return true;
			} else {
				return rangedCooldown < 0 && new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET ).collisionPos == enemy.pos;
			}
		}

		protected boolean doAttack( Char enemy ) {

			if (rangedCooldown > 0) {

				return super.doAttack( enemy );

			} else if (new Ballistica( pos, enemy.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET ).collisionPos == enemy.pos) {

				//set up an attack for next turn
				ArrayList<Integer> candidates = new ArrayList<>();
				for (int i : PathFinder.NEIGHBOURS8){
					int target = enemy.pos + i;
					if (target != pos && new Ballistica(pos, target, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET).collisionPos == target){
						candidates.add(target);
					}
				}

				if (!candidates.isEmpty()){
					targetingPos = Random.element(candidates);

					float delay = GameMath.gate(attackDelay(), (int)Math.ceil(Dungeon.hero.cooldown()), 3*attackDelay());

					for (int i : PathFinder.NEIGHBOURS9){
						if (!Dungeon.level.solid[targetingPos + i]) {
							GameScene.targetedCell(targetingPos + i, delay);
						}
					}

					GLog.n(Messages.get(this, "charging"));
					spend(delay);
					Dungeon.hero.interrupt();
					return true;
				} else {
					rangedCooldown = 1;
					return super.doAttack(enemy);
				}


			} else {

				if (sprite != null && (sprite.visible || Dungeon.level.heroFOV[targetingPos])) {
					sprite.zap( targetingPos );
					return false;
				} else {
					zap();
					return true;
				}

			}
		}

		@Override
		protected void zap() {
			if (targetingPos != -1) {
				spend(1f);

				Invisibility.dispel(this);

				for (int i : PathFinder.NEIGHBOURS9) {
					if (!Dungeon.level.solid[targetingPos + i]) {
						CellEmitter.get(targetingPos + i).burst(ElmoParticle.FACTORY, 5);
						if (Dungeon.level.water[targetingPos + i]) {
							GameScene.add(Blob.seed(targetingPos + i, 2, Fire.class));
						} else {
							GameScene.add(Blob.seed(targetingPos + i, 8, Fire.class));
						}

						Char target = Actor.findChar(targetingPos + i);
						if (target != null && target != this) {
							Buff.affect(target, Burning.class).reignite(target);
							if (target == Dungeon.hero){
								Statistics.questScores[1] -= 200;
							}
						}
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
			}

			targetingPos = -1;
			rangedCooldown = Random.NormalIntRange( 3, 5 );
		}

		@Override
		public int attackSkill(Char target) {
			if (!summonedALly) {
				return 15;
			} else {
				return super.attackSkill(target);
			}
		}

		@Override
		public int damageRoll() {
			if (!summonedALly) {
				return Random.NormalIntRange(10, 12);
			} else {
				return super.damageRoll();
			}
		}

		@Override
		protected void meleeProc(Char enemy, int damage) {
			//no fiery on-hit unless it is an ally summon
			if (summonedALly) {
				super.meleeProc(enemy, damage);
			}
		}

		@Override
		public void die(Object cause) {
			super.die(cause);
			if (alignment == Alignment.ENEMY) {
				Dungeon.level.drop( new Embers(), pos ).sprite.drop();
				//assign score here as player may choose to keep the embers
				Statistics.questScores[1] += 2000;
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.fadeOut(1f, new Callback() {
							@Override
							public void call() {
								if (Dungeon.level != null) {
									Dungeon.level.playLevelMusic();
								}
							}
						});
					}
				});
			}
		}

		@Override
		public boolean reset() {
			return !summonedALly;
		}

		@Override
		public String description() {
			String desc = super.description();

			if (summonedALly){
				desc += " " + Messages.get(this, "desc_ally");
			} else {
				desc += " " + Messages.get(this, "desc_boss");
			}

			return desc;
		}

		private static final String TARGETING_POS = "targeting_pos";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TARGETING_POS, targetingPos);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			targetingPos = bundle.getInt(TARGETING_POS);
		}
	}

	//not a miniboss, no ranged attack, otherwise a newborn elemental
	public static class AllyNewBornElemental extends NewbornFireElemental {

		{
			rangedCooldown = Integer.MAX_VALUE;

			properties.remove(Property.MINIBOSS);
		}

	}
	
	public static class FrostElemental extends Elemental {
		@Override public int mazeElement(){ return 2; } //Maze Tower (L3)
		
		{
			spriteClass = ElementalSprite.Frost.class;
			
			loot = PotionOfFrost.class;
			lootChance = 1/8f;
			
			properties.add( Property.ICY );
			
			harmfulBuffs.add( Burning.class );
		}
		
		@Override
		protected void meleeProc( Char enemy, int damage ) {
			if (Random.Int( 3 ) == 0 || Dungeon.level.water[enemy.pos]) {
				Freezing.freeze( enemy.pos );
				if (enemy.sprite.visible) Splash.at( enemy.sprite.center(), sprite.blood(), 5);
			}
		}
		
		@Override
		protected void rangedProc( Char enemy ) {
			Freezing.freeze( enemy.pos );
			if (enemy.sprite.visible) Splash.at( enemy.sprite.center(), sprite.blood(), 5);
		}
	}
	
	public static class ShockElemental extends Elemental {
		@Override public int mazeElement(){ return 3; } //Maze Tower (L3)
		
		{
			spriteClass = ElementalSprite.Shock.class;
			
			loot = ScrollOfRecharging.class;
			lootChance = 1/4f;
			
			properties.add( Property.ELECTRIC );
		}
		
		@Override
		protected void meleeProc( Char enemy, int damage ) {
			ArrayList<Char> affected = new ArrayList<>();
			ArrayList<Lightning.Arc> arcs = new ArrayList<>();
			Shocking.arc( this, enemy, 2, affected, arcs );
			
			if (!Dungeon.level.water[enemy.pos]) {
				affected.remove( enemy );
			}
			
			for (Char ch : affected) {
				ch.damage( Math.round( damage * 0.4f ), new Shocking() );
				if (ch == Dungeon.hero && !ch.isAlive()){
					Badges.validateDeathFromEnemyMagic();
					Dungeon.fail(this);
					GLog.n( Messages.capitalize(Messages.get(Char.class, "kill", name())) );
				}
			}

			boolean visible = sprite.visible || enemy.sprite.visible;
			for (Char ch : affected){
				if (ch.sprite.visible) visible = true;
			}

			if (visible) {
				sprite.parent.addToFront(new Lightning(arcs, null));
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
			}
		}
		
		@Override
		protected void rangedProc( Char enemy ) {
			Buff.affect( enemy, Blindness.class, Blindness.DURATION/2f );
			if (enemy == Dungeon.hero) {
				GameScene.flash(0x80FFFFFF);
			}
		}
	}
	
	public static class ChaosElemental extends Elemental {
		
		{
			spriteClass = ElementalSprite.Chaos.class;
			
			loot = ScrollOfTransmutation.class;
			lootChance = 1f;
		}

		//Maze Tower (L3, 사용자): 뒤틀린 주문은 원작의 무작위 저주 마법 대신, 칠 때·쏠 때·터질 때마다 불·냉기·전격 중 하나를 무작위로 쓴다
		//(그림은 세 색 혼합, 예고 없음. 다음에 쓸 원소 mazeNext, 저장됨)
		public int mazeNext = Random.IntRange( 1, 3 );

		@Override public int mazeElement(){ return mazeNext; }

		private void reroll(){ mazeNext = Random.IntRange( 1, 3 ); }

		@Override
		protected void meleeProc( Char enemy, int damage ) {
			mazeApply( enemy, mazeNext );
			reroll();
		}

		@Override
		protected void rangedProc( Char enemy ) {
			mazeApply( enemy, mazeNext );
			reroll();
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( "maze_next", mazeNext );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			if (bundle.contains( "maze_next" )) mazeNext = bundle.getInt( "maze_next" );
		}
	}

	public static Class<? extends Elemental> random(){
		float altChance = 1/50f * RatSkull.exoticChanceMultiplier();
		if (Random.Float() < altChance){
			return ChaosElemental.class;
		}
		
		float roll = Random.Float();
		if (roll < 0.4f){
			return FireElemental.class;
		} else if (roll < 0.8f){
			return FrostElemental.class;
		} else {
			return ShockElemental.class;
		}
	}
}
