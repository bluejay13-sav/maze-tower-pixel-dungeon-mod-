/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CellDoor;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ChainRing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.EchoKnot;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FruitBranch;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.HeartValve;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ManaVein;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MemoryLamp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ResonanceCrystal;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rubble;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SapDroplet;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SoulCage;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithEyes;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Maze Tower (T1, 지시서 claude_code_tasks_1126b_pets): 모든 펫의 공통 규칙.
//- 시간 제한 없이 쓰러질 때까지 머물고, 층을 옮기면 따라온다(원작 DirectableAlly는 Mob.holdAllies가 어디 있든 데려감).
//- 지시가 없으면 주인공을 따라다니고, 칸이나 적을 찍어 보낼 수 있다(원작 DirectableAlly 방식).
//- 주인공이 펫 칸으로 걸으면 자리를 바꾼다(원작 Mob.interact).
//- 체력이 0이 되면 물러난다(사라짐). 경험치·드랍은 원작 동료와 같게.
//- 보스의 소품(고정 몹)은 공격하지 않는다. 지시해도 안 한다. 보스는 펫을 평타로 노리지 않는다(Mob.chooseEnemy → untargetable).
//- 1·2·3차 특성은 펫에게 적용되지 않는다(펫의 공격은 주인공의 공격이 아님).
public abstract class PetAlly extends DirectableAlly {

	{
		immunities.add(AllyBuff.class);

		//(사용자) 명령이 없으면 주인공 둘레 leash()칸 안을 맴돈다
		WANDERING = new Roam();
		state = WANDERING;
	}

	//명령이 없을 때 주인공에게서 맴도는 거리 (정령 매·이끼 곰·마지막 근위 1칸, 길든 주문 2칸)
	protected int leash(){
		return com.shatteredpixel.shatteredpixeldungeon.MazeTower.PET_LEASH;
	}

	//주인공이 걸어가는 동안 뒤를 따를 칸 (저장하지 않음, 화면 연출용)
	private int lastHeroPos = -1;
	private int trailCell = -1;

	//(사용자) 주인공이 걸어가는 길에 펫이 서 있으면 멈추지 않고 자리를 바꾼다 (Hero.getCloser).
	//움직이지 못하는 펫(속박·기절), 주인공 칸에 들어갈 수 없는 큰 펫은 바꾸지 않는다.
	public static boolean swappable( Char c, Hero hero ){
		if (!(c instanceof PetAlly) || !c.isAlive() || c.rooted || c.paralysed > 0) return false;
		if (c.properties().contains(Char.Property.LARGE) && !Dungeon.level.openSpace[hero.pos]) return false;
		return true;
	}

