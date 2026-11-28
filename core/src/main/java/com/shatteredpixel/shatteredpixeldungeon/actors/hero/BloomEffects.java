/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;

import java.util.ArrayList;

//Maze Tower: 2차 특성 효과가 원작 코드와 만나는 자리를 한곳에 모은 클래스.
//원작 코드에는 짧은 호출 한 줄씩만 넣고(Hero, Char, SwordSkill, Mana, Burning 등), 실제 계산은 여기서 한다.
//
//(2026-11-24) 예전 2차 특성 29개를 지웠다. 지금 효과가 있는 것은 섬광 보법(BloomSkill)뿐이고,
//나머지 자리는 새 전사·사냥꾼 특성(구현 지시서 C·D 단계)이 붙을 곳으로 비워 두었다.
public class BloomEffects {

	//효과 배열 번호: 1레벨 → 0, 2레벨 → 1, 3레벨 → 2. 없거나 0레벨이면 -1 (효과 없음).
	static int lvl(Hero hero, BloomTalent t){
		return BloomTree.level(hero, t) - 1;
	}

	static boolean isSkill(){
		return SwordSkill.active != null; //(지팡이 스킬은 onStaffSkillDamage)
	}

	// =====================================================================
	// 스킬 한 번 사용 동안의 상태 (SwordSkill 등이 시작/끝에 호출)
	// =====================================================================

	public static class SkillUse {
		public Char target;
		public int manaSpent;
		public boolean wasFullMana;
	}

	public static SkillUse skillUse = null;

	//검·단검·활 스킬 비용
	//(B5) 형태 특성(1레벨 이상이고 지금 켜진 것)마다 B5_FORM_MANA_COST만큼 는다. 둘을 겹치면 둘 다 는다.
	public static int skillCost(Hero hero, int base){
		return Math.max(0, base + formCount(hero) * MazeTower.B5_FORM_MANA_COST);
	}

	//(B5) 지금 작동하는 형태 특성 수
	public static int formCount(Hero hero){
		if (hero == null) return 0;
		int n = 0;
		for (BloomTalent t : BloomTalent.values()){
			if (t.branch == BloomTalent.Branch.FORM && BloomTree.level(hero, t) >= 1) n++;
		}
		return n;
	}

	//지팡이 스킬 비용
	public static int wandSkillCost(Hero hero, int base){
		return Math.max(0, base);
	}

	//평타 한 번 시작 (Hero.onAttackComplete): 새 "한 행동"
	public static void beginNormal(Hero hero){
		BloomRules.beginNormal();
		BloomWarrior.beginNormal(hero); //(C1) 받아치기
		BloomWarrior.beginAttack(hero); //(C3) 벼림을 이 평타에 쓴다
		//Maze Tower (I7): 10층 간수의 "치지 마라" 선고 (평타는 그대로 나간다)
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.violate(hero, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.ATTACK);
	}

	//마나를 쓰기 직전에 호출
	public static SkillUse beginSkill(Hero hero, Char target, int cost){
		SkillUse u = new SkillUse();
		u.target = target;
		u.manaSpent = cost;
		u.wasFullMana = Mana.get(hero).isFull();
		BloomRules.beginSkill(cost); //스킬 한 번이 새 "한 행동"
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetSpell.onSkillStart(); //(T7) 길든 주문: 스킬 한 번에 적 하나당 한 번
		BloomSkill.onSkillStart(hero);
		BloomWarrior.beginAttack(hero); //(C3) 벼림을 이 스킬에 쓴다
		BloomWarrior.beginSkill(hero);  //(C13) 넘치는 잔 각성: 쌓인 피해를 이 스킬에 쓴다
		skillUse = u;
		return u;
	}

	//검·단검·활·반지 스킬이 끝났을 때 (SwordSkill.finish, endSkill 바로 앞)
	public static void onSwordSkillFinished(Hero hero, SwordSkill skill){
		BloomWarrior.onSwordSkillFinished(hero, skill, skillUse == null ? null : skillUse.target); //(C5) 메아리
	}

