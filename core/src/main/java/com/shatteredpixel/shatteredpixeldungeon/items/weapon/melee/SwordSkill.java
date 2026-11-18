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
import com.shatteredpixel.shatteredpixeldungeon.effects.SkillEffects;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
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
		if (c == RunicBlade.class)     return new SwordSkill("phantom", 1, MazeTower.SKILL_RUNIC_BLADE_DMG, 1f, true, MazeTower.SKILL_RUNIC_BLADE_ELEMENT_CHANCE);
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
		String desc = Messages.get(SwordSkill.class, "info", name(), hitsTxt, (Dungeon.hero != null ? manaCost(Dungeon.hero) : MazeTower.SKILL_MANA_COST));
		String extra = Messages.get(SwordSkill.class, nameKey + "_desc");
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
				return Messages.get(SwordSkill.class, "prompt");
			}
		});
	}

	private static void perform(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, int target){
		final Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(SwordSkill.class, "no_target"));
			return;
		}
		//모든 스킬은 인접한 단일 대상
		if (!Dungeon.level.adjacent(hero.pos, enemy.pos)){
			GLog.w(Messages.get(SwordSkill.class, "not_adjacent"));
			return;
		}
		final int cost = manaCost(hero);
		if (Mana.get(hero).cur() < cost){
			GLog.w(Messages.get(SwordSkill.class, "no_mana"));
			ActionIndicator.shake(); //마나 부족: 스킬 버튼 흔들림
			return;
		}
		BloomEffects.beginSkill(hero, enemy, cost); //마나가 가득 찼는지, 집중 상태 기록
		Mana.get(hero).spend(cost);

		hero.busy();
		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				AttackIndicator.target(enemy);
				doHit(hero, wep, skill, enemy, 0);
			}
		});
	}

	//타격 한 번. 여러 번 때리는 스킬은 타격 사이에 MazeTower.SKILL_HIT_INTERVAL초 텀을 둔다.
	//(그동안 주인공은 행동 중이라 다른 누구도 움직이지 않는다)
	private static void doHit(final Hero hero, final MeleeWeapon wep, final SwordSkill skill, final Char enemy, final int index){
		hero.belongings.abilityWeapon = wep;

		SkillEffects.hit(skill.nameKey, hero, enemy, index);
		active = skill;
		if (hero.attack(enemy, skill.dmgPerHit, 0, skill.accMulti)){
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
		}
		active = null;

		if (index + 1 < skill.hits && enemy.isAlive() && hero.isAlive() && hero.sprite != null && hero.sprite.parent != null){
			Delayer wait = new Delayer(MazeTower.SKILL_HIT_INTERVAL){
				@Override
				protected void onComplete() {
					super.onComplete();
					doHit(hero, wep, skill, enemy, index + 1);
				}
			};
			hero.sprite.parent.add(wait);
		} else {
			finish(hero, wep);
		}
	}

	private static void finish(Hero hero, MeleeWeapon wep){
		BloomEffects.endSkill(hero); //처치 시 마나 반환, 흐름·잔향·흘리기

		Invisibility.dispel();
		hero.belongings.abilityWeapon = wep;
		float delay = hero.attackDelay();
		wep.afterAbilityUsed(hero);
		hero.spendAndNext(delay);
		Item.updateQuickslot();
	}
}
