/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.MazeRetreat;
import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TowerCollapse;
import com.shatteredpixel.shatteredpixeldungeon.effects.RetreatEffects;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.items.BloomCore;
import com.shatteredpixel.shatteredpixeldungeon.items.FullBloomCore;
import com.shatteredpixel.shatteredpixeldungeon.items.FruitCore;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornDagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpear;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
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

	private static final int WIDTH      = 150; //버튼 두 개씩 놓으려고 넓힘
	private static final int BTN_HEIGHT = 15;
	private static final int GAP        = 1;

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

		addPair(new RedButton(Messages.get(this, "bloom_core"), 7){
			@Override
			protected void onClick() {
				give(new BloomCore());
			}
		}, new RedButton(Messages.get(this, "fullbloom_core"), 7){
			@Override
			protected void onClick() {
				give(new FullBloomCore());
			}
		});

		addButton(new RedButton(Messages.get(this, "boss20"), 7){
			@Override
			protected void onClick() {
				MazeBosses.force20 = !MazeBosses.empress();
				GLog.p(Messages.get(WndCheats.class, "boss_now", Messages.get(DwarfKing.class, MazeBosses.empress() ? "maze_empress_name" : "maze_wraith_name")));
			}
		});

		addPair(new RedButton(Messages.get(this, "retreat_start"), 7){
			@Override
			protected void onClick() {
				//아뮬렛 없이 그 자리에서 역행 시작 (시험용)
				if (MazeRetreat.active()){
					GLog.w(Messages.get(WndCheats.class, "retreat_already"));
					return;
				}
				hide();
				MazeRetreat.begin(Dungeon.hero);
				RetreatEffects.addOverlays((GameScene) com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.scene());
			}
		}, new RedButton(Messages.get(this, "collapse_now"), 7){
			@Override
			protected void onClick() {
				TowerCollapse c = Dungeon.hero.buff(TowerCollapse.class);
				if (c == null){
					GLog.w(Messages.get(WndCheats.class, "retreat_not_active"));
					return;
				}
				c.cheatCollapse();
			}
		});

		addPair(new RedButton(Messages.get(this, "fruit_core"), 7){
			@Override
			protected void onClick() {
				give(new FruitCore());
			}
		}, new RedButton(Messages.get(this, "armor_charge"), 7){
			@Override
			protected void onClick() {
				if (!(Dungeon.hero.belongings.armor() instanceof ClassArmor)){
					GLog.w(Messages.get(WndCheats.class, "no_class_armor"));
					return;
				}
				((ClassArmor)Dungeon.hero.belongings.armor()).charge = 100;
				Item.updateQuickslot();
			}
		});

		addPair(new RedButton(Messages.get(this, "grant_talent"), 7){
			@Override
			protected void onClick() {
				hide();
				pickTalentCategory();
			}
		}, new RedButton(Messages.get(this, "divine_potion"), 7){
			@Override
			protected void onClick() {
				give(new PotionOfDivineInspiration().identify());
			}
		});

		//베타 테스트용 층 건너뛰기 (명세 8번): 4·9·14·19층까지
		addPair(skipButton(0), skipButton(1));
		addPair(skipButton(2), skipButton(3));
		if (MazeTower.HEART_BOSS || MazeTower.O_MASTER) addButton(skipButton(4)); //24층 (25층 보스 앞, 새 25층 보스가 모두 꺼져 있으면 숨김)

		addPair(new RedButton(Messages.get(this, "level_up"), 7){
			@Override
			protected void onClick() {
				Hero hero = Dungeon.hero;
				hero.earnExp(hero.maxExp() - hero.exp, WndCheats.class);
			}
		}, new RedButton(Messages.get(this, "level_up5"), 7){
			@Override
			protected void onClick() {
				Hero hero = Dungeon.hero;
				for (int i = 0; i < 5; i++) hero.earnExp(hero.maxExp() - hero.exp, WndCheats.class);
			}
		});


		addPair(new RedButton(Messages.get(this, "fill_mana"), 7){
			@Override
			protected void onClick() {
				Mana.get(Dungeon.hero).fill();
			}
		}, new RedButton(Messages.get(this, "empty_mana"), 7){
			@Override
			protected void onClick() {
				Mana m = Mana.get(Dungeon.hero);
				m.spend(m.cur());
			}
		});

		addPair(new RedButton(Messages.get(this, "swords"), 7){
			@Override
			protected void onClick() {
				give(new WornShortsword().identify());
				give(new Shortsword().identify());
				give(new Rapier().identify());
				give(new Scimitar().identify());
				give(new Longsword().identify());
				give(new Katana().identify());
				give(new RunicBlade().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword().identify()); //Maze Tower (W1)
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ResonanceSword().identify()); //Maze Tower (W2)
			}
		}, new RedButton(Messages.get(this, "upgrade"), 7){
			@Override
			protected void onClick() {
				give(new ScrollOfUpgrade().quantity(UPGRADE_SCROLLS_PER_CLICK).identify());
			}
		});

		//(결정) 힘의 물약 지급 대신 힘을 바로 20으로
		addPair(new RedButton(Messages.get(this, "str20"), 7){
			@Override
			protected void onClick() {
				Dungeon.hero.STR = MazeTower.CHEAT_STR;
				Dungeon.hero.sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE, Messages.get(WndCheats.class, "str_set", MazeTower.CHEAT_STR));
				com.shatteredpixel.shatteredpixeldungeon.Badges.validateStrengthAttained();
			}
		}, new RedButton(Messages.get(this, "water_items"), 7){
			@Override
			protected void onClick() {
				//물을 만드는 아이템: 폭풍 구름의 물약, 아쿠아 블라스트 (감정됨)
				give(new PotionOfStormClouds().quantity(2).identify());
				give(new AquaBrew().quantity(2).identify());
				//(E1·E2) 재충전 주문서·정화 물약 시험용
				give(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging().quantity(2).identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity().quantity(2).identify());
			}
		});

		addPair(new RedButton(Messages.get(this, "class_weapons"), 7){
			@Override
			protected void onClick() {
				give(new WornDagger().identify());
				give(new WornShortbow().identify());
				//Maze Tower: 활 7종
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PelletBow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longbow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HornBow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MazeCrossbow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.GreatBow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MazeSpiritBow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ReinforcedHornBow().identify()); //Maze Tower (W3)
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SilverFang().identify()); //Maze Tower (W4)
				give(new Arrow().quantity(50));
				give(new ThrowingSpear().quantity(2).identify());
			}
		}, new RedButton(Messages.get(this, "kill_near"), 7){
			@Override
			protected void onClick() {
				//주인공 주변 5×5(2칸 안)의 모든 적 처치
				int killed = 0;
				for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs.toArray(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob[0])){
					if (m.alignment != com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ENEMY || !m.isAlive()) continue;
					if (Dungeon.level.distance(m.pos, Dungeon.hero.pos) > 2) continue;
					m.die(Dungeon.hero);
					killed++;
				}
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i(Messages.get(WndCheats.class, "killed", killed));
			}
		});

		addPair(new RedButton(Messages.get(this, "wand"), 7){
			@Override
			protected void onClick() {
				give(Generator.randomUsingDefaults(Generator.Category.WAND).identify());
			}
		}, new RedButton(Messages.get(this, "skill_wands"), 7){
			@Override
			protected void onClick() {
				//지팡이 합성 스킬 시험용: 마탄·화염 폭발·서리·번개 막대 (감정됨)
				give(new WandOfMagicMissile().identify());
				give(new WandOfFireblast().identify());
				give(new WandOfFrost().identify());
				give(new WandOfLightning().identify());
			}
		});

		//Maze Tower (K8) 시험용: 미스릴 재료를 주고 우호도를 채운 뒤 미스릴 제작 칸을 바로 연다 (확인이 끝나면 지움)
		addButton(new RedButton(Messages.get(this, "mithril_test"), 7){
			@Override
			protected void onClick() {
				give(new com.shatteredpixel.shatteredpixeldungeon.items.quest.MithrilShard().quantity(3));
				give(new com.shatteredpixel.shatteredpixeldungeon.items.quest.EnchantedMithrilShard());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.quest.EnchantedMithrilShard()); //활 도안용
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longbow().identify());
				give(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HornBow().identify());
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.favor = Math.max(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith.Quest.favor, 5000);
				hide();
				GameScene.show(new WndMithrilForge(new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith(), null));
			}
		});

		//Maze Tower: 치유 물약 5개 (감정됨)
		addButton(new RedButton(Messages.get(this, "healing"), 7){
			@Override
			protected void onClick() {
				give(new com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing().identify().quantity(5));
			}
		});

		addButton(new RedButton(Messages.get(this, "depth"), 7){
			@Override
			protected void onClick() {
				hide();
				showDepthInput();
			}
		});


		addButton(new RedButton(Messages.get(this, "status"), 7){
			@Override
			protected void onClick() {
				hide();
				GameScene.show(new WndOptions(Messages.get(WndCheats.class, "status"),
						Messages.get(WndCheats.class, "status_body"),
						Messages.get(WndCheats.class, "burning"),
						Messages.get(WndCheats.class, "chill"),
						Messages.get(WndCheats.class, "wet"),
						Messages.get(WndCheats.class, "static"),
						Messages.get(WndCheats.class, "frostbite"),
						Messages.get(WndCheats.class, "paralysis"),
						Messages.get(WndCheats.class, "roots"),
						Messages.get(WndCheats.class, "slow")){
					@Override
					protected void onSelect(int index) {
						pickTarget(index);
					}
				});
			}
		});

		resize(WIDTH, (int)pos);
	}

	//버튼 두 개를 한 줄에 반씩

	//Maze Tower (J): 주인공에서 3칸(안 되면 2칸) 떨어진 빈 바닥에 몹 하나를 주인공을 쫓는 상태로 (확인이 끝난 몹의 부르기 버튼은 지운다)
	private static boolean spawnHunting(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m){
		for (int d = 3; d >= 2; d--){
			for (int c : com.watabou.utils.PathFinder.NEIGHBOURS8){
				int cell = Dungeon.hero.pos + c * d;
				if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.passable[cell] || Dungeon.level.solid[cell]
						|| Dungeon.level.pit[cell] || com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell) != null) continue;
				m.pos = cell;
				m.state = m.HUNTING;
				GameScene.add(m);
				m.beckon(Dungeon.hero.pos);
				return true;
			}
		}
		return false;
	}

	private void addPair(RedButton a, RedButton b){
		add(a);
		add(b);
		float y = pos > 0 ? pos += GAP : 0;
		a.setRect(0, y, (WIDTH - GAP)/2f, BTN_HEIGHT);
		b.setRect((WIDTH + GAP)/2f, y, (WIDTH - GAP)/2f, BTN_HEIGHT);
		pos += BTN_HEIGHT;
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

	//2차 특성 직접 얻기: 갈래(공격·형태·생존·유틸) → 특성 → 레벨. 2차 특성이 없는 직업(마법사·도적)은 안내만
	private static void pickTalentCategory(){
		if (BloomTalent.poolOf(Dungeon.hero) == null){
			GLog.w(Messages.get(WndCheats.class, "no_bloom_pool"));
			return;
		}
		BloomTalent.Branch[] b = BloomTalent.Branch.values();
		String[] cats = new String[b.length];
		for (int i = 0; i < b.length; i++) cats[i] = Messages.titleCase(b[i].title());
		GameScene.show(new WndOptions(Messages.get(WndCheats.class, "grant_talent"),
				Messages.get(WndCheats.class, "grant_category"), cats){
			@Override
			protected void onSelect(int index) {
				pickTalent(index);
			}
		});
	}

	//Maze Tower: 2차 특성 묶음 (영웅 직업의 풀, 갈래별)
	private static java.util.ArrayList<BloomTalent> talentGroup(int group){
		java.util.ArrayList<BloomTalent> all = new java.util.ArrayList<>();
		BloomTalent.Pool pool = BloomTalent.poolOf(Dungeon.hero);
		for (BloomTalent t : BloomTalent.values()){
			if (t.inPool(pool) && t.branch.ordinal() == group) all.add(t);
		}
		return all;
	}

	private static void pickTalent(final int category){
		final java.util.ArrayList<BloomTalent> group = talentGroup(category);
		String[] names = new String[group.size()];
		for (int i = 0; i < names.length; i++){
			BloomTalent t = group.get(i);
			int lvl = BloomTree.ownedLevel(Dungeon.hero, t);
			names[i] = Messages.titleCase(t.title()) + (lvl >= 0 ? " (" + lvl + (BloomTree.ownedAwakened(Dungeon.hero, t) ? "+" : "") + ")" : "");
		}
		GameScene.show(new WndOptions(Messages.get(WndCheats.class, "grant_talent"),
				Messages.get(WndCheats.class, "grant_pick"), names){
			@Override
			protected void onSelect(int index) {
				pickLevel(group.get(index));
			}
		});
	}

	private static void pickLevel(final BloomTalent talent){
		GameScene.show(new WndOptions(Messages.titleCase(talent.title()),
				Messages.get(WndCheats.class, "grant_level"), "0", "1", "2", "3",
				Messages.get(WndCheats.class, "grant_awaken")){
			@Override
			protected void onSelect(int index) {
				boolean awaken = index == 4;
				//각성은 1레벨 이상인 특성만 작동한다: 아직 없거나 0레벨이면 3레벨로 같이 준다
				int owned = BloomTree.ownedLevel(Dungeon.hero, talent);
				int lvl = awaken ? (owned >= 1 ? owned : MazeTower.BLOOM_MAX_TALENT_LEVEL) : index;
				if (!BloomTree.get(Dungeon.hero).cheatGrant(Dungeon.hero, talent, lvl, awaken)){
					GLog.w(Messages.get(WndCheats.class, "slots_full"));
					return;
				}
				GLog.p(Messages.get(WndCheats.class, awaken ? "granted_awaken" : "granted", Messages.titleCase(talent.title()), lvl));
			}
		});
	}

	//층 건너뛰기: 목표 레벨까지 올리고 체력·마나를 채운 뒤, 1층부터 목표 층까지 차례대로 만들며 아이템을 가져간다
	private RedButton skipButton( final int i ){
		final int depth = MazeTower.SKIP_DEPTHS[i];
		return new RedButton(Messages.get(this, "skip", depth), 7){
			@Override
			protected void onClick() {
				Hero hero = Dungeon.hero;
				if (Dungeon.depth >= depth){
					GLog.w(Messages.get(WndCheats.class, "skip_past"));
					return;
				}
				hide();
				int lvl = com.shatteredpixel.shatteredpixeldungeon.MazeSkip.levelFor(depth);
				while (hero.lvl < lvl){
					hero.earnExp(hero.maxExp() - hero.exp, WndCheats.class);
				}
				hero.HP = hero.HT;
				Mana.get(hero).fill();
				InterlevelScene.mazeSkipTarget = depth;
				goToDepth(depth);
			}
		};
	}

	//Maze Tower: 층 번호를 넣어 그 층으로 옮긴다 (치트 창, 테스트 빌드의 게임 메뉴 WndGame)
	public static void showDepthInput(){
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
				if (ch == null) return; //(C9 확인용) 주인공 자신에게도 걸 수 있다
				switch (statusIndex){
					case 0:
						Buff.affect(ch, Burning.class).reignite(ch);
						break;
					case 1:
						Buff.affect(ch, Chill.class, Chill.DURATION);
						break;
					case 2:
						Buff.affect(ch, Wet.class);
						break;
					case 3:
						Buff.prolong(ch, StaticCharge.class, MazeTower.STATIC_TURNS);
						break;
					case 4:
						Buff.affect(ch, Frostbite.class, MazeTower.FROSTBITE_TURNS);
						break;
					case 5: //(C2) 판정 확인용: 기절 10턴
						Buff.prolong(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, 10f);
						break;
					case 6: //속박 10턴
						Buff.prolong(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots.class, 10f);
						break;
					case 7: //감속 10턴
						Buff.prolong(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow.class, 10f);
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
