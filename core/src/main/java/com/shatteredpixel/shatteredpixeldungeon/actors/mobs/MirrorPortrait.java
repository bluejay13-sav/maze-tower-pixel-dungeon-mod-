/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTalent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MirrorPortraitSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (L6): 거울 초상. 벽의 초상화에서 걸어 나온 흐릿한 환영 (18층부터, 약함).
//스스로는 공격하지 않고 주인공과 2칸쯤 거리를 두고 따라다닌다. 주인공이 2칸(L6_RANGE) 안에서 공격 스킬을 쓰면, 그때 주인공이 있던 칸에
//경고를 띄우고(Heart 경고와 같은 붉은 칸) 한 박자 뒤(다음 초상 차례가 한 번 지난 뒤) 그 스킬을 따라 쓴다. 그 칸(형태 특성이 넓힌 칸 포함)에
//주인공이 있으면 맞는다((사용자) 주인공이 그 스킬로 실제로 준 피해 그대로). 강화형 스킬(장전·집중으로 쏜 평타)은 따라 쓰지 않는다.
//(사용자) 스킬의 형태가 변화한 것도 따라 한다: 한 번 더(타수), 무거운 일격(밀침), 달군 날(화상), 휘감는 덩굴(속박), 초승달(좌우 칸까지),
//메아리(한 박자 뒤 한 번 더, 약하게). 다른 적에게 튀거나 옮겨 가는 형태(튀는 일격 등)는 주인공만 맞히는 초상에게는 뜻이 없다.
public class MirrorPortrait extends Mob {

	{
		spriteClass = MirrorPortraitSprite.class;

		HP = HT = MazeTower.L6_HP;
		defenseSkill = MazeTower.L6_DEF;
		EXP = 10;
		maxLvl = 26;

		flying = true;
		baseSpeed = MazeTower.L6_SPEED; //(사용자) 조금 느리게
		properties.add(Property.INORGANIC);

