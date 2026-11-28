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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//Maze Tower (T4, 지시서 claude_code_tasks_1126b_pets): 펫 이끼 곰. 역할: 수호.
//- 체력이 높고 주인공과 같은 빠르기로 걷는다. 옆 칸의 적을 때린다(스스로 싸움).
//- 포효: 싸우는 동안 PET_BEAR_ROAR_EVERY턴마다, 곰의 2칸 안의 적이 정해진 턴 동안 곰을 먼저 노린다(보스·고정 몹은 끌리지 않음).
//- 대신 받기: 주인공이 곰의 2칸 안에 있으면 주인공이 받을 피해의 일부를 곰이 대신 받는다(곰의 방어 적용).
//  주인공의 피는 줄었으므로 '피가 줄지 않은 공격'이 아니다. 즉사 피해(최대 체력 이상)와 지속 피해(화상·독·출혈·가스 등)는 대신 받지 않는다.
//- 강화: 두꺼운 가죽(체력·방어), 대신 받기(비율), 포효(시선을 끄는 턴)
//그림은 임시로 원작 놀 병정(sprites/brute.png)을 쓴다.
public class PetBear extends PetAbility {

	@Override
	protected PetAlly create() {
		return new Bear();
	}

	@Override
	public int icon() {
		return HeroIcon.ENDURE; //임시
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{ Talent.PET_HIDE, Talent.PET_SHARE, Talent.PET_ROAR, Talent.HEROIC_ENERGY };
	}

	private static Bear bear(){
		PetAlly p = current();
		return p instanceof Bear ? (Bear) p : null;
	}

	//대신 받는 비율 = 기본 + 강화
	public static float shareRatio(){
		return MazeTower.PET_BEAR_SHARE + MazeTower.PET_SHARE_RATIO[PetTalents.level(Talent.PET_SHARE)];
	}

	//대신 받기 (Hero.damage): 주인공이 받을 피해 중 곰이 가져가고 남은 값을 돌려준다
	public static int shareDamage( Hero hero, int dmg, Object src ){
		Bear b = bear();
		if (b == null || dmg <= 0) return dmg;
		if (src instanceof Buff || src instanceof Blob) return dmg; //지속 피해
		if (dmg >= hero.HT) return dmg;                             //즉사 피해
		if (Dungeon.level.distance(hero.pos, b.pos) > MazeTower.PET_BEAR_SHARE_RANGE) return dmg;
		int part = Math.round(dmg * shareRatio());
		if (part <= 0) return dmg;
		b.damage(Math.max(0, part - b.drRoll()), src);
		return dmg - part;
	}

	//포효에 끌린 적이 노릴 곰 (Mob.chooseEnemy)
	public static Char tauntSource( Mob mob ){
		Taunted t = mob.buff(Taunted.class);
		if (t == null) return null;
		Actor a = Actor.findById(t.object);
		if (a instanceof Bear && ((Bear) a).isAlive()) return (Bear) a;
		t.detach();
		return null;
	}

	public static class Bear extends PetAlly {

		{
			spriteClass = BearSprite.class;

			HP = HT = MazeTower.PET_BEAR_HP;
			defenseSkill = MazeTower.PET_BEAR_DEF;
			baseSpeed = 1f;
		}

		private int roarCD = 0; //다음 포효까지 (싸우는 동안만 줄어듦)

		//두꺼운 가죽: 체력·방어 배율
		private static float hide(){
			return 1f + MazeTower.PET_HIDE_BONUS[PetTalents.level(Talent.PET_HIDE)];
		}

		//강화를 올리면 바로 반영: 늘어난 최대 체력만큼 지금 체력도 는다
		private void updateHT(){
			int ht = Math.round(MazeTower.PET_BEAR_HP * hide());
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
			return MazeTower.PET_BEAR_ACC;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(MazeTower.PET_BEAR_DMG_MIN, MazeTower.PET_BEAR_DMG_MAX);
		}