	//스킬 타격이 모두 끝난 뒤 호출
	public static void endSkill(Hero hero){
		flushPostHit(); //기절 등 타격 뒤 효과
		BladeDancer.endSkill(); //(S3) 끝맺는 칼
		SkillUse u = skillUse;
		skillUse = null;
		if (u == null) return;
		BloomSkill.endSkill(hero, u);
	}

	// =====================================================================
	// (B4) 스킬 타격이 끝났다: 모든 무기 스킬이 지나가는 한 곳
	// 검·단검·활 스킬은 SwordSkill.doHit에서, 타격 하나가 끝날 때마다 부른다.
	// 강화형(장전·집중)은 강화된 평타 한 번이 스킬 타격이다 (장전·집중 행동 자체는 여기를 지나지 않는다).
	// 스킬 형태 특성(메아리·한 번 더·무거운 일격·초승달·튀는 일격 등)은 모두 여기에 붙인다 (B5·C·D).
	// =====================================================================

	public static class SkillHit {
		public Hero hero;
		public Char target;
		public int fromCell;      //주인공이 선 칸 (방향 = fromCell → targetCell)
		public int targetCell;    //타격할 때 대상이 있던 칸 (밀리기 전)
		public boolean hit;       //맞았는지
		public int damage;        //이 타격의 피해 (방어 적용 뒤, 적중 처리에서 기록. 빗나가면 0)
		public boolean charged;   //강화형(장전·집중으로 강화된 평타)이면 true, 공격형이면 false
		public int baseHits;      //본 타격 수 (연격 3, 반월베기 2, 속사 2, 그 밖은 1)
		public int index;         //이번이 몇 번째 본 타격인지 (0부터)
		public String skillKey;   //스킬 이름 키 (slash, thrust, flurry, crescent, phantom, aimed, rapid, pierce, loaded, focused 등)
		public boolean ranged;    //화살을 쏘는 스킬인지
		public float dmgMulti;    //이 타격의 평타 피해 배율 (덧타격은 여기에 비율을 곱한다)
		public float accMulti;    //이 타격의 명중 배율

		//방향 (x, y 각각 -1·0·1)
		public int dirX(){ return Integer.signum(targetCell % Dungeon.level.width() - fromCell % Dungeon.level.width()); }
		public int dirY(){ return Integer.signum(targetCell / Dungeon.level.width() - fromCell / Dungeon.level.width()); }
	}

	//주인공 적중 처리(Hero.attackProc 끝)에서 스킬 타격의 피해를 기록해 둔다
	private static int lastSkillHitDamage = 0;

	//SwordSkill.doHit: 타격을 시작하기 직전에 부른다
	public static void beforeSkillHit(){
		lastSkillHitDamage = 0;
	}

	//Hero.attackProc 끝: 최종 피해가 정해진 적중 (평타·스킬·덧타격·던지기, 파생 피해는 오지 않는다)
	public static void afterHeroAttackProc(Hero hero, Char enemy, int damage){
		if (enemy == null || enemy == hero || BloomRules.isDerived()) return;
		BloomWarrior.bloodyEdge(hero, enemy, damage, isSkill()); //(C19) 피 묻은 날
		BloomHuntress.openWound(hero, enemy, damage, isSkill()); //(D3) 벌어진 상처
		BloomHuntress.markCleanCandidate(enemy);                 //(D6) 값진 일격: 다치지 않은 적인지
		BloomHuntress.pin(hero, enemy);                          //(D17) 못 박기
		BloomHuntress.markEmbedHit(hero, enemy);                 //(D18) 박힌 화살: 근접 평타가 맞았다
		BattleHunter.onArrowHit(hero, enemy);                     //(S7) 전투 사냥꾼 맞불
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetSpell.onHeroHit(hero, enemy); //(T7) 길든 주문: 원소 묻히기
	}

