/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SnowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SwordSkill;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

import static com.shatteredpixel.shatteredpixeldungeon.MazeTower.*;

//Maze Tower: 마법사 지팡이 합성 스킬 (명세 16번).
//지팡이에 합성한 막대에 따라 스킬이 바뀐다. 비용은 마나 30 + 지팡이 충전 1 (WandMana가 처리).
//피해는 모두 지팡이 평타 피해의 배율이고, 마법봉을 피해 출처로 넘겨 2차 스킬 특성이 붙는다
//(BloomEffects.onStaffSkillDamage). 원작 막대 이펙트에 이펙트를 한 가지씩 더한다(날아가는 궤적).
//정해지지 않은 막대는 원작 막대 효과를 그대로 쏜다.
public class StaffSkills {

	public enum Kind {
		BASE_BOLT,       //합성하지 않은 지팡이의 기본 마탄 (원작 마법 화살 효과, 피해 MAGE_BOLT_DMG)
		EMPOWERED_BOLT,  //마탄 합성: 강화 마탄
		FIRE_LANCE,      //화염 폭발 합성: 화염 창
		FROST_ARROW,     //서리 합성: 서리 화살
		THUNDER,         //번개 합성: 낙뢰
		ORIGINAL         //아직 기획 미정: 원작 막대 효과 그대로
	}

	//주인공이 든 지팡이에 합성된 막대로 쏘는 스킬 종류. 지팡이 스킬이 아니면 null.
	public static Kind kindFor( Hero hero, Wand wand ){
		MagesStaff staff = WandMana.equippedStaff(hero);
		if (staff == null || staff.imbuedWand() != wand || !SwordSkill.heroUsesSkills(hero)) return null;
		return kind(staff);
	}

	public static Kind kind( MagesStaff staff ){
		Wand w = staff.imbuedWand();
		if (w == null) return null;
		if (w instanceof WandOfMagicMissile) return staff.mazeImbued ? Kind.EMPOWERED_BOLT : Kind.BASE_BOLT;
		if (w instanceof WandOfFireblast)    return Kind.FIRE_LANCE;
		if (w instanceof WandOfFrost)        return Kind.FROST_ARROW;
		if (w instanceof WandOfLightning)    return Kind.THUNDER;
		return Kind.ORIGINAL;
	}

	//원작 막대 효과 대신 이 클래스가 쏘는지
	public static boolean custom( Hero hero, Wand wand ){
		Kind k = kindFor(hero, wand);
		return k == Kind.EMPOWERED_BOLT || k == Kind.FIRE_LANCE || k == Kind.FROST_ARROW || k == Kind.THUNDER;
	}

