/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//Maze Tower (T1·T3, 지시서 claude_code_tasks_1126b_pets): 펫 정령 매. 역할: 견제.
//- 날아다닌다(바닥 효과·함정 없음, 구덩이 위를 지남). 체력이 낮다.
//- 주인공이 공격한 적을 멀리서(PET_HAWK_RANGE칸 안, 가로막히지 않은 곳) 쫀다. 한 턴에 한 번, 약한 피해.
//  주인공이 공격한 적이 없으면 주인공 곁을 맴돈다. 직접 지시한 적이 있으면 그 적이 먼저.
//- 매가 보는 곳이 주인공 시야에 더해진다.
//- 강화: 날카로운 부리(쪼는 피해), 매의 눈(시야 반경), 성가신 날갯짓(쫀 적의 명중이 잠깐 떨어짐)
public class PetHawk extends PetAbility {

	@Override
	protected PetAlly create() {
		return new Hawk();
	}

	@Override
	public int icon() {
		return HeroIcon.SPIRIT_HAWK;
	}

	@Override
	public Talent[] talents() {
		//(T2) 강화 셋 + 넷째 칸 원작 영웅적인 에너지(갑옷 충전 소모 감소).
		return new Talent[]{ Talent.PET_BEAK, Talent.PET_HAWK_EYE, Talent.PET_FLAP, Talent.HEROIC_ENERGY };
	}

	//주인공이 적을 공격할 때 (Char.attack): 나와 있는 매가 그 적을 노린다
	public static void onHeroAttack( Char enemy ){
		PetAlly pet = current();
		if (pet instanceof Hawk) ((Hawk) pet).heroAttacked(enemy);
	}

	//성가신 날갯짓에 걸린 대상의 명중 배율 (Char.hit)
	public static float accuracyMulti( Char attacker ){
		Flapped f = attacker.buff(Flapped.class);
		return f == null ? 1f : 1f - f.reduce;
	}

	public static class Hawk extends PetAlly {

		{
			spriteClass = HawkSprite.class;

			HP = HT = MazeTower.PET_HAWK_HP;
			defenseSkill = MazeTower.PET_HAWK_DEF;

			flying = true;
			viewDistance = MazeTower.PET_HAWK_VIEW;
			baseSpeed = MazeTower.PET_HAWK_SPEED;
			attacksAutomatically = false;

			immunities.addAll(new BlobImmunity().immunities());
			//(사용자) 날아다녀 땅의 장판(불·냉기·전기 포함)은 받지 않지만, 키가 큰 불의 벽(마법의 불 방)은 받는다
			immunities.remove(com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom.EternalFire.class);
		}

		private int orderedId = -1;  //직접 지시한 적
		private int heroTargetId = -1; //주인공이 마지막으로 공격한 적

		private static Char byId( int id ){
			Actor a = id < 0 ? null : Actor.findById(id);
			return a instanceof Char ? (Char) a : null;
		}

		private static boolean valid( Char ch ){
			return ch != null && ch.isAlive() && Actor.chars().contains(ch)
					&& ch.alignment == Alignment.ENEMY && !isProp(ch);
		}

		void heroAttacked( Char enemy ){
			if (!valid(enemy)) return;
			heroTargetId = enemy.id();
			//지시한 자리를 지키는 중이 아니고, 지시한 적이 따로 없으면 그 적을 노린다
			if (!movingToDefendPos && defendingPos == -1 && !valid(byId(orderedId))){
				aggro(enemy);
				target = enemy.pos;
			}
		}

		@Override
		public void targetChar( Char ch ){
			if (!isProp(ch)) orderedId = ch.id();
			super.targetChar(ch);
		}

		@Override
		public void followHero(){
			orderedId = heroTargetId = -1;
			super.followHero();
		}

		@Override
		public void defendPos( int cell ){
			orderedId = -1;
			super.defendPos(cell);
		}

		@Override
		protected Char chooseEnemy() {
			Char ordered = byId(orderedId);
			if (valid(ordered)) return ordered;
			orderedId = -1;
			Char heroTarget = byId(heroTargetId);
			if (valid(heroTarget) && (fieldOfView == null || fieldOfView[heroTarget.pos] || Dungeon.level.heroFOV[heroTarget.pos])) return heroTarget;
			heroTargetId = -1;
			return null;
		}

		@Override
		public boolean canAttack( Char enemy ){
			if (enemy == null || isProp(enemy)) return false;
			if (super.canAttack(enemy)) return true;
			//멀리서 쪼기: 가로막히지 않은 곳
			return Dungeon.level.distance(pos, enemy.pos) <= MazeTower.PET_HAWK_RANGE
					&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
		}

		@Override
		public boolean sharesVision() {
			return true;
		}

		@Override
		public int attackSkill( Char target ) {
			return MazeTower.PET_HAWK_ACC;
		}

		@Override
		public int damageRoll() {
			int dmg = Random.NormalIntRange(MazeTower.PET_HAWK_DMG_MIN, MazeTower.PET_HAWK_DMG_MAX);
			return Math.round(dmg * (1f + MazeTower.PET_BEAK_DMG[PetTalents.level(Talent.PET_BEAK)])); //날카로운 부리
		}

