/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

//Maze Tower: 횃불 (5층 보스방 입구 방에 하나). 한 번 던지면 사라지는 불붙이기 아이템.
//맞은 대상에게 화상을 걸고, 바닥·벽에 던지면 떨어진 자리에, 문·길을 막은 나무에 던지면 그곳에 불이 붙는다.
//썩은 수액 덩어리는 화상을 입으면(횃불이든 화염 마법이든) 약점이 드러난다 (Goo).
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

	//던진 길에서 횃불이 부딪힌 바로 다음 칸(길을 막은 나무·바리케이드 등). 타는 것이면 거기에도 불을 붙인다
	private int aimedBlock = -1;

	@Override
	public void cast( Hero user, int dst ) {
		aimedBlock = -1;
		Ballistica b = new Ballistica(user.pos, dst, Ballistica.PROJECTILE);
		int idx = b.path.indexOf(b.collisionPos);
		if (idx >= 0 && idx + 1 < b.path.size()){
			int next = b.path.get(idx + 1);
			if (Dungeon.level.flamable[next] && !Dungeon.level.passable[next]) aimedBlock = next;
		}
		super.cast(user, dst);
	}

	@Override
	protected void onThrow( int cell ) {
		//한 번 쓰면 사라진다
		Char ch = Actor.findChar(cell);
		if (ch != null && ch.isAlive()){
			Buff.affect(ch, Burning.class).reignite(ch);
		}
		//바닥, 그리고 문처럼 타는 칸에는 불이 붙는다
		if (!Dungeon.level.solid[cell] || Dungeon.level.flamable[cell]){
			GameScene.add(Blob.seed(cell, MazeTower.TORCH_FIRE, Fire.class));
		}
		//길을 막은 나무·바리케이드에 던졌으면 거기에도
		if (aimedBlock != -1 && Dungeon.level.flamable[aimedBlock]){
			GameScene.add(Blob.seed(aimedBlock, MazeTower.TORCH_FIRE, Fire.class));
			if (Dungeon.level.heroFOV[aimedBlock]) CellEmitter.get(aimedBlock).burst(FlameParticle.FACTORY, 12);
		}
		aimedBlock = -1;
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(FlameParticle.FACTORY, 12);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
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
}
