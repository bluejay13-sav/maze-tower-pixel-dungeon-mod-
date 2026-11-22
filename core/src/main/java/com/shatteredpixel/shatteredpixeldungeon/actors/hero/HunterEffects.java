/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Bloom2Fx;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfElements;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEvasion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfSharpshooting;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorrosion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.FinisherIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 사냥꾼 3차(만개) 추적자·고리지기의 효과 모음. 원작 코드에는 호출 한 줄씩만 넣었다.
//수치는 MazeTower (T3_PREY_*, T3_SNIPE_*, T3_SNARE_*, T3_RING_*, RK_*). 명세는 CLAUDE.md 23번.
public class HunterEffects {

	// =====================================================================
	// 공통
	// =====================================================================

	static boolean tracker(Hero h){
		return h != null && h.subClass == HeroSubClass.TRACKER;
	}

	static boolean ringkeeper(Hero h){
		return h != null && h.subClass == HeroSubClass.RINGKEEPER;
	}

	static int pts(Hero h, Talent t){
		return h == null ? 0 : Math.min(4, h.pointsInTalent(t));
	}

	//지금 스킬로 공격 중인지 (검·단검·활 스킬, 반지 스킬, 필살기, 지팡이 스킬)
	static boolean isSkill(){
		return SwordSkill.active != null || GladiatorEffects.finisherActive || BloomEffects.staffSkillWand != null;
	}

	// =====================================================================
	// 추적자
	// =====================================================================

