/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Reflection;

//Maze Tower: 보스전 체험판 아이템 고르기 창 (게임 메뉴, 체험판에서만).
//마법봉(감정됨, +1)·유물(감정됨)·장신구(감정됨)를 그림을 눌러 골라 가방으로 가져온다. 길게 누르면 설명.
public class WndTrialItems extends Window {

	private static final int COLS  = 7;
	private static final int SIZE  = 18;
	private static final int WIDTH = COLS * SIZE;

	public WndTrialItems(){
		super();
		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width())/2f, 0);
		add(title);
		RenderedTextBlock hint = PixelScene.renderTextBlock(Messages.get(this, "hint"), 6);
		hint.maxWidth(WIDTH);
		hint.setPos(0, title.bottom() + 2);
		add(hint);

		float y = hint.bottom() + 2;
		y = section(Messages.get(this, "wands"), Generator.Category.WAND, y);
		y = section(Messages.get(this, "artifacts"), Generator.Category.ARTIFACT, y);
		y = section(Messages.get(this, "trinkets"), Generator.Category.TRINKET, y);
		resize(WIDTH, (int)y);
	}

	private float section( String name, Generator.Category cat, float y ){
		RenderedTextBlock t = PixelScene.renderTextBlock(name, 7);
		t.hardlight(TITLE_COLOR);
		t.setPos(0, y + 3);
		add(t);
		y = t.bottom() + 2;
		int i = 0;
		for (final Class<?> c : cat.classes){
			final Item proto = make(c);
			if (proto == null) continue;
			IconButton b = new IconButton(new ItemSprite(proto)){
				@Override
				protected void onClick() {
					give(make(c));
				}
				@Override
				protected boolean onLongClick() {
					GameScene.show(new WndInfoItem(make(c)));
					return true;
				}
				@Override
				protected String hoverText() {
					return Messages.titleCase(proto.name());
				}
			};
			b.setRect((i % COLS) * SIZE, y + (i / COLS) * SIZE, SIZE, SIZE);
			add(b);
			i++;
		}
		return y + ((i + COLS - 1) / COLS) * SIZE;
	}

	//감정된 채로 만든다. 마법봉은 +1 (MazeTower.TRIAL_WAND_UPGRADE)
	private static Item make( Class<?> c ){
		Item item = (Item) Reflection.newInstance(c);
		if (item == null) return null;
		if (item instanceof Wand) item.upgrade(MazeTower.TRIAL_WAND_UPGRADE);
		return item.identify();
	}

	private static void give( Item item ){
		if (item == null || Dungeon.hero == null) return;
		if (item.collect()){
			GLog.p(Messages.get(WndTrialItems.class, "got", Messages.titleCase(item.name())));
		} else {
			Dungeon.level.drop(item, Dungeon.hero.pos).sprite.drop();
			GLog.w(Messages.get(WndTrialItems.class, "dropped", Messages.titleCase(item.name())));
		}
	}
}
