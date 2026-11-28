/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.quest;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HornBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//Maze Tower (K8): 미스릴 무기 도안. 대장장이의 미스릴 제작 칸에서 마법이 부여된 미스릴 파편 + 정해진 무기 두 종(재료로 사라짐)으로 얻는다.
//원하는 근접 무기(활 포함)에 쓰면 그 무기가 미스릴 무기가 된다: 강화·마법 부여·저주는 그대로, 피해 ×1.2(MITHRIL_WEAPON_DMG),
//스킬 마나 -5(MITHRIL_MANA_CUT), 그리고 도안마다 고유 능력(아직 정하지 않음, 자리만). 한 무기에 한 번만.
public class MithrilBlueprint extends Item {

	//(사용자) 도안은 무기 계열마다 생김새가 다르다(검 도안·활 도안). (사용자) 도안은 계열과 상관없이 아무 근접 무기(활 포함)에나 쓸 수 있다.
	public enum Family {
		SWORD( MazeItemSprites.MITHRIL_BLUEPRINT ),
		BOW( MazeItemSprites.MITHRIL_BLUEPRINT_BOW );

		public final int image;
		Family( int image ){ this.image = image; }
	}

	//(사용자) 도안마다 필요한 무기 두 종이 정해져 있다. 조합표와 고유 능력은 나중에 정한다. 지금은 계열마다 시험용 한 가지(제안).
	public enum Kind {
		BLUE_EDGE( Family.SWORD, Shortsword.class, Longsword.class ),
		BLUE_STRING( Family.BOW, Longbow.class, HornBow.class );

		public final Family family;
		public final Class<? extends Item> a, b;
		Kind( Family family, Class<? extends Item> a, Class<? extends Item> b ){ this.family = family; this.a = a; this.b = b; }

		public String title(){ return Messages.get(MithrilBlueprint.class, "kind_" + name()); }
		public String ability(){ return Messages.get(MithrilBlueprint.class, "ability_" + name()); }
	}

	//두 무기가 어느 도안의 재료인가 (순서 상관없이, 같은 무기 두 번은 안 됨)
	public static Kind match( Item x, Item y ){
		if (x == null || y == null || x == y) return null;
		for (Kind k : Kind.values()){
			if ((x.getClass() == k.a && y.getClass() == k.b) || (x.getClass() == k.b && y.getClass() == k.a)) return k;
		}
		return null;
	}

	public static Kind kindOf( String name ){
		if (name == null) return null;
		try { return Kind.valueOf(name); } catch (Exception e) { return null; }
	}

	//무기 설명 창에 붙는 미스릴 안내
	public static String weaponInfo( String kindName ){
		Kind k = kindOf(kindName);
		return Messages.get(MithrilBlueprint.class, "weapon_info",
				Math.round((MazeTower.MITHRIL_WEAPON_DMG - 1) * 100), MazeTower.MITHRIL_MANA_CUT,
				k == null ? "?" : k.title(), k == null ? "" : k.ability());
	}

	public static final String AC_USE = "USE";

	{
		image = MazeItemSprites.MITHRIL_BLUEPRINT;
		defaultAction = AC_USE;
		stackable = false;
		bones = false;
	}

	public Kind kind = Kind.BLUE_EDGE;

	public MithrilBlueprint(){ }

	public MithrilBlueprint( Kind kind ){ this.kind = kind; }

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_USE );
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals(AC_USE)) return Messages.get(this, "ac_use");
		return super.actionName(action, hero);
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );
		if (action.equals(AC_USE)){
			curUser = hero;
			curItem = this;
			GameScene.selectItem( selector );
		}
	}

	private final WndBag.ItemSelector selector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(MithrilBlueprint.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			return item instanceof MeleeWeapon && ((Weapon) item).mazeMithril == null;
		}

		@Override
		public void onSelect( Item item ) {
			if (!(item instanceof MeleeWeapon) || ((Weapon) item).mazeMithril != null) return;
			Hero hero = curUser;
			//강화·마법 부여·저주는 손대지 않고 미스릴 표시만 붙인다
			((Weapon) item).mazeMithril = kind.name();
			detach( hero.belongings.backpack );
			hero.sprite.operate( hero.pos );
			Enchanting.show( hero, item );
			Sample.INSTANCE.play( Assets.Sounds.EVOKE );
			GLog.p( Messages.get(MithrilBlueprint.class, "applied", item.name()) );
			Item.updateQuickslot();
			hero.spend( 1f );
			hero.busy();
		}
	};

	//계열마다 다른 그림
	@Override
	public int image() {
		return kind.family.image;
	}

	@Override
	public String name() {
		return Messages.get(this, "name", kind.title());
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", kind.title(), kind.ability(),
				Math.round((MazeTower.MITHRIL_WEAPON_DMG - 1) * 100), MazeTower.MITHRIL_MANA_CUT);
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

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( "maze_kind", kind.name() );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		Kind k = kindOf( bundle.getString("maze_kind") );
		if (k != null) kind = k;
	}
}
