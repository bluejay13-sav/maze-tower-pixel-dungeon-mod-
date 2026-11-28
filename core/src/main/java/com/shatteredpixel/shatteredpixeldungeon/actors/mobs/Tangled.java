/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TangledSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

//Maze Tower (N6): 얽힌 것들. 형상 없이 땅에서 솟았다 파고드는 뿌리들끼리 얽힌 것, 셋이 한 묶음으로 나와 뿌리로 이어진다(21~24층).
// - (사용자) 한 마리가 피해를 받으면 이어진 것들 모두(맞은 것 포함)가 그 피해의 1/4(N6_SHARE)씩 나눠 받는다.
//   셋이 이어져 있으면 한 번 맞을 때 묶음 전체가 원래 피해의 3/4을 잃고, 셋이 거의 함께 깎인다.
// - 서로 N6_LINK_RANGE칸보다 멀어지면 그 뿌리가 끊긴다(사용자). 끊긴 뿌리는 다시 이어지지 않는다. 혼자 남으면 피해를 그대로 받는다.
public class Tangled extends Mob {

	{
		spriteClass = TangledSprite.class;

		HP = HT = MazeTower.N6_HP;
		defenseSkill = 20;

		EXP = 8;
		maxLvl = 30;
	}

	public int[] links = new int[0];   //이어진 것들의 id
	public float shareFlash = 0;        //피해가 뿌리를 타고 지나간 직후 (뿌리 줄이 잠깐 핏빛으로 번쩍, 저장 안 함)
	public int variant = -1;            //그림 모양 0~2 (셋이 서로 다른 모양)
	private boolean grouped = false;    //묶음을 만들었는지

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( MazeTower.N6_DMG_MIN, MazeTower.N6_DMG_MAX );
	}

	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange( 0, 8 );
	}

	//이어진 채 살아 있는 것들 (자기 빼고, 이어진 것의 이어진 것까지)
	public ArrayList<Tangled> group(){
		ArrayList<Tangled> out = new ArrayList<>();
		HashSet<Integer> seen = new HashSet<>();
		ArrayList<Tangled> queue = new ArrayList<>();
		seen.add( id() );
		queue.add( this );
		while (!queue.isEmpty()){
			Tangled t = queue.remove( 0 );
			for (int id : t.links){
				if (seen.contains( id )) continue;
				seen.add( id );
				Actor a = Actor.findById( id );
				if (a instanceof Tangled && ((Tangled) a).isAlive()){
					out.add( (Tangled) a );
					queue.add( (Tangled) a );
				}
			}
		}
		return out;
	}

	public boolean linkedTo( int id ){
		for (int l : links) if (l == id) return true;
		return false;
	}

	private void unlink( int id ){
		if (!linkedTo( id )) return;
		int[] n = new int[links.length - 1];
		int k = 0;
		for (int l : links) if (l != id) n[k++] = l;
		links = n;
	}

	private void link( int id ){
		if (linkedTo( id )) return;
		int[] n = new int[links.length + 1];
		System.arraycopy( links, 0, n, 0, links.length );
		n[links.length] = id;
		links = n;
	}

	@Override
	protected boolean act() {
		if (MazeTower.N6_TANGLED && !grouped){
			grouped = true;
			if (variant < 0) variant = 0;
			makeGroup();
		}
		//멀어지거나 편이 바뀐 것과의 뿌리는 끊긴다
		for (int id : links.clone()){
			Actor a = Actor.findById( id );
			boolean keep = a instanceof Tangled && ((Tangled) a).isAlive()
					&& ((Tangled) a).alignment == alignment
					&& Dungeon.level.distance( pos, ((Tangled) a).pos ) <= MazeTower.N6_LINK_RANGE;
			if (!keep){
				unlink( id );
				if (a instanceof Tangled) ((Tangled) a).unlink( id() );
			}
		}
		return super.act();
	}

	//처음 움직일 때 둘레 빈 칸에 둘을 더 틔워 셋을 서로 잇는다 (원작 구울이 짝을 만드는 방식)
	private void makeGroup(){
		ArrayList<Tangled> made = new ArrayList<>();
		made.add( this );
		for (int i = 1; i < MazeTower.N6_GROUP; i++){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar( c ) == null) cells.add( c );
			}
			if (cells.isEmpty()) break;
			Tangled t = new Tangled();
			t.grouped = true;
			t.variant = i % 3;
			t.state = state == SLEEPING ? t.SLEEPING : t.WANDERING;
			t.pos = Random.element( cells );
			GameScene.add( t );
			Dungeon.level.occupyCell( t );
			if (sprite != null && sprite.visible) Actor.add( new Pushing( t, pos, t.pos ) );
			made.add( t );
		}
		for (Tangled a : made) for (Tangled b : made) if (a != b) a.link( b.id() );
	}

	//뿌리를 타고 나눠 받는 몫 (다시 나누지 않는다)
	public static class Shared {}

	@Override
	public void damage( int dmg, Object src ) {
		if (!MazeTower.N6_TANGLED || src instanceof Shared || dmg <= 0 || !isAlive()){
			super.damage( dmg, src );
			return;
		}
		ArrayList<Tangled> g = group();
		if (g.isEmpty()){
			super.damage( dmg, src );
			return;
		}
		int n = g.size() + 1;
		int share = (int) Math.ceil( dmg * MazeTower.N6_SHARE ); //(사용자) 셋이 각자 1/4씩
		if (com.watabou.utils.DeviceCompat.isDebug()) System.out.println("[Maze Tower] tangled share: hit=" + dmg + " each=" + share + " group=" + n);
		shareFlash = 1f;
		for (Tangled t : g){ t.shareFlash = 1f; t.damage( share, new Shared() ); }
		super.damage( share, src );
	}

	@Override
	public void die( Object cause ) {
		for (Tangled t : MazeTower.N6_TANGLED ? group() : new ArrayList<Tangled>()) t.unlink( id() );
		links = new int[0];
		super.die( cause );
	}

	@Override
	public String description() {
		String d = super.description();
		if (MazeTower.N6_TANGLED){
			int n = group().size();
			if (n > 0) d += "\n\n" + Messages.get( this, "linked", n );
			else d += "\n\n" + Messages.get( this, "alone" );
		}
		return d;
	}

	private static final String LINKS = "maze_links", VARIANT = "maze_variant", GROUPED = "maze_grouped";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LINKS, links );
		bundle.put( VARIANT, variant );
		bundle.put( GROUPED, grouped );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( LINKS )) links = bundle.getIntArray( LINKS );
		variant = bundle.getInt( VARIANT );
		grouped = bundle.getBoolean( GROUPED );
	}
}
