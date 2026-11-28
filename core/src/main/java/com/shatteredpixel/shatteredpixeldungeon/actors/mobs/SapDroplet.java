/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.SapDropMarker;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.GooBossRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SapDropletSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (G2): 5층 썩은 수액 덩어리가 다 쪼그라들었을 때 보스방에 흩어지는 수액 방울. 공격할 수 있는 고정 몹.
//주인공이 밟거나(부딪히면) 때리면 터지고, 불에 타면 그을려 사라진다. 보스가 옆에 오면 흡수한다(Goo).
//보스가 향하는 방울은 떨리고(예고), 시야 밖이어도 안개 위에 표시된다(SapDropMarker).
public class SapDroplet extends Mob {

	{
		spriteClass = SapDropletSprite.class;

		HP = HT = 1;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	private boolean targeted = false;
	private SapDropMarker marker;

	@Override
	protected boolean act() {
		//움직이지도 공격하지도 않는다
		spend( TICK );
		return true;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return 0; //반드시 맞는다
	}

	//주인공이 밟으려 하면(누르면) 원작처럼 평타로 친다 (체력 1이라 한 번에 터진다)
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	public void die( Object cause ) {
		setTargeted( false );
		if ((cause instanceof Burning || cause instanceof Fire) && sprite instanceof SapDropletSprite){
			((SapDropletSprite)sprite).burnOnDeath();
		}
		if (sprite != null && sprite.visible) Sample.INSTANCE.play( com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.HIT_SLASH, 0.6f, 1.6f );
		super.die( cause );
	}

	//보스가 흡수한다: 흡수 그림과 함께 사라진다 (죽는 동작 없이)
	public void absorbedBy( Char boss ){
		setTargeted( false );
		if (Dungeon.level.heroFOV[pos]) BossEffects.atCell(BossEffects.Fx.SAP_ABSORB, pos, 10, false);
		if (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[boss.pos]) BossEffects.fly(BossEffects.Fx.SAP_FLY, pos, boss.pos, 0.25f, null);
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	//보스가 이 방울로 향하는지 (떨림 + 안개 위 표시)
	public void setTargeted( boolean on ){
		if (targeted == on) return;
		targeted = on;
		if (sprite instanceof SapDropletSprite) ((SapDropletSprite)sprite).twitch( on );
		if (on){
			if (marker == null){
				marker = new SapDropMarker( this );
				GameScene.groupOverFog( marker );
			}
		} else if (marker != null){
			marker.killAndErase();
			marker = null;
		}
	}

	public boolean targeted(){
		return targeted;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (marker != null){ marker.killAndErase(); marker = null; }
	}

	// ===== 보스방에 흩뿌리기 =====

	//보스가 처음 있던 방(GooBossRoom)의 빈 바닥 중에서: 보스 2칸 안 제외, 다른 방울 3칸 안 제외(자리가 없으면 1칸까지)
	public static ArrayList<Integer> scatter( Char boss, int count ){
		ArrayList<Integer> room = new ArrayList<>();
		Room r = null;
		if (Dungeon.level instanceof RegularLevel){
			for (Room rm : ((RegularLevel)Dungeon.level).rooms()) if (rm instanceof GooBossRoom){ r = rm; break; }
		}
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (r != null && !r.inside(Dungeon.level.cellToPoint(i))) continue;
			if (r == null && Dungeon.level.distance(i, boss.pos) > 6) continue; //방을 못 찾으면 보스 둘레
			if (!Dungeon.level.passable[i] || Dungeon.level.solid[i] || Dungeon.level.pit[i]) continue;
			if (com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(i) != null) continue;
			if (Dungeon.level.distance(i, boss.pos) <= MazeTower.SAP_DROP_BOSS_GAP) continue;
			room.add(i);
		}
		ArrayList<Integer> chosen = new ArrayList<>();
		for (int gap = MazeTower.SAP_DROP_GAP; gap >= MazeTower.SAP_DROP_GAP_MIN && chosen.size() < count; gap--){
			ArrayList<Integer> pool = new ArrayList<>(room);
			pool.removeAll(chosen);
			while (chosen.size() < count && !pool.isEmpty()){
				int c = pool.remove(Random.Int(pool.size()));
				boolean ok = true;
				for (int o : chosen) if (Dungeon.level.distance(o, c) <= gap){ ok = false; break; }
				if (ok) chosen.add(c);
			}
		}
		return chosen;
	}

	public static ArrayList<SapDroplet> alive(){
		ArrayList<SapDroplet> list = new ArrayList<>();
		if (Dungeon.level == null) return list;
		for (Mob m : Dungeon.level.mobs) if (m instanceof SapDroplet && m.isAlive()) list.add((SapDroplet)m);
		return list;
	}

	//남은 방울을 모두 터뜨린다 (보스가 원래 몸으로 돌아왔거나 쓰러졌을 때)
	public static void popAll(){
		for (SapDroplet d : alive()) d.die( null );
	}

	private static final String TARGETED = "maze_targeted";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TARGETED, targeted );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		targeted = false; //표시는 보스가 다음 턴에 다시 고른다
	}
}