		HUNTING = new Hunting();
	}

	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 4 );
	}

	//스스로는 치지 않는다
	@Override
	protected boolean canAttack( Char enemy ) {
		return false;
	}

	// ===== 따라 쓰기 =====
	public String pendingKey = null;     //따라 쓸 스킬 이름 키
	public float pendingMulti = 0;       //(사용자) 따라 쓸 피해 = 주인공이 그 스킬로 실제로 준 피해의 합 (빗나간 타격은 0)
	public boolean collecting = false;   //주인공의 스킬 타격이 끝날 때마다 피해를 모으는 중
	public int pendingHits = 1;
	public int pendingCell = -1;         //주인공이 있던 칸 (가운데)
	public int[] pendingCells = new int[0]; //경고·판정 칸 (가운데 + 초승달이 넓힌 칸)
	public int pendingWait = 0;          //남은 기다림 (0이 되면 다음 차례에 쓴다)
	public int pushDist = 0;             //무거운 일격: 밀치는 칸 수
	public float burnTurns = 0;          //달군 날: 화상
	public float rootTurns = 0;          //휘감는 덩굴: 속박
	public float echoRatio = 0;          //메아리: 한 박자 뒤 이 비율로 한 번 더

	public com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WormSpawn.ChargeWarn warnFx; //불러온 뒤에는 그림이 다시 그린다

	private static int lv( Hero hero, BloomTalent t ){
		int l = BloomTree.level( hero, t );
		return l <= 0 ? -1 : l - 1;
	}

	//(사용자) 주인공의 스킬 타격이 끝날 때마다 (BloomEffects.onSkillHitDone) 그 피해를 모은다. 강화형은 따라 쓰지 않으므로 세지 않는다
	public static void onHeroSkillHit( com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomEffects.SkillHit h ){
		if (h == null || h.charged || Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m instanceof MirrorPortrait && ((MirrorPortrait) m).collecting) ((MirrorPortrait) m).pendingMulti += Math.max( 0, h.damage )
					* (com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill.DISCHARGE.equals( h.skillKey ) ? com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill.dischargeMirrorScale : 1f); //Maze Tower (W2): 방출은 최소치(200%)로 따라 씀
		}
	}

	//SwordSkill.launch에서 부른다 (강화형 제외). 2칸 안에서 주인공을 보고 있는 초상마다 따라 쓸 준비
	public static void onHeroSkill( Hero hero, SwordSkill skill ){
		if (!MazeTower.L6_MIRROR || skill == null || skill.chargedShot || Dungeon.level == null) return;
		if (hero.invisible > 0) return; //은신 존중
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!(m instanceof MirrorPortrait) || !m.isAlive() || m.alignment != Char.Alignment.ENEMY) continue;
			if (m.buff( com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class ) != null) continue;
			if (Dungeon.level.distance( m.pos, hero.pos ) > MazeTower.L6_RANGE) continue;
			if (m.fieldOfView == null || m.fieldOfView.length != Dungeon.level.length() || !m.fieldOfView[hero.pos]) continue;
			((MirrorPortrait) m).prepare( hero, skill.nameKey,  0, skill.hits, hero.pos, true );
			((MirrorPortrait) m).collecting = true;
		}
	}

	private void prepare( Hero hero, String key, float multi, int hits, int cell, boolean withForms ){
		clearWarn();
		pendingKey = key; pendingMulti = multi; pendingHits = Math.max(1, hits);
		pendingCell = cell; pendingWait = 1;
		pushDist = 0; burnTurns = 0; rootTurns = 0; echoRatio = 0;
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add( cell );
		if (withForms){
			int l;
			if ((l = lv( hero, BloomTalent.W_HEAVY )) >= 0)
				pushDist = BloomTree.awakened( hero, BloomTalent.W_HEAVY ) ? MazeTower.C7_HEAVY_PUSH_AWAKE : MazeTower.C7_HEAVY_PUSH;
			if ((l = lv( hero, BloomTalent.W_HEATED_EDGE )) >= 0) burnTurns = MazeTower.C8_BURN_TURNS[l];
			if ((l = lv( hero, BloomTalent.H_BINDING_VINE )) >= 0) rootTurns = MazeTower.D9_ROOT_TURNS[l];
			if ((l = lv( hero, BloomTalent.W_ECHO )) >= 0) echoRatio = MazeTower.C5_ECHO_RATIO[l];
			if (lv( hero, BloomTalent.W_CRESCENT ) >= 0){
				int w = Dungeon.level.width();
				if (BloomTree.awakened( hero, BloomTalent.W_CRESCENT )){
					for (int o : PathFinder.NEIGHBOURS8) cells.add( cell + o );
				} else {
					int dx = Integer.signum( cell % w - pos % w ), dy = Integer.signum( cell / w - pos / w );
					if (dx != 0 || dy != 0){
						int px, py;
						if (dx == 0 || dy == 0){ px = dy; py = dx; } else { px = dx; py = -dy; }
						cells.add( cell + px + py * w );
						cells.add( cell - px - py * w );
					}
				}
			}
		}
		ArrayList<Integer> ok = new ArrayList<>();
		for (int c : cells){
			if (c < 0 || c >= Dungeon.level.length() || c == pos || Dungeon.level.solid[c] || ok.contains( c )) continue;
			ok.add( c );
		}
		pendingCells = new int[ok.size()];
		for (int i = 0; i < ok.size(); i++) pendingCells[i] = ok.get( i );
		showWarn();
		if (sprite != null){
			sprite.turnTo( pos, cell );
			if (sprite.visible) sprite.showStatus( com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.WARNING,
					Messages.get( SwordSkill.class, key ) );
		}
	}

	public void showWarn(){
		if (pendingCell == -1 || pendingCells.length == 0 || warnFx != null) return;
		ArrayList<Integer> l = new ArrayList<>();
		for (int c : pendingCells) l.add( c );
		warnFx = com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WormSpawn.ChargeWarn.show( l );
	}

	private void clearWarn(){
		if (warnFx != null){ warnFx.fadeOut(); warnFx = null; }
	}

	private void clearPending(){
		clearWarn();
		pendingKey = null; pendingCell = -1; pendingCells = new int[0]; pendingWait = 0;
	}

	@Override
	protected boolean act() {
		if (pendingCell != -1 && isAlive()){
			if (pendingWait > 0){
				collecting = false; //주인공의 행동이 끝났다
				pendingWait--;
				spend( TICK );
				return true;
			}
			return mirror();
		}
		return super.act();
	}

	//따라 쓴다: 경고 칸에 그 스킬 그림, 칸 안의 주인공이 맞는다
	private boolean mirror(){
		Hero hero = Dungeon.hero;
		String key = pendingKey;
		float multi = pendingMulti;
		int hits = pendingHits, center = pendingCell;
		int[] cells = pendingCells;
		int push = pushDist; float burn = burnTurns, root = rootTurns, echo = echoRatio;
		collecting = false;
		clearPending();
		spend( attackDelay() );
		if (sprite != null) sprite.turnTo( pos, center );

		boolean inside = false;
		for (int c : cells) if (c == hero.pos) inside = true;

		if (sprite != null && sprite.visible) sprite.attack( center ); //휘두르는 동작만
		if (inside && hero.isAlive()){
			//스킬 그림: 주인공이 맞는 자리에 그 스킬의 타격 그림
			for (int i = 0; i < hits; i++) com.shatteredpixel.shatteredpixeldungeon.effects.SkillEffects.hit( key, this, hero, i );
			int dmg = Math.round( multi ); //(사용자) 주인공이 준 피해를 그대로 (주인공 방어는 적용)
			dmg = Math.max( 0, dmg - hero.drRoll() );
			hero.damage( dmg, this );
			if (!hero.isAlive()){
				Dungeon.fail( this );
				GLog.n( Messages.get( this, "kill" ) );
				return true;
			}
			if (burn > 0) Buff.affect( hero, Burning.class ).reignite( hero, burn );
			if (root > 0) Buff.prolong( hero, Roots.class, root );
			if (push > 0){
				Ballistica path = new Ballistica( pos, hero.pos, Ballistica.MAGIC_BOLT );
				Ballistica traj = new Ballistica( hero.pos, path.path.get( Math.min( path.dist + push, path.path.size() - 1 ) ), Ballistica.MAGIC_BOLT );
				WandOfBlastWave.throwChar( hero, traj, push, false, false, this );
			}
		} else if (sprite != null && sprite.visible && Dungeon.level.heroFOV[center]){
			//빗나감: 빈 칸에 흩어지는 흐릿한 빛
			com.watabou.utils.PointF p = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld( center );
			MazeFx.at( MazeFx.Fx.SLASH, p.x, p.y );
		}
		//메아리: 한 박자 뒤 같은 스킬을 약하게 한 번 더 (주인공이 그때 있는 칸, 2칸 안일 때)
		if (echo > 0 && hero.isAlive() && Dungeon.level.distance( pos, hero.pos ) <= MazeTower.L6_RANGE){
			prepare( hero, key, multi * echo, hits, hero.pos, false );
		}
		return true;
	}

	@Override
	public void die( Object cause ) {
		clearPending();
		super.die( cause );
	}

	//(사용자) 타락·홀림 같은 '우리 편으로 만드는' 상태는 걸리지 않고, "타락시킬 수 없다"는 글과 함께 깨져 쓰러진다
	// (적으로 쓰러지므로 경험치·전리품은 평소처럼. 원작 타락 마법봉은 버프가 붙지 않았으니 따로 보상하지 않는다)
	@Override
	public synchronized boolean add( Buff buff ) {
		if (buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff && alignment == Alignment.ENEMY){
			if (sprite != null) sprite.showStatus( com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.WARNING, Messages.get( this, "no_corrupt" ) );
			GLog.w( Messages.get( this, "no_corrupt" ) );
			if (isAlive()) die( buff );
			return false;
		}
		return super.add( buff );
	}

	//평소: 공격하지 않고 주인공과 2칸 거리를 두고 따라다닌다
	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV && enemy != null){
				target = enemy.pos;
				int d = Dungeon.level.distance( pos, enemy.pos );
				int old = pos;
				if (d < MazeTower.L6_RANGE && getFurther( enemy.pos )){
					spend( 1 / speed() );
					return moveSprite( old, pos );
				} else if (d > MazeTower.L6_RANGE && getCloser( enemy.pos )){
					spend( 1 / speed() );
					return moveSprite( old, pos );
				}
				spend( TICK );
				return true;
			}
			return super.act( enemyInFOV, justAlerted );
		}
	}

	private static final String P_KEY = "mirror_key", P_MULTI = "mirror_multi", P_HITS = "mirror_hits", P_CELL = "mirror_cell",
			P_CELLS = "mirror_cells", P_WAIT = "mirror_wait", P_PUSH = "mirror_push", P_BURN = "mirror_burn", P_ROOT = "mirror_root", P_ECHO = "mirror_echo";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (pendingCell != -1 && pendingKey != null){
			bundle.put( P_KEY, pendingKey ); bundle.put( P_MULTI, pendingMulti ); bundle.put( P_HITS, pendingHits );
			bundle.put( P_CELL, pendingCell ); bundle.put( P_CELLS, pendingCells ); bundle.put( P_WAIT, pendingWait );
			bundle.put( P_PUSH, pushDist ); bundle.put( P_BURN, burnTurns ); bundle.put( P_ROOT, rootTurns ); bundle.put( P_ECHO, echoRatio );
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( P_CELL )){
			pendingKey = bundle.getString( P_KEY ); pendingMulti = bundle.getFloat( P_MULTI ); pendingHits = bundle.getInt( P_HITS );
			pendingCell = bundle.getInt( P_CELL ); pendingCells = bundle.getIntArray( P_CELLS ); pendingWait = bundle.getInt( P_WAIT );
			pushDist = bundle.getInt( P_PUSH ); burnTurns = bundle.getFloat( P_BURN ); rootTurns = bundle.getFloat( P_ROOT ); echoRatio = bundle.getFloat( P_ECHO );
		}
	}
}
