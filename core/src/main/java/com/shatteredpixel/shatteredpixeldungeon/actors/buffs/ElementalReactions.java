/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomEffects;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.watabou.utils.PathFinder;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

import java.util.ArrayList;

//Maze Tower: 원소 반응. 원작에서 화염과 냉기가 서로를 지우던 처리를 아래 반응으로 바꾼다.
//
//  증기: 화상 + 냉기       → 남은 화상 지속시간의 화상 피해 합계 × 1.5, 화상·냉기 해제
//  폭발: 정전기 + 화상     → 인접 1칸의 모든 적에게 기준 피해 × 1.2
//  해빙: 빙결 + 화염       → 남은 화상 지속시간의 화상 피해 합계 × 2, 빙결·화상 해제
//  빙결: 젖음 + 냉기       → 젖음·냉기 해제, 원작 Frost (순서 무관)
//
//  젖음 + 화상            → 둘 다 해제 (모든 대상, 다른 반응보다 먼저)
//
//반응은 적(주인공·아군이 아닌 대상)에게만 일어난다. 주인공·아군은 원작처럼 서로 지우기만 한다.
public class ElementalReactions {

	//반응 피해의 출처 표시용
	//반응 피해의 출처. 주인공이 반응(폭발 등)에 휘말려 죽으면 사망 기록을 남긴다.
	public static class ReactionDamage implements Hero.Doom {
		@Override
		public void onDeath() {
			Dungeon.fail(this);
			GLog.n(Messages.get(ElementalReactions.class, "ondeath"));
		}
	}

	//상태이상이 걸려 있는 동안 몸에서 계속 나오는 입자 (정전기 스파크, 젖음 물방울).
	//원작은 죽을 때 버프를 떼지 않으므로, 대상이 죽으면 스스로 멈추고 안 보일 때는 숨긴다.
	public static Emitter statusEmitter( final Char ch, Emitter.Factory factory, float interval ){
		if (ch.sprite == null || ch.sprite.parent == null) return null;
		Emitter e = new Emitter(){
			@Override
			public void update() {
				if (on && (!ch.isAlive() || ch.sprite == null)) on = false;
				if (ch.sprite != null) visible = ch.sprite.visible;
				super.update();
			}
		};
		e.pos(ch.sprite);
		ch.sprite.parent.add(e);
		e.pour(factory, interval);
		return e;
	}

	// ===== 피해 먼저, 빙결은 나중에 =====
	//원작은 피해를 받으면 빙결이 풀린다. 공격의 적중 시 효과(지팡이 냉기, 서리 베기 등)로 빙결이 걸려도
	//곧바로 그 공격의 피해에 깨지지 않도록, 공격·피해 처리 중에 생긴 빙결은 피해가 끝난 뒤에 건다.
	private static Char deferTarget = null;
	private static int deferDepth = 0;
	private static boolean pendingFreeze = false;

	//Char.attack(적중 처리 시작)과 Char.damage(시작)에서 호출
	public static void beginDefer( Char ch ){
		if (deferTarget == null){
			deferTarget = ch;
			deferDepth = 0;
			pendingFreeze = false;
		}
		if (deferTarget == ch) deferDepth++;
	}

	//Char.attack(피해 뒤)과 Char.damage(끝)에서 호출. 가장 바깥에서 끝날 때 미뤄 둔 빙결을 건다.
	public static void endDefer( Char ch ){
		if (deferTarget != ch) return;
		deferDepth--;
		if (deferDepth > 0) return;
		boolean freeze = pendingFreeze;
		deferTarget = null;
		pendingFreeze = false;
		if (freeze && ch.isAlive() && ch.buff(Frost.class) == null){
			applyFreeze(ch);
		}
	}

	//빙결이 풀릴 때 원작이 거는 냉기로는 다시 얼리지 않는다 (Frost.detach)
	public static boolean suppressFreeze = false;

