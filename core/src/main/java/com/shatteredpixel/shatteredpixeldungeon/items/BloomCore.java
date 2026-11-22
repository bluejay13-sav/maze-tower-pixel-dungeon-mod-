/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Maze Tower: 개화의 핵. 5층 보스(구)가 떨어뜨린다.
//들고 있는 동안 Lv7~11 레벨업마다 선택권·포인트가 쌓이고(표시만), 사용하면 사라지며 2차 줄이 열린다.
public class BloomCore extends Item {

	private static final String AC_USE = "USE";

	{
		stackable = false;
		image = MazeItemSprites.BLOOM_CORE; //docs/sprites의 그림 (assets/sprites/maze_bloom_core.png)
		defaultAction = AC_USE;
		unique = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_USE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_USE )) {
			BloomTree tree = BloomTree.get(hero);
			if (tree.unlocked){
				GLog.w(Messages.get(this, "already"));
				return;
			}

			detach( hero.belongings.backpack );
			tree.unlock(hero);

			hero.spend( Actor.TICK );
			hero.busy();
			hero.sprite.operate( hero.pos );
			Sample.INSTANCE.play( Assets.Sounds.MASTERY );

			//(결정, 2026-11-21) 꽃이 피는 연출은 삭제 (특성 창에 가려 보이지 않음)


			GLog.p( Messages.get(this, "used", tree.slots.size()) );
			StatusPane.talentBlink = 10f;
			WndHero.lastIdx = 1;
		}
	}

	@Override
	public String desc() {
		String desc = super.desc();
		Hero hero = Dungeon.hero;
		if (hero != null){
			desc += "\n\n" + Messages.get(this, "stored",
					BloomTree.choicesFor(hero), BloomTree.levelPointsFor(hero));
		}
		return desc;
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
		return 0;
	}
}
