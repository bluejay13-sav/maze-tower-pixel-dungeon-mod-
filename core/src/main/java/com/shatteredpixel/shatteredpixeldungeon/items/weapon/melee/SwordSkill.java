/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomEffects;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.GladiatorEffects;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.SproutEffects;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.SkillEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.Callback;

//Maze Tower: 검 계열 고유 스킬.
//원작 결투가의 무기 능력 흐름(MeleeWeapon.AC_ABILITY → 대상 선택 → 공격)을 그대로 타되,
//충전(Charger) 대신 마나를 소모한다. 수치는 MazeTower에 모여 있다.
public class SwordSkill {

	public final String nameKey;     //스킬 이름 문자열 키
	public final int hits;           //타격 횟수
	public final float dmgPerHit;    //타격당 평타 피해 배율
	public final float accMulti;     //명중 배율 (Char.INFINITE_ACCURACY면 반드시 명중)
	public final boolean ignoreDefense;
	public final float elementChanceMulti;
	//기습 상태의 적에게 쓰면 이 배율로 바뀐다 (0 = 해당 없음, 급소 찌르기)
	public float surpriseDmg = 0f;
	//화살을 쏘는 원거리 스킬인지 (조준 사격): 인접하지 않아도 되고, 화살 1발을 쓰고, 맞으면 밀쳐낸다
	public boolean ranged = false;
	//근접 스킬이 닿는 거리 (1 = 인접. 무형검은 MazeTower.SKILL_RUNIC_BLADE_RANGE칸, 마탄이 날아가 닿을 수 있어야 한다)
	public int reach = 1;
	//Maze Tower: 강화형 활 스킬 (석궁 장전·정량궁 집중). -1 = 아님. 쓰면 1턴을 쓰고 다음 원거리 평타가 이 스킬 판정으로 강화된다
	public int charge = -1;
	//벽을 꿰뚫는 화살 (정령의 활 꿰뚫기): 보이는 적이면 길이 막혀도 맞힌다
	public boolean pierce = false;
	//날아가는 화살 모양 (WornShortbow.SHOT_*)
	public int shotKind = WornShortbow.SHOT_PLAIN;
	//Maze Tower: 타격마다 / 스킬이 끝날 때 따로 할 일 (고리지기 반지 스킬: 던지기·충전 터뜨리기·금화 주먹)
	public interface HitHook { void onHit(Hero hero, Char enemy, boolean hit, int index); }
	public HitHook afterHit = null;
	public Runnable onEnd = null;

	//Maze Tower: 고리지기 반지 스킬 (맨손, 인접한 적). 무기 없이 SwordSkill 흐름을 그대로 쓴다
	public static SwordSkill ringSkill(String key, int hits, float dmg){
		return new SwordSkill(key, hits, dmg, 1f, false, 1f);
	}

	public static void launchRing(Hero hero, SwordSkill skill, Char enemy, int cost){
		launch(hero, null, skill, enemy, cost, -1, true);
	}

	private SwordSkill(String nameKey, int hits, float dmgPerHit, float accMulti, boolean ignoreDefense, float elementChanceMulti){
		this.nameKey = nameKey;
		this.hits = hits;
		this.dmgPerHit = dmgPerHit;
		this.accMulti = accMulti;
		this.ignoreDefense = ignoreDefense;
		this.elementChanceMulti = elementChanceMulti;
	}

	private static SwordSkill basic(String key, float dmg){
		return new SwordSkill(key, 1, dmg, 1f, false, 1f);
	}

	//조준 사격 (단궁·탄궁·장궁): 명중 +30%, 밀쳐내기 없음
	private static SwordSkill aimed(float dmg){
		SwordSkill s = new SwordSkill("aimed", 1, dmg, MazeTower.SKILL_AIMED_ACCURACY, false, 1f);
		s.ranged = true;
		return s;
	}

