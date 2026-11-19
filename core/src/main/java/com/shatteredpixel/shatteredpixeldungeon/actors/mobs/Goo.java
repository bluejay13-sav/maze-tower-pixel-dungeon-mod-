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
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SapPuddle;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.BloomCore;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GooSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Goo extends Mob {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 120 : 100;
		EXP = 10;
		defenseSkill = 8;
		spriteClass = GooSprite.class;

		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
		properties.add(Property.ACIDIC);
	}

	private int pumpedUp = 0;
	private int healInc = 1;

	// ===== Maze Tower: 5층 썩은 수액 덩어리 (기획 문서) =====
	//  - 물리 피해(무기·투척·스킬·필살기·4차 능력 = 출처가 캐릭터인 피해)를 80% 덜 받는다
	//  - 횃불을 맞으면(torched) 체력이 절반이 될 때까지 계속 불타고, 그동안 물리 피해 감소가 사라지며 방어력 0,
	//    수액을 더는 만들지 못한다. 체력이 절반 아래면 불이 꺼지고 물리 피해를 30% 더 받는다.
	//  - 불타는 동안 회복하지 못한다. 젖음에 걸리지 않는다.
	//  - 힘을 모아 내리칠 때 주변에 수액 웅덩이(SapPuddle)를 남기고, 수액 위에서 물처럼 회복, 수액 칸으로 옮기면 체력 +10
	private boolean torched = false;

	{
		if (MazeBosses.ENABLED) immunities.add(Wet.class);
	}

	private boolean maze(){
		return MazeBosses.ENABLED;
	}

	//BossTorch가 맞혔을 때. 이미 불붙어 있거나 체력이 절반 아래면 효과 없음(false)
	public boolean torchHit(){
		if (!maze() || torched || HP*2 <= HT) return false;
		torched = true;
		Buff.affect(this, Burning.class).reignite(this);
		if (sprite != null){
			sprite.emitter().burst(FlameParticle.FACTORY, 20);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "maze_torched_status"));
		}
		GLog.p(Messages.get(this, "maze_torched"));
		return true;
	}

	//물리 피해 배율
	private float physicalMulti(){
		if (HP*2 <= HT) return 1f + MazeTower.SAP_HALF_PHYS_BONUS;
		if (torched)    return 1f;
		return 1f - MazeTower.SAP_PHYS_REDUCE;
	}

	//내려칠 때 주변 3×3의 빈 바닥 몇 칸에 수액 웅덩이
	private void spillSap(){
		if (!maze() || torched) return;
		java.util.ArrayList<Integer> cells = new java.util.ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9){
			int c = pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.pit[c] && !Dungeon.level.solid[c]) cells.add(c);
		}
		int count = Random.IntRange(MazeTower.SAP_PUDDLE_MIN, MazeTower.SAP_PUDDLE_MAX);
		while (count-- > 0 && !cells.isEmpty()){
			int c = cells.remove(Random.Int(cells.size()));
			GameScene.add(Blob.seed(c, MazeTower.SAP_PUDDLE_TURNS, SapPuddle.class));
		}
	}

	//물 또는 수액 위
	private boolean onHealingGround(){
		return !flying && (Dungeon.level.water[pos] || (maze() && SapPuddle.at(pos)));
	}

	@Override
	public void move( int step, boolean travelling ) {
		super.move(step, travelling);
		//수액 칸으로 옮기면 체력 +10 (불타는 동안은 회복하지 못한다)
		if (maze() && SapPuddle.at(pos) && HP < HT && buff(Burning.class) == null){
			int heal = Math.min(MazeTower.SAP_MOVE_HEAL, HT - HP);
			HP += heal;
			if (Dungeon.level.heroFOV[pos] && sprite != null){
				sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				BossEffects.atCellBottom(BossEffects.Fx.SAP_HEAL, pos, 10, false);
			}
		}
	}

	@Override
	public int damageRoll() {
		int min = 1;
		int max = (HP*2 <= HT) ? 12 : 8;
		if (pumpedUp > 0) {
			pumpedUp = 0;
			if (enemy == Dungeon.hero) {
				Statistics.qualifiedForBossChallengeBadge = false;
				Statistics.bossScores[0] -= 100;
			}
			return Random.NormalIntRange( min*3, max*3 );
		} else {
			return Random.NormalIntRange( min, max );
		}
	}

	@Override
	public int attackSkill( Char target ) {
		int attack = 10;
		if (HP*2 <= HT) attack = 15;
		if (pumpedUp > 0) attack *= 2;
		return attack;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return (int)(super.defenseSkill(enemy) * ((HP*2 <= HT)? 1.5 : 1));
	}

	@Override
	public int drRoll() {
		if (maze() && torched) return 0; //Maze Tower: 횃불에 맞아 불타는 동안 방어력 0
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	@Override
	public boolean act() {

		if (state != HUNTING && pumpedUp > 0){
			pumpedUp = 0;
			sprite.idle();
		}

		//Maze Tower: 횃불 효과 - 체력이 절반이 될 때까지 계속 불탄다
		if (maze() && torched){
			if (HP*2 <= HT){
				torched = false;
				Buff.detach(this, Burning.class);
				if (Dungeon.level.heroFOV[pos]) GLog.p(Messages.get(this, "maze_torch_out"));
			} else if (buff(Burning.class) == null){
				Buff.affect(this, Burning.class).reignite(this);
			}
		}

		//Maze Tower: 물뿐 아니라 수액 위에서도 회복하고, 불타는 동안은 회복하지 못한다
		boolean heals = maze() ? onHealingGround() && buff(Burning.class) == null
				: !flying && Dungeon.level.water[pos];
		if (heals && HP < HT) {
			HP += healInc;
			Statistics.qualifiedForBossChallengeBadge = false;

			LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
			if (lock != null){
				if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.removeTime(healInc);
				else                                                    lock.removeTime(healInc*1.5f);
			}

			if (Dungeon.level.heroFOV[pos] ){
				sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healInc), FloatingText.HEALING );
			}
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) && healInc < 3) {
				healInc++;
			}
			if (HP*2 > HT) {
				BossHealthBar.bleed(false);
				((GooSprite)sprite).spray(false);
				HP = Math.min(HP, HT);
			}
		} else {
			healInc = 1;
		}
		
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		return super.act();
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (pumpedUp > 0){
			//we check both from and to in this case as projectile logic isn't always symmetrical.
			//this helps trim out BS edge-cases
			return Dungeon.level.distance(enemy.pos, pos) <= 2
						&& new Ballistica( pos, enemy.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == enemy.pos
						&& new Ballistica( enemy.pos, pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == pos;
		} else {
			return super.canAttack(enemy);
		}
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (Random.Int( 3 ) == 0) {
			Buff.affect( enemy, Ooze.class ).set( Ooze.DURATION );
			enemy.sprite.burst( 0x000000, 5 );
		}

		if (pumpedUp > 0) {
			PixelScene.shake( 3, 0.2f );
		}

		return damage;
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();

		if (pumpedUp > 0){
			((GooSprite)sprite).pumpUp( pumpedUp );
		}
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (pumpedUp == 1) {
			pumpedUp++;
			((GooSprite)sprite).pumpUp( pumpedUp );
			if (maze() && Dungeon.level.heroFOV[pos]) BossEffects.onChar(BossEffects.Fx.SAP_CHARGE, sprite, 8, false); //Maze Tower: 힘 모으기

			spend( attackDelay() );

			return true;
		} else if (pumpedUp >= 2 || Random.Int( (HP*2 <= HT) ? 2 : 5 ) > 0) {

			boolean visible = Dungeon.level.heroFOV[pos];

			if (visible) {
				if (pumpedUp >= 2) {
					((GooSprite) sprite).pumpAttack();
				} else {
					sprite.attack(enemy.pos);
				}
			} else {
				if (pumpedUp >= 2){
					((GooSprite)sprite).triggerEmitters();
				}
				attack( enemy );
				Invisibility.dispel(this);
				spend( attackDelay() );
			}

			return !visible;

		} else {

			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
				pumpedUp += 2;
				//don't want to overly punish players with slow move or attack speed
				spend(GameMath.gate(attackDelay(), (int)Math.ceil(enemy.cooldown()), 3*attackDelay()));
			} else {
				pumpedUp++;
				spend( attackDelay() );
			}

			if (maze() && Dungeon.level.heroFOV[pos]) BossEffects.onChar(BossEffects.Fx.SAP_CHARGE, sprite, 8, false); //Maze Tower: 힘 모으기
			((GooSprite)sprite).pumpUp( pumpedUp );

			if (Dungeon.level.heroFOV[pos]) {
				sprite.showStatus( CharSprite.WARNING, Messages.get(this, "!!!") );
				GLog.n( Messages.get(this, "pumpup") );
			}

			return true;
		}
	}

	@Override
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {
		//Maze Tower: 힘을 모아 내리치면 3×3 내려치기 이펙트와 수액 웅덩이
		if (maze() && pumpedUp > 0){
			if (Dungeon.level.heroFOV[pos]) BossEffects.atCell(BossEffects.Fx.SAP_SLAM, pos, 15, false);
			spillSap();
		}
		boolean result = super.attack( enemy, dmgMulti, dmgBonus, accMulti );
		if (pumpedUp > 0) {
			pumpedUp = 0;
			if (enemy == Dungeon.hero) {
				Statistics.qualifiedForBossChallengeBadge = false;
				Statistics.bossScores[0] -= 100;
			}
		}
		return result;
	}

	@Override
	protected boolean getCloser( int target ) {
		if (pumpedUp != 0) {
			pumpedUp = 0;
			sprite.idle();
		}
		return super.getCloser( target );
	}

	@Override
	protected boolean getFurther(int target) {
		if (pumpedUp != 0) {
			pumpedUp = 0;
			sprite.idle();
		}
		return super.getFurther( target );
	}

	@Override
	public void damage(int dmg, Object src) {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
		}
		boolean bleeding = (HP*2 <= HT);
		//Maze Tower: 물리 피해 (출처가 캐릭터: 무기·투척·스킬·필살기·4차 능력) 배율
		if (maze() && src instanceof Char){
			dmg = Math.round(dmg * physicalMulti());
		}
		super.damage(dmg, src);
		if ((HP*2 <= HT) && !bleeding){
			BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "enraged"));
			((GooSprite)sprite).spray(true);
			yell(Messages.get(this, "gluuurp"));
		}
		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.addTime(dmg);
			else                                                    lock.addTime(dmg*1.5f);
		}
	}

	@Override
	public void die( Object cause ) {
		Dungeon.level.mazeBossPos = pos; //Maze Tower: 보스가 쓰러진 자리 (역행 중 흔적)
		
		super.die( cause );
		
		Dungeon.level.unseal();
		
		GameScene.bossSlain();
		Dungeon.level.drop( new WornKey( Dungeon.depth ), pos ).sprite.drop();

		//Maze Tower: 2차 전직 아이템 개화의 핵
		if (MazeTower.GOO_DROPS_BLOOM_CORE) {
			Dungeon.level.drop( new BloomCore(), pos ).sprite.drop();
		}
		
		//60% chance of 2 blobs, 30% chance of 3, 10% chance for 4. Average of 2.5
		int blobs = Random.chances(new float[]{0, 0, 6, 3, 1});
		for (int i = 0; i < blobs; i++){
			int ofs;
			do {
				ofs = PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[pos + ofs]);
			Dungeon.level.drop( new GooBlob(), pos + ofs ).sprite.drop( pos );
		}
		
		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[0] += 1000;
		
		yell( Messages.get(this, "defeated") );
	}
	
	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			yell(Messages.get(this, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	private final String PUMPEDUP = "pumpedup";
	private final String HEALINC = "healinc";

	@Override
	public void storeInBundle( Bundle bundle ) {

		super.storeInBundle( bundle );

		bundle.put( PUMPEDUP , pumpedUp );
		bundle.put( HEALINC, healInc );
		bundle.put( "maze_torched", torched ); //Maze Tower
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {

		super.restoreFromBundle( bundle );

		pumpedUp = bundle.getInt( PUMPEDUP );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if ((HP*2 <= HT)) BossHealthBar.bleed(true);

		healInc = bundle.getInt(HEALINC);
		torched = bundle.getBoolean("maze_torched"); //Maze Tower
	}
	
}
