/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.quest;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;
import com.watabou.utils.Bundle;

//Maze Tower (M1): 시든 세계수 잎 브로치. 5층 현자가 망령 왕 또는 마도시의 여제에게 전해 달라며 준다. 능력은 아직 없다(자리만).
public class SageBrooch extends Item {

	{
		image = MazeItemSprites.SAGE_BROOCH;
		unique = true;
		bones = false;
	}

	public boolean empress = false; //누구에게 전할 것인지

	@Override
	public boolean isUpgradable() { return false; }

	@Override
	public boolean isIdentified() { return true; }

	@Override
	public String desc() {
		String s = Messages.get( this, empress ? "desc_empress" : "desc_wraith" );
		//(사용자) 현자에게 보고하면 브로치가 강화된다
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SageQuest q = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero == null ? null
				: com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.buff( com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SageQuest.class );
		if (q != null && q.reported) s += "\n\n" + Messages.get( this, "desc_bloomed" );
		return s;
	}

	private static final String EMPRESS = "empress";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( EMPRESS, empress );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		empress = bundle.getBoolean( EMPRESS );
	}
}
