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
import com.shatteredpixel.shatteredpixeldungeon.sprites.RubbleSprite;
import com.watabou.noosa.audio.Sample;

//Maze Tower: 무너진 돌무더기 (명세 14번). 역행 중 층마다 길 일부를 막는다.
//체력이 있어 공격으로 부순다. 방어력이 없어 준 피해만큼 그대로 깎인다.
//평타·스킬·필살기·마법 막대·폭탄 등 모든 피해가 들어간다. 움직이지도 공격하지도 않는다.
public class Rubble extends Mob {

	{
		spriteClass = RubbleSprite.class;

		HP = HT = MazeTower.RUBBLE_HP;
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
		//아무것도 하지 않는다
		if (sprite != null){
			sprite.hideAlert();
			sprite.hideLost();
		}
		spend(TICK);
		return true;
	}

	//클릭하면 말을 거는 대신 공격한다
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	public int damageRoll() { return 0; }

	@Override
	public int attackSkill( Char target ) { return 0; }

	@Override
	public int defenseSkill( Char enemy ) { return 0; } //반드시 맞는다

	@Override
	public int drRoll() { return 0; } //방어력 없음

	@Override
	public boolean add( Buff buff ) {
		return false; //상태이상에 걸리지 않는다
	}

	@Override
	public void beckon( int cell ) {
		//움직이지 않는다
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (sprite != null){
			((RubbleSprite)sprite).updateCracks();
			if (Dungeon.level.heroFOV[pos]){
				CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 3);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.5f, 1.3f);
			}
		}
	}

	@Override
	public void die( Object cause ) {
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 10);
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		}
		super.die(cause);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", HP, HT);
	}
}
