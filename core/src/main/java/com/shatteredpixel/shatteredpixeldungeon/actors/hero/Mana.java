/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;

//Maze Tower: 주인공의 마나. 영웅에게 붙는 보이지 않는 버프로 저장·불러오기를 원작 방식 그대로 처리한다.
//장착한 검에 스킬이 있으면 화면 오른쪽 아래 행동 버튼으로 스킬을 쓸 수 있게 한다.
public class Mana extends Buff implements ActionIndicator.Action {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private int mana;

	//영웅의 마나를 가져온다. 없으면 가득 찬 상태로 만든다.
	public static Mana get( Char ch ){
		Mana m = ch.buff(Mana.class);
		if (m == null){
			m = Buff.affect(ch, Mana.class);
			m.mana = m.max();
		}
		return m;
	}

	public int max(){
		return MazeTower.MANA_MAX;
	}

	public int cur(){
		return Math.min(mana, max());
	}

	public boolean isFull(){
		return cur() >= max();
	}

	public void gain( int amount ){
		mana = Math.max(0, Math.min(max(), cur() + amount));
	}

	//마나가 충분하면 소모하고 true, 부족하면 소모하지 않고 false
	public boolean spend( int amount ){
		if (cur() < amount) return false;
		mana = cur() - amount;
		return true;
	}

	public void fill(){
		mana = max();
	}

	//대기(휴식·탐색 포함)로 턴을 넘겼을 때 호출한다
	public static void onWait( Hero hero, float turns ){
		get(hero).gain(Math.round(MazeTower.MANA_PER_WAIT_TURN * turns));
	}

	// ===== 스킬 버튼 (화면 오른쪽 아래 행동 버튼) =====

	@Override
	public boolean act() {
		refreshSkillButton();
		spend(TICK);
		return true;
	}

	@Override
	public void fx(boolean on) {
		if (on) refreshSkillButton();
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	private MeleeWeapon skillWeapon(){
		if (!(target instanceof Hero)) return null;
		Hero hero = (Hero)target;
		if (!SwordSkill.heroUsesSkills(hero)) return null;
		KindOfWeapon w = hero.belongings.weapon();
		if (w instanceof MeleeWeapon && SwordSkill.forWeapon(w) != null) return (MeleeWeapon)w;
		return null;
	}

	public void refreshSkillButton(){
		MeleeWeapon w = skillWeapon();
		if (w == null){
			ActionIndicator.clearAction(this);
		} else if (ActionIndicator.action != this || lastWeapon != w){
			lastWeapon = w;
			ActionIndicator.setAction(this);
		}
	}
	private MeleeWeapon lastWeapon;

	@Override
	public String actionName() {
		MeleeWeapon w = skillWeapon();
		return w == null ? "" : SwordSkill.forWeapon(w).name();
	}

	@Override
	public Visual primaryVisual() {
		MeleeWeapon w = skillWeapon();
		return w == null ? new ItemSprite() : new ItemSprite(w);
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		txt.text(Integer.toString(SwordSkill.manaCost((Hero)target)));
		txt.hardlight(0x88BBFF);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		return 0x2255CC;
	}

	@Override
	public void doAction() {
		MeleeWeapon w = skillWeapon();
		if (w != null) w.execute((Hero)target, MeleeWeapon.AC_ABILITY);
	}

	private static final String MANA = "mana";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(MANA, mana);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		mana = bundle.getInt(MANA);
	}
}
