/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower: 수액 방울 (5층 썩은 수액 덩어리, 명세 22번).
//불에 타서 다 쪼그라든(s4) 수액 덩어리가 주변에 흩뿌린다. 보스가 그 칸에 들어서면 흡수하고,
//2개를 흡수하면 기본 모습으로 돌아온다(Goo). 방울은 스스로 사라지지 않는다.
//(제안, 사용자 확인 필요: 주인공이 터뜨리기·불에 타기는 아직 넣지 않음)
//그림: (2026-11-21) 수액 웅덩이 그림 maze_fx5_sap_puddle (예전 maze_fx5_sap_drop 방울)
public class SapDrop extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				off[cell] = cur[cell];
				volume += off[cell];
			}
		}
	}

	public static boolean at( int cell ){
		SapDrop s = (SapDrop) Dungeon.level.blobs.get(SapDrop.class);
		return s != null && s.volume > 0 && cell >= 0 && cell < s.cur.length && s.cur[cell] > 0;
	}

	//그 칸의 방울을 없앤다
	public static void remove( int cell ){
		SapDrop s = (SapDrop) Dungeon.level.blobs.get(SapDrop.class);
		if (s != null) s.clear(cell);
	}

	//남은 방울을 모두 없앤다
	public static void removeAll(){
		SapDrop s = (SapDrop) Dungeon.level.blobs.get(SapDrop.class);
		if (s != null && s.cur != null){
			for (int i = 0; i < s.cur.length; i++) if (s.cur[i] > 0) s.clear(i);
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		//(결정, 2026-11-21) 방울 대신 수액 웅덩이 그림 (붙어 있으면 이어져 보임, 5층 보스의 수액 웅덩이와 같은 그림)
		emitter.add(new TileFxLayer(this, BossEffects.Fx.SAP_PUDDLE, 4, 0.85f).connect(BossEffects.Fx.SAP_PUDDLE_CONN));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
