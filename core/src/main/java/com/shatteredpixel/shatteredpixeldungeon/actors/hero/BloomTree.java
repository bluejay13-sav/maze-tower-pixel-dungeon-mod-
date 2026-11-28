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

//Maze Tower: 2차 전직 상태. 영웅에게 붙는 보이지 않는 버프로 저장·불러오기를 원작 방식 그대로 처리한다.
//
// - 개화의 핵을 쓰면 unlocked = true, 선택권 1개 + 포인트 BLOOM_CORE_POINTS.
// - Lv7~10 레벨업마다 선택권 1 (총 5칸), Lv7~12 레벨업마다 포인트 1. (핵을 쓰기 전에 오른 레벨도 핵을 쓰는 순간 한꺼번에 들어온다)
// - 선택권이 생기는 순간 그 칸의 선택지 3개를 던전 시드로 정해 저장한다.
//   이미 가진 특성과, 아직 고르지 않은 다른 칸의 선택지는 제외하고, 3개는 갈래가 서로 다르다. 영웅 직업의 풀(BloomTalent.poolOf)에서만 뽑는다.
// - 고른 특성은 0레벨(효과 없음)로 시작하고 포인트로 1~3레벨이 된다.
// - 만개의 핵을 쓰면 분홍 꽃잎 BLOOM_PETALS개. 꽃잎 1개로 1레벨 이상인 특성 하나를 각성한다 (특성마다 1번, 되돌릴 수 없음).
public class BloomTree extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	public static class Slot {
		public BloomTalent chosen = null;               //고른 특성 (null이면 아직 + 칸)
		public BloomTalent[] options = new BloomTalent[0]; //이 칸의 선택지
		public int level = 0;                           //고른 특성의 레벨 (0~3, 0 = 효과 없음)
		public boolean awakened = false;                //만개 각성 (분홍 꽃잎을 썼는지, 되돌릴 수 없음)

