/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomJudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ElementalSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.HashSet;

//Maze Tower (T5·T7, 지시서 claude_code_tasks_1126b_pets): 히든 펫 길든 주문. 여제를 잡은 판에서 현자에게 결실의 핵을 건네면 받는다(MazeSage.giveCore).
//역할: 원소. 결실의 핵 선택지(MazeTower.fruitPaths)에는 나오지 않는다. 그림은 16~19층 풀려난 주문(maze_elemental.png), 원소에 따라 불·냉기·전격 줄.
//- 주인공을 따라다니고 싸우지 않는다. 적은 길든 주문을 노리지 않는다(PetAlly.untargetable). 범위 피해에는 맞는다.
//- 원소 고르기: 부를 때마다 불·냉기·전격 중 하나를 고르는 창이 뜬다.
//- 원소 묻히기: 주문이 나와 있는 동안, 주인공의 스킬 타격(검·단검·활·반지 스킬, 강화된 평타, 지팡이 스킬, 형태 특성의 덧타격)이
//  적에게 맞으면 그 적에게 주문의 원소를 묻힌다(불 = 화상, 냉기 = 냉기, 전격 = 정전기). 스킬 한 번에 적 하나당 한 번. 거리 제한 없음.
//  원소 반응은 지금 있는 코드가 처리한다(원작 상태이상을 붙이므로). 원소·상태이상에 면역인 적에게는 걸리지 않는다.
//- 원소 바꾸기: 주문이 나와 있을 때 능력 단추의 메뉴에서. 한 턴을 쓰고, 바꾼 뒤 PET_SPELL_SHIFT_CD − 빠른 변주 턴 동안 다시 바꿀 수 없다(메뉴 단추와 상태 아이콘에 남은 턴).
//- 강화: 짙은 원소(화상 피해·냉기·정전기 시간), 빠른 변주(바꾸기 대기), 스며드는 손길(평타로도 가끔 묻힘)
public class PetSpell extends PetAbility {

	public static final int FIRE = 0, FROST = 1, SHOCK = 2;

	//부를 때 고른 원소 (create가 쓴다)
	private static int pendingElement = FIRE;

	@Override
	protected PetAlly create() {
		Spell s = new Spell();
		s.element = pendingElement;
		return s;
	}

	@Override
	public int icon() {
		return HeroIcon.ELEMENTAL_BLAST; //임시
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{ Talent.PET_ESSENCE, Talent.PET_SHIFT, Talent.PET_TOUCH, Talent.HEROIC_ENERGY };
	}

	private static Spell spell(){
		PetAlly p = current();
		return p instanceof Spell ? (Spell) p : null;
	}

	//능력 단추(퀵슬롯): 없으면 원소를 골라 부르고, 나와 있으면 다른 펫처럼 지시(칸·적 찍기)
	@Override
	public void use( final ClassArmor armor, final Hero hero ) {
		if (spell() != null){
			super.use(armor, hero);
			return;
		}
		chooseElement(Messages.get(PetSpell.class, "choose_summon"), new ElementChoice() {
			@Override
			public void chosen( int element ) {
				pendingElement = element;
				activate(armor, hero, hero.pos);
			}
		});
	}

	//(사용자) 직업 방어구의 행동 "원소 바꾸기" (ClassArmor.AC_PET_SHIFT): 한 턴, 바꾼 뒤 PET_SPELL_SHIFT_CD − 빠른 변주 턴 대기
	public static void shiftElement( final Hero hero ){
		if (!(hero.armorAbility instanceof PetSpell)) return;
		Spell s = spell();
		if (s == null){
			GLog.w(Messages.get(PetSpell.class, "shift_none"));
			return;
		}
		ShiftCooldown cd = hero.buff(ShiftCooldown.class);
		if (cd != null){
			GLog.w(Messages.get(PetSpell.class, "shift_wait", (int)Math.ceil(cd.cooldown())));
			return;
		}
		((PetSpell) hero.armorAbility).chooseElement(
				Messages.get(PetSpell.class, "choose_shift", Messages.get(PetSpell.class, "el_" + s.element)), new ElementChoice() {
			@Override
			public void chosen( int element ) {
				Spell sp = spell();
				if (sp == null || element == sp.element) return;
				sp.setElement(element);
				Buff.affect(hero, ShiftCooldown.class, MazeTower.PET_SPELL_SHIFT_CD - MazeTower.PET_SHIFT_CUT[PetTalents.level(Talent.PET_SHIFT)]);
				GLog.i(Messages.get(PetSpell.class, "shifted", Messages.get(PetSpell.class, "el_" + element)));
				hero.spendAndNext(Actor.TICK);
			}
		});
	}

