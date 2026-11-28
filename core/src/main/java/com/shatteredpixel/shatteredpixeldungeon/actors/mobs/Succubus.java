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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SuccubusSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class Succubus extends Mob {

	private int blinkCooldown = 0;
	
	{
		spriteClass = SuccubusSprite.class;
		
		HP = HT = 80;
		defenseSkill = 25;
		viewDistance = Light.DISTANCE;
		
		EXP = 12;
		maxLvl = 25;
		
		loot = Generator.Category.SCROLL;
		lootChance = 0.33f;

		properties.add(Property.DEMONIC);
	}
	
	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 25, 30 );
	}
	
	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		
		if (enemy.buff(Charm.class) != null ){
			int shield = (HP - HT) + (5 + damage);
			if (shield > 0){
				HP = HT;
				if (shield < 5){
					sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(5-shield), FloatingText.HEALING);
				}

				Buff.affect(this, Barrier.class).setShield(shield);
				sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			} else {
				HP += 5 + damage;
				sprite.showStatusWithIcon(CharSprite.POSITIVE, "5", FloatingText.HEALING);
			}
			if (Dungeon.level.heroFOV[pos]) {
				Sample.INSTANCE.play( Assets.Sounds.CHARMS );
			}
		} else if (Random.Int( 3 ) == 0) {
			Charm c = Buff.affect( enemy, Charm.class, Charm.DURATION/2f );
			c.object = id();
			c.ignoreNextHit = true; //so that the -5 duration from succubus hit is ignored
			if (Dungeon.level.heroFOV[enemy.pos]) {
				enemy.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
				Sample.INSTANCE.play(Assets.Sounds.CHARMS);
			}
		}
		
		return damage;
	}
	
	@Override
	protected boolean getCloser( int target ) {
		if (fieldOfView[target] && Dungeon.level.distance( pos, target ) > 2 && blinkCooldown <= 0 && !rooted) {
			
			if (blink( target )) {
				spend(-1 / speed());
				return true;
			} else {
				return false;
			}
			
		} else {

			blinkCooldown--;
			return super.getCloser( target );
			
		}
	}
	

	// ===== Maze Tower (N2): 검은 잎의 시녀 =====

	//(사용자) 주인공을 한 번 매혹해 변신한 시녀는 매혹이 끝나도 변신한 모습으로 남는다 (SuccubusSprite.update가 켠다, 저장됨)
	public boolean mazeTransformed = false;

	//주인공이 이 시녀를 볼 수 있는 곳에서 마나를 써서 스킬을 쓸 때마다 그 마나만큼 보호막(Gorge, 피해를 막음)이 찬다.
	//(사용자) 70(N2_SHIELD_MAX, 스킬 3번째)이 차면 과식으로 터진다: 보호막이 사라지고 시녀는 기절(N2_STUN). 다른 몬스터는 피해를 받지 않는다.
	//스킬을 참고 평타로 잡거나, 몰아 써서 터뜨리는 두 갈래가 다 통한다. (사용자) 순간이동해 나타난 칸과 둘레 2~3칸에 검은 낙엽을 깐다.
	public static void mazeOnHeroSkill( int mana ){
		if (!com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_MAID || Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof Succubus) || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			((Succubus) m).mazeFeed( mana );
		}
	}

	private void mazeFeed( int mana ){
		Gorge g = Buff.affect( this, Gorge.class );
		g.fed += mana;
		g.incShield( mana );
		if (sprite != null && sprite.visible){
			sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( mana ), FloatingText.SHIELDING );
			sprite.emitter().burst( com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 4 );
		}
		if (g.fed >= com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_SHIELD_MAX) mazeBurst( g.fed );
	}

	private void mazeBurst( int amount ){
		Buff.detach( this, Gorge.class );
		if (sprite != null && sprite.visible){
			sprite.showStatus( CharSprite.WARNING, com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get( this, "burst" ) );
			sprite.emitter().burst( com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 16 );
			com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake( 1, 0.15f );
			Sample.INSTANCE.play( Assets.Sounds.BLAST, 0.8f );
		}
		Buff.prolong( this, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_STUN );
		//(사용자) 터질 때 둘레 1칸의 다른 적(몬스터)도 피해를 받는다 (스킬을 몰아 써서 적 무리 한가운데서 터뜨리는 전략)
		for (int n : PathFinder.NEIGHBOURS8){
			com.shatteredpixel.shatteredpixeldungeon.actors.Char ch = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar( pos + n );
			if (ch == null || ch == this || !ch.isAlive() || ch.alignment != Alignment.ENEMY) continue;
			ch.damage( Random.NormalIntRange( com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_BURST_MIN, com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_BURST_MAX ), this );
			if (ch.sprite != null && ch.sprite.visible) ch.sprite.emitter().burst( com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 6 );
		}
	}

	//그 칸과 둘레 2~3칸에 검은 낙엽
	private void mazeDropLeaves( int from ){
		if (!com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_MAID) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.drop( Dungeon.level, from );
		ArrayList<Integer> around = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) around.add( from + n );
		java.util.Collections.shuffle( around, new java.util.Random( Random.Long() ) );
		int k = Random.IntRange( com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_LEAF_DROP_MIN, com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_LEAF_DROP_MAX );
		for (int i = 0; i < around.size() && k > 0; i++){
			int c = around.get( i );
			if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c]) continue;
			com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.drop( Dungeon.level, c );
			k--;
		}
	}

	//스킬 마나를 받아먹어 차는 보호막 (fed = 지금까지 받아먹은 마나)
	public static class Gorge extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff {
		{ type = buffType.POSITIVE; }
		public int fed = 0;
		@Override public int icon() { return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.ARMOR; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ) { icon.hardlight( 0.35f, 0.25f, 0.3f ); }
		@Override public String iconTextDisplay() { return Integer.toString( fed ); }
		@Override public String desc() { return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get( this, "desc", fed, com.shatteredpixel.shatteredpixeldungeon.MazeTower.N2_SHIELD_MAX ); }
		@Override public void storeInBundle( Bundle bundle ){ super.storeInBundle( bundle ); bundle.put( "fed", fed ); }
		@Override public void restoreFromBundle( Bundle bundle ){ super.restoreFromBundle( bundle ); fed = bundle.getInt( "fed" ); }
	}

	private boolean blink( int target ) {
		
		Ballistica route = new Ballistica( pos, target, Ballistica.PROJECTILE);
		int cell = route.collisionPos;

		//can't occupy the same cell as another char, so move back one.
		if (Actor.findChar( cell ) != null && cell != this.pos)
			cell = route.path.get(route.dist-1);

		if (Dungeon.level.avoid[ cell ] || (properties().contains(Property.LARGE) && !Dungeon.level.openSpace[cell])){
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8) {
				cell = route.collisionPos + n;
				if (Dungeon.level.passable[cell]
						&& Actor.findChar( cell ) == null
						&& (!properties().contains(Property.LARGE) || Dungeon.level.openSpace[cell])) {
					candidates.add( cell );
				}
			}
			if (candidates.size() > 0)
				cell = Random.element(candidates);
			else {
				blinkCooldown = Random.IntRange(4, 6);
				return false;
			}
		}
		
		ScrollOfTeleportation.appear( this, cell );
		mazeDropLeaves( pos ); //Maze Tower (N2): (사용자) 나타난 칸과 둘레에 검은 낙엽

		blinkCooldown = Random.IntRange(4, 6);
		return true;
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 40;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 10);
	}

	@Override
	public Item createLoot() {
		Class<?extends Scroll> loot;
		do{
			loot = (Class<? extends Scroll>) Random.oneOf(Generator.Category.SCROLL.classes);
		} while (loot == ScrollOfIdentify.class || loot == ScrollOfUpgrade.class);

		return Reflection.newInstance(loot);
	}

	{
		immunities.add( Charm.class );
	}

	private static final String BLINK_CD = "blink_cd";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(BLINK_CD, blinkCooldown);
		bundle.put("maze_transformed", mazeTransformed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		blinkCooldown = bundle.getInt(BLINK_CD);
		mazeTransformed = bundle.getBoolean("maze_transformed");
	}
}
