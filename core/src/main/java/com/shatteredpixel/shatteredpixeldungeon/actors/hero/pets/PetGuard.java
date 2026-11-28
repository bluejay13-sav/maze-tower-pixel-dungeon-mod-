/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MonkSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//Maze Tower (T5·T6, 지시서 claude_code_tasks_1126b_pets): 히든 펫 마지막 근위. 망령 왕을 잡은 판에서 현자에게 결실의 핵을 건네면 받는다(MazeSage.giveCore).
//역할: 맞받아침. 결실의 핵 선택지(MazeTower.fruitPaths)에는 나오지 않는다. 그림은 16~19층 왕실 근위(maze_monk.png 줄 0).
//- 주인공의 옆 칸을 지키며 따라다닌다. 지시가 없으면 주인공에게서 한 칸 넘게 떨어지지 않으려 하고, 자기 옆 칸의 적만 벤다(쫓아가지 않음).
//  적을 직접 찍어 지시하면 그 적은 쫓아가 벤다.
//- 집중 자세: 싸우는 동안 PET_GUARD_STANCE_EVERY − 곧은 자세 턴마다 자세를 잡는다(그림으로 구분). PET_GUARD_STANCE_TURNS턴 이어지거나 공격 하나를 흘리면 풀린다.
//- 흘리기: 자세 중에 근위가 주인공 옆 칸에 있고, 옆 칸의 적이 주인공을 근접 공격(Char.attack)하면 그 공격 하나를 대신 흘린다.
//  주인공은 피해를 받지 않고 '피가 줄지 않은 공격'(회피와 같은 결과, 받아치기 등이 이어짐). 근위도 피해를 받지 않는다.
//  원거리 공격·마법·범위 기술·보스의 예고 기술(Char.attack을 쓰지 않는 것)은 흘리지 않는다.
//- 반격: 흘린 직후 영웅 옆 칸의 그 적을 한 번 벤다(근위 평타 × (PET_GUARD_REQUITE + 되갚는 칼), 시간을 쓰지 않음).
//- 강화: 곧은 자세(자세 간격), 되갚는 칼(반격 배율), 굳은 맹세(체력·방어)
public class PetGuard extends PetAbility {

	@Override
	protected PetAlly create() {
		return new Guard();
	}

	@Override
	public int icon() {
		return HeroIcon.SHOCKWAVE; //임시
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{ Talent.PET_STANCE, Talent.PET_REQUITE, Talent.PET_OATH, Talent.HEROIC_ENERGY };
	}

	private static Guard guard(){
		PetAlly p = current();
		return p instanceof Guard ? (Guard) p : null;
	}

