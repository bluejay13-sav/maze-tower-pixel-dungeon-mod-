/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

//Maze Tower (K4, 사용자): 15층 벌레의 마력 토사가 남긴 마력 증기. 마력 증기와 같지만, 뿌려진 칸에서 한 칸까지만 퍼진다
//(뿌려진 칸과 그 둘레 8칸 밖으로 번진 증기는 지운다). 피해·마력독·터짐·정화·면역은 마력 증기와 같다(ManaVapor를 물려받음).
public class SpitVapor extends ManaVapor {

	private boolean[] allowed;

	//이 칸에 뿌린다: 그 칸과 둘레 한 칸까지 퍼질 수 있게
	public static void spit( int cell, int amount ){
		SpitVapor b = Blob.seed(cell, amount, SpitVapor.class);
		b.allow(cell);
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(b);
	}

	private void allow( int cell ){
		if (allowed == null || allowed.length != com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.length()){
			allowed = new boolean[com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.length()];
		}
		allowed[cell] = true;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = cell + n;
			if (c >= 0 && c < allowed.length) allowed[c] = true;
		}
	}

	@Override
	protected void evolve() {
		super.evolve();
		if (allowed == null || cur == null) return;
		for (int c = 0; c < cur.length && c < allowed.length; c++){
			if (cur[c] > 0 && !allowed[c]){ volume -= cur[c]; cur[c] = 0; }
		}
		//증기가 다 사라지면 다음 토사를 위해 허용 칸을 비운다
		if (volume <= 0) allowed = null;
	}

	@Override
	public String tileDesc() {
		return Messages.get(ManaVapor.class, "desc", MazeTower.J1_LINGER);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		if (allowed != null) bundle.put("maze_allowed", allowed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("maze_allowed")) allowed = bundle.getBooleanArray("maze_allowed");
	}
}
