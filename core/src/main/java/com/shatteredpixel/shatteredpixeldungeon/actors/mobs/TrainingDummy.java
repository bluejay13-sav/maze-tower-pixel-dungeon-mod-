/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.watabou.utils.Bundle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

import java.util.ArrayList;

//Maze Tower: 테스트용 허수아비 (디버그 빌드에서만 1층 시작 지점에 3개).
//움직이지도 공격하지도 않는다. 상태이상·밀침은 받는다. 맞지 않고 몇 턴이 지나면 체력이 다시 가득 찬다.
//주인공을 늘 보고 있는 것으로 친다 (기습·"나를 보지 못한 적" 판정이 켜지지 않게).
public class TrainingDummy extends Mob {

	{
		spriteClass = DummySprite.class;

		HP = HT = MazeTower.DUMMY_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;
		state = PASSIVE;
		alignment = Alignment.ENEMY;
		//(테스트용) 무기물 속성을 주지 않는다: 출혈·독 같은 상태이상도 시험할 수 있게
	}

	private float lastHit = 0;

	@Override
	public int damageRoll() {
		return 0;
	}

	@Override
	public int attackSkill( Char target ) {
		return 0;
	}

	@Override
	public void damage( int dmg, Object src ) {
		//(결정) 허수아비는 죽지 않는다: 체력 1 아래로 내려가지 않는다
		if (dmg >= HP) dmg = HP - 1;
		super.damage(Math.max(0, dmg), src);
		if (HP < 1) HP = 1;
		lastHit = Actor.now();
	}

	@Override
	public void die( Object cause ) {
		//죽지 않는다 (즉사 효과 등으로 불려도 체력 1로 남는다)
		HP = Math.max(1, HP);
	}

	@Override
	protected boolean act() {
		if (isAlive() && HP < HT && Actor.now() - lastHit >= MazeTower.DUMMY_REGEN_TURNS){
			HP = HT;
			if (sprite != null && sprite.visible) sprite.showStatus(CharSprite.POSITIVE, Integer.toString(HT));
		}
		return super.act();
	}

	@Override
	public boolean surprisedBy( Char enemy, boolean attacking ) {
		return false;
	}

	@Override
	public Item createLoot() {
		return null;
	}

	private static final String LAST_HIT = "last_hit";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(LAST_HIT, lastHit);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lastHit = bundle.getFloat(LAST_HIT);
	}

	//짚 빛깔 석상 그림 (원작 석상 그림을 빌린다)
	public static class DummySprite extends StatueSprite {
		public DummySprite(){
			super();
			hardlight(1f, 0.85f, 0.5f);
		}
		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.85f, 0.5f);
		}
	}

	//1층 시작 지점(주인공 둘레 2~3칸)에 허수아비 3개. 디버그 빌드에서만, 이미 있으면 놓지 않는다.
	public static void placeAtStart(){
		if (!com.watabou.utils.DeviceCompat.isDebug() || MazeTower.BOSS_TRIAL) return;
		if (Dungeon.depth != 1 || Dungeon.branch != 0 || Dungeon.hero == null) return;
		for (Mob m : Dungeon.level.mobs) if (m instanceof TrainingDummy) return;

		int from = Dungeon.hero.pos;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int d = 2; d <= 4 && cells.size() < MazeTower.DUMMY_COUNT; d++){
			for (int c = 0; c < Dungeon.level.length() && cells.size() < MazeTower.DUMMY_COUNT; c++){
				if (Dungeon.level.distance(from, c) != d) continue;
				if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Dungeon.level.pit[c]) continue;
				if (c == Dungeon.level.entrance() || c == Dungeon.level.exit()) continue;
				if (Actor.findChar(c) != null || !Dungeon.level.heroFOV[c]) continue;
				//서로 붙여 놓는다 (번지는 반응 등 주변 칸 효과를 보기 쉽게): 두 번째부터는 앞 것과 이웃한 칸만
				if (!cells.isEmpty()){
					boolean nearOne = false;
					for (int o : cells) if (Dungeon.level.adjacent(o, c)) nearOne = true;
					if (!nearOne) continue;
				}
				cells.add(c);
			}
		}
		for (int c : cells){
			TrainingDummy t = new TrainingDummy();
			t.pos = c;
			GameScene.add(t);
		}
	}
}
