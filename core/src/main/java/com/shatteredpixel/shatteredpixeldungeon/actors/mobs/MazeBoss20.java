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
	}
}
