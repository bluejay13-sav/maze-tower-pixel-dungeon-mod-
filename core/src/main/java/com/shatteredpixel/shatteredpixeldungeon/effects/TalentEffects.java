/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaWispParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//Maze Tower: 2차 특성 발동 이펙트 (명세 9번 "특성 발동").
//일회성 연출은 BloomEffects가 발동 순간에 부르고,
//계속 켜져 있는 연출(흘리기 보호막, 집중·흐름 준비 완료, 가득 찬 마나)은 상태창이 매 프레임 updateHero로 맞춘다.
public class TalentEffects {

	private static boolean visible( Char ch ){
		return ch != null && ch.sprite != null && ch.sprite.parent != null && ch.sprite.visible;
	}

	// ===== 일회성 =====

	//원소 폭발: 주인공에서 대상으로 마탄
	public static void elementBurst( Char hero, Char enemy ){
		if (!visible(hero) || !visible(enemy)) return;
		MagicMissile.boltFromChar(enemy.sprite.parent, MagicMissile.MAGIC_MISSILE, hero.sprite, enemy.pos, new Callback() {
			@Override
			public void call() { }
		});
		enemy.sprite.centerEmitter().burst(MagicMissile.MagicParticle.FACTORY, 10);
	}

	//연쇄: 대상 사이 짧은 빛줄기
	public static void chain( Char from, Char to ){
		if (!visible(from) || !visible(to)) return;
		from.sprite.parent.add(new Beam.LightRay(from.sprite.center(), to.sprite.center()));
	}

	//그림자검: 잔상 일격.
	//보랏빛 반투명 주인공 잔상이 적에게 덤벼들며 사라지고, 적에게 보랏빛 베기 궤적과 그림자.
	public static void shadowStrike( Char hero, Char enemy ){
		if (!visible(hero) || !visible(enemy)) return;

		//주인공 잔상
		final Image ghost = new Image(hero.sprite);
		ghost.flipHorizontal = hero.sprite.flipHorizontal;
		ghost.x = hero.sprite.x;
		ghost.y = hero.sprite.y;
		ghost.hardlight(0.55f, 0.35f, 1f);
		ghost.alpha(0.75f);
		hero.sprite.parent.add(ghost);

		//적 쪽으로 2/3쯤 덤벼든다
		PointF to = new PointF(
				hero.sprite.x + (enemy.sprite.x - hero.sprite.x) * 0.65f,
				hero.sprite.y + (enemy.sprite.y - hero.sprite.y) * 0.65f);
		hero.sprite.parent.add(new PosTweener(ghost, to, 0.12f));
		hero.sprite.parent.add(new AlphaTweener(ghost, 0f, 0.35f){
			@Override
			protected void onComplete() {
				super.onComplete();
				ghost.killAndErase();
			}
		});

		//보랏빛 잔상 베기 + 그림자
		PointF h = hero.sprite.center(), e = enemy.sprite.center();
		float d = (float)Math.atan2(e.y - h.y, e.x - h.x);
		new SlashTrail(11, 3, d + 1.1f, -2.2f, 0.08f, 0.08f, 0.22f)
				.trailColor(0xAA77FF)
				.show(enemy.sprite.parent, new PointF(e.x - (float)Math.cos(d) * 5, e.y - (float)Math.sin(d) * 5));
		enemy.sprite.emitter().burst(ShadowParticle.CURSE, 10);
		enemy.sprite.showStatus(0xAA88FF, Messages.get(TalentEffects.class, "shadow"));
	}

	//연격(특성): 쌓이는 중첩 표시 (스킬 연격과 다른 노란 숫자)
	public static void comboStack( Char enemy, int count, int need ){
		if (!visible(enemy)) return;
		enemy.sprite.showStatus(0xFFDD55, count + "/" + need);
	}

	//연격(특성): 중첩이 터질 때
	public static void comboBurst( Char enemy ){
		if (!visible(enemy)) return;
		enemy.sprite.showStatus(0xFFAA00, Messages.get(TalentEffects.class, "combo"));
		enemy.sprite.centerEmitter().burst(Speck.factory(Speck.STAR), 8);
	}

	//처형: 붉은 섬광
	public static void execute( Char enemy ){
		if (!visible(enemy)) return;
		new Flare(6, 20).color(0xFF2222, true).show(enemy.sprite, 0.5f);
	}

	//생명탈취: 회복 입자
	public static void lifeSteal( Char hero ){
		if (!visible(hero)) return;
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
	}

	//마나 반환: 처치한 적 자리에서 주인공에게 푸른 마탄이 날아든다
	public static void manaRefund( final Char hero, Char from, final int amount ){
		if (!visible(hero)) return;
		if (from != null && from.sprite != null && from.sprite.parent != null){
			MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.MAGIC_MISSILE, from.sprite, hero.pos, new Callback() {
				@Override
				public void call() {
					if (hero.sprite != null) hero.sprite.showStatus(0x4488FF, "+" + amount);
				}
			});
		} else {
			hero.sprite.showStatus(0x4488FF, "+" + amount);
		}
	}

	//마나 장막: 발동 순간 보호막이 번쩍이고 사라진다
	public static void manaVeil( Char hero ){
		if (!visible(hero)) return;
		ShieldHalo halo = new ShieldHalo(hero.sprite);
		GameScene.effect(halo);
		halo.putOut();
		Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.8f, 1.2f);
	}

	//흘리기: 받는 공격을 흘릴 때
	public static void parryUsed( Char hero ){
		if (!visible(hero)) return;
		Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY);
	}

	// ===== 계속 켜져 있는 연출 =====

	private static CharSprite lastSprite;
	private static ShieldHalo parryHalo;   //흘리기 대기 중 보호막
	private static GlowBlock readyGlow;    //집중·흐름 준비 완료
	private static Emitter manaWisps;      //가득 찬 마나

	//상태창이 매 프레임 호출. 원하는 상태에 맞춰 켜고 끈다.
	public static void updateHero( Hero hero, boolean parry, boolean ready, boolean fullMana ){
		if (hero == null || hero.sprite == null || hero.sprite.parent == null) return;

		//층을 옮기면 주인공 그림이 새로 만들어지므로 이전 연출은 버린다
		if (hero.sprite != lastSprite){
			lastSprite = hero.sprite;
			parryHalo = null;
			readyGlow = null;
			manaWisps = null;
		}
		boolean alive = hero.isAlive();

		if (parry && alive && parryHalo == null){
			parryHalo = new ShieldHalo(hero.sprite);
			GameScene.effect(parryHalo);
		} else if ((!parry || !alive) && parryHalo != null){
			parryHalo.putOut();
			parryHalo = null;
		}

		if (ready && alive && readyGlow == null){
			readyGlow = GlowBlock.lighten(hero.sprite);
		} else if ((!ready || !alive) && readyGlow != null){
			readyGlow.darken();
			readyGlow = null;
		}

		if (fullMana && alive && manaWisps == null){
			manaWisps = hero.sprite.emitter();
			if (manaWisps != null) manaWisps.pour(ManaWispParticle.FACTORY, 0.25f);
		} else if ((!fullMana || !alive) && manaWisps != null){
			manaWisps.on = false;
			manaWisps = null;
		}
		if (manaWisps != null) manaWisps.visible = hero.sprite.visible;
	}

	static PointF center( Char ch ){
		return ch.sprite.center();
	}
}
