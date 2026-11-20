/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeBossSprites;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

//Maze Tower: 망령 근위병 (20층 망령 왕 2단계, 명세 22번). 그림 maze_m20_wraith_guard_sheet.png 16×20, 13프레임.
//한 턴에 2칸 움직인다. 어둠 속에서는 눈이 멀어(시야 1칸) 소리를 따라간다:
//주인공이 움직이면 그쪽으로, 주인공이 무언가를 던지면 떨어진 쪽으로 (CityBossLevel.occupyCell, Item.onThrow → WraithKing.noise).
//어둠 속에서 은은한 푸른빛을 띠어 멀리서도 보인다.
public class WraithGuard extends Mob {

	{
		spriteClass = GuardSprite.class;

		HP = HT = MazeTower.WG_HP;
		defenseSkill = MazeTower.WG_DEF;
		EXP = 0;
		maxLvl = -2;
		baseSpeed = MazeTower.WG_SPEED;

		state = WANDERING;

		properties.add(Property.UNDEAD);
		properties.add(Property.BOSS_MINION);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(MazeTower.WG_DMG_MIN, MazeTower.WG_DMG_MAX);
	}

	@Override
	public int attackSkill( Char target ) {
		return MazeTower.WG_ACC;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, MazeTower.WG_DR);
	}

	@Override
	protected boolean act() {
		WraithKing k = WraithKing.find();
		viewDistance = (k != null && k.isDark()) ? MazeTower.WK_DARK_VIEW : 8;
		return super.act();
	}

	//소리가 난 곳으로 간다
	public void hear( int cell ){
		if (state == SLEEPING || state == PASSIVE) state = WANDERING;
		if (state == WANDERING) target = cell;
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	public static class GuardSprite extends MobSprite {
		public GuardSprite(){
			super();
			texture("sprites/maze_m20_wraith_guard_sheet.png");
			TextureFilm f = new TextureFilm(texture, 16, 20);
			idle   = MazeBossSprites.anim(f, 3, true, 0, 1);
			run    = MazeBossSprites.anim(f, 12, true, 2, 3, 4, 5);
			attack = MazeBossSprites.anim(f, 15, false, 6, 7, 8, 0);
			zap    = attack.clone();
			die    = MazeBossSprites.anim(f, 8, false, 9, 10, 11, 12);
			play(idle);
		}

		@Override
		public void update() {
			super.update();
			//어둠 속 은은한 푸른빛: 시야 밖(어두운 칸)에서도 흐리게 보인다
			WraithKing k = WraithKing.find();
			boolean dark = k != null && k.isDark();
			visibleOutOfFFOV = dark;
			if (dark){
				float p = 0.75f + 0.25f * (float)Math.sin(com.watabou.noosa.Game.timeTotal * 2.5f);
				rm = 0.6f * p; gm = 0.85f * p; bm = 1.3f * p;
			} else {
				rm = gm = bm = 1f;
			}
		}
	}
}
