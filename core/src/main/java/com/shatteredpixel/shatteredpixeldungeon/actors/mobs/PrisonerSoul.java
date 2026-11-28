/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.utils.Random;

//Maze Tower: 죄수의 영혼 (10층 그림자 간수가 '영혼 해방'으로 철창에서 불러낸다).
//원작 감옥 층 경비병(Guard)과 비슷한 체력·공격으로 근접해 달려든다. 경험치·전리품 없음.
//그림은 6~9층 갇힌 영혼과 같은 푸른 영혼 (H6).
public class PrisonerSoul extends Mob {

	{
		spriteClass = SoulSprite.class;

		HP = HT = 40;
		defenseSkill = 10;
		EXP = 0;
		maxLvl = -2;

		state = HUNTING;
		flying = true;

		properties.add(Property.UNDEAD);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(4, 12);
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 7);
	}

	@Override
	public boolean reset() {
		return false;
	}

	//(사용자) 선고로 끌려 나온 영혼을 쓰러뜨려도 선고가 풀린다 (버텨 낸 것과 같이: 그 철창은 비고 사슬도 끊어진다, 간수를 붙들지는 않음)
	@Override
	public void die( Object cause ) {
		super.die(cause);
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
		if (h == null || !(com.shatteredpixel.shatteredpixeldungeon.Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel)) return;
		ShadowWarden.Sentence s = h.buff(ShadowWarden.Sentence.class);
		if (s == null || s.soulId != id()) return;
		((com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) com.shatteredpixel.shatteredpixeldungeon.Dungeon.level).endSentence(s, false);
		s.detach();
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.i( com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(ShadowWarden.class, "sentence_soul_slain") );
	}

	//(사용자) 10층 간수가 끌어낸 영혼은 주인공이 어디에 있든 곧장 다가와 공격한다 (주인공이 투명이면 원작대로)
	@Override
	protected boolean act() {
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel
				&& alignment == Alignment.ENEMY && h != null && h.isAlive() && h.invisible <= 0 && paralysed <= 0){
			aggro(h);
			target = h.pos;
			if (state != HUNTING) state = HUNTING;
		}
		return super.act();
	}

	//(H6) 6~9층 갇힌 영혼과 같은 푸른 영혼 그림 (예전 임시 그림: 원작 혼령에 분홍빛)
	public static class SoulSprite extends ChainedSoul.SoulSprite {
		//(I7) 간수의 선고로 끌려 나온 영혼은 철창과 사슬로 이어진다 (불러온 뒤에도)
		@Override
		public void link( Char ch ) {
			super.link(ch);
			com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence s =
					com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero == null ? null
					: com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.class);
			if (s != null && s.soulId == ch.id() && s.cageId != -1){
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupEffect(
						new com.shatteredpixel.shatteredpixeldungeon.effects.SoulChain(s.cageId, ch.id()));
				//(사용자) 선고가 걸린 주인공과도 사슬로 이어진다
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupEffect(
						new com.shatteredpixel.shatteredpixeldungeon.effects.SoulChain(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.id(), ch.id(), true));
			}
		}
	}
}
