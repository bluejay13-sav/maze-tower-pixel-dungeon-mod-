/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBloomChoice;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBloomTalent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

//Maze Tower: 상태창 특성 탭. 원작 TalentsPane 대신 1차(비어 있음) / 2차(6칸) 줄을 보여 준다.
public class BloomTalentsPane extends ScrollPane {

	private static final int SLOTS_PER_ROW = 3;
	private static final int SLOT_GAP = 6;

	private final ArrayList<Gizmo> parts = new ArrayList<>();

	private RenderedTextBlock firstTitle;
	private RenderedTextBlock firstInfo;
	private ColorBlock sep;
	private RenderedTextBlock secondTitle;
	private RenderedTextBlock secondInfo;
	private final ArrayList<SlotButton> slotButtons = new ArrayList<>();

	public BloomTalentsPane() {
		super(new Component());
		build();
	}

	private <T extends Gizmo> T part(T g){
		content.add(g);
		parts.add(g);
		return g;
	}

	private void build(){
		Hero hero = Dungeon.hero;
		BloomTree tree = BloomTree.get(hero);
		tree.sync(hero);

		//1차 줄 (1차 능력은 미완성)
		firstTitle = part(PixelScene.renderTextBlock(Messages.get(this, "tier1"), 9));
		firstTitle.hardlight(Window.TITLE_COLOR);
		firstInfo = part(PixelScene.renderTextBlock(
				Messages.get(this, "tier1_info", tree.firstTierTotal(hero)), 6));

		sep = part(new ColorBlock(1, 1, 0xFF000000));

		//2차 줄
		secondTitle = part(PixelScene.renderTextBlock(Messages.get(this, "tier2"), 9));
		secondTitle.hardlight(Window.TITLE_COLOR);

		String info;
		if (!tree.unlocked){
			info = Messages.get(this, "tier2_locked");
		} else {
			info = Messages.get(this, "tier2_points", tree.pointsAvailable(hero), tree.pointsEarned(hero));
		}
		secondInfo = part(PixelScene.renderTextBlock(info, 6));

		slotButtons.clear();
		for (int i = 0; i < MazeTower.BLOOM_SLOTS; i++){
			SlotButton b = part(new SlotButton(i));
			slotButtons.add(b);
		}
	}

	//고르기·강화 후 다시 그린다
	public void rebuild(){
		for (Gizmo g : parts){
			g.killAndErase();
			g.destroy();
		}
		parts.clear();
		build();
		layout();
	}

