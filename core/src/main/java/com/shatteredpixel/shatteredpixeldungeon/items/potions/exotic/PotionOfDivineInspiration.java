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

package com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.BloomTalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class PotionOfDivineInspiration extends ExoticPotion {
	
	{
		icon = ItemSpriteSheet.Icons.POTION_DIVINE;

		talentFactor = 2f;
	}

	protected static boolean identifiedByUse = false;

	@Override
	//need to override drink so that time isn't spent right away
	protected void drink(final Hero hero) {

		if (!isKnown()) {
			identify();
			curItem = detach( hero.belongings.backpack );
			identifiedByUse = true;
		} else {
			identifiedByUse = false;
		}

		//Maze Tower: 원작 특성 티어 대신 열려 있는 1차/2차 줄에 포인트
		if (!MazeTower.ORIGINAL_TALENTS){
			mazeDrink(hero);
			return;
		}

		boolean[] enabled = new boolean[5];
		enabled[1] = enabled[2] = enabled[3] = enabled[4] = true;

		DivineInspirationTracker tracker = hero.buff(DivineInspirationTracker.class);

		if (tracker != null){
			boolean allBoosted = true;
			for (int i = 1; i <= 4; i++){
				if (tracker.isBoosted(i)){
					enabled[i] = false;
				} else {
					allBoosted = false;
				}
			}

			if (allBoosted){
				GLog.w(Messages.get(this, "no_more_points"));
				return;
			}
		}

		GameScene.show(new WndOptions(
				new ItemSprite(this),
				Messages.titleCase(trueName()),
				Messages.get(PotionOfDivineInspiration.class, "select_tier"),
				Messages.titleCase(Messages.get(TalentsPane.class, "tier", 1)),
				Messages.titleCase(Messages.get(TalentsPane.class, "tier", 2)),
				Messages.titleCase(Messages.get(TalentsPane.class, "tier", 3)),
				Messages.titleCase(Messages.get(TalentsPane.class, "tier", 4))
		){
			@Override
			protected boolean enabled(int index) {
				return enabled[index+1];
			}

			@Override
			protected void onSelect(int index) {
				super.onSelect(index);

				if (index != -1){
					Buff.affect(curUser, DivineInspirationTracker.class).setBoosted(index+1);

					if (!identifiedByUse) {
						curItem.detach(curUser.belongings.backpack);
					}
					identifiedByUse = false;

					curUser.busy();
					curUser.sprite.operate(curUser.pos);

					curUser.spendAndNext(1f);

					boolean unspentTalents = false;
					for (int i = 1; i <= Dungeon.hero.talents.size(); i++){
						if (Dungeon.hero.talentPointsAvailable(i) > 0){
							unspentTalents = true;
							break;
						}
					}
					if (unspentTalents){
						StatusPane.talentBlink = 10f;
						WndHero.lastIdx = 1;
					}

					GameScene.showlevelUpStars();

					Sample.INSTANCE.play( Assets.Sounds.DRINK );
					Sample.INSTANCE.playDelayed(Assets.Sounds.LEVELUP, 0.3f, 0.7f, 1.2f);
					Sample.INSTANCE.playDelayed(Assets.Sounds.LEVELUP, 0.6f, 0.7f, 1.2f);
					new Flare( 6, 32 ).color(0xFFFF00, true).show( curUser.sprite, 2f );
					GLog.p(Messages.get(PotionOfDivineInspiration.class, "bonus"));

					if (!anonymous) {
						Catalog.countUse(PotionOfDivineInspiration.class);
						if (Random.Float() < talentChance) {
							Talent.onPotionUsed(curUser, curUser.pos, talentFactor);
						}
					}

				}
			}

			@Override
			public void onBackPressed() {
				//window can be closed if potion is already IDed
				if (!identifiedByUse){
					super.onBackPressed();
				}
			}
		});

	}

	//Maze Tower: 열려 있는 줄(1차/2차) 중 하나를 골라 포인트 +1. 줄마다 물약으로 최대 +3.
	private void mazeDrink(final Hero hero){
		final BloomTree tree = BloomTree.get(hero);
		final int max = MazeTower.DIVINE_MAX_PER_ROW;

		//1차 줄은 항상 열려 있고, 2차 줄은 개화의 핵을 쓰면 열린다
		final boolean[] enabled = new boolean[]{
				tree.firstTierBonus < max,
				tree.unlocked && tree.bonusPoints < max
		};

		if (!enabled[0] && !enabled[1]){
			GLog.w(Messages.get(this, "maze_no_more"));
			return;
		}

		GameScene.show(new WndOptions(
				new ItemSprite(this),
				Messages.titleCase(trueName()),
				Messages.get(PotionOfDivineInspiration.class, "maze_select", MazeTower.DIVINE_POINTS_PER_POTION, max),
				Messages.get(PotionOfDivineInspiration.class, "maze_row1", tree.firstTierBonus, max),
				tree.unlocked
						? Messages.get(PotionOfDivineInspiration.class, "maze_row2", tree.bonusPoints, max)
						: Messages.get(PotionOfDivineInspiration.class, "maze_row2_locked")
		){
			@Override
			protected boolean enabled(int index) {
				return enabled[index];
			}

			@Override
			protected void onSelect(int index) {
				super.onSelect(index);
				if (index == -1) return;

				if (index == 0) tree.firstTierBonus += MazeTower.DIVINE_POINTS_PER_POTION;
				else            tree.bonusPoints    += MazeTower.DIVINE_POINTS_PER_POTION;

				if (!identifiedByUse) {
					curItem.detach(curUser.belongings.backpack);
				}
				identifiedByUse = false;

				curUser.busy();
				curUser.sprite.operate(curUser.pos);
				curUser.spendAndNext(1f);

				if (tree.hasPending(hero)){
					StatusPane.talentBlink = 10f;
					WndHero.lastIdx = 1;
				}

				GameScene.showlevelUpStars();
				Sample.INSTANCE.play( Assets.Sounds.DRINK );
				Sample.INSTANCE.playDelayed(Assets.Sounds.LEVELUP, 0.3f, 0.7f, 1.2f);
				Sample.INSTANCE.playDelayed(Assets.Sounds.LEVELUP, 0.6f, 0.7f, 1.2f);
				new Flare( 6, 32 ).color(0xFFFF00, true).show( curUser.sprite, 2f );
				GLog.p(Messages.get(PotionOfDivineInspiration.class, "maze_bonus",
						MazeTower.DIVINE_POINTS_PER_POTION,
						index == 0 ? Messages.get(BloomTalentsPane.class, "tier1") : Messages.get(BloomTalentsPane.class, "tier2")));

				if (!anonymous) {
					Catalog.countUse(PotionOfDivineInspiration.class);
					if (Random.Float() < talentChance) {
						Talent.onPotionUsed(curUser, curUser.pos, talentFactor);
					}
				}
			}

			@Override
			public void onBackPressed() {
				//처음 마셔서 감정된 경우엔 반드시 골라야 한다 (원작과 같음)
				if (!identifiedByUse){
					super.onBackPressed();
				}
			}
		});
	}

	@Override
	public String desc() {
		if (!MazeTower.ORIGINAL_TALENTS && isKnown()){ //감정 전에는 원작처럼 "알 수 없는 물약" 설명
			return Messages.get(this, "maze_desc", MazeTower.DIVINE_POINTS_PER_POTION, MazeTower.DIVINE_MAX_PER_ROW);
		}
		return super.desc();
	}

	public static class DivineInspirationTracker extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		private boolean[] boostedTiers = new boolean[5];

		private static final String BOOSTED_TIERS = "boosted_tiers";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(BOOSTED_TIERS, boostedTiers);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			boostedTiers = bundle.getBooleanArray(BOOSTED_TIERS);
		}

		public void setBoosted( int tier ){
			boostedTiers[tier] = true;
		}

		public boolean isBoosted( int tier ){
			return boostedTiers[tier];
		}

	}
	
}