	//Hero.attackProc 끝: 방어·적중 효과가 모두 적용된 피해
	public static void recordSkillHitDamage(int damage){
		if (SwordSkill.active != null && !extraHit) lastSkillHitDamage = damage;
	}

	// ===== (B5) 덧타격: 형태 특성이 덧붙인 타격 (잔상, 좌우 칸, 튄 빛줄기 등) =====
	// - 본 타격보다 약하다: 본 타격의 배율 × ratio (ratio는 B5_EXTRA_HIT_MAX를 넘지 않는다)
	// - 치명타(급소 등)가 나지 않는다: inExtraHit()으로 막는다
	// - 다른 형태를 부르지 않는다: 덧타격은 SwordSkill.doHit을 지나지 않으므로 onSkillHitDone이 불리지 않는다
	// - 화살을 쓰지 않는다: 활이면 화살 없이 원거리 피해로만 친다
	// - 적중 효과(원소, 마법 부여, 상처, 밀침)는 원작 공격 처리(hero.attack)를 그대로 지나므로 일어난다
	// 스킬이 진행 중일 때(onSkillHitDone 안에서) 부른다. 그 밖의 때(메아리 잔상 등)는 그 특성이 스킬 판정을 켜고 부른다.

	private static boolean extraHit = false;

	public static boolean inExtraHit(){
		return extraHit;
	}

	//맞았으면 true
	public static boolean extraHit(SkillHit from, Char target, float ratio){
		Hero hero = from.hero;
		if (extraHit || hero == null || target == null || target == hero || !target.isAlive()
				|| target.alignment != Char.Alignment.ENEMY) return false;
		ratio = Math.max(0f, Math.min(ratio, MazeTower.B5_EXTRA_HIT_MAX));
		extraHit = true;
		boolean free = from.ranged && WornShortbow.beginFreeShot(hero);
		boolean hit = false;
		try {
			hit = hero.attack(target, from.dmgMulti * ratio, 0, from.accMulti);
		} finally {
			if (free) WornShortbow.endShot(hero, target, false, false, false);
			extraHit = false;
		}
		BloomRules.debug("extra-hit " + from.skillKey + " -> " + target.name() + " x" + ratio + " hit=" + hit);
		return hit;
	}

	public static int lastSkillHitDamage(){
		return lastSkillHitDamage;
	}

	//스킬 모양 바꾸기 (SwordSkill.launch, beginSkill 바로 뒤): (C6) 한 번 더
	public static SwordSkill reshapeSkill(Hero hero, SwordSkill base){
		return BloomWarrior.oneMore(hero, base);
	}

	//스킬 타격 하나가 끝났을 때
	public static void onSkillHitDone(SkillHit h){
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MirrorPortrait.onHeroSkillHit(h); //Maze Tower (L6): 거울 초상이 따라 쓸 피해를 모은다
		BloomRules.debug("skill-hit " + h.skillKey + " #" + (h.index + 1) + "/" + h.baseHits
				+ (h.charged ? " (charged)" : " (attack)")
				+ " target=" + (h.target == null ? "-" : h.target.name())
				+ " dir=(" + h.dirX() + "," + h.dirY() + ")"
				+ " hit=" + h.hit + " dmg=" + h.damage);
		//형태 특성은 여기에 붙인다 (B5·C·D)
		BloomWarrior.onSkillHitDone(h);  //전사 형태 (달군 날·초승달·무거운 일격)
		BloomHuntress.onSkillHitDone(h); //사냥꾼 형태 (튀는 일격·새잎 자국)
	}

	// ===== 타격 뒤에 거는 효과 (피해가 먼저, 상태이상은 그 다음: 빙결 반응 순서) =====

	private static final ArrayList<Runnable> postHit = new ArrayList<>();

	static void afterHit(Runnable r){
		postHit.add(r);
	}

	static void flushPostHit(){
		if (postHit.isEmpty()) return;
		ArrayList<Runnable> run = new ArrayList<>(postHit);
		postHit.clear();
		for (Runnable r : run) r.run();
	}

	// =====================================================================
	// 명중 (Hero.attackSkill)
	// =====================================================================

