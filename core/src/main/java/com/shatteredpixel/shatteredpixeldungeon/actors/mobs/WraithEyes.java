/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeBossSprites;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;

//Maze Tower: 어둠 속 망령 왕의 푸른 눈 (20층 2단계 기습, 명세 22번). 그림 maze_fx20w_eyes.png 16×16×3.
//기습 1턴 전에 주인공 뒤 칸에 나타난다. 이 눈을 평타·스킬 등으로 치면 기습이 취소되고 그 피해가 숨어 있는 왕에게 들어간다.
//치지 않으면 다음 왕 턴에 왕이 이 칸으로 순간이동해 주인공을 한 대 치고 다른 곳으로 사라진다(WraithKing.actAmbush).
public class WraithEyes extends Mob {

	{
		spriteClass = EyesSprite.class;

		HP = HT = 1000;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.BOSS_MINION);
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

	//맞으면 숨은 왕에게 피해를 넘기고 기습을 막는다
	@Override
	public void damage( int dmg, Object src ) {
		WraithKing k = WraithKing.find();
		if (k != null && dmg > 0) k.counterHit(dmg, src);
		TowerMaster m = TowerMaster.find(); //Maze Tower (O2): 25층 탑의 주인의 기습 눈도 같다
		if (k == null && m != null && dmg > 0) m.counterHit(dmg, src);
		if (sprite != null) sprite.flash();
	}

	@Override public boolean add( Buff buff ) { return false; }
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override protected boolean canAttack( Char enemy ) { return false; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}

	public static class EyesSprite extends MobSprite {
		public EyesSprite(){
			super();
			texture("sprites/maze_fx20w_eyes.png");
			TextureFilm f = new TextureFilm(texture, 16, 16);
			//(결정) 새 눈 4프레임: 불씨 → 실눈 → 노려봄 ↔ 동공이 꿈틀 → 다시 감음 (한 프레임 0.18초)
			idle = MazeBossSprites.anim(f, 1, true, 0, 0, 1, 2, 3, 2, 3, 2, 1, 0);
			idle.delay = 0.18f;
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = MazeBossSprites.anim(f, 12, false, 2, 1, 0);
			play(idle);
		}

		@Override
		protected float fadeTime() {
			return 0.15f;
		}

		private boolean overFog = false;

		@Override
		public void update() {
			//(결정) 눈 칸은 안개로 덮여 밝아지지 않으므로, 눈 그림만 안개 위 층으로 옮긴다
			if (!overFog && parent != null){
				overFog = true;
				parent.erase(this);
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.effectOverFog(this);
			}
			super.update();
			//(결정) 원작 미믹처럼 알아채기 어렵게: 완전한 어둠 속에서 35%만 비친다
			if (ch != null && ch.isAlive()) alpha(MazeTower.WK_EYES_ALPHA);
		}
	}
}
