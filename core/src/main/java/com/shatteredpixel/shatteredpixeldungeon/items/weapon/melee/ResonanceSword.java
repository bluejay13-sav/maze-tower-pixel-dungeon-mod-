/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;

//Maze Tower (W2): 5티어 공명검. 공명 수정을 벼려 넣은 마도시의 검.
//평타 피해는 5티어 표준의 85%(MazeTower.RESONANCE_SWORD_DMG). 스킬 "방출"은 SwordSkill에서 남은 마나를 모두 써서 친다.
//그림 sprites/maze_sword_resonance.png (원본 docs/sprites/weapons2/make_tier5.ps1).
public class ResonanceSword extends MeleeWeapon {

	{
		image = com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.SWORD_RESONANCE; //(사용자 확인) 새 그림
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

		tier = 5;
	}

	@Override
	public int min(int lvl) {
		return Math.round(super.min(lvl) * MazeTower.RESONANCE_SWORD_DMG);
	}

	@Override
	public int max(int lvl) {
		return Math.round(super.max(lvl) * MazeTower.RESONANCE_SWORD_DMG);
	}
}