	public static float accuracyMulti(Hero hero, Char target){
		return BattleHunter.accuracyMulti(hero, target); //(S7) 전투 사냥꾼 코앞 사격 II
	}

	//주인공 공격의 방어력 무시 (Char.attack)
	public static int armorAfterIgnore(Hero hero, Char enemy, int dr){
		return BloomWarrior.armorAfterIgnore(hero, enemy, dr); //(C21) 발도 각성: 방어력 관통
	}

	// =====================================================================
	// 공격 적중 (Hero.attackProc, 방어 적용 후 피해)
	// =====================================================================

	public static int onHeroAttackProc(Hero hero, Char enemy, int damage){
		return processHit(hero, enemy, damage, isSkill());
	}

	// =====================================================================
	// 지팡이 스킬 (마법사): 합성된 마법봉 발사도 스킬로 친다
	// =====================================================================

	//지금 스킬로 쏘고 있는 지팡이의 마법봉 (쏘는 동안만)
	public static Object staffSkillWand = null;

	//쏘는 순간 (WandMana.onZap): 마나를 쓰기 전에 호출
	public static void beginStaffSkill(Hero hero, Object wand, Char target, int cost){
		beginSkill(hero, target, cost);
		staffSkillWand = wand;
	}

	//쏘기가 끝났을 때 (Wand.wandUsed)
	public static void endStaffSkill(Hero hero, Object wand){
		if (wand == null || staffSkillWand != wand) return;
		staffSkillWand = null;
		endSkill(hero);
	}

	//지팡이 마법봉이 준 피해 (Char.damage): 스킬 특성의 피해 배율과 적중 시 효과
	public static int onStaffSkillDamage(Char target, int dmg, Object src){
		if (staffSkillWand == null || src != staffSkillWand || dmg <= 0) return dmg;
		Hero hero = Dungeon.hero;
		if (hero == null || target == hero || target.alignment == Char.Alignment.ALLY) return dmg;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetSpell.onHeroHit(hero, target); //(T7) 길든 주문: 지팡이 스킬도
		return processHit(hero, target, dmg, true);
	}

	//적이 피해를 받을 때 (Char.damage)
	public static int incomingDamage(Char target, int dmg, Object src){
		return DuelMaster.dealt(target, dmg, src); //(S4) 결투가: 결투 상대 +, 다른 적 −
	}

	//평타·스킬 적중 한 번 처리
	private static int processHit(Hero hero, Char enemy, int damage, boolean skill){
		if (enemy == null || enemy == hero) return damage;
		if (BloomRules.isDerived()) return damage; //규칙 1: 파생 피해는 평타도 스킬도 아니다
		damage = BloomWarrior.hitDamage(hero, enemy, damage, skill); //(C1) 받아치기, (C2) 허점 찌르기
		damage = BloomHuntress.hitDamage(hero, enemy, damage, skill); //(D1) 첫 일격, (D2) 약점
		if (skill) damage = BladeDancer.skillDamage(damage); //(S3) 검무가 끝맺는 칼
		damage = BattleHunter.hitDamage(hero, enemy, damage); //(S7) 전투 사냥꾼 코앞 사격
		damage = ForestWarden.hitDamage(hero, enemy, damage); //(S9) 숲지기 풀잎 화살
		if (!skill) return damage;
		if (extraHit) return damage; //(B5) 덧타격에는 본 스킬의 피해 증가(섬광 보법)를 다시 붙이지 않는다
		float bonus = 1f + BloomSkill.hitBonus(hero, enemy); //섬광 보법
		int result = Math.round(damage * bonus);
		BloomSkill.onSkillHit(hero, enemy); //섬광 보법 각성
		return result;
	}

	// =====================================================================
	// 평타가 끝난 뒤 (Hero.onAttackComplete)
	// =====================================================================

