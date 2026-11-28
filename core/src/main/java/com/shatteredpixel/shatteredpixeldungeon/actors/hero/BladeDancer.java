/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

//Maze Tower (지시서 1127, S2): 3차 전직 검무가 (전사, 근접 무기). 이동이 곧 공격이다.
// 스침: 직접 한 칸 걸어 도착하면 둘레 여덟 칸의 적을 모두 스치며 벤다. 평타 판정(명중 굴림, 평타 특성·적중 효과·마법 부여),
//       피해 = 평타 × (30% + 흐름 × 10%), 도착한 뒤의 흐름으로 셈. 턴을 따로 쓰지 않는다. 근접 무기(활 제외)를 들었을 때만.
//       고정 몹(Mob.mazeFixed)과 중립·아군은 스치지 않는다. 잠든 적은 스치고 깨운다.
// 흐름: 걸은 턴마다 +1(최대 5), 걷지 않고 턴을 쓰면 −2. 턴을 쓰지 않는 행동은 바꾸지 않는다.
//       돌진·밀려남·끌려감·순간이동은 걸음이 아니다(그 자체로 턴을 쓰지 않으므로 흐름도 바뀌지 않음).
public class BladeDancer {

	public static boolean is( Hero hero ){
		return hero != null && hero.subClass == HeroSubClass.BLADEDANCER;
	}

	public static int flow( Hero hero ){
		Flow f = hero == null ? null : hero.buff( Flow.class );
		return f == null ? 0 : f.flow;
	}

	//주인공이 턴을 쓸 때 (BloomEffects.onHeroSpend ← Hero.spend, 대기·탐색은 Mana.onWait). moved = 이번 행동이 걸음.
	//돌려주는 값 = 실제로 쓸 시간 (S3 이어지는 걸음이면 0)
	static float onHeroSpend( Hero hero, float time, boolean moved, float spend ){
		if (!is( hero )) return spend;
		FlowStep free = hero.buff( FlowStep.class );
		if (moved){
			//(S3) 이어지는 걸음: 스침으로 쓰러뜨린 다음 한 걸음은 턴을 쓰지 않는다 (그 걸음도 걸음이라 흐름이 쌓이고 스침이 나간다)
			if (free != null){
				free.detach();
				int l = lvl( hero, Talent.FLOWING_STEP );
				if (l > 0) Buff.affect( hero, FlowStepCooldown.class, MazeTower.T3B_FLOWING_STEP_CD[l] );
				spend = 0;
				BloomRules.debug( "bladedancer flowing step: free step" );
			}
			Flow f = Buff.affect( hero, Flow.class );
			f.flow = Math.min( MazeTower.T3B_FLOW_MAX, f.flow + 1 );
			if (graze( hero, f.flow )) grantFlowStep( hero );
			return spend;
		}
		if (free != null && time > 0) free.detach(); //걷지 않고 턴을 쓰면 사라진다
		if (time > 0){
			Flow f = hero.buff( Flow.class );
			if (f != null){
				f.flow -= MazeTower.T3B_FLOW_DECAY;
				if (f.flow <= 0) f.detach();
			}
		}
		return spend;
	}

	//근접 무기(활 제외)를 들었는가
	static boolean holdsMelee( Hero hero ){
		KindOfWeapon w = hero.belongings.attackingWeapon();
		return w instanceof MeleeWeapon && !(w instanceof WornShortbow);
	}

	//스침의 피해 배율 (평타 대비): 30% + 흐름 × (10% + 결 흐름)
	public static float grazeMulti( Hero hero, int flow ){
		return MazeTower.T3B_GRAZE_BASE + flow * perFlow( hero );
	}

	//흐름 하나가 더하는 스침 피해 (S3 결 흐름이 키운다)
	public static float perFlow( Hero hero ){
		int l = hero == null ? 0 : Math.min( MazeTower.T3_MAX_TALENT_LEVEL, hero.pointsInTalent( Talent.FLOW_EDGE ) );
		return MazeTower.T3B_GRAZE_PER_FLOW + MazeTower.T3B_FLOW_EDGE[l];
	}

	//스침: 도착한 칸 둘레 여덟 칸의 적을 모두 벤다 (평타 판정, 턴을 쓰지 않음). 하나라도 쓰러뜨렸으면 true
	private static boolean graze( Hero hero, int flow ){
		boolean killed = false;
		boolean swung = false;
		if (!hero.isAlive() || !holdsMelee( hero )) return false;
		float mult = grazeMulti( hero, flow );
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			Char ch = Actor.findChar( c );
			if (!(ch instanceof Mob) || !ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
			if (((Mob) ch).mazeFixed() || ch.invisible > 0) continue; //(사용자) 도착한 칸 둘레는 직전 시야와 상관없이 모두 벤다
			if (!hero.isAlive()) return killed;
			if (ch.sprite != null && ch.sprite.visible) MazeFx.toward( MazeFx.Fx.SLASH, hero, ch );
			BloomRules.debug( "bladedancer graze -> " + ch.name() + " x" + mult );
			try {
				swung = true;
				BloomEffects.beginNormal( hero );
				boolean h = hero.attack( ch, mult, 0, 1f );
				BloomEffects.afterNormalAttack( hero, ch, h, new Runnable() { @Override public void run() {} } );
				if (!ch.isAlive()) killed = true;
			} finally {
				BloomWarrior.endCounter(); //스침은 턴을 쓰는 걸음 안에서 일어나므로, 평타 한 번 동안의 상태를 비운다
			}
		}
		//(사용자) 투명한 채 스쳐 베면 원작 평타처럼 투명이 풀린다 (한 걸음의 스침들은 모두 투명한 채 들어간다)
		if (swung) com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.dispel();
		return killed;
	}