	//사냥감 (2차 '표식'과 다른 상태). 머리 위에 표식 그림을 주황빛으로
	public static class Prey extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		private PreyOverhead overhead;

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.55f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / MazeTower.T3_PREY_TURNS[4]);
		}

		@Override
		public void fx(boolean on) {
			if (on){
				if (overhead == null && target.sprite != null && target.sprite.parent != null){
					overhead = new PreyOverhead(target);
					target.sprite.parent.add(overhead);
				}
			} else if (overhead != null){
				overhead.killAndErase();
				overhead = null;
			}
		}
	}

	//사냥감 머리 위 표식: 2차 표식(1개) 그림을 주황빛으로, 2차 표식이 함께 있으면 그 위에
	public static class PreyOverhead extends Bloom2Fx.MarkOverhead {
		private final Char ch;

		public PreyOverhead(Char ch){
			super(ch.sprite);
			this.ch = ch;
			stacks(1);
			hardlight(1f, 0.6f, 0.25f);
		}

		@Override
		public void update() {
			super.update();
			if (ch.buff(BloomBuffs.Mark.class) != null) y -= 6;
		}
	}

	static float preyTurns(Hero h){
		return MazeTower.T3_PREY_TURNS[pts(h, Talent.PREY_MARK)];
	}

	// =====================================================================
	// 고리지기: 반지
	// =====================================================================

	static Ring[] worn(Hero h){
		ArrayList<Ring> r = new ArrayList<>();
		if (h.belongings.ring() != null) r.add(h.belongings.ring());
		KindofMisc m = h.belongings.misc();
		if (m instanceof Ring) r.add((Ring)m);
		return r.toArray(new Ring[0]);
	}

	static boolean wears(Hero h, Class<? extends Ring> c){
		for (Ring r : worn(h)) if (r.getClass() == c) return true;
		return false;
	}

	//반지 고유 능력을 쓸 수 있는지: 고리지기 + 무기를 들지 않음 (들면 패시브·스킬 모두 꺼짐)
	public static boolean ringsOpen(Hero h){
		return ringkeeper(h) && h.belongings.weapon() == null && h.buff(MagicImmune.class) == null;
	}

	static boolean passive(Hero h, Class<? extends Ring> c){
		return ringsOpen(h) && wears(h, c);
	}

	//고리의 맥: 패시브 수치 배율
	static float pulse(Hero h){
		return 1f + MazeTower.T3_RING_PULSE[pts(h, Talent.RING_PULSE)];
	}

	//공명: 두 반지를 낄 때 낮은 쪽 반지 레벨 + (차이 × 비율, 내림). Ring.buffedLvl에서 부른다
	public static int resonanceBonus(Ring ring){
		Hero h = Dungeon.hero;
		if (!ringkeeper(h) || h.pointsInTalent(Talent.RING_RESONANCE) <= 0) return 0;
		Ring[] w = worn(h);
		if (w.length < 2 || (w[0] != ring && w[1] != ring)) return 0;
		Ring other = w[0] == ring ? w[1] : w[0];
		int diff = other.mazeBaseLvl() - ring.mazeBaseLvl();
		if (diff <= 0) return 0;
		return (int)Math.floor(diff * MazeTower.T3_RESONANCE[pts(h, Talent.RING_RESONANCE)]);
	}

	// =====================================================================
	// 공격 적중 (Hero.attackProc, 방어 적용 후 피해)
	// =====================================================================

	public static int onHeroAttackProc(Hero hero, Char enemy, int damage){
		float mult = 1f;
		boolean skill = isSkill();

		// ----- 추적자 -----
		if (tracker(hero)){
			Prey prey = enemy.buff(Prey.class);
			int pm = pts(hero, Talent.PREY_MARK);
			if (!skill && prey != null) mult += MazeTower.T3_PREY_DMG[pm];
			//저격: 활을 든 채 떨어진 칸당
			int ls = pts(hero, Talent.LONG_SHOT);
			if (ls > 0 && WornShortbow.equipped(hero) != null){
				int d = Math.min(MazeTower.T3_SNIPE_MAX_TILES, Dungeon.level.distance(hero.pos, enemy.pos));
				mult += d * MazeTower.T3_SNIPE_PER_TILE[ls];
			}
			//스킬로 맞히면 사냥감, 사냥감을 다시 맞히면 지속시간을 처음으로
			if (pm > 0 && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY && (skill || prey != null)){
				Buff.prolong(enemy, Prey.class, preyTurns(hero));
			}
		}

		// ----- 4차 정령 매: 사냥 신호 (매가 쪼은 적에게 다음 기본공격) -----
		if (!skill){
			HawkSignal sig = enemy.buff(HawkSignal.class);
			if (sig != null){
				mult += sig.bonus;
				sig.detach();
			}
		}

		// ----- 고리지기 (맨손 공격) -----
		boolean unarmed = ringsOpen(hero) && hero.belongings.attackingWeapon() == null;
		if (unarmed){
			float p = pulse(hero);
			mult += MazeTower.T3_BARE_HUNT[pts(hero, Talent.BARE_HUNT)];
			if (wears(hero, RingOfHaste.class)){
				mult += movesLastTurn() * MazeTower.RK_MOMENTUM_DMG * p;
			}
			if (wears(hero, RingOfMight.class) && hero.belongings.armor() != null){
				int spare = hero.STR() - hero.belongings.armor().STRReq();
				if (spare > 0) mult += spare * MazeTower.RK_SPARE_STR_DMG * p;
			}
			if (wears(hero, RingOfFuror.class)){
				Heat heat = Buff.affect(hero, Heat.class);
				heat.stack();
			}
			if (wears(hero, RingOfEnergy.class)){
				ArrayList<Wand> wands = hero.belongings.getAllItems(Wand.class);
				if (!wands.isEmpty()){
					Random.element(wands).gainCharge(MazeTower.RK_FLOW_CHARGE * p);
					Item.updateQuickslot();
				}
			}
			if (wears(hero, RingOfWealth.class) && enemy instanceof Mob && enemy.buff(Pickpocketed.class) == null
					&& enemy.alignment == Char.Alignment.ENEMY && Random.Float() < MazeTower.RK_STEAL_CHANCE * p){
				pickpocket(hero, (Mob)enemy);
			}
		}

		damage = Math.round(damage * mult);

		//정확성: 급소 (최종 피해 ×1.3)
		if (unarmed && wears(hero, RingOfAccuracy.class)){
			float accBonus = (RingOfAccuracy.accuracyMultiplier(hero) - 1f) * 100f;
			if (accBonus > 0 && Random.Float() < accBonus * MazeTower.RK_CRIT_PER_ACC * pulse(hero)){
				damage = Math.round(damage * MazeTower.RK_CRIT_DMG);
				if (enemy.sprite != null){
					enemy.sprite.showStatus(CharSprite.WARNING, Messages.get(HunterEffects.class, "crit"));
					enemy.sprite.centerEmitter().burst(SparkParticle.FACTORY, 4);
				}
			}
		}
		return damage;
	}

	//완력: 반지 너클 (맨손 공격이 반지 레벨당 방어 1 무시)
	public static int armorAfterIgnore(Hero hero, int dr){
		if (dr <= 0 || !passive(hero, RingOfForce.class) || hero.belongings.attackingWeapon() != null) return dr;
		int lvl = Ring.getBuffedBonus(hero, RingOfForce.Force.class);
		return Math.max(0, dr - Math.max(0, lvl));
	}

	// ----- 회피: 흘리기 반격 -----
	private static float lastCounter = -1;

	public static void onDodged(Char attacker, Char defender){
		if (!(defender instanceof Hero)) return;
		final Hero hero = (Hero)defender;
		if (!passive(hero, RingOfEvasion.class)) return;
		if (!attacker.isAlive() || attacker.alignment != Char.Alignment.ENEMY) return;
		if (!Dungeon.level.adjacent(attacker.pos, hero.pos)) return; //근접 공격만
		if (lastCounter == Actor.now()) return; //턴당 1회
		lastCounter = Actor.now();
		BloomEffects.beginNormal(hero); //반격은 일반 공격 판정 (새 행동)
		if (hero.sprite != null && attacker.sprite != null){
			com.shatteredpixel.shatteredpixeldungeon.effects.SkillEffects.hit("slash", hero, attacker, 0);
		}
		hero.attack(attacker, MazeTower.RK_COUNTER_DMG * pulse(hero), 0, 1f);
	}

	// ----- 분노: 열기 -----
	public static class Heat extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		int stacks = 0;

		void stack(){
			stacks = Math.min(MazeTower.RK_HEAT_MAX, stacks + 1);
			spend(MazeTower.RK_HEAT_TURNS - cooldown()); //지속시간 갱신
		}

		@Override
		public int icon() { return BuffIndicator.FURY; }
		@Override
		public void tintIcon(Image icon) { icon.hardlight(1f, 0.5f, 0.2f); }
		@Override
		public String iconTextDisplay() { return Integer.toString(stacks); }
		@Override
		public float iconFadePercent() { return Math.max(0, 1f - visualcooldown() / MazeTower.RK_HEAT_TURNS); }
		@Override
		public String desc() {
			return Messages.get(this, "desc", stacks, Math.round(stacks * MazeTower.RK_HEAT_SPEED * pulse(Dungeon.hero) * 100));
		}
		@Override
		public void storeInBundle(Bundle bundle) { super.storeInBundle(bundle); bundle.put("stacks", stacks); }
		@Override
		public void restoreFromBundle(Bundle bundle) { super.restoreFromBundle(bundle); stacks = bundle.getInt("stacks"); }
	}

	public static float heatSpeedMulti(Hero hero){
		Heat h = hero.buff(Heat.class);
		if (h == null || !passive(hero, RingOfFuror.class)) return 1f;
		return 1f + h.stacks * MazeTower.RK_HEAT_SPEED * pulse(hero);
	}

	// ----- 신속: 추진력 (직전 1턴 동안 이동한 칸 수) -----
	private static final ArrayList<Float> heroMoves = new ArrayList<>();

	static int movesLastTurn(){
		float now = Actor.now();
		int n = 0;
		for (Float t : heroMoves) if (now - t <= 1.01f) n++;
		return Math.min(MazeTower.RK_MOMENTUM_TILES, n);
	}

	// ----- 인내: 역경 -----
	public static class Adversity extends CounterBuff {
		{ revivePersists = true; }
	}

	public static void onTenacityReduced(Hero hero, int reduced){
		if (reduced <= 0 || !passive(hero, RingOfTenacity.class)) return;
		Adversity a = Buff.affect(hero, Adversity.class);
		boolean before = a.count() >= MazeTower.RK_ADVERSITY_NEED;
		a.countUp(reduced);
		if (!before && a.count() >= MazeTower.RK_ADVERSITY_NEED && hero.sprite != null){
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(HunterEffects.class, "adversity"));
			hero.sprite.emitter().burst(Speck.factory(Speck.UP), 5);
			GLog.p(Messages.get(HunterEffects.class, "adversity_log", MazeTower.RK_ADVERSITY_ARMOR));
		}
	}

	public static int adversityArmor(Hero hero){
		Adversity a = hero.buff(Adversity.class);
		if (a == null || a.count() < MazeTower.RK_ADVERSITY_NEED || !passive(hero, RingOfTenacity.class)) return 0;
		return MazeTower.RK_ADVERSITY_ARMOR;
	}

	static int adversityCount(Hero hero){
		Adversity a = hero.buff(Adversity.class);
		return a == null ? 0 : (int)Math.min(MazeTower.RK_ADVERSITY_NEED, a.count());
	}

	// ----- 부유함: 소매치기 -----
	public static class Pickpocketed extends Buff {}

	static void pickpocket(Hero hero, Mob mob){
		Buff.affect(mob, Pickpocketed.class);
		Item loot = null;
		try {
			loot = mob.createLoot();
		} catch (Exception ignored){ }
		if (loot == null) loot = new Gold().random();
		if (loot instanceof Gold){
			((Gold)loot).doPickUp(hero, hero.pos);
		} else if (!loot.collect()){
			Dungeon.level.drop(loot, hero.pos).sprite.drop();
		}
		if (hero.sprite != null){
			hero.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(HunterEffects.class, "stole"));
			RingOfWealth.showFlareForBonusDrop(hero.sprite);
		}
		GLog.i(Messages.get(HunterEffects.class, "stole_log", loot.name()));
	}

	// =====================================================================
	// 이동 (Char.move)
	// =====================================================================

	public static void onCharMoved(Char ch, int oldPos){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		if (ch == hero){
			if (ringkeeper(hero)){
				heroMoves.add(Actor.now());
				while (heroMoves.size() > 6) heroMoves.remove(0);
			}
			return;
		}
		//덫: 사냥감이 다가오면 1칸마다 확률로 느려짐
		if (tracker(hero) && ch.buff(Prey.class) != null && ch.alignment == Char.Alignment.ENEMY){
			int lvl = pts(hero, Talent.SNARE);
			if (lvl > 0 && Dungeon.level.distance(ch.pos, hero.pos) < Dungeon.level.distance(oldPos, hero.pos)
					&& Random.Float() < MazeTower.T3_SNARE_CHANCE[lvl]){
				Buff.prolong(ch, Cripple.class, MazeTower.T3_SNARE_TURNS);
				if (Dungeon.level.heroFOV[ch.pos]){
					CellEmitter.bottom(ch.pos).burst(EarthParticle.FACTORY, 6);
					if (ch.sprite != null) ch.sprite.showStatus(CharSprite.WARNING, Messages.get(HunterEffects.class, "snared"));
				}
			}
		}
	}

	// =====================================================================
	// 처치 (BloomEffects.onEnemyKilled, 주인공이 처치했을 때만)
	// =====================================================================

	public static void onEnemyKilled(Hero hero, Char enemy){
		//추적자 마나 흡수: 사냥감을 처치하면
		if (tracker(hero) && enemy.buff(Prey.class) != null){
			int m = MazeTower.T3_PREY_MANA[pts(hero, Talent.PREY_DRAIN)];
			if (m > 0){
				Mana.get(hero).gain(m);
				TalentEffects.manaRefund(hero, enemy, m);
			}
		}
		//고리지기 반지 꽃: 처치 시 마나 + 체력 (분홍빛 회복)
		if (ringkeeper(hero) && pts(hero, Talent.RING_BLOOM) > 0){
			int m = MazeTower.T3_RING_BLOOM_MANA[pts(hero, Talent.RING_BLOOM)];
			Mana.get(hero).gain(m);
			int heal = Math.min(MazeTower.T3_RING_BLOOM_HP, hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				if (hero.sprite != null){
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
			if (hero.sprite != null){
				hero.sprite.emitter().burst(PINK_HEAL, 3);
				hero.sprite.showStatus(0x4488FF, "+" + m);
			}
		}
	}

	//원작 회복 입자를 살짝 분홍빛으로
	private static final Emitter.Factory PINK_HEAL = new Emitter.Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			Speck s = (Speck)emitter.recycle(Speck.class);
			s.reset(index, x, y, Speck.HEALING);
			s.hardlight(1f, 0.72f, 0.85f);
		}
	};

	// =====================================================================
	// 고리지기 반지 스킬
	// =====================================================================

	//반지마다 스킬 이름 키 (null = 스킬 없음: 회피·신속·힘·인내)
	public static String skillKey(Ring r){
		if (r instanceof RingOfAccuracy)      return "pressure";
		if (r instanceof RingOfFuror)         return "frenzy";
		if (r instanceof RingOfElements)      return "purify";
		if (r instanceof RingOfForce)         return "toss";
		if (r instanceof RingOfEnergy)        return "burst";
		if (r instanceof RingOfWealth)        return "goldfist";
		if (r instanceof RingOfSharpshooting) return "volley";
		return null;
	}

	//쓸 수 있는 반지 스킬 (끼고 있는 반지 중 스킬이 있는 것, 차례대로). 무기를 들면 없음
	public static Ring[] skillRings(Hero h){
		if (!ringsOpen(h)) return new Ring[0];
		ArrayList<Ring> res = new ArrayList<>();
		for (Ring r : worn(h)) if (skillKey(r) != null && r.isKnown()) res.add(r);
		return res.toArray(new Ring[0]);
	}

	public static String skillName(Ring r){
		return Messages.get(HunterEffects.class, "skill_" + skillKey(r));
	}

	//마나 말고 따로 필요한 것 (충전된 마법봉, 금화, 투척 무기)
	public static boolean canAfford(Hero h, Ring r){
		if (Mana.get(h).cur() < SwordSkill.manaCost(h)) return false;
		String k = skillKey(r);
		if ("burst".equals(k)) return bestWand(h) != null;
		if ("goldfist".equals(k)) return Dungeon.gold >= goldCost();
		if ("volley".equals(k)) return !h.belongings.getAllItems(MissileWeapon.class).isEmpty();
		return true;
	}

	static int goldCost(){
		return Dungeon.depth * MazeTower.RK_GOLD_PER_DEPTH;
	}

	static Wand bestWand(Hero h){
		Wand best = null;
		for (Wand w : h.belongings.getAllItems(Wand.class)){
			if (w.curCharges > 0 && (best == null || w.curCharges > best.curCharges)) best = w;
		}
		return best;
	}

	public static void useRingSkill(final Hero hero, final Ring ring){
		final String key = skillKey(ring);
		if (key == null || !ringsOpen(hero)) return;
		final int cost = SwordSkill.manaCost(hero);
		if (Mana.get(hero).cur() < cost){
			GLog.w(Messages.get(SwordSkill.class, "no_mana"));
			ActionIndicator.shake();
			return;
		}
		if (!canAfford(hero, ring)){
			GLog.w(Messages.get(HunterEffects.class, "cant_" + key, goldCost()));
			return;
		}

		//대상이 필요 없는 스킬: 정화
		if ("purify".equals(key)){
			purify(hero, cost);
			return;
		}

		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				Char enemy = Actor.findChar(cell);
				if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[cell]){
					GLog.w(Messages.get(SwordSkill.class, "no_target"));
					return;
				}
				if ("volley".equals(key)){
					if (new Ballistica(hero.pos, enemy.pos, Ballistica.PROJECTILE).collisionPos != enemy.pos){
						GLog.w(Messages.get(SwordSkill.class, "no_path"));
						return;
					}
					volley(hero, enemy, cost);
					return;
				}
				if (!Dungeon.level.adjacent(hero.pos, enemy.pos)){
					GLog.w(Messages.get(SwordSkill.class, "not_adjacent"));
					return;
				}
				if (Mana.get(hero).cur() < cost || !canAfford(hero, ring)) return;
				switch (key){
					case "pressure": pressure(hero, enemy, cost); break;
					case "frenzy":   frenzy(hero, enemy, cost); break;
					case "toss":     toss(hero, enemy, cost); break;
					case "burst":    burst(hero, enemy, cost); break;
					case "goldfist": goldFist(hero, enemy, cost); break;
				}
			}

			@Override
			public String prompt() {
				return Messages.get(SwordSkill.class, "volley".equals(key) ? "prompt_ranged" : "prompt");
			}
		});
	}

	// ----- 정확성: 혈점 찌르기 (피해 없음, 턴 소모 없음) -----
	public static class PressurePoint extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}
		@Override
		public int icon() { return BuffIndicator.VULNERABLE; }
		@Override
		public void tintIcon(Image icon) { icon.hardlight(1f, 0.3f, 0.3f); }
		@Override
		public float iconFadePercent() { return Math.max(0, 1f - visualcooldown() / MazeTower.RK_PRESSURE_TURNS); }
	}

	static void pressure(Hero hero, Char enemy, int cost){
		BloomEffects.beginSkill(hero, enemy, cost);
		Mana.get(hero).spend(cost);
		SproutEffects.onSkillUsed(hero);
		Buff.prolong(enemy, PressurePoint.class, MazeTower.RK_PRESSURE_TURNS);
		if (enemy.sprite != null){
			com.shatteredpixel.shatteredpixeldungeon.effects.SkillEffects.hit("thrust", hero, enemy, 0);
			enemy.sprite.flash();
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 1.3f);
		BloomEffects.endSkill(hero);
		Item.updateQuickslot();
	}

	// ----- 분노: 광란 연타 (맨손 5타) -----
	static void frenzy(Hero hero, Char enemy, int cost){
		SwordSkill s = SwordSkill.ringSkill("frenzy", MazeTower.RK_FRENZY_HITS, MazeTower.RK_FRENZY_DMG);
		SwordSkill.launchRing(hero, s, enemy, cost);
	}

	// ----- 원소: 정화 -----
	public static class Purity extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		@Override
		public int icon() { return BuffIndicator.IMMUNITY; }
		@Override
		public void tintIcon(Image icon) { icon.hardlight(0.6f, 1f, 0.8f); }
		@Override
		public float iconFadePercent() { return Math.max(0, 1f - visualcooldown() / MazeTower.RK_PURIFY_TURNS); }
	}

	static void purify(Hero hero, int cost){
		BloomEffects.beginSkill(hero, null, cost);
		Mana.get(hero).spend(cost);
		SproutEffects.onSkillUsed(hero);
		for (Buff b : hero.buffs()){
			if (b.type == Buff.buffType.NEGATIVE) b.detach();
		}
		Buff.prolong(hero, Purity.class, MazeTower.RK_PURIFY_TURNS);
		if (hero.sprite != null){
			hero.sprite.emitter().start(ShaftParticle.FACTORY, 0.2f, 4);
			hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
		}
		Sample.INSTANCE.play(Assets.Sounds.DRINK, 0.6f, 1.4f);
		BloomEffects.endSkill(hero);
		hero.spendAndNext(Actor.TICK);
	}

	// ----- 완력: 던지기 -----
	static void toss(final Hero hero, final Char enemy, int cost){
		SwordSkill s = SwordSkill.ringSkill("toss", 1, MazeTower.RK_THROW_DMG);
		s.afterHit = new SwordSkill.HitHook() {
			@Override
			public void onHit(Hero h, Char e, boolean hit, int index) {
				if (hit && e.isAlive()) throwAway(h, e);
			}
		};
		SwordSkill.launchRing(hero, s, enemy, cost);
	}

	static void throwAway(Hero hero, Char enemy){
		if (enemy.properties().contains(Char.Property.IMMOVABLE)) {
			Buff.prolong(enemy, Paralysis.class, MazeTower.RK_THROW_STUN);
			return;
		}
		int dir = enemy.pos - hero.pos;
		Ballistica traj = new Ballistica(enemy.pos, enemy.pos + dir, Ballistica.MAGIC_BOLT);
		int power = MazeTower.RK_THROW_DIST;
		if (enemy.properties().contains(Char.Property.BOSS)) power = (power + 1) / 2;
		int dist = Math.min(traj.dist, power);
		boolean wall = traj.dist <= power;
		Char other = dist > 0 ? Actor.findChar(traj.path.get(dist)) : null;
		if (other == enemy) other = null;
		boolean collided = wall || other != null;

		WandOfBlastWaveHelper.throwChar(enemy, traj, power, hero);
		Buff.prolong(enemy, Paralysis.class, MazeTower.RK_THROW_STUN);
		if (collided){
			int dmg = hero.damageRoll(); //부딪힌 쪽도 맨손 평타 100%
			if (other != null && other.isAlive()){
				other.damage(dmg, hero);
				if (other.isAlive()) Buff.prolong(other, Paralysis.class, MazeTower.RK_THROW_STUN);
			}
			if (enemy.isAlive()) enemy.damage(dmg, hero);
			if (enemy.sprite != null) enemy.sprite.centerEmitter().burst(Speck.factory(Speck.DUST), 5);
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
		}
	}

	//원작 날려 보내기 (충돌 피해는 위에서 따로 계산하므로 끈다)
	static class WandOfBlastWaveHelper {
		static void throwChar(Char ch, Ballistica traj, int power, Hero cause){
			com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave.throwChar(ch, traj, power, true, false, cause);
		}
	}

	// ----- 충전: 충전 터뜨리기 -----
	static void burst(final Hero hero, final Char enemy, int cost){
		final Wand w = bestWand(hero);
		if (w == null) return;
		final int charges = w.curCharges;
		w.curCharges = 0;
		Item.updateQuickslot();
		SwordSkill s = SwordSkill.ringSkill("burst", 1, MazeTower.RK_BURST_DMG + charges * MazeTower.RK_BURST_PER_CHARGE);
		s.afterHit = new SwordSkill.HitHook() {
			@Override
			public void onHit(Hero h, Char e, boolean hit, int index) {
				if (!hit || !e.isAlive()) return;
				//그 마법봉의 원소가 실린다
				if (w instanceof WandOfFireblast)       BloomEffects.ignite(h, e, 4f);
				else if (w instanceof WandOfFrost)      BloomEffects.chill(h, e, 3f);
				else if (w instanceof WandOfLightning)  Buff.prolong(e, StaticCharge.class, MazeTower.STATIC_TURNS);
				else if (w instanceof WandOfCorrosion)  Buff.affect(e, Corrosion.class).set(3f, Dungeon.scalingDepth() / 2 + 1);
				if (e.sprite != null) e.sprite.centerEmitter().burst(MagicMissile.MagicParticle.FACTORY, 6 + charges * 2);
			}
		};
		SwordSkill.launchRing(hero, s, enemy, cost);
	}

	// ----- 부유함: 금화 주먹 -----
	static void goldFist(final Hero hero, final Char enemy, int cost){
		final int gold = goldCost();
		Dungeon.gold -= gold;
		SwordSkill s = SwordSkill.ringSkill("goldfist", 1, MazeTower.RK_GOLD_DMG);
		s.afterHit = new SwordSkill.HitHook() {
			@Override
			public void onHit(Hero h, Char e, boolean hit, int index) {
				if (e.sprite != null) e.sprite.centerEmitter().burst(Speck.factory(Speck.COIN), 6);
				if (hit && !e.isAlive()){
					Dungeon.gold += gold; //처치하면 쓴 금화를 돌려받는다
					if (h.sprite != null) h.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(HunterEffects.class, "gold_back", gold));
				}
			}
		};
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		SwordSkill.launchRing(hero, s, enemy, cost);
	}

	// ----- 저격: 연속 투척 (가방의 투척 무기 무작위 2개) -----
	static void volley(final Hero hero, final Char enemy, int cost){
		ArrayList<MissileWeapon> stacks = hero.belongings.getAllItems(MissileWeapon.class);
		if (stacks.isEmpty()) return;
		BloomEffects.beginSkill(hero, enemy, cost);
		Mana.get(hero).spend(cost);
		SproutEffects.onSkillUsed(hero);
		final ArrayList<MissileWeapon> picks = new ArrayList<>();
		for (int i = 0; i < MazeTower.RK_VOLLEY_COUNT; i++){
			ArrayList<MissileWeapon> left = new ArrayList<>();
			for (MissileWeapon m : stacks) if (m.quantity() > 0) left.add(m);
			if (left.isEmpty()) break;
			MissileWeapon m = Random.element(left);
			picks.add((MissileWeapon)m.detach(hero.belongings.backpack));
		}
		hero.busy();
		throwNext(hero, enemy, picks, 0);
	}

	static void throwNext(final Hero hero, final Char enemy, final ArrayList<MissileWeapon> picks, final int i){
		if (i >= picks.size() || !enemy.isAlive() || hero.sprite == null || hero.sprite.parent == null){
			//남은 것은 가방으로
			for (int j = i; j < picks.size(); j++) if (!picks.get(j).collect()) Dungeon.level.drop(picks.get(j), hero.pos).sprite.drop();
			BloomEffects.endSkill(hero);
			Item.updateQuickslot();
			hero.spendAndNext(hero.attackDelay());
			return;
		}
		final MissileWeapon mw = picks.get(i);
		((MissileSprite)hero.sprite.parent.recycle(MissileSprite.class)).reset(hero.sprite, enemy.sprite, mw, new Callback() {
			@Override
			public void call() {
				SwordSkill.active = VOLLEY;
				hero.belongings.thrownWeapon = mw;
				boolean hit = hero.attack(enemy, 1f, 0, 1f);
				hero.belongings.thrownWeapon = null;
				SwordSkill.active = null;
				mw.mazeAfterThrow(enemy, enemy.pos, hit);
				hero.sprite.parent.add(new com.watabou.noosa.tweeners.Delayer(MazeTower.SKILL_HIT_INTERVAL){
					@Override
					protected void onComplete() {
						super.onComplete();
						throwNext(hero, enemy, picks, i + 1);
					}
				});
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.MISS, 0.6f, 1.3f);
	}

	//연속 투척을 스킬로 치기 위한 표시 (SwordSkill.active)
	static final SwordSkill VOLLEY = SwordSkill.ringSkill("volley", 1, 1f);

	// ----- 두 번째 반지 스킬 버튼 (검투사 필살기 버튼 자리를 빌린다: 고리지기는 필살기가 없다) -----
	public static class SecondRingButton implements ActionIndicator.Action {
		public static final SecondRingButton INSTANCE = new SecondRingButton();
		private Ring shown = null;
		private boolean lastAfford = true;

		Ring ring(){
			Ring[] r = skillRings(Dungeon.hero);
			return r.length >= 2 ? r[1] : null;
		}

		//상태창이 매 프레임 부른다 (Mana.refreshSkillButton)
		public void refresh(){
			Hero h = Dungeon.hero;
			if (h == null || !ringkeeper(h)) return;
			Ring r = ring();
			if (r == null){
				shown = null;
				FinisherIndicator.clearAction(this);
			} else if (FinisherIndicator.action != this || shown != r){
				shown = r;
				FinisherIndicator.setAction(this);
			} else if (canAfford(h, r) != lastAfford){
				FinisherIndicator.refresh();
			}
			if (r != null) lastAfford = canAfford(h, r);
		}

		@Override
		public String actionName() {
			Ring r = ring();
			return r == null ? "" : skillName(r);
		}

		@Override
		public Visual primaryVisual() {
			Ring r = ring();
			ItemSprite s = r == null ? new ItemSprite() : new ItemSprite(r);
			if (r != null && !canAfford(Dungeon.hero, r)) s.tint(0x808080, 0.75f);
			return s;
		}

		@Override
		public Visual secondaryVisual() {
			BitmapText t = new BitmapText(PixelScene.pixelFont);
			t.text(Integer.toString(SwordSkill.manaCost(Dungeon.hero)));
			Ring r = ring();
			t.hardlight(r != null && canAfford(Dungeon.hero, r) ? 0x88BBFF : 0xFF6666);
			t.measure();
			return t;
		}

		@Override
		public int indicatorColor() {
			return 0x2255CC;
		}

		@Override
		public void doAction() {
			Ring r = ring();
			if (r != null) useRingSkill(Dungeon.hero, r);
		}
	}

	// =====================================================================
	// 반지 설명 (고리지기에게만)
	// =====================================================================

	public static String ringInfo(Ring ring){
		Hero h = Dungeon.hero;
		if (!ringkeeper(h) || !ring.isKnown()) return "";
		String name = ring.getClass().getSimpleName().toLowerCase();
		float p = pulse(h);
		String passive;
		switch (name){
			case "ringofaccuracy":   passive = Messages.get(HunterEffects.class, "p_" + name, String.format("%.1f", MazeTower.RK_CRIT_PER_ACC * 100 * p)); break;
			case "ringofevasion":    passive = Messages.get(HunterEffects.class, "p_" + name, Math.round(MazeTower.RK_COUNTER_DMG * p * 100)); break;
			case "ringofhaste":      passive = Messages.get(HunterEffects.class, "p_" + name, Math.round(MazeTower.RK_MOMENTUM_DMG * p * 100), MazeTower.RK_MOMENTUM_TILES); break;
			case "ringoffuror":      passive = Messages.get(HunterEffects.class, "p_" + name, Math.round(MazeTower.RK_HEAT_SPEED * p * 100), MazeTower.RK_HEAT_MAX); break;
			case "ringoftenacity":   passive = Messages.get(HunterEffects.class, "p_" + name, MazeTower.RK_ADVERSITY_NEED, MazeTower.RK_ADVERSITY_ARMOR, adversityCount(h)); break;
			case "ringofmight":      passive = Messages.get(HunterEffects.class, "p_" + name, Math.round(MazeTower.RK_SPARE_STR_DMG * p * 100)); break;
			case "ringofforce":      passive = Messages.get(HunterEffects.class, "p_" + name); break;
			case "ringofenergy":     passive = Messages.get(HunterEffects.class, "p_" + name, Math.round(MazeTower.RK_FLOW_CHARGE * p * 100)); break;
			case "ringofwealth":     passive = Messages.get(HunterEffects.class, "p_" + name, Math.round(MazeTower.RK_STEAL_CHANCE * p * 100)); break;
			default: passive = null;
		}
		String key = skillKey(ring);
		if (passive == null && key == null) return "";
		String s = "\n\n" + Messages.get(HunterEffects.class, "ring_title");
		if (passive != null) s += "\n" + passive;
		if (key != null) s += "\n" + Messages.get(HunterEffects.class, "s_" + key, SwordSkill.manaCost(h), goldCost());
		s += "\n" + Messages.get(HunterEffects.class, "ring_unarmed");
		return s;
	}

	// =====================================================================
	// 4차 정령 매: 사냥 신호 (매가 쪼은 적, SpiritHawk에서 건다)
	// =====================================================================

	public static class HawkSignal extends FlavourBuff {
		{ type = buffType.NEGATIVE; announced = false; }
		public float bonus = 0f;
		@Override
		public int icon() { return BuffIndicator.MARK; }
		@Override
		public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.3f, 0.5f); }
		@Override
		public void storeInBundle(Bundle bundle) { super.storeInBundle(bundle); bundle.put("bonus", bonus); }
		@Override
		public void restoreFromBundle(Bundle bundle) { super.restoreFromBundle(bundle); bonus = bundle.getFloat("bonus"); }
	}
}