	//추가 평타로 이어서 처리하면 true (그때는 끝난 뒤 done을 부른다). false면 부른 쪽이 바로 done을 처리한다.
	public static boolean afterNormalAttack(final Hero hero, final Char enemy, boolean hit, final Runnable done){
		BloomWarrior.friction(hero, hit);            //(C14) 마찰: 평타가 맞으면 마법봉 충전
		BloomWarrior.longEdge(hero, enemy, hit);     //(C18) 긴 칼날: 뒤 칸의 적도 벤다
		BloomWarrior.afterNormalAttack(hero, enemy); //(C5) 메아리 각성: 잔상이 평타를 따라 친다
		BloomHuntress.afterNormalAttack(hero, enemy); //(D18) 박힌 화살: 근접 평타로 터뜨림
		flushPostHit();
		return false;
	}

	// =====================================================================
	// 처치 (BloomRules.onDeath에서, 주인공이 처치했을 때만)
	// =====================================================================

	//byDerived: 파생 피해로 죽었는지
	public static void onEnemyKilled(Hero hero, Char enemy, boolean byDerived){
		if (enemy instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SapDroplet) return; //(G2) 수액 방울은 처치 보상 특성을 부르지 않는다
		if (enemy instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ChainRing) return; //(H6) 사슬 고리도
		BloomRules.debug("kill " + enemy.name() + (byDerived ? " (derived)" : ""));
		BloomHuntress.onEnemyKilled(hero, enemy, byDerived); //(D6) 값진 일격
		BloomWarrior.onEnemyKilled(hero, enemy); //(C2) 허점 찌르기 각성
	}

	// =====================================================================
	// 계속 켜져 있는 연출 (상태창이 매 프레임 호출)
	// =====================================================================

	public static void updateHeroFx(Hero hero){
		BloomWarrior.updateOneOnOne(hero); //(C11) 일대일 상태 아이콘
	}

	// =====================================================================
	// 받는 피해 (Hero.defenseProc / Hero.damage)
	// =====================================================================

	public static int onHeroDefense(Hero hero, Char enemy, int damage){
		return damage;
	}

	//(B5) 적의 공격으로 피가 줄지 않았다 (회피·방어막이 전부 막음·쳐냄, BloomJudge.afterEnemyAttack)
	public static void onUnhurtByAttack(Hero hero, Char enemy){
		BloomWarrior.onUnhurtByAttack(hero, enemy); //(C1) 받아치기
	}

	//주인공이 턴을 쓸 때 (Hero.spend)
	//돌려주는 값 = 실제로 쓸 시간 (치고 빠지기 각성이면 0)
	public static float onHeroSpend(Hero hero, float time){
		boolean attacked = BloomWarrior.attackingNow();
		boolean moved = BloomHuntress.movedNow();
		BloomWarrior.onHeroSpend(hero, time); //(C1) 받아칠 준비가 사라짐, (C3) 벼림이 쌓임
		float t = BloomHuntress.onHeroSpend(hero, time, attacked, moved); //(D4) 치고 빠지기
		ForestWarden.onHeroSpend(hero); //(S8) 숲지기: 풀 속 기습을 넣은 행동이 끝나면 선 칸의 풀이 눕는다
		return BladeDancer.onHeroSpend(hero, time, moved, t); //(S2) 검무가: 흐름, 걸어 도착하면 스침, (S3) 이어지는 걸음
	}

	//대기·휴식·탐색으로 턴을 쓸 때 (Mana.onWait, 시간을 쓰기 직전)
	public static void onHeroWait(Hero hero, float time){
		onHeroSpend(hero, time);              //턴을 쓴 것 (벼림·숨 고르기·받아치기 해제, 치고 빠지기 해제)
		BloomWarrior.armQuickdraw(hero);      //(C21) 발도: 대기하면 검을 겨눈다
		BloomHuntress.onHeroWait(hero);       //(D10) 풀숲의 몸 각성: 풀 속에서 가만히 있으면 적이 나를 놓친다
	}

