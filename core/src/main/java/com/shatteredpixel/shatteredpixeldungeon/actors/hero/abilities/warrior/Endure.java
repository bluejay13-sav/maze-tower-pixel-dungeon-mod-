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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

public class Endure extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {

		if (hero.buff(EndureTracker.class) != null){
			hero.buff(EndureTracker.class).detach();
		}
		//Maze Tower: 버티기 3턴 + 끝난 뒤 10턴 (원작 12턴)
		Buff.prolong(hero, EndureTracker.class, MazeTower.ORIGINAL_TALENTS ? 12f : 3f + MazeTower.T4_ENDURE_AFTER);

		Combo combo = hero.buff(Combo.class);
		if (combo != null){
			combo.addTime(3f);
		}
		hero.sprite.operate(hero.pos);

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Invisibility.dispel();
		hero.spendAndNext(3f);
	}

	public static class EndureTracker extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public boolean enduring = true;

		//Maze Tower (2026-11-21): 버티는 동안 몸 둘레 반복 그림 (fx3_18)
		private com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Loop endureFx;

		@Override
		public void fx(boolean on) {
			if (on && enduring && !MazeTower.ORIGINAL_TALENTS){
				if (endureFx == null && target.sprite != null) endureFx = com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Loop.on(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.ENDURE, target.sprite);
			} else if (endureFx != null){
				endureFx.stop();
				endureFx = null;
			}
		}

		public int damageBonus = 0;
		public int hitsLeft = 0;

		//Maze Tower: 버티는 동안에도 아이콘을 보여 주고, 남은 턴을 숫자로 표시한다
		//(버티는 동안 = 회색빛 방패, 끝난 뒤 추가 피해를 줄 수 있는 동안 = 붉은 방패)
		private float enduringLeft(){
			return Math.max(0, visualcooldown() - MazeTower.T4_ENDURE_AFTER);
		}

		@Override
		public int icon() {
			if (!MazeTower.ORIGINAL_TALENTS) return BuffIndicator.ARMOR;
			return enduring ? BuffIndicator.NONE : BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			if (!MazeTower.ORIGINAL_TALENTS && enduring){
				icon.hardlight(0.6f, 0.75f, 1f);
				return;
			}
			icon.hardlight(1, 0, 0);
		}

		@Override
		public float iconFadePercent() {
			if (!MazeTower.ORIGINAL_TALENTS){
				if (enduring) return Math.max(0, (3f - enduringLeft()) / 3f);
				return Math.max(0, (MazeTower.T4_ENDURE_AFTER - visualcooldown()) / MazeTower.T4_ENDURE_AFTER);
			}
			return Math.max(0, (10f - visualcooldown()) / 10f);
		}

		@Override
		public String iconTextDisplay() {
			if (!MazeTower.ORIGINAL_TALENTS && enduring) return Integer.toString((int)Math.ceil(enduringLeft()));
			return super.iconTextDisplay();
		}

		@Override
		public String desc() {
			if (!MazeTower.ORIGINAL_TALENTS){
				if (enduring) return Messages.get(this, "maze_desc_enduring", dispTurns(enduringLeft()), damageBonus);
				return Messages.get(this, "maze_desc", damageBonus, hitsLeft, dispTurns(visualcooldown()));
			}
			return Messages.get(this, "desc", damageBonus, hitsLeft);
		}

		@Override
		public String name() {
			if (!MazeTower.ORIGINAL_TALENTS && enduring) return Messages.get(this, "maze_name_enduring");
			return super.name();
		}

		public float adjustDamageTaken(float damage){
			if (enduring) {
				damageBonus += damage/2;

				//Maze Tower: 무시하기 50→55/60/65/70% 감소, 가시갑옷 (받은 피해 일부를 주변 1칸의 적에게)
				if (!MazeTower.ORIGINAL_TALENTS){
					Hero hero = Dungeon.hero;
					thorns(hero, damage);
					return damage * (1f - MazeTower.T4_SHRUG_REDUCE[Math.min(4, hero.pointsInTalent(Talent.SHRUG_IT_OFF))]);
				}

				float damageMulti = 0.5f;
				if (Dungeon.hero.hasTalent(Talent.SHRUG_IT_OFF)){
					//total damage reduction is 60%/68%/74%/80%, based on points in talent
					damageMulti *= Math.pow(0.8f, Dungeon.hero.pointsInTalent(Talent.SHRUG_IT_OFF));
				}

				return damage*damageMulti;
			}
			return damage;
		}

		public void endEnduring(){
			if (!enduring){
				return;
			}

			enduring = false;
			fx(false);

			//Maze Tower: 지속적인 응징 - 추가 피해를 2~3회로 나눠 총 120/150/180/210%
			if (!MazeTower.ORIGINAL_TALENTS){
				int lvl = Math.min(4, Dungeon.hero.pointsInTalent(Talent.SUSTAINED_RETRIBUTION));
				hitsLeft = MazeTower.T4_RETRIB_HITS[lvl];
				damageBonus = Math.round(damageBonus * MazeTower.T4_RETRIB_TOTAL[lvl] / hitsLeft);
				if (damageBonus > 0) {
					target.sprite.centerEmitter().start( Speck.factory( Speck.SCREAM ), 0.3f, 3 );
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
					SpellSprite.show(target, SpellSprite.BERSERK);
				} else {
					detach();
				}
				return;
			}

			damageBonus *= 1f + 0.15f*Dungeon.hero.pointsInTalent(Talent.SUSTAINED_RETRIBUTION);

			int nearby = 0;
			for (Char ch : Actor.chars()){
				if (ch.alignment == Char.Alignment.ENEMY && Dungeon.level.distance(target.pos, ch.pos) <= 2){
					nearby ++;
				}
			}
			damageBonus *= 1f + (nearby*0.05f*Dungeon.hero.pointsInTalent(Talent.EVEN_THE_ODDS));

			hitsLeft = 1+Dungeon.hero.pointsInTalent(Talent.SUSTAINED_RETRIBUTION);
			damageBonus /= hitsLeft;

			if (damageBonus > 0) {
				target.sprite.centerEmitter().start( Speck.factory( Speck.SCREAM ), 0.3f, 3 );
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				SpellSprite.show(target, SpellSprite.BERSERK);
			} else {
				detach();
			}
		}

		//Maze Tower: 가시갑옷 - 버티는 동안 받은 피해(경감 전, 방어력 계산 전)의 일부를 주변 1칸의 적에게
		private void thorns(Hero hero, float damage){
			int lvl = Math.min(4, hero.pointsInTalent(Talent.EVEN_THE_ODDS));
			if (lvl <= 0) return;
			//반올림해 0이 되어도 피해 0이 들어갔다고 숫자를 보여 준다
			int amt = Math.max(0, Math.round(damage * MazeTower.T4_THORNS[lvl]));
			for (int i : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(target.pos + i);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
					ch.damage(amt, hero);
					if (ch.sprite != null) ch.sprite.flash();
					if (ch.sprite != null) com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.THORNS, ch.sprite); //(2026-11-21) 가시갑옷 fx3_20
				}
			}
		}

		public float damageFactor(float damage){
			if (enduring){
				return damage;
			} else {
				int bonusDamage = damageBonus;
				hitsLeft--;

				if (hitsLeft <= 0){
					detach();
				}
				return damage + bonusDamage;
			}
		}

		public static String ENDURING       = "enduring";
		public static String DAMAGE_BONUS   = "damage_bonus";
		public static String HITS_LEFT      = "hits_left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(ENDURING, enduring);
			bundle.put(DAMAGE_BONUS, damageBonus);
			bundle.put(HITS_LEFT, hitsLeft);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			enduring = bundle.getBoolean(ENDURING);
			damageBonus = bundle.getInt(DAMAGE_BONUS);
			hitsLeft = bundle.getInt(HITS_LEFT);
		}
	};

	@Override
	public int icon() {
		return HeroIcon.ENDURE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SUSTAINED_RETRIBUTION, Talent.SHRUG_IT_OFF, Talent.EVEN_THE_ODDS, Talent.HEROIC_ENERGY};
	}
}
