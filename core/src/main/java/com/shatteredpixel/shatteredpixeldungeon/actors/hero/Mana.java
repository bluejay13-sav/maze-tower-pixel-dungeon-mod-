/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandMana;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
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
		int max = MazeTower.MANA_MAX;
		if (target instanceof Hero) max += BloomEffects.bonusMaxMana((Hero)target); //마나 감응
		if (target instanceof Hero) max += SproutEffects.mealManaBonus((Hero)target, this); //1차 특성: 푸짐한 식사·마력이 깃든 식사
		return max;
	}

	//집중: 마지막 행동이 대기·탐색이었는지 (다른 행동을 하면 Hero.act에서 꺼진다)
	public boolean focusReady = false;

	//1차 특성: 특성을 찍은 뒤 먹은 횟수 (영구, 저장됨). 오르는 최대 마나는 현재 특성 레벨로 매번 다시 계산한다
	//(1레벨에 먹고 나중에 2레벨을 찍어도 손해가 없다)
	public int heartyMeals = 0;
	public int empowerMeals = 0;
	//마나 순환: 마나가 가득 찬 채로 이어서 대기한 턴
	public float cycleTurns = 0;

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
		Mana m = get(hero);
		if (m.isFull()) BloomCommon.onWaitAtFull(hero); //명상 각성: 가득 찬 마나로 대기
		SproutEffects.manaCycle(hero, m, turns); //마나 순환 (회복 전에 가득 찼는지 본다)
		//명상: 대기 시 마나 회복 배율
		m.gain(Math.round(MazeTower.MANA_PER_WAIT_TURN * turns * BloomEffects.waitManaMulti(hero)));
		m.focusReady = true;
		Buff.detach(hero, BloomBuffs.ChargeReady.class); //돌진: 제자리에서 기다리면 이동한 기세가 사라진다
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
		//마법사 지팡이: 합성된 마법봉을 쏘는 것이 스킬
		if (w instanceof MagesStaff && ((MagesStaff)w).imbuedWand() != null) return (MeleeWeapon)w;
		return null;
	}

	private static Wand staffWand(MeleeWeapon w){
		return w instanceof MagesStaff ? ((MagesStaff)w).imbuedWand() : null;
	}

	//장착한 검이 바뀌었으면 버튼을 갱신한다. 상태창(StatusPane)이 매 프레임 호출한다.
	public void refreshSkillButton(){
		MeleeWeapon w = skillWeapon();
		if (w == null){
			lastWeapon = null;
			ActionIndicator.clearAction(this);
		} else if (ActionIndicator.action == null
				|| (ActionIndicator.action == this && lastWeapon != w)){
			lastWeapon = w;
			ActionIndicator.setAction(this);
		} else if (ActionIndicator.action == this && canAfford() != lastAffordable){
			//마나가 비용보다 많아지거나 적어지면 버튼을 다시 그린다 (회색 / 원래 색)
			ActionIndicator.refresh();
		}
		lastAffordable = canAfford();
	}
	private MeleeWeapon lastWeapon;
	private boolean lastAffordable = true;

	private boolean canAfford(){
		if (!(target instanceof Hero)) return true;
		if (cur() < SwordSkill.manaCost((Hero)target)) return false;
		//조준 사격은 화살도 1발 있어야 한다
		if (skillWeapon() instanceof WornShortbow && Arrow.count((Hero)target) <= 0) return false;
		//지팡이 스킬은 충전도 1 있어야 한다
		Wand sw = staffWand(skillWeapon());
		return sw == null || sw.curCharges >= 1;
	}

	@Override
	public String actionName() {
		MeleeWeapon w = skillWeapon();
		if (w == null) return "";
		Wand sw = staffWand(w);
		if (sw != null) return WandMana.skillName(sw);
		return SwordSkill.forWeapon(w).name();
	}

	@Override
	public Visual primaryVisual() {
		MeleeWeapon w = skillWeapon();
		ItemSprite ico = w == null ? new ItemSprite() : new ItemSprite(w);
		//마나가 부족하면 회색으로
		if (!canAfford()) ico.tint(0x808080, 0.75f);
		return ico;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		txt.text(Integer.toString(SwordSkill.manaCost((Hero)target)));
		txt.hardlight(canAfford() ? 0x88BBFF : 0xFF6666); //부족하면 빨간 비용
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
		if (w == null) return;
		if (w instanceof MagesStaff) w.execute((Hero)target, MagesStaff.AC_ZAP); //지팡이: 합성된 마법봉 발사
		else w.execute((Hero)target, MeleeWeapon.AC_ABILITY);
	}

	private static final String MANA = "mana";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(MANA, mana);
		bundle.put("hearty_meals", heartyMeals);
		bundle.put("empower_meals", empowerMeals);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		mana = bundle.getInt(MANA);
		heartyMeals = bundle.getInt("hearty_meals");
		empowerMeals = bundle.getInt("empower_meals");
	}
}
