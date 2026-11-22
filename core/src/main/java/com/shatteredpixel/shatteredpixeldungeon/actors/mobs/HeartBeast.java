/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfCorruption;
import com.shatteredpixel.shatteredpixeldungeon.levels.HeartBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeartBeastSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

//Maze Tower: 25층 최종보스 — 심장을 진 짐승 (지시서 claude_code_tasks_1122_boss25.md, 1122b_boss25_update.md가 덮어씀).
//  1페이즈(100~70%): 공격하지 않고 살점을 찾아가 3턴 동안 먹는다(최대 체력 5% 회복). 살점은 처음 2개, 6턴마다 1개, 최대 4개.
//    오염 살점을 먹으면 회복하지 않고 원작 상태이상에 걸린다(사슴이 상태이상에 걸리는 유일한 경우).
//  2페이즈: 살점이 모두 터지고 체력 100%. 받는 피해 90% 감소, 피해로는 현재 25% 구간선 아래로 못 내려간다.
//    4턴마다 돌진(경고 1턴). 시작 5턴 뒤 첫 의태, 사슴으로 돌아온 지 7턴 뒤 다음 의태(4직업 중복 없이). 의태 영웅을 처치하면 체력 -25%.
//  3페이즈(4번째 처치 뒤, 체력 10%): 무적·광폭화. 20턴 버티면 쓰러진다. 튕기는 돌진 / 심장 박동 파동 / 핏줄 바닥 / 살점 비.
//  모든 상태이상 면역(오염 살점만 예외). 원소 피해는 받지만 원소 반응 없음.
public class HeartBeast extends Mob implements com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.PhaseMarks {

	//체력바 페이즈 눈금: 1페이즈 70%, 2페이즈 10% 하나만(의태 처치로 깎이는 25% 지점에는 없음), 3페이즈는 남은 턴
	@Override public float[] phaseMarks(){
		if (phase == 1) return new float[]{ MazeTower.B25_PHASE2_HP };
		if (phase == 2) return new float[]{ MazeTower.B25_PHASE3_HP };
		return new float[0];
	}
	@Override public int turnsLeft(){ return phase == 3 ? Math.max(0, p3Left) : -1; }

	{
		spriteClass = HeartBeastSprite.class;
		HP = HT = MazeTower.B25_HP;
		EXP = MazeTower.B25_EXP;
		defenseSkill = MazeTower.B25_DEF;
		state = HUNTING;
		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
	}

	private int phase = 1;
	//1페이즈
	private int fleshTimer = MazeTower.B25_FLESH_INTERVAL;
	private int eatTarget = -1; //먹는 살점 칸
	private int eatLeft = 0;
	//2페이즈
	public int kills = 0;
	public int usedClasses = 0; //비트: 0 전사 1 마법사 2 도적 3 사냥꾼
	private int mimicTimer = MazeTower.B25_MIMIC_FIRST;
	private int chargeTimer = MazeTower.B25_CHARGE_INTERVAL;
	private int[] chargePath = new int[0]; //경고한 돌진 경로 (비어 있으면 경고 없음)
	private int stunLeft = 0;
	private boolean fresh = false; //의태에서 막 돌아왔다 (그림을 다시 만든다)
	//3페이즈
	private int p3Left = MazeTower.B25_P3_TURNS;
	private int bounceCd = MazeTower.B25_BOUNCE_CD;
	private int waveCd = MazeTower.B25_WAVE_CD;
	private ArrayList<Integer> waves = new ArrayList<>(); //퍼지는 파동의 반지름
	private HashSet<Integer> veins = new HashSet<>();
	private int[] rainCells = new int[0];
	private boolean dying = false;

	//연출 (저장하지 않음)
	private Heart25Fx.CellWarn warn;
	private Heart25Fx.CellWarn rainWarn;
	private final ArrayList<Image> veinImages = new ArrayList<>();
	private boolean busy = false;

	private static boolean allowDebuff = false;