	//스킬 쏘기 (Wand.zapper에서 원작 fx/onZap 대신 호출). 끝나면 done (원작 wandUsed)을 부른다.
	public static void fire( final Hero hero, final Wand wand, int target, final Callback done ){
		final Kind kind = kindFor(hero, wand);
		final MagesStaff staff = WandMana.equippedStaff(hero);
		final Ballistica bolt = new Ballistica(hero.pos, target, Ballistica.MAGIC_BOLT);
		final int cell = bolt.collisionPos;

		if (kind == Kind.THUNDER){
			thunder(hero, wand, staff, cell, done);
			return;
		}

		int type;
		Emitter.Factory trail;
		switch (kind){
			case FIRE_LANCE:  type = MagicMissile.FIRE;          trail = FlameParticle.FACTORY; break;
			case FROST_ARROW: type = MagicMissile.FROST;         trail = SnowParticle.FACTORY;  break;
			case EMPOWERED_BOLT: default:
				type = MagicMissile.MAGIC_MISSILE; trail = BlueLight.FACTORY; break;
		}

		MagicMissile m = MagicMissile.boltFromChar(hero.sprite.parent, type, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				Char ch = Actor.findChar(cell);
				switch (kind){
					case FIRE_LANCE:  fireLance(hero, wand, staff, bolt, ch);  break;
					case FROST_ARROW: frostArrow(hero, wand, staff, ch, cell); break;
					default:          empoweredBolt(hero, wand, staff, ch, cell); break;
				}
				done.call();
			}
		});
		trail(hero, m, trail);
		Sample.INSTANCE.play( kind == Kind.FIRE_LANCE ? Assets.Sounds.BURNING : Assets.Sounds.ZAP );
	}

	private static int roll( Hero hero, MagesStaff staff, float multi ){
		return Math.round(staff.damageRoll(hero) * multi);
	}

	// ===== 강화 마탄 =====

	private static void empoweredBolt( Hero hero, Wand wand, MagesStaff staff, Char ch, int cell ){
		if (ch == null){
			Dungeon.level.pressCell(cell);
			return;
		}
		ch.damage(roll(hero, staff, STAFF_EMPOWERED_BOLT_DMG), wand);
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 1, Random.Float(0.87f, 1.15f) );
		if (ch.sprite != null){
			ch.sprite.burst(0xFFFFFFFF, 4);
			ch.sprite.centerEmitter().burst(BlueLight.FACTORY, 8);
		}
	}

	// ===== 화염 창 =====

	private static void fireLance( Hero hero, Wand wand, MagesStaff staff, Ballistica bolt, Char ch ){
		int hitCell = bolt.collisionPos;
		if (ch != null){
			ch.damage(roll(hero, staff, STAFF_FIRE_LANCE_DMG), wand);
			if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
			if (ch.sprite != null) ch.sprite.emitter().burst(FlameParticle.FACTORY, 6);
		} else {
			//아무도 맞지 않으면 닿은 칸에만 불
			seedFire(hitCell);
			return;
		}
		//적의 뒤쪽 일직선으로 불 (벽에서 멈춘다)
		Ballistica line = new Ballistica(hero.pos, hitCell, Ballistica.STOP_TARGET);
		int last = line.path.get(line.path.size() - 1);
		if (last == hitCell) return;
		Ballistica behind = new Ballistica(hitCell, last, Ballistica.STOP_SOLID);
		for (int i = 1; i <= FIRE_LANCE_LENGTH && i < behind.path.size() && i <= behind.dist; i++){
			int c = behind.path.get(i);
			if (Dungeon.level.solid[c]) break;
			seedFire(c);
		}
	}

	private static void seedFire( int cell ){
		if (Dungeon.level.solid[cell]) return;
		GameScene.add( Blob.seed( cell, FIRE_LANCE_FIRE, Fire.class ) );
	}

	// ===== 서리 화살 =====

	private static void frostArrow( Hero hero, Wand wand, MagesStaff staff, Char ch, int cell ){
		if (ch == null){
			Dungeon.level.pressCell(cell);
			return;
		}
		//피해가 먼저, 그 다음 혹한 (젖은 적은 혹한이 붙는 순간 확정 빙결)
		ch.damage(roll(hero, staff, STAFF_FROST_ARROW_DMG), wand);
		if (ch.isAlive()) Buff.affect(ch, Frostbite.class, FROSTBITE_TURNS);
		if (ch.sprite != null) ch.sprite.emitter().burst(SnowParticle.FACTORY, 8);
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 1, 1.1f );
	}

	// ===== 낙뢰 =====

	private static void thunder( final Hero hero, final Wand wand, final MagesStaff staff, final int cell, final Callback done ){
		final Char main = Actor.findChar(cell);
		final ArrayList<Char> hits = new ArrayList<>();
		if (main != null && main != hero) hits.add(main);

		//대상 주변의 다른 적에게 연쇄 (대상 포함 최대 THUNDER_MAX_TARGETS)
		if (main != null){
			ArrayList<Char> near = new ArrayList<>();
			for (Char c : Actor.chars()){
				if (c != main && c != hero && c.alignment == Char.Alignment.ENEMY && c.isAlive()
						&& Dungeon.level.distance(main.pos, c.pos) <= THUNDER_CHAIN_RANGE){
					near.add(c);
				}
			}
			Collections.shuffle(near, new java.util.Random(Random.Long()));
			for (Char c : near){
				if (hits.size() >= THUNDER_MAX_TARGETS) break;
				hits.add(c);
			}
		}

		//배율: 원작 번개 막대와 같다. 대상이 물 위이거나 젖음·화상 상태면 모두 그대로 (쏘는 순간 판정)
		float multi = 1f;
		if (!hits.isEmpty()){
			boolean full = Wet.onWater(main) || main.buff(Wet.class) != null || main.buff(Burning.class) != null;
			if (!full) multi = 0.4f + 0.6f / hits.size();
		}
		final float finalMulti = multi;

		//하늘에서 대상에게 큰 번개 (두 줄) + 대상에서 연쇄 대상으로
		PointF center = main != null && main.sprite != null ? main.sprite.center()
				: com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(cell);
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		arcs.add(new Lightning.Arc(new PointF(center.x - 3, center.y - 52), center));
		arcs.add(new Lightning.Arc(new PointF(center.x + 4, center.y - 46), center));
		for (Char c : hits){
			if (c != main && c.sprite != null) arcs.add(new Lightning.Arc(center, c.sprite.center()));
		}
		Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		PixelScene.shake(2, 0.3f);

		hero.sprite.parent.addToFront( new Lightning( arcs, new Callback() {
			@Override
			public void call() {
				for (Char c : hits){
					if (!c.isAlive()) continue;
					if (c.sprite != null){
						c.sprite.centerEmitter().burst(SparkParticle.FACTORY, c == main ? 12 : 4);
						c.sprite.flash();
					}
					c.damage(Math.round(staff.damageRoll(hero) * STAFF_THUNDER_DMG * finalMulti), wand);
					//정전기는 피해와 상관없이 모두에게 (불붙은 대상은 이 순간 폭발 반응)
					if (c.isAlive()) Buff.prolong(c, StaticCharge.class, STATIC_TURNS);
				}
				if (main == null) Dungeon.level.pressCell(cell);
				done.call();
			}
		}) );
	}

	// ===== 날아가는 궤적 (원작 막대 이펙트에 더하는 한 가지) =====

	private static void trail( Hero hero, final MagicMissile m, Emitter.Factory factory ){
		if (m == null || hero.sprite == null || hero.sprite.parent == null) return;
		Emitter e = new Emitter(){
			@Override
			public void update() {
				if (m.on && m.parent != null){
					pos(m.x - 2, m.y - 2, 4, 4);
				} else {
					on = false;
				}
				super.update();
			}
		};
		e.pos(m.x - 2, m.y - 2, 4, 4);
		e.pour(factory, 0.02f);
		hero.sprite.parent.add(e);
	}

	//강화 마탄: 날아가는 마탄 주변의 푸른 빛 입자
	public static class BlueLight extends PixelParticle.Shrinking {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((BlueLight)emitter.recycle( BlueLight.class )).reset( x, y );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public void reset( float x, float y ){
			revive();
			this.x = x;
			this.y = y;
			color( Random.oneOf(0x66CCFF, 0x88DDFF, 0x3399FF) );
			size( this.size = Random.Float(1.5f, 3f) );
			left = lifespan = Random.Float(0.3f, 0.55f);
			speed.polar( Random.Float(PointF.PI2), Random.Float(4f, 10f) );
			am = 0.9f;
		}

		@Override
		public void update() {
			super.update();
			am = left / lifespan;
		}
	}
}
