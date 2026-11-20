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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

//Maze Tower: 기억의 등불 (20층 망령 왕 2단계, 명세 22번). 그림 maze_p20_lamp.png 16×24: 0 꺼짐, 1~3 켜짐(반복), 4 부서짐.
//어떤 방식으로든 피해를 입으면(해로운 물약·상태이상 포함) 켜진다: 50턴 동안 어둠이 걷히고 왕이 드러나며 왕은 3턴 기절.
//50턴이 지나면 부서지고 다시 어두워진다. 부서진 등불은 다시 켤 수 없다.
//한 번에 하나만 켜진다: 켜진 등불이 있는 동안 다른 등불은 피해를 입지 않는다. 등불 자체는 체력이 줄지 않는다.
public class MemoryLamp extends Mob {

	{
		spriteClass = LampSprite.class;

		HP = HT = 1;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
		alignment = Alignment.NEUTRAL;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	public boolean lit = false;
	public boolean broken = false;
	private int timer = 0;

	@Override
	protected boolean act() {
		if (sprite != null){
			sprite.hideAlert();
			sprite.hideLost();
		}
		if (lit){
			timer--;
			if (timer <= 0){
				lit = false;
				broken = true;
				if (Dungeon.level.heroFOV[pos]){
					Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f);
				}
				WraithKing k = WraithKing.find();
				if (k != null) k.onLampBroken();
			}
		}
		spend(TICK);
		return true;
	}

	public int turnsLeft(){
		return timer;
	}

	//켜진다 (다른 등불이 켜져 있거나, 이미 켰거나 부서졌으면 아무 일 없음)
	public void tryLight(){
		if (lit || broken) return;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof MemoryLamp && ((MemoryLamp) m).lit) return;
		}
		lit = true;
		timer = MazeTower.WK_LAMP_TURNS;
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		WraithKing k = WraithKing.find();
		if (k != null) k.onLampLit(this);
	}

	@Override
	public void damage( int dmg, Object src ) {
		tryLight(); //체력은 줄지 않는다
	}

	@Override
	public boolean add( Buff buff ) {
		//해로운 상태이상(불·독 가스 등)도 "피해"로 친다
		if (buff.type == Buff.buffType.NEGATIVE) tryLight();
		return false;
	}

	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

	@Override
	public String description() {
		if (broken) return Messages.get(this, "desc_broken");
		if (lit)    return Messages.get(this, "desc_lit", timer);
		return Messages.get(this, "desc");
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("lit", lit);
		bundle.put("broken", broken);
		bundle.put("timer", timer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		lit = bundle.getBoolean("lit");
		broken = bundle.getBoolean("broken");
		timer = bundle.getInt("timer");
	}

	public static class LampSprite extends MobSprite {

		private Animation off, on, dead;
		private Image glow;

		public LampSprite(){
			super();
			texture("sprites/maze_p20_lamp.png");
			TextureFilm f = new TextureFilm(texture, 16, 24);
			off = new Animation(1, true);
			off.frames(f, 0);
			on = new Animation(6, true);
			on.frames(f, 1, 2, 3, 2);
			dead = new Animation(1, true);
			dead.frames(f, 4);
			idle = off;
			run = off.clone();
			attack = off.clone();
			zap = off.clone();
			die = new Animation(20, false);
			die.frames(f, 4);
			play(idle);
		}

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof MemoryLamp)) return;
			MemoryLamp l = (MemoryLamp) ch;
			Animation want = l.broken ? dead : (l.lit ? on : off);
			if (idle != want){
				idle = want;
				if (curAnim != die) play(idle);
			}
			//켜진 등불 둘레 빛 (바닥에 반투명)
			if (l.lit && glow == null && parent != null){
				glow = new Image("sprites/maze_fx20w_lamp_light.png");
				PointF c = DungeonTilemap.tileCenterToWorld(l.pos);
				glow.x = c.x - 24;
				glow.y = c.y - 24;
				glow.alpha(0.7f);
				parent.addToBack(glow);
			} else if (!l.lit && glow != null){
				glow.killAndErase();
				glow = null;
			}
			if (glow != null) glow.visible = visible;
		}

		@Override
		public void kill() {
			if (glow != null) glow.killAndErase();
			super.kill();
		}

		@Override
		protected float fadeTime() {
			return MazeTower.PROP_FADE_TIME;
		}
	}
}
