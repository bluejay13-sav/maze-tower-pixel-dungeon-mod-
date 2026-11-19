/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower: 끈적한 수액 웅덩이 (5층 썩은 수액 덩어리). 보스가 내려칠 때 주변에 남는다.
//수액 위의 다른 대상은 움직임이 느려지고(원작 다리 저림, 1턴씩 갱신), 보스는 수액 위에서 물처럼 회복한다.
//불이 붙으면 수액은 타서 사라진다. 그림: maze_fx5_sap_puddle.png (반복)
public class SapPuddle extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		Fire fire = (Fire) l.blobs.get(Fire.class);
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				if (cur[cell] <= 0){
					off[cell] = 0;
					continue;
				}
				//불에 타서 사라진다
				if (fire != null && fire.volume > 0 && fire.cur[cell] > 0){
					off[cell] = 0;
					continue;
				}
				off[cell] = cur[cell] - 1;
				volume += off[cell];

				Char ch = Actor.findChar(cell);
				if (ch != null && !(ch instanceof Goo) && !ch.flying){
					Buff.prolong(ch, Cripple.class, 1f);
				}
			}
		}
	}

	public static boolean at( int cell ){
		SapPuddle s = (SapPuddle) Dungeon.level.blobs.get(SapPuddle.class);
		return s != null && s.volume > 0 && cell >= 0 && cell < s.cur.length && s.cur[cell] > 0;
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new TileFxLayer(this, BossEffects.Fx.SAP_PUDDLE, 4, 0.85f));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

}
