/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.quest;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;

//Maze Tower (K8): 미스릴 파편. 15층 뿌리를 갉는 것이 원작 저주받은 쇳조각 대신 2~3개 떨어뜨린다.
//나중에 대장장이의 미스릴 무기(특수 무기 청사진)의 핵심 재료가 된다(조합법은 미정). 지금은 모아 두는 재료일 뿐이다.
//그림: sprites/maze_mithril_shard.png (여러 면으로 깎인 옅은 청록 파편, 몸 안에 짧은 푸른 맥), 맥 둘레의 마력 빛이 2초에 한 번 은은하게 숨 쉼(그림 12장을 차례로, MazeItemSprites.animPeriod, MITHRIL_BREATH_PERIOD).
public class MithrilShard extends Item {

	{
		image = MazeItemSprites.MITHRIL_SHARD;
		stackable = true;
		bones = false;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return quantity * MazeTower.MITHRIL_VALUE;
	}
}
