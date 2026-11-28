/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EnchantedMithrilShard;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MithrilBlueprint;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MithrilShard;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Maze Tower (K8): 대장장이의 미스릴 제작 칸 (연금술 솥처럼 재료 칸 3개 + 의뢰 버튼, 원작 재련 창 WndReforge 바탕)
//  · 갑옷 + 미스릴 파편            → 그 갑옷을 미스릴로 보강 (한 번만, 방어 +2·회피 ×1.2, 호감 MITHRIL_ARMOR_COST)
//  · 부여된 미스릴 파편 + 무기 두 종 → 그 조합의 미스릴 무기 도안 (무기는 사라짐, 호감 MITHRIL_BLUEPRINT_COST)
public class WndMithrilForge extends Window {

	private static final int WIDTH    = 120;
	private static final int BTN_SIZE = 32;
	private static final float GAP    = 2;
	private static final float BTN_GAP = 4;

	private final ItemButton[] slots = new ItemButton[3];
	private ItemButton pressed;
	private final RenderedTextBlock result;
	private final RedButton btnCommission;
	private final Window parentWnd;

	private enum Kind { NONE, ARMOR, BLUEPRINT }
	private Kind kind = Kind.NONE;
	private MithrilBlueprint.Kind blueprint;

	public WndMithrilForge( Blacksmith troll, Window wndParent ){
		super();
		parentWnd = wndParent;

		IconTitle titlebar = new IconTitle();
		titlebar.icon( troll.sprite() );
		titlebar.label( Messages.get(this, "title") );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( Messages.get(this, "message",
				MazeTower.MITHRIL_ARMOR_COST, MazeTower.MITHRIL_BLUEPRINT_COST, Blacksmith.Quest.favor), 6 );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		float total = 3 * BTN_SIZE + 2 * BTN_GAP;
		float left = (WIDTH - total) / 2f;
		for (int i = 0; i < 3; i++){
			final int idx = i;
			slots[i] = new ItemButton(){
				@Override
				protected void onClick() {
					pressed = slots[idx];
					GameScene.selectItem( selector );
				}
			};
			slots[i].setRect( left + i * (BTN_SIZE + BTN_GAP), message.bottom() + BTN_GAP, BTN_SIZE, BTN_SIZE );
			add( slots[i] );
		}

		result = PixelScene.renderTextBlock( Messages.get(this, "none"), 6 );
		result.maxWidth( WIDTH );
		result.setPos( 0, slots[0].bottom() + BTN_GAP );
		add( result );

		btnCommission = new RedButton( Messages.get(this, "commission") ){
			@Override
			protected void onClick() {
				commission();
			}
		};
		btnCommission.enable( false );
		btnCommission.setRect( 0, result.bottom() + BTN_GAP + 10, WIDTH, 20 );
		add( btnCommission );

		resize( WIDTH, (int) btnCommission.bottom() );
	}

	private ArrayList<Item> contents(){
		ArrayList<Item> list = new ArrayList<>();
		for (ItemButton b : slots) if (b.item() != null) list.add(b.item());
		return list;
	}

