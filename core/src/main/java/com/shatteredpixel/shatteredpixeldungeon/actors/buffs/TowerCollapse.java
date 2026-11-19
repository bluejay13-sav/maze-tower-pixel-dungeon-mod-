/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.RetreatEffects;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 탑의 붕괴 (명세 14번). 역행 중 주인공에게 붙는다.
//층에 들어설 때마다 남은 턴이 새로 시작하고(아래층일수록 적다), 0이 되면 낙석이 떨어지기 시작한다.
//낙석은 시간이 갈수록, 그리고 아래층일수록 잦아진다. 남은 턴은 버프 아이콘 숫자로 보인다.
//먼지와 피어나는 꽃 연출도 여기서 낸다.
public class TowerCollapse extends Buff {

	{
		type = buffType.NEGATIVE;
		revivePersists = true;
	}

	private int depth = -1;
	private int turnsLeft = 0;
	private float sinceCollapse = 0;   //붕괴가 시작된 뒤 지난 턴
	private float rockCounter = 0;     //다음 낙석까지 쌓인 턴
	private int lastHeroPos = -1;

	public void startFloor( int depth ){
		if (this.depth == depth) return;
		this.depth = depth;
		turnsLeft = MazeTower.collapseTurns(depth);
		sinceCollapse = 0;
		rockCounter = 0;
		GLog.w(Messages.get(this, "floor", turnsLeft));
		BuffIndicator.refreshHero();
	}

	//치트: 바로 붕괴 시작
	public void cheatCollapse(){
		turnsLeft = 1;
		sinceCollapse = 0;
	}

	public boolean collapsing(){
		return turnsLeft <= 0;
	}

	@Override
	public boolean act() {
		if (depth != Dungeon.depth) startFloor(Dungeon.depth);

		if (turnsLeft > 0){
			turnsLeft--;
			if (turnsLeft == 0){
				GLog.n(Messages.get(this, "collapse"));
				PixelScene.shake(3, 1f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			}
		} else {
			sinceCollapse += TICK;
			rockCounter += TICK;
			float interval = Math.max(MazeTower.ROCK_MIN_INTERVAL,
					MazeTower.ROCK_START_INTERVAL - (int)(sinceCollapse / MazeTower.ROCK_SPEEDUP_EVERY));
			interval /= MazeTower.rockFrequency(Dungeon.depth);
			while (rockCounter >= interval){
				rockCounter -= interval;
				dropRock();
			}
		}

		//먼지 (장식)
		if (Random.Float() < MazeTower.DUST_CHANCE){
			ArrayList<Integer> near = RetreatEffects.floorCellsNear(target.pos, 3);
			if (!near.isEmpty()) RetreatEffects.dust(Random.element(near));
		}
		//주인공이 지나간 근처에서 꽃이 피어난다
		if (lastHeroPos != -1 && lastHeroPos != target.pos && Random.Float() < MazeTower.BLOOM_CHANCE){
			int cell = lastHeroPos;
			if (RetreatEffects.isFloor(Dungeon.level, cell) && Actor.findChar(cell) == null){
				RetreatEffects.bloom(cell);
			}
		}
		lastHeroPos = target.pos;

		BuffIndicator.refreshHero();
		spend(TICK);
		return true;
	}

	//주인공 주변 몇 칸 안의 칸 하나에 경고 그림자 → 다음 턴에 돌이 떨어진다
	private void dropRock(){
		ArrayList<Integer> cells = RetreatEffects.floorCellsNear(target.pos, MazeTower.ROCK_RANGE);
		if (cells.isEmpty()) return;
		ArrayList<Integer> pos = new ArrayList<>();
		pos.add(Random.element(cells));
		Buff.append(target, TowerRockFall.class, MazeTower.ROCK_WARN_TURNS).setRockPositions(pos);
	}

	@Override
	public int icon() {
		return BuffIndicator.TIME;
	}

	@Override
	public void tintIcon(Image icon) {
		if (turnsLeft > 0) icon.hardlight(1f, 0.75f, 0.4f);
		else               icon.hardlight(1f, 0.2f, 0.2f);
	}

	@Override
	public float iconFadePercent() {
		if (turnsLeft <= 0 || depth < 0) return 0;
		float total = MazeTower.collapseTurns(depth);
		return Math.max(0, (total - turnsLeft) / total);
	}

	@Override
	public String iconTextDisplay() {
		return turnsLeft > 0 ? Integer.toString(turnsLeft) : "!";
	}

	@Override
	public String desc() {
		if (turnsLeft > 0) return Messages.get(this, "desc", turnsLeft);
		return Messages.get(this, "desc_collapsing");
	}

	private static final String DEPTH  = "depth";
	private static final String LEFT   = "turns_left";
	private static final String SINCE  = "since_collapse";
	private static final String ROCKS  = "rock_counter";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DEPTH, depth);
		bundle.put(LEFT, turnsLeft);
		bundle.put(SINCE, sinceCollapse);
		bundle.put(ROCKS, rockCounter);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		depth = bundle.getInt(DEPTH);
		turnsLeft = bundle.getInt(LEFT);
		sinceCollapse = bundle.getFloat(SINCE);
		rockCounter = bundle.getFloat(ROCKS);
	}

	//낙석 하나: 경고 그림자를 보여 주다가 떨어진다. 피해는 원작 DM-300 낙석과 같다(마비는 없음).
	public static class TowerRockFall extends FlavourBuff {

		private int[] cells;
		private final ArrayList<Visual> warnings = new ArrayList<>();

		public TowerRockFall setRockPositions( java.util.List<Integer> rockPositions ) {
			cells = new int[rockPositions.size()];
			for (int i = 0; i < cells.length; i++) cells[i] = rockPositions.get(i);
			fx(true);
			return this;
		}

		@Override
		public boolean act() {
			fx(false);
			if (cells != null){
				for (int c : cells){
					RetreatEffects.rockFall(c);
					Char ch = Actor.findChar(c);
					if (ch != null) hit(ch);
				}
				PixelScene.shake(2, 0.4f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f);
			}
			detach();
			return true;
		}

		private void hit( Char ch ){
			ch.damage(Random.NormalIntRange(MazeTower.ROCK_DMG_MIN, MazeTower.ROCK_DMG_MAX), this);
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(TowerCollapse.class, "rock_kill"));
			}
		}

		@Override
		public void fx( boolean on ) {
			if (on && cells != null && warnings.isEmpty()){
				for (int c : cells) warnings.add(RetreatEffects.warn(c));
			} else if (!on){
				for (Visual v : warnings) v.killAndErase();
				warnings.clear();
			}
		}

		private static final String CELLS = "cells";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			if (cells != null) bundle.put(CELLS, cells);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			if (bundle.contains(CELLS)) cells = bundle.getIntArray(CELLS);
		}
	}
}
