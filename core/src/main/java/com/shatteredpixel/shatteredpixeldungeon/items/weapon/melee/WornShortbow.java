/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Maze Tower: 단궁 (사냥꾼 시작 무기, 1티어, 활 계열). 활 7종의 바탕 클래스이기도 하다(나머지 6종이 물려받는다).
//  (예전 이름 "낡은 단궁". 저장 파일과 맞도록 클래스 이름은 그대로 둔다)
//  1칸(인접)   근접 평타: 근접 피해, 맞히면 1칸 밀쳐냄, 화살 안 씀
//  2칸         원거리 평타: 원거리 피해, 명중 -30%, 화살 1발
//  3칸 이상    원거리 평타: 원거리 피해, 화살 1발
//스킬은 SwordSkill 표 (단궁은 조준 사격 100%, 명중 +30%, 화살 1발). 그림은 MazeItemSprites, 수치는 MazeTower (티어별 배열).
public class WornShortbow extends MeleeWeapon {

	{
		image = com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.BOW_SHORT;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 1;
		bones = false;
	}

	// ===== 피해 =====

	//근접 피해 (무기 정보의 기본 피해로도 쓰인다)
	@Override
	public int min(int lvl) {
		return MazeTower.BOW_MELEE_MIN[tier] + lvl;
	}

	@Override
	public int max(int lvl) {
		return MazeTower.BOW_MELEE_MAX[tier] + lvl;
	}

	public int rangedMin(int lvl){
		return MazeTower.BOW_RANGED_MIN[tier] + lvl;
	}

	public int rangedMax(int lvl){
		return MazeTower.BOW_RANGED_MAX[tier] + lvl * MazeTower.BOW_RANGED_MAX_SCALE[tier];
	}

	//지금 화살을 쏘는 중인지 (원거리 평타·조준 사격의 타격 판정 동안만 true)
	private static boolean shooting = false;

	public static boolean isShooting(){
		return shooting;
	}

	@Override
	public int damageRoll(Char owner) {
		if (shooting && owner instanceof Hero){
			int lvl = buffedLvl();
			int damage = augment.damageFactor(Hero.heroDamageIntRange(rangedMin(lvl), rangedMax(lvl)));
			int exStr = ((Hero)owner).STR() - STRReq();
			if (exStr > 0) damage += Hero.heroDamageIntRange(0, exStr);
			if (mazeMithril != null) damage = Math.round(damage * MazeTower.MITHRIL_WEAPON_DMG); //Maze Tower (K8): 미스릴 무기 피해 +20%
			return damage;
		}
		return super.damageRoll(owner);
	}

	@Override
	public float accuracyFactor(Char owner, Char target) {
		float acc = super.accuracyFactor(owner, target);
		//2칸 거리 원거리 평타는 명중 패널티 (조준 사격은 제외)
		if (shooting && SwordSkill.active == null && target != null
				&& Dungeon.level.distance(owner.pos, target.pos) == 2
				&& !(owner instanceof Hero && battleHunter((Hero) owner))){ //Maze Tower (S6): 전투 사냥꾼은 2칸 불이익이 없다
			acc *= MazeTower.BOW_CLOSE_RANGE_ACCURACY;
		}
		return acc;
	}

	//인접하지 않은 적도 화살이 있고 화살길이 열려 있으면 공격할 수 있다
	@Override
	public boolean canReach(Char owner, int target) {
		if (super.canReach(owner, target)) return true;
		if (!(owner instanceof Hero) || Arrow.count((Hero)owner) <= 0) return false;
		return clearShot(owner.pos, target);
	}

	//(결정) 활의 사거리는 모든 활이 같다: MazeTower.BOW_RANGE칸까지
	public static boolean clearShot(int from, int target){
		return inRange(from, target) && new Ballistica(from, target, Ballistica.PROJECTILE).collisionPos == target;
	}

	public static boolean inRange(int from, int target){
		return Dungeon.level.distance(from, target) <= range();
	}

	//Maze Tower (S6): 지금 활의 최대 사거리. 전투 사냥꾼이 활을 들었으면 절반(올림)
	public static int range(){
		return battleHunter(Dungeon.hero) ? (MazeTower.BOW_RANGE + 1) / 2 : MazeTower.BOW_RANGE;
	}

