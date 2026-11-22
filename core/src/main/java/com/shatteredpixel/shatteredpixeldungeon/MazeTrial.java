/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.StartScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

//Maze Tower: 보스전 체험판 (MazeTower.BOSS_TRIAL이 켜진 빌드에서만).
//시작 화면 "새 게임" → 원작 영웅 선택 화면(전사·사냥꾼) → 시작 → 보스 셋 중 하나를 고르고, 고른 직업으로 새 게임을 시작해
//곧바로 층 건너뛰기(MazeSkip)로 보스층 입구에 도착한다. 5층 썩은 수액 덩어리는 5층(레벨 6, 1~5층 아이템),
//20층 망령 왕·마도시의 여제는 20층(레벨 19, 지급품)에서 시작한다. 저장된 판은 시작 화면 "이어하기"로 바로 불러온다.
//보스를 쓰러뜨리면 체험 완료 창이 뜬다.
public class MazeTrial {

	public static final int NONE = 0, SAP = 1, WRAITH = 2, EMPRESS = 3, HEART = 4; //(2026-11-22) 25층 심장을 진 짐승 추가

	//이번 판의 체험 보스 (저장된다: Hero 번들). 20층 보스를 고정하는 데 쓴다
	public static int boss = NONE;
	//새 게임을 시작한 뒤 건너뛸 목표 층 (0이면 없음)
	private static int pendingSkip = 0;

	public static boolean on(){
		return MazeTower.BOSS_TRIAL;
	}

	//(결정, 2026-11-21) 체험판은 전사·사냥꾼만 고를 수 있다 (영웅 선택 화면에서 다른 직업은 어둡게, 누르면 안내)
	public static boolean locked( HeroClass cl ){
		return on() && cl != HeroClass.WARRIOR && cl != HeroClass.HUNTRESS;
	}

	//체험판에서는 사냥꾼도 처음부터 열려 있다 (원작은 배지가 있어야 열림)
	public static boolean unlocked( HeroClass cl ){
		return cl.isUnlocked() || (on() && !locked(cl));
	}

