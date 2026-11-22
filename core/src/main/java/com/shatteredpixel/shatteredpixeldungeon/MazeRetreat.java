/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TowerCollapse;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rubble;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

//Maze Tower: 두 갈래 엔딩과 역행 (명세 14번).
//  - 26층에서 아뮬렛을 얻으면 습득(배드 엔딩) / 파괴(역행 시작) 중 하나를 고른다 (AmuletScene)
//  - 역행은 원작 승천 구조를 그대로 쓴다: 원작 '부적의 저주'(AscensionChallenge)가 붙어 있으면 역행 중이다.
//    저주는 '아뮬렛의 저주'로 바뀌어 느려짐·명중률 감소·적 광폭화·적을 죽이면 약해짐만 남는다.
//  - 층마다 붕괴 턴(TowerCollapse), 막힌 길(Rubble), 26층 쪽 계단은 막힌다.
//  - 1층에서 탈출하면 진 엔딩 (원작 승천 탈출, SewerLevel)
public class MazeRetreat {

	public static boolean active(){
		return !MazeTower.ORIGINAL_ENDING && Dungeon.hero != null
				&& Dungeon.hero.buff(AscensionChallenge.class) != null;
	}

	// ===== 엔딩 =====

	//습득(배드 엔딩)으로 끝난 판의 랭킹 사망 원인 표시 ("타락")
	public static class Corrupted {}

	//파괴를 고른 뒤 게임 화면으로 돌아가면 역행을 시작한다 (AmuletScene → GameScene.create)
	public static boolean pendingBegin = false;

	public static void begin( Hero hero ){
		pendingBegin = false;
		if (active()) return;

		//역행 중에는 탑의 심장의 시야 감소가 없다 (지금 서 있는 층에도 바로)
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HallsLevel
				|| Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel
				|| Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HeartBossLevel
				|| Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.LastLevel){
			Dungeon.level.viewDistance = Dungeon.isChallenged(Challenges.DARKNESS) ? 2 : 8;
		}

		//아뮬렛은 부서져 사라진다
		for (Item i : hero.belongings.getAllItems(Amulet.class)){
			i.detachAll(hero.belongings.backpack);
		}

		Buff.affect(hero, AscensionChallenge.class);
		Statistics.highestAscent = Dungeon.depth;
		Buff.affect(hero, TowerCollapse.class).startFloor(Dungeon.depth);

		PixelScene.shake(3, 1.5f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		GLog.n(Messages.get(MazeRetreat.class, "begin"));
	}

	//진 엔딩: 처음으로 역행을 클리어하면 기록해 둔다 (나중에 익스트랙션 모드가 열린다)
	public static void markCleared(){
		SPDSettings.mazeRetreatCleared(true);
	}

	// ===== 층이 바뀔 때 =====

	//역행 중 새 층(처음 내려온 층)에 들어섰을 때 (AscensionChallenge.onLevelSwitch)
	public static void onNewFloor( Level level, int arrival ){
		Buff.affect(Dungeon.hero, TowerCollapse.class).startFloor(Dungeon.depth);
		if (level instanceof RegularLevel && !Dungeon.bossLevel()){
			placeRubble(level, arrival);
		}
		if (!Dungeon.bossLevel()) refillMobs(level);
	}

	//역행 중 층에 들어설 때 적을 다시 깐다: 원래 그 층 적 수(mobLimit)의 RETREAT_MOB_FILL 비율까지 (원작 승천보다 적게)
	private static void refillMobs( Level level ){
		int target = Math.round(level.mobLimit() * MazeTower.RETREAT_MOB_FILL);
		int tries = 0;
		while (level.mobCount() < target && tries++ < 30){
			level.spawnMob(12);
		}
	}

	//26층 쪽(이미 지나온 층)으로 가는 계단은 무너져 쓸 수 없다
	public static boolean blocksTransition( LevelTransition transition ){
		if (!active() || transition == null) return false;
		return transition.type == LevelTransition.Type.REGULAR_EXIT
				|| transition.type == LevelTransition.Type.BRANCH_EXIT;
	}

