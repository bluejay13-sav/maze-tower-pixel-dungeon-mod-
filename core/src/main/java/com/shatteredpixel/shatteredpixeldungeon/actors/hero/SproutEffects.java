/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandMana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;

//Maze Tower: 1차 특성(발아) 20개 중 새로 만든 효과와 바뀐 효과.
//원작 특성을 재사용한 것은 원작 코드 자리에서 수치만 MazeTower 표로 바꿨다 (Maze Tower 주석).
//0레벨은 효과 없음, 2레벨은 1레벨 효과에 새 효과가 더해진다.
public class SproutEffects {

	static int pts(Hero hero, Talent t){
		return hero == null ? 0 : Math.min(2, hero.pointsInTalent(t));
	}

	// ===== 식사 (푸짐한 식사, 마력이 깃든 식사) =====

	public static void onFoodEaten(Hero hero){
		int hearty = pts(hero, Talent.HEARTY_MEAL);
		if (hearty >= 1 && hero.HP / (float)hero.HT < T1_HEARTY_HP){
			int healing = T1_HEARTY_HEAL[hearty];
			hero.HP = Math.min(hero.HP + healing, hero.HT);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);
		}
		if (hearty >= 2) addMealMana(hero, T1_HEARTY_MANA);

		int empower = pts(hero, Talent.EMPOWERING_MEAL);
		if (empower >= 1) addMealMana(hero, T1_EMPOWER_MANA[empower]);
	}

	//먹을 때마다 최대 마나 +n, 최대 T1_MEAL_MANA_TIMES회 (영구)
	private static void addMealMana(Hero hero, int n){
		Mana m = Mana.get(hero);
		if (m.mealTimes >= T1_MEAL_MANA_TIMES) return;
		m.mealTimes++;
		m.mealBonus += n;
		hero.sprite.showStatus(0x66AAFF, "+" + n);
	}

	// ===== 투지 =====

	//스킬을 쓸 때 (검·단검·활 스킬, 지팡이 스킬)
	public static void onSkillUsed(Hero hero){
		int grit = pts(hero, Talent.GRIT);
		if (grit >= 1 && hero.HP <= hero.HT * T1_GRIT_HP){
			giveShield(hero, T1_GRIT_SHIELD[grit]);
		}
	}

	public static void giveShield(Hero hero, int amount){
		if (amount <= 0) return;
		Buff.affect(hero, Barrier.class).setShield(amount);
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.SHIELDING);
	}

	// ===== 마나 순환 =====

	//대기할 때 (회복 전): 마나가 가득 찬 채로 이어서 대기한 턴이 차면 지팡이 마법봉 충전 +1
	public static void manaCycle(Hero hero, Mana m, float turns){
		int lvl = pts(hero, Talent.MANA_CYCLE);
		MagesStaff staff = WandMana.equippedStaff(hero);
		if (lvl < 1 || staff == null || staff.imbuedWand() == null || !m.isFull()){
			m.cycleTurns = 0;
			return;
		}
		m.cycleTurns += turns;
		int need = T1_MANA_CYCLE_TURNS[lvl];
		Wand w = staff.imbuedWand();
		while (m.cycleTurns >= need){
			m.cycleTurns -= need;
			if (w.curCharges < w.maxCharges){
				w.curCharges++;
				ScrollOfRecharging.charge(hero);
				Item.updateQuickslot();
			}
		}
	}

	// ===== 감정 (직감 계열) =====

	//줍는 순간: 2레벨이면 저주 여부를 안다
	public static void onItemCollected(Hero hero, Item item){
		if (item instanceof Armor && pts(hero, Talent.VETERANS_INTUITION) >= 2) item.cursedKnown = true;
		if (item instanceof Ring && pts(hero, Talent.THIEFS_INTUITION) >= 2) item.cursedKnown = true;
		if (item instanceof WornShortbow && pts(hero, Talent.SURVIVALISTS_INTUITION) >= 2) item.cursedKnown = true;
	}

	//2레벨을 찍은 순간: 이미 가진 것도 저주 여부를 안다
	public static void onTalentUpgraded(Hero hero, Talent talent){
		if (talent == Talent.VETERANS_INTUITION || talent == Talent.THIEFS_INTUITION || talent == Talent.SURVIVALISTS_INTUITION){
			for (Item item : hero.belongings){
				onItemCollected(hero, item);
			}
		}
	}

	// ===== 조용한 걸음 =====

	//잠든 적이 깰 확률
	public static float wakeChance(Char waker, float chance){
		if (waker instanceof Hero){
			chance *= 1f - T1_SILENT_STEPS[pts((Hero)waker, Talent.SILENT_STEPS)];
		}
		return chance;
	}

	// ===== 격투술 =====

	//활 근접 평타 피해 + (Talent.onAttackProc)
	public static int brawlBonus(Hero hero, Char enemy){
		int lvl = pts(hero, Talent.FOLLOWUP_STRIKE);
		if (lvl < 1 || WornShortbow.equipped(hero) == null || WornShortbow.isShooting()) return 0;
		if (!Dungeon.level.adjacent(hero.pos, enemy.pos)) return 0;
		return T1_BRAWL_DMG[lvl];
	}

	// ===== 숲의 감각 =====

	public static int searchBonus(Hero hero){
		return T1_SENSES_SEARCH[pts(hero, Talent.HEIGHTENED_SENSES)];
	}

	//2레벨: 이 거리 안의 가장 가까운 적 1마리 (없으면 null)
	public static Mob trackedEnemy(Hero hero){
		if (pts(hero, Talent.HEIGHTENED_SENSES) < 2) return null;
		Mob best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob mob : Dungeon.level.mobs){
			if (mob.alignment != Char.Alignment.ENEMY || Char.hasProp(mob, Char.Property.OBJECT)) continue;
			int d = Dungeon.level.distance(hero.pos, mob.pos);
			if (d <= T1_SENSES_TRACK && d < bestDist){
				bestDist = d;
				best = mob;
			}
		}
		return best;
	}
}
