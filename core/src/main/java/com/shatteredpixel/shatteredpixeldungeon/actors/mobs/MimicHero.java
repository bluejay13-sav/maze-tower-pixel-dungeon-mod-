/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StenchGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx;
import com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Arrow;
import com.shatteredpixel.shatteredpixeldungeon.levels.HeartBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//Maze Tower: 25층 2페이즈 의태 영웅 (지시서 1122 3·4절, 1122b 3절). 잠식된 시체 자체가 사슴이다.
//처치하면 시체가 머리부터 발끝까지 무너지고 그 자리에서 사슴이 다시 만들어진다(사슴 체력 -25%). 경험치·드랍 없음.
//예고 공격은 바닥 경고·조준선을 보여 주고 나간다. 맞으면 방어력의 50%만 적용(도적 기습은 30%).
//기술을 시전하는 순간마다 화면 1px 흔들림(평타 제외).
public class MimicHero extends Mob {

	public static final int WARRIOR = 0, MAGE = 1, ROGUE = 2, HUNTRESS = 3;

	{
		spriteClass = MimicSprite.class;
		EXP = 0;
		state = HUNTING;
		properties.add(Property.MINIBOSS);
		properties.add(Property.DEMONIC);
	}

	//짐승이 의태로 바뀔 때 소환 연출로 넘겨주는 짐승 그림 (물줄기가 되어 사라짐)
	public static com.shatteredpixel.shatteredpixeldungeon.sprites.HeartBeastSprite pendingDeer = null;

	public int cls = WARRIOR;
	public int deerHP, kills, used;
	private boolean summoned = false;
	private boolean busy = false;

	//전사
	private int leapCd = MazeTower.MW_LEAP_FIRST;
	private int leapTarget = -1;
	private boolean raged = false;
	//마법사
	private int thornCd = MazeTower.MM_THORN_FIRST;
	private int thornCenter = -1;
	private int castPos = -1;
	private int blinkCd = 0;
	//도적
	private int stealthCd = MazeTower.MR_STEALTH_FIRST;
	private int poisonCd = MazeTower.MR_POISON_FIRST;
	private int coatLeft = 0;
	private HashSet<Integer> trail = new HashSet<>();
	//사냥꾼
	private int pierceCd = MazeTower.MH_PIERCE_CD;
	private int[] aimPath = new int[0];
	private int trapCd = MazeTower.MH_TRAP_CD;
	private HashMap<Integer, Integer> traps = new HashMap<>(); //칸 → 깔린 뒤 지난 턴
	private boolean trapsRevealed = false;

	//연출 (저장하지 않음)
	private Heart25Fx.CellWarn warn;
	private Group aimLine;
	private final ArrayList<ColorBlock> aimDots = new ArrayList<>();
	private final HashMap<Integer, Image> trapImages = new HashMap<>();
	private final HashMap<Integer, Image> trailImages = new HashMap<>();

	public void setup( int cls, int deerHP, int kills, int used ){
		this.cls = cls;
		this.deerHP = deerHP;
		this.kills = kills;
		this.used = used;
		applyStats();
	}

	private void applyStats(){
		switch (cls){
			case WARRIOR:  HP = HT = MazeTower.MW_HP; defenseSkill = MazeTower.MW_DEF; break;
			case MAGE:     HP = HT = MazeTower.MM_HP; defenseSkill = MazeTower.MM_DEF; break;
			case ROGUE:    HP = HT = MazeTower.MR_HP; defenseSkill = MazeTower.MR_DEF; break;
			default:       HP = HT = MazeTower.MH_HP; defenseSkill = MazeTower.MH_DEF; break;
		}
	}

