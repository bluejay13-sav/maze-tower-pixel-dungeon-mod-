/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Maze Tower (J2): 살아 있는 광맥 (11~14층, 지형처럼 서 있는 고정 몹). 15층 보스방 광맥(ManaVein)과 같은 그림·같은 부서짐(마력 증기).
//공격하지 않는다. 주인공이 바로 옆 칸에 있으면 턴마다 마나가 조금 더 찬다(Mana.turnRegen, 아이콘 Near).
//누구의 공격이든(적의 범위 공격 포함) 맞으면 부서진다. 새끼 굴벌레가 갉아먹는다(J8).
public class LivingVein extends ManaVein {

	@Override
	public String description() {
		return Messages.get(this, "desc", HP, HT, MazeTower.J2_VEIN_MANA);
	}

	//주인공이 살아 있는 광맥 바로 옆 칸에 있는지 (아이콘도 여기서 붙인다)
	public static boolean besideHero( Hero hero ){
		if (hero == null || Dungeon.level == null) return false;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			if (Actor.findChar(c) instanceof LivingVein){
				com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong(hero, Near.class, 1f);
				return true;
			}
		}
		return false;
	}

	//광맥 곁: 마나가 더 빨리 찬다 (보이는 규칙)
	public static class Near extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon( Image icon ) { icon.hardlight(0.45f, 0.7f, 1f); }
		@Override public String desc() { return Messages.get(this, "desc", MazeTower.J2_VEIN_MANA); }
	}

	// ===== 놓기 =====

	//11~14층 일반 층마다 J2_VEINS_MIN~MAX개 (방 안의 트인 바닥, 입구·출구에서 떨어진 곳, 따로 된 난수)
	public static void place( Level level ){
		if (Dungeon.depth < 11 || Dungeon.depth > 14) return;
		Random.pushGenerator( Dungeon.seedCurDepth() ^ 0x3A7E11C5L );
		try {
			boolean[] near = new boolean[level.length()];
			for (int t : new int[]{ level.entrance(), level.exit() }){
				if (t < 0) continue;
				PathFinder.buildDistanceMap( t, BArray.not(level.solid, null), 4 );
				for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
			}
			int want = Random.IntRange(MazeTower.J2_VEINS_MIN, MazeTower.J2_VEINS_MAX);
			int placed = 0;
			for (int tries = 0; tries < 1000 && placed < want; tries++){
				int c = Random.Int(level.length());
				if (near[c] || !openFloor(level, c)) continue;
				//광맥끼리는 조금 떨어지게
				boolean close = false;
				for (Mob m : level.mobs) if (m instanceof LivingVein && level.distance(m.pos, c) < 4) close = true;
				if (close) continue;
				LivingVein v = new LivingVein();
				v.pos = c;
				level.mobs.add(v);
				placed++;
			}
		} finally {
			Random.popGenerator();
		}
	}

	//길을 막지 않는 트인 바닥: 둘레 8칸 중 6칸 이상이 지나갈 수 있고, 위아래나 좌우로만 트인 좁은 길목이 아니다
	//(동굴 방은 들쭉날쭉해서 8칸이 모두 트인 자리만 고르면 너무 적게 놓였다)
	public static boolean openFloor( Level level, int c ){ //Maze Tower (J8): 굴벌레·공명 수정도 같이 씀
		if (!level.insideMap(c)) return false;
		int t = level.map[c];
		if (t != Terrain.EMPTY && t != Terrain.EMPTY_DECO && t != Terrain.GRASS) return false;
		if (level.findMob(c) != null || level.heaps.get(c) != null || level.traps.get(c) != null) return false;
		int open = 0;
		for (int n : PathFinder.NEIGHBOURS8){
			int d = c + n;
			if (level.findMob(d) != null) return false;
			if (level.passable[d] && !level.pit[d]) open++;
		}
		if (open < 6) return false;
		int w = level.width();
		boolean up = level.passable[c - w], dn = level.passable[c + w], lf = level.passable[c - 1], rt = level.passable[c + 1];
		//좁은 길목(한쪽 축만 트임)이면 막힐 수 있다
		if ((up && dn && !lf && !rt) || (lf && rt && !up && !dn)) return false;
		return true;
	}
}
