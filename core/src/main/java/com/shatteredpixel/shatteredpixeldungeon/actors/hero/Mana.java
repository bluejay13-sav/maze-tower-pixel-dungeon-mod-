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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
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
		if (target instanceof Hero) max += BloomEffects.bonusMaxMana((Hero)target); //2차 특성 자리 (지금 없음)
		if (target instanceof Hero) max += SproutEffects.mealManaBonus((Hero)target, this); //1차 특성: 푸짐한 식사·마력이 깃든 식사
		return max;
	}

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
		//Maze Tower (C13): 가득 찬 뒤 넘치는 마나 (넘치는 잔)
		int overflow = amount > 0 ? cur() + amount - max() : 0;
		mana = Math.max(0, Math.min(max(), cur() + amount));
		if (overflow > 0 && target instanceof Hero) BloomEffects.onManaOverflow((Hero)target, overflow);
	}

	//마나가 충분하면 소모하고 true, 부족하면 소모하지 않고 false
	public boolean spend( int amount ){
		if (cur() < amount) return false;
		mana = cur() - amount;
		return true;
	}

	//(A1) 이번 턴에 마나를 써서 공격했는지. 켜져 있으면 이번 턴의 마나 회복을 건너뛴다 (저장됨)
	public boolean skillThisTurn = false;

	//(A1) 마나를 써서 공격할 때(공격형 스킬, 장전·집중, 지팡이 스킬, 반지 스킬) 쓴다. 이번 턴 회복을 막는다
	public boolean spendForSkill( int amount ){
		if (!spend(amount)) return false;
		if (amount > 0) skillThisTurn = true;
		if (amount > 0) com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Succubus.mazeOnHeroSkill(amount); //Maze Tower (N2): 검은 잎의 시녀가 그 마나를 받아먹는다
		//Maze Tower (I7): 10층 간수의 "스킬을 쓰지 마라" 선고 (스킬은 그대로 나간다)
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.violate(target, com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.SKILL);
		return true;
	}

	//(A1) 턴이 흐를 때마다 마나가 찬다. 이번 턴에 마나를 써서 공격했으면 건너뛴다
	private void turnRegen(){
		if (!(target instanceof Hero) || !target.isAlive()) return;
		//Maze Tower (N1): 검은 낙엽 위에서는 마나가 차지 않고, 새잎 땅 위에서는 더 찬다 (아이콘은 그 위에 있는 동안만)
		boolean withered = com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.blocks(target);
		boolean fresh = !withered && com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.boosts(target);
		if (withered) Buff.prolong(target, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.Withered.class, 1.5f);
		else Buff.detach(target, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.Withered.class);
		if (fresh) Buff.prolong(target, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.Fresh.class, 1.5f);
		else Buff.detach(target, com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.Fresh.class);
		if (skillThisTurn){
			skillThisTurn = false;
			return;
		}
		//Maze Tower (J6): 수정 실에 걸려 있는 동안은 마나가 차지 않는다 (광맥 옆이어도)
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Web.CrystalThread.blocks(target)) return;
		if (withered) return; //Maze Tower (N1)
		gain(MazeTower.A1_MANA_PER_TURN);
		if (fresh) gain(MazeTower.N1_SPROUT_MANA); //Maze Tower (N1)
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GroundTremor.ensure((Hero) target); //Maze Tower (J9): 13·14층이면 땅울림을 붙인다
		//Maze Tower (J2): 살아 있는 광맥 옆 칸이면 조금 더 찬다 (광맥 수와 상관없이 한 번)
		if (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LivingVein.besideHero((Hero) target)){
			gain(MazeTower.J2_VEIN_MANA);
		}
	}

	public void fill(){
		mana = max();
	}

	//대기(휴식·탐색 포함)로 턴을 넘겼을 때 호출한다
	//(A1) 기본 마나 회복은 턴마다 act()에서 한다. 여기서는 대기의 덤(A1_MANA_WAIT_BONUS, 지금 0)만 준다
	public static void onWait( Hero hero, float turns ){
		//Maze Tower: 대기·휴식·탐색은 원작이 spendConstant로 시간을 써서 Hero.spend를 지나지 않는다.
		//여기서 2차 특성의 "턴을 썼다"(벼림·숨 고르기·받아치기)와 "대기했다"(발도)를 알린다.
		BloomEffects.onHeroWait(hero, turns);
		Mana m = get(hero);
		SproutEffects.manaCycle(hero, m, turns); //1차 특성 마나 순환 (회복 전에 가득 찼는지 본다)
		m.gain(Math.round(MazeTower.A1_MANA_WAIT_BONUS * turns * BloomEffects.waitManaMulti(hero)));
	}

	// ===== 스킬 버튼 (화면 오른쪽 아래 행동 버튼) =====

	//Buff는 같은 시각이면 주인공보다 늦게 움직이므로, 그 턴의 주인공 행동이 끝난 뒤에 회복을 판정한다
	@Override
	public boolean act() {
		turnRegen(); //(A1)
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
		DuelMaster.Button.INSTANCE.refresh(); //Maze Tower (S4): 결투가의 결투 걸기 단추
		BattleHunter.BashButton.INSTANCE.refresh(); //Maze Tower (S6): 전투 사냥꾼의 활대 타격 단추
		Object w = skillWeapon();
		if (w == null){
			lastWeapon = null;
			ActionIndicator.clearAction(this);
		} else if (ActionIndicator.action == null
				|| (ActionIndicator.action instanceof Mana && ActionIndicator.action != this) //Maze Tower (사용자): 예전 주인공(불러오기 전 등)의 마나 버튼이 남아 있으면 바꾼다 — 그 주인공의 옛 자리에서 사거리를 재어 "닿지 않을 것"이 뜨던 문제
				|| (ActionIndicator.action == this && lastWeapon != w)){
			lastWeapon = w;
			ActionIndicator.setAction(this);
		} else if (ActionIndicator.action == this && (canAfford() != lastAffordable || !java.util.Objects.equals(dischargeLabel(), lastLabel))){
			//마나가 비용보다 많아지거나 적어지면 버튼을 다시 그린다 (회색 / 원래 색)
			//Maze Tower (W2): 공명검 방출은 마나가 변할 때마다 배율 숫자를 다시 그린다
			ActionIndicator.refresh();
		}
		lastAffordable = canAfford();
		lastLabel = dischargeLabel();
	}
	private Object lastWeapon;
	private boolean lastAffordable = true;
	private String lastLabel = null;

	//Maze Tower (W2): 공명검 방출 버튼에 보일 배율 (예: "275%"). 공명검이 아니면 null
	private String dischargeLabel(){
		if (!(target instanceof Hero) || skillWeapon() == null) return null;
		return SwordSkill.dischargeLabel((Hero)target);
	}

	private boolean canAfford(){
		if (!(target instanceof Hero)) return true;
		if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor.skillsBlocked(target)) return false; //Maze Tower (J1): 마력 증기 - 버튼이 회색
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
		if (dischargeLabel() != null) return SwordSkill.forWeapon(w).name() + " " + dischargeLabel(); //Maze Tower (W2): "방출 275%"
		return SwordSkill.forWeapon(w).name();
	}

	@Override
	public Visual primaryVisual() {
		Item src = skillWeapon();
		ItemSprite ico = src == null ? new ItemSprite() : new ItemSprite(src);
		//마나가 부족하면 회색으로
		if (!canAfford()) ico.tint(0x808080, 0.75f);
		return ico;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		String dl = dischargeLabel();
		txt.text(dl != null ? dl : Integer.toString(SwordSkill.manaCost((Hero)target))); //Maze Tower (W2): 방출은 마나 대신 지금 배율
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
		bundle.put("skill_this_turn", skillThisTurn);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		mana = bundle.getInt(MANA);
		heartyMeals = bundle.getInt("hearty_meals");
		empowerMeals = bundle.getInt("empower_meals");
		skillThisTurn = bundle.getBoolean("skill_this_turn");
	}
}
