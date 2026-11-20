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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
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

//Maze Tower: 상태창 특성 탭. 원작 TalentsPane 대신 1차(발아) / 2차(개화, 6칸) / 3차(만개) / 4차(결실) 줄을 보여 준다.
public class BloomTalentsPane extends ScrollPane {

	private static final int SLOTS_PER_ROW = 3;
	private static final int SLOT_GAP = 6;

	private final ArrayList<Gizmo> parts = new ArrayList<>();

	private TalentsPane.TalentTierPane firstTier; //1차(발아) 줄: 원작 특성 줄을 그대로 쓴다
	private ColorBlock sep;
	private RenderedTextBlock secondTitle;
	private RenderedTextBlock secondInfo;
	private final ArrayList<Image> secondStars = new ArrayList<>(); //2차 포인트 별 (1차·3차 줄과 같은 모양)
	private final ArrayList<Image> secondPetals = new ArrayList<>(); //남은 분홍 꽃잎 (만개 각성), 제목 왼쪽
	private final ArrayList<SlotButton> slotButtons = new ArrayList<>();

	//3차(만개) 줄: 만개의 핵을 쓰면 원작 특성 줄(3단계)로 보여 준다
	private ColorBlock sep3;
	private TalentsPane.TalentTierPane thirdTier;
	private RenderedTextBlock thirdTitle;
	private RenderedTextBlock thirdInfo;

	//4차(결실) 줄: 결실의 핵을 쓰면 원작 특성 줄(4단계)로 보여 준다
	private ColorBlock sep4;
	private TalentsPane.TalentTierPane fourthTier;
	private RenderedTextBlock fourthTitle;
	private RenderedTextBlock fourthInfo;

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

		//1차(발아) 줄: 직업 특성 5개, 원작처럼 별(포인트)을 보고 특성을 눌러 포인트를 넣는다
		firstTier = part(new TalentsPane.TalentTierPane(hero.talents.get(0), 1, TalentButton.Mode.UPGRADE));
		firstTier.title.text(Messages.get(this, "tier1"));

		sep = part(new ColorBlock(1, 1, 0xFF000000));

		//2차 줄
		secondTitle = part(PixelScene.renderTextBlock(Messages.get(this, "tier2"), 9));
		secondTitle.hardlight(Window.TITLE_COLOR);

		String info;
		if (!tree.unlocked){
			info = Messages.get(this, "tier2_locked");
		} else {
			info = Messages.get(this, "tier2_hint");
		}
		secondInfo = part(PixelScene.renderTextBlock(info, 6));

		//남은 포인트는 별로: 밝은 별 = 쓸 수 있는 포인트, 회색 = 쓴 포인트, 어두운 별 = 아직 얻지 못한 포인트
		secondStars.clear();
		if (tree.unlocked){
			int regular = BloomTree.regularPoints();
			int total = regular + tree.bonusPoints;
			int open = tree.pointsAvailable(hero);
			int used = tree.pointsSpent();
			for (int i = 0; i < total; i++){
				Image im = part(new Speck().image(Speck.STAR));
				secondStars.add(im);
				if (i >= open && i < open + used){
					im.tint(0.75f, 0.75f, 0.75f, 0.9f);
				} else if (i >= open + used){
					im.tint(0f, 0f, 0f, 0.9f);
				}
			}
		}

		//남은 꽃잎: 제목 왼쪽에 분홍 꽃잎 아이콘 (별 아님)
		secondPetals.clear();
		for (int i = 0; i < tree.petalsAvailable(); i++){
			Image p = part(BloomTalentIcon.petal());
			p.scale.set(0.5f);
			secondPetals.add(p);
		}

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

		//3차(만개) 줄
		sep3 = part(new ColorBlock(1, 1, 0xFF000000));
		thirdTier = null;
		thirdTitle = null;
		thirdInfo = null;
		if (hero.subClass != HeroSubClass.NONE && !hero.talents.get(2).isEmpty()){
			thirdTier = part(new TalentsPane.TalentTierPane(hero.talents.get(2), 3, TalentButton.Mode.UPGRADE));
			thirdTier.title.text(Messages.get(this, "tier3_open", Messages.titleCase(hero.subClass.title())));
		} else {
			thirdTitle = part(PixelScene.renderTextBlock(Messages.get(this, "tier3"), 9));
			thirdTitle.hardlight(Window.TITLE_COLOR);
			thirdInfo = part(PixelScene.renderTextBlock(Messages.get(this, "tier3_locked"), 6));
		}

