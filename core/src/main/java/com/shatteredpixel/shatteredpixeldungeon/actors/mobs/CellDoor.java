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

//Maze Tower (I1): 10층 1단계 감방 미로의 감방 문 (고정 몹, 쇠창살 문).
//지나갈 수 없고, 때려서 부술 수 있다((결정) 피해와 상관없이 I1_CELLDOOR_HITS번 맞으면, 반드시 맞음). 움직이지도 공격하지도 않는다. 경험치·전리품 없음.
//(결정) 한 번 부서지거나 열린 문은 그대로 남는다. 간수에게 잡히면 문이 아직 잠겨 있는 가장 가까운 감방으로 끌려간다.
public class CellDoor extends Mob {

	{
		spriteClass = DoorSprite.class;

		HP = HT = MazeTower.I1_CELLDOOR_HITS;
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

	//주인공이 누르면 원작처럼 평타로 친다
	@Override public boolean heroShouldInteract() { return false; }
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public boolean add( Buff buff ) { return false; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }
	@Override public void rollToDropLoot() { }

	@Override
	public void damage( int dmg, Object src ) {
		//(결정) 얼마나 세게 치든 한 대에 1씩: 늘 정해진 횟수만큼 쳐야 부서진다
		if (dmg < 0 || !isAlive()) return;
		super.damage(1, src);
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG, 0.6f, 0.8f );
		if (isAlive() && sprite instanceof DoorSprite) ((DoorSprite) sprite).rattle();
	}

	//(I5) 풀려난 영혼이 문을 열어 준다: 부서지는 소리 없이 철컥 열리며 사라진다 (다시 서지 않음)
	public void openBySoul(){
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 6);
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
		}
		super.die(null);
		com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel.onDoorOpened(pos);
	}

	@Override
	public void die( Object cause ) {
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 4);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		}
		super.die(cause);
		com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel.onDoorOpened(pos); //(결정) 간수가 살피러 온다
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", HP, HT);
	}

	//쇠창살 문 (16×16 × 5: 0 닫힘, 1 맞아서 흔들림, 2~4 부서짐)
	public static class DoorSprite extends MobSprite {

		private Animation shake;

		public DoorSprite(){
			super();
			texture( Assets.Sprites.CELL_DOOR );
			TextureFilm f = new TextureFilm( texture, 16, 16 );
			idle = new Animation( 1, true );
			idle.frames( f, 0 );
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			shake = new Animation( 16, false );
			shake.frames( f, 1, 0, 1, 0 );
			die = new Animation( 12, false );
			die.frames( f, 2, 3, 4 );
			play( idle );
		}

		public void rattle(){
			play( shake );
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == shake){ idle(); return; }
			super.onComplete(anim);
		}

		@Override
		protected float fadeTime() {
			return MazeTower.PROP_FADE_TIME;
		}

		@Override
		public int blood() {
			return 0xFF8A8A96;
		}
	}
}