	// ===== 막힌 길: 돌무더기 =====

	//길을 완전히 막지 않는 칸(주로 통로)에 1~2개
	private static void placeRubble( Level level, int arrival ){
		LevelTransition up = level.getTransition(LevelTransition.Type.REGULAR_ENTRANCE);
		if (up == null) return;
		int goal = up.cell();

		boolean[] blocked = new boolean[level.length()];
		ArrayList<Integer> corridors = new ArrayList<>();
		ArrayList<Integer> others = new ArrayList<>();
		int w = level.width();
		for (int i = 0; i < level.length(); i++){
			if (!level.passable[i] || level.solid[i] || level.pit[i]) continue;
			if (Actor.findChar(i) != null || level.heaps.get(i) != null) continue;
			if (level.distance(i, goal) <= 3 || (arrival >= 0 && level.distance(i, arrival) <= 3)) continue;
			if (level.getTransition(i) != null) continue;
			//통로: 위아래나 좌우 한쪽으로만 뚫려 있는 칸
			boolean ns = pass(level, i - w) && pass(level, i + w) && !pass(level, i - 1) && !pass(level, i + 1);
			boolean ew = pass(level, i - 1) && pass(level, i + 1) && !pass(level, i - w) && !pass(level, i + w);
			if (ns || ew) corridors.add(i);
			else          others.add(i);
		}
		Collections.shuffle(corridors, new java.util.Random(Random.Long()));
		Collections.shuffle(others, new java.util.Random(Random.Long()));
		ArrayList<Integer> candidates = new ArrayList<>(corridors);
		candidates.addAll(others);

		int count = Random.IntRange(MazeTower.RUBBLE_MIN, MazeTower.RUBBLE_MAX);
		int start = arrival >= 0 ? arrival : goal;
		for (int cell : candidates){
			if (count <= 0) break;
			blocked[cell] = true;
			if (reachableWithout(level, blocked, start, goal)){
				Rubble r = new Rubble();
				r.pos = cell;
				level.mobs.add(r);
				count--;
			} else {
				blocked[cell] = false;
			}
		}
	}

	private static boolean pass( Level level, int cell ){
		return cell >= 0 && cell < level.length() && level.passable[cell];
	}

	//돌무더기가 막아도 도착한 곳에서 1층 쪽 계단까지 갈 수 있는지
	private static boolean reachableWithout( Level level, boolean[] blocked, int from, int to ){
		boolean[] ok = BArray.or(level.passable, level.avoid, null);
		for (int i = 0; i < ok.length; i++){
			if (blocked[i]) ok[i] = false;
		}
		ok[from] = true;
		PathFinder.buildDistanceMap(to, ok);
		return PathFinder.distance[from] != Integer.MAX_VALUE;
	}

	//역행 중(또는 막 시작하려는 중)에는 탑의 심장 그림을 역행 버전으로 (명세 20번)
	public static String tiles( String tex ){
		if ((active() || pendingBegin) && Assets.Environment.TILES_HALLS.equals(tex)) return Assets.Environment.TILES_HALLS_RETURN;
		return tex;
	}

	public static String features( String tex ){
		if ((active() || pendingBegin) && Assets.Environment.TERRAIN_FEATURES.equals(tex)) return Assets.Environment.TERRAIN_FEATURES_RETURN;
		if ((active() || pendingBegin) && Assets.Environment.RAISED_TERRAIN.equals(tex)) return Assets.Environment.RAISED_TERRAIN_RETURN;
		return tex;
	}

	public static String water( String tex ){
		if ((active() || pendingBegin) && Assets.Environment.WATER_HALLS.equals(tex)) return Assets.Environment.WATER_HALLS_RETURN;
		return tex;
	}
}
