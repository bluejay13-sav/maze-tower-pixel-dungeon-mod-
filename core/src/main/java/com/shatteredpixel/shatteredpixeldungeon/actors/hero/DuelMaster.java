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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.FinisherIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;

//Maze Tower (지시서 1127, S4): 3차 전직 결투가 (전사, 무기 무관). 적 하나를 지목해 그 적에게 강해지고 다른 적에게는 약해진다.
// 결투 걸기: 단추(필살기 단추 자리, FinisherIndicator)를 눌러 보이는 적 하나를 찍는다. 턴을 쓰지 않는다.
//           고정 몹(Mob.mazeFixed)·중립·아군에게는 걸 수 없고, 결투 중이거나 대기 중이면 걸 수 없다.
// 결투 중: 결투 상대에게 주는 피해 +15%, 다른 적에게 주는 피해 −15% (상대가 보스면 둘 다 7%). 고정 몹에게 주는 피해는 줄지 않는다.
//         주인공이 직접 주는 피해만(평타·스킬·스침·던지기, 피해 출처가 주인공). 마법봉·물약·파생 피해는 아니다.
// 시간: 15턴. 상대를 쓰러뜨리거나 시간이 다 되면 가장 가까운 보이는 적에게 넘어간다(다시 15턴). 넘어갈 적이 없으면 끝나고
//       다시 걸기까지 10턴(이겨서 끝났으면 5턴). 시간 안에 쓰러뜨리면 체력 6%·마나 10을 돌려받는다.
//       도중에 풀거나 상대를 바꿀 수 없다. 층을 옮기면 끝나고 10턴 대기. 상대가 보이지 않아도 시간은 흐른다.
public class DuelMaster {

	public static boolean is( Hero hero ){
		return hero != null && hero.subClass == HeroSubClass.DUELMASTER;
	}

	static int lvl( Hero hero, Talent t ){
		return hero == null ? 0 : Math.min( MazeTower.T3_MAX_TALENT_LEVEL, hero.pointsInTalent( t ) );
	}

	//결투 상대에게 더 주는 피해 = 다른 적에게 덜 주는 피해 (S5 정면 승부가 키운다)
	public static float bonus( Hero hero, boolean boss ){
		int l = lvl( hero, Talent.HEAD_ON );
		return boss ? MazeTower.T3B_DUEL_BONUS_BOSS[l] : MazeTower.T3B_DUEL_BONUS[l];
	}

	//결투가 이어지는 턴 (S5 긴 승부가 늘린다)
	public static int duelTurns( Hero hero ){
		return MazeTower.T3B_DUEL_TURNS + MazeTower.T3B_LONG_DUEL[lvl( hero, Talent.LONG_DUEL )];
	}

	//결투를 걸 수 있는 적인가 (보이는, 살아 있는 적. 고정 몹·중립·아군·투명은 아님)
	static boolean duelable( Hero hero, Char ch ){
		if (!(ch instanceof Mob) || ch == hero || !ch.isAlive()) return false;
		if (ch.alignment != Char.Alignment.ENEMY || ((Mob) ch).mazeFixed()) return false;
		return ch.invisible <= 0 && Dungeon.level.heroFOV[ch.pos];
	}

	public static Char target( Hero hero ){
		Duel d = hero == null ? null : hero.buff( Duel.class );
		if (d == null) return null;
		Actor a = Actor.findById( d.targetId );
		return a instanceof Char ? (Char) a : null;
	}

	// ----- 결투 걸기 -----
	static void start( Hero hero, Char ch ){
		Duel d = Buff.affect( hero, Duel.class );
		d.targetId = ch.id();
		d.left = duelTurns( hero );
		d.boss = Char.hasProp( ch, Char.Property.BOSS ) || Char.hasProp( ch, Char.Property.MINIBOSS );
		d.depth = Dungeon.depth;
		d.branch = Dungeon.branch;
		d.delayFirst(); //거는 순간은 턴이 아니므로, 남은 턴은 다음 턴부터 센다
		Buff.affect( ch, DuelMark.class );
		if (ch.sprite != null) ch.sprite.showStatus( CharSprite.WARNING, Messages.get( DuelMaster.class, "duel" ) );
		BloomRules.debug( "duel -> " + ch.name() + " (" + (int) d.left + " turns" + (d.boss ? ", boss" : "") + ")" );
		Button.INSTANCE.dirty = true;
	}

