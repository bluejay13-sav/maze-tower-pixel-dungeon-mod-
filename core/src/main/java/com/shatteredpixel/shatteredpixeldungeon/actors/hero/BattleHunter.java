/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.FinisherIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Visual;
import com.watabou.utils.PathFinder;

//Maze Tower (지시서 1127, S6): 3차 전직 전투 사냥꾼 (사냥꾼, 활 전용). 활을 들었을 때만 아래 규칙이 켜진다 (WornShortbow.battleHunter).
// 가까운 사격: 옆 칸의 적도 화살로 쏜다(원거리 판정, 화살을 쓰고 밀치지 않으며 근접 평타 효과를 받지 않음). 2칸 거리의 명중 −30%가 없다.
//             활 스킬도 옆 칸의 적에게 쓸 수 있다. 화살이 없으면 옆 칸의 적을 누르는 기본 공격이 저절로 활대 타격이 된다.
// 활대 타격: 따로 된 단추(필살기 단추 자리)로 옆 칸의 적을 친다 — 지금 활의 근접 평타와 같은 공격(화살 안 씀, 1칸 밀쳐냄, 근접 평타 효과 모두).
// 사거리: 활의 최대 사거리가 절반(올림, WornShortbow.range). 최대 사거리를 보는 다른 규칙도 줄어든 값을 본다.
public class BattleHunter {

	public static boolean is( Hero hero ){
		return hero != null && hero.subClass == HeroSubClass.BATTLEHUNTER;
	}


	static int lvl( Hero hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent t ){
		return hero == null ? 0 : Math.min( com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3_MAX_TALENT_LEVEL, hero.pointsInTalent( t ) );
	}

	//2칸 안의 적에게 쏜 화살인가 (평타 사격·활 스킬 모두, 활을 들었을 때만)
	private static int closeShotDist( Hero hero, Char enemy ){
		if (enemy == null || !WornShortbow.battleHunter( hero ) || !WornShortbow.isShooting()) return -1;
		int d = Dungeon.level.distance( hero.pos, enemy.pos );
		return d <= 2 ? d : -1;
	}

	// ----- (S7) 코앞 사격: 1칸 화살 피해 +10/20/30/40%, 2칸 +6/12/18/24% (BloomEffects.processHit) -----
	public static int hitDamage( Hero hero, Char enemy, int damage ){
		int d = closeShotDist( hero, enemy );
		int l = lvl( hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CLOSE_SHOT );
		if (d < 0 || l <= 0) return damage;
		float b = d <= 1 ? com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_POINT_BLANK_1[l] : com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_POINT_BLANK_2[l];
		BloomRules.debug( "close shot +" + Math.round( b * 100 ) + "% (" + d + " tiles)" );
		return Math.round( damage * (1f + b) );
	}

	// ----- (S7) 코앞 사격 II: 2칸 안의 적에게 쏜 화살의 명중 +10/20/30/40% (Hero.attackSkill ← BloomEffects.accuracyMulti) -----
	public static float accuracyMulti( Hero hero, Char target ){
		int l = lvl( hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.CLOSE_SHOT_II );
		if (l <= 0 || closeShotDist( hero, target ) < 0) return 1f;
		return 1f + com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_POINT_BLANK_ACC[l];
	}

	// ----- (S7) 맞불: 2칸 안의 적을 화살로 맞힐 때마다 방어막(원작 Barrier) 2/3/4/5, 상한 8/12/16/20 (BloomEffects.afterHeroAttackProc) -----
	public static void onArrowHit( Hero hero, Char enemy ){
		int l = lvl( hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.RETURN_FIRE );
		if (l <= 0 || closeShotDist( hero, enemy ) < 0) return;
		int cap = com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_RETURN_FIRE_CAP[l];
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier b = hero.buff( com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class );
		int cur = b == null ? 0 : b.shielding();
		int add = Math.min( com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_RETURN_FIRE[l], cap - cur );
		if (add <= 0) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect( hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class ).incShield( add );
		hero.sprite.showStatusWithIcon( com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE, Integer.toString( add ), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.SHIELDING );
	}

	// ----- (S7) 발 빼기: 활대 타격으로 밀쳐낸 적이 실제로 밀려나면 2/3/4/5턴 느려짐(원작 Cripple, 면역이면 안 걸림) -----
	private static Char stepBackTarget = null;

	//활 근접 평타의 밀쳐내기 직전 (WornShortbow.knockback)
	public static void markStepBack( Hero hero, Char enemy ){
		stepBackTarget = WornShortbow.battleHunter( hero ) && lvl( hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.STEP_BACK ) > 0 ? enemy : null;
	}

