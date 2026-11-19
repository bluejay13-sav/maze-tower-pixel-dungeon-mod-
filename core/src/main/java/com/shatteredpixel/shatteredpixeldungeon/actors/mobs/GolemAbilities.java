/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.FrostCrystals;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Maze Tower: 15층 보스 (원작 DM-300 바탕, 기획 문서). 판마다 둘 중 하나 (MazeBosses.crystalGiant).
//  마도 골렘: 원작 기둥(충전·무적)과 독가스를 쓰지 않는다. 독가스 자리에 5×5 내려치기(범위 안의 광맥은 즉시 부서짐).
//            광맥이 부서지면 마력 증기. 젖으면 5턴 기절, 정전기가 끝날 때 3턴 기절.
//  수정 거인: 원작 구조(기둥에서 힘을 받아 강화·낙석·돌진) 그대로, 기둥은 수정 기둥. 독가스 대신 냉기 결정.
//            불에 타면 수정이 녹아 방어력 0이 되고, 다시 자라기까지 50턴.
public class GolemAbilities {

	public static boolean golem(){
		return MazeBosses.ENABLED && !MazeBosses.crystalGiant();
	}

	public static boolean crystal(){
		return MazeBosses.ENABLED && MazeBosses.crystalGiant();
	}

	// ===== 마도 골렘: 5×5 내려치기 =====

	//원작 독가스 대신 (DM300.ventGas). 경고 표시 뒤 내려친다
	public static void slam( DM300 golem, Char target ){
		Dungeon.hero.interrupt();
		float delay = GameMath.gate(Actor.TICK, (int)Math.ceil(target.cooldown()), 3*Actor.TICK);
		for (int c : area(golem.pos)){
			GameScene.targetedCell(c, delay);
		}
		Buff.append(golem, GolemSlam.class, delay);
		if (Dungeon.level.heroFOV[golem.pos]) GLog.n(Messages.get(GolemAbilities.class, "slam_warn"));
	}

	//주변 5×5 (벽 제외)
	private static java.util.ArrayList<Integer> area( int center ){
		java.util.ArrayList<Integer> cells = new java.util.ArrayList<>();
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		for (int y = cy - 2; y <= cy + 2; y++){
			for (int x = cx - 2; x <= cx + 2; x++){
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y * w;
				if (c != center && !Dungeon.level.solid[c]) cells.add(c);
			}
		}
		return cells;
	}

	public static class GolemSlam extends FlavourBuff {
		@Override
		public boolean act() {
			if (target instanceof DM300 && target.isAlive()){
				DM300 golem = (DM300)target;
				if (Dungeon.level.heroFOV[golem.pos]){
					BossEffects.atCell(BossEffects.Fx.SLAM5, golem.pos, 15, false);
					PixelScene.shake(3, 0.5f);
				}
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				for (int c : area(golem.pos)){
					Char ch = Actor.findChar(c);
					if (ch == null) continue;
					if (ch instanceof ManaVein){
						ch.die(golem); //범위 안의 광맥은 즉시 부서진다
						continue;
					}
					if (ch.alignment == golem.alignment) continue;
					int dmg = golem.damageRoll() - ch.drRoll();
					if (dmg > 0) ch.damage(dmg, golem);
					if (ch == Dungeon.hero && !ch.isAlive()){
						Dungeon.fail(golem);
					}
				}
			}
			detach();
			return true;
		}
	}

	// ===== 마도 골렘: 기절 =====

	public static void stun( DM300 golem, float turns ){
		Buff.prolong(golem, Paralysis.class, turns);
		if (golem.sprite != null && Dungeon.level.heroFOV[golem.pos]){
			MovieClip c = BossEffects.clip(BossEffects.Fx.STUN, 6, true);
			c.x = golem.sprite.center().x - BossEffects.Fx.STUN.w / 2f;
			c.y = golem.sprite.y - 4;
			GameScene.effect(c);
			golem.stunFx = c;
			GLog.p(Messages.get(GolemAbilities.class, "stunned"));
		}
	}

	// ===== 수정 거인: 냉기 결정 =====

	//원작 독가스 대신 (DM300.ventGas). 날아간 줄과 맞은 곳 주변에 결정
	public static void frost( DM300 giant, Char target ){
		Dungeon.hero.interrupt();
		Ballistica trajectory = new Ballistica(giant.pos, target.pos, Ballistica.STOP_TARGET);
		for (int c : trajectory.subPath(1, trajectory.dist)){
			if (!Dungeon.level.solid[c]) GameScene.add(Blob.seed(c, MazeTower.FROST_CRYSTAL_TURNS, FrostCrystals.class));
		}
		for (int n : PathFinder.NEIGHBOURS9){
			int c = trajectory.collisionPos + n;
			if (!Dungeon.level.solid[c] && Random.Int(4) > 0){
				GameScene.add(Blob.seed(c, MazeTower.FROST_CRYSTAL_TURNS, FrostCrystals.class));
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f);
	}

	//불에 타서 수정이 녹았을 때
	public static void melt( DM300 giant ){
		if (giant.sprite != null && Dungeon.level.heroFOV[giant.pos]){
			BossEffects.onChar(BossEffects.Fx.MELT, giant.sprite, 6, false);
			GLog.p(Messages.get(GolemAbilities.class, "melted"));
		}
	}

	@SuppressWarnings("unused")
	private static void unused( Bundle b ){ }
}
