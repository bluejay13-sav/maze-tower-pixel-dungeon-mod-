/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SproutSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Maze Tower (F6): 기생 싹 - 3층부터 드물게 놓이는 움직이지 않는 함정형 몹.
//처음엔 숨어 있다(원작 미믹처럼 중립·가만히): 적 표시·공격 버튼·자동 조준에 잡히지 않는다. 그림은 가끔 살짝 떨린다.
//주인공이 바로 옆 칸(대각선 포함)에 들어서면 뿌리로 묶고 약한 피해를 주며 드러난다 (다시 묶기까지 간격).
//탐색하거나, 살펴보거나(정보 창), 맞으면 드러난다. 드러난 뒤에는 옆에 있는 주인공을 약하게 때리는 움직이지 않는 적.
public class ParasiteSprout extends Mob {

	{
		spriteClass = SproutSprite.class;

		HP = HT = MazeTower.F6_SPROUT_HP;
		defenseSkill = 0;

		EXP = MazeTower.F6_SPROUT_EXP;
		maxLvl = 8;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
	}

	public boolean hidden = true;
	private int cooldown = 0;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( MazeTower.F6_SPROUT_DMG_MIN, MazeTower.F6_SPROUT_DMG_MAX );
	}

	@Override
	public int attackSkill( Char target ) {
		return 10;
	}

	@Override
	protected boolean act() {
		if (cooldown > 0) cooldown--;
		return super.act();
	}

	//움직이지 않는다
	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	//숨어 있을 때 주인공이 누르면 말을 거는 대신 공격한다 (맞으면 드러난다)
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage( dmg, src );
		if (isAlive()) reveal();
	}

	@Override
	public String description() {
		//살펴보면 드러난다
		if (hidden && Dungeon.level != null && Dungeon.level.mobs.contains(this)) reveal();
		return super.description();
	}

	public void reveal(){
		if (!hidden) return;
		hidden = false;
		alignment = Alignment.ENEMY;
		state = HUNTING;
		if (Dungeon.hero != null){
			enemy = Dungeon.hero;
			Dungeon.hero.interrupt();
		}
		if (sprite != null && sprite.visible){
			sprite.showAlert();
			GLog.w( Messages.get(this, "revealed") );
		}
	}

	//주인공이 바로 옆 칸에 들어섰다: 뿌리로 묶는다
	private void snare( Hero hero ){
		if (!isAlive() || cooldown > 0 || hero.flying || buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class) != null) return;
		cooldown = MazeTower.F6_SPROUT_COOLDOWN;
		reveal();
		if (sprite instanceof SproutSprite && sprite.visible) ((SproutSprite)sprite).lash( hero.pos );
		Buff.prolong( hero, Roots.class, MazeTower.F6_SPROUT_ROOT_TURNS );
		if (hero.sprite != null) hero.sprite.emitter().burst( LeafParticle.GENERAL, 6 );
		GLog.w( Messages.get(this, "snared") );
		hero.damage( damageRoll(), this );
	}

	//Level.occupyCell: 주인공이 칸에 들어섰을 때
	public static void onHeroEnterCell( Hero hero ){
		if (hero == null || Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof ParasiteSprout && Dungeon.level.adjacent(m.pos, hero.pos)){
				((ParasiteSprout)m).snare( hero );
			}
		}
	}

	//Hero.search: 일부러 탐색한 칸의 기생 싹이 드러난다
	public static void revealAt( int cell ){
		if (Dungeon.level == null) return;
		Mob m = Dungeon.level.findMob( cell );
		if (m instanceof ParasiteSprout) ((ParasiteSprout)m).reveal();
	}

	//층을 만들 때 놓는다 (SewerLevel.createMobs). 원작 층 생성의 난수 흐름을 바꾸지 않도록 따로 된 난수를 쓴다.
	public static void place( Level level ){
		if (Dungeon.depth < MazeTower.F6_SPROUT_MIN_DEPTH) return;
		Random.pushGenerator( Dungeon.seedCurDepth() ^ 0x2B5C70A1L );
		try {
			if (Random.Float() >= MazeTower.F6_SPROUT_LEVEL_CHANCE) return;
			int count = Random.Float() < MazeTower.F6_SPROUT_EXTRA_CHANCE ? 2 : 1;
			//입구·출구 근처에는 두지 않는다
			boolean[] near = new boolean[level.length()];
			for (int t : new int[]{ level.entrance(), level.exit() }){
				if (t < 0) continue;
				PathFinder.buildDistanceMap( t, BArray.not(level.solid, null), 3 );
				for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
			}
			int placed = 0;
			for (int tries = 0; tries < 300 && placed < count; tries++){
				int c = Random.Int( level.length() );
				int t = level.map[c];
				if (!level.insideMap(c) || near[c] || level.findMob(c) != null || level.heaps.get(c) != null) continue;
				if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.GRASS) continue;
				//Maze Tower: 만들고 있는 층(level)의 수액을 본다 (Dungeon.level은 아직 이전 층이거나, 층 건너뛰기 중엔 비어 있다)
				com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob sap = level.blobs.get(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SapFloor.class);
				if (sap != null && sap.cur != null && sap.cur[c] > 0) continue;
				boolean nextToOther = false; //두 싹은 서로 붙지 않게
				for (Mob m : level.mobs) if (m instanceof ParasiteSprout && level.distance(m.pos, c) < 4) nextToOther = true;
				if (nextToOther) continue;
				ParasiteSprout s = new ParasiteSprout();
				s.pos = c;
				level.mobs.add( s );
				placed++;
			}
		} finally {
			Random.popGenerator();
		}
	}

	private static final String HIDDEN = "maze_hidden", COOLDOWN = "maze_cooldown";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( HIDDEN, hidden );
		bundle.put( COOLDOWN, cooldown );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		hidden = bundle.getBoolean( HIDDEN );
		cooldown = bundle.getInt( COOLDOWN );
		if (hidden){
			alignment = Alignment.NEUTRAL;
			state = PASSIVE;
		} else {
			alignment = Alignment.ENEMY;
		}
	}
}
