/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//Maze Tower (O1·O2): 25층 심장 판막 (기획 docs/boss25_heart_plan.md). 방 가장자리 검은 뿌리 끝에 박힌 매듭 위의 판막, 그 안에 핵을 지녔던 수문장의 메아리가 갇혀 있다.
//종류: 0 수액(5층) / 1 등불(10층) / 2 어둠 = 망령 왕 판(20층) / 3 꽃잎 = 여제 판(20층).
//움직이지도 공격하지도 않는 고정 몹. 체력이 0이 되면 사라지지 않고 재로 부스러진 그루터기로 남는다(broken, 다시 닫기 O4를 위해).
//(O2, 결정) 그 보스를 이긴 방법으로 깬다:
//  수액 = 물리 피해(출처가 캐릭터)를 O_SAP_PHYS_REDUCE만큼 흘린다. 불이 붙어 타는 동안(원작 화상)은 다 들어간다 ((사용자) 횃불은 두지 않는다: 화염 물약·마법봉·불 장판 등 가진 불로).
//  등불 = 앞쪽 부채꼴 빛(O_LANTERN_RANGE칸)이 O_LANTERN_TURN턴마다 45°씩 돈다. 빛 안에서 친 공격은 튕겨 나고(피해 없음), 빛 밖(뒤)에서 쳐야 들어간다. (사용자) 주인공이 빛 안에 있으면 주인이 턴마다 낙뢰를 떨군다(TowerMaster).
//  어둠 = 둘레 O_DARK_RADIUS칸이 어둠이다(MasterBossLevel.updateFieldOfView). 어둠 속 판막에는 아무것도 닿지 않는다. (사용자) 어둠 속에 있으면 주인이 망령 왕처럼 기습한다(TowerMaster).
//         (사용자) 어둠 속 무작위 자리의 기억의 등불을 치거나 상태이상을 걸면 O_DARK_LIGHT_TURNS턴 밝아지고, 꺼지면 어둠 속 다른 자리에 새 등불이 생긴다.
//  꽃잎 = (사용자) 여제는 따로 풀 장치 없이 피하면서 때리는 보스였다: 꽃잎의 보호로 받는 피해 × O_PETAL_TAKEN(늘). 주인공이 둘레 O_PETAL_ZONE칸 안에 있으면
//         주인이 여제의 강화 마탄·전기비를 턴을 쓰지 않고 덤으로 메아리쳐 쓴다(TowerMaster.petalEcho). 피하면서 때려 깬다.
//깨면: 그 뿌리로 새잎빛이 흘러 줄기가 자라고(MasterBossLevel.startRootFlow), 줄기 둘레 새잎 땅이 한 겹 넓어진다. ((사용자) 따로 받는 것은 없다)
//그림: sprites/maze_b25_valves.png (16×16, 종류마다 7칸: 0~5 가만히(3~5는 숨 쉬어 밝음, 꽃잎은 12°씩 돎), 6 깨짐, 원본 docs/sprites/boss25/make_props25.ps1)
public class HeartValve extends Mob {

	public static final int SAP = 0, LANTERN = 1, DARK = 2, PETAL = 3;

	{
		spriteClass = ValveSprite.class;

		HP = HT = MazeTower.O_VALVE_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
		alignment = Alignment.NEUTRAL;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	public int kind = SAP;
	public boolean broken = false;

	//등불: 빛이 향하는 8방향(LanternCone), 도는 시계
	public int facing = -1;
	private int turnTimer = 0;
	private int sweepDir = 1; //(사용자) 등불이 방 안쪽 방향들 사이를 왕복하는 방향 (+1 시계 / -1 반시계)

	//(사용자) 등불은 벽 뒤를 비추지 않게, 판막 바로 옆 칸이 방 바닥인 방향들(가장 긴 이어진 부채)의 끝에서 끝까지 45°씩 왕복한다
	private int nextFacing(){
		boolean[] ok = new boolean[8];
		int n = 0;
		for (int d = 0; d < 8; d++){
			int c = pos + LanternCone.DIRS[d][0] + LanternCone.DIRS[d][1] * Dungeon.level.width();
			ok[d] = Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).isRoomFloor(c);
			if (ok[d]) n++;
		}
		if (n == 0 || n == 8) return (facing + 1) % 8; //둘러싸였거나 다 트였으면 원래처럼 돈다
		for (int tries = 0; tries < 2; tries++){
			int next = ((facing + sweepDir) % 8 + 8) % 8;
			if (ok[next]) return next;
			sweepDir = -sweepDir; //끝에 닿으면 되돌아간다
		}
		return facing;
	}
	private com.shatteredpixel.shatteredpixeldungeon.effects.LanternLight light;
	private final java.util.HashSet<Integer> litCells = new java.util.HashSet<>();


