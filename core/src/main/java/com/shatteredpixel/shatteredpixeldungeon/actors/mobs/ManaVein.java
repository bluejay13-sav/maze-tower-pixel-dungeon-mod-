/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//Maze Tower: 광맥 (15층 마도 골렘 보스방, 원작 기둥 대신). 부서지면 마력 증기를 뿜는다(그 칸과 주변 1칸, 15턴).
//마도 골렘의 5×5 내려치기 범위 안에 있으면 바로 부서진다. 주인공도 때려서 부술 수 있다.
//그림: core/src/main/assets/sprites/maze_p15_vein.png
public class ManaVein extends Mob {

	{
		spriteClass = VeinSprite.class;

		HP = HT = MazeTower.VEIN_HP;
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

	@Override public boolean heroShouldInteract() { return false; }
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public boolean add( Buff buff ) { return false; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

	@Override
	public void die( Object cause ) {
		//마력 증기 분출
		for (int n : PathFinder.NEIGHBOURS9){
			int c = pos + n;
			if (!Dungeon.level.solid[c]){
				GameScene.add(Blob.seed(c, MazeTower.VAPOR_TURNS, ManaVapor.class));
			}
		}
		if (Dungeon.level.heroFOV[pos]){
			BossEffects.atCellBottom(BossEffects.Fx.VEIN_BREAK, pos, 10, false);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		}
		super.die(cause);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", HP, HT);
	}

	public static class VeinSprite extends MobSprite {
		public VeinSprite(){
			super();
			texture("sprites/maze_p15_vein.png");
			TextureFilm f = new TextureFilm(texture, 17, 17);
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