	//무기별 스킬. 표에 없는 무기는 null (스킬 없음)
	public static SwordSkill forWeapon(Item wep){
		if (wep == null) return null;
		Class<?> c = wep.getClass();
		if (c == WornShortsword.class) return basic("slash", MazeTower.SKILL_WORN_SHORTSWORD_DMG);
		if (c == Shortsword.class)     return basic("slash", MazeTower.SKILL_SHORTSWORD_DMG);
		if (c == Longsword.class)      return basic("slash", MazeTower.SKILL_LONGSWORD_DMG);
		if (c == Rapier.class)         return new SwordSkill("thrust", 1, MazeTower.SKILL_RAPIER_DMG, Char.INFINITE_ACCURACY, false, 1f);
		if (c == Scimitar.class)       return new SwordSkill("flurry", MazeTower.SKILL_SCIMITAR_HITS, MazeTower.SKILL_SCIMITAR_DMG, 1f, false, 1f);
		if (c == Katana.class)         return new SwordSkill("crescent", MazeTower.SKILL_KATANA_HITS, MazeTower.SKILL_KATANA_DMG, MazeTower.SKILL_KATANA_ACCURACY, false, 1f);
		if (c == RunicBlade.class){
			SwordSkill s = new SwordSkill("phantom", 1, MazeTower.SKILL_RUNIC_BLADE_DMG, 1f, true, MazeTower.SKILL_RUNIC_BLADE_ELEMENT_CHANCE);
			s.reach = MazeTower.SKILL_RUNIC_BLADE_RANGE;
			return s;
		}
		//Maze Tower: 활 7종
		if (c == WornShortbow.class) return aimed(MazeTower.SKILL_AIMED_SHOT_DMG);
		if (c == PelletBow.class)    return aimed(MazeTower.SKILL_PELLET_AIMED_DMG);
		if (c == Longbow.class)      return aimed(MazeTower.SKILL_LONG_AIMED_DMG);
		if (c == HornBow.class){
			SwordSkill s = new SwordSkill("rapid", MazeTower.SKILL_RAPID_HITS, MazeTower.SKILL_RAPID_DMG, 1f, false, 1f);
			s.ranged = true;
			return s;
		}
		if (c == MazeCrossbow.class){
			SwordSkill s = basic("load", MazeTower.SKILL_LOAD_DMG);
			s.ranged = true;
			s.charge = BowCharge.LOAD;
			return s;
		}
		if (c == GreatBow.class){
			SwordSkill s = basic("focus", MazeTower.SKILL_FOCUS_DMG);
			s.ranged = true;
			s.charge = BowCharge.FOCUS;
			return s;
		}
		if (c == MazeSpiritBow.class){
			SwordSkill s = basic("pierce", MazeTower.SKILL_PIERCE_DMG);
			s.ranged = true;
			s.pierce = true;
			s.shotKind = WornShortbow.SHOT_MAGIC;
			return s;
		}
		if (c == WornDagger.class){
			SwordSkill s = basic("vital", MazeTower.SKILL_WORN_DAGGER_DMG);
			s.surpriseDmg = MazeTower.SKILL_WORN_DAGGER_SURPRISE_DMG;
			return s;
		}
		return null;
	}

	//이 영웅이 마나 스킬을 쓰는지 (결투가는 원작 능력을 그대로 쓴다)
	public static boolean heroUsesSkills(Hero hero){
		return hero != null && MazeTower.isPlayableClass(hero.heroClass); //4직업 모두 마나 스킬
	}

	public static int manaCost(Hero hero){
		return BloomEffects.skillCost(hero, MazeTower.SKILL_MANA_COST); //절약
	}

	// ===== 스킬 사용 중 상태 (다른 코드에서 확인용) =====

	//지금 처리 중인 스킬. 스킬 타격 중에만 null이 아니다.
	public static SwordSkill active = null;

	//급소 찌르기를 기습이 아닌 상태로 쓰는 중이면 맞을 때 피가 튀지 않는다 (핏빛 연출은 기습 전용)
	private static boolean noBlood = false;
	public static boolean hidingHitBlood(){
		return active != null && noBlood;
	}