		@Override
		public int drRoll() {
			float h = hide();
			return super.drRoll() + Random.NormalIntRange(Math.round(MazeTower.PET_BEAR_DR_MIN * h), Math.round(MazeTower.PET_BEAR_DR_MAX * h));
		}

		@Override
		protected boolean act() {
			updateHT();
			//포효: 싸우는 동안(노리는 적이 있을 때)만 간격이 줄고, 둘레 2칸에 끌 적이 있으면 그 턴을 써서 포효한다
			if (state == HUNTING && enemy != null && enemy.isAlive()){
				if (roarCD > 0) roarCD--;
				if (roarCD <= 0 && roar()){
					roarCD = MazeTower.PET_BEAR_ROAR_EVERY;
					spend(TICK);
					return true;
				}
			}
			return super.act();
		}

		private boolean roar(){
			boolean any = false;
			int turns = MazeTower.PET_BEAR_ROAR_TURNS + MazeTower.PET_ROAR_TURNS[PetTalents.level(Talent.PET_ROAR)];
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
				if (m.alignment != Alignment.ENEMY || !m.isAlive()) continue;
				if (Dungeon.level.distance(pos, m.pos) > MazeTower.PET_BEAR_ROAR_RANGE) continue;
				if (m.properties().contains(Property.BOSS) || m.properties().contains(Property.IMMOVABLE) || isProp(m)) continue;
				Taunted t = Buff.prolong(m, Taunted.class, turns);
				if (t == null) continue; //면역
				t.object = id();
				m.aggro(this);
				any = true;
			}
			if (any && sprite != null){
				sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.2f, 2);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.6f, 0.8f);
			}
			return any;
		}

		private static final String ROAR_CD = "roar_cd";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(ROAR_CD, roarCD);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			roarCD = bundle.getInt(ROAR_CD);
		}

		@Override
		public String description() {
			return super.description() + "\n\n" + Messages.get(PetBear.class, "desc_share",
					Math.round(shareRatio() * 100));
		}
	}

	//포효에 끌림: 이 동안 곰을 먼저 노린다. 끝나면 다시 가까운 대상(주인공 포함)을 고른다.
	public static class Taunted extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		public int object = -1;

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.5f, 0.8f, 0.4f);
		}

		@Override
		public float iconFadePercent() {
			float max = MazeTower.PET_BEAR_ROAR_TURNS + MazeTower.PET_ROAR_TURNS[4];
			return Math.max(0, (max - visualcooldown()) / max);
		}

		@Override
		public void detach() {
			super.detach();
			if (target instanceof Mob && target.isAlive()) ((Mob) target).aggro(null); //다음 차례에 가까운 대상을 다시 고른다
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("object", object);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt("object");
		}
	}

	//(사용자 확인) 이끼 곰 그림 sprites/maze_pet_bear.png (20×18 × 12): 짙은 잿빛 곰, 어깨 혹과 둥근 엉덩이, 거친 이끼 덩어리와 옆구리로 늘어진 이끼 가닥, 낮은 머리·어두운 눈, 짧은 네 다리.
	//칸 0·1 가만히(숨), 2~5 걷기(다리마다 뒤로 디딤 → 듦 → 앞으로 디딤 → 가운데, 대각선 두 쌍이 반 박자 엇갈림), 6~8 공격(일어섰다 덮침), 9~11 쓰러짐. 원본 docs/sprites/pets/make_bear2.ps1.
	public static class BearSprite extends MobSprite {
		public BearSprite() {
			super();
			texture("sprites/maze_pet_bear.png");
			TextureFilm frames = new TextureFilm(texture, 20, 18);
			idle = new Animation(2, true);
			idle.frames(frames, 0, 0, 0, 1, 0, 0, 1, 1);
			run = new Animation(8, true);
			run.frames(frames, 2, 3, 4, 5);
			attack = new Animation(10, false);
			attack.frames(frames, 6, 7, 8);
			die = new Animation(8, false);
			die.frames(frames, 9, 10, 11);
			play(idle);
		}
	}
}