	//Maze Tower (S6): 3차 전투 사냥꾼이 활을 들었는가 (활을 내려놓으면 규칙이 모두 꺼진다)
	public static boolean battleHunter(Hero hero){
		return hero != null && hero.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.BATTLEHUNTER && equipped(hero) != null;
	}

	//Maze Tower (S6): 다음 근접 공격은 활대 타격 단추로 친 것 (옆 칸이어도 화살을 쏘지 않는다)
	public static boolean bashNext = false;

	//옆 칸의 적도 화살로 쏘는가 (전투 사냥꾼, 화살이 있고, 활대 타격 단추가 아닐 때)
	public static boolean shootsAdjacent(Hero hero){
		return battleHunter(hero) && !bashNext && Arrow.count(hero) > 0;
	}

	// ===== 주인공의 활 공격 (Hero, SwordSkill에서 부른다) =====

	public static WornShortbow equipped(Hero hero){
		if (hero == null) return null;
		KindOfWeapon w = hero.belongings.weapon();
		return w instanceof WornShortbow ? (WornShortbow)w : null;
	}

	//이 공격이 화살을 쏘는 원거리 공격인지
	public static boolean isShot(Hero hero, Char target){
		return equipped(hero) != null && target != null
				&& (!Dungeon.level.adjacent(hero.pos, target.pos) || shootsAdjacent(hero)) //Maze Tower (S6): 전투 사냥꾼은 옆 칸에도 쏜다
				&& Arrow.count(hero) > 0;
	}

	//활 시위를 당기는 동작 뒤에 화살이 날아가고, 닿으면 then을 부른다
	public static void fireAnimation(final Hero hero, final Char target, final Callback then){
		fireAnimation(hero, target, then, SHOT_PLAIN);
	}

	//화살 모양: 보통 / 장전된 화살(회색) / 꿰뚫는 화살(푸른 마법 입자)
	public static final int SHOT_PLAIN = 0, SHOT_LOADED = 1, SHOT_MAGIC = 2;

	public static void fireAnimation(final Hero hero, final Char target, final Callback then, final int kind){
		//(2026-11-22) 원작 정령의 활처럼: 짧은 던지기 동작(zap)과 동시에 화살이 날아간다.
		//  예전엔 근접 휘두르기 동작을 다 한 뒤에 화살이 나가서 어색했다
		hero.sprite.zap(target.pos, new Callback() { @Override public void call() { hero.sprite.idle(); } });
		launchArrow(hero, target, then, kind);
	}

	//시위 동작 없이 화살만 날린다 (속사의 두 번째 화살 등)
	public static void launchArrow(final Hero hero, final Char target, final Callback then, int kind){
		Sample.INSTANCE.play(kind == SHOT_LOADED ? Assets.Sounds.ATK_CROSSBOW : Assets.Sounds.ATK_SPIRITBOW, 1, 1, 0.95f);
		final MissileSprite m = (MissileSprite)hero.sprite.parent.recycle(MissileSprite.class);
		//(2026-11-21) 새 화살 그림: 기본 · 강화(석궁 장전, 회색 기운) · 정령 화살. 날아가는 입자는 없다
		m.reset(hero.sprite, target.sprite, new Arrow.Shot(kind), then);
		m.resetColor();
	}

	//Maze Tower (B5): 덧타격은 화살을 쓰지 않는다. 원거리 피해로만 친다. 끝나면 endShot(hero, target, false, false, false)
	public static boolean beginFreeShot(Hero hero){
		if (equipped(hero) == null) return false;
		shooting = true;
		return true;
	}

	//타격 판정 직전: 원거리면 화살 1발을 쓰고 원거리 피해로 친다
	public static boolean beginShot(Hero hero, Char target, boolean ranged){
		if (!ranged || equipped(hero) == null) return false;
		if (!Arrow.use(hero)) return false;
		shooting = true;
		return true;
	}

	//타격 판정 직후: 화살 처리와 밀쳐내기
	//  쏜 화살: 맞으면 적에게 박힘(죽을 때 일정 확률로 떨어짐), 빗나가면 그 자리에 떨어짐
	//  push: 맞으면 1칸 밀쳐냄 (근접 평타, 조준 사격)
	public static void endShot(Hero hero, Char target, boolean hit, boolean shot, boolean push){
		shooting = false;
		bashNext = false; //Maze Tower (S6)
		if (shot){
			if (!hit){
				Arrow.dropAt(target.pos, 1);
			} else if (target.isAlive()){
				Arrow.Stuck.add(target);
			} else {
				Arrow.recover(target.pos, 1);
			}
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.ForestWarden.onArrowAt(hero, target.pos); //Maze Tower (S8): 숲지기 - 화살이 떨어진 칸·맞은 적의 발밑에 풀
		}
		if (push && hit) knockback(hero, target);
	}

