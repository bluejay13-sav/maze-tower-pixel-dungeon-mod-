/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SoulMothSprite;

//Maze Tower (H4): 넋 나방 떼 (6~7층). 원작 파리떼처럼 맞으면 둘로 갈라진다.
//쓰러질 때마다 주인공에게 마나를 조금(H4_MOTH_MANA) 돌려준다. 갈라 놓을수록 거두는 마나가 늘어난다.
public class SoulMoth extends Swarm {

	{
		spriteClass = SoulMothSprite.class;
	}

	@Override
	protected Swarm newSwarm() {
		return new SoulMoth();
	}

	@Override
	public void die( Object cause ) {
		if (Dungeon.hero != null && Dungeon.hero.isAlive()){
			Mana m = Mana.get(Dungeon.hero);
			if (m != null){
				m.gain(MazeTower.H4_MOTH_MANA);
				if (Dungeon.hero.sprite != null && sprite != null && sprite.visible){
					TalentEffects.manaRefund(Dungeon.hero, this, MazeTower.H4_MOTH_MANA);
				}
			}
		}
		super.die( cause );
	}
}