	//직업 방어구 행동 이름: 대기 중이면 남은 턴을 붙인다
	public static String shiftActionName( Hero hero ){
		ShiftCooldown cd = hero.buff(ShiftCooldown.class);
		return cd == null ? Messages.get(PetSpell.class, "ac_shift")
				: Messages.get(PetSpell.class, "ac_shift_wait", (int)Math.ceil(cd.cooldown()));
	}

	private interface ElementChoice {
		void chosen( int element );
	}

	private void chooseElement( String body, final ElementChoice cb ){
		GameScene.show(new WndOptions(new HeroIcon(this), Messages.titleCase(name()), body,
				Messages.get(PetSpell.class, "el_" + FIRE), Messages.get(PetSpell.class, "el_" + FROST),
				Messages.get(PetSpell.class, "el_" + SHOCK), Messages.get(PetSpell.class, "menu_cancel")){
			@Override
			protected void onSelect( int index ) {
				if (index >= FIRE && index <= SHOCK) cb.chosen(index);
			}
		});
	}

	// ===== 원소 묻히기 =====

	//이번 스킬에서 이미 원소를 묻힌 적 (스킬 한 번에 적 하나당 한 번)
	private static final HashSet<Integer> marked = new HashSet<>();

	//스킬을 시작할 때 (BloomEffects.beginSkill)
	public static void onSkillStart(){
		marked.clear();
	}

	//주인공의 적중 (BloomEffects.afterHeroAttackProc: 평타·스킬·덧타격·던지기 / onStaffSkillDamage: 지팡이 스킬)
	public static void onHeroHit( Hero hero, Char enemy ){
		Spell s = spell();
		if (s == null || enemy == null || enemy == hero || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY) return;
		BloomJudge.AttackKind kind = BloomJudge.attackKind();
		if (kind == BloomJudge.AttackKind.SKILL){
			if (!marked.add(enemy.id())) return;
		} else if (kind == BloomJudge.AttackKind.NORMAL){
			//스며드는 손길: 평타로 맞힌 적에게도 가끔 (0레벨은 없음)
			if (Random.Float() >= MazeTower.PET_TOUCH_CHANCE[PetTalents.level(Talent.PET_TOUCH)]) return;
		} else {
			return;
		}
		apply(enemy, s.element);
	}

	//원소를 묻힌다 (원작 상태이상을 붙이므로 원소 반응도 그대로 일어난다. 면역이면 붙지 않는다)
	private static void apply( Char ch, int element ){
		int lv = PetTalents.level(Talent.PET_ESSENCE);
		switch (element){
			case FIRE:
				Burning b = Buff.affect(ch, Burning.class);
				if (b == null) return;
				b.mazeDmgMulti = Math.max(b.mazeDmgMulti, 1f + MazeTower.PET_ESSENCE_BURN[lv]); //짙은 원소: 화상 피해
				b.reignite(ch);
				break;
			case FROST:
				Buff.prolong(ch, Chill.class, MazeTower.PET_SPELL_CHILL + MazeTower.PET_ESSENCE_TURNS[lv]);
				break;
			case SHOCK:
				Buff.prolong(ch, StaticCharge.class, MazeTower.STATIC_TURNS + MazeTower.PET_ESSENCE_TURNS[lv]);
				break;
		}
	}

	//(사용자) 불의 벽(MagicalFireRoom.EternalFire) 칸이나 그 옆 칸(대각선 포함)에 있는, 그 원소의 길든 주문
	//냉기 주문이 오면 벽 전체가 사라지고 주문은 증기 피해, 전격 주문이 오면 벽 전체가 사라지고 그 자리에서 폭발 반응
	public static Spell wallSpellNear( int cell, int element ){
		Spell s = spell();
		if (s == null || s.element != element || !s.isAlive()) return null;
		return Dungeon.level.distance(cell, s.pos) <= 1 ? s : null;
	}

	//피해 출처의 원소 (불·냉기·전격, 아니면 -1). 원소 반응 피해는 원소가 아니다.
	public static int elementOf( Object src ){
		if (src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.ReactionDamage) return -1;
		if (src instanceof Burning || src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Inferno
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast) return FIRE;
		if (src instanceof Chill || src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blizzard
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost) return FROST;
		if (src instanceof StaticCharge || com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions.isElectric(src)) return SHOCK;
		return -1;
	}

	// ===== 길든 주문 =====

	public static class Spell extends PetAlly {

		{
			spriteClass = SpellSprite.class;

			HP = HT = MazeTower.PET_SPELL_HP;
			defenseSkill = MazeTower.PET_SPELL_DEF;
			baseSpeed = 1f;
			flying = true;

			//(사용자) 떠 있어서 땅의 장판은 받지 않지만, 키가 큰 불의 벽(마법의 불 방)은 받는다
			immunities.addAll(new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity().immunities());
			immunities.remove(com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.MagicalFireRoom.EternalFire.class);
		}