		//4차(결실) 줄
		sep4 = part(new ColorBlock(1, 1, 0xFF000000));
		fourthTier = null;
		fourthTitle = null;
		fourthInfo = null;
		if (hero.armorAbility != null && !hero.talents.get(3).isEmpty()){
			fourthTier = part(new TalentsPane.TalentTierPane(hero.talents.get(3), 4, TalentButton.Mode.UPGRADE));
			fourthTier.title.text(Messages.get(this, "tier4_open", Messages.titleCase(hero.armorAbility.name())));
		} else {
			fourthTitle = part(PixelScene.renderTextBlock(Messages.get(this, "tier4"), 9));
			fourthTitle.hardlight(Window.TITLE_COLOR);
			fourthInfo = part(PixelScene.renderTextBlock(Messages.get(this, "tier4_locked"), 6));
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

		firstTier.setRect(0, top, width, 0);
		top = firstTier.bottom() + 4;

		sep.x = 0;
		sep.y = top;
		sep.size(width, 1);
		top += 4;

		//제목 + 별을 가운데에 (원작 특성 줄과 같은 배치, 기본 5개를 넘는 별은 다음 줄)
		int regStars = BloomTree.regularPoints();
		float petalW = secondPetals.isEmpty() ? 0 : secondPetals.size()*9 + 1;
		float titleWidth = petalW + secondTitle.width() + (secondStars.isEmpty() ? 0 : 2 + Math.min(secondStars.size(), regStars)*6);
		float titleLeft = (width - titleWidth)/2f;
		secondTitle.setPos(titleLeft + petalW, top);
		for (int i = 0; i < secondPetals.size(); i++){
			Image p = secondPetals.get(i);
			p.x = titleLeft + i*9;
			p.y = secondTitle.top() + (secondTitle.height() - 8)/2f;
			PixelScene.align(p);
		}
		float starLeft = secondTitle.right() + 2;
		float starTop = secondTitle.top();
		if (regStars < secondStars.size()) starTop -= 2;
		float bottom = secondTitle.bottom();
		int n = regStars;
		for (Image star : secondStars){
			star.x = starLeft;
			star.y = starTop;
			PixelScene.align(star);
			bottom = Math.max(bottom, star.y + star.height());
			starLeft += 6;
			if (--n == 0){
				starTop += 6;
				starLeft = secondTitle.right() + 2;
			}
		}
		top = bottom + 3;
		secondInfo.maxWidth((int)width - 4);
		secondInfo.setPos(2, top);
		top = secondInfo.bottom() + 4;

		for (int i = 0; i < slotButtons.size(); i++){
			int col = i % SLOTS_PER_ROW;
			int row = i / SLOTS_PER_ROW;
			//줄마다 가운데 정렬 (5칸이면 3 + 2)
			int inRow = Math.min(SLOTS_PER_ROW, slotButtons.size() - row*SLOTS_PER_ROW);
			float rowWidth = inRow * TalentButton.WIDTH + (inRow-1) * SLOT_GAP;
			float left = (width - rowWidth)/2f;
			slotButtons.get(i).setPos(left + col*(TalentButton.WIDTH + SLOT_GAP),
					top + row*(TalentButton.HEIGHT + 4));
			PixelScene.align(slotButtons.get(i));
		}
		int rows = (slotButtons.size() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW;
		top += rows*(TalentButton.HEIGHT + 4);

		sep3.x = 0;
		sep3.y = top;
		sep3.size(width, 1);
		top += 3;
		if (thirdTier != null){
			thirdTier.setRect(0, top, width, 0);
			top = thirdTier.bottom() + 4;
		} else {
			top += 1;
			thirdTitle.setPos((width - thirdTitle.width())/2f, top);
			top = thirdTitle.bottom() + 3;
			thirdInfo.maxWidth((int)width - 4);
			thirdInfo.setPos(2, top);
			top = thirdInfo.bottom() + 4;
		}

		sep4.x = 0;
		sep4.y = top;
		sep4.size(width, 1);
		top += 3;
		if (fourthTier != null){
			fourthTier.setRect(0, top, width, 0);
			top = fourthTier.bottom() + 4;
		} else {
			top += 1;
			fourthTitle.setPos((width - fourthTitle.width())/2f, top);
			top = fourthTitle.bottom() + 3;
			fourthInfo.maxWidth((int)width - 4);
			fourthInfo.setPos(2, top);
			top = fourthInfo.bottom() + 4;
		}

		content.setSize(width, Math.max(height, top));
	}

	//2차 줄 한 칸: 고른 특성 / + 칸 / 잠긴 칸
	public class SlotButton extends Button {

		private final int index;
		private Image bg;
		private Image icon;
		private ColorBlock fill;
		private ColorBlock awakeGlow; //만개 각성: 칸 위에 살짝 겹치는 분홍빛 (숨 쉬듯 밝아졌다 어두워짐)
		private float glowTime = 0;

		public SlotButton(int index){
			super();
			hotArea.blockLevel = PointerArea.NEVER_BLOCK;
			this.index = index;

			BloomTree tree = BloomTree.get(Dungeon.hero);
			BloomTree.Slot slot = index < tree.slots.size() ? tree.slots.get(index) : null;

			if (slot != null && slot.isChosen() && slot.awakened){
				awakeGlow = new ColorBlock(1, 1, 0xFFFF5FC0); //화사한 분홍
			}

			fill = new ColorBlock(0, 4, 0xFFFFFF44);
			add(fill);

			bg = new Image(Assets.Interfaces.TALENT_BUTTON);
			//특성 최대 레벨 3 → 원작 3포인트 특성 테두리
			bg.frame(20*(MazeTower.BLOOM_MAX_TALENT_LEVEL-1), 0, TalentButton.WIDTH, TalentButton.HEIGHT);
			add(bg);
			//각성 분홍빛은 칸 그림 위에 살짝 겹친다 (뒤에 두면 칸 그림에 가려 보이지 않는다)
			if (awakeGlow != null) add(awakeGlow);

			if (slot != null && slot.isChosen()){
				icon = new BloomTalentIcon(slot.chosen);
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
			if (awakeGlow != null){
				//칸 테두리 안쪽에만 (은은하게)
				awakeGlow.x = x + 1;
				awakeGlow.y = y + 1;
				awakeGlow.size(TalentButton.WIDTH - 2, TalentButton.HEIGHT - 2);
			}

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

			//만개 각성: 은은한 분홍빛으로 숨 쉬듯 빛난다
			if (awakeGlow != null){
				glowTime += Game.elapsed;
				float b = 0.5f + 0.5f * (float)Math.sin(glowTime * 2.2f);
				awakeGlow.alpha(0.06f + 0.16f * b); //살짝만 보이게
				if (appearTime <= 0 && flashTime <= 0){
					bg.hardlight(1f, 0.78f - 0.14f * b, 0.9f - 0.06f * b);
				}
			}

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
