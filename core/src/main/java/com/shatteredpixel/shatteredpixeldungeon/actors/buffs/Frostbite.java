/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//Maze Tower: 혹한 (명세 16번). 서리 화살(마법사 지팡이 서리 합성 스킬)이 건다.
//냉기(Chill)와 같은 효과(느려짐)이고 원소 반응에서도 냉기로 판정된다.
//하지만 원소 반응으로는 절대 사라지지 않고 지속시간 동안 남는다(빙결된 뒤에도, 증기가 터져도).
//그래서 혹한이 남아 있는 동안 불이 붙을 때마다 증기가 다시 일어날 수 있다(의도).
//원작 냉기와 다른 클래스라서, 반응이 냉기를 지울 때(Buff.detach(Chill.class)) 함께 지워지지 않는다.
public class Frostbite extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public boolean attachTo( Char target ) {
		//냉기에 면역이면 혹한도 걸리지 않는다
		if (target.isImmune(Chill.class)) return false;
		//붙는 순간 냉기처럼 반응한다 (화상 → 증기, 젖음 → 빙결). 혹한 자체는 그래도 붙는다.
		ElementalReactions.onFrostbite(target);
		return super.attachTo(target);
	}

	//냉기와 같은 느려짐: 남은 1턴마다 10%, 최대 50%
	public float speedFactor(){
		return Math.max(0.5f, 1 - cooldown()*0.1f);
	}

	@Override
	public int icon() {
		return BuffIndicator.FROST;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.35f, 0.6f, 1f); //냉기보다 짙은 푸른빛
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (com.shatteredpixel.shatteredpixeldungeon.MazeTower.FROSTBITE_TURNS - visualcooldown())
				/ com.shatteredpixel.shatteredpixeldungeon.MazeTower.FROSTBITE_TURNS);
	}

	@Override
	public void fx(boolean on) {
		if (on) target.sprite.add(CharSprite.State.CHILLED);
		else if (target.buff(Chill.class) == null) target.sprite.remove(CharSprite.State.CHILLED);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns(), Messages.decimalFormat("#.##", (1f-speedFactor())*100f));
	}
}
