/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SilverParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//Maze Tower: 검 스킬 이펙트. 스킬 이름 기준이라 같은 이름의 스킬은 무기가 달라도 같은 이펙트를 쓴다.
//여러 번 때리는 스킬은 SwordSkill이 타격 사이에 텀을 두고 타격마다 hit()를 부른다.
//  베기     흰 베기 궤적
//  찌르기   가느다란 강철 화살촉이 꿰뚫고 지나감
//  연격     짧은 궤적, 타격마다 번쩍 (3타)
//  반월베기 반달 모양 궤적 + 은색 가루, 타격마다 방향이 바뀜 (2타)
//  무형검   푸른 마탄 칼날
public class SkillEffects {

	private static final int WHITE = 0xFFFFFF;
	private static final int PALE  = 0xEEEEFF;
	private static final int BLUE  = 0x66AAFF;

	//타격 한 번의 이펙트. index는 0부터 시작하는 타격 번호
	public static void hit( String skillKey, Char hero, Char enemy, int index ){
		if (hero.sprite == null || enemy.sprite == null || enemy.sprite.parent == null) return;
		if (!enemy.sprite.visible) return;

		switch (skillKey){
			case "slash":    slash(hero, enemy, WHITE); break;
			case "thrust":   thrust(hero, enemy); break;
			case "flurry":   flurry(hero, enemy, index); break;
			case "crescent": crescent(hero, enemy, index); break;
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

	//찌르기: 가느다란 강철 화살촉이 대상을 꿰뚫고 지나간다. 닿는 순간 쇳빛 불똥.
	private static void thrust( Char hero, final Char enemy ){
		float d = dir(hero, enemy);
		PointF h = hero.sprite.center();
		PointF c = enemy.sprite.center();
		float cos = (float)Math.cos(d), sin = (float)Math.sin(d);
		PointF from = new PointF(h.x + cos * 4, h.y + sin * 4);
		PointF to   = new PointF(c.x + cos * 7, c.y + sin * 7);
		ThrustTip.fire(layer(enemy), from, to, 0f, new Runnable() {
			@Override
			public void run() {
				if (enemy.sprite != null){
					enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 4);
					enemy.sprite.centerEmitter().burst(SilverParticle.FACTORY, 5);
				}
			}
		});
	}

	//연격: 짧은 궤적, 타격마다 번쩍. 타격마다 궤적 방향이 엇갈린다.
	private static void flurry( Char hero, Char enemy, int index ){
		float d = dir(hero, enemy);
		float start = d - 0.8f + Random.Float(-0.6f, 0.6f);
		float sweep = (index % 2 == 0) ? 1.6f : -1.6f;
		if (sweep < 0) start += 1.6f;
		new SlashTrail(8, 3, start, sweep, 0f, 0.06f, 0.15f)
				.trailColor(WHITE)
				.show(layer(enemy), pulledBack(enemy, d, 3));
		enemy.sprite.flash();
	}

	//반월베기: 반달 모양 궤적 + 은색 가루. 두 번째 타격은 반대 방향.
	private static void crescent( Char hero, Char enemy, int index ){
		float d = dir(hero, enemy);
		PointF c = pulledBack(enemy, d, 4);
		float half = (float)Math.PI / 2f;
		if (index % 2 == 0){
			new SlashTrail(14, 5, d - half,  (float)Math.PI, 0f, 0.12f, 0.22f)
					.trailColor(PALE).sparkle(SilverParticle.FACTORY).show(layer(enemy), c);
		} else {
			new SlashTrail(14, 5, d + half, -(float)Math.PI, 0f, 0.12f, 0.22f)
					.trailColor(PALE).sparkle(SilverParticle.FACTORY).show(layer(enemy), c);
		}
	}

	//무형검: 푸른 마탄이 날아가 푸른 칼날 궤적
	private static void phantom( final Char hero, final Char enemy ){
		MagicMissile.boltFromChar(layer(enemy), MagicMissile.MAGIC_MISSILE, hero.sprite, enemy.pos, new Callback() {
			@Override
			public void call() {
				//이펙트 전용. 피해는 SwordSkill에서 이미 처리한다.
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
