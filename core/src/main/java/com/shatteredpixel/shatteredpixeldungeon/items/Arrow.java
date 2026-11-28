/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//Maze Tower: 화살. 활 계열 무기의 원거리 평타와 조준 사격이 1발씩 쓴다.
//(2026-11-21) 그림: 아이템 docs/sprites/hunter_fx/arrow_item.png, 날아가는 화살 fx_hunter_00~02 (Arrow.Shot)
public class Arrow extends Item {

	{
		image = com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW;
		stackable = true;
	}

	//(2026-11-22) 1개일 때는 화살 한 대 그림 (아이템 그림은 2개 묶음이라 바닥에 1개만 있어도 묶음으로 보였음)
	@Override
	public int image() {
		if (image == com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW && quantity() == 1){
			return com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW_SHOT;
		}
		return super.image();
	}

	@Override
	public boolean isUpgradable() { return false; }

	@Override
	public boolean isIdentified() { return true; }

	@Override
	public int value() { return quantity; }

	// ===== 주인공의 화살 =====

	public static int count( Hero hero ){
		if (hero == null) return 0;
		Arrow a = hero.belongings.getItem(Arrow.class);
		return a == null ? 0 : a.quantity();
	}

	//1발 쓴다. 없으면 false
	public static boolean use( Hero hero ){
		Arrow a = hero.belongings.getItem(Arrow.class);
		if (a == null || a.quantity() <= 0) return false;
		a.detach(hero.belongings.backpack);
		updateQuickslot();
		return true;
	}

	public static void dropAt( int cell, int n ){
		if (n <= 0 || cell < 0 || Dungeon.level == null) return;
		Dungeon.level.drop(new Arrow().quantity(n), cell).sprite.drop();
	}

	//화살 n발이 박혀 있던 적이 죽음: 화살마다 일정 확률로 그 자리에 떨어진다
	public static void recover( int cell, int n ){
		int got = 0;
		for (int i = 0; i < n; i++){
			//Maze Tower (D19): 화살 줍기 - 쓰러진 적이 화살을 더 자주 떨군다
			if (Random.Float() < MazeTower.ARROW_RECOVER_CHANCE
					+ com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomHuntress.arrowDropBonus()) got++;
		}
		dropAt(cell, got);
	}

	// ===== 적에게 박힌 화살 (눈에 보이지 않는 표시) =====

	public static class Stuck extends Buff {
		{ type = buffType.NEUTRAL; }

		public int count = 0;

		public static void add( Char ch ){
			Buff.affect(ch, Stuck.class).count++;
		}

		@Override
		public void detach() {
			//적이 죽으면 (Actor에서 빠지면서 버프가 떨어진다) 박힌 화살 일부가 떨어진다
			if (target != null && !target.isAlive()){
				recover(target.pos, count);
				count = 0;
			}
			super.detach();
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("count", count);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			count = bundle.getInt("count");
		}
	}

	// ===== 사냥꾼의 투척 무기 분해 =====

	public static boolean canDisassemble( Hero hero, MissileWeapon w ){
		return hero != null && hero.heroClass == HeroClass.HUNTRESS && MazeTower.isPlayableClass(hero.heroClass);
	}

	public static int arrowsFrom( MissileWeapon w ){
		int n = w.quantity() * w.tier * MazeTower.ARROWS_PER_THROWN_TIER;
		if (w.cursed) n /= 2;
		return Math.max(1, n);
	}

	public static void disassemble( Hero hero, MissileWeapon w ){
		int n = arrowsFrom(w);
		String name = w.name();
		w.detachAll(hero.belongings.backpack);
		Arrow a = new Arrow();
		a.quantity(n);
		if (!a.collect()){
			Dungeon.level.drop(a, hero.pos).sprite.drop();
		}
		GLog.p(Messages.get(Arrow.class, "disassembled", name, n));
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(1f);
		updateQuickslot();
	}

	//날아가는 화살 그림 (MissileSprite가 이 그림을 날린다). 기본 화살에는 입자가 없고,
	//강화 화살(석궁 장전)·정령 화살의 빛은 그림 가장자리에 들어 있다. 석궁 볼트는 따로 없다.
	public static class Shot extends Arrow {
		public Shot(){ this(0); }
		public Shot(int kind){
			image = kind == 1 ? com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW_LOADED
					: kind == 2 ? com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW_SPIRIT
					: com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW_SHOT;
		}
	}
}
