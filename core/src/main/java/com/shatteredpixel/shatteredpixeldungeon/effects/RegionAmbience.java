/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeRetreat;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 구역 분위기 이펙트 (명세 20번). 주인공이 행동할 때마다 1/REGION_FX_CHANCE 확률로,
//보이는 바닥 칸 하나에서 한 번 재생한다. 한 화면에 하나씩만.
//  봉인의 회랑(6~10층): 바닥에서 영혼이 피어올라 사라짐
//  잊힌 마도시(16~20층): 청록·보라·금빛 입자가 떠올라 사라짐
//  탑의 심장(21~26층, 역행 전): 바닥에서 붉은 고리가 두 번 "두-근" 퍼짐
public class RegionAmbience {

	private static MovieClip current = null;

	public static void onHeroTurn(){
		if (Dungeon.level == null || Dungeon.hero == null) return;
		if (current != null && current.alive && current.parent != null) return;
		current = null;
		if (Random.Int(MazeTower.REGION_FX_CHANCE) != 0) return;

		BossEffects.Fx fx = fxFor(Dungeon.depth);
		if (fx == null) return;

		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.heroFOV[i] && Dungeon.level.passable[i] && !Dungeon.level.solid[i]
					&& !Dungeon.level.water[i] && !Dungeon.level.pit[i] && i != Dungeon.hero.pos){
				cells.add(i);
			}
		}
		if (cells.isEmpty()) return;
		int cell = Random.element(cells);

		if (fx == BossEffects.Fx.REGION_SOUL){
			current = BossEffects.atCellBottom(fx, cell, 8, false);
		} else {
			current = BossEffects.atCell(fx, cell, fx == BossEffects.Fx.REGION_HEARTBEAT ? 10 : 8, false);
		}
	}

	private static BossEffects.Fx fxFor( int depth ){
		if (depth >= 6 && depth <= 10)  return BossEffects.Fx.REGION_SOUL;
		if (depth >= 16 && depth <= 20) return BossEffects.Fx.REGION_MAGIC;
		//(사용자) 탑의 심장의 박동 이펙트는 없앰 (바닥 핏줄의 두근 박동이 대신함). 다시 켜려면 MazeTower.HEART_REGION_BEAT
		if (MazeTower.HEART_REGION_BEAT && depth >= 21 && depth <= 26 && !MazeRetreat.active()) return BossEffects.Fx.REGION_HEARTBEAT;
		return null;
	}
}
