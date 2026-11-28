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
				if (ch != null && !(ch instanceof Goo) && !ch.flying && !ch.isImmune(getClass())){ //Maze Tower (E2): 정화 물약의 면역
					Buff.prolong(ch, Cripple.class, 1f);
				}
			}
		}
	}

	//Maze Tower: 수액이 물 위에 떨어지면 그 칸의 물을 없애고 수액이 대신한다 (물과 이어져 보이지 않게)
	@Override
	public void seed( Level level, int cell, int amount ) {
		if (onDoor( level, cell )) return; //(사용자) 문 칸에는 수액이 생기지 않는다
		super.seed( level, cell, amount );
		replaceWater( level, cell );
	}

	//(사용자) 문이 있는 칸 (열린 문·닫힌 문·잠긴 문·비밀 문·수정 문): 수액 웅덩이(SapFloor·SapPuddle)가 생기지 않는다
	public static boolean onDoor( Level level, int cell ){
		if (level == null || cell < 0 || cell >= level.length()) return false;
		int t = level.map[cell];
		return t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.DOOR
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.OPEN_DOOR
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.LOCKED_DOOR
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.HERO_LKD_DR
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.SECRET_DOOR
				|| t == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.CRYSTAL_DOOR;
	}

	public static void replaceWater( Level level, int cell ){
		if (level.map[cell] != com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WATER) return;
		Level.set( cell, com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.EMPTY, level );
		//지금 보고 있는 층이면 화면도 고친다 (층을 만드는 중이면 아직 화면이 없다)
		if (level == Dungeon.level) com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.updateMap( cell );
	}

	public static boolean at( int cell ){
		SapPuddle s = (SapPuddle) Dungeon.level.blobs.get(SapPuddle.class);
		return s != null && s.volume > 0 && cell >= 0 && cell < s.cur.length && s.cur[cell] > 0;
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new TileFxLayer(this, BossEffects.Fx.SAP_PUDDLE, 4, 0.85f).connect(BossEffects.Fx.SAP_PUDDLE_CONN)); //Maze Tower: 붙어 있는 수액은 이어져 보인다
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

}
