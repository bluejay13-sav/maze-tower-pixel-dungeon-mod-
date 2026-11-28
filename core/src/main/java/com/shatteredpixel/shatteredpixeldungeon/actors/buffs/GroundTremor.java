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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (J9): 땅울림 (13·14층). 15층 보스가 땅 밑을 지나가는 것.
//그 층에 있는 동안 J9_EVERY_MIN~MAX턴마다 한 번, 주인공 가까이(J9_NEAR칸 안)를 지나는 한 줄(가로·세로·대각선, 최대 9칸)에 금이 간다(예고, 그림 sprites/maze_tremor_crack.png).
//주인공이 한 번 움직일 틈이 있고, 그다음 이 상태가 행동할 때 그 줄 위의 모두(주인공·적·아군)가 피해 J9_DMG_MIN~MAX(방어 무시)를 받는다.
//주인공에게 붙는 보이지 않는 상태(아이콘 없음). Mana.turnRegen에서 13·14층이면 붙인다. 저장됨.
public class GroundTremor extends Buff {

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int depth = -1;
	private int countdown = -1;
	private int dir = 0;                         //금 방향: 0 가로, 1 세로, 2 "\", 3 "/"
	private ArrayList<Integer> line = null;      //예고한 줄
	private CrackFx fx;

	public static void ensure( Hero hero ){
		if (!MazeTower.J9_TREMOR || hero == null || Dungeon.level == null) return;
		if ((Dungeon.depth == 13 || Dungeon.depth == 14) && !Dungeon.bossLevel() && hero.buff(GroundTremor.class) == null){
			Buff.affect(hero, GroundTremor.class);
		}
	}

	@Override
	public boolean act() {
		if (Dungeon.depth != 13 && Dungeon.depth != 14){
			clearLine();
			depth = -1;
			spend(TICK);
			return true;
		}
		if (depth != Dungeon.depth){
			clearLine();
			depth = Dungeon.depth;
			countdown = Random.IntRange(MazeTower.J9_EVERY_MIN, MazeTower.J9_EVERY_MAX);
		}
		if (line != null){
			strike();
			clearLine();
			countdown = Random.IntRange(MazeTower.J9_EVERY_MIN, MazeTower.J9_EVERY_MAX);
		} else if (--countdown <= 0){
			if (pickLine()){
				showFx();
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.5f, 0.7f);
				GLog.w(Messages.get(GroundTremor.class, "rumble"));
			} else {
				countdown = 5; //줄을 못 그으면 조금 뒤에 다시
			}
		}
		spend(TICK);
		return true;
	}

	private static final int[][] DIRS = { {1,0}, {0,1}, {1,1}, {1,-1} };

	private boolean pickLine(){
		Level l = Dungeon.level;
		int w = l.width();
		int hp = target.pos;
		for (int tries = 0; tries < 30; tries++){
			int ox = Random.IntRange(-MazeTower.J9_NEAR, MazeTower.J9_NEAR), oy = Random.IntRange(-MazeTower.J9_NEAR, MazeTower.J9_NEAR);
			int c = hp + ox + oy * w;
			if (c < 0 || c >= l.length() || !floor(l, c)) continue;
			int d = Random.Int(4);
			int step = DIRS[d][0] + DIRS[d][1] * w;
			ArrayList<Integer> cells = new ArrayList<>();
			cells.add(c);
			for (int sgn = -1; sgn <= 1; sgn += 2){
				int p = c;
				for (int i = 0; i < MazeTower.J9_HALF_LEN; i++){
					int n = p + sgn * step;
					if (n < 0 || n >= l.length() || !floor(l, n) || Math.abs(n % w - p % w) > 1) break;
					if (sgn < 0) cells.add(0, n); else cells.add(n);
					p = n;
				}
			}
			if (cells.size() < 4) continue;
			line = cells;
			dir = d;
			return true;
		}
		return false;
	}

	private static boolean floor( Level l, int c ){
		return l.passable[c] && !l.solid[c] && !l.pit[c];
	}

	private void strike(){
		boolean seen = false;
		for (int c : line){
			if (Dungeon.level.heroFOV[c]){
				seen = true;
				CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
			}
			Char ch = Actor.findChar(c);
			if (ch == null || !ch.isAlive()) continue;
			ch.damage(Random.NormalIntRange(MazeTower.J9_DMG_MIN, MazeTower.J9_DMG_MAX), new Hit());
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(Hit.class);
				GLog.n(Messages.get(GroundTremor.class, "ondeath"));
			}
		}
		if (seen){
			PixelScene.shake(1, 0.15f);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.8f);
			GLog.w(Messages.get(GroundTremor.class, "strike"));
		}
	}

	//치트: 다음 행동에 바로 금이 가게
	public void triggerSoon(){ if (line == null) countdown = 1; }

	//사망 기록용 피해 출처
	public static class Hit {}

	private void clearLine(){
		line = null;
		if (fx != null){ fx.fadeOut(); fx = null; }
	}

	private void showFx(){
		if (line == null) return;
		if (fx != null) fx.fadeOut();
		fx = new CrackFx(line, dir);
		GameScene.groupFloor(fx);
	}

	@Override
	public void fx( boolean on ) {
		if (on && line != null && fx == null) showFx();
		if (!on && fx != null){ fx.fadeOut(); fx = null; }
	}

	@Override
	public void detach() {
		clearLine();
		super.detach();
	}

	private static final String DEPTH = "depth", COUNTDOWN = "countdown", DIR = "dir", LINE = "line";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(DEPTH, depth);
		bundle.put(COUNTDOWN, countdown);
		bundle.put(DIR, dir);
		if (line != null){
			int[] a = new int[line.size()];
			for (int i = 0; i < a.length; i++) a[i] = line.get(i);
			bundle.put(LINE, a);
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		depth = bundle.getInt(DEPTH);
		countdown = bundle.getInt(COUNTDOWN);
		dir = bundle.getInt(DIR);
		if (bundle.contains(LINE)){
			line = new ArrayList<>();
			for (int c : bundle.getIntArray(LINE)) line.add(c);
		}
	}

	//금 그림: 줄의 칸마다 방향에 맞는 금(16x16), 천천히 진해졌다 옅어짐
	public static class CrackFx extends Group {
		private final ArrayList<Image> cracks = new ArrayList<>();
		private float time = 0, fade = 1f, appear = 0f;
		private boolean fading = false;

		public CrackFx( ArrayList<Integer> cells, int dir ){
			for (int c : cells){
				Image im = new Image("sprites/maze_tremor_crack.png", dir * 16, 0, 16, 16);
				PointF p = DungeonTilemap.tileToWorld(c);
				im.x = p.x; im.y = p.y;
				add(im);
				cracks.add(im);
			}
			update();
		}

		public void fadeOut(){ fading = true; }

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			appear = Math.min(1f, appear + Game.elapsed / 0.3f);
			if (fading){ fade -= Game.elapsed / 0.3f; if (fade <= 0){ killAndErase(); return; } }
			float a = appear * fade * (0.9f + 0.05f * (float)Math.sin(time * 1.5f)); //(사용자) 반짝이지 않게 아주 느리게 조금만
			for (Image im : cracks){
				im.alpha(a);
				//주인공이 볼 수 있는 칸만
				int cell = (int)(im.x / 16) + (int)(im.y / 16) * Dungeon.level.width();
				im.visible = cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
			}
		}
	}
}