	//결투가 끝나 다음 적으로 넘어간다. 넘어갈 적이 없으면 끝나고 대기 (이겼으면 5턴, 아니면 10턴)
	static void transfer( Hero hero, Char from, boolean win ){
		Duel d = hero.buff( Duel.class );
		if (from != null){
			DuelMark m = from.buff( DuelMark.class );
			if (m != null) m.detach();
		}
		if (d != null) d.detach();
		Char next = null;
		int best = Integer.MAX_VALUE;
		for (Mob m : Dungeon.level.mobs){
			if (m == from || !duelable( hero, m )) continue;
			int dist = Dungeon.level.distance( hero.pos, m.pos );
			if (dist < best){ best = dist; next = m; }
		}
		if (next != null){
			start( hero, next );
		} else {
			cooldown( hero, win ? MazeTower.T3B_DUEL_COOLDOWN_WIN : MazeTower.T3B_DUEL_COOLDOWN );
			BloomRules.debug( "duel ended, cooldown " + (win ? MazeTower.T3B_DUEL_COOLDOWN_WIN : MazeTower.T3B_DUEL_COOLDOWN) );
		}
		Button.INSTANCE.dirty = true;
	}

	//층을 옮기거나 하여 결투가 그냥 끝난다 (10턴 대기).
	//(사용자) 예전 상대에 붙은 결투 표식은 그 층에 남아 있다가, 그 층에 다시 오면 스스로 사라진다 (DuelMark.act·stillMarked)
	static void end( Hero hero ){
		Duel d = hero.buff( Duel.class );
		if (d != null) d.detach();
		cooldown( hero, MazeTower.T3B_DUEL_COOLDOWN );
		Button.INSTANCE.dirty = true;
	}

	//다시 걸기 대기 (아이콘이 늘 가득 찬 채 시작하게 처음 길이를 기억한다)
	static void cooldown( Hero hero, float turns ){
		DuelCooldown c = Buff.affect( hero, DuelCooldown.class, turns );
		c.full = turns;
	}

	// ----- 주인공이 준 피해 (Char.damage ← BloomEffects.incomingDamage) -----
	public static int dealt( Char target, int dmg, Object src ){
		if (!(src instanceof Hero) || dmg <= 0) return dmg;
		Hero hero = (Hero) src;
		if (!is( hero ) || target == hero || target.alignment == Char.Alignment.ALLY) return dmg;
		Duel d = hero.buff( Duel.class );
		if (d == null) return dmg;
		float b = bonus( hero, d.boss );
		if (target.id() == d.targetId) return Math.round( dmg * (1f + b) );
		if (target instanceof Mob && ((Mob) target).mazeFixed()) return dmg; //고정 몹에게 주는 피해는 줄지 않는다
		return Math.round( dmg * (1f - b) );
	}

	// ----- (S5) 맞선 눈: 결투 상대에게 받는 피해 −6/12/18/24% (Hero.damage ← BloomEffects.onHeroDamage). 결투 상대의 공격·기술에만 -----
	public static int taken( Hero hero, int dmg, Object src ){
		if (!is( hero ) || dmg <= 0 || !(src instanceof Char)) return dmg;
		Duel d = hero.buff( Duel.class );
		if (d == null || ((Char) src).id() != d.targetId) return dmg;
		int l = lvl( hero, Talent.LOCKED_EYES );
		if (l <= 0) return dmg;
		return Math.round( dmg * (1f - MazeTower.T3B_LOCKED_EYES[l]) );
	}

