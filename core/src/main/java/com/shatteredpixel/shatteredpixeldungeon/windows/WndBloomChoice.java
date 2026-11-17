/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BloomTalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

//Maze Tower: + 칸을 눌렀을 때 그 칸에 저장된 특성 3개를 보여 주는 창.
//특성을 누르면 자세한 설명과 "선택" 버튼이 나온다. 창을 닫으면 보류.
public class WndBloomChoice extends Window {

	private static final int WIDTH = 120;
	private static final int BTN_HEIGHT = 20;
	private static final int GAP = 2;

	public WndBloomChoice(final int slotIndex, final BloomTalentsPane pane){
		super();

		BloomTree tree = BloomTree.get(Dungeon.hero);
		BloomTree.Slot slot = tree.slots.get(slotIndex);

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width())/2f, 0);
		add(title);

		RenderedTextBlock body = PixelScene.renderTextBlock(Messages.get(this, "body"), 6);
		body.maxWidth(WIDTH);
		body.setPos(0, title.bottom() + 2*GAP);
		add(body);

		float pos = body.bottom() + 2*GAP;

		for (final BloomTalent t : slot.options){
			RedButton btn = new RedButton(Messages.titleCase(t.title()), 8){
				@Override
				protected void onClick() {
					super.onClick();
					show(WndBloomTalent.forChoice(slotIndex, t, pane, WndBloomChoice.this));
				}
			};
			btn.icon(new TalentIcon(t.icon()));
			btn.leftJustify = true;
			btn.setRect(0, pos, WIDTH, BTN_HEIGHT);
			add(btn);
			pos = btn.bottom() + 1;

			RenderedTextBlock shortDesc = PixelScene.renderTextBlock(t.shortDesc(), 6);
			shortDesc.maxWidth(WIDTH - 4);
			shortDesc.setPos(2, pos);
			add(shortDesc);
			pos = shortDesc.bottom() + 3*GAP;
		}

		RedButton hold = new RedButton(Messages.get(this, "hold")){
			@Override
			protected void onClick() {
				super.onClick();
				hide();
			}
		};
		hold.setRect(0, pos, WIDTH, BTN_HEIGHT);
		add(hold);

		resize(WIDTH, (int)hold.bottom());
	}

	static void show(Window w){
		if (ShatteredPixelDungeon.scene() instanceof GameScene){
			GameScene.show(w);
		} else {
			ShatteredPixelDungeon.scene().addToFront(w);
		}
	}
}
