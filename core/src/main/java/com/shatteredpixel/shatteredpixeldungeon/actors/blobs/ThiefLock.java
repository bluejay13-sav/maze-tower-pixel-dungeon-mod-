/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;

//Maze Tower (H3): 탈옥수가 걸어 잠근 문. 칸의 값 = 남은 턴. 0이 되면 원래 문(닫힌 문)으로 돌아간다.
//잠긴 동안 주인공은 열쇠로도 열 수 없다(Hero.handle·actUnlock이 막음, 열쇠를 헛되이 쓰지 않게).
public class ThiefLock extends Blob {

	@Override
	protected void evolve() {
		int w = Dungeon.level.width();
		for (int i = area.top; i < area.bottom; i++){
			for (int j = area.left; j < area.right; j++){
				int cell = j + i * w;
				if (cur[cell] <= 0){ off[cell] = 0; continue; }
				off[cell] = cur[cell] - 1;
				if (off[cell] <= 0){
					off[cell] = 0;
					unlock(cell);
				}
				volume += off[cell];
			}
		}
	}

	private static void unlock( int cell ){
		if (Dungeon.level.map[cell] == Terrain.LOCKED_DOOR){
			Level.set(cell, Terrain.DOOR);
			GameScene.updateMap(cell);
			Dungeon.observe(); //문 너머 시야를 바로 다시 계산
		}
	}

	//그 칸이 탈옥수가 잠근 문인지
	public static boolean at( int cell ){
		Blob b = Dungeon.level.blobs.get(ThiefLock.class);
		return b != null && b.volume > 0 && cell >= 0 && cell < b.cur.length && b.cur[cell] > 0
				&& Dungeon.level.map[cell] == Terrain.LOCKED_DOOR;
	}

	//살펴보기 창: 열쇠 안내 대신 몇 턴 뒤 열리는지 (WndInfoCell이 지형 설명 자리에 쓴다)
	public static String desc( int cell ){
		Blob b = Dungeon.level.blobs.get(ThiefLock.class);
		int left = b != null ? Math.max(1, b.cur[cell]) : 1;
		return Messages.get(ThiefLock.class, "desc", left);
	}
}
