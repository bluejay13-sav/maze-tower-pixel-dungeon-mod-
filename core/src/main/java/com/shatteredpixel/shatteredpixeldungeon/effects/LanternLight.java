/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//Maze Tower (H1): 봉인 등롱이 던지는 부채꼴 빛. 빛이 닿는 바닥 칸을 아주 옅은 노란빛으로 칠한다.
//(결정) 주인공이 보고 있는 칸에는 전체 범위, 시야 밖에는 등롱 가까이의 축소된 범위만 보인다. 시야가 바뀌면 칸마다 서서히 나타나고 사라진다.
//(결정) 눈이 아프지 않게 은은하게, 빛이 닿는 칸은 모두 같은 밝기(끝자락도 똑같이 발각되므로).
//빛이 돌거나 옮겨 갈 때 칸을 한꺼번에 지웠다 그리지 않고 칸마다 서서히 밝아지고 서서히 사라진다 (깜빡임 없음).
public class LanternLight extends Group {

	private static final int S = DungeonTilemap.SIZE;
	private static final int COLOR = 0xFFE07A;
	private static final float FADE_SPEED = 0.25f; //초당 바뀌는 밝기 (가장 밝은 칸이 0.4초쯤에 다 바뀜)

	private static class Cell {
		ColorBlock block;
		float alpha = 0, target = 0;
	}

	private final HashMap<Integer, Cell> cells = new HashMap<>();

	//시야 밖에서도 보이는 칸 (등롱 가까이, 축소된 범위)
	private java.util.Set<Integer> nearCells = new java.util.HashSet<>();

	//Maze Tower (I2): 10층 간수의 등불은 빛이 닿는 칸이 늘 보인다(showAll). 다음 턴 예고는 칸 가운데의 작은 점(inset)으로 그린다
	private boolean showAll = false;
	private int inset = 0;
	private float fadeSpeed = FADE_SPEED;

	public LanternLight(){ }

	public LanternLight( boolean showAll, int inset, float fadeSpeed ){
		this.showAll = showAll;
		this.inset = inset;
		this.fadeSpeed = fadeSpeed;
	}

	//모든 칸을 바로 지운다 (방이 바뀔 때)
	public void clearNow(){
		for (Cell c : cells.values()) c.block.killAndErase();
		cells.clear();
	}

	//칸 → 밝기(0~1, 실제 투명도), near: 시야 밖이어도 보일 칸. 목록에 없는 칸은 서서히 사라진다
	public void set( Map<Integer, Float> light, java.util.Set<Integer> near ){
		nearCells = near;
		int w = Dungeon.level.width();
		for (Cell c : cells.values()) c.target = 0;
		for (Map.Entry<Integer, Float> e : light.entrySet()){
			Cell c = cells.get(e.getKey());
			if (c == null){
				c = new Cell();
				c.block = new ColorBlock( S - 2*inset, S - 2*inset, 0xFF000000 | COLOR );
				c.block.x = (e.getKey() % w) * S + inset;
				c.block.y = (e.getKey() / w) * S + inset;
				c.block.alpha( 0 );
				add( c.block );
				cells.put(e.getKey(), c);
			}
			c.target = e.getValue();
		}
	}

	@Override
	public void update() {
		super.update();
		float step = fadeSpeed * Game.elapsed;
		Iterator<Map.Entry<Integer, Cell>> it = cells.entrySet().iterator();
		boolean[] fov = Dungeon.level != null ? Dungeon.level.heroFOV : null;
		while (it.hasNext()){
			Map.Entry<Integer, Cell> e = it.next();
			Cell c = e.getValue();
			//(결정) 주인공이 보고 있는 칸에는 전체 범위, 시야 밖에는 등롱 가까이의 축소된 범위만
			int cell = e.getKey();
			boolean seen = showAll || (fov != null && cell < fov.length && fov[cell]) || nearCells.contains(cell);
			float want = seen ? c.target : 0;
			if (c.alpha < want)      c.alpha = Math.min(want, c.alpha + step);
			else if (c.alpha > want) c.alpha = Math.max(want, c.alpha - step);
			c.block.alpha( c.alpha );
			if (c.target <= 0 && c.alpha <= 0.002f){
				c.block.killAndErase();
				it.remove();
			}
		}
	}

	private boolean fading = false;

	//다 사라진 뒤에 지운다 (등롱이 쓰러졌을 때)
	public void fadeAway(){
		fading = true;
		for (Cell c : cells.values()) c.target = 0;
	}

	@Override
	public synchronized void draw() {
		if (fading && cells.isEmpty()){
			killAndErase();
			return;
		}
		super.draw();
	}
}