	static int lvl( Hero hero, Talent t ){
		return hero == null ? 0 : Math.min( MazeTower.T3_MAX_TALENT_LEVEL, hero.pointsInTalent( t ) );
	}

	// ----- (S3) 이어지는 걸음: 스침으로 적을 쓰러뜨리면 다음 한 걸음이 턴을 쓰지 않는다 (다시 켜지기까지 8/6/4/2턴) -----
	private static void grantFlowStep( Hero hero ){
		if (lvl( hero, Talent.FLOWING_STEP ) <= 0) return;
		if (hero.buff( FlowStepCooldown.class ) != null || hero.buff( FlowStep.class ) != null) return;
		Buff.affect( hero, FlowStep.class );
		BloomRules.debug( "bladedancer flowing step: ready" );
	}

	// ----- (S3) 춤추는 발: 흐름이 있는 동안 흐름 하나마다 회피 +3/6/9/12% (Hero.defenseSkill ← BloomEffects.heroEvasionMulti) -----
	public static float evasionMulti( Hero hero ){
		if (!is( hero )) return 1f;
		int l = lvl( hero, Talent.DANCING_FEET );
		int f = flow( hero );
		if (l <= 0 || f <= 0) return 1f;
		return 1f + f * MazeTower.T3B_DANCING_FEET[l];
	}

	// ----- (S3) 끝맺는 칼: 흐름을 가진 채 마나를 쓰는 공격 스킬을 쓰면 흐름을 모두 쓰고, 쓴 흐름 하나마다 그 스킬의 피해 +5/10/15/20% -----
	private static float finishBonus = 0f;

	//검·단검 스킬을 실제로 쓸 때 (SwordSkill.launch, beginSkill 바로 뒤). 마나 없이 이어 쓰거나 강화된 평타면 아님
	public static void onSkillLaunch( Hero hero, boolean spendMana, boolean charged ){
		finishBonus = 0f;
		if (!is( hero ) || !spendMana || charged) return;
		int l = lvl( hero, Talent.FINISHING_BLADE );
		Flow f = hero.buff( Flow.class );
		if (l <= 0 || f == null || f.flow <= 0) return;
		finishBonus = f.flow * MazeTower.T3B_FINISHING_BLADE[l];
		BloomRules.debug( "bladedancer finishing blade: flow " + f.flow + " -> +" + Math.round( finishBonus * 100 ) + "%" );
		f.detach();
	}

	//스킬 타격의 피해 (BloomEffects.processHit, 스킬 판정)
	public static int skillDamage( int damage ){
		return finishBonus > 0 ? Math.round( damage * (1f + finishBonus) ) : damage;
	}

	//스킬이 끝났을 때 (BloomEffects.endSkill)
	public static void endSkill(){
		finishBonus = 0f;
	}

	//다음 한 걸음이 턴을 쓰지 않는다 (이어지는 걸음)
	public static class FlowStep extends Buff {
		{
			type = buffType.POSITIVE;
		}
		@Override
		public int icon() {
			return BuffIndicator.HASTE;
		}
		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.7f, 0.95f, 0.55f ); //새잎빛
		}
		@Override
		public String name() {
			return Messages.get( this, "name" );
		}
		@Override
		public String desc() {
			return Messages.get( this, "desc" );
		}
	}

	//이어지는 걸음이 다시 켜지기까지
	public static class FlowStepCooldown extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {
		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}
		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.5f, 0.5f, 0.5f );
		}
		@Override
		public float iconFadePercent() {
			int l = lvl( Dungeon.hero, Talent.FLOWING_STEP );
			int max = l > 0 ? MazeTower.T3B_FLOWING_STEP_CD[l] : 1;
			return Math.max( 0, (max - visualcooldown()) / max );
		}
		@Override
		public String iconTextDisplay() {
			return Integer.toString( (int) visualcooldown() );
		}
		@Override
		public String name() {
			return Messages.get( this, "name" );
		}
		@Override
		public String desc() {
			return Messages.get( this, "desc", dispTurns() );
		}
	}

	//흐름 (상태 아이콘에 숫자)
	public static class Flow extends Buff {

		{
			type = buffType.POSITIVE;
		}

		public int flow = 0;

		@Override
		public int icon() {
			return BuffIndicator.MOMENTUM;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.7f, 0.95f, 0.55f ); //새잎빛
		}

		@Override
		public float iconFadePercent() {
			return Math.max( 0, 1f - flow / (float) MazeTower.T3B_FLOW_MAX );
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString( flow );
		}

		@Override
		public String name() {
			return Messages.get( this, "name" );
		}

		@Override
		public String desc() {
			Hero hero = target instanceof Hero ? (Hero) target : null;
			return Messages.get( this, "desc", flow, Math.round( grazeMulti( hero, flow ) * 100 ), Math.round( perFlow( hero ) * 100 ) );
		}

		private static final String FLOW = "flow";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( FLOW, flow );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			flow = bundle.getInt( FLOW );
		}
	}
}