	// ----- 결투 상대가 쓰러졌다 (Mob.die) -----
	public static void onMobDie( Mob mob ){
		Hero hero = Dungeon.hero;
		if (!is( hero ) || !hero.isAlive()) return;
		Duel d = hero.buff( Duel.class );
		if (d == null || d.targetId != mob.id()) return;
		//승리: 체력·마나를 조금 돌려받는다 (S5 승자의 숨이 늘린다)
		int hl = lvl( hero, Talent.VICTOR_BREATH );
		int heal = Math.round( hero.HT * (MazeTower.T3B_DUEL_WIN_HP + MazeTower.T3B_VICTOR_HP[hl]) );
		heal = Math.min( heal, hero.HT - hero.HP );
		if (heal > 0){
			hero.HP += heal;
			hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
		}
		int mana = MazeTower.T3B_DUEL_WIN_MANA + MazeTower.T3B_VICTOR_MANA[hl];
		Mana.get( hero ).gain( mana );
		GLog.p( Messages.get( DuelMaster.class, "win" ) );
		BloomRules.debug( "duel won: +" + heal + " hp, +" + mana + " mana" );
		transfer( hero, mob, true );
	}

	// ----- 주인공의 결투 상태 (남은 턴, 저장됨) -----
	public static class Duel extends Buff {

		{
			type = buffType.POSITIVE;
			actPriority = HERO_PRIO - 1; //주인공 다음에 시간을 센다
		}

		public int targetId = -1;
		public float left = 0;
		public boolean boss = false;
		public int depth = -1, branch = 0;

		void delayFirst(){
			spend( TICK );
		}

		@Override
		public boolean act() {
			Hero hero = target instanceof Hero ? (Hero) target : null;
			if (hero == null || !is( hero )){ detach(); return true; }
			if (depth != Dungeon.depth || branch != Dungeon.branch){
				end( hero ); //층을 옮기면 결투가 끝난다
				return true;
			}
			left -= TICK;
			Char t = DuelMaster.target( hero );
			if (t == null || !t.isAlive()){
				transfer( hero, t, false ); //상대가 사라졌다 (쓰러짐은 onMobDie가 먼저 처리)
				return true;
			}
			if (left <= 0){
				transfer( hero, t, false ); //시간이 다 됐다 (보상 없음)
				return true;
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 1f, 0.35f, 0.3f );
		}

		@Override
		public float iconFadePercent() {
			Hero h = target instanceof Hero ? (Hero) target : null;
			return Math.max( 0, 1f - left / Math.max( 1, duelTurns( h ) ) );
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString( (int) Math.ceil( left ) );
		}

		@Override
		public String name() {
			return Messages.get( this, "name" );
		}

		@Override
		public String desc() {
			Hero h = target instanceof Hero ? (Hero) target : null;
			Char t = DuelMaster.target( h );
			int b = Math.round( bonus( h, boss ) * 100 );
			return Messages.get( this, "desc", t == null ? "?" : t.name(), b, b, (int) Math.ceil( left ) );
		}

