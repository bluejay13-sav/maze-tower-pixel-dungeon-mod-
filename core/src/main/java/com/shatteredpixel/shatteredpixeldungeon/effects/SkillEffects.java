/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//Maze Tower: 검 스킬 이펙트. 스킬 이름 기준이라 같은 이름의 스킬은 무기가 달라도 같은 이펙트를 쓴다.
//  베기     흰 베기 궤적
//  찌르기   일직선 찌르기 섬광
//  연격     짧은 궤적 3연속, 타격마다 번쩍
//  반월베기 반달 모양 궤적 2연속
//  무형검   푸른 마탄 칼날
public class SkillEffects {

	private static final int WHITE = 0xFFFFFF;
	private static final int PALE  = 0xEEEEFF;
	private static final int BLUE  = 0x66AAFF;

	public static void play( String skillKey, Char hero, Char enemy ){
		if (hero.sprite == null || enemy.sprite == null || enemy.sprite.parent == null) return;
		if (!enemy.sprite.visible) return;

		switch (skillKey){
			case "slash":    slash(hero, enemy, WHITE); break;
			case "thrust":   thrust(hero, enemy); break;
			case "flurry":   flurry(hero, enemy); break;
			case "crescent": crescent(hero, enemy); break;
			case "phantom":  phantom(hero, enemy); break;
		}
	}

	private static float dir( Char hero, Char enemy ){
		PointF h = hero.sprite.center(), e = enemy.sprite.center();
		return (float)Math.atan2(e.y - h.y, e.x - h.x);
	}

	//대상 중심에서 주인공 쪽으로 조금 당긴 점 (궤적이 대상을 가로지르게)
	private static PointF pulledBack( Char enemy, float dir, float by ){
		PointF c = enemy.sprite.center();
		return new PointF(c.x - (float)Math.cos(dir) * by, c.y - (float)Math.sin(dir) * by);
	}

	private static Group layer( Char enemy ){
		return enemy.sprite.parent;
	}

	//베기: 흰 베기 궤적 (다른 궤적의 기본형)
	private static void slash( Char hero, Char enemy, int color ){
		float d = dir(hero, enemy);
		new SlashTrail(12, 4, d - 1.1f, 2.2f, 0f, 0.10f, 0.20f)
				.trailColor(color)
				.show(layer(enemy), pulledBack(enemy, d, 6));
	}

	//찌르기: 주인공에서 대상 너머까지 일직선 섬광
	private static void thrust( Char hero, Char enemy ){
		float d = dir(hero, enemy);
		PointF from = hero.sprite.center();
		PointF c = enemy.sprite.center();
		PointF to = new PointF(c.x + (float)Math.cos(d) * 8, c.y + (float)Math.sin(d) * 8);
		layer(enemy).add(new Beam.LightRay(from, to));
		enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 6);
	}

	//연격: 짧은 궤적 3연속, 타격마다 번쩍
	private static void flurry( final Char hero, final Char enemy ){
		float d = dir(hero, enemy);
		for (int i = 0; i < 3; i++){
			float start = d - 0.8f + Random.Float(-0.6f, 0.6f);
			float sweep = (i % 2 == 0) ? 1.6f : -1.6f;
			if (sweep < 0) start += 1.6f;
			new SlashTrail(8, 3, start, sweep, i * 0.09f, 0.06f, 0.15f)
					.trailColor(WHITE)
					.onStart(new Runnable() {
						@Override
						public void run() {
							if (enemy.sprite != null) enemy.sprite.flash();
						}
					})
					.show(layer(enemy), pulledBack(enemy, d, 3));
		}
	}

	//반월베기: 반달 모양 궤적 2연속 (서로 반대 방향)
	private static void crescent( Char hero, Char enemy ){
		float d = dir(hero, enemy);
		PointF c = pulledBack(enemy, d, 4);
		new SlashTrail(14, 5, d - (float)Math.PI/2f,  (float)Math.PI, 0f,    0.12f, 0.22f).trailColor(PALE).show(layer(enemy), c);
		new SlashTrail(14, 5, d + (float)Math.PI/2f, -(float)Math.PI, 0.15f, 0.12f, 0.22f).trailColor(PALE).show(layer(enemy), c);
	}

	//무형검: 푸른 마탄이 날아가 푸른 칼날 궤적
	private static void phantom( final Char hero, final Char enemy ){
		MagicMissile.boltFromChar(layer(enemy), MagicMissile.MAGIC_MISSILE, hero.sprite, enemy.pos, new Callback() {
			@Override
			public void call() {
				//날아가는 동안 대상이 사라졌을 수 있다
			}
		});
		float d = dir(hero, enemy);
		new SlashTrail(12, 4, d - 1.1f, 2.2f, 0.08f, 0.10f, 0.25f)
				.trailColor(BLUE)
				.onStart(new Runnable() {
					@Override
					public void run() {
						if (enemy.sprite != null){
							enemy.sprite.centerEmitter().burst(MagicMissile.MagicParticle.FACTORY, 10);
						}
					}
				})
				.show(layer(enemy), pulledBack(enemy, d, 6));
	}
}
