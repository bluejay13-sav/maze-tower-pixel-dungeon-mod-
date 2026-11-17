/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

//Maze Tower: 2차 전직 상태. 영웅에게 붙는 보이지 않는 버프로 저장·불러오기를 원작 방식 그대로 처리한다.
//
// - 개화의 핵을 쓰면 unlocked = true, 기본 선택권 1개.
// - Lv7~11 레벨업마다 선택권 1, 포인트 1. (핵을 쓰기 전에 오른 레벨도 핵을 쓰는 순간 한꺼번에 들어온다)
// - 선택권이 생기는 순간 그 칸의 선택지 3개를 던전 시드로 정해 저장한다.
//   이미 가진 특성과, 아직 고르지 않은 다른 칸의 선택지는 제외한다.
public class BloomTree extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	public static class Slot {
		public BloomTalent chosen = null;               //고른 특성 (null이면 아직 + 칸)
		public BloomTalent[] options = new BloomTalent[0]; //이 칸의 선택지
		public int level = 0;                           //고른 특성의 레벨 (0~2)

		public boolean isChosen(){
			return chosen != null;
		}
	}

	public boolean unlocked = false;
	public final ArrayList<Slot> slots = new ArrayList<>();

	//신성한 영감의 물약으로 얻은 2차 추가 포인트
	public int bonusPoints = 0;
	//신성한 영감의 물약으로 얻은 1차 추가 포인트 (1차 능력은 미완성, 쌓이기만 한다)
	public int firstTierBonus = 0;

	//상태창에서 이미 본 칸 수 (새로 생긴 + 칸만 빛나며 등장하게)
	public int seenSlots = 0;

	//1차 포인트 합계 (레벨 + 물약)
	public int firstTierTotal( Hero hero ){
		return firstTierPoints(hero) + firstTierBonus;
	}

	// ===== 가져오기 =====

	public static BloomTree get( Char ch ){
		BloomTree t = ch.buff(BloomTree.class);
		if (t == null) t = Buff.affect(ch, BloomTree.class);
		return t;
	}

	//영웅이 가진 특성 레벨. 없으면 -1. (7단계 특성 효과에서 사용)
	public static int level( Hero hero, BloomTalent talent ){
		if (hero == null) return -1;
		BloomTree t = hero.buff(BloomTree.class);
		if (t == null) return -1;
		for (Slot s : t.slots){
			if (s.chosen == talent) return s.level;
		}
		return -1;
	}

	public static boolean has( Hero hero, BloomTalent talent ){
		return level(hero, talent) >= 0;
	}

	// ===== 선택권·포인트 계산 =====

	//Lv7~11 중 도달한 레벨 수 (0~5)
	public static int bloomLevelsReached( Hero hero ){
		int reached = hero.lvl - MazeTower.BLOOM_FIRST_LEVEL + 1;
		int max = MazeTower.BLOOM_LAST_LEVEL - MazeTower.BLOOM_FIRST_LEVEL + 1;
		return Math.max(0, Math.min(max, reached));
	}

	//1차 포인트 (Lv2~6, 쌓이기만 한다)
	public static int firstTierPoints( Hero hero ){
		int reached = hero.lvl - MazeTower.FIRST_TIER_FIRST_LEVEL + 1;
		int max = MazeTower.FIRST_TIER_LAST_LEVEL - MazeTower.FIRST_TIER_FIRST_LEVEL + 1;
		return Math.max(0, Math.min(max, reached));
	}

	//핵을 쓰면 받게 될(또는 받은) 선택권 수
	public static int choicesFor( Hero hero ){
		return Math.min(MazeTower.BLOOM_SLOTS, 1 + bloomLevelsReached(hero));
	}

	public int pointsEarned( Hero hero ){
		if (!unlocked) return 0;
		return bloomLevelsReached(hero) + bonusPoints;
	}

	public int pointsSpent(){
		int spent = 0;
		for (Slot s : slots){
			if (s.isChosen()) spent += s.level;
		}
		return spent;
	}

	public int pointsAvailable( Hero hero ){
		return Math.max(0, pointsEarned(hero) - pointsSpent());
	}

	public int unchosenSlots(){
		int n = 0;
		for (Slot s : slots){
			if (!s.isChosen()) n++;
		}
		return n;
	}

	public boolean canUpgradeSomething( Hero hero ){
		if (pointsAvailable(hero) <= 0) return false;
		for (Slot s : slots){
			if (s.isChosen() && s.level < MazeTower.BLOOM_MAX_TALENT_LEVEL) return true;
		}
		return false;
	}

	//고를 칸이나 쓸 포인트가 남았는지 (상태창 알림용)
	public boolean hasPending( Hero hero ){
		return unlocked && (unchosenSlots() > 0 || canUpgradeSomething(hero));
	}

	// ===== 변화 =====

	//개화의 핵 사용
	public void unlock( Hero hero ){
		unlocked = true;
		sync(hero);
	}

	//받을 선택권만큼 칸을 만든다. 레벨업, 핵 사용, 창 열 때 호출해도 안전하다.
	public void sync( Hero hero ){
		if (!unlocked) return;
		int target = choicesFor(hero);
		//저장 데이터에서 선택지가 사라진(특성 이름이 바뀐 등) 미선택 칸은 다시 정한다
		for (int i = 0; i < slots.size(); i++){
			Slot s = slots.get(i);
			if (!s.isChosen() && s.options.length == 0){
				s.options = rollOptions(i);
			}
		}
		while (slots.size() < target){
			Slot s = new Slot();
			s.options = rollOptions(slots.size());
			slots.add(s);
		}
	}

	private BloomTalent[] rollOptions( int slotIndex ){
		ArrayList<BloomTalent> pool = new ArrayList<>(Arrays.asList(BloomTalent.values()));
		for (Slot s : slots){
			if (s.isChosen()) {
				pool.remove(s.chosen);
			} else {
				for (BloomTalent t : s.options) pool.remove(t);
			}
		}

		ArrayList<BloomTalent> picked = new ArrayList<>();
		//던전 시드 + 칸 번호로 정한다. 같은 시드, 같은 선택이면 같은 선택지.
		Random.pushGenerator(Dungeon.seed + 0x4D41_5A45L + 7919L * (slotIndex + 1));
		while (picked.size() < MazeTower.BLOOM_CHOICES_PER_SLOT && !pool.isEmpty()){
			picked.add(pool.remove(Random.Int(pool.size())));
		}
		Random.popGenerator();

		return picked.toArray(new BloomTalent[0]);
	}

	public void choose( int slotIndex, BloomTalent talent ){
		Slot s = slots.get(slotIndex);
		if (s.isChosen()) return;
		s.chosen = talent;
		s.level = 0;
		//남은 선택지는 자동으로 목록에 돌아간다 (다음에 만들어지는 칸에서 다시 나올 수 있다)
		s.options = new BloomTalent[0];
	}

	public boolean upgrade( Hero hero, int slotIndex ){
		Slot s = slots.get(slotIndex);
		if (!s.isChosen() || s.level >= MazeTower.BLOOM_MAX_TALENT_LEVEL) return false;
		if (pointsAvailable(hero) <= 0) return false;
		s.level++;
		return true;
	}

	//테스트용 치트: 특성을 원하는 레벨로 바로 얻는다 (포인트와 무관). 6칸이 모두 찼으면 false.
	public boolean cheatGrant( Hero hero, BloomTalent talent, int level ){
		unlocked = true;
		for (Slot s : slots){
			if (s.chosen == talent){
				s.level = level;
				return true;
			}
		}
		Slot target = null;
		for (Slot s : slots){
			if (!s.isChosen()){
				target = s;
				break;
			}
		}
		if (target == null){
			if (slots.size() >= MazeTower.BLOOM_SLOTS) return false;
			target = new Slot();
			slots.add(target);
		}
		target.chosen = talent;
		target.options = new BloomTalent[0];
		target.level = level;
		return true;
	}

	// ===== 저장 =====

	private static final String UNLOCKED = "unlocked";
	private static final String BONUS    = "bonus_points";
	private static final String FIRST_BONUS = "first_tier_bonus";
	private static final String SEEN     = "seen_slots";
	private static final String COUNT    = "slot_count";
	private static final String CHOSEN   = "slot_chosen_";
	private static final String OPTIONS  = "slot_options_";
	private static final String LEVEL    = "slot_level_";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(UNLOCKED, unlocked);
		bundle.put(BONUS, bonusPoints);
		bundle.put(FIRST_BONUS, firstTierBonus);
		bundle.put(SEEN, seenSlots);
		bundle.put(COUNT, slots.size());
		for (int i = 0; i < slots.size(); i++){
			Slot s = slots.get(i);
			if (s.chosen != null) bundle.put(CHOSEN + i, s.chosen.name());
			String[] opts = new String[s.options.length];
			for (int j = 0; j < opts.length; j++) opts[j] = s.options[j].name();
			bundle.put(OPTIONS + i, opts);
			bundle.put(LEVEL + i, s.level);
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		unlocked = bundle.getBoolean(UNLOCKED);
		bonusPoints = bundle.getInt(BONUS);
		firstTierBonus = bundle.getInt(FIRST_BONUS);
		seenSlots = bundle.getInt(SEEN);
		slots.clear();
		int count = bundle.getInt(COUNT);
		for (int i = 0; i < count; i++){
			Slot s = new Slot();
			if (bundle.contains(CHOSEN + i)){
				try {
					s.chosen = BloomTalent.valueOf(bundle.getString(CHOSEN + i));
				} catch (Exception e){
					s.chosen = null;
				}
			}
			ArrayList<BloomTalent> opts = new ArrayList<>();
			if (bundle.contains(OPTIONS + i)){
				for (String n : bundle.getStringArray(OPTIONS + i)){
					try {
						opts.add(BloomTalent.valueOf(n));
					} catch (Exception e){
						//없어진 특성은 건너뛴다
					}
				}
			}
			s.options = opts.toArray(new BloomTalent[0]);
			//저장값이 범위를 벗어나도 효과 배열을 넘지 않게
			s.level = Math.max(0, Math.min(MazeTower.BLOOM_MAX_TALENT_LEVEL, bundle.getInt(LEVEL + i)));
			slots.add(s);
		}
	}
}