	//주인공의 회피 배율 (Hero.defenseSkill)
	public static float heroEvasionMulti(Hero hero, Char enemy){
		return BloomHuntress.evasionMulti(hero, enemy) //(D10) 풀숲의 몸
				* BladeDancer.evasionMulti(hero); //(S3) 검무가 춤추는 발
	}

	//잠든 적이 깨어날 확률 (Mob.Sleeping)
	public static float wakeChance(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob mob, float chance){
		return BloomHuntress.wakeChance(mob, chance); //(D1) 첫 일격 각성
	}

	//적이 칸에 들어섰다 (Level.occupyCell)
	public static void onMobEnterCell(Char ch){
		BloomHuntress.onMobEnterCell(Dungeon.hero, ch); //(D15) 덫 놓기 (덫이 먼저)
		BloomWarrior.onMobEnterCell(Dungeon.hero, ch); //(C21) 발도
		ForestWarden.onMobEnterCell(ch); //(S9) 숲지기 뿌리 걸음
	}

	//적의 공격(Char.attack)이 시작되기 직전. true면 쳐내서 공격이 빗나간다
	public static boolean tryDeflect(Hero hero, Char enemy){
		return com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetGuard.tryParry(hero, enemy) //(T6) 펫 마지막 근위의 흘리기
				|| BloomHuntress.tautDodge(hero, enemy)  //(D14) 팽팽한 숨 각성: 가득 찬 동안 첫 공격을 완전히 피함
				|| BloomWarrior.tryDeflect(hero, enemy); //(C20) 받아넘기기
	}

	//주인공의 이번 공격이 반드시 맞는지 (Char.hit)
	public static boolean heroSureHit(Hero hero, Char defender){
		return BloomWarrior.sureHit(hero)           //(C3) 가득 벼린 공격 (각성)
				|| BloomHuntress.farSureHit(hero, defender) //(D5) 먼 과녁 각성: 최대 사거리의 기습
				|| BloomHuntress.strideSureHit(hero);       //(D16) 정령의 춤 각성: 다섯 걸음 뒤 첫 공격
	}

	//평타의 턴 소모 (Hero.finishAttackComplete)
	public static float normalAttackDelay(Hero hero, float delay){
		return BloomWarrior.normalAttackDelay(hero, delay); //(C1) 받아치기 각성
	}

	//주인공이 칸에 들어섰다 (Level.occupyCell, 함정·풀 처리 전)
	public static void onHeroEnterCell(Hero hero, int cell){
		BloomWarrior.onHeroEnterCell(hero, cell); //(C15) 가벼운 발
		BloomHuntress.onHeroEnterCell(hero, cell); //(D12) 약초 캐기 (풀을 밟기 전)
	}

	//주인공이 한 걸음 걸었다 (Hero.getCloser, 턴을 쓰기 직전)
	public static void onHeroWalked(){
		BloomHuntress.onHeroMoved(); //(D4) 치고 빠지기
		//Maze Tower (I7): 10층 간수의 "걷지 마라" 선고
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.violate(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.MOVE);
	}

	//주인공이 함정을 밟았다 (Level.pressCell). true면 함정이 발동하지 않는다
	public static boolean blockTrap(Hero hero, int cell){
		return BloomWarrior.blockTrap(hero, cell); //(C15) 가벼운 발 각성
	}

	//주인공에게 나쁜 상태이상이 붙기 직전 (Buff.attachTo). true면 붙지 않는다
	public static boolean blockHeroDebuff(Hero hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff){
		return BloomWarrior.blockHeroDebuff(hero, buff); //(C9) 숨 고르기 각성
	}

	//적의 공격(Char.attack) 한 번이 끝났다. hit = 맞았는지
	public static void onEnemyAttackDone(Hero hero, Char enemy, boolean hit){
		BloomWarrior.onEnemyAttackDone(hero, enemy, hit); //(C11) 일대일 각성 반격
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetGuard.afterEnemyAttack(enemy); //(T6) 마지막 근위가 흘린 뒤 반격
	}

