/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ManaWispParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
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

	//그림자검: 적 둘레를 한 바퀴 도는 동그라미 베기. 두 겹으로 그린다.
	//  몸통: 보라빛 도는 거의 검은 그림자 고리 (어두운 색이 그대로 칠해지는 solid)
	//  테두리: 바깥쪽 가는 보라 빛 (어두운 바닥에서도 모양이 보이게)
	public static void shadowStrike( Char hero, Char enemy ){
		if (!visible(hero) || !visible(enemy)) return;

		//적 그림이 공격 동작 등으로 흔들려도 칸 한가운데를 기준으로 그린다
		PointF h = hero.sprite.center(), e = enemy.sprite.destinationCenter();
		//주인공 쪽에서 베기 시작해 원의 60%만 돈다
		float start = (float)Math.atan2(h.y - e.y, h.x - e.x);
		float full = (float)Math.PI * 2f * SHADOW_SWEEP;
		new SlashTrail(SHADOW_RADIUS, 3.5f, start, full, 0f, 0.14f, 0.24f)
				.ring().trailColor(SHADOW_BODY).solid()
				.show(enemy.sprite.parent, e);
		new SlashTrail(SHADOW_RADIUS + 1f, 1f, start, full, 0f, 0.14f, 0.24f)
				.ring().trailColor(SHADOW_RIM)
				.show(enemy.sprite.parent, e);
	}

	//동그라미 베기 반지름 (적 그림보다 살짝 크게)
	private static final float SHADOW_RADIUS = 7f;
	//동그라미 베기가 도는 양 (1 = 한 바퀴)
	private static final float SHADOW_SWEEP = 0.6f;

	//그림자검 색: 몸통은 보라빛 도는 거의 검은색, 테두리는 은은한 보라 빛
	private static final int SHADOW_BODY = 0x1E1828;
	private static final int SHADOW_RIM  = 0x6A5A8C;

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
			MagicMissile bolt = MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.MAGIC_MISSILE, from.sprite, hero.pos, new Callback() {
				@Override
				public void call() {
					if (hero.sprite != null){
						hero.sprite.showStatus(0x4488FF, "+" + amount);
						hero.sprite.centerEmitter().burst(MagicMissile.MagicParticle.FACTORY, 8);
					}
				}
			});
			//원작 마탄(초당 200)보다 훨씬 느리게 날아와 눈으로 따라갈 수 있게
			bolt.setSpeed(MazeTower.MANA_REFUND_BOLT_SPEED);
		} else {
			hero.sprite.showStatus(0x4488FF, "+" + amount);
		}
	}

	//마나 회복(대기): 몸에서 푸른 입자가 위로 피어오른다 (젖음 물방울은 아래로 떨어져 구분된다)
	public static void manaRegen( Char hero ){
		if (!visible(hero)) return;
		hero.sprite.emitter().burst(ManaWispParticle.FACTORY, 5);
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
	private static Flare manaAura;         //가득 찬 마나

	//상태창이 매 프레임 호출. 원하는 상태에 맞춰 켜고 끈다.
	public static void updateHero( Hero hero, boolean parry, boolean ready, boolean fullMana ){
		if (hero == null || hero.sprite == null || hero.sprite.parent == null) return;

		//층을 옮기면 주인공 그림이 새로 만들어지므로 이전 연출은 버린다
		if (hero.sprite != lastSprite){
			lastSprite = hero.sprite;
			parryHalo = null;
			readyGlow = null;
			manaAura = null;
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

		//가득 찬 마나: 주인공 뒤에서 천천히 도는 은은한 푸른 빛살 (젖음의 물방울과 구분되게 입자가 아닌 빛)
		if (fullMana && alive && manaAura == null){
			manaAura = new Flare(7, 20);
			manaAura.color(0x2A5CFF, true);
			manaAura.angularSpeed = 25;
			manaAura.show(hero.sprite, 0);
			manaAura.alpha(0.45f);
		} else if ((!fullMana || !alive) && manaAura != null){
			manaAura.killAndErase();
			manaAura = null;
		}
		if (manaAura != null){
			manaAura.point(hero.sprite.center());
			manaAura.visible = hero.sprite.visible;
		}
	}

	static PointF center( Char ch ){
		return ch.sprite.center();
	}
}
