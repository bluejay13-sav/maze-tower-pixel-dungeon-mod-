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
//시작 화면의 "플레이"를 누르면 보스 셋 중 하나를 고르고, 전사로 새 게임을 시작해 곧바로 층 건너뛰기(MazeSkip)로
//보스층 바로 앞 층에 도착한다(그 층까지의 아이템·레벨을 모두 받은 채). 5층 썩은 수액 덩어리는 4층에서,
//20층 망령 왕·마도시의 여제는 19층에서 시작한다. 보스를 쓰러뜨리면 체험 완료 창이 뜬다.
public class MazeTrial {

	public static final int NONE = 0, SAP = 1, WRAITH = 2, EMPRESS = 3;

	//이번 판의 체험 보스 (저장된다: Hero 번들). 20층 보스를 고정하는 데 쓴다
	public static int boss = NONE;
	//새 게임을 시작한 뒤 건너뛸 목표 층 (0이면 없음)
	private static int pendingSkip = 0;

	public static boolean on(){
		return MazeTower.BOSS_TRIAL;
	}

	//시작 화면 "플레이": 보스 고르기 (저장된 판이 있으면 이어하기도)
	public static void showSelect(){
		final boolean hasSaves = !GamesInProgress.checkAll().isEmpty();
		String[] opts = hasSaves
				? new String[]{ Messages.get(MazeTrial.class, "sap"), Messages.get(MazeTrial.class, "wraith"), Messages.get(MazeTrial.class, "empress"), Messages.get(MazeTrial.class, "continue") }
				: new String[]{ Messages.get(MazeTrial.class, "sap"), Messages.get(MazeTrial.class, "wraith"), Messages.get(MazeTrial.class, "empress") };
		ShatteredPixelDungeon.scene().addToFront(new WndOptions(
				Messages.get(MazeTrial.class, "title"),
				Messages.get(MazeTrial.class, "desc"),
				opts){
			@Override
			protected void onSelect(int index) {
				if (index == 3){
					ShatteredPixelDungeon.switchNoFade(StartScene.class);
					return;
				}
				start(index == 0 ? SAP : index == 1 ? WRAITH : EMPRESS);
			}
		});
	}

	//전사로 새 게임을 시작한다 (빈 저장 칸, 없으면 1번 칸을 덮어씀)
	public static void start( int which ){
		boss = which;
		applyBossChoice();
		pendingSkip = which == SAP ? 4 : 19;
		int slot = GamesInProgress.firstEmpty();
		if (slot == -1){
			slot = 1;
			Dungeon.deleteGame(slot, true);
		}
		GamesInProgress.curSlot = slot;
		GamesInProgress.selectedClass = HeroClass.WARRIOR;
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
				int lvl = MazeSkip.levelFor(depth);
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
