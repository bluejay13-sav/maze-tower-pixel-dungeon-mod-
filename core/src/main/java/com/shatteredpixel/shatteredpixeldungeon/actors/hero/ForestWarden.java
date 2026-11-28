/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

//Maze Tower (지시서 1127, S8): 3차 전직 숲지기 (사냥꾼, 무기 무관). 싸울수록 방이 숲이 된다.
// 풀이 자란다: 걸어서 떠난 칸, 화살이 떨어진 칸, 화살에 맞은 적의 발밑에 높은 풀이 자란다(원작 고랑진 풀 FURROWED_GRASS:
//             씨앗·이슬을 떨구지 않고, 원작 높은 풀처럼 시야를 가리고 불에 탄다). 물·함정·문·계단·구덩이·특수 바닥·식물이 있는 칸은 건너뛴다.
// 풀이 눕지 않는다: 숲지기가 밟은 높은 풀(원작 풀·자란 풀 모두)은 눕지 않고 씨앗·이슬도 나오지 않는다(HighGrass.trample). 적·동료가 밟으면 원작대로.
// 풀 속 기습: 높은 풀 칸에 서서 넣는 모든 공격(근접·화살·던지기·스킬)은 원작 기습 판정(Mob.surprisedBy, 공격할 때만).
//            기습을 넣으면 그 행동이 끝날 때 선 칸의 풀이 눕는다(풀 한 칸에 기습 한 번, 여러 번 치는 스킬은 모든 타격이 기습).
//            '적이 나를 아직 보지 못했다'는 판정(surprisedBy(…, false))은 바꾸지 않는다.
public class ForestWarden {

	public static boolean is( Hero hero ){
		return hero != null && hero.subClass == HeroSubClass.FORESTWARDEN;
	}

	static boolean highGrass( int cell ){
		int t = Dungeon.level.map[cell];
		return t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	//풀을 둘 수 있는 칸 (빈 바닥·장식 바닥·짧은 풀·잿더미만. 물·함정·문·계단·구덩이·특수 바닥·식물은 아님)
	static boolean canGrow( int cell ){
		Level l = Dungeon.level;
		if (l == null || cell < 0 || cell >= l.length()) return false;
		int t = l.map[cell];
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.GRASS && t != Terrain.EMBERS) return false;
		if (l.traps.get( cell ) != null || l.plants.get( cell ) != null) return false;
		return !l.pit[cell];
	}

	//높은 풀(씨앗·이슬을 떨구지 않는 고랑진 풀)을 기른다
	public static boolean grow( int cell ){
		return grow( cell, true );
	}

	private static boolean grow( int cell, boolean canSpread ){
		if (!canGrow( cell )) return false;
		Level.set( cell, Terrain.FURROWED_GRASS );
		GameScene.updateMap( cell );
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get( cell ).burst( LeafParticle.LEVEL_SPECIFIC, 3 );
		if (canSpread) spread( cell ); //(S9) 번지는 숲 (번져서 자란 풀은 다시 번지지 않는다)
		Dungeon.observe(); //높은 풀은 시야를 가린다
		return true;
	}


	static int lvl( Hero hero, Talent t ){
		return hero == null ? 0 : Math.min( com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3_MAX_TALENT_LEVEL, hero.pointsInTalent( t ) );
	}

