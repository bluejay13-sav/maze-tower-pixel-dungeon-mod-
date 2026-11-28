/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower (D13): 연막 깃의 연막. 원작 연막(SmokeScreen)처럼 시야를 가리지만(Level 시야 계산),
//퍼지지 않고 놓인 칸에서만 정해진 턴 동안 머문다. 밝은 빛깔(원작 김 입자)로 그린다.
public class FeatherSmoke extends Blob {

	@Override
	protected void evolve() {
		int w = Dungeon.level.width();
		for (int i = area.top; i < area.bottom; i++){
			for (int j = area.left; j < area.right; j++){
				int cell = j + i * w;
				int v = cur[cell] > 0 ? cur[cell] - 1 : 0; //퍼지지 않고 한 턴에 1씩 줄어든다
				off[cell] = v;
				volume += v;
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( Speck.factory( Speck.STEAM ), 0.12f );
	}

	@Override
	public String tileDesc() {
		return Messages.get(SmokeScreen.class, "desc");
	}
}
