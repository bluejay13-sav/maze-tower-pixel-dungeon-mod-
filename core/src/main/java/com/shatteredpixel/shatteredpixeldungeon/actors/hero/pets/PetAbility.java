/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (T1, 지시서 claude_code_tasks_1126b_pets): 4차 = 펫. 결실의 핵으로 고른 펫이 그 판의 갑옷 능력(hero.armorAbility)이 된다.
//능력 단추: 펫이 없으면 충전을 써서 옆 칸에 부르고, 나와 있으면 칸이나 적을 찍어 지시한다(충전 안 씀).
//펫은 죽지 않는다: 체력이 0이 되면 물러나고(사라지고), 다시 부르면 체력이 가득 찬 채로 온다. 강화(4차 포인트)는 주인공에게 남는다.
public abstract class PetAbility extends ArmorAbility {

	{
		baseChargeUse = MazeTower.PET_SUMMON_CHARGE;
	}

	//새 펫 하나
	protected abstract PetAlly create();

	//지금 나와 있는 펫 (없으면 null). 펫은 한 번에 한 마리.
	public static PetAlly current(){
		for (Char ch : Actor.chars()){
			if (ch instanceof PetAlly && ch.isAlive()) return (PetAlly) ch;
		}
		return null;
	}

	@Override
	public String targetingPrompt() {
		return current() == null ? null : Messages.get(PetAbility.class, "prompt");
	}

	@Override
	public boolean useTargeting(){
		return false;
	}

	@Override
	public float chargeUse( Hero hero ) {
		return current() == null ? super.chargeUse(hero) : 0;
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ) {
		PetAlly pet = current();
		if (pet != null){
			if (target != null) pet.direct(target);
			return;
		}

		PetAlly ally = create();
		ArrayList<Integer> spawnPoints = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int p = hero.pos + n;
			if (Actor.findChar(p) == null && ally.canStandAt(p)) spawnPoints.add(p);
		}
		//옆 칸이 모두 막혀 있으면 부르지 못하고 충전도 쓰지 않는다
		if (spawnPoints.isEmpty()){
			GLog.w(Messages.get(PetAbility.class, "no_space"));
			return;
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		ally.pos = Random.element(spawnPoints);
		GameScene.add(ally);
		ScrollOfTeleportation.appear(ally, ally.pos);
		ally.onSummoned();
		Dungeon.observe();

		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(PetAbility.class, "common")
				+ "\n\n" + Messages.get(PetAbility.class, "cost", (int)baseChargeUse);
	}
}
