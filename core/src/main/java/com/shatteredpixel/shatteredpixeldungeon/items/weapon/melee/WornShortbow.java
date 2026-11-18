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

//Maze Tower: 낡은 단궁 (사냥꾼 시작 무기, 1티어, 활 계열).
//  1칸(인접)   근접 평타: 근접 피해, 맞히면 1칸 밀쳐냄, 화살 안 씀
//  2칸         원거리 평타: 원거리 피해, 명중 -30%, 화살 1발
//  3칸 이상    원거리 평타: 원거리 피해, 화살 1발
//스킬은 조준 사격 (SwordSkill, 원거리 피해 100%, 맞으면 1칸 밀쳐냄, 화살 1발).
//그림은 임시로 원작 정령의 활. 수치는 MazeTower.
public class WornShortbow extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SPIRIT_BOW;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 1;
		bones = false;
	}

	// ===== 피해 =====

	//근접 피해 (무기 정보의 기본 피해로도 쓰인다)
	@Override
	public int min(int lvl) {
		return MazeTower.BOW_MELEE_MIN + lvl * MazeTower.BOW_MELEE_MIN_SCALE;
	}

	@Override
	public int max(int lvl) {
		return MazeTower.BOW_MELEE_MAX + lvl * MazeTower.BOW_MELEE_MAX_SCALE;
	}

	public int rangedMin(int lvl){
		return MazeTower.BOW_RANGED_MIN + lvl * MazeTower.BOW_RANGED_MIN_SCALE;
	}

	public int rangedMax(int lvl){
		return MazeTower.BOW_RANGED_MAX + lvl * MazeTower.BOW_RANGED_MAX_SCALE;
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
			return damage;
		}
		return super.damageRoll(owner);
	}

	@Override
	public float accuracyFactor(Char owner, Char target) {
		float acc = super.accuracyFactor(owner, target);
		//2칸 거리 원거리 평타는 명중 패널티 (조준 사격은 제외)
		if (shooting && SwordSkill.active == null && target != null
				&& Dungeon.level.distance(owner.pos, target.pos) == 2){
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

	public static boolean clearShot(int from, int target){
		return new Ballistica(from, target, Ballistica.PROJECTILE).collisionPos == target;
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
				&& !Dungeon.level.adjacent(hero.pos, target.pos)
				&& Arrow.count(hero) > 0;
	}

	//활 시위를 당기는 동작 뒤에 화살이 날아가고, 닿으면 then을 부른다
	public static void fireAnimation(final Hero hero, final Char target, final Callback then){
		hero.sprite.attack(target.pos, new Callback() {
			@Override
			public void call() {
				hero.sprite.idle();
				Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1, 1, 0.95f);
				((MissileSprite)hero.sprite.parent.recycle(MissileSprite.class))
						.reset(hero.sprite, target.sprite, new Arrow(), then);
			}
		});
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
		if (shot){
			if (!hit){
				Arrow.dropAt(target.pos, 1);
			} else if (target.isAlive()){
				Arrow.Stuck.add(target);
			} else {
				Arrow.recover(target.pos, 1);
			}
		}
		if (push && hit) knockback(hero, target);
	}

	public static void knockback(Hero hero, Char enemy){
		if (!enemy.isAlive() || Pushing.pushingExistsForChar(enemy) || enemy.properties().contains(Char.Property.IMMOVABLE)) return;
		Ballistica trajectory = new Ballistica(hero.pos, enemy.pos, Ballistica.STOP_TARGET);
		trajectory = new Ballistica(trajectory.collisionPos, trajectory.path.get(trajectory.path.size() - 1), Ballistica.PROJECTILE);
		WandOfBlastWave.throwChar(enemy, trajectory, MazeTower.BOW_KNOCKBACK, true, false, hero);
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
