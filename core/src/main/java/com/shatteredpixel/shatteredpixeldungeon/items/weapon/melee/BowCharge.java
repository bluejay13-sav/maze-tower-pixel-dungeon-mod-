/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//Maze Tower: 강화형 활 스킬 (석궁 장전 / 정량궁 집중).
//스킬을 쓰면 1턴을 쓰고 이 상태가 붙는다. 다음 원거리 평타(Hero.actAttack)가 SwordSkill.fireCharged로 강화된 스킬 판정 공격이 된다.
//장전·집중 행동 자체는 공격 스킬이 아니라서 2차 스킬 특성을 받지 않는다 (BloomEffects.beginSkill을 부르지 않음).
public class BowCharge extends Buff {

	public static final int LOAD = 0;  //석궁 장전
	public static final int FOCUS = 1; //정량궁 집중

	{
		type = buffType.POSITIVE;
	}

	public int kind = LOAD;
	public int paid = 0; //낸 마나 (강화된 평타에서 비용 반환 상한으로 쓴다)

	//스킬 버튼으로 장전·집중
	public static void use(Hero hero, MeleeWeapon wep, int kind, int cost){
		if (!(wep instanceof WornShortbow)) return;
		if (Arrow.count(hero) <= 0){
			GLog.w(Messages.get(SwordSkill.class, "no_arrows"));
			ActionIndicator.shake();
			return;
		}
		if (hero.buff(BowCharge.class) != null){
			GLog.w(Messages.get(BowCharge.class, "already"));
			return;
		}
		if (!Mana.get(hero).spend(cost)) return;

		BowCharge c = Buff.affect(hero, BowCharge.class);
		c.kind = kind;
		c.paid = cost;

		if (hero.sprite != null){
			hero.sprite.operate(hero.pos);
			if (kind == LOAD){
				//장전: 쇠붙이 걸리는 소리 + 회색 먼지
				Sample.INSTANCE.play(Assets.Sounds.UNLOCK, 1f, 1.3f);
				com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.H_LOAD, hero.sprite); //(2026-11-21) fx_hunter_03
			} else {
				//집중: 은은한 흰 빛이 모였다 사라짐
				Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.6f, 1.2f);
				com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.onChar(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.H_FOCUS, hero.sprite); //(2026-11-21) fx_hunter_04
			}
		}
		GLog.i(Messages.get(BowCharge.class, kind == LOAD ? "loaded" : "focused"));
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return BuffIndicator.MARK;
	}

	@Override
	public void tintIcon(Image icon) {
		if (kind == LOAD) icon.hardlight(0.6f, 0.6f, 0.6f);
		else icon.hardlight(1f, 0.85f, 0.3f);
	}

	@Override
	public String name() {
		return Messages.get(this, kind == LOAD ? "name_load" : "name_focus");
	}

	@Override
	public String desc() {
		return Messages.get(this, kind == LOAD ? "desc_load" : "desc_focus");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("kind", kind);
		bundle.put("paid", paid);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		kind = bundle.getInt("kind");
		paid = bundle.getInt("paid");
	}
}
