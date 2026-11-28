/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

//Maze Tower (O3): 25층 탑의 주인의 뿌리 창·가지 부채 그림 (사용자 확인).
//sprites/maze_b25_spikes.png (16×24, 줄 0 뿌리 창 6칸 / 줄 1 휩쓰는 가지 7칸, 원본 docs/sprites/boss25/make_spikes25.ps1, 움짤 anim_spikes.html).
//칸 아래 16줄 + 위로 8줄 솟는다. delay초 뒤 한 번 재생하고 사라진다. 휩쓰는 가지는 오른쪽으로 휩쓰는 그림이라 왼쪽으로 휩쓸 때 좌우를 뒤집는다.
//시간(초)으로 재생하므로 화면 프레임과 상관없이 미리보기와 같은 빠르기.
public class MasterSpikeFx extends Image {

	public static final String TEX = "sprites/maze_b25_spikes.png";
	public static final int ROOT = 0, WHIP = 1;

	//뿌리 창: 갈라짐 → 솟음 → 다 솟음(0.6초 머묾, 흔들리지 않음) → 들어감 → 닫힘 / 휩쓰는 가지: 갈라짐 → 젖혀 솟음 → 휘둘러 곧게 → 넘어감 → 들어감 → 닫힘
	private static final int[] ORDER_ROOT = {0, 1, 2, 2, 2, 2, 2, 2, 4, 5};
	private static final int[] ORDER_WHIP = {0, 1, 2, 3, 4, 4, 5, 6};

	private final int row, cell;
	private final int[] order;
	private final float fps;
	private float t;
	private final TextureFilm film;

	public MasterSpikeFx( int cell, int row, float delay, boolean flip ){
		super(TEX);
		this.cell = cell;
		this.row = row;
		this.order = row == ROOT ? ORDER_ROOT : ORDER_WHIP;
		this.fps = row == ROOT ? MazeTower.O_SPIKE_FPS : MazeTower.O_WHIP_FPS;
		this.t = -delay;
		film = new TextureFilm(texture, 16, 24);
		PointF p = DungeonTilemap.tileToWorld(cell);
		x = p.x;
		y = p.y - 8;
		if (flip){
			origin.set(8, 12);
			scale.x = -1;
		}
		visible = false;
		frame(film.get(row * 7 + order[0]));
	}

	@Override
	public void update() {
		super.update();
		t += Game.elapsed;
		if (t < 0){ visible = false; return; }
		int i = (int)(t * fps);
		if (i >= order.length){ killAndErase(); return; }
		frame(film.get(row * 7 + order[i]));
		visible = Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}
}