	//(사용자) 명령이 없으면 주인공 둘레 leash()칸 안에서 가끔 한 칸씩 맴돌고, 멀어지면 다가온다.
	//칸으로 보낸 명령이면 그리로 간다(도착하면 act에서 명령을 지워 다시 맴돈다). 노릴 적이 보이면 싸운다(원작 동료와 같음).
	protected class Roam extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && !movingToDefendPos && (defendingPos == -1 || canAttack(enemy))){
				enemySeen = true;
				notice();
				alerted = true;
				state = HUNTING;
				target = enemy.pos;
				return true;
			}
			enemySeen = false;
			int oldPos = pos;
			Hero hero = Dungeon.hero;
			//(사용자) 주인공이 걸어가는 동안에는 둘레를 맴돌지 않고 뒤를 따른다(가는 길에 끼어들어 이동이 끊기지 않게):
			//주인공이 방금 떠난 칸을 기억해 두고, 멀어지면 그 칸으로 간다
			boolean heroWalking = hero != null && hero.curAction instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction.Move;
			if (hero != null){
				if (lastHeroPos != -1 && lastHeroPos != hero.pos) trailCell = lastHeroPos;
				lastHeroPos = hero.pos;
			}
			int goal = -1;
			if (defendingPos != -1){
				goal = defendingPos;
			} else if (hero != null && Dungeon.level.distance(pos, hero.pos) > leash()){
				goal = hero.pos;
				if (heroWalking && trailCell != -1 && trailCell != pos && Actor.findChar(trailCell) == null
						&& (Dungeon.level.passable[trailCell] || (flying && Dungeon.level.avoid[trailCell]))){
					goal = trailCell;
				}
			} else if (heroWalking){
				//곁에 있으면 그 자리에 머문다
			} else if (hero != null && com.watabou.utils.Random.Int(2) == 0){
				//leash칸 안: 가끔 둘레의 빈 칸(주인공 leash칸 안)으로 한 칸
				java.util.ArrayList<Integer> cand = new java.util.ArrayList<>();
				for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
					int c = pos + n;
					if (c < 0 || c >= Dungeon.level.length() || c == hero.pos || Actor.findChar(c) != null) continue;
					if (!(Dungeon.level.passable[c] || (flying && Dungeon.level.avoid[c]))) continue;
					if (Dungeon.level.distance(c, hero.pos) > leash()) continue;
					cand.add(c);
				}
				if (!cand.isEmpty()) goal = com.watabou.utils.Random.element(cand);
			}
			if (goal != -1 && goal != pos){
				target = goal;
				if (getCloser(goal)){
					spend(1 / speed());
					if (pos == defendingPos) movingToDefendPos = false;
					return moveSprite(oldPos, pos);
				} else if (movingToDefendPos){
					defendingPos = pos;
					movingToDefendPos = false;
				}
			}
			spend(TICK);
			return true;
		}
	}

	//(사용자) 이동 속도: 정령 매는 주인공보다 빠르고(기본 2), 나머지 펫은 주인공과 같다(기본 1).
	//주인공이 신속의 반지 등으로 보통보다 빨라지면 펫도 그 배율만큼 빨라진다(펫이 뒤처지지 않게). 보스전(속도 봉인)에서는 보정하지 않는다.
	//펫 자신에게 걸린 느려짐 등은 그대로 적용된다(super.speed()에 곱함).
	@Override
	public float speed() {
		float s = super.speed();
		Hero hero = Dungeon.hero;
		if (hero != null && hero.isAlive() && hero.sprite != null && !com.shatteredpixel.shatteredpixeldungeon.MazeBosses.bossFight()){
			s *= Math.max(1f, hero.speed());
		}
		return s;
	}

	//(사용자) 칸을 찍어 보낸 펫은 그 칸에 닿으면(또는 더 갈 수 없으면) 따로 명령하지 않아도 다시 주인공을 따라온다
	//(원작 동료는 그 칸을 계속 지킨다)
	@Override
	protected boolean act() {
		if (defendingPos != -1 && !movingToDefendPos) clearDefensingPos();
		return super.act();
	}

	//이 칸에 부를 수 있는지 (날지 않는 펫은 지나갈 수 있는 칸만)
	public boolean canStandAt( int cell ){
		return Dungeon.level.passable[cell] || (flying && Dungeon.level.avoid[cell]);
	}

	//부른 직후 (연출 등)
	public void onSummoned(){
	}

	//펫이 보는 곳이 주인공 시야에 더해지는지 (정령 매)
	public boolean sharesVision(){
		return false;
	}

	//지시: 칸을 찍으면 그리로, 적을 찍으면 그 적을, 주인공을 찍으면 따라옴. 소품은 공격하지 않는다.
	public void direct( int cell ){
		Char ch = Actor.findChar(cell);
		if (ch != null && ch != Dungeon.hero && Dungeon.level.heroFOV[cell] && isProp(ch)){
			GLog.w(Messages.get(PetAlly.class, "no_prop"));
			return;
		}
		directTocell(cell);
	}

	@Override
	public void defendPos( int cell ){
		GLog.i(Messages.get(PetAlly.class, "direct_defend", name()));
		super.defendPos(cell);
	}

	@Override
	public void followHero(){
		GLog.i(Messages.get(PetAlly.class, "direct_follow", name()));
		super.followHero();
	}

	@Override
	public void targetChar( Char ch ){
		if (isProp(ch)) return;
		GLog.i(Messages.get(PetAlly.class, "direct_attack", name()));
		super.targetChar(ch);
	}

	@Override
	protected Char chooseEnemy() {
		Char e = super.chooseEnemy();
		if (e != null && isProp(e)){
			enemy = null;
			return null;
		}
		return e;
	}

	@Override
	public boolean canAttack( Char enemy ){
		return !isProp(enemy) && super.canAttack(enemy);
	}

	@Override
	public void die( Object cause ){
		flying = false;
		super.die(cause);
		GLog.w(Messages.get(PetAlly.class, "retreat", name()));
		if (Dungeon.hero != null) Dungeon.hero.interrupt();
	}

	@Override
	public void destroy() {
		super.destroy();
		Dungeon.observe();
	}

	@Override
	public String description() {
		return super.description() + "\n\n" + Messages.get(PetAlly.class, "desc_common");
	}

	//보스의 소품(고정 몹): 펫이 공격하지 않는다 (펫이 기믹을 대신 풀어 주지 않게)
	public static boolean isProp( Char ch ){
		return ch instanceof SapDroplet || ch instanceof ChainRing || ch instanceof CellDoor
				|| ch instanceof ManaVein || ch instanceof EchoKnot || ch instanceof HeartValve
				|| ch instanceof SoulCage || ch instanceof ResonanceCrystal || ch instanceof FruitBranch
				|| ch instanceof MemoryLamp || ch instanceof WraithEyes || ch instanceof Rubble;
	}

	//(사용자) 주인공이 쏜 화살·던진 것·마법 탄 등의 투사체는 경로의 펫을 지나쳐 뒤의 적에게 간다 (Ballistica, WornShortbow.aim).
	//펫이 선 칸을 직접 고른 경우(to)는 그 칸에서 멈춘다(펫에게 무언가를 던지거나 쓸 때).
	public static boolean passThrough( Integer source, int cell, int to ){
		if (source == null || Dungeon.hero == null || source != Dungeon.hero.pos || cell == to) return false;
		return Actor.findChar(cell) instanceof PetAlly;
	}

	//이 몹이 이 펫을 평타의 대상으로 삼지 않는지: 보스는 펫을 노리지 않는다
	public static boolean untargetable( Mob attacker, Char target ){
		if (!(target instanceof PetAlly)) return false;
		if (attacker.properties().contains(Char.Property.BOSS)) return true;
		return ((PetAlly) target).ignoredByEnemies();
	}

	//적이 이 펫을 아예 노리지 않는지 (길든 주문, T7)
	public boolean ignoredByEnemies(){
		return false;
	}
}
