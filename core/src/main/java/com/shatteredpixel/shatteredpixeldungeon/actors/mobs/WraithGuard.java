/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
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

	public static final int NOISE_STEP  = 1; //주인공 발소리
	public static final int NOISE_THROW = 2; //던진 물건이 떨어진 소리 (먼저)

	private int noisePriority = 0; //지금 쫓는 소리의 우선순위 (도착하면 0)

	//(결정) 조사 문구: 방이 어두울 때와 밝을 때(등불이 켜졌거나 3단계) 다르다
	@Override
	public String description() {
		WraithKing k = WraithKing.find();
		return Messages.get(this, k != null && k.isDark() ? "desc_dark" : "desc");
	}

	@Override
	protected boolean act() {
		WraithKing k = WraithKing.find();
		boolean dark = k != null && k.isDark();
		viewDistance = dark ? MazeTower.WK_DARK_VIEW : 8;
		//쫓던 소리가 난 곳에 닿으면 다음 소리를 받는다
		if (noisePriority > 0 && (pos == target || Dungeon.level.adjacent(pos, target))) noisePriority = 0;
		return super.act();
	}

	//(결정) 어둠 속에서는 소리를 따라간다: 던진 물건이 떨어진 칸이 먼저, 그다음이 주인공 발소리.
	//던진 물건 쪽으로 가는 동안에는 발소리를 무시한다
	public void hear( int cell, int priority ){
		WraithKing k = WraithKing.find();
		if (k == null || !k.isDark()) return;
		if (priority < noisePriority) return;
		noisePriority = priority;
		if (state == SLEEPING || state == PASSIVE || state == HUNTING) state = WANDERING;
		target = cell;
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

		//어둠 속 푸른빛 (깜깜한 안개 위에 그린다)
		private com.watabou.noosa.Halo glow;
		private boolean overFog = false; //그림이 안개 위 층에 있는지

		@Override
		public void kill() {
			if (glow != null) glow.killAndErase();
			super.kill();
		}

		@Override
		public void update() {
			super.update();
			//어둠 속 은은한 푸른빛: 시야 밖(깜깜한 칸)에서도 빛만은 보인다
			WraithKing k = WraithKing.find();
			boolean dark = k != null && k.isDark() && ch != null && ch.isAlive();
			visibleOutOfFFOV = dark;
			if (dark && glow == null){
				glow = new com.watabou.noosa.Halo(9f, 0x5AA0FF, 0.3f);
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.effectOverFog(glow);
			} else if (!dark && glow != null){
				glow.killAndErase();
				glow = null;
			}
			if (glow != null){
				com.watabou.utils.PointF c = center();
				glow.point(c.x, c.y);
				glow.alpha(0.22f + 0.1f * (float)Math.sin(com.watabou.noosa.Game.timeTotal * 2.5f));
			}
			//(결정) 어둠 속에서 몸통 칸이 보이면(주인공 바로 옆) 머리(위 칸)까지 보이도록 그림을 안개 위 층으로 옮긴다.
			//몸통이 다시 어둠에 묻히거나 방이 밝아지면 원래 층(캐릭터 층)으로 돌아온다
			boolean shown = dark && Dungeon.level.heroFOV[ch.pos];
			if (shown != overFog && parent != null){
				com.watabou.noosa.Group from = parent;
				from.erase(this);
				if (shown) com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.effectOverFog(this);
				else       com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.addMobSpriteBack(this);
				overFog = shown;
			}
			if (dark){
				float p = 0.75f + 0.25f * (float)Math.sin(com.watabou.noosa.Game.timeTotal * 2.5f);
				rm = 0.6f * p; gm = 0.85f * p; bm = 1.3f * p;
			} else {
				rm = gm = bm = 1f;
			}
		}
	}
}