	//방어 무시 타격 중인지 (Char.attack에서 확인)
	public static boolean ignoringDefense(){
		return active != null && active.ignoreDefense;
	}

	public String name(){
		return Messages.get(SwordSkill.class, nameKey);
	}

	public String desc(){
		String hitsTxt = hits > 1
				? Messages.get(SwordSkill.class, "dmg_multi", Math.round(dmgPerHit*100), hits, Math.round(dmgPerHit*hits*100))
				: Messages.get(SwordSkill.class, "dmg_single", Math.round(dmgPerHit*100));
		int cost = Dungeon.hero != null ? manaCost(Dungeon.hero) : MazeTower.SKILL_MANA_COST;
		String desc;
		//Maze Tower: 활 스킬은 설명 첫머리가 다르다 (화살을 쏨 / 장전·집중은 피해 없음)
		if (charge >= 0)  desc = Messages.get(SwordSkill.class, "info_charge", name(), cost).trim();
		else if (ranged)  desc = Messages.get(SwordSkill.class, "info_ranged", name(), hitsTxt, cost);
		else              desc = Messages.get(SwordSkill.class, "info", name(), hitsTxt, cost);
		String extra = surpriseDmg > 0
				? Messages.get(SwordSkill.class, nameKey + "_desc", Math.round(surpriseDmg*100))
				: Messages.get(SwordSkill.class, nameKey + "_desc");
		if (!extra.isEmpty() && !extra.startsWith("!!!")) desc += " " + extra;
		return desc;
	}

	// ===== 실행 =====