	// ----- (S9) 번지는 숲: 풀이 자랄 때 15/30/45/60% 확률로 옆의 빈 바닥 한 칸에도 함께 자란다 (번져 자란 풀도 씨앗·이슬 없음) -----
	private static void spread( int cell ){
		Hero hero = Dungeon.hero;
		int l = lvl( hero, Talent.SPREADING_GROVE );
		if (!is( hero ) || l <= 0 || com.watabou.utils.Random.Float() >= com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_SPREADING_GROVE[l]) return;
		java.util.ArrayList<Integer> cand = new java.util.ArrayList<>();
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (c == hero.pos || com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar( c ) != null) continue;
			if (canGrow( c ) && Dungeon.level.map[c] != Terrain.GRASS) cand.add( c ); //빈 바닥 (이미 풀이 있는 칸 말고)
		}
		if (cand.isEmpty()) return;
		grow( com.watabou.utils.Random.element( cand ), false );
	}

	// ----- (S9) 풀잎 화살: 풀 속에서 넣은 기습의 피해 +8/16/24/32% (근접 포함, BloomEffects.processHit) -----
	public static int hitDamage( Hero hero, Char enemy, int damage ){
		if (!is( hero ) || !(enemy instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob)) return damage;
		int l = lvl( hero, Talent.LEAF_ARROW );
		if (l <= 0 || !highGrass( hero.pos ) || !hero.canSurpriseAttack()) return damage;
		if (((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob) enemy).mazeNoSurprise()) return damage;
		BloomRules.debug( "leaf arrow +" + Math.round( com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_LEAF_ARROW[l] * 100 ) + "%" );
		return Math.round( damage * (1f + com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_LEAF_ARROW[l]) );
	}

	// ----- (S9) 숲의 품: 풀 속에 서 있는 동안 받는 근접 피해 −6/12/18/24% (Hero.damage ← BloomEffects.onHeroDamage) -----
	public static int taken( Hero hero, int dmg, Object src ){
		if (!is( hero ) || dmg <= 0 || !(src instanceof Char) || !highGrass( hero.pos )) return dmg;
		if (!Dungeon.level.adjacent( hero.pos, ((Char) src).pos )) return dmg; //근접 공격만
		int l = lvl( hero, Talent.GROVE_EMBRACE );
		if (l <= 0) return dmg;
		return Math.round( dmg * (1f - com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_GROVE_EMBRACE[l]) );
	}

	//숲의 품 4레벨: 높은 풀이 주인공의 시야를 가리지 않는다 (Level.updateFieldOfView, 주인공 시야에만)
	public static boolean seesThroughGrass( Hero hero ){
		return is( hero ) && lvl( hero, Talent.GROVE_EMBRACE ) >= com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3_MAX_TALENT_LEVEL;
	}

	// ----- (S9) 뿌리 걸음: 높은 풀(원작 풀·자란 풀)에 들어온 적이 1/2/3/4턴 느려진다 (Level.occupyCell ← BloomEffects.onMobEnterCell, 풀이 눕기 전) -----
	public static void onMobEnterCell( Char ch ){
		Hero hero = Dungeon.hero;
		int l = lvl( hero, Talent.ROOT_STEP );
		if (!is( hero ) || l <= 0 || !(ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob)) return;
		if (!ch.isAlive() || ch.alignment != Char.Alignment.ENEMY || ch.flying || !highGrass( ch.pos )) return;
		if (((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob) ch).mazeFixed()) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong( ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_ROOT_STEP[l] );
		BloomRules.debug( "root step: " + ch.name() + " crippled " + com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_ROOT_STEP[l] );
	}

	// ----- 풀이 자란다 -----

	//주인공이 걸어서 칸을 떠났다 (Hero.getCloser)
	public static void onHeroLeft( Hero hero, int cell ){
		if (is( hero )) grow( cell );
	}

	//주인공의 화살이 떨어졌거나 적을 맞혔다 (WornShortbow.endShot·emptyShot). 맞힌 적이면 그 적의 발밑
	public static void onArrowAt( Hero hero, int cell ){
		if (is( hero )) grow( cell );
	}

	// ----- 풀이 눕지 않는다 (HighGrass.trample) -----
	public static boolean keepsGrass( Char ch ){
		return ch instanceof Hero && is( (Hero) ch );
	}

	// ----- 풀 속 기습 -----
	private static boolean ambushUsed = false;

	//Mob.surprisedBy(주인공, 공격할 때): 높은 풀 칸에 서서 넣는 공격이면 기습
	public static boolean grassAmbush( Char enemy ){
		if (!(enemy instanceof Hero) || !is( (Hero) enemy )) return false;
		Hero hero = (Hero) enemy;
		if (!highGrass( hero.pos ) || !hero.canSurpriseAttack()) return false;
		ambushUsed = true;
		return true;
	}

	//주인공이 턴을 쓸 때 (BloomEffects.onHeroSpend): 이번 행동에 풀 속 기습을 넣었으면 선 칸의 풀이 눕는다
	static void onHeroSpend( Hero hero ){
		if (!ambushUsed) return;
		ambushUsed = false;
		if (hero == null || !highGrass( hero.pos )) return;
		Level.set( hero.pos, Terrain.GRASS );
		GameScene.updateMap( hero.pos );
		Dungeon.observe();
		BloomRules.debug( "forest warden: grass ambush, grass flattened" );
	}
}