		//(사용자) 명령이 없으면 주인공 2칸 안에서 떠다닌다 (PetAlly.Roam)
		@Override
		protected int leash(){
			return MazeTower.PET_SPELL_LEASH;
		}

		public int element = FIRE;
		private int orderedId = -1;   //직접 지시한 적
		private int skillCD = 0;      //다음 원소 탄(스킬)까지
		boolean usingSkill = false;   //지금 쏘는 탄이 스킬인지 (그림이 본다)

		void setElement( int el ){
			element = el;
			if (sprite instanceof SpellSprite) ((SpellSprite) sprite).setElement(el);
		}

		// ===== (사용자) 싸운다: 약한 탄(평타)을 쏘다가 PET_SPELL_SKILL_EVERY턴마다 원소를 묻히는 강한 탄(스킬) =====

		@Override
		public int attackSkill( Char target ) {
			return MazeTower.PET_SPELL_ACC;
		}

		@Override
		public int damageRoll() {
			return Random.NormalIntRange(MazeTower.PET_SPELL_DMG_MIN, MazeTower.PET_SPELL_DMG_MAX);
		}

		//탄이 닿는가: 평타 PET_SPELL_RANGE칸·스킬(준비됐을 때) PET_SPELL_SKILL_RANGE칸 안, 마법 탄 길이 가로막히지 않음
		private boolean boltReach( Char e ){
			int range = skillCD <= 0 ? MazeTower.PET_SPELL_SKILL_RANGE : MazeTower.PET_SPELL_RANGE;
			return e != null && Dungeon.level.distance(pos, e.pos) <= range
					&& new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos, e.pos,
						com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT).collisionPos == e.pos;
		}

		@Override
		public boolean canAttack( Char enemy ) {
			return enemy != null && !isProp(enemy) && boltReach(enemy);
		}

		private static Char byId( int id ){
			Actor a = id < 0 ? null : Actor.findById(id);
			return a instanceof Char ? (Char) a : null;
		}

		private static boolean valid( Char ch ){
			return ch != null && ch.isAlive() && Actor.chars().contains(ch)
					&& ch.alignment == Alignment.ENEMY && !isProp(ch);
		}

		//지시한 적이 있으면 그 적(쫓아감), 없으면 지금 자리에서 탄이 닿는 깨어 있는 적 중 주인공에게 가장 가까운 적 (쫓아가지 않음)
		@Override
		protected Char chooseEnemy() {
			Char ordered = byId(orderedId);
			if (valid(ordered)) return ordered;
			orderedId = -1;
			Char best = null;
			int bestDist = Integer.MAX_VALUE;
			for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs){
				if (!valid(m) || m.state == m.SLEEPING || m.state == m.PASSIVE || m.invisible > 0) continue;
				if (fieldOfView != null && !fieldOfView[m.pos]) continue;
				if (!boltReach(m)) continue;
				int d = Dungeon.hero == null ? 0 : Dungeon.level.distance(m.pos, Dungeon.hero.pos);
				if (d < bestDist){ best = m; bestDist = d; }
			}
			return best;
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

		@Override
		protected boolean act() {
			if (skillCD > 0) skillCD--;
			//노릴 적이 사라지면 헤매지 않고 바로 주인공 곁으로
			if (state == HUNTING && chooseEnemy() == null){
				enemy = null;
				state = WANDERING;
			}
			return super.act();
		}

		@Override
		protected boolean doAttack( Char enemy ) {
			usingSkill = skillCD <= 0;
			if (sprite != null && (sprite.visible || enemy.sprite.visible)){
				sprite.zap(enemy.pos);
				return false;
			}
			zap();
			return true;
		}

		public void onZapComplete(){
			zap();
			next();
		}

		private void zap(){
			spend(attackDelay());
			Char e = enemy;
			if (e == null || !e.isAlive()){ usingSkill = false; return; }
			if (usingSkill){
				boolean hit = attack(e, MazeTower.PET_SPELL_SKILL_DMG, 0, 1);
				if (hit && e.isAlive()) apply(e, element);
				skillCD = MazeTower.PET_SPELL_SKILL_EVERY;
			} else {
				attack(e);
			}
			usingSkill = false;
		}


		//(사용자) 자기 원소의 피해에는 면역이지만, 상태이상은 붙어 원소 반응은 일어난다(ElementalReactions.reacts가 길든 주문을 넣음).
		//반응 피해(증기·해빙·폭발)는 자기 원소가 아니므로 받는다. 다른 원소의 피해는 그대로 받는다.
		@Override
		public void damage( int dmg, Object src ) {
			if (dmg > 0 && elementOf(src) == element){
				return;
			}
			super.damage(dmg, src);
		}

		//적은 길든 주문을 노리지 않는다
		@Override
		public boolean ignoredByEnemies() {
			return true;
		}