	//무기의 AC_ABILITY에서 호출된다 (무기는 이미 장착된 상태)
	public static void execute(final Hero hero, final MeleeWeapon wep){
		final SwordSkill skill = forWeapon(wep);
		if (skill == null) return;

		if (wep.STRReq() > hero.STR()){
			GLog.w(Messages.get(MeleeWeapon.class, "ability_low_str"));
			return;
		}
		if (Mana.get(hero).cur() < manaCost(hero)){
			GLog.w(Messages.get(SwordSkill.class, "no_mana"));
			ActionIndicator.shake(); //마나 부족: 스킬 버튼 흔들림
			return;
		}

		//Maze Tower: 강화형 활 스킬 (장전·집중)은 대상을 고르지 않고 바로 쓴다
		if (skill.charge >= 0){
			BowCharge.use(hero, wep, skill.charge, manaCost(hero));
			return;
		}

		wep.usesTargeting = true;
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell != null) {
					perform(hero, wep, skill, cell);
				}
			}

			@Override
			public String prompt() {
				return Messages.get(SwordSkill.class, skill.ranged ? "prompt_ranged" : "prompt"); //Maze Tower: 활 스킬은 떨어진 적도
			}
		});
	}

	private static void perform(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, int target){
		final Char enemy = Actor.findChar(target);
		int dashIn = -1; //섬광 보법으로 돌진할 칸
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(SwordSkill.class, "no_target"));
			return;
		}
		if (skill.ranged){
			//조준 사격: 화살이 있어야 하고, 화살길이 열려 있어야 한다
			if (Arrow.count(hero) <= 0){
				GLog.w(Messages.get(SwordSkill.class, "no_arrows"));
				ActionIndicator.shake();
				return;
			}
			if (!skill.pierce && !WornShortbow.clearShot(hero.pos, enemy.pos)){ //꿰뚫기는 벽 너머도 맞힌다
				GLog.w(Messages.get(SwordSkill.class, "no_path"));
				return;
			}
		//무형검: reach칸 안, 마탄이 날아가 닿을 수 있는 적
		} else if (skill.reach > 1){
			if (Dungeon.level.distance(hero.pos, enemy.pos) > skill.reach
					|| new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, enemy.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE).collisionPos != enemy.pos){
				GLog.w(Messages.get(SwordSkill.class, "not_in_reach", skill.reach));
				return;
			}
		//나머지 스킬은 인접한 단일 대상
		} else if (!Dungeon.level.adjacent(hero.pos, enemy.pos)){
			//Maze Tower: 2차 특성 섬광 보법 (근거리 스킬): 떨어진 적이면 바로 앞 칸으로 돌진
			dashIn = com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.dashInCell(hero, enemy);
			if (dashIn == -1){
				GLog.w(Messages.get(SwordSkill.class, "not_adjacent"));
				return;
			}
		}
		final int cost = manaCost(hero);
		if (Mana.get(hero).cur() < cost){
			GLog.w(Messages.get(SwordSkill.class, "no_mana"));
			ActionIndicator.shake(); //마나 부족: 스킬 버튼 흔들림
			return;
		}
		launch(hero, wep, skill, enemy, cost, dashIn, true);
	}

	//Maze Tower: 대상 확인이 끝난 스킬을 실제로 쓴다.
	//  spendMana = false: 강화된 평타 (장전·집중으로 이미 마나를 낸 원거리 평타. 스킬 판정이지만 마나는 다시 쓰지 않는다)
	private static void launch(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, final Char enemy, final int cost, int dashIn, boolean spendMana){
		//급소 찌르기: 기습 상태인지는 휘두르기 전에 본다
		final boolean surprise = skill.surpriseDmg > 0 && enemy instanceof Mob && ((Mob)enemy).surprisedBy(hero, true);
		BloomEffects.beginSkill(hero, enemy, cost); //마나가 가득 찼는지, 집중 상태 기록
		if (skill.ranged) com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.markRanged(); //Maze Tower: 2차 특성 섬광 보법 (원거리 스킬)
		if (dashIn != -1){
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.markDashIn(); //Maze Tower: 섬광 보법 (근거리 스킬 돌진)
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.dash(hero, dashIn);
		}
		if (spendMana) Mana.get(hero).spend(cost);
		SproutEffects.onSkillUsed(hero); //1차 특성: 투지
		GladiatorEffects.onSkillStart(); //3차 검투사: 기술 연계용 적중 기록

		hero.busy();
		Callback strike = new Callback() {
			@Override
			public void call() {
				AttackIndicator.target(enemy);
				doHit(hero, wep, skill, enemy, 0, surprise);
			}
		};
		if (skill.ranged) WornShortbow.fireAnimation(hero, enemy, strike, skill.shotKind); //화살이 날아가 닿으면 타격
		else              hero.sprite.attack(enemy.pos, strike);
	}

	//타격 한 번. 여러 번 때리는 스킬은 타격 사이에 MazeTower.SKILL_HIT_INTERVAL초 텀을 둔다.
	//(그동안 주인공은 행동 중이라 다른 누구도 움직이지 않는다)
	private static void doHit(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, final Char enemy, final int index, final boolean surprise){
		if (index == 0) startPos = enemy.pos;
		hero.belongings.abilityWeapon = wep;

		SkillEffects.hit(skill.nameKey, hero, enemy, index, surprise);
		active = skill;
		noBlood = skill.surpriseDmg > 0 && !surprise;
		boolean wasEnemy = GladiatorEffects.wasEnemy(enemy);
		boolean shot = skill.ranged && WornShortbow.beginShot(hero, enemy, true); //화살 1발, 원거리 피해
		boolean hit = hero.attack(enemy, surprise ? skill.surpriseDmg : skill.dmgPerHit, 0, skill.accMulti);
		if (hit){
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
		}
		if (skill.ranged) WornShortbow.endShot(hero, enemy, hit, shot, false); //박힌 화살 (Maze Tower: 활 스킬은 밀쳐내지 않는다)
		if (shot && hit) SkillEffects.afterHit(skill.nameKey, hero, enemy); //Maze Tower: 활 스킬이 맞으면 피가 튀는 등
		GladiatorEffects.onSkillHit(hero, enemy, hit, wasEnemy); //검투사: 타격 한 번마다 연속 타격 +1
		if (skill.afterHit != null) skill.afterHit.onHit(hero, enemy, hit, index); //Maze Tower: 반지 스킬
		active = null;
		noBlood = false;

		//(결정) 맞은 적이 다른 곳으로 순간이동했거나 은신하면 남은 타격은 멈춘다
		//Maze Tower: 화살을 여러 발 쏘는 스킬(속사)은 화살이 떨어지면 멈춘다
		if (index + 1 < skill.hits && enemy.isAlive() && hero.isAlive() && stillThere(enemy) && hero.sprite != null && hero.sprite.parent != null
				&& (!skill.ranged || Arrow.count(hero) > 0)){
			Delayer wait = new Delayer(MazeTower.SKILL_HIT_INTERVAL){
				@Override
				protected void onComplete() {
					super.onComplete();
					if (skill.ranged && hero.sprite != null && hero.sprite.parent != null && enemy.sprite != null){
						//다음 화살이 날아가 닿으면 타격
						WornShortbow.launchArrow(hero, enemy, new Callback() {
							@Override
							public void call() {
								doHit(hero, wep, skill, enemy, index + 1, surprise);
							}
						}, skill.shotKind);
					} else {
						doHit(hero, wep, skill, enemy, index + 1, surprise);
					}
				}
			};
			hero.sprite.parent.add(wait);
		} else {
			finish(hero, wep, skill);
		}
	}

	//Maze Tower: 장전·집중으로 강화된 원거리 평타. Hero.actAttack에서 부른다.
	//스킬 판정이라 2차 스킬 특성·추적자 사냥감 지정 등이 붙는다. 마나는 장전·집중할 때 이미 냈다.
	public static void fireCharged(final Hero hero, final WornShortbow bow, final BowCharge charge, final Char enemy){
		SwordSkill skill = basic(charge.kind == BowCharge.LOAD ? "loaded" : "focused",
				charge.kind == BowCharge.LOAD ? MazeTower.SKILL_LOAD_DMG : MazeTower.SKILL_FOCUS_DMG);
		skill = new SwordSkill(skill.nameKey, 1, skill.dmgPerHit,
				charge.kind == BowCharge.LOAD ? MazeTower.SKILL_LOAD_ACCURACY : MazeTower.SKILL_FOCUS_ACCURACY, false, 1f);
		skill.ranged = true;
		skill.shotKind = charge.kind == BowCharge.LOAD ? WornShortbow.SHOT_LOADED : WornShortbow.SHOT_PLAIN;
		int paid = charge.paid;
		charge.detach();
		launch(hero, bow, skill, enemy, paid, -1, false);
	}

	//여러 번 때리는 공격: 첫 타격 때 적이 있던 칸
	private static int startPos = -1;

	//Maze Tower: 연속 공격을 이어 가도 되는지 (적이 순간이동하거나 은신하면 멈춘다)
	public static boolean stillThere( Char enemy ){
		return enemy.isAlive() && enemy.pos == startPos && enemy.invisible <= 0;
	}

	private static void finish(Hero hero, MeleeWeapon wep, SwordSkill skill){
		if (skill != null && skill.onEnd != null) skill.onEnd.run(); //Maze Tower: 반지 스킬 (금화 주먹 처치 시 돌려받기 등)
		BloomEffects.endSkill(hero); //처치 시 마나 반환, 흐름·잔향·흘리기
		GladiatorEffects.onSkillEnd(hero); //검투사 기술 연계: 타격이 끝난 뒤 연속 타격 +

		Invisibility.dispel();
		hero.belongings.abilityWeapon = wep;
		float delay = hero.attackDelay();
		if (wep != null) wep.afterAbilityUsed(hero);
		else hero.belongings.abilityWeapon = null; //Maze Tower: 반지 스킬 (맨손)
		hero.spendAndNext(delay);
		Item.updateQuickslot();
	}
}
