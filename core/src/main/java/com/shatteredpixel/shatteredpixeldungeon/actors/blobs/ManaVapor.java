/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Maze Tower: 마력 증기 (15층 마도 골렘 보스방). 광맥이 부서지면 그 자리에서 뿜어져 나와 15턴 동안 남는다.
//원작 독가스처럼 매 턴 피해를 주고, 주인공의 마나를 턴당 10 깎는다. 퍼지지 않는다.
//보스(마도 골렘)는 면역. 정화 물약(원작 BlobImmunity)을 마시면 면역. 그림: maze_fx15g_mana_vapor.png (반복)
public class ManaVapor extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				if (cur[cell] <= 0){
					off[cell] = 0;
					continue;
				}
				off[cell] = cur[cell] - 1;
				volume += off[cell];

				Char ch = Actor.findChar(cell);
				if (ch != null && !ch.isImmune(getClass())){
					int dmg = 1 + Dungeon.scalingDepth() / 5; //원작 독가스와 같은 피해
					ch.damage(dmg, this);
					if (ch instanceof Hero){
						Mana.get(ch).spend(Math.min(Mana.get(ch).cur(), MazeTower.VAPOR_MANA_DRAIN));
						if (!ch.isAlive()){
							Dungeon.fail(this);
							GLog.n(Messages.get(this, "ondeath"));
						}
					}
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new TileFxLayer(this, BossEffects.Fx.MANA_VAPOR, 6, 0.8f).shaded());
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