	//적이 주인공을 공격하기 직전 (Char.attack → BloomEffects.tryDeflect). true면 그 공격은 흘려진다(빗나감)
	public static boolean tryParry( Hero hero, Char attacker ){
		Guard g = guard();
		if (g == null || !g.inStance()) return false;
		if (attacker == null || attacker.alignment != Char.Alignment.ENEMY) return false;
		if (!Dungeon.level.adjacent(attacker.pos, hero.pos)) return false; //근접 공격만
		if (!Dungeon.level.adjacent(g.pos, hero.pos)) return false;        //근위가 주인공 옆 칸에 있을 때
		if (g.paralysed > 0 || g.buff(Frost.class) != null) return false;
		g.stanceLeft = 0;
		g.counterId = attacker.id();
		if (g.sprite != null && g.sprite.visible){
			g.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(PetGuard.class, "parry"));
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, Random.Float(0.96f, 1.05f));
		}
		return true;
	}

	//적의 공격 처리가 끝난 뒤 (BloomEffects.onEnemyAttackDone): 흘렸으면 그 적을 벤다
	public static void afterEnemyAttack( Char attacker ){
		Guard g = guard();
		if (g == null || g.counterId != attacker.id()) return;
		g.counterId = -1;
		g.counter(attacker);
	}

	public static class Guard extends PetAlly {

		{
			spriteClass = GuardSprite.class;

			HP = HT = MazeTower.PET_GUARD_HP;
			defenseSkill = MazeTower.PET_GUARD_DEF;
			baseSpeed = 1f;
		}

		private int orderedId = -1;   //직접 지시한 적
		private int stanceCD = 0;     //다음 집중 자세까지 (싸우는 동안만 줄어듦)
		private int stanceLeft = 0;   //집중 자세가 남은 근위 턴
		private int counterId = -1;   //흘린 뒤 벨 적

		public boolean inStance(){
			return stanceLeft > 0;
		}

		//굳은 맹세: 체력·방어 배율
		private static float oath(){
			return 1f + MazeTower.PET_OATH_BONUS[PetTalents.level(Talent.PET_OATH)];
		}

		private void updateHT(){
			int ht = Math.round(MazeTower.PET_GUARD_HP * oath());
			if (ht > HT) HP += ht - HT;
			HT = ht;
			HP = Math.min(HP, HT);
		}

		@Override
		public void onSummoned() {
			updateHT();
			HP = HT;
		}

		@Override
		public int attackSkill( Char target ) {
			return MazeTower.PET_GUARD_ACC;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(MazeTower.PET_GUARD_DMG_MIN, MazeTower.PET_GUARD_DMG_MAX);
		}

		@Override
		public int drRoll() {
			float o = oath();
			return super.drRoll() + Random.NormalIntRange(Math.round(MazeTower.PET_GUARD_DR_MIN * o), Math.round(MazeTower.PET_GUARD_DR_MAX * o));
		}

		private static Char byId( int id ){
			Actor a = id < 0 ? null : Actor.findById(id);
			return a instanceof Char ? (Char) a : null;
		}

		private static boolean valid( Char ch ){
			return ch != null && ch.isAlive() && Actor.chars().contains(ch)
					&& ch.alignment == Alignment.ENEMY && !isProp(ch);
		}

		@Override
		public void targetChar( Char ch ){
			if (!isProp(ch)) orderedId = ch.id();
			super.targetChar(ch);
		}

		@Override
		public void followHero(){
			orderedId = -1;
			super.followHero();
		}

		@Override
		public void defendPos( int cell ){
			orderedId = -1;
			super.defendPos(cell);
		}

		//지시한 적이 있으면 그 적, 없으면 바로 옆 칸의 적만 (쫓아가지 않고 주인공 곁을 지킨다)
		@Override
		protected Char chooseEnemy() {
			Char ordered = byId(orderedId);
			if (valid(ordered)) return ordered;
			orderedId = -1;
			if (enemy != null && valid(enemy) && Dungeon.level.adjacent(pos, enemy.pos)) return enemy;
			Char best = null;
			for (Mob m : Dungeon.level.mobs){
				if (!valid(m) || !Dungeon.level.adjacent(pos, m.pos)) continue;
				if (m.state == m.SLEEPING || m.state == m.PASSIVE) continue;
				if (best == null || m.HP < best.HP) best = m;
			}
			return best;
		}

		//싸우는 중: 주인공이 보는 곳에 주인공 쪽을 쫓는 적이 있다
		private boolean fighting(){
			for (Mob m : Dungeon.level.mobs){
				if (m.alignment == Alignment.ENEMY && m.isAlive() && m.state == m.HUNTING
						&& Dungeon.level.heroFOV[m.pos] && !isProp(m)) return true;
			}
			return false;
		}

		@Override
		protected boolean act() {
			updateHT();
			if (stanceLeft > 0) stanceLeft--;
			if (fighting()){
				if (stanceCD > 0) stanceCD--;
				if (stanceCD <= 0 && stanceLeft <= 0){
					stanceLeft = MazeTower.PET_GUARD_STANCE_TURNS;
					stanceCD = MazeTower.PET_GUARD_STANCE_EVERY - MazeTower.PET_STANCE_CUT[PetTalents.level(Talent.PET_STANCE)];
				}
			}
			//노릴 적이 사라지면 헤매지 않고 바로 주인공 곁으로
			if (state == HUNTING && chooseEnemy() == null){
				enemy = null;
				state = WANDERING;
			}
			return super.act();
		}

		//흘린 직후 그 적을 한 번 벤다 (상대의 공격 처리가 끝난 바로 다음, 시간을 쓰지 않음). (사용자) 그 적이 영웅 옆 칸(1칸 안)에 있으면 근위와 떨어져 있어도 벤다
		void counter( final Char foe ){
			final Guard self = this;
			Actor.add(new Actor() {
				{ actPriority = VFX_PRIO; }
				@Override
				protected boolean act() {
					Actor.remove(this);
					if (self.isAlive() && foe.isAlive() && self.paralysed == 0 && self.buff(Frost.class) == null
							&& Dungeon.hero != null && Dungeon.level.adjacent(Dungeon.hero.pos, foe.pos) && !isProp(foe)){ //(사용자) 영웅 옆 칸의 적이면 근위 옆이 아니어도
						if (self.sprite instanceof MonkSprite) ((MonkSprite) self.sprite).counter(foe.pos);
						self.attack(foe, MazeTower.PET_GUARD_REQUITE + MazeTower.PET_REQUITE_DMG[PetTalents.level(Talent.PET_REQUITE)], 0, 1);
					}
					return true;
				}
			});
		}

		private static final String ORDERED = "ordered", STANCE_CD = "stance_cd", STANCE_LEFT = "stance_left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(ORDERED, orderedId);
			bundle.put(STANCE_CD, stanceCD);
			bundle.put(STANCE_LEFT, stanceLeft);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			orderedId = bundle.contains(ORDERED) ? bundle.getInt(ORDERED) : -1;
			stanceCD = bundle.getInt(STANCE_CD);
			stanceLeft = bundle.getInt(STANCE_LEFT);
		}

		@Override
		public String description() {
			String s = super.description();
			if (inStance()) s += "\n\n" + Messages.get(PetGuard.class, "desc_stance", stanceLeft);
			return s;
		}
	}

	//(사용자 확인) 왕실 근위 그림을 다시 칠한 펫 그림 sprites/maze_pet_guard.png (18×18 × 15, 칸 배치는 왕실 근위와 같음):
	//남빛 망토, 넋빛(하늘빛) 장식과 얼굴, 투구 위 하늘빛 깃털. 원본 docs/sprites/pets/make_pets_sheets.ps1.
	//집중 자세 중에는 가만히 있는 모습이 자세 그림(검을 얼굴 앞에 곧게)으로 바뀐다.
	public static class GuardSprite extends MonkSprite {
		public GuardSprite() {
			super("sprites/maze_pet_guard.png", 0);
		}

		@Override
		public void update() {
			super.update();
			if (ch instanceof Guard && ch.isAlive()){
				Animation want = ((Guard) ch).inStance() ? stance : baseIdle;
				if (idle != want){
					boolean idling = curAnim == idle;
					idle = want;
					if (idling) play(idle);
				}
			}
		}
	}
}
