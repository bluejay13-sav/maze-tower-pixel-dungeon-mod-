/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.items.BloomCore;
import com.shatteredpixel.shatteredpixeldungeon.items.BossTorch;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.FruitCore;
import com.shatteredpixel.shatteredpixeldungeon.items.FullBloomCore;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FlashBangBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShrapnelBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.WoollyBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.GreatBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HornBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MazeCrossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MazeSpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PelletBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornDagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//Maze Tower: 보스전 체험판 "아이템 꺼내기" 치트 창 (게임 메뉴, 체험판에서만. 사용자가 확인한 시안 그대로, 2026-11-22).
//위 탭 12개(두 줄): 무기 · 방어구 · 투척 무기 · 물약 · 주문서 · 반지 / 마법봉 · 유물 · 장신구 · 음식 · 폭탄과 기타 · 마탑 전용.
//가운데 7칸 격자(4줄씩, 오른쪽 ^·v로 넘김). 그림을 누르면 아래 설정대로 바로 가방에(가득 차면 발밑에), 길게 누르면 설명.
//설정: 강화(-/+, 빠른 +0·+3·+6·+10), 수량(쌓이는 아이템만), 감정됨·저주(장비·마법봉만). "이 종류 전부 받기"는 지금 탭 전부를 설정대로.
public class WndTrialItems extends Window {

	private static final int COLS  = 7;
	private static final int ROWS  = 4;
	private static final int SIZE  = 18;
	private static final int GRID_W = COLS * SIZE;
	private static final int WIDTH = GRID_W + 8;
	private static final int TAB_W = 21, TAB_H = 18, TAB_GAP = 1;
	private static final int TABS = 12;

	//마지막 설정은 창을 닫았다 열어도 남는다
	private static int tab = 0;
	private static int upgrade = 0;
	private static int quantity = 1;
	private static boolean identified = true;
	private static boolean cursed = false;

	private int page = 0;
	private Class<?> lastGiven = null;

	private final ArrayList<Component> gridParts = new ArrayList<>();
	private final ArrayList<com.watabou.noosa.Gizmo> tabParts = new ArrayList<>();
	private RenderedTextBlock catLabel, pageLabel, upText, qtyText;
	private ColorBlock thumb;
	private float gridY, tabY, valuesY;

