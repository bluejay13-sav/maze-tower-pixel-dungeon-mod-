/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;

//Maze Tower: 부적 철창 (10층 그림자 간수 보스방). 그림자 간수가 '영혼 해방'을 쓰면 철창에서 죄수의 영혼이 나온다.
//철창을 부수면 그 철창에서는 더는 영혼을 불러내지 못한다. 움직이지도 공격하지도 않고, 반드시 맞는다.
//그림: core/src/main/assets/sprites/maze_p14_cage.png
public class SoulCage extends Mob {

	{
		spriteClass = CageSprite.class;

		HP = HT = MazeTower.CAGE_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
		alignment = Alignment.NEUTRAL;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	@Override
	protected boolean act() {
		if (sprite != null){
			sprite.hideAlert();
			sprite.hideLost();
		}
		spend(TICK);
		return true;
	}

	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public boolean add( Buff buff ) { return false; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

	@Override
	public void die( Object cause ) {
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 6);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		}
		super.die(cause);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", HP, HT);
	}

	public static class CageSprite extends MobSprite {
		public CageSprite(){
			super();
			texture("sprites/maze_p14_cage.png");
			TextureFilm f = new TextureFilm(texture, 15, 15);
			idle = new Animation(1, true);
			idle.frames(f, 0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = idle.clone();
			play(idle);
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == die){
				killAndErase();
				return;
			}
			super.onComplete(anim);
		}
	}
}
