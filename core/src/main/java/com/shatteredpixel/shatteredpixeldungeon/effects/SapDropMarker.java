/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SapDroplet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

//Maze Tower (G3): 5층 보스가 향하는 수액 방울 표시. 안개 위(GameScene.effectOverFog)에 그려 시야 밖에서도 보인다.
//(결정) 방울 그림을 흐릿하게 + 칸 둘레에 숨 쉬듯 빛나는 호박색 테두리(모서리는 1픽셀 둥글게).
public class SapDropMarker extends Group {

	private static final int S = DungeonTilemap.SIZE;
	private static final int COLOR = 0xF5BF55;

	private final SapDroplet drop;
	private final Image ghost;
	private float time = 0;

	public SapDropMarker( SapDroplet drop ){
		this.drop = drop;
		int w = Dungeon.level.width();
		float x = (drop.pos % w) * S, y = (drop.pos / w) * S;

		ghost = new Image( "sprites/maze_sapdrop.png" );
		ghost.frame( 0, 0, 16, 16 );
		ghost.x = x; ghost.y = y;
		add( ghost );

		//테두리: 칸 안쪽 1픽셀, 네 모서리는 1픽셀씩 비운다
		line( x + 1, y,         S - 2, 1 );
		line( x + 1, y + S - 1, S - 2, 1 );
		line( x,         y + 1, 1, S - 2 );
		line( x + S - 1, y + 1, 1, S - 2 );
	}

	private void line( float x, float y, float w, float h ){
		ColorBlock b = new ColorBlock( w, h, 0xFF000000 | COLOR );
		b.x = x; b.y = y;
		add( b );
	}

	@Override
	public void update() {
		super.update();
		if (drop == null || !drop.isAlive() || !drop.targeted()){
			killAndErase();
			return;
		}
		time += Game.elapsed;
		float breath = 0.55f + 0.35f * (float)Math.sin( time * 4f );
		boolean seen = Dungeon.level.heroFOV[drop.pos];
		//보이는 칸에서는 진짜 방울이 보이므로 흐릿한 그림은 숨기고 테두리만
		ghost.visible = !seen;
		ghost.alpha( 0.55f );
		for (int i = 0; i < length; i++){
			if (members.get(i) instanceof ColorBlock) ((ColorBlock)members.get(i)).alpha( breath );
		}
	}
}