	public WndTrialItems(){
		super();
		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width())/2f, 0);
		add(title);

		tabY = title.bottom() + 3;
		buildTabs();

		float y = tabY + 2 * (TAB_H + TAB_GAP) + 3;
		catLabel = PixelScene.renderTextBlock(7);
		catLabel.hardlight(TITLE_COLOR);
		add(catLabel);
		catLabel.setPos(0, y);
		pageLabel = PixelScene.renderTextBlock(6);
		pageLabel.hardlight(0xB0A8A0);
		add(pageLabel);
		gridY = y + 10;

		//격자 바탕
		ColorBlock bg = new ColorBlock(GRID_W + 1, ROWS * SIZE + 1, 0xFF2A2220);
		bg.x = -1; bg.y = gridY - 1;
		add(bg);
		//오른쪽 넘김 막대: ^, 막대, v
		RedButton up = new RedButton("^", 6){
			@Override protected void onClick(){ if (page > 0){ page--; buildGrid(); } }
		};
		up.setRect(GRID_W + 1, gridY - 1, 7, 9);
		add(up);
		RedButton down = new RedButton("v", 6){
			@Override protected void onClick(){ if (page < pages() - 1){ page++; buildGrid(); } }
		};
		down.setRect(GRID_W + 1, gridY + ROWS * SIZE - 8, 7, 9);
		add(down);
		ColorBlock track = new ColorBlock(2, ROWS * SIZE - 20, 0xFF807060);
		track.x = GRID_W + 3.5f; track.y = gridY + 9;
		add(track);
		thumb = new ColorBlock(2, 10, 0xFFD0C0A0);
		thumb.x = GRID_W + 3.5f;
		add(thumb);

		y = gridY + ROWS * SIZE + 5;
		y = settings(y);

		RedButton all = new RedButton(Messages.get(this, "all"), 7){
			@Override protected void onClick(){
				for (Class<?> c : classes(tab)) give(make(c));
			}
		};
		all.setRect(0, y, WIDTH, 15);
		add(all);
		y = all.bottom() + 3;

		RenderedTextBlock hint = PixelScene.renderTextBlock(Messages.get(this, "hint"), 6);
		hint.hardlight(0xB0A8A0);
		hint.maxWidth(WIDTH);
		hint.setPos((WIDTH - hint.width()) / 2f, y);
		add(hint);

		resize(WIDTH, (int)hint.bottom() + 1);
		buildGrid();
	}

	// ---------------- 탭 ----------------
	private void buildTabs(){
		for (com.watabou.noosa.Gizmo c : tabParts) c.killAndErase();
		tabParts.clear();
		for (int i = 0; i < TABS; i++){
			final int t = i;
			float x = (i % 6) * (TAB_W + TAB_GAP) + 1;
			float ty = tabY + (i / 6) * (TAB_H + TAB_GAP);
			Bevel box = new Bevel(t == tab);
			box.setRect(x, ty, TAB_W, TAB_H);
			add(box);
			tabParts.add(box);
			ItemSprite icon = new ItemSprite(tabIcon(i));
			if (t != tab) icon.alpha(0.6f);
			IconButton b = new IconButton(icon){
				@Override
				protected void onClick() {
					if (tab == t) return;
					tab = t;
					page = 0;
					lastGiven = null;
					buildTabs();
					buildGrid();
				}
				@Override
				protected String hoverText() {
					return Messages.get(WndTrialItems.class, "tab_" + t);
				}
			};
			b.setRect(x, ty, TAB_W, TAB_H);
			add(b);
			tabParts.add(b);
		}
	}

	//베벨 상자 (원작 버튼 느낌). sel이면 밝게 + 금테 (고른 탭·방금 받은 칸)
	private static class Bevel extends Component {
		private final boolean sel;
		private final int dark, light;
		private ColorBlock fill, hi, lo1, lo2;
		private ColorBlock[] edge;
		Bevel( boolean sel ){ this(sel, sel ? 0xFF554B40 : 0xFF2C2622, sel ? 0xFF7A6C5C : 0xFF3A332D); }
		Bevel( boolean sel, int dark, int light ){
			super();
			this.sel = sel; this.dark = dark; this.light = light;
			fill = new ColorBlock(1, 1, dark); add(fill);
			hi = new ColorBlock(1, 1, light); add(hi);
			lo1 = new ColorBlock(1, 1, 0xFF1A1512); add(lo1);
			lo2 = new ColorBlock(1, 1, 0xFF1A1512); add(lo2);
			if (sel){
				edge = new ColorBlock[4];
				for (int i = 0; i < 4; i++){ edge[i] = new ColorBlock(1, 1, 0xFFFFE060); add(edge[i]); }
			}
		}
		@Override
		protected void layout() {
			fill.x = x; fill.y = y; fill.size(width, height);
			hi.x = x; hi.y = y; hi.size(width, 1);
			lo1.x = x; lo1.y = y + height - 1; lo1.size(width, 1);
			lo2.x = x + width - 1; lo2.y = y; lo2.size(1, height);
			if (sel){
				edge[0].x = x; edge[0].y = y; edge[0].size(width, 1);
				edge[1].x = x; edge[1].y = y + height - 1; edge[1].size(width, 1);
				edge[2].x = x; edge[2].y = y; edge[2].size(1, height);
				edge[3].x = x + width - 1; edge[3].y = y; edge[3].size(1, height);
			}
		}
	}

	private static Item tabIcon( int t ){
		switch (t){
			case 0:  return new Longsword();
			case 1:  return make0(Generator.Category.ARMOR.classes, Generator.Category.ARMOR.classes.length - 1);
			case 2:  return make0(Generator.Category.MIS_T2.classes, 0);
			case 3:  return make0(Generator.Category.POTION.classes, 0);
			case 4:  return make0(Generator.Category.SCROLL.classes, 0);
			case 5:  return make0(Generator.Category.RING.classes, 0);
			case 6:  return make0(Generator.Category.WAND.classes, 0);
			case 7:  return make0(Generator.Category.ARTIFACT.classes, 2);
			case 8:  return make0(Generator.Category.TRINKET.classes, 0);
			case 9:  return make0(Generator.Category.FOOD.classes, 0);
			case 10: return new Bomb();
			default: return new BloomCore();
		}
	}

	private static Item make0( Class<?>[] cs, int i ){
		Object o = cs.length > 0 ? Reflection.newInstance(cs[Math.max(0, Math.min(cs.length - 1, i))]) : null;
		return o instanceof Item ? (Item) o : new Bomb();
	}

	//탭마다 아이템 종류 (원작 아이템 분류 + 마탑 아이템)
	private static ArrayList<Class<?>> classes( int t ){
		LinkedHashSet<Class<?>> s = new LinkedHashSet<>();
		switch (t){
			case 0:
				//마탑 검 7종 · 활 7종 · 낡은 단검이 먼저, 그다음 원작 무기
				addAll(s, WornShortsword.class, Shortsword.class, Rapier.class, Scimitar.class, Longsword.class, Katana.class, RunicBlade.class,
						WornShortbow.class, PelletBow.class, Longbow.class, HornBow.class, MazeCrossbow.class, GreatBow.class, MazeSpiritBow.class,
						WornDagger.class);
				for (Generator.Category c : new Generator.Category[]{Generator.Category.WEP_T1, Generator.Category.WEP_T2,
						Generator.Category.WEP_T3, Generator.Category.WEP_T4, Generator.Category.WEP_T5}) addAll(s, c.classes);
				break;
			case 1: addAll(s, Generator.Category.ARMOR.classes); break;
			case 2:
				for (Generator.Category c : new Generator.Category[]{Generator.Category.MIS_T1, Generator.Category.MIS_T2,
						Generator.Category.MIS_T3, Generator.Category.MIS_T4, Generator.Category.MIS_T5}) addAll(s, c.classes);
				break;
			case 3:
				addAll(s, Generator.Category.POTION.classes);
				s.addAll(ExoticPotion.regToExo.values());
				break;
			case 4:
				addAll(s, Generator.Category.SCROLL.classes);
				s.addAll(ExoticScroll.regToExo.values());
				break;
			case 5: addAll(s, Generator.Category.RING.classes); break;
			case 6: addAll(s, Generator.Category.WAND.classes); break;
			case 7: addAll(s, Generator.Category.ARTIFACT.classes); break;
			case 8: addAll(s, Generator.Category.TRINKET.classes); break;
			case 9: addAll(s, Generator.Category.FOOD.classes); break;
			case 10:
				addAll(s, Bomb.class, Firebomb.class, FrostBomb.class, FlashBangBomb.class, HolyBomb.class, ShrapnelBomb.class,
						SmokeBomb.class, WoollyBomb.class, RegrowthBomb.class, ArcaneBomb.class, Noisemaker.class,
						Ankh.class, Honeypot.class, VelvetPouch.class, ScrollHolder.class, PotionBandolier.class, MagicalHolster.class);
				addAll(s, Generator.Category.SEED.classes);
				addAll(s, Generator.Category.STONE.classes);
				break;
			default:
				addAll(s, BloomCore.class, FullBloomCore.class, FruitCore.class, Arrow.class, BossTorch.class);
				break;
		}
		ArrayList<Class<?>> out = new ArrayList<>();
		for (Class<?> c : s) if (c != null && Reflection.newInstance(c) instanceof Item) out.add(c);
		return out;
	}

	private static void addAll( LinkedHashSet<Class<?>> s, Class<?>... cs ){
		if (cs == null) return;
		for (Class<?> c : cs) if (c != null) s.add(c);
	}

	// ---------------- 격자 ----------------
	private int pages(){
		int n = classes(tab).size();
		return Math.max(1, (n + COLS * ROWS - 1) / (COLS * ROWS));
	}

	private void buildGrid(){
		for (Component c : gridParts) c.killAndErase();
		gridParts.clear();
		ArrayList<Class<?>> list = classes(tab);
		int per = COLS * ROWS;
		page = Math.max(0, Math.min(page, pages() - 1));
		catLabel.text(Messages.get(this, "count", Messages.get(this, "tab_" + tab), list.size()));
		pageLabel.text((page + 1) + "/" + pages());
		pageLabel.setPos(GRID_W - pageLabel.width(), catLabel.top() + 1);

		IconButton bubbleAt = null;
		for (int i = 0; i < per && page * per + i < list.size(); i++){
			final Class<?> c = list.get(page * per + i);
			final Item proto = make(c);
			if (proto == null) continue;
			float x = (i % COLS) * SIZE, y = gridY + (i / COLS) * SIZE;
			boolean sel = c == lastGiven;
			Bevel slot = sel ? new Bevel(true, 0xFF6A5A30, 0xFF8A7840) : new Bevel(false, 0xFF3A302C, 0xFF44392F);
			slot.setRect(x, y, SIZE - 1, SIZE - 1);
			add(slot);
			gridParts.add(slot);
			IconButton b = new IconButton(new ItemSprite(proto)){
				@Override
				protected void onClick() {
					give(make(c));
					lastGiven = c;
					buildGrid();
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
			b.setRect(x, y, SIZE - 1, SIZE - 1);
			add(b);
			gridParts.add(b);
			if (sel) bubbleAt = b;
		}
		//방금 받은 아이템 이름 말풍선 (그 칸 바로 아래, 맨 아래 줄이면 위)
		if (bubbleAt != null){
			Item it = make(lastGiven);
			String n = Messages.titleCase(it.name());
			if (it.isUpgradable() && upgrade > 0 && !(it instanceof Artifact)) n += " +" + upgrade;
			RenderedTextBlock name = PixelScene.renderTextBlock(n, 6);
			Component holder = new Component();
			ColorBlock bub = new ColorBlock(name.width() + 6, name.height() + 3, 0xC8000000);
			float bx = Math.max(0, Math.min(GRID_W - bub.width(), bubbleAt.left() + SIZE / 2f - bub.width() / 2f));
			float by = bubbleAt.bottom() + 1;
			if (by + bub.height() > gridY + ROWS * SIZE) by = bubbleAt.top() - bub.height() - 1;
			bub.x = bx; bub.y = by;
			holder.add(bub);
			name.setPos(bx + 3, by + 1.5f);
			holder.add(name);
			add(holder);
			gridParts.add(holder);
		}
		//넘김 막대 위치
		float trackH = ROWS * SIZE - 20;
		int p = pages();
		thumb.size(2, Math.max(4, trackH / p));
		thumb.y = gridY + 9 + (trackH - thumb.height()) * (p <= 1 ? 0 : page / (float)(p - 1));
	}

	// ---------------- 설정 ----------------
	private float settings( float y ){
		valuesY = y;
		//강화
		RenderedTextBlock up = PixelScene.renderTextBlock(Messages.get(this, "upgrade"), 7);
		up.setPos(2, y + 3);
		add(up);
		addStep(34, y, -1, true);
		upText = PixelScene.renderTextBlock(9);
		upText.hardlight(TITLE_COLOR);
		add(upText);
		addStep(74, y, +1, true);
		//수량
		RenderedTextBlock qt = PixelScene.renderTextBlock(Messages.get(this, "quantity"), 7);
		qt.setPos(2, y + 19);
		add(qt);
		addStep(34, y + 16, -1, false);
		qtyText = PixelScene.renderTextBlock(9);
		qtyText.hardlight(TITLE_COLOR);
		add(qtyText);
		addStep(74, y + 16, +1, false);
		//빠른 강화 +0 +3 +6 +10
		int[] quick = {0, 3, 6, 10};
		for (int i = 0; i < 4; i++){
			final int v = quick[i];
			RedButton q = new RedButton("+" + v, 6){
				@Override protected void onClick(){ upgrade = v; refreshValues(); }
			};
			q.setRect(94 + (i % 2) * 20, y + (i / 2) * 16, 19, 13);
			add(q);
		}
		refreshValues();
		y += 33;
		//감정됨 · 저주
		CheckBox idBox = new CheckBox(Messages.get(this, "identified")){
			@Override protected void onClick(){ super.onClick(); identified = checked(); }
		};
		idBox.checked(identified);
		idBox.setRect(0, y, WIDTH / 2f - 1, 15);
		add(idBox);
		CheckBox curseBox = new CheckBox(Messages.get(this, "cursed")){
			@Override protected void onClick(){ super.onClick(); cursed = checked(); }
		};
		curseBox.checked(cursed);
		curseBox.setRect(WIDTH / 2f + 1, y, WIDTH / 2f - 1, 15);
		add(curseBox);
		return y + 18;
	}

	private void addStep( float x, float y, final int d, final boolean isUpgrade ){
		RedButton b = new RedButton(d < 0 ? "-" : "+", 7){
			@Override protected void onClick(){
				if (isUpgrade) upgrade = Math.max(0, Math.min(MazeTower.TRIAL_CHEAT_MAX_UPGRADE, upgrade + d));
				else quantity = Math.max(1, Math.min(MazeTower.TRIAL_CHEAT_MAX_QTY, quantity + d));
				refreshValues();
			}
		};
		b.setRect(x, y, 14, 13);
		add(b);
	}

	//강화 숫자·수량 숫자는 -/+ 사이 가운데
	private void refreshValues(){
		if (upText == null || qtyText == null) return;
		upText.text("+" + upgrade);
		qtyText.text(Integer.toString(quantity));
		upText.setPos(61 - upText.width() / 2f, valuesY + 2);
		qtyText.setPos(61 - qtyText.width() / 2f, valuesY + 18);
		if (lastGiven != null && catLabel != null) buildGrid();
	}

	// ---------------- 아이템 만들기·받기 ----------------
	//설정대로: 강화(강화되는 것만, 유물 제외), 수량(쌓이는 것만), 감정, 저주(장비·마법봉만)
	private static Item make( Class<?> c ){
		Object o = Reflection.newInstance(c);
		if (!(o instanceof Item)) return null;
		Item item = (Item) o;
		if (item.stackable) item.quantity(quantity);
		if (upgrade > 0 && item.isUpgradable() && !(item instanceof Artifact)) item.upgrade(upgrade);
		if (identified) item.identify();
		if (cursed && (item instanceof EquipableItem || item instanceof Wand)){
			item.cursed = true;
			if (identified) item.cursedKnown = true;
		}
		return item;
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