	//맵 위의 올바른 칸에 있는지 (배치 전·제거 후 대상 보호)
	public static boolean validPos( Char ch ){
		return ch != null && Dungeon.level != null && ch.pos >= 0 && ch.pos < Dungeon.level.length();
	}

	public static boolean reacts( Char ch ){
		return ch != null && ch.alignment != Char.Alignment.ALLY;
	}

	public static boolean isWet( Char ch ){
		//젖음에 면역이면 물 위에서도 젖지 않는다 (썩은 수액 덩어리 등)
		if (ch.isImmune(Wet.class)) return false;
		//빙결 상태면 물 위에 있어도 젖지 않은 것으로 본다
		if (ch.buff(Frost.class) != null) return false;
		return ch.buff(Wet.class) != null || Wet.onWater(ch);
	}

	//기준 피해: 층이 깊을수록 커진다. 잔류원소 2레벨이면 +50%.
	public static int baseDamage(){
		float base = MazeTower.REACTION_BASE_FLAT + MazeTower.REACTION_BASE_PER_DEPTH * Dungeon.scalingDepth();
		base *= BloomEffects.reactionDamageMulti(Dungeon.hero);
		return Math.max(1, Math.round(base));
	}

	// ===== 상태이상이 붙기 직전 (true를 돌려주면 그 상태이상은 붙지 않는다) =====

	//화상이 붙기 직전 (Burning.attachTo)
	public static boolean beforeBurning( Char ch, Burning burning ){
		if (ch.isImmune(Burning.class)) return false; //면역이면 붙지도, 반응하지도 않는다
		//젖음 + 화상: 어떤 대상이든 둘 다 사라진다 (다른 반응보다 먼저)
		if (isWet(ch)){
			Buff.detach(ch, Wet.class);
			douse(ch);
			return true;
		}
		if (!reacts(ch)){
			Buff.detach(ch, Chill.class); //원작 처리
			return false;
		}
		//증기·해빙은 화상 지속시간을 알아야 하므로 일단 붙이고, 지속시간이 정해지는 reignite에서 터뜨린다
		if (ch.buff(Frost.class) != null){
			Buff.detach(ch, Frost.class);
			Buff.detach(ch, Chill.class); //물 위에서 빙결이 풀릴 때 원작이 거는 냉기도 함께 지운다
			burning.pendingReaction = PENDING_THAW;
			return false;
		}
		//혹한도 냉기로 판정한다 (혹한은 지우지 않는다)
		if (ch.buff(Chill.class) != null || ch.buff(Frostbite.class) != null){
			Buff.detach(ch, Chill.class);
			burning.pendingReaction = PENDING_STEAM;
			return false;
		}
		if (ch.buff(StaticCharge.class) != null){
			explode(ch);
		}
		return false;
	}

	//냉기가 붙기 직전 (Chill.attachTo)
	public static boolean beforeChill( Char ch ){
		if (ch.isImmune(Chill.class)) return false;
		if (!reacts(ch)){
			Buff.detach(ch, Burning.class); //원작 처리
			return false;
		}
		if (ch.buff(Burning.class) != null){
			steamFromBurning(ch, ch.buff(Burning.class));
			return true;
		}
		if (!suppressFreeze && isWet(ch) && ch.buff(Frost.class) == null){
			//빙결: 젖음과 냉기는 사라지고 얼어붙는다 (냉기는 붙지 않는다)
			Buff.detach(ch, Wet.class);
			freeze(ch);
			return true;
		}
		return false;
	}

	//혹한이 붙는 순간 (Frostbite.attachTo): 냉기처럼 반응하지만 혹한 자체는 지우지 않는다
	public static void onFrostbite( Char ch ){
		if (!reacts(ch)){
			Buff.detach(ch, Burning.class); //주인공·아군은 원작 냉기처럼 화상만 지운다
			return;
		}
		if (ch.buff(Burning.class) != null){
			steamFromBurning(ch, ch.buff(Burning.class));
			return;
		}
		if (!suppressFreeze && isWet(ch) && ch.buff(Frost.class) == null){
			Buff.detach(ch, Wet.class);
			freeze(ch); //젖은 적은 확정 빙결
		}
	}

