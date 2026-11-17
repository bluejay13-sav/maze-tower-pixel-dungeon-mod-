/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.DeviceCompat;

//Maze Tower: 테스트용 치트 메뉴. 디버그 빌드(desktop:debug)에서만 게임 메뉴에 버튼이 생긴다.
//이후 단계에서 개화의 핵, 마나, 젖음·정전기 항목을 여기에 추가한다.
public class WndCheats extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP        = 2;

	//검 계열 소환 시 한 번에 주는 강화 주문서 수
	private static final int UPGRADE_SCROLLS_PER_CLICK = 1;

	//이동 가능한 가장 깊은 층
	private static final int MAX_DEPTH = 26;

	private float pos = 0;

	public static boolean enabled(){
		return DeviceCompat.isDebug();
	}

	public WndCheats(){
		super();

		addButton(new RedButton(Messages.get(this, "divine_potion"), 7){
			@Override
			protected void onClick() {
				give(new PotionOfDivineInspiration().identify());
			}
		});

		addButton(new RedButton(Messages.get(this, "level_up"), 7){
			@Override
			protected void onClick() {
				Hero hero = Dungeon.hero;
				hero.earnExp(hero.maxExp() - hero.exp, WndCheats.class);
			}
		});

		addButton(new RedButton(Messages.get(this, "fill_mana"), 7){
			@Override
			protected void onClick() {
				Mana.get(Dungeon.hero).fill();
			}
		});

		addButton(new RedButton(Messages.get(this, "swords"), 7){
			@Override
			protected void onClick() {
				give(new WornShortsword().identify());
				give(new Shortsword().identify());
				give(new Rapier().identify());
				give(new Scimitar().identify());
				give(new Longsword().identify());
				give(new Katana().identify());
				give(new RunicBlade().identify());
			}
		});

		addButton(new RedButton(Messages.get(this, "upgrade"), 7){
			@Override
			protected void onClick() {
				give(new ScrollOfUpgrade().quantity(UPGRADE_SCROLLS_PER_CLICK).identify());
			}
		});

		addButton(new RedButton(Messages.get(this, "depth"), 7){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndTextInput(Messages.get(WndCheats.class, "depth"),
						Messages.get(WndCheats.class, "depth_body", MAX_DEPTH),
						Integer.toString(Dungeon.depth), 2, false,
						Messages.get(WndCheats.class, "go"), Messages.get(WndCheats.class, "cancel")){
					@Override
					public void onSelect(boolean positive, String text) {
						if (!positive) return;
						int depth;
						try {
							depth = Integer.parseInt(text.trim());
						} catch (NumberFormatException e){
							return;
						}
						if (depth < 1 || depth > MAX_DEPTH) return;
						goToDepth(depth);
					}
				});
			}
		});

		addButton(new RedButton(Messages.get(this, "status"), 7){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndOptions(Messages.get(WndCheats.class, "status"),
						Messages.get(WndCheats.class, "status_body"),
						Messages.get(WndCheats.class, "burning"),
						Messages.get(WndCheats.class, "chill")){
					@Override
					protected void onSelect(int index) {
						pickTarget(index);
					}
				});
			}
		});

		resize(WIDTH, (int)pos);
	}

	private void addButton(RedButton btn){
		add(btn);
		btn.setRect(0, pos > 0 ? pos += GAP : 0, WIDTH, BTN_HEIGHT);
		pos += BTN_HEIGHT;
	}

	private static void give(Item item){
		if (item.collect()){
			GLog.p(Messages.get(WndCheats.class, "got", item.name()));
		} else {
			Dungeon.level.drop(item, Dungeon.hero.pos).sprite.drop();
		}
	}

	private static void goToDepth(int depth){
		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = depth;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1;
		Game.switchScene(InterlevelScene.class);
	}

	private static void pickTarget(final int statusIndex){
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				Char ch = Actor.findChar(cell);
				if (ch == null || ch == Dungeon.hero) return;
				switch (statusIndex){
					case 0:
						Buff.affect(ch, Burning.class).reignite(ch);
						break;
					case 1:
						Buff.affect(ch, Chill.class, Chill.DURATION);
						break;
				}
			}

			@Override
			public String prompt() {
				return Messages.get(WndCheats.class, "pick_target");
			}
		});
	}
}
