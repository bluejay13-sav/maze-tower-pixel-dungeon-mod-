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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class HeroicLeap extends ArmorAbility {

	{
		baseChargeUse = 35f;
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public float chargeUse( Hero hero ) {
		float chargeUse = super.chargeUse(hero);
		if (hero.buff(DoubleJumpTracker.class) != null){
			if (MazeTower.ORIGINAL_TALENTS) {
				//reduced charge use by 16%/30%/41%/50%
				chargeUse *= Math.pow(0.84, hero.pointsInTalent(Talent.DOUBLE_JUMP));
			} else {
				//Maze Tower: 이중 도약 16/30/40/50%
				chargeUse *= 1f - MazeTower.T4_DOUBLE_JUMP[Math.min(4, hero.pointsInTalent(Talent.DOUBLE_JUMP))];
			}
		}
		return chargeUse;
	}

	@Override
	public void activate( ClassArmor armor, Hero hero, Integer target ) {
		if (target != null) {

			if (hero.rooted){
				PixelScene.shake( 1, 1f );
				return;
			}

			Ballistica route = new Ballistica(hero.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
			int cell = route.collisionPos;

			//can't occupy the same cell as another char, so move back one.
			int backTrace = route.dist-1;

			//Maze Tower: 최대 도약 거리
			if (!MazeTower.ORIGINAL_TALENTS && route.dist > MazeTower.LEAP_MAX_DIST){
				cell = route.path.get(MazeTower.LEAP_MAX_DIST);
				backTrace = MazeTower.LEAP_MAX_DIST - 1;
			}
			while (Actor.findChar( cell ) != null && cell != hero.pos) {
				cell = route.path.get(backTrace);
				backTrace--;
			}

			armor.charge -= chargeUse( hero );
			armor.updateQuickslot();

			final int dest = cell;
			hero.busy();
			hero.sprite.jump(hero.pos, cell, new Callback() {
				@Override
				public void call() {
					hero.move(dest);
					Dungeon.level.occupyCell(hero);
					Dungeon.observe();
					GameScene.updateFog();

					for (int i : PathFinder.NEIGHBOURS8) {
						Char mob = Actor.findChar(hero.pos + i);
						if (mob != null && mob != hero && mob.alignment != Char.Alignment.ALLY && !MazeTower.ORIGINAL_TALENTS) {
							//Maze Tower: 몸통 박치기 5 + 방어력 × 비율 (적 방어력은 원작처럼 뺀다), 충격파 2칸 + 취약 확률
							int slam = Math.min(4, hero.pointsInTalent(Talent.BODY_SLAM));
							if (slam > 0){
								int damage = MazeTower.T4_BODY_SLAM_FLAT + Math.round(hero.drRoll() * MazeTower.T4_BODY_SLAM_ARMOR[slam]);
								damage -= mob.drRoll();
								mob.damage(Math.max(0, damage), hero);
								if (mob.sprite != null) com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.SLAM_WAVE, mob.sprite);
							}
							int wave = Math.min(4, hero.pointsInTalent(Talent.IMPACT_WAVE));
							if (wave > 0 && mob.isAlive() && mob.pos == hero.pos + i){
								if (slam <= 0 && mob.sprite != null) com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.SLAM_WAVE, mob.sprite);
								Ballistica trajectory = new Ballistica(mob.pos, mob.pos + i, Ballistica.MAGIC_BOLT);
								WandOfBlastWave.throwChar(mob, trajectory, MazeTower.T4_IMPACT_PUSH, true, true, HeroicLeap.this);
								if (Random.Float() < MazeTower.T4_IMPACT_VULN[wave]){
									Buff.prolong(mob, Vulnerable.class, MazeTower.T4_IMPACT_VULN_TURNS);
								}
							}
						} else if (mob != null && mob != hero && mob.alignment != Char.Alignment.ALLY) {
							if (hero.hasTalent(Talent.BODY_SLAM)){
								int damage = Hero.heroDamageIntRange(hero.pointsInTalent(Talent.BODY_SLAM), 4*hero.pointsInTalent(Talent.BODY_SLAM));
								damage += Math.round(hero.drRoll()*0.25f*hero.pointsInTalent(Talent.BODY_SLAM));
								damage -= mob.drRoll();
								mob.damage(damage, hero);
							}
							if (mob.pos == hero.pos + i && hero.hasTalent(Talent.IMPACT_WAVE)){
								Ballistica trajectory = new Ballistica(mob.pos, mob.pos + i, Ballistica.MAGIC_BOLT);
								int strength = 1+hero.pointsInTalent(Talent.IMPACT_WAVE);
								WandOfBlastWave.throwChar(mob, trajectory, strength, true, true, HeroicLeap.this);
								if (Random.Int(4) < hero.pointsInTalent(Talent.IMPACT_WAVE)){
									Buff.prolong(mob, Vulnerable.class, 5f);
								}
							}
						}
					}

					if (MazeTower.ORIGINAL_TALENTS){
						WandOfBlastWave.BlastWave.blast(dest);
						PixelScene.shake(2, 0.5f);
					} else {
						//Maze Tower (2026-11-21): 다시 그린 착지 그림 (fx3_16), 몸통 박치기·충격파를 받은 적에게 fx3_17, 흔들림 1픽셀
						com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.atCell(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.LEAP, dest);
						PixelScene.shake(1, 0.1f);
					}

					Invisibility.dispel();
					hero.spendAndNext(Actor.TICK);

					if (hero.buff(DoubleJumpTracker.class) != null){
						hero.buff(DoubleJumpTracker.class).detach();
					} else {
						if (hero.hasTalent(Talent.DOUBLE_JUMP)) {
							Buff.affect(hero, DoubleJumpTracker.class, MazeTower.T4_DOUBLE_JUMP_TURNS);
						}
					}
				}
			});
		}
	}

	public static class DoubleJumpTracker extends FlavourBuff{};

	@Override
	public int icon() {
		return HeroIcon.HEROIC_LEAP;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BODY_SLAM, Talent.IMPACT_WAVE, Talent.DOUBLE_JUMP, Talent.HEROIC_ENERGY};
	}
}