	//빙결이 붙기 직전 (Frost.attachTo)
	public static boolean beforeFrost( Char ch ){
		if (ch.isImmune(Frost.class)) return false;
		if (!reacts(ch)){
			Buff.detach(ch, Burning.class); //원작 처리
			return false;
		}
		if (ch.buff(Burning.class) != null){
			thawFromBurning(ch, ch.buff(Burning.class));
			return true;
		}
		return false;
	}

	//젖음이 붙기 직전 (Wet.attachTo): 불타고 있으면 둘 다 사라진다
	public static boolean beforeWet( Char ch ){
		if (ch.isImmune(Wet.class)) return false;
		Burning b = ch.buff(Burning.class);
		if (b != null){
			b.detach();
			douse(ch);
			return true;
		}
		//냉기 상태로 젖으면: 냉기·젖음은 사라지고 얼어붙는다
		if (reacts(ch) && (ch.buff(Chill.class) != null || ch.buff(Frostbite.class) != null) && ch.buff(Frost.class) == null){
			Buff.detach(ch, Chill.class); //혹한은 남는다
			freeze(ch);
			return true;
		}
		return false;
	}

	//불이 꺼지는 효과 (반응 피해 없음)
	private static void douse( Char ch ){
		if (ch.sprite != null && validPos(ch) && Dungeon.level.heroFOV[ch.pos]){
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 4);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.5f);
		}
	}

	//정전기가 붙은 직후 (StaticCharge.attachTo)
	public static void onStaticApplied( Char ch ){
		if (reacts(ch) && ch.buff(Burning.class) != null){
			explode(ch);
		}
	}

	// ===== 전격 피해 → 정전기 =====

	public static boolean isElectric( Object src ){
		return src instanceof Shocking
				|| src instanceof WandOfLightning
				|| src instanceof Electricity
				|| src instanceof DM100.LightningBolt
				|| src instanceof BloomEffects.ShockDamage;
	}

	//Char.damage에서 호출
	public static void onDamage( Char ch, int dmg, Object src ){
		if (dmg > 0 && ch.isAlive() && isElectric(src)){
			Buff.prolong(ch, StaticCharge.class, MazeTower.STATIC_TURNS);
		}
	}

	// ===== 반응 =====

	public static final int PENDING_NONE  = 0;
	public static final int PENDING_STEAM = 1;
	public static final int PENDING_THAW  = 2;

	//Burning.reignite/act에서 호출: 미뤄 둔 증기·해빙을 일으킨다
	public static void resolvePending( Char ch, Burning burning ){
		int kind = burning.pendingReaction;
		burning.pendingReaction = PENDING_NONE;
		if (kind == PENDING_STEAM)     steamFromBurning(ch, burning);
		else if (kind == PENDING_THAW) thawFromBurning(ch, burning);
	}

	//남은 화상 지속시간 동안 받을 화상 피해를 한꺼번에 계산해 배율을 곱한다. 화상은 해제.
	private static int burnReactionDamage( Burning burning, float multi ){
		int burnTotal = burning.totalIncomingDMG();
		if (burning.target != null) burning.detach(); //붙지 못한 화상이면 뗄 대상이 없다
		float dmg = burnTotal * multi * BloomEffects.reactionDamageMulti(Dungeon.hero);
		return Math.max(1, Math.round(dmg));
	}

	//증기: 남은 화상 피해 합계 × 1.5, 화상·냉기 해제
	public static void steamFromBurning( Char ch, Burning burning ){
		steam(ch, burnReactionDamage(burning, MazeTower.REACTION_STEAM_MULTI));
	}

	//해빙: 남은 화상 피해 합계 × 2, 빙결·화상 해제
	public static void thawFromBurning( Char ch, Burning burning ){
		thaw(ch, burnReactionDamage(burning, MazeTower.REACTION_THAW_MULTI));
	}

	//반응 이펙트를 그려도 되는지 (주인공 시야 안)
	private static boolean seen( Char ch ){
		return ch.sprite != null && validPos(ch) && Dungeon.level.heroFOV[ch.pos];
	}

	//증기: 큰 김 (Speck.STEAM을 그 칸과 주변 칸에)
	private static void steam( Char ch, int dmg ){
		announce(ch, "steam", 0xDDDDDD);
		if (seen(ch)){
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 25);
			for (int n : PathFinder.NEIGHBOURS8){
				int c = ch.pos + n;
				if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c]){
					CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), 4);
				}
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		Sample.INSTANCE.play(Assets.Sounds.GAS, 0.6f);
		ch.damage(dmg, new ReactionDamage());
	}

	private static boolean exploding = false;

	//폭발: BlastParticle + 번개 섬광 (중심에서 맞는 적들로 번개, 화면 흔들림)
	private static void explode( Char ch ){
		if (exploding) return;
		exploding = true;
		int dmg = Math.round(baseDamage() * MazeTower.REACTION_EXPLOSION_MULTI);
		announce(ch, "explosion", 0xFF8800);

		ArrayList<Char> hit = new ArrayList<>();
		for (Char c : Actor.chars()){
			//폭발은 적·주인공·아군 가리지 않고 1칸 안의 모두에게
			if (c.isAlive()
					&& Dungeon.level.distance(ch.pos, c.pos) <= 1){
				hit.add(c);
			}
		}

		if (seen(ch)){
			CellEmitter.center(ch.pos).burst(BlastParticle.FACTORY, 30);
			CellEmitter.center(ch.pos).burst(SparkParticle.FACTORY, 20);
			ArrayList<Lightning.Arc> arcs = new ArrayList<>();
			for (Char c : hit){
				if (c != ch && c.sprite != null){
					arcs.add(new Lightning.Arc(ch.sprite.center(), c.sprite.center()));
				}
				if (c.sprite != null) c.sprite.flash();
			}
			if (!arcs.isEmpty() && ch.sprite.parent != null){
				ch.sprite.parent.addToFront(new Lightning(arcs, null));
			}
			PixelScene.shake(2, 0.3f);
		}
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);

		for (Char c : hit){
			c.damage(dmg, new ReactionDamage());
		}
		exploding = false;
	}

	//해빙: 얼음이 깨지며(원작 IceBlock이 녹고 얼음 조각) 불꽃이 튄다
	private static void thaw( Char ch, int dmg ){
		announce(ch, "thaw", 0x88CCFF);
		if (seen(ch)){
			Splash.at(ch.pos, 0xCCEEFF, 12);
			ch.sprite.emitter().burst(FlameParticle.FACTORY, 12);
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 8);
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f);
		ch.damage(dmg, new ReactionDamage());
	}

	//빙결 생성: 눈보라 + 원작 IceBlock (Frost가 붙으면 자동으로 얼음 덩어리가 생긴다)
	private static void freeze( Char ch ){
		//공격·피해 처리 중이면 피해가 끝난 뒤에 얼린다 (endDefer)
		if (deferTarget == ch){
			pendingFreeze = true;
			return;
		}
		applyFreeze(ch);
	}

	private static void applyFreeze( Char ch ){
		announce(ch, "freeze", 0x66AAFF);
		if (seen(ch)){
			ch.sprite.emitter().burst(SnowParticle.FACTORY, 20);
			Splash.at(ch.pos, 0x99CCFF, 6);
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.5f, 1.4f);
		Buff.affect(ch, Frost.class, MazeTower.FREEZE_TURNS);
	}

	private static void announce( Char ch, String key, int color ){
		if (ch.sprite != null && validPos(ch) && Dungeon.level.heroFOV[ch.pos]){
			ch.sprite.showStatus(color, Messages.get(ElementalReactions.class, key));
		}
	}
}