	@Override
	protected boolean act() {
		if (sprite != null){
			sprite.hideAlert();
			sprite.hideLost();
		}
		//(사용자) 수액 판막: 불이 붙은 뒤 7턴에 걸쳐 타 들어감
		if (!broken && igniteLeft > 0){
			int chunk = (int)Math.ceil(igniteDmgLeft / (float) igniteLeft);
			igniteDmgLeft -= chunk;
			igniteLeft--;
			//정해진 몫만 (원작 화상의 턴마다 피해는 판막에 들어가지 않는다)
			damage(chunk, IGNITE);
			if (sprite != null && Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle.FACTORY, 3);
		}
		if (!broken){
			if (kind == LANTERN){
				if (facing < 0) facing = LanternCone.octantToward(pos, MasterBossLevel.SAPLING);
				if (++turnTimer >= MazeTower.O_LANTERN_TURN){
					turnTimer = 0;
					facing = nextFacing(); //(사용자) 방 안쪽 방향들 사이만 왕복
				}
				updateLight();
			}
		} else if (Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).state() == MasterBossLevel.State.FIGHT){
			brokenFor++; //(O4) 깨진 뒤 센 턴: O_RECLOSE_DELAY가 되면 (다음에 수리할 판막이면) 일렁이기 시작
			if (shimmering() && (TowerMaster.find() == null || TowerMaster.find().repairPos == -1)) shimmerFor++;
		}
		spend(TICK);
		return true;
	}

	// ===== (O4) 다시 닫기 =====
	//(사용자) 깨진 판막의 그루터기는 다시 닫힐 때까지 계속 검게 일렁인다(ValveSprite). O_RECLOSE_DELAY턴 뒤 주인이 순간이동해 4턴 동안 수리하고(TowerMaster),
	//다 고치면 판막이 체력 O_RECLOSE_HP(50%)로 다시 서고 기믹(깨는 조건·근처 기술·메아리 기술)이 모두 돌아온다. 3막에는 없음(O6)
	public int brokenFor = 0;
	public int shimmerFor = 0; //(사용자) 일렁인 턴 (다음에 수리할 판막으로 일렁이는 동안만 셈)

	public boolean readyToClose(){
		return broken && !absorbed && shimmerFor >= MazeTower.O_RECLOSE_WARN; //(사용자) 일렁이기 시작해 O_RECLOSE_WARN턴을 다 채워야
	}

	//(사용자) 깨진 뒤 O_RECLOSE_DELAY턴이 지나면 검게 일렁이기 시작한다 (주인이 곧 닫으러 온다는 예고, O_RECLOSE_WARN턴 뒤)
	//(사용자) 깨진 판막이 여럿이어도 다음에 수리할 하나만 일렁인다: 주인이 가거나 고치는 중인 판막, 없으면 가장 먼저 깨진 판막
	public boolean shimmering(){
		if (!broken || absorbed || brokenFor < MazeTower.O_RECLOSE_DELAY) return false;
		TowerMaster tm = TowerMaster.find(); //(점검) 한 번만 부른다: 화면 쪽에서 두 번 부르는 사이 주인이 쓰러지면 둘째가 null
		if (tm != null && tm.stage >= 3) return false; //(O6) 3막에는 다시 닫기가 없다
		if (tm != null && tm.repairPos != -1) return tm.repairPos == pos;
		if (tm != null && tm.repairCd() > 0) return false; //(사용자) 마지막 수리 뒤 O_RECLOSE_GAP턴 동안은 아무 판막도 일렁이지 않는다
		if (!(Dungeon.level instanceof MasterBossLevel)) return false;
		HeartValve next = null;
		for (HeartValve mob : ((MasterBossLevel) Dungeon.level).valves()){ //(점검) 화면 그리기 쪽에서 불리므로 mobs를 돌지 않는다
			if (mob.broken && !mob.absorbed && (next == null || mob.brokenFor > next.brokenFor)) next = mob;
		}
		return next == this;
	}

	// ===== (O6) 3막: 주인이 살아 있던 판막을 흡수한다 =====
	//판막은 재가 되어 기믹(범위·빛·어둠·덤 기술)이 사라지고, 그 메아리 기술은 주인에게 끝까지 남아 더 세진다(MasterSkills, O_ABSORB_MUL). 다시 닫히지 않는다
	public boolean absorbed = false;

	public void absorb(){
		if (broken) return;
		absorbed = true;
		broken = true;
		if (sprite != null){ ((ValveSprite) sprite).setKind(kind, true); sprite.resetColor(); }
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(ShadowParticle.UP, 16);
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 3);
		}
		if (kind == LANTERN) updateLight();
	}

	public void restore(){
		if (!broken) return;
		broken = false;
		igniteLeft = 0; igniteDmgLeft = 0;
		brokenFor = 0;
		shimmerFor = 0;
		HP = Math.max(1, Math.round(HT * MazeTower.O_RECLOSE_HP));
		if (sprite != null){
			((ValveSprite) sprite).setKind(kind, false);
			sprite.resetColor();
			sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "closed"));
		}
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(ShadowParticle.UP, 14);
			Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.8f);
		}
		if (kind == LANTERN){ facing = -1; updateLight(); }
		if (Dungeon.level instanceof MasterBossLevel) ((MasterBossLevel) Dungeon.level).onValveRestored(this);
		com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshBoss(); //(점검) "판막의 보호" 아이콘을 바로 갱신
		Dungeon.observe();
		GameScene.updateFog();
	}

	// ===== 등불: 앞쪽 부채꼴 빛 =====

	//빛이 닿는 칸을 다시 계산해 그린다 (봉인 등롱과 같은 LanternCone·LanternLight)
	public void updateLight(){
		if (kind != LANTERN || Dungeon.level == null) return;
		if (facing < 0) facing = LanternCone.octantToward(pos, MasterBossLevel.SAPLING);
		litCells.clear();
		java.util.HashMap<Integer, Float> cells = new java.util.HashMap<>();
		if (!broken){
			boolean[] fov = new boolean[Dungeon.level.length()];
			Dungeon.level.updateFieldOfView(this, fov);
			LanternCone.mask(pos, facing, fov, MazeTower.H1_CONE_HALF, MazeTower.O_LANTERN_RANGE);
			for (int i = 0; i < fov.length; i++){
				if (!fov[i] || Dungeon.level.solid[i] || i == pos) continue;
				if (!MasterBossLevel.inZone(i, this)) continue; //(사용자) 빛도 판막 범위(둥근 5칸) 테두리 안까지만
				litCells.add(i);
				cells.put(i, MazeTower.H1_LIGHT_ALPHA);
			}
		}
		if (sprite == null) return;
		if (light == null || light.parent == null){
			if (broken) return;
			light = new com.shatteredpixel.shatteredpixeldungeon.effects.LanternLight();
			GameScene.groupOverFog(light);
		}
		if (broken){ light.fadeAway(); light = null; return; }
		light.set(cells, new java.util.HashSet<Integer>());
	}

	//빛 안의 칸인지 (자기 칸 제외)
	public boolean lit( int cell ){
		if (facing < 0) updateLight();
		return litCells.contains(cell);
	}

	// ===== 받는 피해 =====

	//(O2) 판막을 깨는 법. 피해를 고치거나 막는다
	@Override
	public void damage( int dmg, Object src ) {
		if (broken || !isAlive()) return;
		//(사용자) 불이 붙으면 정해진 60%만 타 들어간다: 원작 화상의 턴마다 피해는 판막에 들어가지 않는다 (둘이 겹쳐 한 번에 깨지던 문제)
		if (src instanceof Burning) return;
		boolean hero = src != IGNITE && (src instanceof Char || !(src instanceof Buff || src instanceof Blob)); //캐릭터가 친 것 (마법봉·폭발처럼 출처가 물건이어도 주인공이 한 것, 상태이상·장판은 아님)
		//(사용자) 모든 판막: 주인공이 둘레 O_VALVE_ZONE칸(둥글게) 밖에 있으면 무적이다 (멀리서 안전하게 쏘는 빈틈을 막음).
		//친 사람이 범위 밖이어도 막힌다. 화상·장판처럼 이어지는 피해도 주인공이 범위 밖이면 들어가지 않는다 (불을 붙여 두고 물러나 기다리는 것도 막음).
		//원작처럼 머리 위에 "무적"(isInvulnerable). 범위는 바닥에 판막 색의 옅은 테두리(MasterBossLevel.ValveZone)와 설명 창으로 알린다
		Char attacker = hero ? (src instanceof Char ? (Char) src : Dungeon.hero) : Dungeon.hero;
		if (outOfReach() || (attacker != null && !MasterBossLevel.inZone(attacker.pos, this))){
			if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "invulnerable"));
			return;
		}
		switch (kind){
			case SAP:
				//물리 피해를 흘린다. 타는 동안은 다 들어간다
				if (src instanceof Char && buff(Burning.class) == null){
					dmg = Math.round(dmg * (1f - MazeTower.O_SAP_PHYS_REDUCE));
					if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "slip"));
				}
				break;
			case LANTERN:
				//빛 안에서 친 공격은 튕겨 난다 (화상 같은 이어지는 피해는 그대로)
				if (hero){
					//attacker: 위에서 정한 친 사람
					if (attacker != null && lit(attacker.pos)){
						if (sprite != null && Dungeon.level.heroFOV[pos]){
							sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "deflect"));
							Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.8f, Random.Float(0.95f, 1.05f));
						}
						return;
					}
				}
				break;
			case DARK:
				//어둠 속 판막에는 아무것도 닿지 않는다 (기억의 등불이 켜져야 칠 수 있다)
				if (Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).darkActive()){
					if (sprite != null) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "shrouded"));
					return;
				}
				break;
			case PETAL:
				dmg = Math.round(dmg * MazeTower.O_PETAL_TAKEN);
				break;
		}
		super.damage(dmg, src);
	}

	@Override public boolean heroShouldInteract() { return false; }
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

	//수액 판막만 불이 붙는다 (원작 화상). 다른 상태이상은 받지 않는다
	//(사용자) 불이 새로 붙으면 O_SAP_IGNITE_TURNS(7)턴에 걸쳐 최대 체력의 O_SAP_IGNITE_DMG(60%)만큼 타 들어간다 (원작 화상의 턴마다 피해는 판막에 들어가지 않음, 다시 닫혀 체력이 50%인 판막은 불만 붙여도 깨짐).
	//다른 피해와 같이 주인공이 판막 범위 밖에 있는 턴에는 들어가지 않는다
	private int igniteLeft = 0, igniteDmgLeft = 0;
	//불이 타 들어가는 몫의 피해 출처 (캐릭터가 친 것이 아님: 물리 피해 감소 없음)
	private static final Object IGNITE = new Object();
	@Override
	public boolean add( Buff buff ) {
		if (kind == SAP && !broken && buff instanceof Burning){
			boolean fresh = buff(Burning.class) == null;
			boolean ok = super.add(buff);
			if (ok && fresh && Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).state() == MasterBossLevel.State.FIGHT){
				if (sprite != null && Dungeon.level.heroFOV[pos]){
					sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "ignite"));
					CellEmitter.get(pos).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle.FACTORY, 12);
					Sample.INSTANCE.play(Assets.Sounds.BURNING, 1f, 0.9f);
				}
				igniteLeft = MazeTower.O_SAP_IGNITE_TURNS;
				igniteDmgLeft = Math.round(HT * MazeTower.O_SAP_IGNITE_DMG);
			}
			return ok;
		}
		return false;
	}

	//깨진 그루터기는 다치지 않는다
	@Override
	public boolean isInvulnerable( Class effect ) {
		return broken || outOfReach() || super.isInvulnerable(effect);
	}

	//(사용자) 주인공이 판막 범위(O_VALVE_ZONE칸) 밖에 있으면 판막은 무적
	public boolean outOfReach(){
		return !broken && Dungeon.hero != null && !MasterBossLevel.inZone(Dungeon.hero.pos, this);
	}

	//체력이 0이 되면 사라지지 않고 깨진 그루터기로 남는다
	@Override
	public void die( Object cause ) {
		if (broken) return;
		broken = true;
		brokenFor = 0;
		shimmerFor = 0;
		HP = 1;
		Buff.detach(this, Burning.class);
		if (sprite != null){
			((ValveSprite) sprite).showBroken();
			sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "broke"));
		}
		if (Dungeon.level.heroFOV[pos]){
			CellEmitter.get(pos).burst(ShadowParticle.UP, 12);
			CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 4);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.8f);
			PixelScene.shake(1, 0.15f);
		}
		if (kind == LANTERN) updateLight(); //빛은 서서히 사라진다
		MasterBossLevel.startRootFlow(this); //(사용자) 새잎빛이 뿌리를 타고 줄기로 흘러가 줄기가 자란다
		GLog.p(Messages.get(this, "broke_log"));
		if (Dungeon.level instanceof MasterBossLevel) ((MasterBossLevel) Dungeon.level).onValveBroken(this);
		com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.refreshBoss(); //(점검) "판막의 보호" 아이콘을 바로 갱신
		Dungeon.observe(); //어둠 판막이면 어둠이 걷힌다
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + kind);
	}

	@Override
	public String description() {
		if (broken) return Messages.get(this, "desc_broken");
		String d = Messages.get(this, "desc_" + kind) + "\n\n" + Messages.get(this, "how_" + kind) + "\n\n" + Messages.get(this, "zone");
		if (kind == SAP && buff(Burning.class) != null) d += "\n\n" + Messages.get(this, "now_burning");
		return d + "\n\n" + Messages.get(this, "hp", HP, HT);
	}

	@Override
	public void destroy() {
		super.destroy();
		if (light != null){ light.fadeAway(); light = null; }
	}

	private static final String KIND = "maze_kind";
	private static final String BROKEN = "maze_broken";
	private static final String FACING = "maze_facing";
	private static final String TURN_TIMER = "maze_turn_timer";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( KIND, kind );
		bundle.put( BROKEN, broken );
		bundle.put( "maze_broken_for", brokenFor );
		bundle.put( "maze_shimmer_for", shimmerFor );
		bundle.put( FACING, facing );
		bundle.put( TURN_TIMER, turnTimer );
		bundle.put( "maze_sweep_dir", sweepDir );
		bundle.put( "maze_absorbed", absorbed );
		bundle.put( "maze_ignite", new int[]{ igniteLeft, igniteDmgLeft } );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		kind = bundle.getInt( KIND );
		broken = bundle.getBoolean( BROKEN );
		brokenFor = bundle.getInt( "maze_broken_for" );
		shimmerFor = bundle.getInt( "maze_shimmer_for" );
		facing = bundle.contains( FACING ) ? bundle.getInt( FACING ) : -1;
		turnTimer = bundle.getInt( TURN_TIMER );
		if (bundle.contains( "maze_sweep_dir" )) sweepDir = bundle.getInt( "maze_sweep_dir" );
		absorbed = bundle.getBoolean( "maze_absorbed" );
		if (bundle.contains( "maze_ignite" )){ int[] ig = bundle.getIntArray( "maze_ignite" ); igniteLeft = ig[0]; igniteDmgLeft = ig[1]; }
	}

	public static class ValveSprite extends MobSprite {

		private TextureFilm film;
		private int kind = -1;

		public ValveSprite(){
			super();
			texture("sprites/maze_b25_valves.png");
			film = new TextureFilm(texture, 16, 16);
			setKind(0, false);
		}

		void setKind( int k, boolean broken ){
			kind = k;
			int o = k * 7;
			idle = new Animation(6, true);
			if (broken) idle.frames(film, o + 6);
			else idle.frames(film, o, o + 1, o + 2, o + 3, o + 4, o + 5);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(20, false);
			die.frames(film, o + 6);
			play(idle);
		}

		public void showBroken(){
			setKind(Math.max(0, kind), true);
		}

		//(O4, 사용자) 깨진 그루터기는 다시 닫힐 때까지 검게 일렁인다 (어두워졌다 돌아오기 + 그림자 넋이 피어오름)
		private com.watabou.noosa.particles.Emitter dark;
		private float darkT = 0;

		@Override
		public void update() {
			super.update();
			boolean pulse = ch instanceof HeartValve && ((HeartValve) ch).shimmering()
					&& Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).state() == MasterBossLevel.State.FIGHT;
			if (pulse){
				darkT += com.watabou.noosa.Game.elapsed;
				float k = 0.35f + 0.25f * (float)Math.sin(darkT * 3f);
				tint(0.1f, 0f, 0.05f, k);
				if (dark == null && parent != null){
					dark = emitter();
					dark.pour(ShadowParticle.UP, 0.35f);
				}
				if (dark != null) dark.on = visible;
			} else if (dark != null){
				dark.on = false;
				dark = null;
				resetColor();
			}
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof HeartValve){
				setKind(((HeartValve) ch).kind, ((HeartValve) ch).broken);
				((HeartValve) ch).updateLight(); //등불: 불러온 뒤에도 빛을 다시 그린다
			}
		}

		//땅에 박힌 매듭이라 좌우로 뒤집지 않는다 (꽃잎이 도는 방향도 그대로)
		@Override
		public void turnTo( int from, int to ) { flipHorizontal = false; }
	}
}
