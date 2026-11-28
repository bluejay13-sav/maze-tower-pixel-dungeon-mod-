/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Maze Tower (H6): 갇힌 영혼 (6~9층, 중립).
//벽의 사슬 고리(ChainRing)에 묶여 웅크려 있다. 묶인 동안은 싸우지 않고, 다치지도 움직이지도 않는다.
//고리가 부서지면 풀려나 그 층에서 H6_FREED_TURNS턴 동안 주인공 곁에서 같이 싸우다 사라진다(경험치·전리품 없음).
public class ChainedSoul extends Mob {

	{
		spriteClass = SoulSprite.class;

		HP = HT = 40;
		defenseSkill = 10;
		EXP = 0;
		maxLvl = -2;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
		flying = true;

		properties.add(Property.UNDEAD);
		properties.add(Property.IMMOVABLE);
	}

	public boolean freed = false;
	public int ringId = -1;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(4, 12);
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 7);
	}

	//묶인 동안은 아무것도 다치게 하지 못한다
	@Override
	public boolean isInvulnerable( Class effect ) {
		return !freed || super.isInvulnerable(effect);
	}

	@Override
	public boolean reset() {
		return false;
	}

	//묶인 동안 누르면: 고리를 부숴야 한다는 안내 (자리를 바꾸지 않는다)
	@Override
	public boolean interact( Char c ) {
		if (!freed && c == Dungeon.hero){
			GLog.i( Messages.get(this, "chained") );
			return true;
		}
		return super.interact(c);
	}

	@Override
	protected boolean act() {
		if (!freed){
			spend(TICK);
			return true;
		}
		//싸울 적이 없으면 주인공을 따라다닌다
		if (state == WANDERING && Dungeon.hero != null && Dungeon.hero.isAlive()){
			target = Dungeon.hero.pos;
		}
		return super.act();
	}

	//고리가 부서지면 풀려난다
	public void free(){
		if (freed) return;
		//(I5) 10층 감방 미로의 영혼은 갇혀 있던 감방의 문을 열어 주고 넋빛으로 흩어진다 (풀어 준 수를 센다)
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel
				&& ((com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) Dungeon.level).onSoulFreed(this)){
			freed = true;
			if (sprite != null){
				sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
				if (sprite instanceof SoulSprite) ((SoulSprite) sprite).unchain();
			}
			if (Dungeon.level.heroFOV[pos]) GLog.p( Messages.get(this, "opens_door") );
			die(null);
			return;
		}
		freed = true;
		properties.remove(Property.IMMOVABLE);
		alignment = Alignment.ALLY;
		state = WANDERING;
		Buff.affect(this, Freedom.class, MazeTower.H6_FREED_TURNS);
		if (sprite != null){
			sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
			if (sprite instanceof SoulSprite) ((SoulSprite) sprite).unchain();
		}
		if (Dungeon.level.heroFOV[pos]) GLog.p( Messages.get(this, "freed") );
	}

	@Override
	public String description() {
		return super.description() + "\n\n" + Messages.get(this, freed ? "desc_freed" : "desc_chained");
	}

	//풀려난 뒤 남은 턴. 다 되면 넋이 흩어져 사라진다
	public static class Freedom extends FlavourBuff {
		@Override
		public void detach() {
			super.detach();
			if (target != null && target.isAlive()){
				if (Dungeon.level.heroFOV[target.pos]) GLog.i( Messages.get(ChainedSoul.class, "fades") );
				target.die(null);
			}
		}
	}

	private static final String FREED = "freed", RING = "ring_id";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(FREED, freed);
		bundle.put(RING, ringId);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		freed = bundle.getBoolean(FREED);
		ringId = bundle.getInt(RING);
		if (freed) properties.remove(Property.IMMOVABLE);
	}

	// ===== 놓기 =====

	//벽 바로 아래(위가 벽)이고 그 옆도 같은 조건인 바닥 칸 둘을 찾아 고리와 영혼을 놓는다. 놓았으면 true
	public static boolean placePair( Level level, int near, int radius, boolean addNow ){
		int w = level.width();
		java.util.ArrayList<Integer> cells = new java.util.ArrayList<>();
		for (int c = w; c < level.length() - w; c++){
			if (near >= 0 && level.distance(near, c) > radius) continue;
			cells.add(c);
		}
		Random.shuffle(cells);
		for (int c : cells){
			if (!wallFloor(level, c)) continue;
			for (int d : new int[]{1, -1}){
				int s = c + d;
				if (!wallFloor(level, s)) continue;
				ChainRing ring = new ChainRing();
				ring.pos = c;
				ChainedSoul soul = new ChainedSoul();
				soul.pos = s;
				ring.soulId = soul.id();
				soul.ringId = ring.id();
				if (addNow){
					GameScene.add(ring);
					GameScene.add(soul);
				} else {
					level.mobs.add(ring);
					level.mobs.add(soul);
				}
				return true;
			}
		}
		return false;
	}

	private static boolean wallFloor( Level level, int c ){
		if (c < level.width() || c >= level.length() || !level.insideMap(c)) return false;
		int t = level.map[c];
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.EMPTY_SP) return false;
		int up = c - level.width();
		if (!level.solid[up] || level.map[up] == Terrain.DOOR || level.map[up] == Terrain.OPEN_DOOR) return false;
		//(사용자) 1칸짜리 통로에는 놓지 않는다: 아래 칸도 지나갈 수 있는 바닥이어야 한다 (방 안의 벽 앞)
		int down = c + level.width();
		if (down >= level.length() || level.solid[down] || !level.passable[down]
				|| level.map[down] == Terrain.DOOR || level.map[down] == Terrain.OPEN_DOOR) return false;
		if (level.findMob(c) != null || level.heaps.get(c) != null) return false;
		if (Dungeon.hero != null && Dungeon.hero.pos == c) return false;
		return true;
	}

	//6~9층 일반 층에 H6_LEVEL_CHANCE 확률로 한 쌍 (입구·출구에서 떨어진 곳, 따로 된 난수)
	public static void place( Level level ){
		if (Dungeon.depth < 6 || Dungeon.depth > 9) return;
		Random.pushGenerator( Dungeon.seedCurDepth() ^ 0x5E1A3C77L );
		try {
			if (Random.Float() >= MazeTower.H6_LEVEL_CHANCE) return;
			boolean[] near = new boolean[level.length()];
			for (int t : new int[]{ level.entrance(), level.exit() }){
				if (t < 0) continue;
				PathFinder.buildDistanceMap( t, BArray.not(level.solid, null), 4 );
				for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
			}
			for (int tries = 0; tries < 40; tries++){
				int c = Random.Int(level.length());
				if (near[c] || !wallFloor(level, c)) continue;
				if (placePair(level, c, 0, false)) return;
			}
		} finally {
			Random.popGenerator();
		}
	}

	// ===== 그림 =====

	//웅크린 푸른 영혼 (원작 혼령 칸 배치 14×15: 0·1 가만히, 2·3 공격, 4~7 사라짐). 10층 죄수의 영혼도 같은 그림
	public static class SoulSprite extends MobSprite {

		private com.shatteredpixel.shatteredpixeldungeon.effects.SoulChain chain;

		public SoulSprite(){
			super();
			texture( Assets.Sprites.SOUL );
			TextureFilm frames = new TextureFilm( texture, 14, 15 );
			idle = new Animation( 3, true );
			idle.frames( frames, 0, 1 );
			run = new Animation( 10, true );
			run.frames( frames, 0, 1 );
			attack = new Animation( 10, false );
			attack.frames( frames, 0, 2, 3 );
			die = new Animation( 8, false );
			die.frames( frames, 4, 5, 6, 7 );
			play( idle );
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof ChainedSoul && !((ChainedSoul) ch).freed && ((ChainedSoul) ch).ringId != -1){
				chain = new com.shatteredpixel.shatteredpixeldungeon.effects.SoulChain(((ChainedSoul) ch).ringId, ch.id());
				GameScene.groupEffect(chain);
			}
		}

		public void unchain(){
			if (chain != null){ chain.killAndErase(); chain = null; }
		}

		@Override
		public void die() {
			unchain();
			super.die();
		}

		@Override
		public void kill() {
			unchain();
			super.kill();
		}

		@Override
		public int blood() {
			return 0xFF8FC8FF;
		}
	}
}
