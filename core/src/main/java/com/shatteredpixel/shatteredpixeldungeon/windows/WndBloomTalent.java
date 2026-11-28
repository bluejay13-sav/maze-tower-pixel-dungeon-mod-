/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BloomTalentIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.BloomTalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//Maze Tower: 2차 특성 설명 창. 고르기(선택 창에서), 강화·각성(해금한 칸에서) 버튼이 붙는다.
public class WndBloomTalent extends Window {

	private static final float GAP = 2;
	private static final int WIDTH_MIN = 120;
	private static final int WIDTH_MAX = 220;

	//각성한 특성 제목 색 (은은한 분홍)
	private static final int AWAKENED_COLOR = 0xFF9FD6;

	private float buttonTop;
	private int wndWidth;

	private WndBloomTalent(BloomTalent talent, int level, boolean awakened){
		super();

		int width = WIDTH_MIN;

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new BloomTalentIcon(talent));
		String title = Messages.titleCase(talent.title());
		if (level >= 0) title += " " + Messages.get(WndBloomTalent.class, "level", level);
		if (awakened) title += " " + Messages.get(WndBloomTalent.class, "awakened_tag");
		titlebar.label(title, awakened ? AWAKENED_COLOR : Window.TITLE_COLOR);
		titlebar.setRect(0, 0, width, 0);
		add(titlebar);

		String desc = talent.desc();
		//(B2) 전용 특성인데 지금 그 계열 무기를 들고 있지 않으면 꺼져 있다고 알린다
		if (!talent.activeFor(Dungeon.hero)){
			desc += "\n\n" + Messages.get(WndBloomTalent.class, talent.scope == BloomTalent.Scope.BOW ? "inactive_bow" : "inactive_sword");
		}
		RenderedTextBlock txtInfo = PixelScene.renderTextBlock(desc, 6);
		txtInfo.maxWidth(width);
		txtInfo.setPos(titlebar.left(), titlebar.bottom() + 2*GAP);
		add(txtInfo);

