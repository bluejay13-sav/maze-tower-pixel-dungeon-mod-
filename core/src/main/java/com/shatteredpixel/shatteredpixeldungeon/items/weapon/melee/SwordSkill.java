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
	//Maze Tower (B4): 장전·집중으로 강화된 평타를 쏘는 중 (fireCharged). 이 한 발이 강화형 스킬의 스킬 타격이다
	public boolean chargedShot = false;
	//벽을 꿰뚫는 화살 (정령의 활 꿰뚫기): 보이는 적이면 길이 막혀도 맞힌다
	public boolean pierce = false;
	//Maze Tower (W4): 숨통 물기 - 맞는 순간 대상 체력이 최대의 절반보다 낮으면 그 타격은 방어 무시 (타격마다 따로 판정)
	public boolean halfHpPierce = false;
	private static boolean pierceThisHit = false;
	//날아가는 화살 모양 (WornShortbow.SHOT_*)
	public int shotKind = WornShortbow.SHOT_PLAIN;

	//Maze Tower (C6): 한 번 더 각성 - 마지막 타는 반드시 적중
	public boolean sureLast = false;

	//Maze Tower (C6): 타수와 타격당 배율만 바꾼 사본 (나머지 성질은 그대로)
	public SwordSkill withHits(int newHits, float newDmgPerHit, float surpriseScale){
		SwordSkill s = new SwordSkill(nameKey, newHits, newDmgPerHit, accMulti, ignoreDefense, elementChanceMulti);
		s.surpriseDmg = surpriseDmg * surpriseScale;
		s.ranged = ranged;
		s.reach = reach;
		s.charge = charge;
		s.chargedShot = chargedShot;
		s.pierce = pierce;
		s.halfHpPierce = halfHpPierce; //Maze Tower (W4): 한 번 더로 두 발이 되어도 발마다 판정
		s.shotKind = shotKind;
		s.sureLast = sureLast;
		return s;
	}

	//Maze Tower (C5): 메아리 잔상이 다시 쓰는 스킬 (저장해 둔 값으로 다시 만든다)
	public static SwordSkill echoOf(String key, int hits, float dmg, float acc, boolean ignoreDefense, boolean ranged, int reach, boolean pierce){
		SwordSkill s = new SwordSkill(key, hits, dmg, acc, ignoreDefense, 1f);
		s.ranged = ranged;
		s.reach = reach;
		s.pierce = pierce;
		return s;
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

	// ===== Maze Tower (W2): 공명검 방출 =====
	public static final String DISCHARGE = "discharge";

	public boolean isDischarge(){
		return DISCHARGE.equals(nameKey);
	}

	private static SwordSkill discharge(float mult){
		return new SwordSkill(DISCHARGE, 1, mult, 1f, false, 1f);
	}

	//방출로 쓸 마나: 남은 마나 전부 (비용보다 적으면 쓸 수 없으므로, 미리 보기에는 비용으로 계산)
	public static int dischargeSpend(Hero hero){
		if (hero == null) return MazeTower.SKILL_MANA_COST;
		return Math.max(manaCost(hero), Mana.get(hero).cur());
	}

	//배율: 형태 특성의 추가 마나를 먼저 뺀 남은 마나로 계산한다. 200% + (남은 마나 − 30) × 2.5%
	public static float dischargeMult(Hero hero, int spent){
		int extra = hero == null ? 0 : BloomEffects.skillCost(hero, MazeTower.SKILL_MANA_COST) - MazeTower.SKILL_MANA_COST;
		int basis = spent - extra;
		return MazeTower.SKILL_DISCHARGE_BASE + Math.max(0, basis - MazeTower.SKILL_MANA_COST) * MazeTower.SKILL_DISCHARGE_PER_MANA;
	}

	//스킬 버튼에 보일 배율 (공명검을 들고 있지 않으면 null)
	public static String dischargeLabel(Hero hero){
		if (hero == null || !(hero.belongings.weapon() instanceof ResonanceSword)) return null;
		return Math.round(dischargeMult(hero, dischargeSpend(hero)) * 100) + "%";
	}

	//지금 쓰는 방출: 쓴 마나(반환 계산용, 이어 쓰기는 0)와 반환을 이미 받았는지
	private static int dischargeSpent = 0;
	private static boolean dischargeRefunded = false;
	//거울 초상이 방출을 따라 쓸 때는 최소치(200%)로: 실제 배율에 대한 비 (3절 9번 임시 기본값)
	public static float dischargeMirrorScale = 1f;

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
		//Maze Tower (W1): 5티어 대검(원작 특대검) - 내려찍기
		if (c == Greatsword.class)     return basic("smash", MazeTower.SKILL_GREATSWORD_DMG);
		//Maze Tower (W2): 5티어 공명검 - 방출. 배율은 지금 마나로 쓰면 나올 값 (설명창·스킬 버튼에 보임)
		if (c == ResonanceSword.class) return discharge(dischargeMult(Dungeon.hero, dischargeSpend(Dungeon.hero)));
		//Maze Tower: 활 7종
		if (c == WornShortbow.class) return aimed(MazeTower.SKILL_AIMED_SHOT_DMG);
		if (c == PelletBow.class)    return aimed(MazeTower.SKILL_PELLET_AIMED_DMG);
		if (c == Longbow.class)      return aimed(MazeTower.SKILL_LONG_AIMED_DMG);
		if (c == HornBow.class){
			SwordSkill s = new SwordSkill("rapid", MazeTower.SKILL_RAPID_HITS, MazeTower.SKILL_RAPID_DMG, 1f, false, 1f);
			s.ranged = true;
			return s;
		}
		if (c == ReinforcedHornBow.class){ //Maze Tower (W3): 강화 각궁 - 각궁과 같은 속사, 발당 피해만 다르다
			SwordSkill s = new SwordSkill("rapid", MazeTower.SKILL_RAPID_HITS, MazeTower.SKILL_REINFORCED_RAPID_DMG, 1f, false, 1f);
			s.ranged = true;
			return s;
		}
		if (c == SilverFang.class){ //Maze Tower (W4): 은 송곳니 - 숨통 물기
			SwordSkill s = basic("throat", MazeTower.SKILL_THROAT_DMG);
			s.ranged = true;
			s.halfHpPierce = true;
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
		int cost = BloomEffects.skillCost(hero, MazeTower.SKILL_MANA_COST); //절약
		//Maze Tower (K8): 미스릴 무기를 들면 스킬 마나 -5
		if (hero != null && hero.belongings.weapon() instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) hero.belongings.weapon()).mazeMithril != null){
			cost = Math.max(0, cost - MazeTower.MITHRIL_MANA_CUT);
		}
		return cost;
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
		return active != null && (active.ignoreDefense || pierceThisHit); //Maze Tower (W4): 숨통 물기
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
		else if (isDischarge()) desc = Messages.get(SwordSkill.class, "info_discharge", name(), hitsTxt, cost); //Maze Tower (W2): 남은 마나 전부
		else if (ranged)  desc = Messages.get(SwordSkill.class, "info_ranged", name(), hitsTxt, cost);
		else              desc = Messages.get(SwordSkill.class, "info", name(), hitsTxt, cost);
		String extra = surpriseDmg > 0
				? Messages.get(SwordSkill.class, nameKey + "_desc", Math.round(surpriseDmg*100))
				: isDischarge()
				? Messages.get(SwordSkill.class, "discharge_desc", Math.round(MazeTower.SKILL_DISCHARGE_BASE*100),
						MazeTower.SKILL_MANA_COST, MazeTower.SKILL_DISCHARGE_PER_MANA*100, Math.round(MazeTower.SKILL_DISCHARGE_REFUND*100))
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
		//Maze Tower (J1): 마력 증기 안(과 나온 직후)에는 스킬을 쓰지 못한다
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.skillsBlocked(hero)){
			GLog.w(Messages.get(SwordSkill.class, "vapor_blocked"));
			ActionIndicator.shake();
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
				//Maze Tower (사용자): 활 스킬은 사거리(BOW_RANGE칸) 밖의 칸을 고를 수 없다 — 알려 주고 다시 고르게
				if (cell != null && skill.ranged && cell != hero.pos && !WornShortbow.inRange(hero.pos, cell)){
					GLog.w(Messages.get(SwordSkill.class, "out_of_range", WornShortbow.range()));
					GameScene.selectCell(this);
					return;
				}
				if (cell != null) {
					//Maze Tower: (사용자) 퀵슬롯의 검을 두 번 누르면 스킬 대신 타게팅된 적에게 기본공격 (공격 범위 안일 때만)
					if (com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.mazeAutoFire && !skill.ranged){
						autoAttack(hero, cell);
					} else {
						perform(hero, wep, skill, cell);
					}
				}
			}

			@Override
			public String prompt() {
				return Messages.get(SwordSkill.class, skill.ranged ? "prompt_ranged" : "prompt"); //Maze Tower: 활 스킬은 떨어진 적도
			}
		});
	}

	//Maze Tower: (사용자) 퀵슬롯 두 번 누르기 → 타게팅된 적에게 기본공격. 공격 범위 밖이면 걸어가지 않고 알려 준다
	public static void autoAttack(Hero hero, int cell){
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == hero) return;
		if (!hero.canAttack(ch)){
			GLog.w(Messages.get(SwordSkill.class, "auto_out_of_range"));
			return;
		}
		if (hero.handle(cell)) hero.next();
	}

	//Maze Tower: (사용자) 활 스킬은 칸을 골라 쏜다. 화살길 위의 첫 캐릭터에게 스킬, 아무도 없으면 화살이 그 칸에 떨어진다(마나·턴은 쓴다)
	private static void performRanged(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, int target){
		if (target == hero.pos) return;
		if (!WornShortbow.inRange(hero.pos, target)){ //(사용자) 사거리 밖의 칸은 캐릭터가 없어도 쏘지 않는다
			GLog.w(Messages.get(SwordSkill.class, "out_of_range", WornShortbow.range()));
			return;
		}
		if (Arrow.count(hero) <= 0){
			GLog.w(Messages.get(SwordSkill.class, "no_arrows"));
			ActionIndicator.shake();
			return;
		}
		int[] a = WornShortbow.aim(hero, target, skill.pierce);
		Char enemy = a[0] >= 0 ? Actor.findChar(a[0]) : null;
		if (enemy != null && WornShortbow.blocksShot(hero, enemy)){
			GLog.w(Messages.get(SwordSkill.class, "no_path"));
			return;
		}
		if (enemy == null && a[1] == hero.pos){
			GLog.w(Messages.get(SwordSkill.class, "no_path"));
			return;
		}
		final int cost = manaCost(hero);
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.skillsBlocked(hero)){ //Maze Tower (J1)
			GLog.w(Messages.get(SwordSkill.class, "vapor_blocked"));
			ActionIndicator.shake();
			return;
		}
		if (Mana.get(hero).cur() < cost){
			GLog.w(Messages.get(SwordSkill.class, "no_mana"));
			ActionIndicator.shake();
			return;
		}
		if (enemy != null){
			launch(hero, wep, skill, enemy, cost, -1, true);
		} else {
			Mana.get(hero).spendForSkill(cost);
			WornShortbow.emptyShot(hero, a[1], skill.shotKind);
			Item.updateQuickslot();
		}
	}

	private static void perform(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, int target){
		if (skill.ranged){ performRanged(hero, wep, skill, target); return; }
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
			if (!WornShortbow.inRange(hero.pos, enemy.pos)){ //(결정) 활의 사거리 MazeTower.BOW_RANGE칸
				GLog.w(Messages.get(SwordSkill.class, "out_of_range", WornShortbow.range()));
				return;
			}
			if (!skill.pierce && !WornShortbow.clearShot(hero.pos, enemy.pos)){ //꿰뚫기는 벽 너머도 맞힌다
				GLog.w(Messages.get(SwordSkill.class, "no_path"));
				return;
			}
		//무형검: reach칸 안, 마탄이 날아가 닿을 수 있는 적
		} else if (skill.reach > 1){
			//(2026-11-24) 섬광 보법이 준비되어 있으면 무형검도 떨어진 적 바로 앞 칸으로 돌진해 쓴다. 못 하면 그 자리에서 쓴다
			if (!Dungeon.level.adjacent(hero.pos, enemy.pos)){
				dashIn = com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.dashInCell(hero, enemy);
			}
			if (dashIn == -1 && (Dungeon.level.distance(hero.pos, enemy.pos) > skill.reach
					|| new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, enemy.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.PROJECTILE).collisionPos != enemy.pos)){
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
		int cost = manaCost(hero);
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.skillsBlocked(hero)){ //Maze Tower (J1)
			GLog.w(Messages.get(SwordSkill.class, "vapor_blocked"));
			ActionIndicator.shake();
			return;
		}
		if (Mana.get(hero).cur() < cost){
			GLog.w(Messages.get(SwordSkill.class, "no_mana"));
			ActionIndicator.shake(); //마나 부족: 스킬 버튼 흔들림
			return;
		}
		if (skill.isDischarge()) cost = Mana.get(hero).cur(); //Maze Tower (W2): 방출은 남은 마나를 전부 쓴다
		launch(hero, wep, skill, enemy, cost, dashIn, true);
	}

	//Maze Tower: 대상 확인이 끝난 스킬을 실제로 쓴다.
	//  spendMana = false: 강화된 평타 (장전·집중으로 이미 마나를 낸 원거리 평타. 스킬 판정이지만 마나는 다시 쓰지 않는다)
	private static void launch(final Hero hero, final MeleeWeapon wep, SwordSkill base, final Char enemy, final int cost, int dashIn, boolean spendMana){
		//Maze Tower (W2): 방출의 배율은 실제로 쓴 마나로 정한다. 마나 없이 이어 쓰면(섬광 보법 각성) 최소치, 반환 없음
		if (base.isDischarge()){
			float mult = spendMana ? dischargeMult(hero, cost) : MazeTower.SKILL_DISCHARGE_BASE;
			base = discharge(mult);
			dischargeSpent = spendMana ? cost : 0;
			dischargeRefunded = false;
			dischargeMirrorScale = MazeTower.SKILL_DISCHARGE_BASE / mult;
		}
		BloomEffects.beginSkill(hero, enemy, cost); //마나가 가득 찼는지, 집중 상태 기록
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.BladeDancer.onSkillLaunch(hero, spendMana, base.chargedShot); //Maze Tower (S3): 검무가 끝맺는 칼
		currentBase = base; //Maze Tower (C17): 섬광 보법 각성으로 이어 쓸 때 다시 쓰는 원래 스킬
		final SwordSkill skill = BloomEffects.reshapeSkill(hero, base); //Maze Tower (C6): 한 번 더 (타수 +1)
		//급소 찌르기: 기습 상태인지는 휘두르기 전에 본다
		final boolean surprise = skill.surpriseDmg > 0 && enemy instanceof Mob && ((Mob)enemy).surprisedBy(hero, true);
		if (dashIn != -1){
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.markDashIn(); //Maze Tower: 섬광 보법 (검 스킬 돌진)
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.dash(hero, dashIn);
		}
		if (!skill.chargedShot) com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MirrorPortrait.onHeroSkill(hero, skill); //Maze Tower (L6): 거울 초상이 따라 쓸 준비 (강화형 제외)
		if (spendMana) Mana.get(hero).spendForSkill(cost); //(A1) 마나를 써서 공격한 턴은 회복 없음
		SproutEffects.onSkillUsed(hero); //1차 특성: 투지

		hero.busy();
		Callback strike = new Callback() {
			@Override
			public void call() {
				AttackIndicator.target(enemy);
				//(2026-11-21) 꿰뚫기: 정령 화살이 지나간 벽 칸에 잔광 (fx_hunter_05)
				if (skill.pierce){
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica b = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(hero.pos, enemy.pos, com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET);
					for (int c : b.subPath(1, b.dist)){
						if (Dungeon.level.solid[c] && Dungeon.level.heroFOV[c] || Dungeon.level.solid[c] && Dungeon.level.visited[c]){
							com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.atCell(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.H_WALL, c);
						}
					}
				}
				//Maze Tower (W1): (사용자) 내려찍기는 궤적이 중간쯤 갔을 때 피해가 들어가게: 궤적 첫 장을 먼저 그리고, 가운데를 지나는 장과 함께 타격
				if ("smash".equals(skill.nameKey) && hero.sprite != null && hero.sprite.parent != null){
					SkillEffects.smashSwing(hero, enemy);
					hero.sprite.parent.add(new Delayer(SkillEffects.SMASH_SWING_TIME){
						@Override
						protected void onComplete() {
							super.onComplete();
							doHit(hero, wep, skill, enemy, 0, surprise);
						}
					});
					return;
				}
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
		boolean shot = skill.ranged && WornShortbow.beginShot(hero, enemy, true); //화살 1발, 원거리 피해
		int heroCell = hero.pos, enemyCell = enemy.pos;
		BloomEffects.beforeSkillHit();
		float acc = skill.sureLast && index == skill.hits - 1 ? Char.INFINITE_ACCURACY : skill.accMulti; //Maze Tower (C6): 한 번 더 각성
		//Maze Tower (W4): 숨통 물기 - 맞는 순간(이 타격 직전) 체력이 최대의 절반보다 낮으면 방어 무시. 정확히 절반이면 아님
		pierceThisHit = skill.halfHpPierce && enemy.HP * 2 < enemy.HT;
		boolean hit = hero.attack(enemy, surprise ? skill.surpriseDmg : skill.dmgPerHit, 0, acc);
		if (hit && pierceThisHit && enemy.sprite != null) enemy.sprite.flash(); //방어를 무시한 타격: 짧은 은빛 번쩍임
		pierceThisHit = false;
		if (hit){
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
		}
		//Maze Tower (W2): 방출의 본 타격으로 쓰러뜨리면 쓴 마나의 40%를 돌려받는다 (방출 한 번에 한 번, 덧타격·파생 피해는 여기를 지나지 않음)
		if (hit && skill.isDischarge() && dischargeSpent > 0 && !dischargeRefunded && !enemy.isAlive()){
			dischargeRefunded = true;
			int back = (int)Math.floor(dischargeSpent * MazeTower.SKILL_DISCHARGE_REFUND);
			if (back > 0){
				Mana.get(hero).gain(back); //스킬의 효과라 이 턴에도 들어온다 (턴 회복은 그대로 건너뜀)
				com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects.manaRefund(hero, enemy, back);
			}
		}
		if (skill.ranged) WornShortbow.endShot(hero, enemy, hit, shot, false); //박힌 화살 (Maze Tower: 활 스킬은 밀쳐내지 않는다)
		if (shot && hit) SkillEffects.afterHit(skill.nameKey, hero, enemy); //Maze Tower: 활 스킬이 맞으면 피가 튀는 등

		//Maze Tower (B4): 스킬 타격이 끝났다 → 형태 특성이 모두 여기에 붙는다
		BloomEffects.SkillHit h = new BloomEffects.SkillHit();
		h.hero = hero;
		h.target = enemy;
		h.fromCell = heroCell;
		h.targetCell = enemyCell;
		h.hit = hit;
		h.damage = hit ? BloomEffects.lastSkillHitDamage() : 0;
		h.charged = skill.chargedShot;
		h.baseHits = skill.hits;
		h.index = index;
		h.skillKey = skill.nameKey;
		h.ranged = skill.ranged;
		h.dmgMulti = surprise ? skill.surpriseDmg : skill.dmgPerHit;
		h.accMulti = skill.accMulti; //덧타격은 각성 필중을 물려받지 않는다
		BloomEffects.onSkillHitDone(h);

		active = null;
		noBlood = false;

		//(결정) 맞은 적이 다른 곳으로 순간이동했거나 은신하면 남은 타격은 멈춘다
		//Maze Tower: 화살을 여러 발 쏘는 스킬(속사)은 화살이 떨어지면 멈춘다
		if (index + 1 < skill.hits && enemy.isAlive() && hero.isAlive() && stillThere(enemy) && hero.sprite != null && hero.sprite.parent != null
				&& (!skill.ranged || Arrow.count(hero) > 0)){
			Delayer wait = new Delayer(skill.ranged ? MazeTower.SKILL_RAPID_INTERVAL : MazeTower.SKILL_HIT_INTERVAL){ //(결정, 2026-11-21) 속사 화살 사이는 짧게
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
	//스킬 판정이라 2차 스킬 특성 등이 붙는다. 마나는 장전·집중할 때 이미 냈다.
	public static void fireCharged(final Hero hero, final WornShortbow bow, final BowCharge charge, final Char enemy){
		SwordSkill skill = basic(charge.kind == BowCharge.LOAD ? "loaded" : "focused",
				charge.kind == BowCharge.LOAD ? MazeTower.SKILL_LOAD_DMG : MazeTower.SKILL_FOCUS_DMG);
		skill = new SwordSkill(skill.nameKey, 1, skill.dmgPerHit,
				charge.kind == BowCharge.LOAD ? MazeTower.SKILL_LOAD_ACCURACY : MazeTower.SKILL_FOCUS_ACCURACY, false, 1f);
		skill.ranged = true;
		skill.chargedShot = true; //(B4) 강화형: 이 평타 한 번이 스킬 타격
		skill.shotKind = charge.kind == BowCharge.LOAD ? WornShortbow.SHOT_LOADED : WornShortbow.SHOT_PLAIN;
		int paid = charge.paid;
		charge.detach();
		launch(hero, bow, skill, enemy, paid, -1, false);
	}

	//여러 번 때리는 공격: 첫 타격 때 적이 있던 칸
	private static int startPos = -1;

	//Maze Tower (C17): 지금 쓰는 스킬의 원래 모양 (한 번 더로 바뀌기 전)
	private static SwordSkill currentBase = null;

	//Maze Tower: 연속 공격을 이어 가도 되는지 (적이 순간이동하거나 은신하면 멈춘다)
	public static boolean stillThere( Char enemy ){
		return enemy.isAlive() && enemy.pos == startPos && enemy.invisible <= 0;
	}

	private static void finish(Hero hero, MeleeWeapon wep, SwordSkill skill){
		if (skill != null) BloomEffects.onSwordSkillFinished(hero, skill); //Maze Tower (C5): 메아리
		//Maze Tower (C17): 섬광 보법 각성 - 돌진해 쓴 스킬로 쓰러뜨렸으면 다음 적에게 마나 없이 이어서 쓴다
		final com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.Chain chain =
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomSkill.chainTarget(hero);
		final SwordSkill base = currentBase;
		BloomEffects.endSkill(hero); //처치 시 마나 반환, 흐름·잔향·흘리기

		if (chain != null && base != null && hero.isAlive()){
			//턴은 이어진 스킬이 모두 끝난 뒤 한 번만 쓴다
			launch(hero, wep, base, chain.target, 0, chain.dashIn, false);
			return;
		}

		Invisibility.dispel();
		hero.belongings.abilityWeapon = wep;
		float delay = hero.attackDelay();
		if (wep != null) wep.afterAbilityUsed(hero);
		else hero.belongings.abilityWeapon = null;
		hero.spendAndNext(delay);
		Item.updateQuickslot();
	}
}