	//방어막이 피해를 막은 뒤 (Char.damage). 남은(넘친) 피해를 돌려준다
	public static int afterShield(Hero hero, int shieldBefore, int shieldAfter, int dmg){
		return BloomWarrior.afterShield(hero, shieldBefore, shieldAfter, dmg); //(C10) 마지막 겹
	}

	//받는 모든 피해
	public static int onHeroDamage(Hero hero, int dmg, Object src){
		if (dmg > 0) BloomWarrior.onHeroHurt(hero); //(C9) 숨 고르기: 맞은 턴에는 방어막이 차지 않는다
		dmg = BloomWarrior.reduceIncoming(hero, dmg, src); //(C10) 마지막 겹 깨진 뒤, (C11) 일대일, (C15) 가벼운 발
		dmg = BloomHuntress.breakfall(hero, dmg, src);     //(D11) 낙법
		dmg = DuelMaster.taken(hero, dmg, src);           //(S5) 결투가 맞선 눈: 결투 상대에게 받는 피해 −
		dmg = ForestWarden.taken(hero, dmg, src);         //(S9) 숲지기 숲의 품: 풀 속에서 받는 근접 피해 −
		return dmg;
	}

	//내가 받는 상태이상 저항·지속
	public static float heroResistFactor(Hero hero, Class<?> effect){
		return 1f;
	}

	public static float heroIncomingDuration(Hero hero, Class<?> effect){
		return 1f;
	}

	// =====================================================================
	// 마나 (Mana)
	// =====================================================================

	//마나가 가득 찬 뒤 넘친 양 (Mana.gain)
	public static void onManaOverflow(Hero hero, int overflow){
		BloomWarrior.onManaOverflow(hero, overflow); //(C13) 넘치는 잔
	}

	public static int bonusMaxMana(Hero hero){
		return 0;
	}

	public static int waitManaMulti(Hero hero){
		return 1;
	}

	// =====================================================================
	// 원소 (상태이상 걸기, 반응)
	// =====================================================================

	public static void ignite(Hero hero, Char enemy, float turns){
		if (!enemy.isAlive()) return;
		Buff.affect(enemy, Burning.class).reignite(enemy, turns);
		if (enemy.sprite != null) enemy.sprite.emitter().burst(FlameParticle.FACTORY, 6);
	}

	public static void chill(Hero hero, Char enemy, float turns){
		if (!enemy.isAlive()) return;
		Buff.prolong(enemy, Chill.class, turns);
		if (enemy.sprite != null) enemy.sprite.emitter().burst(SnowParticle.FACTORY, 6);
	}

	//원소 반응 피해 배율
	public static float reactionDamageMulti(Hero hero){
		return 1f;
	}

	//원소 반응이 일어났을 때 (ElementalReactions.announce)
	public static void onReaction(Char ch){
	}

	//피해를 주는 반응(증기·해빙·폭발)이 피해를 준 뒤. trigger = 반응을 일으킨 원소 (ElementalReactions.TRIGGER_*)
	public static void onReactionDamage(Char ch, int dmg, int trigger){
		BloomRules.debug("reaction dmg " + dmg + " on " + (ch == null ? "-" : ch.name()) + " (my attack: " + heroAttackInProgress() + ")");
		BloomWarrior.onReactionDamage(Dungeon.hero, ch, dmg, trigger); //(C4) 번지는 반응
	}

	// =====================================================================
	// (C4) 주인공의 공격이 진행 중인지: "내 공격이 일으킨 반응"을 가린다
	// 주인공의 Char.attack(평타·스킬·덧타격·던지기), 검·활·반지 스킬, 지팡이 스킬, 마법봉 발사가 처리되는 동안
	// =====================================================================

	private static int heroAttackDepth = 0;

	public static void beginHeroAttack(){ heroAttackDepth++; }
	public static void endHeroAttack(){ if (heroAttackDepth > 0) heroAttackDepth--; }

	public static boolean heroAttackInProgress(){
		return heroAttackDepth > 0 || SwordSkill.active != null || staffSkillWand != null;
	}
}
