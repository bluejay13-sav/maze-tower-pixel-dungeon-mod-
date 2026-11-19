/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//Maze Tower: 냉기 결정 (15층 수정 거인이 원작 독가스 대신 뿌린다). 퍼지지 않고 바닥에 남는다.
//결정 위에 서 있으면 냉기 (젖어 있으면 원소 반응으로 빙결). 수정 거인은 면역.
//결정에 불이 붙으면 증기가 터져 그 칸과 주변 1칸의 결정이 사라진다. 그림: maze_fx15c_frost_tile.png (반복)
public class FrostCrystals extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		Fire fire = (Fire) l.blobs.get(Fire.class);
		int cell;
		boolean[] burst = null;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				if (cur[cell] <= 0){
					off[cell] = 0;
					continue;
				}
				//불이 닿으면 증기 폭발: 주변 결정까지 사라진다
				if (fire != null && fire.volume > 0 && fire.cur[cell] > 0){
					if (burst == null) burst = new boolean[l.length()];
					burst[cell] = true;
					off[cell] = 0;
					continue;
				}
				off[cell] = cur[cell] - 1;
				volume += off[cell];

				Char ch = Actor.findChar(cell);
				if (ch != null && !(ch instanceof DM300) && !ch.flying && !ch.isImmune(getClass())){
					Buff.prolong(ch, Chill.class, MazeTower.FROST_CRYSTAL_CHILL);
				}
			}
		}
		if (burst != null){
			for (int c = 0; c < burst.length; c++){
				if (!burst[c]) continue;
				for (int n : PathFinder.NEIGHBOURS9){
					int k = c + n;
					if (k >= 0 && k < off.length && off[k] > 0){
						volume -= off[k];
						off[k] = 0;
					}
				}
				if (l.heroFOV[c]) BossEffects.atCell(BossEffects.Fx.STEAM, c, 12, false);
			}
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new TileFxLayer(this, BossEffects.Fx.FROST_TILE, 3, 0.9f));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}
