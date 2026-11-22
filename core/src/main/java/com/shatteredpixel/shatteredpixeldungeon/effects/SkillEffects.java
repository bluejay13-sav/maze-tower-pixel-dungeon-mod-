/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.watabou.noosa.MovieClip;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//Maze Tower: 검 스킬 이펙트. 스킬 이름 기준이라 같은 이름의 스킬은 무기가 달라도 같은 이펙트를 쓴다.
//여러 번 때리는 스킬은 SwordSkill이 타격 사이에 텀을 두고 타격마다 hit()를 부른다.
//(2026-11-21) 코드로 그리던 궤적을 다시 그린 그림 시트로 바꿨다 (MazeFx, docs/sprites/fx_redo/fx3_00~07).
//  베기 fx3_00 / 찌르기 fx3_01 / 연격 fx3_02 (3타, 타격마다 두 프레임) / 반월베기 fx3_03 (2타)
//  무형검 fx3_04 (+ 푸른 마탄) / 급소 찌르기 fx3_05, 기습 fx3_06 / 조준 사격 적중 fx3_07
//방향이 있는 그림은 주인공 → 대상 방향으로 돌린다.
public class SkillEffects {

	//타격 한 번의 이펙트. index는 0부터 시작하는 타격 번호
	public static void hit( String skillKey, Char hero, Char enemy, int index ){
		hit(skillKey, hero, enemy, index, false);
	}

	//surprise: 기습 상태의 적에게 쓴 경우 (급소 찌르기)
	public static void hit( String skillKey, Char hero, Char enemy, int index, boolean surprise ){
		if (hero.sprite == null || enemy.sprite == null || enemy.sprite.parent == null) return;
		if (!enemy.sprite.visible) return;

		switch (skillKey){
			case "slash":    MazeFx.toward(MazeFx.Fx.SLASH, hero, enemy); break;
			case "thrust":   MazeFx.toward(MazeFx.Fx.THRUST, hero, enemy); break;
			case "flurry":   flurry(hero, enemy, index); break;
			case "crescent": crescent(hero, enemy, index); break;
			case "phantom":  phantom(hero, enemy); break;
			case "vital":    vital(hero, enemy, surprise); break;
			case "aimed":    break; //(2026-11-21 밤) 조준 사격 적중 그림(fx3_07) 삭제
			//Maze Tower: 고리지기 반지 스킬 (맨손)
			case "frenzy":   flurry(hero, enemy, index % 3); break;
			case "toss":     MazeFx.toward(MazeFx.Fx.SLASH, hero, enemy); break;
			case "burst":    tinted(hero, enemy, 0x88BBFF); break;
			case "goldfist": tinted(hero, enemy, 0xFFD040); break;
		}
	}

	private static void tinted( Char hero, Char enemy, int color ){
		MovieClip c = MazeFx.toward(MazeFx.Fx.SLASH, hero, enemy);
		if (c != null) c.tint(color, 0.6f);
	}

	//연격: 타격마다 두 프레임 (사선 → 반대 사선 → 크게 가로지르는 마무리), 타격마다 번쩍
	private static void flurry( Char hero, Char enemy, int index ){
		int i = Math.min(index, 2);
		MazeFx.toward(MazeFx.Fx.FLURRY, hero, enemy, i * 2, i * 2 + 1);
		enemy.sprite.flash();
	}

	//반월베기: 첫 타는 앞 두 프레임(작은 반달), 둘째 타는 나머지 세 프레임(크고 두꺼운 반달)
	private static void crescent( Char hero, Char enemy, int index ){
		if (index % 2 == 0) MazeFx.toward(MazeFx.Fx.CRESCENT, hero, enemy, 0, 1);
		else                MazeFx.toward(MazeFx.Fx.CRESCENT, hero, enemy, 2, 3, 4);
	}

	//무형검: 푸른 마탄이 날아가 대상에서 칼날 그림
	private static void phantom( final Char hero, final Char enemy ){
		MagicMissile.boltFromChar(enemy.sprite.parent, MagicMissile.MAGIC_MISSILE, hero.sprite, enemy.pos, new Callback() {
			@Override
			public void call() {
				//이펙트 전용. 피해는 SwordSkill에서 이미 처리한다.
			}
		});
		MazeFx.toward(MazeFx.Fx.PHANTOM, hero, enemy);
	}

	//급소 찌르기: 짧고 빠른 찌르기. 기습이면 핏빛 그림 + 피 3방울 (기습이 아니면 피가 튀지 않는다)
	private static void vital( Char hero, Char enemy, boolean surprise ){
		MazeFx.toward(surprise ? MazeFx.Fx.VITAL_SURPRISE : MazeFx.Fx.VITAL, hero, enemy);
		if (surprise){
			PointF h = hero.sprite.center(), c = enemy.sprite.center();
			Splash.at(c, (float)Math.atan2(c.y - h.y, c.x - h.x), 0.5f, enemy.sprite.blood(), 3);
		}
	}

	//Maze Tower: 활 스킬이 맞은 뒤 (피해가 들어간 뒤) 연출
	public static void afterHit( String skillKey, Char hero, Char enemy ){
		if (hero.sprite == null || enemy.sprite == null || enemy.sprite.parent == null || !enemy.sprite.visible) return;
		PointF c = enemy.sprite.center();
		float ang = PointF.angle(hero.sprite.center(), c);
		switch (skillKey){
			case "aimed": //피가 조금 튐
				Splash.at(c, ang, 3.1415926f / 2, enemy.sprite.blood(), 3);
				break;
			case "rapid":
				Splash.at(c, ang, 3.1415926f / 2, enemy.sprite.blood(), 2);
				enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 2);
				break;
			case "loaded": //장전된 화살: 회색 먼지 + 불똥
				enemy.sprite.centerEmitter().burst(Speck.factory(Speck.DUST), 4);
				enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
				enemy.sprite.flash();
				break;
			case "focused": //집중: 피가 많이 튐
				Splash.at(c, ang, 3.1415926f / 1.5f, enemy.sprite.blood(), 9);
				enemy.sprite.flash();
				break;
			case "pierce": //푸른 마법 입자
				enemy.sprite.centerEmitter().burst(MagicMissile.MagicParticle.FACTORY, 8);
				enemy.sprite.flash();
				break;
		}
	}
}