	//밀쳐내기가 시작될 때 (WandOfBlastWave.throwChar): 이 밀침이 발 빼기 대상인지 가져가고 지운다
	public static boolean takeStepBack( Char ch ){
		boolean r = ch != null && ch == stepBackTarget;
		stepBackTarget = null;
		return r;
	}

	//실제로 밀려났을 때 (WandOfBlastWave.throwChar의 밀림이 끝난 뒤)
	public static void applyStepBack( Char ch ){
		Hero hero = Dungeon.hero;
		int l = lvl( hero, com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.STEP_BACK );
		if (l <= 0 || !ch.isAlive()) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.prolong( ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple.class, com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_STEP_BACK[l] );
		BloomRules.debug( "step back: " + ch.name() + " crippled " + com.shatteredpixel.shatteredpixeldungeon.MazeTower.T3B_STEP_BACK[l] );
	}

	// ----- 활대 타격 단추 (필살기 단추 자리를 빌린다: 전투 사냥꾼은 필살기가 없다) -----
	public static class BashButton implements ActionIndicator.Action {
		public static final BashButton INSTANCE = new BashButton();
		private Object shownWeapon = null;

		//상태창이 매 프레임 부른다 (Mana.refreshSkillButton). 활을 들었을 때만 보인다
		public void refresh(){
			Hero h = Dungeon.hero;
			if (h == null || !WornShortbow.battleHunter( h )){
				if (FinisherIndicator.action == this) FinisherIndicator.clearAction( this );
				shownWeapon = null;
				return;
			}
			Object w = h.belongings.weapon();
			if (FinisherIndicator.action != this || shownWeapon != w){
				shownWeapon = w;
				FinisherIndicator.setAction( this );
			}
		}

		@Override
		public String actionName() {
			return Messages.get( BattleHunter.class, "bash" );
		}

		@Override
		public Visual primaryVisual() {
			Hero h = Dungeon.hero;
			if (h == null || h.belongings.weapon() == null) return new ItemSprite();
			return new ItemSprite( h.belongings.weapon() );
		}

		@Override
		public Visual secondaryVisual() {
			return null;
		}

		@Override
		public int indicatorColor() {
			return 0x6A8C3A;
		}

		@Override
		public void doAction() {
			final Hero h = Dungeon.hero;
			if (h == null || !WornShortbow.battleHunter( h )) return;
			//옆 칸의 적이 하나면 바로 친다, 여럿이면 고른다
			Char only = null;
			int count = 0;
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( h.pos + n );
				if (bashable( h, ch )){ only = ch; count++; }
			}
			if (count == 0){
				GLog.w( Messages.get( BattleHunter.class, "no_adjacent" ) );
				return;
			}
			if (count == 1){
				bash( h, only );
				return;
			}
			GameScene.selectCell( new CellSelector.Listener() {
				@Override
				public void onSelect( Integer cell ) {
					if (cell == null) return;
					Char ch = Actor.findChar( cell );
					if (!bashable( h, ch )){
						GLog.w( Messages.get( BattleHunter.class, "no_adjacent" ) );
						return;
					}
					bash( h, ch );
				}
				@Override
				public String prompt() {
					return Messages.get( BattleHunter.class, "prompt" );
				}
			} );
		}

		private static boolean bashable( Hero h, Char ch ){
			//(점검) 적과 누르면 치는 소품(감방 문 등)만. 상점 주인·NPC는 누르면 말 걸기가 되어 활대 타격 표시가 남았다
			return ch != null && ch != h && ch.isAlive()
					&& (ch.alignment == Char.Alignment.ENEMY || (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob && ((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob) ch).mazeFixed()))
					&& ch.invisible <= 0 && Dungeon.level.adjacent( h.pos, ch.pos );
		}

		//원작 근접 평타로 친다 (WornShortbow.bashNext: 이번 한 번은 옆 칸이어도 화살을 쏘지 않는다)
		private static void bash( Hero h, Char ch ){
			WornShortbow.bashNext = true;
			boolean acted = h.handle( ch.pos );
			//(점검) 그 대상을 치는 행동이 아니게 되면(말 걸기·이동 등) 표시를 바로 지운다
			if (!(h.curAction instanceof HeroAction.Attack) || ((HeroAction.Attack) h.curAction).target != ch) WornShortbow.bashNext = false;
			if (acted) h.next();
		}
	}
}
