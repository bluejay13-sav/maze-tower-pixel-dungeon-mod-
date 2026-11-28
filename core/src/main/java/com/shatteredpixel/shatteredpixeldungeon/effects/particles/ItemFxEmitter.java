/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;

//Maze Tower: 아이템 그림 위에 붙는 이미터. 가방에서 그림을 키워 보일 때(InventorySlot) 입자 자리도 같은 배율로 맞추도록
//붙은 그림의 배율을 알려 준다.
public class ItemFxEmitter extends Emitter {

	public float targetScale(){
		return target == null ? 1f : target.scale.x;
	}

	public static float scaleOf( Emitter e ){
		return e instanceof ItemFxEmitter ? ((ItemFxEmitter) e).targetScale() : 1f;
	}
}