		while (PixelScene.landscape() && txtInfo.height() > 120 && width < WIDTH_MAX){
			width += 20;
			txtInfo.maxWidth(width);
		}
		titlebar.setRect(0, 0, width, 0);
		wndWidth = width;
		buttonTop = txtInfo.bottom() + 2*GAP;
		resize(width, (int)(txtInfo.bottom() + GAP));
	}

	//아래에 버튼을 하나 더 붙인다. close = 누르면 이 창을 닫는지
	private RedButton addButton(String text, com.watabou.noosa.Image icon, final boolean close, final Runnable onClick){
		RedButton button = new RedButton(text){
			@Override
			protected void onClick() {
				super.onClick();
				if (close) hide();
				onClick.run();
			}
		};
		if (icon != null) button.icon(icon);
		button.setRect(0, buttonTop, wndWidth, 18);
		add(button);
		buttonTop = button.bottom() + 1;
		resize(wndWidth, (int)button.bottom() + 1);
		return button;
	}

	//+ 칸의 선택지 하나: "이 특성 선택"
	public static WndBloomTalent forChoice(final int slotIndex, final BloomTalent talent,
	                                       final BloomTalentsPane pane, final Window choiceWnd){
		WndBloomTalent w = new WndBloomTalent(talent, -1, false);
		w.addButton(Messages.get(WndBloomTalent.class, "choose"), Icons.get(Icons.TALENT), true, new Runnable() {
			@Override
			public void run() {
				Hero hero = Dungeon.hero;
				BloomTree tree = BloomTree.get(hero);
				tree.choose(slotIndex, talent);
				if (choiceWnd != null) choiceWnd.hide();
				GLog.p(Messages.get(WndBloomTalent.class, "chosen", Messages.titleCase(talent.title())));
				if (!tree.hasPending(hero)) StatusPane.talentBlink = 0;
				if (pane != null){
					pane.rebuild();
					pane.chosen(slotIndex); //칸이 채워지며 번쩍
				}
			}
		});
		return w;
	}

	//이미 고른 칸: 포인트가 있으면 "강화", 꽃잎이 있고 아직 각성하지 않았으면 "각성"
	public static WndBloomTalent forOwned(final int slotIndex, final BloomTalentsPane pane){
		final Hero hero = Dungeon.hero;
		final BloomTree tree = BloomTree.get(hero);
		final BloomTree.Slot slot = tree.slots.get(slotIndex);

		final WndBloomTalent w = new WndBloomTalent(slot.chosen, slot.level, slot.awakened);

		boolean canUpgrade = hero.isAlive()
				&& tree.pointsAvailable(hero) > 0
				&& slot.level < MazeTower.BLOOM_MAX_TALENT_LEVEL;
		if (canUpgrade){
			//강화한 뒤에도 창을 닫지 않는다: 새 레벨로 같은 창을 다시 연다
			w.addButton(Messages.get(WndBloomTalent.class, "upgrade"), Icons.get(Icons.TALENT), true, new Runnable() {
				@Override
				public void run() {
					if (tree.upgrade(hero, slotIndex)){
						if (!tree.hasPending(hero)) StatusPane.talentBlink = 0;
						if (pane != null){
							pane.rebuild();
							pane.celebrate(slotIndex);
						}
					}
					WndBloomChoice.show(forOwned(slotIndex, pane));
				}
			});
		}

		//0레벨 특성은 각성할 수 없다: 버튼을 꺼 두고 이유를 보여 준다
		if (hero.isAlive() && !slot.awakened && tree.petalsAvailable() > 0 && slot.level < MazeTower.BLOOM_AWAKEN_MIN_LEVEL){
			w.addButton(Messages.get(WndBloomTalent.class, "awaken_need_level", MazeTower.BLOOM_AWAKEN_MIN_LEVEL), BloomTalentIcon.petal(), false, new Runnable() {
				@Override public void run() { }
			}).enable(false);
		}

		boolean canAwaken = hero.isAlive() && !slot.awakened && tree.petalsAvailable() > 0 && slot.level >= MazeTower.BLOOM_AWAKEN_MIN_LEVEL;
		if (canAwaken){
			w.addButton(Messages.get(WndBloomTalent.class, "awaken", tree.petalsAvailable()), BloomTalentIcon.petal(), false, new Runnable() {
				@Override
				public void run() {
					//되돌릴 수 없으니 한 번 더 묻는다
					WndBloomChoice.show(new WndOptions(BloomTalentIcon.petal(),
							Messages.get(WndBloomTalent.class, "awaken_title"),
							Messages.get(WndBloomTalent.class, "awaken_confirm", Messages.titleCase(slot.chosen.title())),
							Messages.get(WndBloomTalent.class, "awaken_yes"),
							Messages.get(WndBloomTalent.class, "awaken_no")){
						@Override
						protected void onSelect(int index) {
							if (index != 0) return;
							if (!tree.awaken(slotIndex)) return;
							w.hide();
							awakenFx(hero);
							GLog.p(Messages.get(WndBloomTalent.class, "awakened", Messages.titleCase(slot.chosen.title())));
							if (!tree.hasPending(hero)) StatusPane.talentBlink = 0;
							if (pane != null) pane.rebuild();
							WndBloomChoice.show(forOwned(slotIndex, pane));
						}
					});
				}
			});
		}
		return w;
	}

	//각성 순간: 주인공 주위로 분홍 꽃잎이 흩날린다 (보스 벚꽃잎 입자 재사용)
	private static void awakenFx(Hero hero){
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP, 0.8f, 1.3f);
		if (hero.sprite != null && hero.sprite.parent != null){
			hero.sprite.emitter().burst(BossPetalParticle.FACTORIES[BossPetalParticle.VIVID], 24);
		}
	}
}
