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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM100;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Maze Tower: 원소 반응. 원작에서 화염과 냉기가 서로를 지우던 처리를 아래 반응으로 바꾼다.
//
//  증기: 화상 + 냉기       → 남은 화상 지속시간의 화상 피해 합계 × 1.5, 화상·냉기 해제
//  폭발: 정전기 + 화상     → 인접 1칸의 모든 적에게 기준 피해 × 1.2
//  해빙: 빙결 + 화염       → 남은 화상 지속시간의 화상 피해 합계 × 2, 빙결·화상 해제
//  빙결: 젖은 적 + 냉기    → 원작 Frost
//
//반응은 적(주인공·아군이 아닌 대상)에게만 일어난다. 주인공·아군은 원작처럼 서로 지우기만 한다.
public class ElementalReactions {

	//반응 피해의 출처 표시용
	public static class ReactionDamage {}

	//빙결이 풀릴 때 원작이 거는 냉기로는 다시 얼리지 않는다 (Frost.detach)
	public static boolean suppressFreeze = false;

	private static boolean reacts( Char ch ){
		return ch != null && ch.alignment != Char.Alignment.ALLY;
	}

	public static boolean isWet( Char ch ){
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
		if (!reacts(ch)){
			Buff.detach(ch, Chill.class); //원작 처리
			return false;
		}
		//증기·해빙은 화상 지속시간을 알아야 하므로 일단 붙이고, 지속시간이 정해지는 reignite에서 터뜨린다
		if (ch.buff(Frost.class) != null){
			Buff.detach(ch, Frost.class);
			burning.pendingReaction = PENDING_THAW;
			return false;
		}
		if (ch.buff(Chill.class) != null){
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
		if (!reacts(ch)){
			Buff.detach(ch, Burning.class); //원작 처리
			return false;
		}
		if (ch.buff(Burning.class) != null){
			steamFromBurning(ch, ch.buff(Burning.class));
			return true;
		}
		if (!suppressFreeze && isWet(ch) && ch.buff(Frost.class) == null){
			freeze(ch);
			return true;
		}
		return false;
	}

	//빙결이 붙기 직전 (Frost.attachTo)
	public static boolean beforeFrost( Char ch ){
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
		burning.detach();
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

	private static void steam( Char ch, int dmg ){
		announce(ch, "steam", 0xCCCCCC);
		if (ch.sprite != null) CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 10);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		ch.damage(dmg, new ReactionDamage());
	}

	private static boolean exploding = false;

	private static void explode( Char ch ){
		if (exploding) return;
		exploding = true;
		int dmg = Math.round(baseDamage() * MazeTower.REACTION_EXPLOSION_MULTI);
		announce(ch, "explosion", 0xFF8800);
		CellEmitter.center(ch.pos).burst(BlastParticle.FACTORY, 20);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);

		ArrayList<Char> hit = new ArrayList<>();
		for (Char c : Actor.chars()){
			if (c.isAlive() && c.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.distance(ch.pos, c.pos) <= 1){
				hit.add(c);
			}
		}
		for (Char c : hit){
			c.damage(dmg, new ReactionDamage());
		}
		exploding = false;
	}

	private static void thaw( Char ch, int dmg ){
		announce(ch, "thaw", 0x88CCFF);
		if (ch.sprite != null){
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 8);
			ch.sprite.emitter().burst(SnowParticle.FACTORY, 8);
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		ch.damage(dmg, new ReactionDamage());
	}

	private static void freeze( Char ch ){
		announce(ch, "freeze", 0x66AAFF);
		Buff.affect(ch, Frost.class, MazeTower.FREEZE_TURNS);
	}

	private static void announce( Char ch, String key, int color ){
		if (ch.sprite != null && Dungeon.level.heroFOV[ch.pos]){
			ch.sprite.showStatus(color, Messages.get(ElementalReactions.class, key));
		}
	}
}
