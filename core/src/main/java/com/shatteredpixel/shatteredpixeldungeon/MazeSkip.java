/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.BossTorch;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.utils.PathFinder;

import java.io.IOException;
import java.util.ArrayList;

//Maze Tower: 베타 테스트용 "층 건너뛰기" 치트 (명세 8번).
//1층부터 목표 층까지 그 판의 던전 시드 그대로 차례대로 층을 만들어 저장하고(평소 게임과 같은 순서라 시드가 어긋나지 않음),
//지나온 층(목표 층 포함)의 바닥에 놓인 아이템을 모두 가져온다. 목표 층의 올라가는 계단 바로 앞 칸에 도착한다.
//레벨·체력·마나는 치트 창(WndCheats)에서 미리 채운다.
//가져오지 않는 것: 상점 물건, 잠긴 상자·수정 상자(열쇠가 필요), 열쇠, 이슬, 5층 보스방 횃불, 몹이 떨어뜨릴 아이템, 퀘스트 보상.
public class MazeSkip {

	//목표 층 → 목표 레벨 (MazeTower.SKIP_DEPTHS / SKIP_LEVELS)
	public static int levelFor( int depth ){
		for (int i = 0; i < MazeTower.SKIP_DEPTHS.length; i++){
			if (MazeTower.SKIP_DEPTHS[i] == depth) return MazeTower.SKIP_LEVELS[i];
		}
		return 1;
	}

	//InterlevelScene(RETURN) 안에서 부른다. 목표 층 Level을 돌려준다 (아직 switchLevel 전)
	public static Level generate( int target ) throws IOException {
		ArrayList<Item> loot = new ArrayList<>();
		Level last = null;
		for (int d = 1; d <= target; d++){
			Dungeon.depth = d;
			Dungeon.branch = 0;
			Level l = Dungeon.levelHasBeenGenerated(d, 0)
					? Dungeon.loadLevel(GamesInProgress.curSlot)
					: Dungeon.newLevel();
			Dungeon.level = l;
			take(l, loot);
			if (d < target){
				Dungeon.saveLevel(GamesInProgress.curSlot);
			}
			last = l;
		}
		Statistics.deepestFloor = Math.max(Statistics.deepestFloor, target);

		//반드시 주는 것: 주문서 가방·물약 가방(없으면, 먼저 넣어야 주문서·물약이 가방으로 들어간다),
		//9층부터 개화의 핵, 14·19층은 만개의 핵도
		ArrayList<Item> give = new ArrayList<>();
		if (Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder.class) == null){
			give.add(new com.shatteredpixel.shatteredpixeldungeon.items.bags.ScrollHolder());
		}
		if (Dungeon.hero.belongings.getItem(com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier.class) == null){
			give.add(new com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier());
		}
		if (target >= MazeTower.SKIP_BLOOM_DEPTH)     give.add(new com.shatteredpixel.shatteredpixeldungeon.items.BloomCore());
		if (target >= MazeTower.SKIP_FULLBLOOM_DEPTH) give.add(new com.shatteredpixel.shatteredpixeldungeon.items.FullBloomCore());
		loot.addAll(0, give);

		//가방에 넣는다. 가방이 차면 목표 층의 도착 칸에 내려놓는다
		int arrive = arrivalCell(last);
		for (Item item : loot){
			if (!item.collect()){
				last.drop(item, arrive);
			}
		}
		return last;
	}

	private static void take( Level l, ArrayList<Item> loot ){
		ArrayList<Integer> emptied = new ArrayList<>();
		for (Heap h : l.heaps.valueList()){
			if (h.type == Heap.Type.FOR_SALE || h.type == Heap.Type.LOCKED_CHEST || h.type == Heap.Type.CRYSTAL_CHEST) continue;
			ArrayList<Item> keep = new ArrayList<>();
			for (Item item : h.items){
				//(결정) 시체 먼지(원작 마법사 퀘스트 물건)도 가져오지 않는다
				if (item instanceof Key || item instanceof Dewdrop || item instanceof BossTorch
						|| item instanceof com.shatteredpixel.shatteredpixeldungeon.items.quest.CorpseDust){
					keep.add(item);
				} else if (item instanceof Gold){
					Dungeon.gold += item.quantity();
					Statistics.goldCollected += item.quantity();
				} else if (item instanceof EnergyCrystal){
					Dungeon.energy += item.quantity();
				} else {
					loot.add(item);
				}
			}
			h.items.clear();
			h.items.addAll(keep);
			if (h.items.isEmpty()) emptied.add(h.pos);
		}
		for (int pos : emptied) l.heaps.remove(pos);
	}

	//올라가는 계단(다음 층 입구) 바로 앞 빈 칸
	public static int arrivalCell( Level l ){
		int exit = l.exit();
		for (int n : PathFinder.NEIGHBOURS4){
			int c = exit + n;
			if (ok(l, c)) return c;
		}
		for (int n : PathFinder.NEIGHBOURS8){
			int c = exit + n;
			if (ok(l, c)) return c;
		}
		return l.entrance();
	}

	private static boolean ok( Level l, int c ){
		if (c < 0 || c >= l.length() || !l.passable[c] || l.solid[c] || l.pit[c]) return false;
		for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : l.mobs){
			if (m.pos == c) return false;
		}
		return true;
	}
}
