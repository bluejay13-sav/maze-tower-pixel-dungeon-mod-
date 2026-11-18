/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseAbility;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;

//Maze Tower: 결실의 핵 (4차 전직). 20층 보스(드워프 왕)가 왕의 왕관 대신 떨어뜨린다.
//사용하면 원작 왕관처럼 갑옷 능력 선택 창이 열리고, 갑옷이 직업 갑옷이 되는 처리도 원작 그대로다.
//그림은 22단계에서 바꾼다. 그때까지는 임시로 원작 숙련의 서 그림 + 검푸른 빛.
public class FruitCore extends KingsCrown {

	private static final String AC_USE = "WEAR"; //원작 왕관 행동을 그대로 쓴다

	{
		image = ItemSpriteSheet.MASTERY;
	}

	private static final ItemSprite.Glowing DARK_BLUE = new ItemSprite.Glowing( 0x3355AA, 2f );

	@Override
	public ItemSprite.Glowing glowing() {
		return DARK_BLUE;
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals( AC_USE )) {
			if (hero.armorAbility != null){
				GLog.w(Messages.get(this, "already"));
				return;
			}
			if (MazeTower.fruitPaths(hero.heroClass).length == 0){
				GLog.w(Messages.get(this, "not_ready"));
				return;
			}
			curUser = hero;
			if (hero.belongings.armor() != null){
				GameScene.show( new WndChooseAbility(this, hero.belongings.armor(), hero));
			} else {
				GLog.w( Messages.get(this, "naked"));
			}
			return;
		}
		super.execute( hero, action );
	}

	@Override
	public void upgradeArmor(Hero hero, Armor armor, ArmorAbility ability) {
		super.upgradeArmor(hero, armor, ability);

		//결실: 검푸른 빛과 초록 잎빛 (별은 쓰지 않는다)
		Flare fruit = new Flare(8, 40);
		fruit.color(0x3355AA, true);
		fruit.angularSpeed = 35;
		fruit.show(hero.sprite, 2.6f);
		Flare leaf = new Flare(5, 24);
		leaf.color(0x66CC66, true);
		leaf.angularSpeed = -60;
		leaf.show(hero.sprite, 2.2f);

		GLog.p( Messages.get(this, "bloomed", Messages.titleCase(ability.name())) );
		StatusPane.talentBlink = 10f;
		WndHero.lastIdx = 1;
	}
}
