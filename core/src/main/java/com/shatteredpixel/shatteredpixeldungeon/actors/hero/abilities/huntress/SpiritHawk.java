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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class SpiritHawk extends ArmorAbility {

	@Override
	public String targetingPrompt() {
		if (getHawk() == null) {
			return super.targetingPrompt();
		} else {
			return Messages.get(this, "prompt");
		}
	}

	@Override
	public boolean useTargeting(){
		return false;
	}

	{
		baseChargeUse = 35f;
	}

	@Override
	public float chargeUse(Hero hero) {
		if (getHawk() == null) {
			return super.chargeUse(hero);
		} else {
			return 0;
		}
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		HawkAlly ally = getHawk();

		if (ally != null){
			if (target == null){
				return;
			} else {
				ally.directTocell(target);
			}
		} else {
			ArrayList<Integer> spawnPoints = new ArrayList<>();
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				int p = hero.pos + PathFinder.NEIGHBOURS8[i];
				if (Actor.findChar(p) == null && (Dungeon.level.passable[p] || Dungeon.level.avoid[p])) {
					spawnPoints.add(p);
				}
			}

			if (!spawnPoints.isEmpty()){
				armor.charge -= chargeUse(hero);
				armor.updateQuickslot();

				ally = new HawkAlly();
				if (!MAZE_ORIGINAL()) ally.timeRemaining = com.shatteredpixel.shatteredpixeldungeon.MazeTower.HAWK_TURNS + com.shatteredpixel.shatteredpixeldungeon.MazeTower.T4_HAWK_LINGER[Math.min(4, hero.pointsInTalent(Talent.HAWK_LINGER))]; //Maze Tower: 정령 매 지속
				ally.pos = Random.element(spawnPoints);
				GameScene.add(ally);

				ScrollOfTeleportation.appear(ally, ally.pos);
				//Maze Tower (2026-11-21): 꽃잎이 모여 매가 된다 (fx_hunter_32)
				if (!MAZE_ORIGINAL() && ally.sprite != null) com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.H_HAWK_IN, ally.sprite);
				Dungeon.observe();

				Invisibility.dispel();
				hero.spendAndNext(Actor.TICK);

			} else {
				GLog.w(Messages.get(this, "no_space"));
			}
		}

	}

	@Override
	public int icon() {
		return HeroIcon.SPIRIT_HAWK;
	}

	@Override
	public Talent[] talents() {
		if (!MAZE_ORIGINAL()) return new Talent[]{Talent.HAWK_LINGER, Talent.HAWK_EYE, Talent.HUNT_SIGNAL, Talent.HEROIC_ENERGY}; //Maze Tower: 사냥꾼 4차 정령 매 특성
		return new Talent[]{Talent.EAGLE_EYE, Talent.GO_FOR_THE_EYES, Talent.SWIFT_SPIRIT, Talent.HEROIC_ENERGY};
	}

	//Maze Tower: 원작 정령 매 그대로 쓸지 (false면 사냥꾼 4차 결실 규칙)
	static boolean MAZE_ORIGINAL(){
		return com.shatteredpixel.shatteredpixeldungeon.MazeTower.ORIGINAL_ARMOR_ABILITY;
	}

	//Maze Tower: 매의 눈 (시야 +칸)
	static int eyeBonus(){
		if (MAZE_ORIGINAL()) return Dungeon.hero.pointsInTalent(Talent.EAGLE_EYE);
		return com.shatteredpixel.shatteredpixeldungeon.MazeTower.T4_HAWK_EYE[Math.min(4, Dungeon.hero.pointsInTalent(Talent.HAWK_EYE))];
	}

	private static HawkAlly getHawk(){
		for (Char ch : Actor.chars()){
			if (ch instanceof HawkAlly){
				return (HawkAlly) ch;
			}
		}
		return null;
	}

	public static class HawkAlly extends DirectableAlly {

		{
			spriteClass = HawkSprite.class;

			HP = HT = 10;
			defenseSkill = 60;

			flying = true;
			if (Dungeon.hero != null) {
				viewDistance = MAZE_ORIGINAL() ? (int) GameMath.gate(6, 6 + Dungeon.hero.pointsInTalent(Talent.EAGLE_EYE), 8) : 6 + eyeBonus(); //Maze Tower: 매의 눈
				baseSpeed = 2f + Dungeon.hero.pointsInTalent(Talent.SWIFT_SPIRIT) / 2f;
			} else {
				viewDistance = 6;
				baseSpeed = 2f;
			}
			attacksAutomatically = false;

			immunities.addAll(new BlobImmunity().immunities());
			immunities.add(AllyBuff.class);
		}

		@Override
		public int attackSkill(Char target) {
			return 60;
		}

		private int dodgesUsed = 0;
		private float timeRemaining = 100f;

		@Override
		public int defenseSkill(Char enemy) {
			if (Dungeon.hero.hasTalent(Talent.SWIFT_SPIRIT) &&
					dodgesUsed < 2*Dungeon.hero.pointsInTalent(Talent.SWIFT_SPIRIT)) {
				dodgesUsed++;
				return Char.INFINITE_EVASION;
			}
			return super.defenseSkill(enemy);
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(5, 10);
		}

		@Override
		public int attackProc(Char enemy, int damage) {
			damage = super.attackProc( enemy, damage );
			//Maze Tower: 쪼으면 실명, 사냥 신호 (원작 특성 대신)
			if (!MAZE_ORIGINAL()){
				Buff.prolong( enemy, Blindness.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.HAWK_BLIND );
				int sig = Math.min(4, Dungeon.hero.pointsInTalent(Talent.HUNT_SIGNAL));
				if (sig > 0 && enemy.isAlive()){
					com.shatteredpixel.shatteredpixeldungeon.actors.hero.HunterEffects.HawkSignal s = Buff.prolong(enemy, com.shatteredpixel.shatteredpixeldungeon.actors.hero.HunterEffects.HawkSignal.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.HAWK_SIGNAL_TURNS);
					s.bonus = com.shatteredpixel.shatteredpixeldungeon.MazeTower.T4_HUNT_SIGNAL[sig];
				}
				return damage;
			}
			switch (Dungeon.hero.pointsInTalent(Talent.GO_FOR_THE_EYES)){
				case 1:
					Buff.prolong( enemy, Blindness.class, 2);
					break;
				case 2:
					Buff.prolong( enemy, Blindness.class, 5);
					break;
				case 3:
					Buff.prolong( enemy, Blindness.class, 5);
					Buff.prolong( enemy, Cripple.class, 2);
					break;
				case 4:
					Buff.prolong( enemy, Blindness.class, 5);
					Buff.prolong( enemy, Cripple.class, 5);
					break;
				default:
					//do nothing
			}

			return damage;
		}

		@Override
		protected boolean act() {
			if (timeRemaining <= 0){
				die(null);
				Dungeon.hero.interrupt();
				return true;
			}
			viewDistance = 6 + eyeBonus(); //Maze Tower: 매의 눈
			baseSpeed = 2f + Dungeon.hero.pointsInTalent(Talent.SWIFT_SPIRIT)/2f;
			boolean result = super.act();
			Dungeon.level.updateFieldOfView( this, fieldOfView );
			GameScene.updateFog(pos, viewDistance+(int)Math.ceil(speed()));
			//Maze Tower: 매의 눈 4레벨 - 매가 지나간 곳의 함정·비밀문을 찾아낸다
			if (!MAZE_ORIGINAL() && Dungeon.hero.pointsInTalent(Talent.HAWK_EYE) >= 4){
				for (int n : PathFinder.NEIGHBOURS9){
					int c = pos + n;
					if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.secret[c]) Dungeon.level.discover(c);
				}
			}
			return result;
		}

		@Override
		public void die(Object cause) {
			flying = false;
			super.die(cause);
		}

		@Override
		protected void spend(float time) {
			super.spend(time);
			timeRemaining -= time;
		}

		@Override
		public void destroy() {
			super.destroy();
			Dungeon.observe();
			GameScene.updateFog();
		}

		@Override
		public void defendPos(int cell) {
			GLog.i(Messages.get(this, "direct_defend"));
			super.defendPos(cell);
		}

		@Override
		public void followHero() {
			GLog.i(Messages.get(this, "direct_follow"));
			super.followHero();
		}

		@Override
		public void targetChar(Char ch) {
			GLog.i(Messages.get(this, "direct_attack"));
			super.targetChar(ch);
		}

		@Override
		public String description() {
			String message = Messages.get(this, "desc", (int)timeRemaining);
			if (Actor.chars().contains(this)){
				message += "\n\n" + Messages.get(this, "desc_remaining", (int)timeRemaining);
				if (dodgesUsed < 2*Dungeon.hero.pointsInTalent(Talent.SWIFT_SPIRIT)){
					message += "\n" + Messages.get(this, "desc_dodges", (2*Dungeon.hero.pointsInTalent(Talent.SWIFT_SPIRIT) - dodgesUsed));
				}
			}
			return message;
		}

		private static final String DODGES_USED     = "dodges_used";
		private static final String TIME_REMAINING  = "time_remaining";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DODGES_USED, dodgesUsed);
			bundle.put(TIME_REMAINING, timeRemaining);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			dodgesUsed = bundle.getInt(DODGES_USED);
			timeRemaining = bundle.getFloat(TIME_REMAINING);
		}
	}

	public static class HawkSprite extends MobSprite {

		public HawkSprite() {
			super();

			//Maze Tower (2026-11-21): 새 정령 매 그림 (푸른빛, fx_hunter_31 날기 4프레임 반복).
			//(결정) 쪼기 동작 없이 날기 프레임 그대로 부딪히고 피해 숫자만. 사라질 때 꽃잎으로 흩어진다(fx_hunter_33), 몸 뒤에 은은한 빛(fx_hunter_34)
			if (!MAZE_ORIGINAL()){
				texture( "sprites/maze_fx_hunter_31.png" );
				TextureFilm f = new TextureFilm( texture, 16, 16 );
				idle = new Animation( 10, true );
				idle.frames( f, 0, 1, 2, 3 );
				run = idle.clone();
				attack = new Animation( 12, false );
				attack.frames( f, 0, 1, 2, 3 );
				die = new Animation( 20, false );
				die.frames( f, 0 );
				play( idle );
				return;
			}

			texture( Assets.Sprites.SPIRIT_HAWK );

			TextureFilm frames = new TextureFilm( texture, 15, 15 );

			int c = 0;

			idle = new Animation( 6, true );
			idle.frames( frames, 0, 1 );

			run = new Animation( 8, true );
			run.frames( frames, 0, 1 );

			attack = new Animation( 12, false );
			attack.frames( frames, 2, 3, 0, 1 );

			die = new Animation( 12, false );
			die.frames( frames, 4, 5, 6 );

			play( idle );
		}

		@Override
		public int blood() {
			return 0xFF00FFFF;
		}

		private com.watabou.noosa.Image glow; //Maze Tower: 매 몸 뒤의 빛

		@Override
		public void update() {
			super.update();
			if (MAZE_ORIGINAL() || parent == null) return;
			if (glow == null){
				glow = new com.watabou.noosa.Image("sprites/maze_fx_hunter_34.png");
				parent.addToBack(glow);
			}
			com.watabou.utils.PointF c = center();
			glow.x = c.x - 16;
			glow.y = c.y - 16;
			glow.visible = visible && (ch == null || ch.isAlive());
		}

		@Override
		public void kill() {
			if (glow != null){ glow.killAndErase(); glow = null; }
			super.kill();
		}

		@Override
		public void die() {
			if (!MAZE_ORIGINAL()){
				//꽃잎으로 흩어진다
				com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.H_HAWK_OUT, this);
				if (glow != null){ glow.killAndErase(); glow = null; }
				super.die();
				alpha(0);
				return;
			}
			super.die();
			emitter().start( ShaftParticle.FACTORY, 0.3f, 4 );
			emitter().start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );
		}
	}
}
