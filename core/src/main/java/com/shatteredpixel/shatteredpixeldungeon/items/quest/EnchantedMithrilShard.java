/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.quest;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower (K8): 마법이 부여된 미스릴 파편. 연금술 솥에서 미스릴 파편 1 + 충전의 주문서 1로 만든다.
//대장장이의 미스릴 제작 칸에 무기 두 종과 함께 넣으면 그 조합의 미스릴 무기 도안을 얻는다(MithrilBlueprint).
public class EnchantedMithrilShard extends Item {

	{
		image = MazeItemSprites.MITHRIL_SHARD_ENCHANTED;
		stackable = true;
		bones = false;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return quantity * MazeTower.MITHRIL_VALUE * 2;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs = new Class[]{MithrilShard.class, ScrollOfRecharging.class};
			inQuantity = new int[]{1, 1};
			cost = MazeTower.MITHRIL_ENCHANT_ENERGY; //(사용자) 파편 1 + 충전의 주문서 1만 (에너지 0)
			output = EnchantedMithrilShard.class;
			outQuantity = 1;
		}
	}
}