	public static void knockback(Hero hero, Char enemy){
		if (!enemy.isAlive() || Pushing.pushingExistsForChar(enemy) || enemy.properties().contains(Char.Property.IMMOVABLE)) return;
		Ballistica trajectory = new Ballistica(hero.pos, enemy.pos, Ballistica.STOP_TARGET);
		trajectory = new Ballistica(trajectory.collisionPos, trajectory.path.get(trajectory.path.size() - 1), Ballistica.PROJECTILE);
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.BattleHunter.markStepBack(hero, enemy); //Maze Tower (S7): 발 빼기
		WandOfBlastWave.throwChar(enemy, trajectory, MazeTower.BOW_KNOCKBACK, true, false, hero);
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomHuntress.onBowKnockback(hero, enemy); //Maze Tower (D21): 활대 치기 (밀쳐낸 뒤)
	}

	// ===== (사용자) 칸을 지정해 쏘기 =====
	//퀵슬롯(기본 행동 "쏘기") → 칸을 고르면 원거리 평타. 스킬 버튼 → 칸을 고르면 스킬(SwordSkill.perform).
	//화살길 위의 첫 캐릭터를 맞히고, 아무도 없으면 화살이 그 칸(사거리·벽에서 멈춘 칸)에 떨어진다.

	public static final String AC_SHOOT = "SHOOT";

	@Override
	public String defaultAction() {
		if (Dungeon.hero != null && SwordSkill.heroUsesSkills(Dungeon.hero) && isEquipped(Dungeon.hero)) return AC_SHOOT;
		return super.defaultAction();
	}