	public static MimicHero find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs) if (m instanceof MimicHero && m.isAlive()) return (MimicHero) m;
		return null;
	}

	// ===== 수치 =====
	@Override
	public int attackSkill( Char target ) {
		switch (cls){
			case WARRIOR: return MazeTower.MW_ACC;
			case MAGE:    return MazeTower.MM_ACC;
			case ROGUE:   return MazeTower.MR_ACC;
			default:      return MazeTower.MH_ACC;
		}
	}

	@Override
	public int damageRoll() {
		switch (cls){
			case WARRIOR: return Math.round(Random.NormalIntRange(MazeTower.MW_MIN, MazeTower.MW_MAX) * (raged ? 1f + MazeTower.MW_RAGE_DMG : 1f));
			case MAGE:    return Random.NormalIntRange(MazeTower.MM_MIN, MazeTower.MM_MAX);
			case ROGUE:   return Random.NormalIntRange(MazeTower.MR_MIN, MazeTower.MR_MAX);
			default:
				int d = Random.NormalIntRange(MazeTower.MH_MIN, MazeTower.MH_MAX);
				return enemy != null && Dungeon.level.adjacent(pos, enemy.pos) ? Math.round(d * MazeTower.MH_CLAW_MULTI) : d;
		}
	}

	@Override
	public int drRoll() {
		int max;
		switch (cls){
			case WARRIOR: max = MazeTower.MW_DR; break;
			case MAGE:    max = MazeTower.MM_DR; break;
			case ROGUE:   max = MazeTower.MR_DR; break;
			default:      max = MazeTower.MH_DR; break;
		}
		return super.drRoll() + Random.NormalIntRange(0, max);
	}

	@Override
	public float speed() {
		float s = super.speed();
		if (cls == ROGUE){
			s *= MazeTower.MR_MOVE_SPEED;
			if (invisible > 0) s *= MazeTower.MR_STEALTH_SPEED;
		}
		return s;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (cls == MAGE || cls == HUNTRESS){
			if (Dungeon.level.adjacent(pos, enemy.pos)) return cls == HUNTRESS ? cornered() : true;
			return new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
		}
		return super.canAttack(enemy);
	}

	//(2026-11-22) 차지형 기술을 끊는 상태: 마비·빙결·화상, 또는 자기 칸에 마비 가스·불·냉기·전기
	private boolean interruptedHere(){
		if (paralysed > 0 || buff(Paralysis.class) != null || buff(Burning.class) != null
				|| buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class) != null) return true;
		for (Blob b : Dungeon.level.blobs.values()){
			if (!(b instanceof ParalyticGas || b instanceof Fire || b instanceof Freezing || b instanceof Electricity)) continue;
			if (b.volume > 0 && b.cur[pos] > 0) return true;
		}
		return false;
	}

	//마비·빙결·화상이 붙는 순간 바로 끊김 (가시밭 경고 중)
	@Override
	public synchronized boolean add( Buff buff ) {
		boolean ok = super.add(buff);
		if (ok && cls == MAGE && thornCenter != -1 && (buff instanceof Paralysis || buff instanceof Burning
				|| buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost)){
			cancelCharge();
		}
		return ok;
	}

	// ===== 피해 =====
	@Override
	public void damage( int dmg, Object src ) {
		//마법사 가시밭 경고 중: 번개·불 피해를 받으면 시전이 끊김
		if (cls == MAGE && thornCenter != -1 && src != null){
			String n = src.getClass().getSimpleName();
			if (n.contains("Lightning") || n.contains("Electric") || n.contains("Shock") || src instanceof Burning || src instanceof Fire || n.contains("Fire")){
				cancelCharge();
			}
		}
		super.damage(dmg, src);
		if (cls == WARRIOR && !raged && isAlive() && HP <= HT * MazeTower.MW_RAGE_HP){
			raged = true;
			if (sprite != null){
				sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "rage"));
				sprite.hardlight(1f, 0.55f, 0.55f);
			}
		}
	}

	@Override
	public void die( Object cause ) {
		clearFx();
		final int at = pos;
		if (sprite instanceof MimicSprite){
			((MimicSprite) sprite).collapse();
		}
		super.die(cause);
		HeartBeast.returnFrom(this);
	}

	@Override
	public void rollToDropLoot() {}

	// ===== 행동 =====
	@Override
	protected boolean act() {
		if (busy) return false;
		if (!summoned){
			summoned = true;
			if (sprite instanceof MimicSprite && sprite.visible){
				busy = true;
				((MimicSprite) sprite).summon(new Callback() {
					@Override
					public void call() {
						busy = false;
						spend(TICK);
						next();
					}
				});
				return false;
			}
		}
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) fieldOfView = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		Hero hero = Dungeon.hero;
		enemy = hero;
		enemySeen = true;
		state = HUNTING;

		if (paralysed > 0 || buff(Charm.class) != null){
			if (thornCenter != -1 || aimPath.length > 0) cancelCharge();
			return super.act();
		}
		if (cls == HUNTRESS) updateTraps();
		if (cls == ROGUE) checkTrailReveal();

		if (hero != null && hero.isAlive()){
			Boolean r = null;
			switch (cls){
				case WARRIOR:  r = actWarrior(hero); break;
				case MAGE:     r = actMage(hero); break;
				case ROGUE:    r = actRogue(hero); break;
				case HUNTRESS: r = actHuntress(hero); break;
			}
			if (r != null) return r;
		}
		return super.act();
	}

	private boolean sees( Hero h ){
		return fieldOfView[h.pos] && h.invisible <= 0;
	}

	// ---------------- 전사 ----------------
	private Boolean actWarrior( Hero hero ){
		if (leapTarget != -1){
			final int target = leapTarget;
			leapTarget = -1;
			return leap(target);
		}
		leapCd--;
		if (leapCd <= 0 && sees(hero) && Dungeon.level.distance(pos, hero.pos) <= MazeTower.MW_LEAP_RANGE
				&& Dungeon.level.distance(pos, hero.pos) > 1){
			leapCd = MazeTower.MW_LEAP_CD;
			leapTarget = hero.pos;
			if (sprite != null) sprite.turnTo(pos, hero.pos);
			showWarn(area3(hero.pos));
			Heart25Fx.shake();
			spend(TICK);
			return true;
		}
		return null;
	}

	private boolean leap( final int target ){
		final Hero hero = Dungeon.hero;
		final ArrayList<Integer> area = area3(target);
		//착지 칸에 영웅이 있으면 인접한 1칸으로 무작위로 밀려남
		int land = target;
		Char there = Actor.findChar(land);
		if (there != null && there != this && there != hero){
			land = pos;
		}
		final int landCell = land;
		final int from = pos;
		final Callback done = new Callback() {
			@Override
			public void call() {
				if (hero.isAlive() && area.contains(hero.pos)){
					strike(hero, MazeTower.MW_LEAP_MIN, MazeTower.MW_LEAP_MAX, MazeTower.B25_TELEGRAPH_ARMOR, raged ? 1f + MazeTower.MW_RAGE_DMG : 1f);
					if (hero.isAlive() && hero.pos == landCell){
						ArrayList<Integer> free = new ArrayList<>();
						for (int n : PathFinder.NEIGHBOURS8){
							int c = landCell + n;
							if (Dungeon.level.passable[c] && Actor.findChar(c) == null) free.add(c);
						}
						if (!free.isEmpty()){
							int to = Random.element(free);
							Actor.add(new com.shatteredpixel.shatteredpixeldungeon.effects.Pushing(hero, hero.pos, to));
							hero.pos = to;
							Dungeon.level.occupyCell(hero);
						}
					}
				}
				move(landCell, false);
				//착지: 3×3 번쩍 → 가운데에서 균열, 파편·핏방울
				Heart25Fx.CellWarn flash = Heart25Fx.CellWarn.show(area, null, 0xFF6070, 0);
				flash.fadeOut();
				MovieClip c = MazeFx.atCell(MazeFx.Fx.LEAP, landCell);
				if (c != null) c.hardlight(1f, 0.2f, 0.25f);
				CellEmitter.center(landCell).burst(BloodParticle.BURST, 10);
				CellEmitter.get(landCell).burst(Speck.factory(Speck.ROCK), 5);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 0.8f);
				if (warn != null){ warn.fadeOut(); warn = null; }
			}
		};
		if (sprite != null && sprite.visible){
			busy = true;
			sprite.jump(from, landCell == from ? target : landCell, new Callback() {
				@Override
				public void call() {
					done.call();
					if (sprite != null) sprite.place(pos);
					busy = false;
					spend(TICK);
					next();
				}
			});
			return false;
		}
		done.call();
		spend(TICK);
		return true;
	}

	// ---------------- 마법사 ----------------
	private Boolean actMage( final Hero hero ){
		//가시밭: 지난 턴에 경고한 3×3
		//(2026-11-22) 경고 중 밀려났거나(충격파 등), 마비·빙결·불·전기를 맞았으면 끊김.
		//  물약 가스·불은 가스가 자기 차례에 상태이상을 걸어서, 그보다 먼저 가시가 나가던 것을 고침: 자기 칸의 가스·불도 본다
		if (thornCenter != -1 && (castPos != pos || interruptedHere())){
			cancelCharge();
			spend(TICK);
			return true;
		}
		if (thornCenter != -1){
			int c = thornCenter;
			thornCenter = -1;
			ArrayList<Integer> area = area3(c);
			{
				for (int cell : area){
					CellEmitter.get(cell).burst(BloodParticle.BURST, 3);
					thornFx(cell);
				}
				if (area.contains(hero.pos)){
					strike(hero, MazeTower.MM_THORN_MIN, MazeTower.MM_THORN_MAX, MazeTower.B25_TELEGRAPH_ARMOR, 1f);
				}
				if (warn != null){ warn.fadeOut(); warn = null; }
				Sample.INSTANCE.play(Assets.Sounds.HIT_STAB, 1f, 0.7f);
				spend(TICK);
				return true;
			}
		}
		blinkCd--;
		thornCd--;
		//피의 점멸: 영웅이 인접하면 4칸 떨어진 곳으로
		if (Dungeon.level.adjacent(pos, hero.pos) && blinkCd <= 0){
			int to = blinkCell(hero);
			if (to != -1){
				blinkCd = MazeTower.MM_BLINK_CD;
				CellEmitter.center(pos).burst(BloodParticle.BURST, 10);
				Heart25Fx.shake();
				move(to, false);
				if (sprite != null) sprite.place(to);
				CellEmitter.center(to).burst(BloodParticle.BURST, 6);
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
				spend(TICK);
				return true;
			}
		}
		if (thornCd <= 0 && sees(hero)){
			thornCd = MazeTower.MM_THORN_CD;
			thornCenter = hero.pos;
			castPos = pos;
			if (sprite != null) sprite.turnTo(pos, hero.pos);
			showWarn(area3(hero.pos));
			Heart25Fx.shake();
			spend(TICK);
			return true;
		}
		//3~4칸 유지: 가까우면 물러나고, 멀거나 시야가 끊기면 다가간다
		int d = Dungeon.level.distance(pos, hero.pos);
		if (d < MazeTower.MM_KEEP_MIN && sees(hero)){
			int old = pos;
			if (getFurther(hero.pos)){
				spend(1 / speed());
				return moveSprite(old, pos);
			}
		}
		return null;
	}

	private int blinkCell( Hero hero ){
		HeartBossLevel lv = Dungeon.level instanceof HeartBossLevel ? (HeartBossLevel) Dungeon.level : null;
		ArrayList<Integer> cand = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (lv != null && !lv.inRoom(c)) continue;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (Dungeon.level.distance(c, hero.pos) == MazeTower.MM_BLINK_DIST) cand.add(c);
		}
		return cand.isEmpty() ? -1 : Random.element(cand);
	}

	//끝으로 갈수록 가늘어지는 혈관 가시 (코드 그림)
	//(2026-11-22, 미리보기 25F_5에 맞춤) 칸마다 혈관 가시: 굵은 큰 가시 하나(아래 넓고 끝으로 갈수록 가늘어짐,
	//  왼쪽 어둡고 오른쪽 밝은 명암, 끝은 옅은 흰빛) + 왼쪽에 휘어진 작은 가시 하나.
	//  낮게 솟음 → 다 솟음(잠시) → 반쯤 가라앉음 → 바닥에 작은 혹만 남음 → 사라짐
	private static final float[] THORN_STAGE_SCALE = { 0.35f, 1f, 0.55f, 0.18f };
	private static final float[] THORN_STAGE_TIME  = { 0.06f, 0.18f, 0.09f, 0.09f };

	private void thornFx( int cell ){
		final PointF p = DungeonTilemap.tileToWorld(cell);
		final Group g = new Group();
		GameScene.groupEffect(g);
		final int mainX = 8 + Random.Int(2);          //큰 가시 가운데 (칸 안 x)
		final int mainH = 11 + Random.Int(2);         //큰 가시 높이
		final int sideX = 3 + Random.Int(2);          //작은 가시 밑동
		final int sideH = 5 + Random.Int(2);
		buildThorn(g, p, mainX, mainH, sideX, sideH, THORN_STAGE_SCALE[0]);
		g.parent.add(new Gizmo(){
			float t = 0;
			int stage = 0;
			@Override public void update(){
				t += Game.elapsed;
				if (t < THORN_STAGE_TIME[stage]) return;
				t = 0;
				stage++;
				if (stage >= THORN_STAGE_SCALE.length){ g.killAndErase(); killAndErase(); return; }
				buildThorn(g, p, mainX, mainH, sideX, sideH, THORN_STAGE_SCALE[stage]);
			}
		});
	}

	private static void buildThorn( Group g, PointF p, int mainX, int mainH, int sideX, int sideH, float scale ){
		g.clear();
		float base = p.y + 15;
		//큰 가시: 아래 폭 4 → 끝 1
		int h = Math.max(1, Math.round(mainH * scale));
		for (int i = 0; i < h; i++){
			float k = (float) i / mainH;
			int w = Math.max(1, Math.round(4 * (1f - k)));
			int left = mainX - w / 2;
			boolean tip = i >= h - 2 && scale > 0.5f;
			for (int x = 0; x < w; x++){
				int col;
				if (tip) col = x == w - 1 ? 0xFFF2E6EA : 0xFFC8B8BE;          //끝: 옅은 흰빛
				else if (x == 0) col = 0xFF6A1024;                                //왼쪽 그림자
				else if (x == w - 1) col = 0xFFF08CA0;                            //오른쪽 빛
				else col = 0xFFC0304A;
				ColorBlock b = new ColorBlock(1, 1, col);
				b.x = p.x + left + x; b.y = base - i;
				g.add(b);
			}
		}
		//작은 가시: 1px 굵기, 위로 갈수록 왼쪽으로 휘어짐
		int sh = Math.max(1, Math.round(sideH * scale));
		for (int i = 0; i < sh; i++){
			int x = sideX - (i * i) / 8;
			boolean tip = i >= sh - 1 && scale > 0.5f;
			ColorBlock b = new ColorBlock(i == 0 ? 2 : 1, 1, tip ? 0xFFE8DCE0 : (i < sh / 2 ? 0xFF8A1A30 : 0xFFD05068));
			b.x = p.x + x; b.y = base - i;
			g.add(b);
		}
	}

	// ---------------- 도적 ----------------
	private Boolean actRogue( Hero hero ){
		if (coatLeft > 0) coatLeft--;
		stealthCd--;
		poisonCd--;
		if (poisonCd <= 0 && invisible <= 0){
			poisonCd = MazeTower.MR_POISON_CD;
			coatLeft = MazeTower.MR_POISON_TURNS;
			Heart25Fx.shake();
			if (sprite != null){
				sprite.showStatus(0xB070FF, Messages.get(this, "coat"));
				sprite.centerEmitter().burst(ElmoParticle.FACTORY, 4);
			}
			spend(TICK);
			return true;
		}
		if (stealthCd <= 0 && invisible <= 0){
			stealthCd = MazeTower.MR_STEALTH_CD;
			CellEmitter.get(pos).burst(Speck.factory(Speck.SMOKE), 8);
			Heart25Fx.shake();
			Buff.affect(this, Invisibility.class, MazeTower.MR_STEALTH_TURNS);
			if (Dungeon.level.distance(pos, hero.pos) <= 1){
				ArrayList<Integer> cand = new ArrayList<>();
				for (int c = 0; c < Dungeon.level.length(); c++){
					if (Dungeon.level.distance(c, hero.pos) <= MazeTower.MR_STEALTH_SHIFT && Dungeon.level.passable[c]
							&& Actor.findChar(c) == null) cand.add(c);
				}
				if (!cand.isEmpty()){
					int to = Random.element(cand);
					move(to, false);
					if (sprite != null) sprite.place(to);
					addTrail(to);
				}
			}
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
			spend(TICK);
			return true;
		}
		return null;
	}

	@Override
	public void move( int step, boolean travelling ) {
		super.move(step, travelling);
		if (cls == ROGUE && invisible > 0) addTrail(step);
	}

	private void addTrail( int cell ){
		trail.add(cell);
		drawTrail(cell);
	}

	private void drawTrail( int cell ){
		if (trailImages.containsKey(cell) || !(Dungeon.level != null)) return;
		Image i = new Image("sprites/maze_b7_flesh_fx.png");
		i.frame(new TextureFilm(i.texture, 24, 24).get(1 * 12 + 9));
		PointF p = DungeonTilemap.tileToWorld(cell);
		i.x = p.x - 4; i.y = p.y - 4;
		i.alpha(0.85f);
		GameScene.effect(i);
		trailImages.put(cell, i);
	}

	//핏방울 흔적이 남은 칸에 피해를 주는 물약을 뿌려 두면 은신이 풀린다
	private void checkTrailReveal(){
		if (invisible <= 0) return;
		HashSet<Integer> check = new HashSet<>(trail);
		check.add(pos);
		for (Blob b : Dungeon.level.blobs.values()){
			if (!(b instanceof ToxicGas || b instanceof ParalyticGas || b instanceof CorrosiveGas || b instanceof Fire
					|| b instanceof Freezing || b instanceof StenchGas || b instanceof Electricity)) continue;
			if (b.volume <= 0) continue;
			for (int c : check){
				if (b.cur[c] > 0){ reveal(); return; }
			}
		}
	}

	//은신 해제: 그림자 막이 찢겨 흩어짐 → 하얗게 번쩍 → 윤곽이 잠깐 빛남 → 1px 움찔
	public void reveal(){
		if (invisible <= 0 && buff(Invisibility.class) == null) return;
		Buff.detach(this, Invisibility.class);
		invisible = 0;
		if (sprite != null){
			sprite.alpha(1f);
			CellEmitter.get(pos).burst(ShadowParticle.UP, 14);
			sprite.flash();
			sprite.parent.add(new Gizmo(){
				float t = 0.25f;
				@Override public void update(){
					t -= Game.elapsed;
					if (sprite != null) sprite.lightness(t > 0 ? 0.75f : 0.5f);
					if (t <= 0){ if (sprite != null) sprite.resetColor(); killAndErase(); }
				}
			});
			Heart25Fx.nudge(sprite, Random.Int(2) == 0 ? -1 : 1, 0);
		}
		for (Image i : trailImages.values()) i.killAndErase();
		trailImages.clear();
		trail.clear();
	}

	// ---------------- 사냥꾼 ----------------
	private boolean cornered(){
		Hero h = Dungeon.hero;
		if (h == null) return false;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null && Dungeon.level.distance(c, h.pos) > Dungeon.level.distance(pos, h.pos)) return false;
		}
		return true;
	}

	private Boolean actHuntress( final Hero hero ){
		//관통 혈시: 지난 턴 조준선
		if (aimPath.length > 0){
			final int[] path = aimPath;
			aimPath = new int[0];
			clearAim(false);
			for (int c : path){
				if (c == hero.pos){
					strike(hero, MazeTower.MH_PIERCE_MIN, MazeTower.MH_PIERCE_MAX, MazeTower.B25_TELEGRAPH_ARMOR, 1f);
					break;
				}
			}
			if (sprite != null && path.length > 0){
				final int last = path[path.length - 1];
				fireArrow(last, true, null);
			}
			Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1f, 0.8f);
			//쏠 때 2칸 뒤로 물러남
			for (int i = 0; i < MazeTower.MH_PIERCE_BACK; i++){
				int old = pos;
				if (getFurther(hero.pos)) { if (sprite != null) sprite.place(pos); }
				else break;
			}
			spend(TICK);
			return true;
		}
		pierceCd--;
		trapCd--;
		if (trapCd <= 0){
			trapCd = MazeTower.MH_TRAP_CD;
			placeTraps(hero);
			Heart25Fx.shake();
			spend(TICK);
			return true;
		}
		if (pierceCd <= 0 && sees(hero)){
			pierceCd = MazeTower.MH_PIERCE_CD;
			Ballistica b = new Ballistica(pos, hero.pos, Ballistica.STOP_SOLID);
			ArrayList<Integer> line = new ArrayList<>();
			for (int c : b.subPath(1, b.dist)){
				if (Dungeon.level.solid[c]) break;
				line.add(c);
			}
			int[] a = new int[line.size()];
			for (int i = 0; i < a.length; i++) a[i] = line.get(i);
			aimPath = a;
			showAim(line);
			if (sprite != null) sprite.turnTo(pos, hero.pos);
			Heart25Fx.shake();
			spend(TICK);
			return true;
		}
		//4칸 유지: 인접하면 물러나기만 하고, 구석에 몰리면 발톱
		int d = Dungeon.level.distance(pos, hero.pos);
		if (d < MazeTower.MH_KEEP){
			int old = pos;
			if (getFurther(hero.pos)){
				spend(1 / speed());
				return moveSprite(old, pos);
			}
			if (d > 1 || cornered()) return null; //구석: 일반 공격(발톱)으로
		}
		return null;
	}

	private void showAim( ArrayList<Integer> line ){
		clearAim(false);
		aimLine = new Group();
		if (line.isEmpty()) return;
		PointF a = sprite != null ? sprite.center() : DungeonTilemap.tileCenterToWorld(pos);
		PointF b = DungeonTilemap.tileCenterToWorld(line.get(line.size() - 1));
		float len = (float)Math.hypot(b.x - a.x, b.y - a.y);
		float ang = (float)Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x));
		for (float s = 4; s < len; s += 4){
			ColorBlock d = new ColorBlock(2, 1, 0xFFE0283C);
			d.origin.set(0, 0.5f);
			d.angle = ang;
			d.x = a.x + (b.x - a.x) * s / len;
			d.y = a.y + (b.y - a.y) * s / len;
			d.alpha(0.85f);
			aimLine.add(d);
			aimDots.add(d);
		}
		GameScene.groupEffect(aimLine);
	}

	//조준선 지우기. broken이면 토막 나 떨어지며 사라진다
	private void clearAim( boolean broken ){
		if (aimLine == null) return;
		final Group g = aimLine;
		final ArrayList<ColorBlock> dots = new ArrayList<>(aimDots);
		aimDots.clear();
		aimLine = null;
		if (!broken){ g.killAndErase(); return; }
		g.parent.add(new Gizmo(){
			float t = 0;
			@Override public void update(){
				t += Game.elapsed;
				for (int i = 0; i < dots.size(); i++){
					ColorBlock m = dots.get(i);
					m.y += Game.elapsed * 30f * (1 + (i % 3));
					m.alpha(Math.max(0, 0.85f - t * 2f));
				}
				if (t > 0.45f){ g.killAndErase(); killAndErase(); }
			}
		});
	}

	private void placeTraps( Hero hero ){
		ArrayList<Integer> cand = new ArrayList<>();
		HeartBossLevel lv = Dungeon.level instanceof HeartBossLevel ? (HeartBossLevel) Dungeon.level : null;
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (lv != null && !lv.inRoom(c)) continue;
			if (!Dungeon.level.passable[c] || Actor.findChar(c) != null || traps.containsKey(c)) continue;
			if (Dungeon.level.distance(c, pos) <= MazeTower.MH_TRAP_RANGE) cand.add(c);
		}
		for (int i = 0; i < MazeTower.MH_TRAP_COUNT && !cand.isEmpty(); i++){
			Integer c = Random.element(cand);
			cand.remove(c);
			traps.put(c, 0);
			drawTrap(c);
		}
		trapsRevealed = false;
		if (sprite != null) sprite.showStatus(0xE0283C, Messages.get(this, "traps"));
	}

	private void drawTrap( int cell ){
		if (trapImages.containsKey(cell)) return;
		//(2026-11-22) 붉은 바닥에서도 보이게: 뼈색 촉수 세 갈래 + 어두운 테두리 (sprites/maze_b7_vein_trap.png, 16×16)
		Image i = new Image("sprites/maze_b7_vein_trap.png");
		PointF p = DungeonTilemap.tileToWorld(cell);
		i.x = p.x; i.y = p.y;
		GameScene.effect(i);
		trapImages.put(cell, i);
		updateTrapAlpha(cell);
	}

	private void updateTrapAlpha( int cell ){
		Image i = trapImages.get(cell);
		if (i == null) return;
		int age = traps.containsKey(cell) ? traps.get(cell) : 0;
		float a = trapsRevealed ? 0.9f : Math.max(0f, 1f - age / (float)MazeTower.MH_TRAP_FADE);
		i.alpha(a);
	}

	private void updateTraps(){
		if (Dungeon.hero != null && Dungeon.hero.buff(MindVision.class) != null) revealTraps();
		Blob fire = Dungeon.level.blobs.get(Fire.class);
		for (Integer c : new ArrayList<>(traps.keySet())){
			traps.put(c, traps.get(c) + 1);
			if (fire != null && fire.volume > 0 && fire.cur[c] > 0){
				burnTrap(c);
				continue;
			}
			updateTrapAlpha(c);
		}
	}

	//불꽃이 핥고 덫이 시들어 재로 흩어짐
	private void burnTrap( int cell ){
		traps.remove(cell);
		Image i = trapImages.remove(cell);
		if (i != null) i.killAndErase();
		CellEmitter.get(cell).burst(FlameParticle.FACTORY, 6);
		CellEmitter.get(cell).burst(Speck.factory(Speck.SMOKE), 3);
	}

	//촉수가 허공을 움켜쥐고 시듦 (밟거나 던진 물건)
	private void snapTrap( int cell, boolean grabbed ){
		traps.remove(cell);
		Image i = trapImages.remove(cell);
		if (i != null){
			final Image im = i;
			im.alpha(1f);
			im.parent.add(new Gizmo(){
				float t = 0.4f;
				@Override public void update(){
					t -= Game.elapsed;
					im.scale.set(1f, 1f + (0.4f - t));
					im.alpha(Math.max(0, t / 0.4f));
					if (t <= 0){ im.killAndErase(); killAndErase(); }
				}
			});
		}
		CellEmitter.center(cell).burst(BloodParticle.BURST, grabbed ? 6 : 3);
	}

	public void revealTraps(){
		if (traps.isEmpty() || trapsRevealed) return;
		trapsRevealed = true;
		for (int c : traps.keySet()){
			updateTrapAlpha(c);
			CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 2);
		}
	}

	public void revealTrapsOn( ArrayList<Integer> cells ){
		boolean any = false;
		for (int c : cells) if (traps.containsKey(c)){ any = true; Image i = trapImages.get(c); if (i != null) i.alpha(0.9f); }
		if (any) for (int c : cells) if (traps.containsKey(c)) CellEmitter.center(c).burst(Speck.factory(Speck.LIGHT), 2);
	}

	// ---------------- 바깥에서 부르는 것 ----------------

	//영웅이 칸에 들어섬 (HeartBossLevel.occupyCell)
	public static void onOccupy( Char ch ){
		MimicHero m = find();
		if (m == null || ch != Dungeon.hero || !m.traps.containsKey(ch.pos)) return;
		Buff.prolong(ch, Roots.class, MazeTower.MH_TRAP_ROOT);
		m.snapTrap(ch.pos, true);
		Sample.INSTANCE.play(Assets.Sounds.TRAP);
	}

	//던진 물건이 떨어짐 (Item.onThrow): 덫이 미리 터진다
	public static void onItemLand( int cell ){
		MimicHero m = find();
		if (m != null && m.traps.containsKey(cell)) m.snapTrap(cell, false);
	}

	//탐색·마법 지도
	public static void onSearch(){
		MimicHero m = find();
		if (m != null) m.revealTraps();
	}

	//굴절광: 빛에 닿은 은신을 해제하고, 은신한 덫을 드러낸다
	public static void onPrismatic( ArrayList<Integer> cells ){
		MimicHero m = find();
		if (m == null) return;
		if (cells.contains(m.pos)) m.reveal();
		m.revealTrapsOn(cells);
	}

	//이식: 1턴 매혹 + 모으던 기술 취소
	public void onTransfusion(){
		cancelCharge();
	}

	//정화의 물약: 칼날의 독이 지워짐
	public void purify(){
		if (coatLeft > 0){
			coatLeft = 0;
			if (sprite != null) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "uncoat"));
		}
	}

	//차지형 기술 취소 (가시밭·관통 혈시 조준)
	public void cancelCharge(){
		if (thornCenter != -1){
			ArrayList<Integer> area = area3(thornCenter);
			thornCenter = -1;
			//경고 칸이 조각나 흩어짐 → 지팡이 구슬이 터지고 피가 흘러내림 → 1px 움찔
			for (int c : area) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 2);
			if (warn != null){ warn.fadeOut(); warn = null; }
			if (sprite != null){
				sprite.centerEmitter().burst(BloodParticle.BURST, 8);
				Heart25Fx.nudge(sprite, sprite.flipHorizontal ? 1 : -1, 0);
				sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "interrupted"));
			}
		}
		if (aimPath.length > 0){
			aimPath = new int[0];
			clearAim(true);
			if (sprite != null){
				Heart25Fx.nudge(sprite, sprite.flipHorizontal ? 1 : -1, 0);
				sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "interrupted"));
			}
		}
	}

	// ---------------- 공격 ----------------
	@Override
	protected boolean doAttack( Char enemy ) {
		if ((cls == MAGE || cls == HUNTRESS) && !Dungeon.level.adjacent(pos, enemy.pos)){
			if (sprite != null && sprite.visible){
				sprite.turnTo(pos, enemy.pos);
				final Char e = enemy;
				fireArrow(enemy.pos, cls == HUNTRESS, new Callback() {
					@Override
					public void call() {
						attack(e);
						spend(attackDelay());
						next();
					}
				});
				return false;
			}
			attack(enemy);
			spend(attackDelay());
			return true;
		}
		return super.doAttack(enemy);
	}

	@Override
	public void onAttackComplete() {
		if (cls == ROGUE && enemy != null){
			if (invisible > 0){
				//기습: 반드시 적중, 방어 30%, 은신이 풀림
				strike(enemy, MazeTower.MR_AMBUSH_MIN, MazeTower.MR_AMBUSH_MAX, MazeTower.B25_AMBUSH_ARMOR, 1f);
				reveal();
				MazeFx.toward(MazeFx.Fx.VITAL_SURPRISE, this, enemy);
				spend(attackDelay());
				next();
				return;
			}
			//이중 발톱: 1타 앞손(위→아래), 2타 뒷손(아래→위)
			for (int i = 0; i < MazeTower.MR_HITS && enemy.isAlive(); i++){
				attack(enemy);
				MazeFx.toward(MazeFx.Fx.FLURRY, this, enemy, i == 0 ? 0 : 2, i == 0 ? 1 : 3);
			}
			spend(attackDelay());
			next();
			return;
		}
		super.onAttackComplete();
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (cls == ROGUE && coatLeft > 0 && enemy == Dungeon.hero){
			//독바르기: 방어력을 40% 더 무시 (그만큼 피해를 돌려 줌) + 독 10씩 쌓임
			damage += Math.round(enemy.drRoll() * MazeTower.MR_POISON_ARMOR);
			Buff.affect(enemy, Poison.class).extend(MazeTower.MR_POISON_STACK);
		}
		if (enemy == Dungeon.hero && damage > 0) Heart25Fx.heroHit(enemy, pos);
		return damage;
	}

	//예고 공격·기습: 반드시 맞고, 방어력의 armor 비율만 적용
	private void strike( Char target, int min, int max, float armor, float multi ){
		if (!target.isAlive()) return;
		int dmg = Math.round(Random.NormalIntRange(min, max) * multi) - Math.round(target.drRoll() * armor);
		target.damage(Math.max(0, dmg), this);
		Heart25Fx.heroHit(target, pos);
		if (target == Dungeon.hero && !target.isAlive()) Dungeon.fail(this);
	}

	//핏빛 탄환(직선) / 화살(촉·대·깃)
	private void fireArrow( int target, boolean arrow, final Callback done ){
		if (sprite == null || sprite.parent == null){ if (done != null) done.call(); return; }
		MissileSprite m = (MissileSprite) sprite.parent.recycle(MissileSprite.class);
		m.reset(sprite, target, new Arrow.Shot(arrow ? 0 : 1), new Callback() {
			@Override
			public void call() {
				if (done != null) done.call();
			}
		});
		if (!arrow) m.hardlight(1f, 0.15f, 0.2f);
		else m.hardlight(1f, 0.6f, 0.6f);
	}

	private void showWarn( ArrayList<Integer> cells ){
		if (warn != null) warn.fadeOut();
		warn = Heart25Fx.CellWarn.show(cells, null, Heart25Fx.BLOOD, 0);
		warn.pulse = 9f;
	}

	private ArrayList<Integer> area3( int c ){
		ArrayList<Integer> a = new ArrayList<>();
		a.add(c);
		for (int n : PathFinder.NEIGHBOURS8){
			int x = c + n;
			if (x >= 0 && x < Dungeon.level.length() && !Dungeon.level.solid[x]) a.add(x);
		}
		return a;
	}

	private void clearFx(){
		if (warn != null){ warn.fadeOut(); warn = null; }
		clearAim(false);
		for (Image i : trapImages.values()) i.killAndErase();
		trapImages.clear();
		for (Image i : trailImages.values()) i.killAndErase();
		trailImages.clear();
	}

	// ===== 글 =====
	@Override
	public String name() {
		return Messages.get(this, "name_" + cls);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc");
	}

	// ===== 저장 =====
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("cls", cls);
		bundle.put("deer_hp", deerHP);
		bundle.put("kills", kills);
		bundle.put("used", used);
		bundle.put("summoned", summoned);
		bundle.put("leap_cd", leapCd);
		bundle.put("leap_target", leapTarget);
		bundle.put("raged", raged);
		bundle.put("thorn_cd", thornCd);
		bundle.put("blink_cd", blinkCd);
		bundle.put("stealth_cd", stealthCd);
		bundle.put("poison_cd", poisonCd);
		bundle.put("coat", coatLeft);
		bundle.put("pierce_cd", pierceCd);
		bundle.put("trap_cd", trapCd);
		int[] tc = new int[traps.size()], ta = new int[traps.size()];
		int i = 0;
		for (int c : traps.keySet()){ tc[i] = c; ta[i] = traps.get(c); i++; }
		bundle.put("trap_cells", tc);
		bundle.put("trap_ages", ta);
		bundle.put("traps_revealed", trapsRevealed);
		int[] tr = new int[trail.size()];
		i = 0;
		for (int c : trail) tr[i++] = c;
		bundle.put("trail", tr);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cls = bundle.getInt("cls");
		deerHP = bundle.getInt("deer_hp");
		kills = bundle.getInt("kills");
		used = bundle.getInt("used");
		summoned = bundle.getBoolean("summoned");
		leapCd = bundle.getInt("leap_cd");
		leapTarget = bundle.getInt("leap_target");
		raged = bundle.getBoolean("raged");
		thornCd = bundle.getInt("thorn_cd");
		blinkCd = bundle.getInt("blink_cd");
		stealthCd = bundle.getInt("stealth_cd");
		poisonCd = bundle.getInt("poison_cd");
		coatLeft = bundle.getInt("coat");
		pierceCd = bundle.getInt("pierce_cd");
		trapCd = bundle.getInt("trap_cd");
		int[] tc = bundle.getIntArray("trap_cells"), ta = bundle.getIntArray("trap_ages");
		traps.clear();
		if (tc != null) for (int i = 0; i < tc.length; i++) traps.put(tc[i], ta[i]);
		trapsRevealed = bundle.getBoolean("traps_revealed");
		trail.clear();
		int[] tr = bundle.getIntArray("trail");
		if (tr != null) for (int c : tr) trail.add(c);
		leapTarget = -1; //경고 상태는 저장하지 않는다 (불러오면 다시 시전)
	}

	@Override
	public CharSprite sprite() {
		CharSprite s = super.sprite();
		if (s instanceof MimicSprite) ((MimicSprite) s).setClass(cls);
		return s;
	}

	//불러온 뒤 덫·흔적 그림을 다시 그린다
	public void redrawAfterLoad(){
		for (int c : traps.keySet()) drawTrap(c);
		for (int c : trail) drawTrail(c);
	}

	// =====================================================================
	// 그림: 무기 든 의태 영웅 (maze_b7_mimic_<직업>.png, 36×24 한 장). 맨손 시체는 원작 영웅 그림(warrior.png 등)에 핏빛.
	// 소환: 살점 창이 공중에 누운 시체를 4번 꿰뚫고(누움 → 60° → 30° → 섬) 착지, 0.9초 맨손 → 무기 생성(손목 꿈틀 → 혈관이 자람 → 굳음 → 반짝).
	// 처치: 머리부터 발끝까지 살점이 되어 무너진다.
	// 무기 혈관은 심장 박동(48 bpm)에 맞춰 밝아진다.
	// =====================================================================
	public static class MimicSprite extends MobSprite {

		private static final String[] HERO = {Assets.Sprites.WARRIOR, Assets.Sprites.MAGE, Assets.Sprites.ROGUE, Assets.Sprites.HUNTRESS};
		private static final String[] ARMED = {"warrior", "mage", "rogue", "huntress"};
		private int cls = 0;
		private float beat = 0;
		private float cut = -1; //무너지는 중 (0 → 1, 위에서 아래로)
		private boolean cinematic = false;

		public MimicSprite(){
			super();
			setClass(0);
		}

		public void setClass( int c ){
			cls = c;
			texture("sprites/maze_b7_mimic_" + ARMED[c] + ".png");
			TextureFilm film = new TextureFilm(texture, 36, 24);
			idle = new MovieClip.Animation(1, true);
			idle.frames(film, 0);
			run = new MovieClip.Animation(10, true);
			run.frames(film, 0, 0);
			attack = new MovieClip.Animation(12, false);
			attack.frames(film, 0, 0, 0);
			zap = attack.clone();
			die = new MovieClip.Animation(1, false);
			die.frames(film, 0);
			play(idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof MimicHero){ setClass(((MimicHero) ch).cls); ((MimicHero) ch).redrawAfterLoad(); }
		}

		@Override
		public void attack( int cell ) {
			super.attack(cell);
			PointF a = DungeonTilemap.tileCenterToWorld(ch.pos), b = DungeonTilemap.tileCenterToWorld(cell);
			Heart25Fx.nudge(this, Math.signum(b.x - a.x), Math.signum(b.y - a.y));
		}

		@Override
		public void update() {
			super.update();
			beat += Game.elapsed;
			if (ch != null && ch.invisible > 0 && !cinematic) alpha(0f);
			else if (!cinematic && cut < 0 && am < 1f && ch != null && ch.invisible <= 0) alpha(1f);
			//혈관이 박동에 맞춰 밝아짐
			float period = 60f / MazeTower.B25_BPM;
			boolean on = (beat % period) < period * 3 / 16f;
			if (!cinematic && cut < 0 && ch != null && ch.invisible <= 0) brightness(on ? 1.15f : 1f);
			if (cut >= 0){
				//(2026-11-22, 참고 움짤 25F_8) 하얗게 번쩍(0.15초) → 머리부터 한 줄씩 붉은 살점 줄무늬가 되어 흩어짐
				flashLeft -= Game.elapsed;
				if (flashLeft > 0){ lightness(1f); return; }
				resetColor();
				cut = Math.min(1f, cut + Game.elapsed / 0.55f);
				RectF f = new TextureFilm(texture, 36, 24).get(0);
				float h = f.bottom - f.top;
				int px = (int)(cut * 24);
				while (stripes < px && parent != null){ stripe(f, stripes); stripes++; }
				frame(new RectF(f.left, f.top + h * cut, f.right, f.bottom));
				y = baseY + px;
				if (cut >= 1){ killAndErase(); }
			}
		}

		private float baseY;
		private float flashLeft = 0;
		private int stripes = 0;

		public void collapse(){
			if (parent == null) return;
			baseY = y;
			cut = 0;
			flashLeft = 0.15f;
			stripes = 0;
		}

		//한 줄(1px)을 붉은 살점 줄무늬로 떼어 내 옆으로 밀리며 흐려지게
		private void stripe( RectF f, int row ){
			final Image s = new Image(texture);
			float h = f.bottom - f.top;
			s.frame(new RectF(f.left, f.top + h * row / 24f, f.right, f.top + h * (row + 1) / 24f));
			s.flipHorizontal = flipHorizontal;
			s.hardlight(1f, 0.2f, 0.28f);
			s.x = x; s.y = baseY + row;
			final float drift = Random.Float(-6f, 6f);
			parent.add(s);
			parent.add(new Gizmo(){
				float t = 0;
				@Override public void update(){
					t += Game.elapsed;
					s.x += drift * Game.elapsed;
					s.y += 10 * Game.elapsed;
					s.alpha(Math.max(0, 1f - t / 0.45f));
					if (t >= 0.45f){ s.killAndErase(); killAndErase(); }
				}
			});
		}

		//(2026-11-22) 그림(36×24)에서 몸 가운데가 그림 가운데보다 4px 왼쪽이라, 몸이 칸 가운데에 오게 옮긴다 (좌우 반전이면 반대로)
		static final int BODY_SHIFT = 4;

		@Override
		public PointF worldToCamera( int cell ) {
			PointF p = super.worldToCamera(cell);
			p.x += flipHorizontal ? -BODY_SHIFT : BODY_SHIFT;
			return p;
		}

		@Override
		public void turnTo( int from, int to ) {
			boolean before = flipHorizontal;
			super.turnTo(from, to);
			if (before != flipHorizontal && !isMoving) x += flipHorizontal ? -2 * BODY_SHIFT : 2 * BODY_SHIFT;
		}

		@Override
		public void die() {
			//처치 연출은 collapse
		}

		@Override
		public void onComplete( MovieClip.Animation anim ) {
			if (anim == die) return;
			super.onComplete(anim);
		}

		//소환 연출 (2026-11-22, 참고 움짤 25F_3, 약 4.6초). 끝나면 done
		//시체 칸 바닥에 핏빛 고리 → 창백한 시체가 누운 채 떠오름 → 짐승이 위에서부터 살점 물줄기가 되어 시체로 흘러 들어감
		//(물줄기가 닿을 때마다 누움 → 60° → 30°, 혈관이 번짐) → 마지막 살점 창이 위에서 내리꽂혀 바로 서서 착지 → 0.9초 맨손 → 무기 생성
		public void summon( final Callback done ){
			cinematic = true;
			alpha(0f);
			final com.shatteredpixel.shatteredpixeldungeon.sprites.HeartBeastSprite deer = pendingDeer;
			pendingDeer = null;
			final Image corpse = new Image(HERO[cls]);
			corpse.frame(new TextureFilm(corpse.texture, 12, 15).get(0));
			corpse.origin.set(6, 7.5f);
			corpse.hardlight(0.82f, 0.82f, 0.88f);
			final float cx = x + width / 2f - 6 - BODY_SHIFT * (flipHorizontal ? -1 : 1), cy = y + height - 15;
			corpse.x = cx; corpse.y = cy + 4;
			corpse.angle = 90;
			corpse.alpha(0f);
			parent.add(corpse);
			Heart25Fx.ellipse(parent, cx + 6, cy + 14, 2, 10, 0.9f, 0xFFC0182A, 16);
			final MimicSprite self = this;
			final Group g = parent;
			parent.add(new Gizmo(){
				float t = 0;
				float streamT = 0;
				int step = 0;
				@Override
				public void update() {
					t += Game.elapsed;
					//떠오름 (0 ~ 0.9초)
					if (t < 0.9f){
						float k = t / 0.9f;
						corpse.alpha(k);
						corpse.y = cy + 4 - 10 * k;
						if (step == 0 && t > 0.45f){ step++; Heart25Fx.ellipse(g, cx + 6, cy + 14, 2, 12, 0.9f, 0xFFC0182A, 16); }
					}
					//짐승이 물줄기가 됨 (1.0 ~ 2.2초)
					if (step <= 1 && t >= 1.0f){
						step = 2;
						if (deer != null && deer.parent != null) deer.dissolve();
					}
					if (t >= 1.0f && t < 2.2f && deer != null && deer.parent != null){
						streamT += Game.elapsed;
						while (streamT > 0.02f){
							streamT -= 0.02f;
							float sx = deer.x + 6 + Random.Float(20), sy = deer.y + 4 + Random.Float(16);
							fleshBit(g, sx, sy, corpse.x + 6, corpse.y + 7);
						}
					}
					if (step == 2 && t >= 1.5f){ step++; corpse.angle = 60; corpse.hardlight(1f, 0.6f, 0.62f); Heart25Fx.nudge(corpse, -1, 0); }
					if (step == 3 && t >= 1.9f){ step++; corpse.angle = 30; corpse.hardlight(1f, 0.45f, 0.48f); Heart25Fx.nudge(corpse, -1, 0); }
					if (step == 4 && t >= 2.2f){ step++; if (deer != null) deer.killAndErase(); spear(corpse, 90, true); }
					//마지막 창: 바로 서서 착지
					if (step == 5 && t >= 2.35f){
						step++;
						corpse.angle = 0;
						corpse.y = cy;
						corpse.scale.set(1f, 0.87f);
						Heart25Fx.shake();
						Heart25Fx.ellipse(g, cx + 6, cy + 14, 3, 12, 0.4f, 0xFFF0C0C8, 14);
						com.watabou.noosa.particles.Emitter e = new com.watabou.noosa.particles.Emitter();
						e.pos(cx, cy + 13, 12, 2);
						g.add(e);
						e.burst(BloodParticle.BURST, 10);
					}
					if (step == 6 && t > 2.5f){ step++; corpse.scale.set(1f, 0.93f); }
					if (step == 7 && t > 2.6f){ step++; corpse.scale.set(1f, 1f); }
					//0.9초 맨손 대기 → 무기 생성 (손목 꿈틀 0.3 → 혈관이 자람 0.7 → 굳음 0.3)
					float wStart = 2.35f + 0.9f;
					if (t >= wStart){
						float k = Math.min(1f, (t - wStart) / 1.3f);
						if (t < wStart + 0.3f) corpse.x = cx + (Random.Int(3) - 1) * 0.5f;
						self.alpha(k);
						corpse.alpha(1f - k);
						if (k >= 1f){
							corpse.killAndErase();
							self.brightness(1.6f);
							Heart25Fx.shake();
							cinematic = false;
							killAndErase();
							done.call();
						}
					}
				}
			});
		}

		//짐승에서 시체로 날아가는 살점 조각 (위로 휘는 궤적)
		private static void fleshBit( final Group g, final float sx, final float sy, final float tx, final float ty ){
			final ColorBlock b = new ColorBlock(Random.Int(2) == 0 ? 2 : 1, 1, Random.Int(3) == 0 ? 0xFFFFA3C7 : 0xFFC0182A);
			b.x = sx; b.y = sy;
			g.add(b);
			final float life = Random.Float(0.35f, 0.5f), lift = Random.Float(8, 18);
			g.add(new Gizmo(){
				float t = 0;
				@Override public void update(){
					t += Game.elapsed;
					float k = Math.min(1f, t / life);
					b.x = sx + (tx - sx) * k;
					b.y = sy + (ty - sy) * k - (float)Math.sin(k * Math.PI) * lift;
					if (k >= 1f){ b.killAndErase(); killAndErase(); }
				}
			});
		}

		private void spear( final Image corpse, float dirDeg, boolean fromAbove ){
			final ColorBlock s = new ColorBlock(10, 1, 0xFFC0182A);
			s.origin.set(10, 0.5f);
			final PointF target = new PointF(corpse.x + 6, corpse.y + 7);
			final float rad = (float)Math.toRadians(fromAbove ? -90 : dirDeg);
			final PointF start = new PointF(target.x - (float)Math.cos(rad) * 30, target.y - (float)Math.sin(rad) * 30);
			if (fromAbove){ start.set(target.x, target.y - 30); }
			s.angle = (float)Math.toDegrees(Math.atan2(target.y - start.y, target.x - start.x));
			s.x = start.x; s.y = start.y;
			parent.add(s);
			parent.add(new Gizmo(){
				float t = 0;
				@Override public void update(){
					t += Game.elapsed;
					float k = Math.min(1f, t / 0.12f);
					s.x = start.x + (target.x - start.x) * k;
					s.y = start.y + (target.y - start.y) * k;
					if (k >= 1f){
						s.killAndErase();
						killAndErase();
						float dx = Math.signum(target.x - start.x), dy = Math.signum(target.y - start.y);
						Heart25Fx.nudge(corpse, dx, dy);
						com.watabou.noosa.particles.Emitter e = new com.watabou.noosa.particles.Emitter();
						e.pos(target.x - 2, target.y - 2, 4, 4);
						corpse.parent.add(e);
						e.burst(BloodParticle.BURST, 6);
					}
				}
			});
		}
	}
}