	@Override
	protected void layout() {
		super.layout();

		float top = 2;

		firstTitle.setPos((width - firstTitle.width())/2f, top);
		top = firstTitle.bottom() + 3;
		firstInfo.maxWidth((int)width - 4);
		firstInfo.setPos(2, top);
		top = firstInfo.bottom() + 4;

		sep.x = 0;
		sep.y = top;
		sep.size(width, 1);
		top += 4;

		secondTitle.setPos((width - secondTitle.width())/2f, top);
		top = secondTitle.bottom() + 3;
		secondInfo.maxWidth((int)width - 4);
		secondInfo.setPos(2, top);
		top = secondInfo.bottom() + 4;

		float rowWidth = SLOTS_PER_ROW * TalentButton.WIDTH + (SLOTS_PER_ROW-1) * SLOT_GAP;
		float left = (width - rowWidth)/2f;
		for (int i = 0; i < slotButtons.size(); i++){
			int col = i % SLOTS_PER_ROW;
			int row = i / SLOTS_PER_ROW;
			slotButtons.get(i).setPos(left + col*(TalentButton.WIDTH + SLOT_GAP),
					top + row*(TalentButton.HEIGHT + 4));
			PixelScene.align(slotButtons.get(i));
		}
		int rows = (slotButtons.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
		top += rows*(TalentButton.HEIGHT + 4);

		content.setSize(width, Math.max(height, top));
	}

	//2차 줄 한 칸: 고른 특성 / + 칸 / 잠긴 칸
	public class SlotButton extends Button {

		private final int index;
		private Image bg;
		private Image icon;
		private ColorBlock fill;

		public SlotButton(int index){
			super();
			hotArea.blockLevel = PointerArea.NEVER_BLOCK;
			this.index = index;

			BloomTree tree = BloomTree.get(Dungeon.hero);
			BloomTree.Slot slot = index < tree.slots.size() ? tree.slots.get(index) : null;

			fill = new ColorBlock(0, 4, 0xFFFFFF44);
			add(fill);

			bg = new Image(Assets.Interfaces.TALENT_BUTTON);
			//특성 최대 레벨 2 → 원작 2포인트 특성 테두리
			bg.frame(20*(MazeTower.BLOOM_MAX_TALENT_LEVEL-1), 0, TalentButton.WIDTH, TalentButton.HEIGHT);
			add(bg);

			if (slot != null && slot.isChosen()){
				icon = new TalentIcon(slot.chosen.icon());
			} else if (slot != null){
				icon = Icons.get(Icons.PLUS);
				bg.hardlight(1f, 1f, 0.5f);
			} else {
				icon = null;
				bg.alpha(0.3f);
			}
			if (icon != null) add(icon);
		}

		private BloomTree.Slot slot(){
			BloomTree tree = BloomTree.get(Dungeon.hero);
			return index < tree.slots.size() ? tree.slots.get(index) : null;
		}

		@Override
		protected void layout() {
			width = TalentButton.WIDTH;
			height = TalentButton.HEIGHT;
			super.layout();

			BloomTree.Slot slot = slot();
			float lvl = (slot != null && slot.isChosen()) ? slot.level : 0;
			fill.x = x + 2;
			fill.y = y + TalentButton.WIDTH - 1;
			fill.size(lvl / MazeTower.BLOOM_MAX_TALENT_LEVEL * (TalentButton.WIDTH - 4), 5);

			bg.x = x;
			bg.y = y;

			if (icon != null){
				icon.x = x + 2 + (16 - icon.width())/2f;
				icon.y = y + 2 + (16 - icon.height())/2f;
				PixelScene.align(icon);
			}
		}

		@Override
		protected void onPointerDown() {
			if (icon != null) icon.brightness(1.5f);
			bg.brightness(1.5f);
			Sample.INSTANCE.play(Assets.Sounds.CLICK);
		}

		@Override
		protected void onPointerUp() {
			if (icon != null) icon.resetColor();
			bg.resetColor();
			if (slot() != null && !slot().isChosen()) bg.hardlight(1f, 1f, 0.5f);
		}

		@Override
		protected void onClick() {
			super.onClick();
			BloomTree.Slot slot = slot();
			if (slot == null) return;

			Window w;
			if (slot.isChosen()){
				w = WndBloomTalent.forOwned(index, BloomTalentsPane.this);
			} else {
				w = new WndBloomChoice(index, BloomTalentsPane.this);
			}
			if (ShatteredPixelDungeon.scene() instanceof GameScene){
				GameScene.show(w);
			} else {
				ShatteredPixelDungeon.scene().addToFront(w);
			}
		}

		@Override
		protected String hoverText() {
			BloomTree.Slot slot = slot();
			if (slot == null) return Messages.get(BloomTalentsPane.class, "slot_locked");
			if (!slot.isChosen()) return Messages.get(BloomTalentsPane.class, "slot_plus");
			return Messages.titleCase(slot.chosen.title());
		}
	}

	//강화했을 때 별 효과
	public void celebrate(int index){
		if (index < 0 || index >= slotButtons.size()) return;
		SlotButton b = slotButtons.get(index);
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP, 0.7f, 1.2f);
		Emitter emitter = (Emitter) content.recycle(Emitter.class);
		emitter.revive();
		emitter.pos(b.left() + b.width()/2f, b.top() + TalentButton.WIDTH);
		emitter.burst(Speck.factory(Speck.STAR), 12);
	}
}