		@Override
		public String name() {
			return Messages.get(PetSpell.class, "spell_name", Messages.get(PetSpell.class, "el_" + element));
		}

		@Override
		public String description() {
			return super.description() + "\n\n" + Messages.get(PetSpell.class, "desc_element", Messages.get(PetSpell.class, "el_" + element));
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put("element", element);
			bundle.put("ordered", orderedId);
			bundle.put("skill_cd", skillCD);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			element = bundle.getInt("element");
			orderedId = bundle.contains("ordered") ? bundle.getInt("ordered") : -1;
			skillCD = bundle.getInt("skill_cd");
		}
	}

	//원소 바꾸기 대기 (주인공에게, 남은 턴)
	public static class ShiftCooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight(0.9f, 0.6f, 1f);
		}

		@Override
		public float iconFadePercent() {
			float max = MazeTower.PET_SPELL_SHIFT_CD;
			return Math.max(0, (max - visualcooldown()) / max);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	//(사용자 확인) 길든 주문 그림 sprites/maze_pet_spell.png (12×14 × 38칸 × 3줄: 불·냉기·전격). 풀려난 주문 그림을 그대로 쓰고
	//발밑에서 도는 고리만 아주 은은한 무지개빛. 칸 0~23 가만히(고리 빛깔이 2.4초에 한 바퀴), 24~26 걷기, 27~29 공격, 30~37 쓰러짐.
	//원본 docs/sprites/pets/make_pets_sheets.ps1. 원소가 떠 있는 자리에서만 작고 차분한 입자.
	public static class SpellSprite extends ElementalSprite {

		private static final int ROW = 38; //한 줄(원소 하나)의 칸 수
		private boolean petTex = false;
		private Emitter quiet;
		private int shown = -1;

		@Override
		protected int texOffset() {
			return 0;
		}

		@Override
		protected Emitter createEmitter() {
			quiet = emitter();
			shown = -1;
			refresh();
			return quiet;
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			refresh();
		}

		void setElement( int el ){
			refresh();
		}

		private void refresh(){
			int el = ch instanceof Spell ? ((Spell) ch).element : FIRE;
			if (el == shown) return;
			shown = el;
			if (!petTex){
				texture("sprites/maze_pet_spell.png");
				petTex = true;
			}
			int c = el * ROW;
			TextureFilm frames = new TextureFilm(texture, 12, 14);
			boolean idling = curAnim == null || curAnim == idle;
			idle = new Animation(10, true);
			Object[] idleFrames = new Object[24];
			for (int i = 0; i < 24; i++) idleFrames[i] = c + i;
			idle.frames(frames, idleFrames);
			run = new Animation(12, true);
			run.frames(frames, c + 24, c + 25, c + 26);
			attack = new Animation(15, false);
			attack.frames(frames, c + 27, c + 28, c + 29);
			zap = attack.clone();
			die = new Animation(15, false);
			die.frames(frames, c + 30, c + 31, c + 32, c + 33, c + 34, c + 35, c + 36, c + 37);
			if (idling) play(idle);
			if (quiet != null){
				quiet.pos(this, 3, 2, 6, 7);
				Emitter.Factory f = el == FIRE ? QUIET_FLAME : el == FROST ? QUIET_MAGIC : QUIET_SPARK;
				quiet.pour(f, 0.06f * MazeTower.L3_PARTICLE_SPARSE);
			}
		}

		//(사용자) 탄 쏘기: 평타는 원소 빛깔의 작은 탄, 스킬은 같은 탄 + 닿을 때 원소 입자 (전격 스킬은 원작 전격 주문처럼 빛줄기)
		@Override
		public void zap( int cell ) {
			super.zap(cell, null);
			final Spell s = ch instanceof Spell ? (Spell) ch : null;
			final boolean skill = s != null && s.usingSkill;
			int el = s != null ? s.element : FIRE;
			if (el == SHOCK && skill){
				parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Beam.LightRay(center(),
						com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(cell)));
				com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.RAY);
				if (s != null) s.onZapComplete();
				return;
			}
			int type = el == FIRE ? com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.FIRE
					: el == FROST ? com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.FROST
					: com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.LIGHT_MISSILE;
			com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.boltFromChar(parent, type, this, cell, new com.watabou.utils.Callback() {
				@Override
				public void call() {
					if (skill){
						Char t = Actor.findChar(cell);
						if (t != null && t.sprite != null && t.sprite.visible){
							if (el == FIRE) t.sprite.centerEmitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle.FACTORY, 6);
							else t.sprite.centerEmitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.MagicParticle.FACTORY, 6);
						}
					}
					if (s != null) s.onZapComplete();
				}
			});
			com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.ZAP);
		}

		@Override
		public int blood() {
			return 0xFFAACCFF;
		}
	}
}