		private static final String TARGET = "target", LEFT = "left", BOSS = "boss", DEPTH = "depth", BRANCH = "branch";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( TARGET, targetId );
			bundle.put( LEFT, left );
			bundle.put( BOSS, boss );
			bundle.put( DEPTH, depth );
			bundle.put( BRANCH, branch );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			targetId = bundle.getInt( TARGET );
			left = bundle.getFloat( LEFT );
			boss = bundle.getBoolean( BOSS );
			depth = bundle.getInt( DEPTH );
			branch = bundle.getInt( BRANCH );
		}
	}

	//다시 결투를 걸 수 있기까지
	public static class DuelCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}
		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}
		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.5f, 0.5f, 0.5f );
		}
		public float full = MazeTower.T3B_DUEL_COOLDOWN; //(점검) 이번 대기의 처음 길이 (이겨서 끝나면 5턴)
		@Override
		public float iconFadePercent() {
			float f = Math.max( 1f, full );
			return Math.max( 0, (f - visualcooldown()) / f );
		}
		private static final String FULL = "full";
		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( FULL, full );
		}
		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			if (bundle.contains( FULL )) full = bundle.getFloat( FULL );
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
		@Override
		public void detach() {
			super.detach();
			Button.INSTANCE.dirty = true;
		}
	}

	//결투 상대 표시 (머리 위 은은한 표식, 저장됨)
	public static class DuelMark extends Buff {
		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		private Bloom2Fx.MarkOverhead overhead;

		//(사용자) 주인공의 결투가 지금도 이 몹을 가리키는가 (층을 옮겨 결투가 끝났으면 아님)
		boolean stillMarked(){
			Hero h = Dungeon.hero;
			Duel d = h == null ? null : h.buff( Duel.class );
			return target != null && d != null && d.targetId == target.id() && d.depth == Dungeon.depth && d.branch == Dungeon.branch;
		}

		@Override
		public boolean act() {
			if (!stillMarked()){ detach(); return true; }
			spend( TICK );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 1f, 0.35f, 0.3f );
		}

		@Override
		public void fx( boolean on ) {
			if (on){
				if (overhead == null && stillMarked() && target.sprite != null && target.sprite.parent != null){
					//(사용자) 효과 층에 그려 벽·기둥에 가리지 않게 (Bloom2Fx.markOverhead)
					overhead = Bloom2Fx.markOverhead( target.sprite );
					if (overhead != null){
						overhead.stacks( 1 );
						overhead.hardlight( 1f, 0.45f, 0.35f );
						overhead.alpha( 0.8f );
					}
				}
			} else if (overhead != null){
				overhead.killAndErase();
				overhead = null;
			}
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

	// ----- 결투 걸기 단추 (필살기 단추 자리를 빌린다: 결투가는 필살기가 없다) -----
	public static class Button implements ActionIndicator.Action {
		public static final Button INSTANCE = new Button();
		boolean dirty = true;
		private String lastLabel = null;

		//상태창이 매 프레임 부른다 (Mana.refreshSkillButton)
		public void refresh(){
			Hero h = Dungeon.hero;
			if (h == null || !is( h )){
				if (FinisherIndicator.action == this) FinisherIndicator.clearAction( this );
				return;
			}
			if (FinisherIndicator.action != this){
				FinisherIndicator.setAction( this );
				dirty = false;
				lastLabel = label( h );
				return;
			}
			String l = label( h );
			if (dirty || !l.equals( lastLabel )){
				dirty = false;
				lastLabel = l;
				FinisherIndicator.refresh();
			}
		}

		//단추에 적는 숫자: 결투 중이면 남은 턴, 대기 중이면 남은 대기
		private static String label( Hero h ){
			Duel d = h.buff( Duel.class );
			if (d != null) return Integer.toString( (int) Math.ceil( d.left ) );
			DuelCooldown c = h.buff( DuelCooldown.class );
			if (c != null) return Integer.toString( (int) Math.ceil( c.visualcooldown() ) );
			return "";
		}

		private static boolean ready( Hero h ){
			return h.buff( Duel.class ) == null && h.buff( DuelCooldown.class ) == null;
		}

		@Override
		public String actionName() {
			return Messages.get( DuelMaster.class, "action" );
		}

		@Override
		public Visual primaryVisual() {
			Image s = new HeroIcon( HeroSubClass.DUELMASTER );
			if (Dungeon.hero != null && !ready( Dungeon.hero )) s.tint( 0x808080, 0.6f );
			return s;
		}

		@Override
		public Visual secondaryVisual() {
			if (Dungeon.hero == null) return null;
			String l = label( Dungeon.hero );
			if (l.isEmpty()) return null;
			BitmapText t = new BitmapText( PixelScene.pixelFont );
			t.text( l );
			t.hardlight( Dungeon.hero.buff( Duel.class ) != null ? 0xFFCC66 : 0xFF6666 );
			t.measure();
			return t;
		}

		@Override
		public int indicatorColor() {
			return 0xAA3322;
		}

		@Override
		public void doAction() {
			Hero h = Dungeon.hero;
			if (h == null || !is( h )) return;
			if (h.buff( Duel.class ) != null){
				GLog.w( Messages.get( DuelMaster.class, "already" ) );
				return;
			}
			if (h.buff( DuelCooldown.class ) != null){
				GLog.w( Messages.get( DuelMaster.class, "cooldown" ) );
				return;
			}
			GameScene.selectCell( new CellSelector.Listener() {
				@Override
				public void onSelect( Integer cell ) {
					if (cell == null) return;
					Hero hero = Dungeon.hero;
					Char ch = Actor.findChar( cell );
					if (ch == null || !duelable( hero, ch )){
						GLog.w( Messages.get( DuelMaster.class, "bad_target" ) );
						return;
					}
					start( hero, ch ); //턴을 쓰지 않는다
				}
				@Override
				public String prompt() {
					return Messages.get( DuelMaster.class, "prompt" );
				}
			} );
		}
	}
}