	@Override
	public java.util.ArrayList<String> actions(Hero hero) {
		java.util.ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && SwordSkill.heroUsesSkills(hero)) actions.add(0, AC_SHOOT);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_SHOOT)) return Messages.upperCase(Messages.get(WornShortbow.class, "ac_shoot"));
		return super.actionName(action, hero);
	}

	@Override
	public void execute(final Hero hero, String action) {
		if (!action.equals(AC_SHOOT)){ super.execute(hero, action); return; }
		curUser = hero;
		curItem = this;
		if (!isEquipped(hero)){
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(MeleeWeapon.class, "ability_need_equip"));
			return;
		}
		usesTargeting = true;
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.selectCell(new com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				//Maze Tower (사용자): 사거리(BOW_RANGE칸) 밖의 칸은 고를 수 없다 — 알려 주고 다시 고르게
				if (cell != null && cell != hero.pos && !inRange(hero.pos, cell)){
					com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(SwordSkill.class, "out_of_range", range()));
					com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.selectCell(this);
					return;
				}
				if (cell != null) shootCell(hero, cell);
			}
			@Override
			public String prompt() {
				return Messages.get(SwordSkill.class, "prompt_ranged");
			}
		});
	}

	//화살길: 주인공 → 고른 칸. 결과 {맞을 캐릭터가 있는 칸(없으면 -1), 화살이 떨어질 칸}.
	//  보통 화살은 벽이나 첫 캐릭터에서 멈춘다. 꿰뚫는 화살(pierce)은 벽을 지나 화살길 위의 첫 캐릭터에서 멈춘다.
	//  (사용자, 의도) 꿰뚫기는 시야가 없는 벽 뒤의 어둠에 쏘아도 그곳의 보이지 않는 적을 맞힌다.
	//  사거리(BOW_RANGE) 밖의 칸은 보지 않는다.
	public static int[] aim(Hero hero, int cell, boolean pierce){
		Ballistica b = new Ballistica(hero.pos, cell, pierce ? Ballistica.STOP_TARGET : Ballistica.PROJECTILE);
		int land = hero.pos;
		for (int c : b.subPath(1, b.dist)){
			if (!inRange(hero.pos, c)) break;
			com.shatteredpixel.shatteredpixeldungeon.actors.Char ch = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(c);
			if (ch != null && ch != hero && !(ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetAlly)){ //Maze Tower: 화살은 펫을 지나친다
				return new int[]{ c, c };
			}
			if (Dungeon.level.solid[c]){
				if (pierce) continue;
				break;
			}
			land = c;
		}
		return new int[]{ -1, land };
	}

	//쏘면 안 되는 캐릭터 (아군·말 거는 NPC)
	public static boolean blocksShot(Hero hero, com.shatteredpixel.shatteredpixeldungeon.actors.Char ch){
		return ch.alignment == com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY
				|| ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC
				|| hero.isCharmedBy(ch);
	}

	//퀵슬롯 "쏘기": 원거리 평타
	public static void shootCell(Hero hero, int cell){
		if (equipped(hero) == null || cell == hero.pos) return;
		//고른 칸의 캐릭터가 사거리 밖이면 쏘지 않는다 (퀵슬롯 두 번 누르기로 타게팅된 적이 멀 때도)
		com.shatteredpixel.shatteredpixeldungeon.actors.Char at = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(cell);
		if (!inRange(hero.pos, cell)){ //(사용자) 사거리 밖의 칸은 캐릭터가 없어도 쏘지 않는다
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(SwordSkill.class, "out_of_range", range()));
			return;
		}
		if (Arrow.count(hero) <= 0){
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(SwordSkill.class, "no_arrows"));
			return;
		}
		int[] a = aim(hero, cell, false);
		if (a[0] >= 0){
			com.shatteredpixel.shatteredpixeldungeon.actors.Char ch = com.shatteredpixel.shatteredpixeldungeon.actors.Actor.findChar(a[0]);
			if (blocksShot(hero, ch)){
				com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(SwordSkill.class, "no_path"));
				return;
			}
			//붙어 있는 적은 원래처럼 활대로 친다 (근접 평타). Maze Tower (S6): 전투 사냥꾼은 옆 칸에도 쏜다
			if (Dungeon.level.adjacent(hero.pos, ch.pos) && !shootsAdjacent(hero)){
				if (hero.handle(ch.pos)) hero.next();
				return;
			}
			hero.mazeBowShot(ch);
			return;
		}
		if (a[1] == hero.pos){
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(Messages.get(SwordSkill.class, "no_path"));
			return;
		}
		//장전·집중해 둔 한 발을 빈 칸에 쏘면 그 강화는 쓰인 것으로 친다
		BowCharge charged = hero.buff(BowCharge.class);
		int kind = SHOT_PLAIN;
		if (charged != null){
			if (charged.kind == BowCharge.LOAD) kind = SHOT_LOADED;
			charged.detach();
		}
		emptyShot(hero, a[1], kind);
	}

	//아무도 맞지 않는 화살: 화살 1발을 쓰고 그 칸에 떨어진다. 평타 한 번만큼 턴을 쓴다
	public static void emptyShot(final Hero hero, final int land, int kind){
		if (!Arrow.use(hero)) return;
		hero.busy();
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.dispel();
		hero.sprite.zap(land, new Callback() { @Override public void call() { hero.sprite.idle(); } });
		Sample.INSTANCE.play(kind == SHOT_LOADED ? Assets.Sounds.ATK_CROSSBOW : Assets.Sounds.ATK_SPIRITBOW, 1, 1, 0.95f);
		final MissileSprite m = (MissileSprite)hero.sprite.parent.recycle(MissileSprite.class);
		m.reset(hero.sprite, land, new Arrow.Shot(kind), new Callback() {
			@Override
			public void call() {
				Arrow.dropAt(land, 1);
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.ForestWarden.onArrowAt(hero, land); //Maze Tower (S8)
				hero.spendAndNext(hero.attackDelay());
			}
		});
		m.resetColor();
	}

	// ===== 설명 =====

	@Override
	public String desc() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return super.desc() + "\n\n" + Messages.get(WornShortbow.class, "bow_desc",
				augment.damageFactor(min(lvl)), augment.damageFactor(max(lvl)),
				augment.damageFactor(rangedMin(lvl)), augment.damageFactor(rangedMax(lvl)),
				Math.round((1 - MazeTower.BOW_CLOSE_RANGE_ACCURACY) * 100),
				Dungeon.hero != null ? Arrow.count(Dungeon.hero) : 0);
	}
}