	//칸에 든 것으로 어떤 의뢰인지 정한다 (칸 순서 상관없음)
	private void evaluate(){
		kind = Kind.NONE;
		blueprint = null;
		ArrayList<Item> c = contents();
		Armor armor = null; MithrilShard shard = null; EnchantedMithrilShard eshard = null;
		ArrayList<Item> weapons = new ArrayList<>();
		boolean dup = false;
		for (int i = 0; i < c.size(); i++){
			for (int j = i + 1; j < c.size(); j++) if (c.get(i) == c.get(j)) dup = true;
			Item it = c.get(i);
			if (it instanceof Armor) armor = (Armor) it;
			else if (it instanceof MithrilShard) shard = (MithrilShard) it;
			else if (it instanceof EnchantedMithrilShard) eshard = (EnchantedMithrilShard) it;
			else if (it instanceof MeleeWeapon) weapons.add(it);
		}
		String text;
		int cost = 0;
		if (!dup && c.size() == 2 && armor != null && shard != null){
			if (armor.mazeMithril){
				text = Messages.get(this, "armor_done");
			} else {
				kind = Kind.ARMOR;
				cost = MazeTower.MITHRIL_ARMOR_COST;
				text = Messages.get(this, "armor_result", armor.name(), cost);
			}
		} else if (!dup && c.size() == 3 && eshard != null && weapons.size() == 2){
			blueprint = MithrilBlueprint.match(weapons.get(0), weapons.get(1));
			if (blueprint == null){
				text = Messages.get(this, "no_recipe");
			} else {
				kind = Kind.BLUEPRINT;
				cost = MazeTower.MITHRIL_BLUEPRINT_COST;
				text = Messages.get(this, "blueprint_result", blueprint.title(), cost);
			}
		} else {
			text = c.isEmpty() ? Messages.get(this, "none") : Messages.get(this, "no_recipe");
		}
		if (kind != Kind.NONE && Blacksmith.Quest.favor < cost) text += " " + Messages.get(this, "no_favor", Blacksmith.Quest.favor);
		result.text( text );
		btnCommission.enable( kind != Kind.NONE && Blacksmith.Quest.favor >= cost );
		btnCommission.setPos( 0, result.bottom() + BTN_GAP + 10 );
		resize( WIDTH, (int) btnCommission.bottom() );
	}

	private void commission(){
		Hero hero = Dungeon.hero;
		ArrayList<Item> c = contents();
		if (kind == Kind.ARMOR){
			Armor armor = null; Item shard = null;
			for (Item it : c){ if (it instanceof Armor) armor = (Armor) it; else if (it instanceof MithrilShard) shard = it; }
			if (armor == null || shard == null || armor.mazeMithril) return;
			shard.detach( hero.belongings.backpack );
			armor.mazeMithril = true; //방어 +2, 회피 ×1.2 (강화·마법 부여·저주는 그대로)
			Blacksmith.Quest.favor -= MazeTower.MITHRIL_ARMOR_COST;
			GLog.p( Messages.get(this, "armor_done_log", armor.name()) );
		} else if (kind == Kind.BLUEPRINT && blueprint != null){
			//장착한 저주받은 무기는 벗을 수 없으니(원작 규칙) 아무것도 쓰기 전에 막는다
			for (Item it : c){
				if (it instanceof MeleeWeapon && it.isEquipped(hero) && it.cursed){
					GLog.w( Messages.get(EquipableItem.class, "unequip_cursed") );
					return;
				}
			}
			for (Item it : c){
				if (it instanceof EnchantedMithrilShard){
					it.detach( hero.belongings.backpack );
				} else if (it instanceof MeleeWeapon){
					if (it.isEquipped(hero)) ((EquipableItem) it).doUnequip(hero, false, false);
					it.detachAll( hero.belongings.backpack );
				}
			}
			Blacksmith.Quest.favor -= MazeTower.MITHRIL_BLUEPRINT_COST;
			MithrilBlueprint bp = new MithrilBlueprint( blueprint );
			if (!bp.collect()) Dungeon.level.drop( bp, hero.pos ).sprite.drop();
			GLog.p( Messages.get(this, "blueprint_log", bp.name()) );
		} else {
			return;
		}
		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		Item.evoke( hero );
		Item.updateQuickslot();
		hide();
		if (parentWnd != null) parentWnd.hide();
	}

	private final WndBag.ItemSelector selector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(WndMithrilForge.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			//(사용자) 무기·갑옷은 감정된 것만, 그리고 저주받은 것은 재료 칸에 넣을 수 없다
			//  예외 (사용자): 지금 입고 있는 저주받은 갑옷은 (감정 전이어도) 미스릴 보강을 의뢰할 수 있다.
			//  보강은 갑옷에 표시만 더하므로 벗기지 않고 입은 채로 둔다 (저주도 그대로)
			if (item instanceof Armor && item.cursed && item.isEquipped(Dungeon.hero)) return true;
			if ((item instanceof Armor || item instanceof MeleeWeapon) && (!item.isIdentified() || item.cursed)) return false;
			return item instanceof MithrilShard || item instanceof EnchantedMithrilShard
					|| item instanceof Armor || item instanceof MeleeWeapon;
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && pressed != null && pressed.parent != null){
				pressed.item( item );
				evaluate();
			}
		}
	};
}
