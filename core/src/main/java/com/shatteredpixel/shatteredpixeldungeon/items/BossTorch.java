/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

//Maze Tower: 5층 보스방의 횃불 (기획 문서 5층 약점). 맵 어딘가에 하나 있다.
//주워서 썩은 수액 덩어리에게 던지면, 보스는 체력이 절반이 될 때까지 계속 불타고
//방어력이 떨어지며(물리 피해 감소가 사라짐) 더는 수액을 만들지 못한다. 빗나가면 그 자리에 떨어진다.
//그림: core/src/main/assets/sprites/maze_p13_torch.png
public class BossTorch extends Item {

	{
		image = MazeItemSprites.BOSS_TORCH;
		defaultAction = AC_THROW;
		unique = true;
		bones = false;
	}

	private static final ItemSprite.Glowing FLAME = new ItemSprite.Glowing(0xFF8833, 1.2f);

	@Override
	public ItemSprite.Glowing glowing() {
		return FLAME;
	}

	@Override
	public Emitter emitter() {
		Emitter e = new Emitter();
		e.pos(6, 0, 4, 4);
		e.fillTarget = false;
		e.pour(FlameParticle.FACTORY, 0.15f);
		return e;
	}

	@Override
	protected void onThrow( int cell ) {
		Char ch = Actor.findChar(cell);
		if (ch instanceof Goo && ch.isAlive() && ((Goo)ch).torchHit()){
			//보스에게 맞으면 횃불은 불타 사라진다
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
			return;
		}
		super.onThrow(cell);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0;
	}

	@SuppressWarnings("unused")
	private static boolean onBossFloor(){
		return Dungeon.depth == 5;
	}
}