		public boolean isChosen(){
			return chosen != null;
		}
	}

	public boolean unlocked = false;
	public final ArrayList<Slot> slots = new ArrayList<>();

	//신성한 영감의 물약으로 얻은 2차 추가 포인트
	public int bonusPoints = 0;
	//신성한 영감의 물약으로 얻은 1차 추가 포인트
	public int firstTierBonus = 0;
	//신성한 영감의 물약으로 얻은 3차(만개) 추가 포인트
	public int thirdTierBonus = 0;
	//신성한 영감의 물약으로 얻은 4차(결실) 추가 포인트
	public int fourthTierBonus = 0;

	//만개의 핵으로 받은 분홍 꽃잎 수 (쓴 꽃잎 = 각성한 칸 수)
	public int petalsGranted = 0;

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

	//효과가 쓰는 특성 레벨 (0~3). 없으면 -1. 0레벨은 효과 없음.
	//(B2) 전용 특성은 그 계열 무기를 들고 있지 않으면 -1 (꺼짐). 모든 특성 효과는 이것(또는 awakened)만 본다.
	public static int level( Hero hero, BloomTalent talent ){
		Slot s = slotOf(hero, talent, true);
		return s == null ? -1 : s.level;
	}

	public static boolean has( Hero hero, BloomTalent talent ){
		return level(hero, talent) >= 0;
	}

	//각성했는지 (레벨과 상관없음). (B2) 전용 특성은 무기가 맞지 않으면 각성도 꺼진다
	public static boolean awakened( Hero hero, BloomTalent talent ){
		Slot s = slotOf(hero, talent, true);
		return s != null && s.awakened;
	}

	//(B2) 무기와 상관없이 가진 레벨 (특성 창·치트 표시용). 없으면 -1
	public static int ownedLevel( Hero hero, BloomTalent talent ){
		Slot s = slotOf(hero, talent, false);
		return s == null ? -1 : s.level;
	}

	public static boolean ownedAwakened( Hero hero, BloomTalent talent ){
		Slot s = slotOf(hero, talent, false);
		return s != null && s.awakened;
	}

	private static Slot slotOf( Hero hero, BloomTalent talent, boolean mustBeActive ){
		if (hero == null) return null;
		BloomTree t = hero.buff(BloomTree.class);
		if (t == null) return null;
		//(B1) 영웅의 풀에 없는 특성은 (창을 열어 칸이 비워지기 전이라도) 가지지 않은 것으로 본다
		if (!talent.inPool(BloomTalent.poolOf(hero))) return null;
		if (mustBeActive && !talent.activeFor(hero)) return null;
		for (Slot s : t.slots){
			if (s.chosen == talent) return s;
		}
		return null;
	}

	// ===== 선택권·포인트 계산 =====

	private static int levelsIn( Hero hero, int first, int last ){
		int reached = hero.lvl - first + 1;
		return Math.max(0, Math.min(last - first + 1, reached));
	}

	//선택권을 주는 레벨(Lv7~10) 중 도달한 수
	public static int choiceLevelsReached( Hero hero ){
		return levelsIn(hero, MazeTower.BLOOM_FIRST_LEVEL, MazeTower.BLOOM_CHOICE_LAST_LEVEL);
	}

	//포인트를 주는 레벨(Lv7~12) 중 도달한 수
	public static int pointLevelsReached( Hero hero ){
		return levelsIn(hero, MazeTower.BLOOM_FIRST_LEVEL, MazeTower.BLOOM_LAST_LEVEL);
	}

	//1차 포인트 (Lv2~6)
	public static int firstTierPoints( Hero hero ){
		return levelsIn(hero, MazeTower.FIRST_TIER_FIRST_LEVEL, MazeTower.FIRST_TIER_LAST_LEVEL);
	}

	//핵을 쓰면 받게 될(또는 받은) 선택권 수
	public static int choicesFor( Hero hero ){
		return Math.min(MazeTower.BLOOM_SLOTS, 1 + choiceLevelsReached(hero));
	}

	//핵을 쓰면 받게 될(또는 받은) 레벨 포인트 (물약 제외)
	public static int levelPointsFor( Hero hero ){
		return MazeTower.BLOOM_CORE_POINTS + pointLevelsReached(hero);
	}

	//물약을 뺀 2차 포인트 최대치 (특성 창의 별 개수)
	public static int regularPoints(){
		return MazeTower.BLOOM_CORE_POINTS + MazeTower.BLOOM_LAST_LEVEL - MazeTower.BLOOM_FIRST_LEVEL + 1;
	}

	public int pointsEarned( Hero hero ){
		if (!unlocked) return 0;
		return levelPointsFor(hero) + bonusPoints;
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

	// ===== 만개 각성 (분홍 꽃잎) =====

	public int petalsUsed(){
		int n = 0;
		for (Slot s : slots){
			if (s.isChosen() && s.awakened) n++;
		}
		return n;
	}

	public int petalsAvailable(){
		return Math.max(0, petalsGranted - petalsUsed());
	}

	//각성할 수 있는 특성이 있고 꽃잎이 남았는지
	public boolean canAwakenSomething(){
		if (petalsAvailable() <= 0) return false;
		for (Slot s : slots){
			if (s.isChosen() && !s.awakened && s.level >= MazeTower.BLOOM_AWAKEN_MIN_LEVEL) return true;
		}
		return false;
	}

	public boolean awaken( int slotIndex ){
		Slot s = slots.get(slotIndex);
		if (!s.isChosen() || s.awakened || petalsAvailable() <= 0) return false;
		if (s.level < MazeTower.BLOOM_AWAKEN_MIN_LEVEL) return false; //(결정) 1레벨 이상 찍은 특성만 각성할 수 있다
		s.awakened = true;
		return true;
	}

	//고를 칸, 쓸 포인트, 쓸 꽃잎이 남았는지 (상태창 알림용)
	public boolean hasPending( Hero hero ){
		return unlocked && (unchosenSlots() > 0 || canUpgradeSomething(hero) || canAwakenSomething());
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
		BloomTalent.Pool pool = BloomTalent.poolOf(hero);
		//2차 특성이 아직 없는 직업(마법사·도적)은 칸을 만들지 않는다 (예전 저장의 칸도 비운다)
		if (pool == null){
			slots.clear();
			return;
		}
		//(B1) 영웅의 풀에 없는 특성(예: 새 풀로 바뀌기 전의 저장 파일)을 고른 칸은 비우고 다시 받는다.
		//쓴 포인트와 꽃잎은 그 칸이 비면서 돌아온다.
		for (Slot s : slots){
			if (s.isChosen() && !s.chosen.inPool(pool)){
				s.chosen = null;
				s.level = 0;
				s.awakened = false;
				s.options = new BloomTalent[0];
			}
			if (!s.isChosen()){
				for (BloomTalent t : s.options){
					if (!t.inPool(pool)){
						s.options = new BloomTalent[0];
						break;
					}
				}
			}
		}
		noteExistingForms(); //(B5) 이미 칸에 있는 형태 (예전 저장 파일)
		int target = choicesFor(hero);
		//저장 데이터에서 선택지가 사라진(특성 이름이 바뀐 등) 미선택 칸은 다시 정한다
		for (int i = 0; i < slots.size(); i++){
			Slot s = slots.get(i);
			if (!s.isChosen() && s.options.length == 0){
				s.options = rollOptions(pool, i);
			}
		}
		while (slots.size() < target){
			Slot s = new Slot();
			s.options = rollOptions(pool, slots.size());
			slots.add(s);
		}
	}

	//선택지 3개: 남은 특성 중 하나를 고르고, 그 갈래를 빼고 다음을 고른다.
	//그 밖의 가중치는 없다. 남은 갈래가 모자라면 같은 갈래도 허용한다.
	//영웅 직업의 풀에 있는 특성만 뽑는다. (결정, 2026-11-24) 전용 특성(검 전용·활 전용)도 들고 있는 무기와 상관없이
	//다른 특성과 똑같이 나온다 (무기를 바꿀지 고르는 재미). 효과는 그 무기를 들었을 때만 작동한다(B2).
	private BloomTalent[] rollOptions( BloomTalent.Pool which, int slotIndex ){
		return fillOptions(which, new ArrayList<BloomTalent>(), Dungeon.seed + 0x4D41_5A45L + 7919L * (slotIndex + 1));
	}

	//(결정, B5) 한 판(던전 시드)에서 선택지에 나오는 형태 특성은 모두 합쳐 B5_FORM_MAX(2)종까지다.
	//처음 나온 두 형태만 기록해 두고, 그 뒤에는 이 둘만 다시 나올 수 있다(고르지 않고 남긴 것이 다른 칸에 다시 나오는 것은 괜찮다).
	//그래서 형태는 둘보다 많이 고를 수 없고, 이미 정해진 선택지를 바꿀 일도 없다.
	public final ArrayList<BloomTalent> offeredForms = new ArrayList<>();

	private boolean formAllowed( BloomTalent t ){
		return t.branch != BloomTalent.Branch.FORM
				|| offeredForms.contains(t)
				|| offeredForms.size() < MazeTower.B5_FORM_MAX;
	}

	private void noteForm( BloomTalent t ){
		if (t != null && t.branch == BloomTalent.Branch.FORM && !offeredForms.contains(t)) offeredForms.add(t);
	}

	//예전 저장 파일: 이미 칸에 있는 형태를 기록한다 (둘보다 많아도 그대로 둔다)
	private void noteExistingForms(){
		for (Slot s : slots){
			noteForm(s.chosen);
			for (BloomTalent t : s.options) noteForm(t);
		}
	}

	//picked에 이미 든 것은 두고, 모자란 만큼 남은 특성에서 채운다 (seed: 던전 시드에서 만든 값)
	private BloomTalent[] fillOptions( BloomTalent.Pool which, ArrayList<BloomTalent> picked, long seed ){
		ArrayList<BloomTalent> pool = new ArrayList<>();
		for (BloomTalent t : BloomTalent.values()){
			if (t.inPool(which)) pool.add(t);
		}
		for (Slot s : slots){
			if (s.isChosen()) {
				pool.remove(s.chosen);
			} else {
				for (BloomTalent t : s.options) pool.remove(t);
			}
		}
		pool.removeAll(picked);

		//던전 시드 + 칸 번호로 정한다. 같은 시드, 같은 선택이면 같은 선택지.
		Random.pushGenerator(seed);
		try {
			while (picked.size() < MazeTower.BLOOM_CHOICES_PER_SLOT){
				//(B5) 형태 두 종이 이미 나왔으면 다른 형태는 뺀다 (한 칸 안에서 형태를 뽑을 때마다 다시 본다)
				ArrayList<BloomTalent> allowed = new ArrayList<>();
				for (BloomTalent t : pool) if (formAllowed(t)) allowed.add(t);
				if (allowed.isEmpty()) break;
				ArrayList<BloomTalent> fresh = new ArrayList<>();
				for (BloomTalent t : allowed){
					boolean same = false;
					for (BloomTalent p : picked) if (p.pickGroup() == t.pickGroup()) same = true;
					if (!same) fresh.add(t);
				}
				ArrayList<BloomTalent> from = fresh.isEmpty() ? allowed : fresh;
				BloomTalent t = from.get(Random.Int(from.size()));
				pool.remove(t);
				picked.add(t);
				noteForm(t);
			}
		} finally {
			Random.popGenerator();
		}

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

	//테스트용 치트: 특성을 원하는 레벨로 바로 얻는다 (포인트와 무관). 칸이 모두 찼으면 false.
	public boolean cheatGrant( Hero hero, BloomTalent talent, int level, boolean awaken ){
		unlocked = true;
		Slot target = null;
		for (Slot s : slots){
			if (s.chosen == talent) target = s;
		}
		if (target == null){
			for (Slot s : slots){
				if (!s.isChosen()){
					target = s;
					break;
				}
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
		if (awaken && !target.awakened){
			target.awakened = true;
			//치트 각성은 꽃잎을 따로 준다 (남은 꽃잎은 그대로)
			petalsGranted++;
		}
		noteForm(talent); //(B5) 치트로 얻은 형태도 "나온 형태"로 센다
		return true;
	}

	// ===== 저장 =====

	//2차 특성 개편(31개, 5칸, 0~3레벨) 저장 형식. 이 값이 없는 예전 저장은 2차 칸을 비우고 다시 받는다.
	private static final int TREE_VERSION = 2;

	private static final String VERSION  = "tree_version";
	private static final String UNLOCKED = "unlocked";
	private static final String BONUS    = "bonus_points";
	private static final String FIRST_BONUS = "first_tier_bonus";
	private static final String THIRD_BONUS = "third_tier_bonus";
	private static final String FOURTH_BONUS = "fourth_tier_bonus";
	private static final String PETALS   = "petals_granted";
	private static final String SEEN     = "seen_slots";
	private static final String COUNT    = "slot_count";
	private static final String CHOSEN   = "slot_chosen_";
	private static final String OPTIONS  = "slot_options_";
	private static final String LEVEL    = "slot_level_";
	private static final String AWAKENED = "slot_awakened_";
	private static final String OFFERED_FORMS = "offered_forms";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(VERSION, TREE_VERSION);
		bundle.put(UNLOCKED, unlocked);
		bundle.put(BONUS, bonusPoints);
		bundle.put(FIRST_BONUS, firstTierBonus);
		bundle.put(THIRD_BONUS, thirdTierBonus);
		bundle.put(FOURTH_BONUS, fourthTierBonus);
		bundle.put(PETALS, petalsGranted);
		bundle.put(SEEN, seenSlots);
		bundle.put(COUNT, slots.size());
		String[] forms = new String[offeredForms.size()];
		for (int i = 0; i < forms.length; i++) forms[i] = offeredForms.get(i).name();
		bundle.put(OFFERED_FORMS, forms);
		for (int i = 0; i < slots.size(); i++){
			Slot s = slots.get(i);
			if (s.chosen != null) bundle.put(CHOSEN + i, s.chosen.name());
			String[] opts = new String[s.options.length];
			for (int j = 0; j < opts.length; j++) opts[j] = s.options[j].name();
			bundle.put(OPTIONS + i, opts);
			bundle.put(LEVEL + i, s.level);
			bundle.put(AWAKENED + i, s.awakened);
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		unlocked = bundle.getBoolean(UNLOCKED);
		bonusPoints = bundle.getInt(BONUS);
		firstTierBonus = bundle.getInt(FIRST_BONUS);
		thirdTierBonus = bundle.getInt(THIRD_BONUS);
		fourthTierBonus = bundle.getInt(FOURTH_BONUS);
		petalsGranted = bundle.getInt(PETALS);
		slots.clear();
		offeredForms.clear();
		if (bundle.contains(OFFERED_FORMS)){
			for (String n : bundle.getStringArray(OFFERED_FORMS)){
				try {
					offeredForms.add(BloomTalent.valueOf(n));
				} catch (Exception e){
					//없어진 특성은 건너뛴다
				}
			}
		}

		//예전 2차 특성(24개, 6칸) 저장: 칸을 비우고 새 규칙으로 다시 받는다 (물약 포인트는 그대로)
		if (bundle.getInt(VERSION) < TREE_VERSION){
			seenSlots = 0;
			return;
		}

		seenSlots = bundle.getInt(SEEN);
		int count = Math.min(bundle.getInt(COUNT), MazeTower.BLOOM_SLOTS);
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
			s.awakened = s.chosen != null && bundle.getBoolean(AWAKENED + i);
			slots.add(s);
		}
	}
}
