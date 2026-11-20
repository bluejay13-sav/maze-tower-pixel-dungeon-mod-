/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;

//Maze Tower: 20층 두 보스(망령 왕·마도시의 여제) 공통 처리 (명세 22번).
//쓰러질 때: 원작 드워프 왕처럼 결실의 핵(원작은 왕의 왕관)·배지·점수·방 열기.
public class MazeBoss20 {

	//(결정) 단계가 넘어가는 피해: 문턱(threshold, 최대 체력 비율)을 넘어 내려가면 체력이 문턱보다 1% 낮은 곳에서 멈추도록 피해를 줄인다.
	//예: 망령 왕 11%에서 큰 피해 → 9%. threshold가 0이면 그대로
	public static int capPhaseDamage( Mob boss, int dmg, float threshold ){
		if (threshold <= 0 || dmg <= 0) return dmg;
		if (boss.HP <= boss.HT * threshold) return dmg;
		int floorHP = Math.max(1, (int)Math.floor(boss.HT * (threshold - MazeTower.B20_PHASE_DROP)));
		if (boss.HP - dmg < floorHP) return Math.max(0, boss.HP - floorHP);
		return dmg;
	}

	//피해를 입은 뒤: 문턱을 넘었으면 체력을 정확히 문턱보다 1% 낮게 맞춘다 (예: 10% 아래로 내려가면 9%)
	public static void settlePhaseHP( Mob boss, int preHP, float threshold ){
		if (threshold <= 0 || !boss.isAlive()) return;
		if (preHP > boss.HT * threshold && boss.HP <= boss.HT * threshold){
			boss.HP = Math.max(1, (int)Math.floor(boss.HT * (threshold - MazeTower.B20_PHASE_DROP)));
		}
	}

	//보스 Mob.die가 끝난 뒤에 부른다
	public static void afterDeath( Mob boss ){
		int at = boss.pos;
		if (!MazeTower.ORIGINAL_ARMOR_ABILITY){
			if (MazeTower.DWARF_KING_DROPS_FRUIT_CORE && Dungeon.hero.armorAbility == null){
				Dungeon.level.drop(new com.shatteredpixel.shatteredpixeldungeon.items.FruitCore(), at).sprite.drop(at);
			}
		} else {
			Dungeon.level.drop(new KingsCrown(), at).sprite.drop();
		}

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[3] += 4000;

		Dungeon.level.unseal();

		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) {
			beacon.upgrade();
		}
		com.shatteredpixel.shatteredpixeldungeon.MazeTrial.onBossDefeated(); //Maze Tower: 보스전 체험판이면 체험 완료 창
	}
}