	public static HeartBeast find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs) if (m instanceof HeartBeast && m.isAlive()) return (HeartBeast) m;
		return null;
	}

	public int phase(){ return phase; }
	public int p3Left(){ return p3Left; }

	private HeartBossLevel level(){
		return Dungeon.level instanceof HeartBossLevel ? (HeartBossLevel) Dungeon.level : null;
	}

	private HeartBeastSprite hsprite(){
		return sprite instanceof HeartBeastSprite ? (HeartBeastSprite) sprite : null;
	}

	// ===== 수치 =====
	@Override public int attackSkill( Char target ){ return MazeTower.B25_ACC; }
	@Override public int drRoll(){ return super.drRoll() + Random.NormalIntRange(0, MazeTower.B25_DR); }
	@Override public int damageRoll(){ return 0; }

	// ===== 면역: 모든 상태이상 (오염 살점만 예외) =====
	private static final HashMap<Class, Boolean> NEG = new HashMap<>();

	@Override
	public boolean isImmune( Class effect ) {
		if (allowDebuff) return false;
		if (Buff.class.isAssignableFrom(effect)){
			Boolean neg = NEG.get(effect);
			if (neg == null){
				Object b = Reflection.newInstance(effect);
				neg = b instanceof Buff && ((Buff) b).type == Buff.buffType.NEGATIVE;
				//주인공 특성이 거는 표식류는 상태이상이 아니다
				String n = effect.getName();
				if (n.contains("BloomBuffs$Mark") || n.contains("HunterEffects$Prey") || n.contains("HunterEffects$HawkSignal")) neg = false;
				NEG.put(effect, neg);
			}
			if (neg) return true;
		}
		return super.isImmune(effect);
	}

	@Override
	public boolean isInvulnerable( Class effect ) {
		return phase == 3 || dying || super.isInvulnerable(effect);
	}

	// ===== 피해 =====
	@Override
	public void damage( int dmg, Object src ) {
		if (phase == 3 || dying || !isAlive()) return;
		if (phase == 2){
			dmg = Math.round(dmg * MazeTower.B25_P2_TAKEN);
			int floor = bandFloor(kills);
			if (HP - dmg < floor) dmg = Math.max(0, HP - floor);
			if (dmg <= 0){
				if (sprite != null && src != null) sprite.showStatus(CharSprite.NEUTRAL, "0");
				return;
			}
		}
		if (phase == 1) dmg = Math.min(dmg, HP - 1); //1페이즈에서 한 번에 쓰러지지 않는다 (2페이즈로 넘어감)
		super.damage(dmg, src);
		if (phase == 1 && isAlive() && HP <= HT * MazeTower.B25_PHASE2_HP) enterPhase2();
	}

	//의태를 k번 처치한 사슴이 피해로 내려갈 수 있는 가장 낮은 체력
	public int bandFloor( int k ){
		if (k < MazeTower.B25_MIMIC_KILLS - 1){
			return (int)Math.ceil(HT * (1f - MazeTower.B25_BAND * (k + 1)));
		}
		return (int)Math.floor(HT * MazeTower.B25_PHASE3_HP) + 1;
	}

	@Override
	public void die( Object cause ) {
		if (!dying) return; //3페이즈를 버텨야만 쓰러진다
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof HeartFlesh) ((HeartFlesh) m).vanish(false);
		}
		clearFx();
		GameScene.bossSlain();
		if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) && Statistics.spawnersAlive == 4){
			Badges.validateBossChallengeCompleted();
		} else {
			Statistics.qualifiedForBossChallengeBadge = false;
		}
		Statistics.bossScores[4] += 5000 + 1250 * Statistics.spawnersAlive;
		Badges.validateTakingTheMick(cause);
		Dungeon.level.mazeBossPos = pos;
		Dungeon.level.unseal();
		super.die(cause);
		com.shatteredpixel.shatteredpixeldungeon.MazeTrial.onBossDefeated();
	}

	// ===== 시작 =====
	public void beginFight(){
		BossHealthBar.assignBoss(this);
		for (int i = 0; i < MazeTower.B25_FLESH_START; i++) spawnFlesh();
	}

	@Override
	public void notice() {
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss(this);
	}

	// ===== 행동 =====
	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) fieldOfView = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		if (busy) return false;
		if (!BossHealthBar.isAssigned() && level() != null && level().locked) BossHealthBar.assignBoss(this);
		if (fresh){
			fresh = false;
			if (hsprite() != null) hsprite().rebuild();
		}
		if (dying){ spend(TICK); return true; }
		switch (phase){
			case 1: return actPhase1();
			case 2: return actPhase2();
			default: return actPhase3();
		}
	}

	// ---------------- 1페이즈 ----------------
	private boolean actPhase1(){
		if (paralysed > 0){ spend(TICK); return true; }
		fleshTimer--;
		if (fleshTimer <= 0){
			fleshTimer = MazeTower.B25_FLESH_INTERVAL;
			spawnFlesh();
		}

		if (eatLeft > 0){
			HeartFlesh f = HeartFlesh.at(eatTarget);
			if (f == null){ eatLeft = 0; }
			else {
				eatLeft--;
				if (eatLeft <= 0) finishEat(f);
				spend(TICK);
				return true;
			}
		}

		HeartFlesh target = nearestFlesh();
		if (target == null){
			if (sprite != null && hsprite().mode() != HeartBeastSprite.Mode.IDLE) sprite.idle();
			spend(TICK);
			return true;
		}
		if (Dungeon.level.adjacent(pos, target.pos)){
			eatTarget = target.pos;
			eatLeft = MazeTower.B25_EAT_TURNS;
			target.eaten = true;
			if (hsprite() != null) hsprite().eat(target.pos);
			spend(TICK);
			return true;
		}
		int old = pos;
		if (getCloser(target.pos)){
			spend(1 / speed());
			return moveSprite(old, pos);
		}
		spend(TICK);
		return true;
	}

	private HeartFlesh nearestFlesh(){
		HeartFlesh best = null; int bd = Integer.MAX_VALUE;
		PathFinder.buildDistanceMap(pos, com.watabou.utils.BArray.not(Dungeon.level.solid, null));
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof HeartFlesh)) continue;
			int d = PathFinder.distance[m.pos];
			if (d < bd){ bd = d; best = (HeartFlesh) m; }
		}
		return best;
	}

	private void spawnFlesh(){
		HeartBossLevel lv = level();
		if (lv == null) return;
		int count = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof HeartFlesh) count++;
		if (count >= MazeTower.B25_FLESH_MAX) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (!lv.inRoom(c) || !Dungeon.level.passable[c] || Actor.findChar(c) != null) continue;
			if (Dungeon.level.distance(c, pos) <= MazeTower.B25_FLESH_GAP) continue;
			if (Dungeon.hero != null && Dungeon.level.distance(c, Dungeon.hero.pos) <= MazeTower.B25_FLESH_GAP) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		HeartFlesh f = new HeartFlesh();
		f.pos = Random.element(cells);
		GameScene.add(f);
		CellEmitter.center(f.pos).burst(BloodParticle.BURST, 6);
	}

	private void finishEat( HeartFlesh f ){
		eatLeft = 0;
		if (f.type == HeartFlesh.CLEAN){
			int heal = Math.round(HT * MazeTower.B25_EAT_HEAL);
			HP = Math.min(HT, HP + heal);
			if (sprite != null) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
		} else {
			applyContamination(f);
		}
		f.vanish(false);
		if (sprite != null) sprite.idle();
	}

	//오염 살점: 원작 코드의 효과·지속시간 그대로
	private void applyContamination( HeartFlesh f ){
		allowDebuff = true;
		try {
			switch (f.type){
				case HeartFlesh.POISON:
					//원작 독(슬픔이끼와 같은 층 비례 독)
					Buff.affect(this, Poison.class).set(5 + Math.round(2 * Dungeon.scalingDepth() / 3f));
					break;
				case HeartFlesh.PARALYSIS:
					Buff.prolong(this, Paralysis.class, Paralysis.DURATION); //원작 마비 가스
					break;
				case HeartFlesh.CHILL:
					Buff.prolong(this, Chill.class, Chill.DURATION); //원작 냉기
					break;
				case HeartFlesh.CORRODE:
					Buff.affect(this, Corrosion.class).set(f.corrodeLeft, Math.round(f.corrodeDmg)); //원작 부식 가스가 건 값
					break;
				case HeartFlesh.CORRUPT:
					WandOfCorruption.mazeDebuffBoss(this, f.corruptLvl); //원작 타락 마법봉이 보스에게 거는 디버프
					break;
			}
		} finally {
			allowDebuff = false;
		}
		if (sprite != null) sprite.showStatus(CharSprite.NEGATIVE, Messages.get(HeartFlesh.class, "contam_" + f.type));
	}

	private void enterPhase2(){
		phase = 2;
		eatLeft = 0;
		HP = HT;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof HeartFlesh) ((HeartFlesh) m).vanish(true);
		}
		mimicTimer = MazeTower.B25_MIMIC_FIRST;
		chargeTimer = MazeTower.B25_CHARGE_INTERVAL;
		state = HUNTING;
		if (hsprite() != null){
			hsprite().roar(new Callback() {
				@Override public void call() { if (sprite != null) sprite.idle(); }
			});
		}
		Heart25Fx.shake();
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.7f);
		GLog.n(Messages.get(this, "phase2"));
		BossHealthBar.assignBoss(this);
	}

	// ---------------- 2페이즈 (사슴) ----------------
	private boolean actPhase2(){
		Hero hero = Dungeon.hero;
		if (stunLeft > 0){
			stunLeft--;
			spend(TICK);
			return true;
		}
		if (chargePath.length > 0){
			return runCharge(chargePath, null, false);
		}
		mimicTimer--;
		chargeTimer--;
		if (mimicTimer <= 0){
			startMimic();
			return true;
		}
		if (chargeTimer <= 0 && hero != null && hero.isAlive() && fieldOfView[hero.pos]){
			chargeTimer = MazeTower.B25_CHARGE_INTERVAL;
			warnCharge(hero);
			spend(TICK);
			return true;
		}
		//평소: 영웅을 바라보며 거리를 둔다 (가까이 오면 비켜서고, 멀면 다가간다)
		if (hero != null){
			int d = Dungeon.level.distance(pos, hero.pos);
			int old = pos;
			boolean moved = false;
			if (d > 5) moved = getCloser(hero.pos);
			else if (d < 2) moved = getFurther(hero.pos);
			if (moved){
				spend(1 / speed());
				return moveSprite(old, pos);
			}
			if (sprite != null){
				hsprite().face(pos, hero.pos);
				if (hsprite().mode() != HeartBeastSprite.Mode.IDLE) sprite.idle();
			}
		}
		spend(TICK);
		return true;
	}

	private void warnCharge( Hero hero ){
		Ballistica b = new Ballistica(pos, hero.pos, Ballistica.STOP_SOLID);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : b.subPath(1, b.dist)){
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		chargePath = toArray(cells);
		if (hsprite() != null){
			hsprite().face(pos, hero.pos);
			hsprite().stomp();
		}
		Heart25Fx.shake(); //기술 시전
		showWarn(cells, null);
	}

	private void showWarn( ArrayList<Integer> cells, float[] alphas ){
		if (warn != null) warn.fadeOut();
		warn = Heart25Fx.CellWarn.show(cells, alphas, Heart25Fx.BLOOD, 0.035f);
	}

	//돌진 실행 (2페이즈: 영웅·벽에 닿으면 멈춤. 3페이즈: bounce 경로 이어 달리기)
	private boolean runCharge( final int[] path, final int[] after, final boolean rage ){
		chargePath = new int[0];
		final boolean animate = sprite != null && sprite.visible && sprite.parent != null;
		if (!animate){
			segment(path, after, rage, false, null);
			spend(TICK);
			return true;
		}
		busy = true;
		segment(path, after, rage, true, new Callback() {
			@Override
			public void call() {
				busy = false;
				spend(TICK);
				next();
			}
		});
		return false;
	}

	//돌진 한 구간: 영웅에 닿으면 적중·멈춤, 벽에 닿으면 (튕길 구간이 있으면) 튕겨서 다음 구간, 없으면 멈춤
	private void segment( final int[] path, final int[] after, final boolean rage, final boolean animate, final Callback finish ){
		final Hero hero = Dungeon.hero;
		final int start = pos;
		int stop = -1;
		boolean hitHero = false;
		for (int i = 0; i < path.length; i++){
			int c = path[i];
			Char ch = Actor.findChar(c);
			if (ch == hero){ hitHero = true; break; }
			if (ch != null && ch != this) break;
			if (Dungeon.level.solid[c]) break;
			stop = i;
		}
		final int end = stop >= 0 ? path[stop] : pos;
		final boolean hit = hitHero;
		final boolean wall = !hitHero && (stop == path.length - 1 || stop < 0
				|| Dungeon.level.solid[path[Math.min(path.length - 1, stop + 1)]]);
		final int hitCell = hitHero ? hero.pos : -1;
		final ArrayList<Integer> passed = new ArrayList<>();
		for (int i = 0; i <= stop; i++) passed.add(path[i]);
		if (end != pos) move(end, false);
		if (rage && phase == 3 && p3Left <= MazeTower.B25_P3_STAGE2) veins.addAll(passed);

		final Callback resolve = new Callback() {
			@Override
			public void call() {
				boolean bounced = finishCharge(start, end, hit, hitCell, wall, after != null, rage);
				if (bounced){
					segment(after, null, rage, animate, finish);
				} else if (finish != null){
					finish.call();
				}
			}
		};
		if (animate) animateRun(start, passed, resolve);
		else resolve.call();
	}

	private void animateRun( int from, final ArrayList<Integer> cells, final Callback done ){
		final HeartBeastSprite s = hsprite();
		final PointF startP = s.worldToCamera(from);
		final ArrayList<PointF> pts = new ArrayList<>();
		pts.add(startP);
		for (int c : cells) pts.add(s.worldToCamera(c));
		if (cells.isEmpty()){ done.call(); return; }
		PointF last = DungeonTilemap.tileCenterToWorld(cells.get(cells.size() - 1)), first = DungeonTilemap.tileCenterToWorld(from);
		s.run((float)Math.toDegrees(Math.atan2(last.y - first.y, last.x - first.x)));
		final float perCell = 0.045f;
		s.parent.add(new Gizmo(){
			float t = 0;
			int passedIdx = 0;
			@Override
			public void update() {
				t += Game.elapsed;
				float f = t / perCell;
				int i = (int)f;
				if (i >= pts.size() - 1){
					PointF p = pts.get(pts.size() - 1);
					s.x = p.x; s.y = p.y;
					killAndErase();
					if (warn != null){ for (int c : cells) warn.off(c); }
					s.faceSide();
					s.idle();
					done.call();
					return;
				}
				PointF a = pts.get(i), b = pts.get(i + 1);
				float k = f - i;
				s.x = a.x + (b.x - a.x) * k;
				s.y = a.y + (b.y - a.y) * k;
				while (passedIdx < i && passedIdx < cells.size()){
					if (warn != null) warn.off(cells.get(passedIdx));
					passedIdx++;
				}
			}
		});
	}

	//돌을 끝낸 뒤 처리. 튕겨서 다음 구간으로 이어 가면 true
	private boolean finishCharge( int from, int end, boolean hit, int hitCell, boolean wall, boolean canBounce, boolean rage ){
		Hero hero = Dungeon.hero;
		if (hit && hero != null && hero.isAlive()){
			int min = rage ? MazeTower.B25_BOUNCE_MIN : MazeTower.B25_CHARGE_MIN;
			int max = rage ? MazeTower.B25_BOUNCE_MAX : MazeTower.B25_CHARGE_MAX;
			int dmg = Random.NormalIntRange(min, max) - Math.round(hero.drRoll() * (rage ? MazeTower.B25_P3_ARMOR : MazeTower.B25_CHARGE_ARMOR));
			hero.damage(Math.max(0, dmg), this);
			Heart25Fx.heroHit(hero, pos);
			if (!rage) Heart25Fx.shake();
			if (hero.isAlive()){
				int dx = Integer.signum(hitCell % Dungeon.level.width() - pos % Dungeon.level.width());
				int dy = Integer.signum(hitCell / Dungeon.level.width() - pos / Dungeon.level.width());
				if (dx == 0 && dy == 0){ dx = Integer.signum(hitCell % Dungeon.level.width() - from % Dungeon.level.width()); dy = Integer.signum(hitCell / Dungeon.level.width() - from / Dungeon.level.width()); }
				int power = rage ? MazeTower.B25_BOUNCE_KNOCK : MazeTower.B25_CHARGE_KNOCK;
				int target = hero.pos + (dx + dy * Dungeon.level.width()) * power;
				target = Math.max(0, Math.min(Dungeon.level.length() - 1, target));
				Ballistica traj = new Ballistica(hero.pos, target, Ballistica.MAGIC_BOLT);
				WandOfBlastWave.throwChar(hero, traj, power, false, false, this);
				if (!rage) Buff.prolong(hero, Paralysis.class, MazeTower.B25_CHARGE_STUN);
			} else {
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "kill"));
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
			if (sprite != null) CellEmitter.center(hitCell).burst(Speck.factory(Speck.LIGHT), 4);
			if (warn != null){ warn.fadeOut(); warn = null; }
			return false;
		}
		if (wall && canBounce){
			//3페이즈 벽 충돌: 납작하게 눌림(1px 흔들림, 3페이즈에서 흔들리는 유일한 순간) → 튕겨 나와 새 방향으로
			Heart25Fx.shake();
			if (sprite != null && sprite.parent != null){
				sprite.scale.set(1.12f, 0.8f);
				sprite.parent.add(new Gizmo(){
					float t = 1 / 60f;
					@Override public void update(){ t -= Game.elapsed; if (t <= 0){ if (sprite != null) sprite.scale.set(1f); killAndErase(); } }
				});
			}
			wallDebris(end);
			return true;
		}
		if (wall){
			if (!rage) Heart25Fx.shake();
			wallDebris(end);
			if (phase == 2){
				stunLeft = (int)MazeTower.B25_WALL_STUN;
				if (sprite != null){
					Heart25Fx.nudge(sprite, 0, -1);
					sprite.centerEmitter().burst(Speck.factory(Speck.STAR), 4);
				}
			}
		}
		if (warn != null){ warn.fadeOut(); warn = null; }
		return false;
	}

	private void wallDebris( int cell ){
		CellEmitter.get(cell).burst(Speck.factory(Speck.ROCK), 4);
		CellEmitter.center(cell).burst(Speck.factory(Speck.LIGHT), 3);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 1.2f);
	}

	// ---------------- 의태 ----------------
	private void startMimic(){
		ArrayList<Integer> left = new ArrayList<>();
		for (int i = 0; i < 4; i++) if ((usedClasses & (1 << i)) == 0) left.add(i);
		if (left.isEmpty()) left.add(Random.Int(4));
		int cls = Random.element(left);
		usedClasses |= 1 << cls;

		MimicHero m = new MimicHero();
		m.setup(cls, HP, kills, usedClasses);
		m.pos = pos;
		clearFx();
		final int at = pos;
		if (hsprite() != null) hsprite().bleed(20);
		Dungeon.level.mobs.remove(this);
		Actor.remove(this);
		if (sprite != null) sprite.killAndErase();
		GameScene.add(m);
		Heart25Fx.shake();
		BossHealthBar.assignBoss(m, true);
		spend(TICK);
	}

	//의태 영웅이 쓰러졌다: 사슴으로 돌아온다 (체력 -25%). 4번째면 3페이즈
	public static void returnFrom( MimicHero m ){
		HeartBeast b = new HeartBeast();
		b.phase = 2;
		b.state = b.HUNTING;
		b.kills = m.kills + 1;
		b.usedClasses = m.used;
		b.HP = Math.max(1, m.deerHP - Math.round(b.HT * MazeTower.B25_BAND));
		if (b.kills < MazeTower.B25_MIMIC_KILLS) b.HP = Math.max(b.HP, b.bandFloor(b.kills));
		b.mimicTimer = MazeTower.B25_MIMIC_NEXT;
		b.chargeTimer = MazeTower.B25_CHARGE_INTERVAL;
		b.pos = m.pos;
		b.fresh = true;
		if (b.kills >= MazeTower.B25_MIMIC_KILLS){
			b.HP = Math.min(b.HP, Math.max(1, (int)Math.floor(b.HT * MazeTower.B25_PHASE3_HP)));
			b.phase = 3;
			b.p3Left = MazeTower.B25_P3_TURNS;
		}
		GameScene.add(b);
		BossHealthBar.assignBoss(b, true);
		if (b.phase == 3) b.enterPhase3();
	}

	// ---------------- 3페이즈 ----------------
	private void enterPhase3(){
		phase = 3;
		p3Left = MazeTower.B25_P3_TURNS;
		bounceCd = 1;
		waveCd = MazeTower.B25_WAVE_CD;
		if (hsprite() != null){
			hsprite().bpm = MazeTower.B25_BPM_RAGE;
			hsprite().strongGlitch = true;
			hsprite().roar(new Callback() {
				@Override public void call() { if (sprite != null) sprite.idle(); }
			});
		}
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 1f, 0.6f);
		GLog.n(Messages.get(this, "phase3"));
		BossHealthBar.bleed(true);
	}

	private int stage(){
		if (p3Left > MazeTower.B25_P3_STAGE2) return 1;
		if (p3Left > MazeTower.B25_P3_STAGE3) return 2;
		return 3;
	}

	private boolean actPhase3(){
		Hero hero = Dungeon.hero;
		if (hsprite() != null){ hsprite().bpm = MazeTower.B25_BPM_RAGE; hsprite().strongGlitch = true; }
		int st = stage();

		//살점 비: 지난 턴에 예고한 칸에 떨어진다
		if (rainCells.length > 0){
			for (int c : rainCells){
				rainFall(c);
				if (hero != null && hero.isAlive() && hero.pos == c){
					int dmg = Random.NormalIntRange(MazeTower.B25_RAIN_MIN, MazeTower.B25_RAIN_MAX) - Math.round(hero.drRoll() * MazeTower.B25_P3_ARMOR);
					hero.damage(Math.max(0, dmg), this);
					Heart25Fx.heroHit(hero, c - Dungeon.level.width());
					if (!hero.isAlive()) Dungeon.fail(this);
				}
			}
			rainCells = new int[0];
			if (rainWarn != null){ rainWarn.fadeOut(); rainWarn = null; }
		}

		//심장 박동 파동: 한 턴에 한 칸씩 퍼짐
		ArrayList<Integer> nextWaves = new ArrayList<>();
		for (int r : waves){
			int nr = r + 1;
			if (nr > Math.max(MazeTower.B25_ROOM_W, MazeTower.B25_ROOM_H)) continue;
			nextWaves.add(nr);
			showRing(nr);
			if (hero != null && hero.isAlive() && Dungeon.level.distance(pos, hero.pos) == nr){
				int dmg = Random.NormalIntRange(MazeTower.B25_WAVE_MIN, MazeTower.B25_WAVE_MAX) - Math.round(hero.drRoll() * MazeTower.B25_P3_ARMOR);
				hero.damage(Math.max(0, dmg), this);
				Heart25Fx.heroHit(hero, pos);
				if (!hero.isAlive()) Dungeon.fail(this);
			}
		}
		waves = nextWaves;

		//핏줄 바닥
		if (hero != null && hero.isAlive() && veins.contains(hero.pos)){
			hero.damage(MazeTower.B25_VEIN_DMG, this);
			Buff.prolong(hero, Cripple.class, MazeTower.B25_VEIN_SLOW);
			if (!hero.isAlive()) Dungeon.fail(this);
		}
		drawVeins();

		p3Left--;
		if (p3Left <= 0){
			startDeath();
			spend(TICK);
			return true;
		}

		//튕기는 돌진
		if (chargePath.length > 0){
			int[] seg2 = bounceAfter;
			bounceAfter = null;
			return runCharge(chargePath, seg2, true);
		}
		bounceCd--;
		boolean acted = false;
		if (bounceCd <= 0 && hero != null){
			bounceCd = MazeTower.B25_BOUNCE_CD;
			warnBounce(hero);
			acted = true;
		}
		if (st >= 2){
			waveCd--;
			if (waveCd <= 0){
				waveCd = MazeTower.B25_WAVE_CD;
				waves.add(0);
				if (hsprite() != null) hsprite().stomp();
				if (sprite != null && sprite.parent != null) sprite.parent.add(new Heart25Fx.Vignette(1.6f));
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 0.6f);
			}
		}
		if (st >= 3 && hero != null) warnRain(hero);
		if (!acted && hero != null && sprite != null && chargePath.length == 0){
			hsprite().face(pos, hero.pos);
		}
		spend(TICK);
		return true;
	}

	private int[] bounceAfter = null;

	//튕기는 돌진 경로: 영웅 방향에서 20~35° 비껴 벽까지 → 영웅 쪽으로 튕김(벽면과 25~65°) → 다음 벽에서 멈춤
	private void warnBounce( Hero hero ){
		int w = Dungeon.level.width();
		PointF me = new PointF(pos % w + 0.5f, pos / w + 0.5f);
		PointF hp = new PointF(hero.pos % w + 0.5f, hero.pos / w + 0.5f);
		double base = Math.atan2(hp.y - me.y, hp.x - me.x);
		double off = Math.toRadians(Random.Float(MazeTower.B25_BOUNCE_AIM_MIN, MazeTower.B25_BOUNCE_AIM_MAX)) * (Random.Int(2) == 0 ? -1 : 1);
		double a1 = base + off;
		ArrayList<Integer> seg1 = trace(me, Math.cos(a1), Math.sin(a1), -1);
		if (seg1.isEmpty()) return;
		int last = seg1.get(seg1.size() - 1);
		//어느 벽에 닿았나
		double dx = Math.cos(a1), dy = Math.sin(a1);
		int lx = last % w, ly = last / w;
		boolean wallX = Dungeon.level.solid[last + (int)Math.signum(dx)];
		boolean wallY = Dungeon.level.solid[last + (int)Math.signum(dy) * w];
		if (!wallX && !wallY){ wallX = Math.abs(dx) >= Math.abs(dy); wallY = !wallX; }
		//벽면 방향 u, 벽에서 나오는 방향 n
		double ux, uy, nx, ny;
		if (wallX && !wallY){ ux = 0; uy = 1; nx = -Math.signum(dx); ny = 0; }
		else if (wallY && !wallX){ ux = 1; uy = 0; nx = 0; ny = -Math.signum(dy); }
		else { ux = 0; uy = 1; nx = -Math.signum(dx); ny = 0; }
		PointF lp = new PointF(lx + 0.5f, ly + 0.5f);
		double tx = hp.x - lp.x, ty = hp.y - lp.y;
		double along = tx * ux + ty * uy, out = tx * nx + ty * ny;
		double deg = Math.toDegrees(Math.atan2(Math.max(0, out), Math.abs(along)));
		if (out <= 0) deg = MazeTower.B25_BOUNCE_WALL_MIN;
		deg = Math.max(MazeTower.B25_BOUNCE_WALL_MIN, Math.min(MazeTower.B25_BOUNCE_WALL_MAX, deg));
		double s = along == 0 ? (Random.Int(2) == 0 ? -1 : 1) : Math.signum(along);
		double rad = Math.toRadians(deg);
		double ndx = Math.cos(rad) * s * ux + Math.sin(rad) * nx;
		double ndy = Math.cos(rad) * s * uy + Math.sin(rad) * ny;
		ArrayList<Integer> seg2 = trace(lp, ndx, ndy, last);
		//확인용: 튕김 방향과 벽면 사이 각도 (25~65°)
		System.out.println("[Maze Tower] 25F bounce angle to wall = " + String.format("%.1f", deg));

		chargePath = toArray(seg1);
		bounceAfter = toArray(seg2);
		ArrayList<Integer> all = new ArrayList<>(seg1);
		float[] alphas = new float[seg1.size() + Math.min(seg2.size(), MazeTower.B25_BOUNCE_WARN_AFTER)];
		for (int i = 0; i < seg1.size(); i++) alphas[i] = 1f;
		for (int i = 0; i < Math.min(seg2.size(), MazeTower.B25_BOUNCE_WARN_AFTER); i++){
			all.add(seg2.get(i));
			alphas[seg1.size() + i] = MazeTower.B25_BOUNCE_WARN_ALPHA[i];
		}
		if (hsprite() != null){ hsprite().faceAngle((float)Math.toDegrees(a1)); hsprite().stomp(); }
		showWarn(all, alphas);
	}

	//칸 단위 직선 (8방향으로 끊지 않음): 시작점에서 방향으로 나아가며 들어가는 칸을 차례로, 벽 앞까지
	private ArrayList<Integer> trace( PointF from, double dx, double dy, int skip ){
		int w = Dungeon.level.width();
		ArrayList<Integer> out = new ArrayList<>();
		double len = Math.hypot(dx, dy);
		if (len == 0) return out;
		dx /= len; dy /= len;
		double x = from.x, y = from.y;
		int cur = (int)Math.floor(y) * w + (int)Math.floor(x);
		for (int step = 0; step < 400; step++){
			x += dx * 0.1; y += dy * 0.1;
			int c = (int)Math.floor(y) * w + (int)Math.floor(x);
			if (c == cur) continue;
			if (c < 0 || c >= Dungeon.level.length() || Dungeon.level.solid[c]) break;
			cur = c;
			if (c != skip) out.add(c);
		}
		return out;
	}

	private void showRing( int r ){
		HeartBossLevel lv = level();
		if (lv == null) return;
		ArrayList<Integer> ring = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (lv.inRoom(c) && Dungeon.level.distance(pos, c) == r) ring.add(c);
		}
		final Heart25Fx.CellWarn w = Heart25Fx.CellWarn.show(ring, null, 0xE0203A, 0);
		w.parent.add(new Gizmo(){
			float t = 0.35f;
			@Override public void update(){ t -= Game.elapsed; if (t <= 0){ w.fadeOut(); killAndErase(); } }
		});
	}

	private void warnRain( Hero hero ){
		HeartBossLevel lv = level();
		if (lv == null) return;
		ArrayList<Integer> cand = new ArrayList<>();
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (lv.inRoom(c) && !Dungeon.level.solid[c] && Dungeon.level.distance(c, hero.pos) <= 2 && c != pos) cand.add(c);
		}
		ArrayList<Integer> pick = new ArrayList<>();
		if (cand.contains(hero.pos)) pick.add(hero.pos);
		while (pick.size() < MazeTower.B25_RAIN_COUNT && !cand.isEmpty()){
			Integer c = Random.element(cand);
			cand.remove(c);
			if (!pick.contains(c)) pick.add(c);
		}
		rainCells = toArray(pick);
		for (int c : rainCells) rainShadow(c);
	}

	//살점 덩어리 모양 그림자 경고 (원형 표시 아님)
	private void rainShadow( int cell ){
		final Image s = new Image("sprites/maze_b7_flesh_contaminated.png");
		s.frame(0, 0, 16, 16);
		s.hardlight(0f, 0f, 0f);
		s.alpha(0.45f);
		PointF p = DungeonTilemap.tileToWorld(cell);
		s.x = p.x; s.y = p.y + 2;
		GameScene.effect(s);
		s.parent.add(new Gizmo(){
			float t = 0;
			@Override public void update(){
				t += Game.elapsed;
				if (t > 1.2f || rainCells.length == 0){ s.killAndErase(); killAndErase(); }
			}
		});
	}

	//살점이 떨어져 터지고 핏자국만 남는다 (장애물 없음)
	private void rainFall( final int cell ){
		final Image f = new Image("sprites/maze_b7_flesh_contaminated.png");
		f.frame(0, 0, 16, 16);
		final PointF p = DungeonTilemap.tileToWorld(cell);
		f.x = p.x; f.y = p.y - 40;
		GameScene.effect(f);
		f.parent.add(new Gizmo(){
			@Override public void update(){
				f.y += Game.elapsed * 400f;
				if (f.y >= p.y){
					f.killAndErase();
					killAndErase();
					CellEmitter.center(cell).burst(BloodParticle.BURST, 8);
					Image stain = new Image("sprites/maze_b7_flesh_fx.png");
					stain.frame(new TextureFilm(stain.texture, 24, 24).get(6 * 12 + 10));
					stain.x = p.x - 4; stain.y = p.y - 4;
					stain.alpha(0.8f);
					GameScene.effect(stain);
				}
			}
		});
	}

	private void drawVeins(){
		for (Image i : veinImages) i.killAndErase();
		veinImages.clear();
		for (int c : veins){
			Image v = new Image("sprites/maze_b7_flesh_fx.png");
			v.frame(new TextureFilm(v.texture, 24, 24).get(1 * 12 + 9));
			PointF p = DungeonTilemap.tileToWorld(c);
			v.x = p.x - 4; v.y = p.y - 4;
			v.hardlight(0.75f, 0.05f, 0.15f);
			v.alpha(0.7f);
			GameScene.effect(v);
			veinImages.add(v);
		}
	}

	// ---------------- 사망 '멈추는 심장' ----------------
	private void startDeath(){
		dying = true;
		clearFx();
		final HeartBeastSprite s = hsprite();
		if (s == null){ die(null); return; }
		s.strongGlitch = false;
		busy = true;
		s.parent.add(new Gizmo(){
			float t = 0;
			@Override public void update(){
				t += Game.elapsed;
				s.bpm = t < 1.5f ? MazeTower.B25_BPM_RAGE : t < 3f ? MazeTower.B25_BPM : 0;
				if (t >= 3f){
					killAndErase();
					s.deathSequence(new Callback() {
						@Override public void call() {
							s.petalBurst();
							busy = false;
							HeartBeast.this.die(null);
							next();
						}
					});
				}
			}
		});
	}

	private void clearFx(){
		if (warn != null){ warn.fadeOut(); warn = null; }
		if (rainWarn != null){ rainWarn.fadeOut(); rainWarn = null; }
		for (Image i : veinImages) i.killAndErase();
		veinImages.clear();
	}

	private static int[] toArray( ArrayList<Integer> l ){
		int[] a = new int[l.size()];
		for (int i = 0; i < a.length; i++) a[i] = l.get(i);
		return a;
	}

	// ===== 글 =====
	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String description() {
		return Messages.get(this, "desc_" + Math.min(3, phase));
	}

	// ===== 저장 =====
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("flesh_timer", fleshTimer);
		bundle.put("eat_target", eatTarget);
		bundle.put("eat_left", eatLeft);
		bundle.put("kills", kills);
		bundle.put("used", usedClasses);
		bundle.put("mimic_timer", mimicTimer);
		bundle.put("charge_timer", chargeTimer);
		bundle.put("stun", stunLeft);
		bundle.put("p3_left", p3Left);
		bundle.put("bounce_cd", bounceCd);
		bundle.put("wave_cd", waveCd);
		int[] w = new int[waves.size()];
		for (int i = 0; i < w.length; i++) w[i] = waves.get(i);
		bundle.put("waves", w);
		int[] v = new int[veins.size()];
		int i = 0;
		for (int c : veins) v[i++] = c;
		bundle.put("veins", v);
		bundle.put("dying", dying);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt("phase");
		fleshTimer = bundle.getInt("flesh_timer");
		eatTarget = bundle.getInt("eat_target");
		eatLeft = bundle.getInt("eat_left");
		kills = bundle.getInt("kills");
		usedClasses = bundle.getInt("used");
		mimicTimer = bundle.getInt("mimic_timer");
		chargeTimer = bundle.getInt("charge_timer");
		stunLeft = bundle.getInt("stun");
		p3Left = bundle.getInt("p3_left");
		bounceCd = bundle.getInt("bounce_cd");
		waveCd = bundle.getInt("wave_cd");
		waves.clear();
		for (int r : bundle.getIntArray("waves")) waves.add(r);
		veins.clear();
		for (int c : bundle.getIntArray("veins")) veins.add(c);
		dying = bundle.getBoolean("dying");
		if (phase >= 2) BossHealthBar.assignBoss(this);
		if (phase == 3) BossHealthBar.bleed(true);
	}

	@Override
	public CharSprite sprite() {
		CharSprite s = super.sprite();
		if (s instanceof HeartBeastSprite && phase == 3){
			((HeartBeastSprite) s).bpm = MazeTower.B25_BPM_RAGE;
			((HeartBeastSprite) s).strongGlitch = true;
		}
		return s;
	}
}
