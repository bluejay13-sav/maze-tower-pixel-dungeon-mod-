/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BruteSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Brute extends Mob {
	
	{
		spriteClass = BruteSprite.class;
		
		HP = HT = 40;
		defenseSkill = 15;
		
		EXP = 8;
		maxLvl = 16;
		
		loot = Gold.class;
		lootChance = 0.5f;
	}
	
	protected boolean hasRaged = false;
	
	@Override
	public int damageRoll() {
		return buff(BruteRage.class) != null ?
				Random.NormalIntRange( 15, 40 ) :
				Random.NormalIntRange( 5, 25 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 20;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 8);
	}

	//Maze Tower (J4): 갱도에 갇힌 광부. 광폭이 체력 회복으로 풀렸는지 (풀린 뒤 쓰러지면 터지지 않는다, 저장됨)
	protected boolean mazeRageEnded = false;

	//Maze Tower (J4): 지금 광폭 중인지 (그림의 광폭 줄, 마석 터짐 판정)
	public boolean mazeRaging(){
		return hasRaged && !mazeRageEnded && HP <= 0;
	}

	@Override
	public void die(Object cause) {
		//Maze Tower (J4): 광폭 중에 쓰러지면(맞아서든, 광폭이 저절로 꺼져서든 — 결정) 몸의 마석이 터진다. 구덩이는 안 터짐
		boolean burst = mazeRaging() && cause != Chasm.class;

		super.die(cause);

		if (cause == Chasm.class){
			hasRaged = true; //don't let enrage trigger for chasm deaths
		}

		if (burst) mazeBurst();
	}

	//Maze Tower (J4): 둘레 1칸의 모두(주인공·다른 적 포함)에게 피해
	private void mazeBurst(){
		if (Dungeon.level == null) return;
		boolean seen = Dungeon.level.heroFOV[pos];
		if (seen){
			MazeFx.atCell(MazeFx.Fx.MINE_BURST, pos); //(사용자) 광부 칸 가운데에 푸른 폭발 한 번 (9칸마다 띄우지 않음)
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.9f);
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 1.3f);
			PixelScene.shake(1, 0.15f);
			GLog.w(Messages.get(Brute.class, "burst"));
		}
		for (int i : PathFinder.NEIGHBOURS8){
			int c = pos + i;
			if (c < 0 || c >= Dungeon.level.length()) continue;
			Char ch = Actor.findChar(c);
			if (ch == null || !ch.isAlive()) continue;
			ch.damage(Random.NormalIntRange(MazeTower.J4_BURST_MIN, MazeTower.J4_BURST_MAX), new MineBurst());
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(MineBurst.class);
				GLog.n(Messages.get(Brute.class, "burst_kill"));
			}
		}
	}

	//Maze Tower (J4): 사망 기록용 피해 출처
	public static class MineBurst {}

	//cache this buff to prevent having to call buff(...) a bunch in isAlive
	private BruteRage rage;

	@Override
	public boolean isAlive() {
		if (super.isAlive()){
			return true;
		} else {
			if (!hasRaged){
				triggerEnrage();
			}
			if (rage == null){
				for (BruteRage b : buffs(BruteRage.class)){
					rage = b;
				}
			}
			return rage != null && rage.shielding() > 0;
		}
	}
	
	protected void triggerEnrage(){
		rage = Buff.affect(this, BruteRage.class);
		rage.setShield(HT/2 + 4);
		sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(HT/2), FloatingText.SHIELDING );
		if (Dungeon.level.heroFOV[pos]) {
			SpellSprite.show( this, SpellSprite.BERSERK);
		}
		spend( TICK );
		hasRaged = true;
	}
	
	private static final String HAS_RAGED = "has_raged";
	private static final String RAGE_ENDED = "maze_rage_ended";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HAS_RAGED, hasRaged);
		bundle.put(RAGE_ENDED, mazeRageEnded);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		hasRaged = bundle.getBoolean(HAS_RAGED);
		mazeRageEnded = bundle.getBoolean(RAGE_ENDED);
	}
	
	public static class BruteRage extends ShieldBuff {
		
		{
			type = buffType.POSITIVE;
		}
		
		@Override
		public boolean act() {
			
			if (target.HP > 0){
				if (target instanceof Brute) ((Brute) target).mazeRageEnded = true; //Maze Tower (J4)
				detach();
				return true;
			}

			absorbDamage( Math.round(4*AscensionChallenge.statModifier(target)));
			
			if (shielding() <= 0){
				target.die(null);
			}
			
			spend( TICK );
			
			return true;
		}
		
		@Override
		public void detach() {
			super.detach();
			decShield(shielding()); //clear shielding to track that this was detached
		}

		@Override
		public int icon () {
			return BuffIndicator.FURY;
		}
		
		@Override
		public String desc () {
			return Messages.get(this, "desc", shielding());
		}

	}
}
