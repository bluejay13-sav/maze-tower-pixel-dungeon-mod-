/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BloomTalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Maze Tower: 2차 특성 설명 창. 고르기(선택 창에서) 또는 강화(해금한 칸에서) 버튼이 붙는다.
public class WndBloomTalent extends Window {

	private static final float GAP = 2;
	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	private WndBloomTalent(BloomTalent talent, int level, String buttonText, final Runnable onButton){
		super();

		int width = WIDTH_MIN;

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new TalentIcon(talent.icon()));
		String title = Messages.titleCase(talent.title());
		if (level >= 0) title += " " + Messages.get(WndBloomTalent.class, "level", level);
		titlebar.label(title, Window.TITLE_COLOR);
		titlebar.setRect(0, 0, width, 0);
		add(titlebar);

		RenderedTextBlock txtInfo = PixelScene.renderTextBlock(talent.desc(), 6);
		txtInfo.maxWidth(width);
		txtInfo.setPos(titlebar.left(), titlebar.bottom() + 2*GAP);
		add(txtInfo);

		while (PixelScene.landscape() && txtInfo.height() > 120 && width < WIDTH_MAX){
			width += 20;
			txtInfo.maxWidth(width);
		}
		titlebar.setRect(0, 0, width, 0);
		resize(width, (int)(txtInfo.bottom() + GAP));

		if (buttonText != null){
			RedButton button = new RedButton(buttonText){
				@Override
				protected void onClick() {
					super.onClick();
					hide();
					onButton.run();
				}
			};
			button.icon(Icons.get(Icons.TALENT));
			button.setRect(0, txtInfo.bottom() + 2*GAP, width, 18);
			add(button);
			resize(width, (int)button.bottom() + 1);
		}
	}

	//+ 칸의 선택지 하나: "이 특성 선택"
	public static WndBloomTalent forChoice(final int slotIndex, final BloomTalent talent,
	                                       final BloomTalentsPane pane, final Window choiceWnd){
		return new WndBloomTalent(talent, -1, Messages.get(WndBloomTalent.class, "choose"), new Runnable() {
			@Override
			public void run() {
				Hero hero = Dungeon.hero;
				BloomTree tree = BloomTree.get(hero);
				tree.choose(slotIndex, talent);
				if (choiceWnd != null) choiceWnd.hide();
				GLog.p(Messages.get(WndBloomTalent.class, "chosen", Messages.titleCase(talent.title())));
				if (!tree.hasPending(hero)) StatusPane.talentBlink = 0;
				if (pane != null) pane.rebuild();
			}
		});
	}

	//이미 고른 칸: 포인트가 있으면 "강화"
	public static WndBloomTalent forOwned(final int slotIndex, final BloomTalentsPane pane){
		final Hero hero = Dungeon.hero;
		final BloomTree tree = BloomTree.get(hero);
		BloomTree.Slot slot = tree.slots.get(slotIndex);

		boolean canUpgrade = hero.isAlive()
				&& tree.pointsAvailable(hero) > 0
				&& slot.level < MazeTower.BLOOM_MAX_TALENT_LEVEL;

		return new WndBloomTalent(slot.chosen, slot.level,
				canUpgrade ? Messages.get(WndBloomTalent.class, "upgrade") : null,
				new Runnable() {
					@Override
					public void run() {
						if (tree.upgrade(hero, slotIndex)){
							if (!tree.hasPending(hero)) StatusPane.talentBlink = 0;
							if (pane != null){
								pane.rebuild();
								pane.celebrate(slotIndex);
							}
						}
					}
				});
	}
}