	//시작 화면 "새 게임": 원작처럼 영웅 선택 화면 (전사·사냥꾼만 고를 수 있음). 시작을 누르면 showSelect
	public static void newGame(){
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
		GamesInProgress.curSlot = 1;
		ShatteredPixelDungeon.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene.class);
	}

	//가장 최근에 한 저장된 판 (없으면 null)
	public static GamesInProgress.Info latestSave(){
		GamesInProgress.Info best = null;
		for (GamesInProgress.Info info : GamesInProgress.checkAll()){
			if (best == null || info.lastPlayed > best.lastPlayed) best = info;
		}
		return best;
	}

	//시작 화면 "이어하기": 가장 최근 저장된 판을 바로 불러온다
	public static void continueLatest(){
		GamesInProgress.Info info = latestSave();
		if (info == null) return;
		GamesInProgress.curSlot = info.slot;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		ActionIndicator.clearAction();
		com.shatteredpixel.shatteredpixeldungeon.ui.FinisherIndicator.clearAction(null);
		InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
		ShatteredPixelDungeon.switchScene(InterlevelScene.class);
	}

	//영웅 선택 화면의 시작 버튼: 도전할 보스 고르기 (5층 / 20층 두 보스)
	public static void showSelect(){
		ShatteredPixelDungeon.scene().addToFront(new WndOptions(
				Messages.get(MazeTrial.class, "title"),
				Messages.get(MazeTrial.class, "desc"),
				Messages.get(MazeTrial.class, "sap"), Messages.get(MazeTrial.class, "wraith"), Messages.get(MazeTrial.class, "empress"), Messages.get(MazeTrial.class, "heart")){
			@Override
			protected void onSelect(int index) {
				start(index == 0 ? SAP : index == 1 ? WRAITH : index == 2 ? EMPRESS : HEART);
			}
		});
	}

	//영웅 선택 화면에서 고른 직업(전사·사냥꾼)으로 새 게임을 시작한다 (빈 저장 칸, 없으면 1번 칸을 덮어씀)
	public static void start( int which ){
		boss = which;
		applyBossChoice();
		pendingSkip = which == SAP ? MazeTower.TRIAL_SAP_DEPTH : which == HEART ? MazeTower.TRIAL_HEART_DEPTH : MazeTower.TRIAL_CITY_DEPTH;
		int slot = GamesInProgress.firstEmpty();
		if (slot == -1){
			slot = 1;
			Dungeon.deleteGame(slot, true);
		}
		GamesInProgress.curSlot = slot;
		if (GamesInProgress.selectedClass == null || locked(GamesInProgress.selectedClass)) GamesInProgress.selectedClass = HeroClass.WARRIOR;
		MazeNames.pending = "";
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.initSeed();
		ActionIndicator.clearAction();
		com.shatteredpixel.shatteredpixeldungeon.ui.FinisherIndicator.clearAction(null);
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene(InterlevelScene.class);
	}

	//20층 보스를 고른 쪽으로 고정
	private static void applyBossChoice(){
		if (boss == WRAITH)       MazeBosses.force20 = false;
		else if (boss == EMPRESS) MazeBosses.force20 = true;
		else                      MazeBosses.force20 = null;
	}

	//GameScene.create 끝에서 부른다: 새 게임 1층에 도착하면 곧바로 목표 층까지 건너뛴다 (치트의 층 건너뛰기와 같다)
	public static void afterSceneCreate(){
		if (!on() || pendingSkip <= 0 || Dungeon.hero == null || Dungeon.depth != 1) return;
		final int depth = pendingSkip;
		pendingSkip = 0;
		Game.runOnRenderThread(new com.watabou.utils.Callback() {
			@Override
			public void call() {
				Hero hero = Dungeon.hero;
				//보스층에서 바로 시작하지만 레벨은 그 앞 층 건너뛰기와 같다 (5층 → 레벨 6, 20층 → 레벨 19)
				int lvl = MazeSkip.levelFor(depth - 1);
				while (hero.lvl < lvl){
					hero.earnExp(hero.maxExp() - hero.exp, MazeTrial.class);
				}
				hero.HP = hero.HT;
				Mana.get(hero).fill();
				InterlevelScene.mazeSkipTarget = depth;
				Level.beforeTransition();
				InterlevelScene.mode = InterlevelScene.Mode.RETURN;
				InterlevelScene.returnDepth = depth;
				InterlevelScene.returnBranch = 0;
				InterlevelScene.returnPos = -1;
				Game.switchScene(InterlevelScene.class);
			}
		});
	}

	//보스를 쓰러뜨렸다 (5층 수액 덩어리, 20층 두 보스)
	public static void onBossDefeated(){
		if (!on()) return;
		GameScene.show(new WndOptions(
				Messages.get(MazeTrial.class, "clear_title"),
				Messages.get(MazeTrial.class, "clear_desc"),
				Messages.get(MazeTrial.class, "to_title"),
				Messages.get(MazeTrial.class, "stay")){
			@Override
			protected void onSelect(int index) {
				if (index == 0){
					try {
						Dungeon.saveAll();
					} catch (Exception e){
						ShatteredPixelDungeon.reportException(e);
					}
					ShatteredPixelDungeon.switchNoFade(TitleScene.class);
				}
			}
		});
	}

	//(결정) 체험판에서 19층으로 건너뛸 때(20층 보스) 더 주는 물건 (MazeSkip.generate에서 받는다). (2026-11-21) 사냥꾼은 검 대신 활 7종·투창, 화살은 5층·20층 모두:
	//모든 물약 감정된 채 5개씩, 모든 주문서 감정된 채 5개씩(강화의 주문서만 12개), 전사 계열 검 7종(감정됨), 힘 16 갑옷(비늘 갑옷, 감정됨)
	public static java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> kit( int target ){
		java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> items = new java.util.ArrayList<>();
		if (!on() || (target != MazeTower.TRIAL_CITY_DEPTH && target != MazeTower.TRIAL_SAP_DEPTH && target != MazeTower.TRIAL_HEART_DEPTH)) return items; //(2026-11-22) 25층 짐승도 20층과 같은 지급품
		//(결정) 모든 반지 감정된 채 +2 (마법봉·유물·장신구는 게임 메뉴의 체험판 아이템 창에서 골라 받는다, WndTrialItems)
		for (Class<?> c : com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.RING.classes){
			com.shatteredpixel.shatteredpixeldungeon.items.Item r = (com.shatteredpixel.shatteredpixeldungeon.items.Item) com.watabou.utils.Reflection.newInstance(c);
			if (r == null) continue;
			r.upgrade(MazeTower.TRIAL_RING_UPGRADE);
			items.add(r.identify());
		}
		boolean huntress = Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.HUNTRESS;
		//(결정, 2026-11-21) 사냥꾼은 화살을 넉넉히 (5층·20층 모두)
		if (huntress) items.add(new com.shatteredpixel.shatteredpixeldungeon.items.Arrow().quantity(target != MazeTower.TRIAL_SAP_DEPTH ? MazeTower.TRIAL_ARROWS_CITY : MazeTower.TRIAL_ARROWS_SAP));
		//(결정) 반지는 5층·20층 둘 다, 아래 물약·주문서·검·갑옷은 20층만
		if (target != MazeTower.TRIAL_CITY_DEPTH && target != MazeTower.TRIAL_HEART_DEPTH) return items;
		for (Class<?> c : com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.POTION.classes){
			com.shatteredpixel.shatteredpixeldungeon.items.Item p = (com.shatteredpixel.shatteredpixeldungeon.items.Item) com.watabou.utils.Reflection.newInstance(c);
			if (p == null) continue;
			p.identify();
			items.add(p.quantity(MazeTower.TRIAL_POTIONS));
		}
		for (Class<?> c : com.shatteredpixel.shatteredpixeldungeon.items.Generator.Category.SCROLL.classes){
			com.shatteredpixel.shatteredpixeldungeon.items.Item s = (com.shatteredpixel.shatteredpixeldungeon.items.Item) com.watabou.utils.Reflection.newInstance(c);
			if (s == null) continue;
			s.identify();
			boolean upgrade = s instanceof com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
			items.add(s.quantity(upgrade ? MazeTower.TRIAL_UPGRADES : MazeTower.TRIAL_SCROLLS));
		}
		if (huntress){
			//(결정, 2026-11-21) 사냥꾼: 활 7종(감정됨) + 고리지기 연속 투척을 시험할 투창
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PelletBow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longbow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HornBow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MazeCrossbow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.GreatBow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MazeSpiritBow().identify());
			items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.Javelin().quantity(MazeTower.TRIAL_JAVELINS).identify());
		} else {
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword().identify());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword().identify());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier().identify());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar().identify());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword().identify());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana().identify());
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade().identify());
		}
		items.add(new com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor().identify()); //4티어, 힘 16
		return items;
	}

	// ===== 저장 (Hero 번들) =====
	private static final String BOSS = "maze_trial_boss";

	public static void store( Bundle bundle ){
		if (boss != NONE) bundle.put(BOSS, boss);
	}

	public static void restore( Bundle bundle ){
		boss = bundle.contains(BOSS) ? bundle.getInt(BOSS) : NONE;
		if (on() && boss != NONE) applyBossChoice();
	}
}