		@Override
		public int attackProc( Char enemy, int damage ) {
			damage = super.attackProc(enemy, damage);
			//성가신 날갯짓: 쫀 적의 명중이 잠깐 떨어진다 (0레벨은 없음, 면역이면 걸리지 않음)
			int lv = PetTalents.level(Talent.PET_FLAP);
			if (lv > 0 && enemy.isAlive()){
				Flapped f = Buff.prolong(enemy, Flapped.class, MazeTower.PET_FLAP_TURNS[lv]);
				if (f != null) f.reduce = Math.max(f.reduce, MazeTower.PET_FLAP_ACC[lv]);
			}
			return damage;
		}

		@Override
		public void onSummoned() {
			//꽃잎이 모여 매가 된다 (fx_hunter_32)
			if (sprite != null) MazeFx.onChar(MazeFx.Fx.H_HAWK_IN, sprite);
		}

		@Override
		protected boolean act() {
			viewDistance = MazeTower.PET_HAWK_VIEW + MazeTower.PET_EYE_VIEW[PetTalents.level(Talent.PET_HAWK_EYE)]; //매의 눈
			//노릴 적이 사라지면 헤매지 않고 바로 주인공 곁으로 (원작은 "?"를 띄우고 아무 데로나 간다)
			if (state == HUNTING && chooseEnemy() == null){
				enemy = null;
				state = WANDERING;
			}
			boolean result = super.act();
			Dungeon.level.updateFieldOfView(this, fieldOfView);
			GameScene.updateFog(pos, viewDistance + (int)Math.ceil(speed()));
			return result;
		}

		private static final String ORDERED = "ordered";
		private static final String HERO_TARGET = "hero_target";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(ORDERED, orderedId);
			bundle.put(HERO_TARGET, heroTargetId);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			orderedId = bundle.contains(ORDERED) ? bundle.getInt(ORDERED) : -1;
			heroTargetId = bundle.contains(HERO_TARGET) ? bundle.getInt(HERO_TARGET) : -1;
		}
	}

	//성가신 날갯짓: 명중이 떨어진 상태 (원작 눈멂 아이콘을 푸른빛으로)
	public static class Flapped extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public float reduce = 0f;

		@Override
		public int icon() {
			return BuffIndicator.BLINDNESS;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.4f, 0.7f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (MazeTower.PET_FLAP_TURNS[4] - visualcooldown()) / MazeTower.PET_FLAP_TURNS[4]);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round(reduce * 100), dispTurns());
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("reduce", reduce);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			reduce = bundle.getFloat("reduce");
		}
	}

	//새 정령 매 그림 (푸른빛, fx_hunter_31 날기 4프레임 반복). 쪼기 동작 없이, 적 쪽으로 짧게 날아들었다가 돌아온다(그 순간 피해).
	//물러날 때 꽃잎으로 흩어진다(fx_hunter_33), 몸 뒤에 은은한 빛(fx_hunter_34)
	public static class HawkSprite extends MobSprite {

		private Image glow;

		public HawkSprite() {
			super();
			texture("sprites/maze_fx_hunter_31.png");
			TextureFilm f = new TextureFilm(texture, 16, 16);
			idle = new Animation(10, true);
			idle.frames(f, 0, 1, 2, 3);
			run = idle.clone();
			attack = new Animation(12, false);
			attack.frames(f, 0, 1, 2, 3);
			die = new Animation(20, false);
			die.frames(f, 0);
			play(idle);
		}

		//멀리서 쪼기: 적 쪽으로 PET_HAWK_DIVE만큼 날아들어 피해를 주고 제자리로 돌아온다
		@Override
		public synchronized void attack( int cell, Callback callback ) {
			if (ch == null || parent == null){
				super.attack(cell, callback);
				return;
			}
			turnTo(ch.pos, cell);
			final PointF home = worldToCamera(ch.pos);
			PointF far = worldToCamera(cell);
			final PointF dive = new PointF(home.x + (far.x - home.x) * MazeTower.PET_HAWK_DIVE,
					home.y + (far.y - home.y) * MazeTower.PET_HAWK_DIVE);
			parent.add(new PosTweener(this, dive, MazeTower.PET_HAWK_DIVE_TIME){
				@Override
				protected void onComplete() {
					super.onComplete();
					if (callback != null) callback.call();
					else if (ch != null) ch.onAttackComplete();
					if (parent != null && ch != null && ch.isAlive()){
						parent.add(new PosTweener(HawkSprite.this, worldToCamera(ch.pos), MazeTower.PET_HAWK_DIVE_TIME));
					}
				}
			});
		}

		@Override
		public int blood() {
			return 0xFF00FFFF;
		}

		@Override
		public void update() {
			super.update();
			if (parent == null) return;
			if (glow == null){
				glow = new Image("sprites/maze_fx_hunter_34.png");
				parent.addToBack(glow);
			}
			PointF c = center();
			glow.x = c.x - 16;
			glow.y = c.y - 16;
			glow.visible = visible && (ch == null || ch.isAlive());
		}

		@Override
		public void kill() {
			if (glow != null){ glow.killAndErase(); glow = null; }
			super.kill();
		}

		@Override
		public void die() {
			MazeFx.onChar(MazeFx.Fx.H_HAWK_OUT, this);
			if (glow != null){ glow.killAndErase(); glow = null; }
			super.die();
			alpha(0);
		}
	}
}
