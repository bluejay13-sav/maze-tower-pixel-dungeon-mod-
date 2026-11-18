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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBloomChoice;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBloomTalent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
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
			//처음 보는 + 칸은 빛나며 등장한다 (여러 칸이면 차례로)
			if (i < tree.slots.size() && i >= tree.seenSlots && !tree.slots.get(i).isChosen()){
				b.appear((i - tree.seenSlots) * 0.2f);
			}
		}
		tree.seenSlots = Math.max(tree.seenSlots, tree.slots.size());
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

		// ===== 연출: + 칸 등장(빛나며 커짐), 특성 선택(번쩍) =====

		private float appearDelay = -1;  //등장 연출까지 남은 시간 (-1이면 없음)
		private float appearTime = 0;    //등장 연출 남은 시간
		private float flashTime = 0;     //번쩍임 남은 시간
		private static final float APPEAR_DURATION = 1.0f;
		private static final float FLASH_DURATION = 0.45f;

		public void appear( float delay ){
			appearDelay = delay;
			if (icon != null) icon.alpha(0f);
			bg.alpha(0.3f);
		}

		public void flash(){
			flashTime = FLASH_DURATION;
		}

		private void restoreColors(){
			bg.resetColor();
			if (icon != null) icon.resetColor();
			if (slot() != null && !slot().isChosen()) bg.hardlight(1f, 1f, 0.5f);
		}

		@Override
		public void update() {
			super.update();

			if (appearDelay >= 0){
				appearDelay -= Game.elapsed;
				if (appearDelay < 0){
					appearTime = APPEAR_DURATION;
					Sample.INSTANCE.play(Assets.Sounds.CHARMS, 0.6f, 1.4f);
				}
			}
			if (appearTime > 0){
				appearTime -= Game.elapsed;
				float p = 1f - Math.max(0, appearTime) / APPEAR_DURATION; //0→1
				//서서히 나타나며 밝게 깜빡이다가 원래 색으로
				float glow = 1f + 1.2f * (1f - p) * Math.abs((float)Math.sin(p * Math.PI * 3));
				bg.alpha(Math.min(1f, 0.3f + p * 1.5f));
				bg.brightness(glow);
				if (icon != null){
					icon.alpha(Math.min(1f, p * 2f));
					icon.brightness(glow);
				}
				if (appearTime <= 0){
					restoreColors();
					bg.alpha(1f);
					if (icon != null) icon.alpha(1f);
				}
			}
			if (flashTime > 0){
				flashTime -= Game.elapsed;
				float f = Math.max(0, flashTime) / FLASH_DURATION;
				bg.brightness(1f + 2f * f);
				if (icon != null) icon.brightness(1f + 2f * f);
				if (flashTime <= 0) restoreColors();
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

	//특성을 골라 칸이 채워질 때: 번쩍
	public void chosen(int index){
		if (index < 0 || index >= slotButtons.size()) return;
		slotButtons.get(index).flash();
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP, 0.8f, 1.0f);
	}

	//강화했을 때: 소리만
	public void celebrate(int index){
		if (index < 0 || index >= slotButtons.size()) return;
		//강화는 번쩍임 없이 소리만 (얻을 때만 한 번 번쩍인다)
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP, 0.7f, 1.2f);
	}
}
