/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

//Maze Tower (D15): 덫 놓기 - 던지는 무기를 꽂아 만든 덫. 칸마다 꽂힌 무기를 기억한다.
//스스로 사라지지 않는다. 적이 그 칸에 들어서면(Level.occupyCell → BloomHuntress) 터지고 무기는 그 칸에 떨어진다.
//주인공은 밟아도 터지지 않는다.
public class SnareTrap extends Blob {

	public final HashMap<Integer, Item> weapons = new HashMap<>();

	@Override
	protected void evolve() {
		int w = Dungeon.level.width();
		for (int i = area.top; i < area.bottom; i++){
			for (int j = area.left; j < area.right; j++){
				int cell = j + i * w;
				off[cell] = cur[cell]; //줄어들지 않는다
				volume += off[cell];
			}
		}
	}

	public void plant( int cell, Item weapon ){
		weapons.put(cell, weapon);
	}

	//그 칸의 덫을 거둔다 (꽂혀 있던 무기를 돌려준다)
	public Item spring( int cell ){
		clear(cell);
		return weapons.remove(cell);
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( LeafParticle.GENERAL, 0.8f );
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	private static final String CELLS = "snare_cells", ITEMS = "snare_items";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		int[] cells = new int[weapons.size()];
		ArrayList<Item> items = new ArrayList<>();
		int i = 0;
		for (Integer c : weapons.keySet()){
			cells[i++] = c;
			items.add(weapons.get(c));
		}
		bundle.put(CELLS, cells);
		bundle.put(ITEMS, items);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		weapons.clear();
		if (!bundle.contains(CELLS)) return;
		int[] cells = bundle.getIntArray(CELLS);
		Collection<Bundlable> items = bundle.getCollection(ITEMS);
		int i = 0;
		for (Bundlable b : items){
			if (i < cells.length && b instanceof Item) weapons.put(cells[i], (Item)b);
			i++;
		}
	}
}
