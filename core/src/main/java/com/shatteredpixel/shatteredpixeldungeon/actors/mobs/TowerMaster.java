/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import java.util.ArrayList;

//Maze Tower (O1): 25층 탑의 주인 (기획 docs/boss25_heart_plan.md). 원작 주인공이 아뮬렛을 들고 거슬러 올랐다가 타락해 탑의 새 주인이 된 자.
//가슴에 박힌 검은 옹이가 탑의 심장. 체력 1000(결정), 막 경계 70%·35%(O5·O6).
//(O1) 지금은 1막의 뼈대만: 줄기 곁에서 움직이지 않고, 옆 칸의 주인공은 지팡이로 바닥을 짚어 친다(임시 평타). 메아리·몸의 기술은 O3부터.
//그림: sprites/maze_b25_master.png (24×26 × 18, 오른쪽을 보는 모습, 원본 docs/sprites/boss25/make_master_anim.ps1)
public class TowerMaster extends Mob implements com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.PhaseMarks {

	//(사용자) 보스 체력 바 페이즈 눈금 — 2막(O_ACT2_AT)·3막(O_ACT3_AT)
	@Override public float[] phaseMarks(){ return new float[]{ MazeTower.O_ACT2_AT, MazeTower.O_ACT3_AT }; }
	@Override public int turnsLeft(){ return -1; }

	{
		spriteClass = MasterSprite.class;

		HP = HT = MazeTower.O_MASTER_HP;
		EXP = MazeTower.O_MASTER_EXP;
		defenseSkill = MazeTower.O_MASTER_DEF;
		maxLvl = -2;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);
		properties.add(Property.DEMONIC);
	}

	public void beginFight(){
		state = HUNTING;
		BossHealthBar.assignBoss(this);
		yell(Messages.get(this, "notice"));
	}

	@Override
	public void notice() {
		if (!BossHealthBar.isAssigned()) BossHealthBar.assignBoss(this);
	}

	@Override
	protected boolean act() {
		if (!BossHealthBar.isAssigned() && Dungeon.level.locked) BossHealthBar.assignBoss(this);
		if (Dungeon.level.locked && buff(ValveWard.class) == null) Buff.affect(this, ValveWard.class); //(사용자) 판막의 보호 아이콘
		MasterBossLevel l = Dungeon.level instanceof MasterBossLevel ? (MasterBossLevel) Dungeon.level : null;
		Hero h = Dungeon.hero;
		//(사용자) 꽃잎 판막 둘레의 덤 기술: 턴을 쓰지 않는다 (지난 턴에 경고한 것은 어디 있든 그대로 쏜다)
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && h != null && h.isAlive()) petalEcho(l, h);
		if (h != null && !h.isAlive()) { spend(TICK); return true; }
		//(O5) 체력이 O_ACT2_AT 아래로 내려가 멈춰 있으면 2막이 열린다 (그 턴은 연출만)
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && stage == 1 && HP <= act2Floor() && travelTo == -1) return startAct2(l);
		//(O6) 체력이 O_ACT3_AT 아래로 내려가 멈춰 있으면 3막이 열린다 (다시 닫는 중이어도 거두고)
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && stage == 2 && HP <= act3Floor()) return startAct3(l);
		//(사용자) 여제 판 머리 위 창의 남은 턴은 어떤 턴이든 센다 (던지는 것은 아래 spearTurn)
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && h != null && h.isAlive()) skills.spearTick(l);
		//(사용자) 열매를 먹은 뒤의 무방비는 언제나 1턴: 먹은 다음 주인공 차례 동안만 반드시 맞고, 주인은 한 번만 쉰다
		eatVulnerable = false;
		//(O6) 걸어 들어가 열매를 먹었으면(onEnterCell) 이번 턴이 먹는 턴: 아무것도 하지 않는다
		if (eating > 0){ eating = 0; if (sprite != null) sprite.idle(); spend(TICK); return true; }
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && stage >= 3 && h != null && h.isAlive()){
			fruitTurn(l, h);
			//(점검) 익는 순간 그 칸에 서 있어 이번 턴에 먹었으면 이번 턴이 먹는 턴 (다음 턴은 평소대로)
			if (eating > 0){ eating = 0; eatVulnerable = true; if (sprite != null) sprite.idle(); spend(TICK); return true; }
		}
		if (l != null && l.state() == MasterBossLevel.State.FIGHT) skills.tick();
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && (repairPos == -1 || returning) && repairCd > 0) repairCd--; //(사용자) 마지막 수리를 마친 때부터 셈
		//(O4) 다시 닫는 중이면 그것만 (끊을 수 없음, 주인이 기절해 있어도 센다)
		if (repairPos != -1 && l != null && h != null && h.isAlive()) return actRepair(l);
		//(O4) 깨진 지 O_RECLOSE_DELAY턴이 된 판막이 있으면 (예고한 기술·기습이 없을 때) 순간이동해 다시 닫기 시작
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && h != null && h.isAlive() && skills.pending == MasterSkills.NONE && ambushCell == -1 && startRepair(l)){
			spend(TICK);
			return true;
		}
		//(O3) 지난 턴에 예고한 기술은 그대로 터진다 (예고 = 실제, 주인이 기절해 있어도). 그 턴의 기술로 친다
		//(사용자) 경고가 여러 턴인 기술(수액 내려치기)은 다 셀 때까지 힘을 모은다 (다른 기술 없음)
		if (skills.pending != MasterSkills.NONE && h != null && h.isAlive() && skills.waiting()){
			spend(TICK);
			return true;
		}
		if (skills.pending != MasterSkills.NONE && h != null && h.isAlive()){
			boolean wait = skills.resolve();
			spend(TICK);
			return !wait;
		}
		//(O3, 사용자) 여제 판의 머리 위 창: 남은 턴을 세고, 0이 되면 던진다 (그 턴의 기술)
		//(O6) 뿌리 해일: 고리가 솟는 턴은 그 턴의 기술 (경고만 남은 턴에는 새 기술 없이 걷기·평타)
		if (stage >= 3 && l != null && l.state() == MasterBossLevel.State.FIGHT && h != null && h.isAlive()){
			int tide = skills.tideTurn();
			if (tide >= 0){ spend(TICK); return tide == 0; }
		}
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && h != null && h.isAlive() && skills.spearTurn(l)){
			spend(TICK);
			return !skills.spearWait; //(점검) 창이 닿을 때까지 차례를 기다림
		}
		if (l != null && l.state() == MasterBossLevel.State.FIGHT && h != null && h.isAlive() && paralysed <= 0){
			//한 턴에 기술 하나: 지난 턴에 노린 기습 > 새 기습 > 등불 빛 안의 낙뢰 > 새 기술(MasterSkills) > 원작 몬스터처럼 (옆 칸이면 평타)
			if (ambushCell != -1) return actAmbush();
			boolean hidden = h.invisible > 0; //은신 존중: 주인공을 노리는 기술은 쓰지 않는다
			if (l.heroInDark()){
				if (ambushCd > 0) ambushCd--;
				if (ambushCd <= 0 && !hidden && startAmbush(l)){
					spend(TICK);
					return true;
				}
			}
			HeartValve lv = l.valveOf(HeartValve.LANTERN);
			if (lv != null && !lv.broken && !hidden && lv.lit(h.pos)){
				thunder(h);
				spend(TICK);
				return true;
			}
			//(O6, 사용자) 열매가 익었거나 곧 익으면 싸움을 멈추고 먹으러 걸어간다
			int sf = seekFruit(l);
			if (sf != 0) return sf == 1;
			//(O3) 몸의 기술·메아리 기술 (예고)
			if (skills.tryStart(l, h)){
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	// ===== (O4, 사용자) 다시 닫기 =====
	//깨진 지 O_RECLOSE_DELAY턴이 된 판막(먼저 깨진 것부터 하나씩)으로 순간이동해 O_RECLOSE_TURNS턴 동안 다시 닫는다(지팡이를 앞에 꽂고 고개를 숙임).
	//끊을 수 없고 다른 기술은 쓰지 않는다(그동안 등이 비어 가장 큰 딜 구간). 다 닫으면 판막이 다시 서고(HeartValve.restore) 줄기 곁 제자리로 돌아온다.
	public int repairPos = -1;   //다시 닫는 판막의 칸
	private int repairLeft = 0;
	private int home = -1;        //돌아올 자리 (처음 나온 칸)
	private int repairCd = 0;     //(사용자) 하나를 다 닫은 뒤 다음 판막을 닫으러 가기까지 남은 턴 (O_RECLOSE_GAP)

	//(사용자) 거리에 따라 한 턴에 한 번씩 최대 O_HOP_MAX번(한 번에 약 O_HOP_LEN칸) 끊어서 순간이동해 판막 옆에 닿고, 돌아올 때도 같다
	private int travelTo = -1;    //지금 순간이동해 가는 곳 (-1이면 가는 중 아님)
	private int hopsLeft = 0;
	private boolean returning = false;

	//(사용자) 마지막 수리 뒤 다음 판막이 일렁이기 시작하기까지 남은 턴
	public int repairCd(){ return repairCd; }

	//판막 옆에서 지팡이를 꽂고 닫는 중인지 (가고 오는 순간이동 중은 아님)
	public boolean closing(){ return repairPos != -1 && travelTo == -1; }

	private static int hopsFor( int from, int to ){
		int d = Dungeon.level.distance(from, to);
		return Math.max(1, Math.min(MazeTower.O_HOP_MAX, (int)Math.ceil(d / (float) MazeTower.O_HOP_LEN)));
	}

	//남은 순간이동 수에 맞춰 목적지 쪽으로 한 번 (목적지와 같은 비율의 지점에 가장 가까운 빈 바닥)
	private void hop( MasterBossLevel l ){
		if (hopsLeft <= 0 || travelTo == -1) return;
		int w = l.width();
		int x = pos % w, y = pos / w, tx = travelTo % w, ty = travelTo / w;
		int want = hopsLeft <= 1 ? travelTo : Math.round(x + (tx - x) / (float) hopsLeft) + Math.round(y + (ty - y) / (float) hopsLeft) * w;
		int best = -1, bestD = 99;
		for (int c = 0; c < l.length(); c++){
			if (!l.isRoomFloor(c) || (Actor.findChar(c) != null && c != pos)) continue;
			int d = l.distance(c, want);
			if (d < bestD){ bestD = d; best = c; }
		}
		hopsLeft--;
		if (best == -1 || best == pos) return;
		leafTeleport(l, best);
		if (repairPos >= 0 && sprite != null) sprite.turnTo(pos, repairPos);
	}

	//(O5, 사용자) 순간이동은 모두 낙엽으로: 떠난 자리와 내려앉은 자리에서 검은 낙엽이 흩날리고, 내려앉은 칸 둘레에 검은 낙엽을 깐다(새잎 땅 위에는 안 깔림)
	void leafTeleport( MasterBossLevel l, int to ){
		l.leafBurst(pos, 10);
		if (l.heroFOV[pos] || l.heroFOV[to]){
			Sample.INSTANCE.play(Assets.Sounds.GRASS, 0.9f, 0.7f);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.35f, 0.8f);
		}
		if (sprite != null) sprite.interruptMotion();
		move(to, false);
		if (sprite != null){
			sprite.interruptMotion();
			sprite.place(to);
			if (sprite.parent != null){
				sprite.alpha(0);
				sprite.parent.add(new com.watabou.noosa.tweeners.AlphaTweener(sprite, 1, 0.4f));
			}
		}
		Dungeon.level.occupyCell(this);
		l.leafBurst(to, 14);
		for (int c = 0; c < l.length(); c++){
			if (l.distance(c, to) <= MazeTower.O_HOP_LEAVES) l.layLeaves(c);
		}
		Dungeon.observe();
		GameScene.updateFog();
	}

	// ===== (O5) 막 =====
	//1막 메아리 → 2막 걷는 나무 (O_ACT2_AT 아래에서 멈췄다가 다음 차례에 넘어감) → 3막 (O6)
	public int stage = 1;

	private int act2Floor(){ return (int)Math.ceil(HT * MazeTower.O_ACT2_AT); }

	//(사용자) 판막의 보호: 1·2막에 판막이 하나도 깨져 있지 않은 동안
	public boolean valveWarded(){
		return stage < 3 && Dungeon.level instanceof MasterBossLevel && ((MasterBossLevel) Dungeon.level).state() == MasterBossLevel.State.FIGHT && ((MasterBossLevel) Dungeon.level).brokenValves() == 0;
	}

	//보스 체력 바 밑 "판막의 보호" 아이콘 (보호가 있는 동안만 보임)
	public static class ValveWard extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.POSITIVE; announced = false; }
		@Override public boolean act(){ spend(TICK); return true; }
		@Override public int icon(){ return target instanceof TowerMaster && ((TowerMaster) target).valveWarded() ? com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.ARMOR : com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.55f, 0.35f, 0.45f); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round(MazeTower.O_VALVE_WARD * 100)); }
	}

	//(결정) 막 경계에서 한 번에 넘어가지 않고 멈춘다
	@Override
	public void damage( int dmg, Object src ) {
		//(O6, 사용자) 주인공이 열매를 먹은 동안 주인에게 주는 피해 증가
		if (dmg > 0 && src != this && Dungeon.hero != null && Dungeon.hero.buff(FruitBlessing.class) != null) dmg = Math.round(dmg * MazeTower.O_FRUIT_DMG);
		//(사용자) 1·2막에 판막이 하나도 깨져 있지 않으면 받는 피해 -70% (판막을 깨러 가게, "판막의 보호" 아이콘). 3막에는 없다
		if (dmg > 0 && valveWarded()) dmg = Math.round(dmg * (1f - MazeTower.O_VALVE_WARD));
		if (dmg > 0 && buff(MasterFruit.class) != null) dmg = Math.round(dmg * MazeTower.O_FRUIT_TAKEN); //(사용자) 세계수의 열매를 먹은 동안 받는 피해 감소
		if (stage == 1 && dmg > 0 && HP - dmg < act2Floor()) dmg = Math.max(0, HP - act2Floor());
		if (stage == 2 && dmg > 0 && HP - dmg < act3Floor()) dmg = Math.max(0, HP - act3Floor());
		super.damage(dmg, src);
	}

	//2막이 열린다: 턴이 멈추고 등에서 가지가 뻗는다(그림은 사용자 확인 뒤 O_MASTER2_ART), 둘레에 검은 낙엽이 흩날려 깔린다
	private boolean startAct2( MasterBossLevel l ){
		stage = 2;
		l.rustleCell = -1; //1막에 밟은 낙엽 소리는 잊는다
		skills.cancel();
		if (ambushCell != -1){ removeEyes(); ambushCell = -1; }
		yell(Messages.get(this, "act2"));
		GLog.w(Messages.get(this, "act2_log"));
		Sample.INSTANCE.play(Assets.Sounds.GRASS, 1f, 0.6f);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.7f, 0.7f);
		PixelScene.shake(1, 0.3f);
		for (int c = 0; c < l.length(); c++){
			if (l.distance(c, pos) <= 2){ l.leafBurst(c, 4); l.layLeaves(c); }
		}
		if (sprite != null){
			((MasterSprite) sprite).setStage(2, true);
			sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 12);
		}
		spend(TICK);
		if (sprite == null || sprite.parent == null) return true;
		//연출 동안 턴이 멈춘다
		GameScene.groupEffect(new com.watabou.noosa.Group(){
			float t = 0;
			@Override public void update(){
				super.update();
				t += Game.elapsed;
				if (t >= MazeTower.O_ACT2_PAUSE){ killAndErase(); skillDone(); }
			}
		});
		return false;
	}

	//판막 옆 빈 칸 (닿을 자리)
	private int besideValve( MasterBossLevel l, int valvePos ){
		int at = -1, bestD = 99;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = valvePos + n;
			if (c < 0 || c >= l.length() || !l.isRoomFloor(c) || (Actor.findChar(c) != null && c != pos)) continue;
			int d = l.distance(c, pos);
			if (d < bestD){ bestD = d; at = c; } //주인 쪽에서 가까운 옆 칸
		}
		return at;
	}

	private boolean startRepair( MasterBossLevel l ){
		HeartValve best = null;
		for (Mob mob : l.mobs){
			if (!(mob instanceof HeartValve)) continue;
			HeartValve v = (HeartValve) mob;
			if (v.readyToClose() && (best == null || v.brokenFor > best.brokenFor)) best = v;
		}
		if (best == null || repairCd > 0 || stage >= 3) return false; //(O6) 3막에는 다시 닫기가 없다
		int at = besideValve(l, best.pos);
		if (at == -1) return false; //자리가 비면 다음 턴에 다시 본다
		if (home == -1 || stage >= 2) home = pos; //2막에는 수리를 마치고 떠났던 자리로 돌아온다
		skills.cancelSpear();
		repairPos = best.pos;
		repairLeft = 0;
		returning = false;
		travelTo = at;
		hopsLeft = hopsFor(pos, at);
		GLog.w(Messages.get(this, "repair_start"));
		Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.6f, 0.8f);
		hop(l);
		if (hopsLeft <= 0) beginClosing();
		return true;
	}

	//판막 옆에 닿았다: 지팡이를 꽂고 다시 닫기 시작
	private void beginClosing(){
		travelTo = -1;
		repairLeft = MazeTower.O_RECLOSE_TURNS;
		if (sprite != null){
			sprite.turnTo(pos, repairPos);
			((MasterSprite) sprite).repairPose(true);
			sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE, Integer.toString(repairLeft));
		}
	}

	//다시 닫기의 한 턴: 가는 중이면 한 번 순간이동 / 닫는 중이면 남은 턴을 센다 / 다 닫으면 판막을 세우고 돌아가기 시작 / 돌아오는 중이면 한 번 순간이동
	private boolean actRepair( MasterBossLevel l ){
		if (travelTo != -1){
			//가는 길에 자리가 막혔으면 다시 고른다
			if (!returning && hopsLeft <= 1 && Actor.findChar(travelTo) != null && Actor.findChar(travelTo) != this){
				int at = besideValve(l, repairPos);
				if (at != -1) travelTo = at;
			}
			hop(l);
			if (hopsLeft <= 0){
				if (returning){
					travelTo = -1;
					returning = false;
					repairPos = -1;
					if (sprite != null) ((MasterSprite) sprite).repairPose(false);
				} else {
					beginClosing();
				}
			}
			spend(TICK);
			return true;
		}
		repairLeft--;
		Char c = Actor.findChar(repairPos);
		HeartValve v = c instanceof HeartValve ? (HeartValve) c : null;
		if (repairLeft > 0 && v != null && v.broken){
			if (sprite != null){
				((MasterSprite) sprite).repairPose(true);
				if (Dungeon.level.heroFOV[pos]) sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE, Integer.toString(repairLeft));
			}
			if (v.sprite != null && Dungeon.level.heroFOV[repairPos]) CellEmitter.get(repairPos).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 4);
			spend(TICK);
			return true;
		}
		if (v != null) v.restore();
		repairCd = MazeTower.O_RECLOSE_GAP;
		if (sprite != null) ((MasterSprite) sprite).repairPose(false);
		//줄기 곁 제자리로 끊어서 돌아간다 (이번 턴에 첫 번째)
		int back = home != -1 ? home : MasterBossLevel.MASTER_START;
		returning = true;
		travelTo = back;
		hopsLeft = hopsFor(pos, back);
		hop(l);
		if (hopsLeft <= 0){ travelTo = -1; returning = false; repairPos = -1; }
		spend(TICK);
		return true;
	}

	// ===== (O6) 3막 "심장" =====
	private int act3Floor(){ return (int)Math.ceil(HT * MazeTower.O_ACT3_AT); }

	//3막이 열린다: 턴이 멈추고, 다시 닫는 중이었으면 거두고, 줄기 곁으로 돌아와 그때 살아 있던 판막을 모두 흡수한다
	private boolean startAct3( MasterBossLevel l ){
		stage = 3;
		skills.cancel();
		skills.cancelSpear();
		skills.beginAct3();
		clearEchoFx();
		if (ambushCell != -1){ removeEyes(); ambushCell = -1; }
		if (repairPos != -1 || travelTo != -1){
			repairPos = -1; travelTo = -1; returning = false; hopsLeft = 0;
			if (sprite != null) ((MasterSprite) sprite).repairPose(false);
		}
		fruitRegrow = MazeTower.O_FRUIT_FIRST;
		yell(Messages.get(this, "act3"));
		GLog.w(Messages.get(this, "act3_log"));
		//줄기 곁(처음 나온 칸)으로
		int back = MasterBossLevel.MASTER_START;
		if (pos != back && Actor.findChar(back) == null) leafTeleport(l, back);
		//살아 있던 판막을 흡수: 판막에서 주인에게 그림자 넋이 빨려 들고 판막은 재가 된다
		boolean any = false;
		for (Mob mob : l.mobs.toArray(new Mob[0])){
			if (!(mob instanceof HeartValve)) continue;
			final HeartValve v = (HeartValve) mob;
			if (v.broken) continue;
			any = true;
			if (v.sprite != null && sprite != null && sprite.parent != null){
				com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.boltFromChar(sprite.parent, com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.SHADOW, v.sprite, pos, null);
			}
			v.absorb();
			GLog.n(Messages.get(this, "absorb", Messages.get(this, "valve_" + v.kind)));
		}
		if (!any) GLog.p(Messages.get(this, "absorb_none"));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.5f);
		Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.8f, 0.7f);
		PixelScene.shake(1, 0.4f);
		if (sprite != null){
			((MasterSprite) sprite).setStage(3, true);
			sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 20);
		}
		Dungeon.observe();
		GameScene.updateFog();
		spend(TICK);
		if (sprite == null || sprite.parent == null) return true;
		GameScene.groupEffect(new com.watabou.noosa.Group(){
			float t = 0;
			@Override public void update(){
				super.update();
				t += Game.elapsed;
				if (t >= MazeTower.O_ACT3_PAUSE){ killAndErase(); skillDone(); }
			}
		});
		return false;
	}

	// ----- (O6, 사용자) 세계수의 열매: 주인공과 주인이 다툰다 -----
	//(사용자) 새잎 땅 칸 중 무작위(없으면 줄기 바로 옆 칸)에 맺혀 O_FRUIT_RIPEN턴 동안 익고(안개 위에 보이고 보스 체력 바 밑에 남은 턴), 익으면 먼저 그 칸에 들어선 쪽이 먹는다.
	//주인공: 체력·마나 회복 + O_FRUIT_TURNS턴 동안 주인에게 주는 피해 증가·받는 피해 감소. 주인: 먹는 1턴은 무방비(반드시 맞음), 다음 기술 셋의 피해 1.5배.
	//주인은 열매가 익었거나 곧 익으면(남은 턴 <= 거리) 싸움을 멈추고 걸어간다. 먹은 뒤 O_FRUIT_REGROW턴 지나 다시 맺힌다
	public int fruitCell = -1;
	public int fruitLeft = 0;     //익기까지 남은 턴 (0이면 익음)
	private int fruitRegrow = 0;  //다음 열매가 맺히기까지
	public int fruitCharges = 0;  //주인이 먹고 남은 강해진 기술 수
	private int eating = 0;       //먹는 중 (그 턴은 무방비)
	private boolean eatVulnerable = false; //(사용자) 이번 턴에 먹어서, 다음 주인 턴까지 반드시 맞는다

	public boolean fruitRipe(){ return fruitCell != -1 && fruitLeft <= 0; }

	private void fruitTurn( MasterBossLevel l, Hero h ){
		if (fruitCell == -1){
			if (--fruitRegrow > 0) return;
			//(사용자) 새잎 땅 칸 중 무작위, 새잎 땅이 없으면 줄기 바로 옆 칸
			ArrayList<Integer> spots = new ArrayList<>();
			for (int c = 0; c < l.length(); c++){
				if (!l.isRoomFloor(c) || Actor.findChar(c) != null) continue;
				if (com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.at(l, c)) spots.add(c);
			}
			if (spots.isEmpty()){
				for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
					int c = MasterBossLevel.SAPLING + n;
					if (l.isRoomFloor(c) && Actor.findChar(c) == null) spots.add(c);
				}
			}
			if (spots.isEmpty()) return;
			fruitCell = Random.element(spots);
			fruitLeft = MazeTower.O_FRUIT_RIPEN;
			GLog.w(Messages.get(this, "fruit_start", MazeTower.O_FRUIT_RIPEN));
			Sample.INSTANCE.play(Assets.Sounds.GRASS, 1f, 0.6f);
			Buff.affect(this, FruitCountdown.class);
			showFruit();
			if (fruitFx != null) fruitFx.flare();
			return;
		}
		if (fruitLeft > 0 && --fruitLeft == 0){
			GLog.w(Messages.get(this, "fruit_ripe"));
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.7f, 1.3f);
			if (fruitFx != null) fruitFx.flare();
		}
		//익은 열매 칸에 서 있던 쪽이 먹는다
		if (fruitRipe()){
			if (h.pos == fruitCell) heroEats(h);
			else if (pos == fruitCell) masterEats();
		}
	}

	//칸에 들어섰을 때 (MasterBossLevel.occupyCell)
	public void onEnterCell( Char ch ){
		if (!fruitRipe() || ch.pos != fruitCell) return;
		if (ch == Dungeon.hero) heroEats(Dungeon.hero);
		else if (ch == this) masterEats();
	}

	private void heroEats( Hero h ){
		int at = fruitCell;
		fruitGone();
		int heal = Math.min(h.HT - h.HP, Math.round(h.HT * MazeTower.O_FRUIT_HEAL));
		h.HP += heal;
		if (h.sprite != null && heal > 0) h.sprite.showStatusWithIcon(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana mana = com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana.get(h);
		if (mana != null) mana.gain(MazeTower.O_FRUIT_MANA);
		Buff.prolong(h, FruitBlessing.class, MazeTower.O_FRUIT_TURNS);
		GLog.p(Messages.get(this, "fruit_hero"));
		Sample.INSTANCE.play(Assets.Sounds.DRINK, 1f, 1.2f);
		if (Dungeon.level.heroFOV[at]) CellEmitter.get(at).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle.LEVEL_SPECIFIC, 10);
	}

	private void masterEats(){
		int at = fruitCell;
		fruitGone();
		Buff.prolong(this, MasterFruit.class, MazeTower.O_FRUIT_TURNS); //(사용자) 주인공과 같은 힘: 주는 피해 증가·받는 피해 감소
		eating = 1;
		GLog.n(Messages.get(this, "fruit_master"));
		Sample.INSTANCE.play(Assets.Sounds.EAT, 1f, 0.7f);
		if (sprite != null){
			sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.NEGATIVE, Messages.get(this, "eating"));
			sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 10);
		}
		if (Dungeon.level.heroFOV[at]) CellEmitter.get(at).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 8);
	}

	private void fruitGone(){
		fruitCell = -1;
		fruitLeft = 0;
		fruitRegrow = MazeTower.O_FRUIT_REGROW;
		Buff.detach(this, FruitCountdown.class);
		if (fruitFx != null){ fruitFx.killAndErase(); fruitFx = null; }
	}

	//익었거나 곧 익으면(남은 턴 <= 거리) 걸어간다. 0 = 이번 턴을 쓰지 않음 / 1 = 썼음 / 2 = 썼고 걷는 그림을 기다림
	//(고침) 예전에는 칸만 옮기고 그림을 움직이지 않아, 주인이 제자리에서 멀리 있는 열매를 먹는 것처럼 보였다
	private int seekFruit( MasterBossLevel l ){
		if (stage < 3 || fruitCell == -1) return 0;
		if (pos == fruitCell) return !fruitRipe() && waitOnFruit() ? 1 : 0;
		if (fruitLeft > l.distance(pos, fruitCell)) return 0;
		int old = pos;
		if (!getCloser(fruitCell)) return 0;
		spend(1 / speed());
		return moveSprite(old, pos) ? 1 : 2;
	}

	//익기 전에 열매 칸에 닿았으면 그 자리에서 기다린다 (옆 칸의 주인공은 평타)
	private boolean waitOnFruit(){
		if (Dungeon.hero != null && Dungeon.level.adjacent(pos, Dungeon.hero.pos)) return false;
		spend(TICK);
		return true;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		if (eating > 0 || eatVulnerable) return 0; //(O6) 열매를 먹는 1턴은 반드시 맞는다
		return super.defenseSkill(enemy);
	}

	//세계수의 열매 그림 (안개 위): 익을수록 커지고, 익으면 숨 쉬듯 새잎빛으로 빛남. 그림이 정해질 때까지 원작 산딸기 그림을 새잎빛으로
	private FruitFx fruitFx;

	public void showFruit(){
		if (fruitFx != null){ fruitFx.killAndErase(); fruitFx = null; }
		if (fruitCell == -1 || sprite == null || sprite.parent == null) return;
		fruitFx = new FruitFx(this);
		GameScene.groupOverFog(fruitFx);
	}

	public static class FruitFx extends com.watabou.noosa.Group {
		private final TowerMaster tm;
		private final com.watabou.noosa.Image img;
		private com.watabou.noosa.TextureFilm film;
		private com.watabou.noosa.Halo halo;
		private float t = 0, flareT = 0;
		FruitFx( TowerMaster tm ){
			this.tm = tm;
			if (MazeTower.O_FRUIT_ART){
				//(사용자 확인 뒤) 세계수의 열매 sprites/maze_b25_fruit.png 16×16 × 6: 0~3 익어 감, 4·5 다 익음(새잎빛 테두리가 숨 쉼). 원본 docs/sprites/boss25/make_fruit25.ps1
				halo = new com.watabou.noosa.Halo(9, 0xC8F088, 0f);
				add(halo);
				img = new com.watabou.noosa.Image("sprites/maze_b25_fruit.png");
				film = new com.watabou.noosa.TextureFilm(img.texture, 16, 16);
				img.frame(film.get(0));
			} else {
				img = new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.BERRY, null);
			}
			add(img);
		}
		void flare(){ flareT = 0.5f; }
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (tm.fruitCell == -1){ killAndErase(); return; }
			float ripe = 1f - Math.max(0, tm.fruitLeft) / (float) MazeTower.O_FRUIT_RIPEN;
			float glow = tm.fruitRipe() ? 0.2f + 0.2f * (float)Math.sin(t * 4f) : 0f;
			if (flareT > 0){ flareT -= Game.elapsed; glow = Math.max(glow, flareT * 1.6f); }
			if (film != null){
				PointF tl = DungeonTilemap.tileToWorld(tm.fruitCell);
				int fr = tm.fruitRipe() ? ((int)(t * 2.5f) % 2 == 0 ? 4 : 5) : Math.min(3, (int)(ripe * 4));
				img.frame(film.get(fr));
				img.x = tl.x; img.y = tl.y;
				halo.point(tl.x + 8, tl.y + 6);
				halo.am = glow * 0.9f;
				return;
			}
			PointF p = DungeonTilemap.tileCenterToWorld(tm.fruitCell);
			float s = 0.55f + 0.45f * ripe;
			img.origin.set(img.width / 2f, img.height / 2f);
			img.scale.set(s);
			img.x = p.x - img.width / 2f;
			img.y = p.y - img.height / 2f - 2 + (float)Math.sin(t * 2.2f);
			img.resetColor();
			img.hardlight(0.45f + glow * 0.5f, 0.85f + glow * 0.15f, 0.3f + glow * 0.5f);
		}
	}

	//보스 체력 바 밑 열매 아이콘: 익기까지 남은 턴 (익으면 "!")
	public static class FruitCountdown extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{ type = buffType.NEUTRAL; announced = false; }
		private TowerMaster tm(){ return target instanceof TowerMaster ? (TowerMaster) target : null; }
		@Override public boolean act(){ TowerMaster m = tm(); if (m == null || m.fruitCell == -1) detach(); spend(TICK); return true; }
		@Override public int icon(){ TowerMaster m = tm(); return m == null || m.fruitCell == -1 ? com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE : com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.WELL_FED; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.6f, 0.95f, 0.4f); }
		@Override public float iconFadePercent(){ TowerMaster m = tm(); return m == null ? 0 : Math.max(0, Math.min(1, m.fruitLeft / (float) MazeTower.O_FRUIT_RIPEN)); }
		@Override public String iconTextDisplay(){ TowerMaster m = tm(); return m == null ? "" : m.fruitLeft > 0 ? Integer.toString(m.fruitLeft) : "!"; }
		@Override public String desc(){ TowerMaster m = tm(); return Messages.get(this, "desc", m == null ? 0 : Math.max(0, m.fruitLeft)); }
	}

	//(사용자) 주인이 열매를 먹은 뒤: 주인공과 같은 힘 (기술·평타 피해 증가 MasterSkills.setMul·damageRoll, 받는 피해 감소 damage), 보스 체력 바 밑 아이콘
	public static class MasterFruit extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {
		{ type = buffType.POSITIVE; }
		@Override public int icon(){ return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.HERB_HEALING; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.55f, 0.9f, 0.4f); }
		@Override public float iconFadePercent(){ return Math.max(0, (MazeTower.O_FRUIT_TURNS - visualcooldown()) / MazeTower.O_FRUIT_TURNS); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round((MazeTower.O_FRUIT_DMG - 1) * 100), Math.round((1 - MazeTower.O_FRUIT_TAKEN) * 100), dispTurns()); }
	}

	//주인공이 열매를 먹은 뒤: 주인에게 주는 피해 증가·받는 피해 감소 (Hero.damage, TowerMaster.damage)
	public static class FruitBlessing extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff {
		{ type = buffType.POSITIVE; }
		@Override public int icon(){ return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.HERB_HEALING; }
		@Override public void tintIcon( com.watabou.noosa.Image icon ){ icon.hardlight(0.55f, 0.9f, 0.4f); }
		@Override public float iconFadePercent(){ return Math.max(0, (MazeTower.O_FRUIT_TURNS - visualcooldown()) / MazeTower.O_FRUIT_TURNS); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round((MazeTower.O_FRUIT_DMG - 1) * 100), Math.round((1 - MazeTower.O_FRUIT_TAKEN) * 100), dispTurns()); }
	}

	//(O3) 1막 기술 (MasterSkills)
	public final MasterSkills skills = new MasterSkills(this);

	//기술 연출(사슬)이 끝나면 주인의 차례를 넘긴다
	void skillDone(){ next(); }

	// ===== (O2, 사용자) 꽃잎 판막(여제 판) 둘레 O_PETAL_ZONE칸 안에 있으면: 여제의 강화 마탄·전기비를 덤으로 =====
	//(결정) "한 턴에 한 기술"의 예외: 턴을 쓰지 않고 원래 패턴과 함께 쓴다. 대신 20층과 같은 경고 1턴·같은 계산·같은 그림, 쿨타임(마탄 3·전기비 7),
	//새 경고는 한 턴에 하나. 강화 마탄은 꽃잎 판막에서 주인공 쪽으로 방 끝(벽)까지 줄기도 꿰뚫는다. 경고한 것은 다음 주인 턴에 주인공이 어디 있든 그대로 터진다.
	private int petalBoltCd = 1, petalRainCd = MazeTower.EM_RAIN_INTERVAL / 2;
	private int[] petalBolt = null;   //경고한 강화 마탄 궤적
	private int petalBoltFrom = -1;
	private int[] petalRain = null;   //경고한 전기비 칸
	private com.shatteredpixel.shatteredpixeldungeon.effects.BoltWarnLine boltLine;
	private com.shatteredpixel.shatteredpixeldungeon.effects.CellWarnFx rainLook;

	private void petalEcho( MasterBossLevel l, Hero h ){
		if (petalBolt != null) firePetalBolt();
		if (petalRain != null) firePetalRain();
		if (!h.isAlive()) return;
		HeartValve pv = l.valveOf(HeartValve.PETAL);
		if (pv == null || pv.broken) return;
		petalBoltCd--; petalRainCd--;
		if (paralysed > 0 || h.invisible > 0 || !MasterBossLevel.inRadius(h.pos, pv.pos, MazeTower.O_PETAL_ZONE)) return;
		if (petalBoltCd <= 0 && petalBolt == null){
			warnPetalBolt(l, pv);
			petalBoltCd = MazeTower.EM_BOLT_INTERVAL;
		} else if (petalRainCd <= 0 && petalRain == null){
			warnPetalRain(l);
			petalRainCd = MazeTower.EM_RAIN_INTERVAL;
		}
	}

	private void warnPetalBolt( MasterBossLevel l, HeartValve pv ){
		com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica b = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pv.pos, Dungeon.hero.pos, 0);
		ArrayList<Integer> path = new ArrayList<>();
		for (int c : b.path){
			if (c == pv.pos) continue;
			if (l.map[c] == com.shatteredpixel.shatteredpixeldungeon.levels.Terrain.WALL) break; //방 끝(벽)까지
			path.add(c);
		}
		if (path.isEmpty()) return;
		petalBoltFrom = pv.pos;
		petalBolt = new int[path.size()];
		for (int i = 0; i < path.size(); i++) petalBolt[i] = path.get(i);
		if (pv.sprite != null) pv.sprite.flash();
		showBoltWarn();
	}

	private void showBoltWarn(){
		if (boltLine != null){ boltLine.killAndErase(); boltLine = null; }
		if (petalBolt == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		PointF a = DungeonTilemap.tileCenterToWorld(petalBoltFrom);
		PointF b = DungeonTilemap.tileCenterToWorld(petalBolt[petalBolt.length - 1]);
		float dx = b.x - a.x, dy = b.y - a.y, len = (float)Math.sqrt(dx*dx + dy*dy);
		if (len > 0) b = new PointF(b.x + dx / len * DungeonTilemap.SIZE / 2f, b.y + dy / len * DungeonTilemap.SIZE / 2f);
		boltLine = new com.shatteredpixel.shatteredpixeldungeon.effects.BoltWarnLine(a, b);
		Dungeon.hero.sprite.parent.addToBack(boltLine);
	}

	private void firePetalBolt(){
		if (boltLine != null){ boltLine.killAndErase(); boltLine = null; }
		int[] path = petalBolt;
		petalBolt = null;
		int end = path[path.length - 1];
		com.watabou.noosa.MovieClip m = com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.clip(com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_BOLT, 12, true);
		PointF a = DungeonTilemap.tileCenterToWorld(petalBoltFrom), b = DungeonTilemap.tileCenterToWorld(end);
		m.origin.set(8, 8);
		m.x = a.x - 8; m.y = a.y - 8;
		m.angle = (float)(Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI);
		GameScene.effect(m);
		final com.watabou.noosa.MovieClip mm = m;
		m.parent.add(new com.watabou.noosa.tweeners.PosTweener(m, new PointF(b.x - 8, b.y - 8), 0.04f * path.length){
			@Override
			protected void onComplete() { mm.killAndErase(); }
		});
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		for (int c : path){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || ch.alignment == Alignment.ENEMY || ch instanceof HeartValve || ch instanceof MemoryLamp) continue;
			ch.damage(Random.NormalIntRange(MazeTower.EM_BOLT_MIN, MazeTower.EM_BOLT_MAX), this);
			if (ch.sprite != null) ch.sprite.flash();
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "ondeath_echo"));
			}
		}
	}

	private void warnPetalRain( MasterBossLevel l ){
		int h = Dungeon.hero.pos;
		int w = l.width();
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(h);
		//20층과 같이: 주인공 칸 + 주인공 둘레 6×6칸 안 무작위 (주인공이 가운데 2×2 중 한 칸에 오도록)
		int left = h % w - (MazeTower.EM_RAIN_AREA / 2 - Random.Int(2));
		int top  = h / w - (MazeTower.EM_RAIN_AREA / 2 - Random.Int(2));
		ArrayList<Integer> near = new ArrayList<>();
		for (int c = 0; c < l.length(); c++){
			if (c == h || !l.isRoomFloor(c)) continue;
			int x = c % w, y = c / w;
			if (x >= left && x < left + MazeTower.EM_RAIN_AREA && y >= top && y < top + MazeTower.EM_RAIN_AREA) near.add(c);
		}
		while (cells.size() < MazeTower.EM_RAIN_CELLS && !near.isEmpty()) cells.add(near.remove(Random.Int(near.size())));
		petalRain = new int[cells.size()];
		for (int i = 0; i < cells.size(); i++) petalRain[i] = cells.get(i);
		HeartValve pv = l.valveOf(HeartValve.PETAL);
		if (pv != null && pv.sprite != null) pv.sprite.flash();
		showRainWarn();
	}

	private void showRainWarn(){
		if (rainLook != null){ rainLook.kill(); rainLook = null; }
		if (petalRain == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		ArrayList<Integer> list = new ArrayList<>();
		for (int c : petalRain) list.add(c);
		rainLook = new com.shatteredpixel.shatteredpixeldungeon.effects.CellWarnFx(list, 0x3A3208, 0xC9A52A, 0xFFF0A0, 0xFFE066);
		rainLook.attach(Dungeon.hero.sprite.parent);
	}

	private void firePetalRain(){
		if (rainLook != null){ rainLook.kill(); rainLook = null; }
		int[] cells = petalRain;
		petalRain = null;
		boolean seen = false;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				seen = true;
				com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.atCellBottom(com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_RAIN, c, 12, false);
			}
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || ch.alignment == Alignment.ENEMY || ch instanceof HeartValve || ch instanceof MemoryLamp) continue;
			ch.damage(Random.NormalIntRange(MazeTower.EM_RAIN_MIN, MazeTower.EM_RAIN_MAX), new com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity());
			if (!ch.isAlive()){
				if (ch == Dungeon.hero){
					Dungeon.fail(this);
					GLog.n(Messages.get(this, "ondeath_echo"));
				}
				continue;
			}
			Buff.affect(ch, StaticCharge.class, MazeTower.EM_RAIN_STATIC);
		}
		if (seen) Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
	}

	//불러온 뒤 경고 그림을 다시 만든다 (MasterSprite.link)
	public void restoreFx(){
		skills.showFx();
		skills.showTide();
		showFruit();
		if (petalBolt != null) showBoltWarn(); //(점검) 화면을 다시 만들면 예전 경고는 사라진 화면의 것이므로 늘 다시 그린다
		if (petalRain != null) showRainWarn();
	}

	private void clearEchoFx(){
		if (boltLine != null){ boltLine.killAndErase(); boltLine = null; }
		if (rainLook != null){ rainLook.kill(); rainLook = null; }
		petalBolt = null; petalRain = null;
	}

	// ===== (O2, 사용자) 등불 판막의 빛 안에 있으면: 턴마다 낙뢰 =====
	//하늘에서 주인공에게 두 줄 번개(마법사 낙뢰와 같은 그림), 피해 O_LANTERN_BOLT_*(방어 무시) + 정전기. 빛 자체가 예고다
	private void thunder( Hero h ){
		if (sprite != null){ final com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite sp = sprite; sp.zap(h.pos, new com.watabou.utils.Callback(){ @Override public void call(){ sp.idle(); } }); } //(사용자) 낙뢰 뒤 가만히 동작으로 (그림이 굳지 않게)
		if (h.sprite != null && h.sprite.parent != null){
			PointF c = h.sprite.center();
			ArrayList<Lightning.Arc> arcs = new ArrayList<>();
			arcs.add(new Lightning.Arc(new PointF(c.x - 3, c.y - 52), c));
			arcs.add(new Lightning.Arc(new PointF(c.x + 4, c.y - 46), c));
			h.sprite.parent.addToFront(new Lightning(arcs, null));
			h.sprite.centerEmitter().burst(SparkParticle.FACTORY, 10);
			h.sprite.flash();
		}
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		PixelScene.shake(1, 0.15f);
		h.damage(Random.NormalIntRange(MazeTower.O_LANTERN_BOLT_MIN, MazeTower.O_LANTERN_BOLT_MAX), new Thunder());
		if (h.isAlive()) Buff.prolong(h, StaticCharge.class, 2f);
		else {
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "ondeath_thunder"));
		}
	}

	//낙뢰 피해의 출처 (사망 기록)
	public static class Thunder {}

	// ===== (O2, 사용자) 어둠 속에 있으면: 20층 망령 왕처럼 기습 =====
	//주인공이 마지막으로 움직인 방향의 반대쪽 칸(안 되면 둘레 빈칸)에 푸른 눈(WraithEyes)이 나타난다. 눈을 치면 기습이 막히고 그 피해의 10%가 주인에게.
	//치지 않으면 다음 주인 턴에 어둠 속에서 주인의 그림자가 그 칸에 나타나 반드시 맞는 일격(평타 × 1.3, 방어 50%만)을 하고 스러진다. 주인은 줄기 곁 제자리.
	private int ambushCd = MazeTower.O_AMBUSH_INTERVAL;
	private int ambushCell = -1;
	private boolean countered = false;

	//(점검) 화면 그리기 쪽(판막 일렁임 등)에서도 불리므로 mobs를 돌지 않고 들고 있는 주인을 쓴다 (만들어질 때 채우고 쓰러지면 비움)
	private static volatile TowerMaster current;
	{ current = this; }
	//새 25층을 만들거나 불러올 때 예전 판의 주인을 잊는다 (MasterBossLevel)
	public static void clearCurrent(){ current = null; }

	public static TowerMaster find(){
		TowerMaster c = current;
		if (c != null && c.isAlive() && Dungeon.level instanceof MasterBossLevel) return c;
		return null;
	}

	//(사용자) 눈은 어둠이 깔린 칸에서만 나온다 (어둠에 막 들어선 주인공의 뒤가 어둠 밖이면 둘레의 어둠 칸, 없으면 다음 턴에 다시 본다)
	private boolean validAmbushCell( MasterBossLevel l, int c ){
		return c >= 0 && c < l.length() && l.isRoomFloor(c) && Actor.findChar(c) == null && l.passable[c]
				&& l.darkZone(c, l.valveOf(HeartValve.DARK));
	}

	private boolean startAmbush( MasterBossLevel l ){
		int hp = Dungeon.hero.pos;
		int behind = -1;
		if (l.lastHeroStep != 0 && validAmbushCell(l, hp - l.lastHeroStep) && l.adjacent(hp - l.lastHeroStep, hp)) behind = hp - l.lastHeroStep;
		if (behind == -1){
			ArrayList<Integer> opts = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8) if (validAmbushCell(l, hp + n)) opts.add(hp + n);
			if (opts.isEmpty()){ ambushCd = 1; return false; }
			behind = Random.element(opts);
		}
		ambushCd = MazeTower.O_AMBUSH_INTERVAL;
		ambushCell = behind;
		countered = false;
		WraithEyes e = new WraithEyes();
		e.pos = behind;
		GameScene.add(e);
		Dungeon.observe();
		return true;
	}

	private static void removeEyes(){
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) if (m instanceof WraithEyes) m.die(null);
	}

	//눈이 맞았다 (WraithEyes.damage): 기습을 막고 피해의 10%를 주인에게
	public void counterHit( int dmg, Object src ){
		if (ambushCell == -1 || countered) return;
		countered = true;
		damage(Math.max(1, Math.round(dmg * Random.Float(MazeTower.WK_COUNTER_MIN, MazeTower.WK_COUNTER_MAX))), src);
	}

	private boolean actAmbush(){
		int cell = ambushCell;
		boolean wasCountered = countered;
		removeEyes();
		ambushCell = -1;
		countered = false;
		Hero h = Dungeon.hero;
		if (wasCountered){
			GLog.p(Messages.get(this, "ambush_blocked"));
		} else if (h.isAlive()){
			//눈이 있던 칸(막혔거나 주인공이 멀어졌으면 주인공 옆 빈칸)에서 덮친다
			int at = (Actor.findChar(cell) == null && Dungeon.level.adjacent(cell, h.pos)) ? cell : -1;
			if (at == -1){
				ArrayList<Integer> opts = new ArrayList<>();
				for (int n : PathFinder.NEIGHBOURS8){
					int c = h.pos + n;
					if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null) opts.add(c);
				}
				at = opts.isEmpty() ? h.pos : Random.element(opts);
			}
			ambushShadow(at, h.pos);
			int dmg = Math.round(damageRoll() * MazeTower.WK_AMBUSH_MULTI * MazeTower.WK_AMBUSH_DMG);
			int armor = Math.round(h.drRoll() * (1f - MazeTower.WK_AMBUSH_ARMOR_IGNORE));
			dmg = Math.max(0, dmg - armor);
			if (h.sprite != null){
				h.sprite.flash();
				com.shatteredpixel.shatteredpixeldungeon.effects.Wound.hit(h);
				clawFx(h, at);
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
			h.damage(dmg, this);
			if (!h.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "ondeath_ambush"));
			}
		}
		spend(TICK);
		return true;
	}

	//어둠 속에서 잠깐 보이는 주인의 그림자 (지팡이를 내리치는 칸, 안개 위에 0.5초)
	private void ambushShadow( int at, int target ){
		if (sprite == null) return;
		com.watabou.noosa.Image im = new com.watabou.noosa.Image(sprite.texture);
		boolean big = stage >= 2 && MazeTower.O_MASTER2_ART; //(점검) 2·3막 그림은 32×30
		int fw = big ? 32 : 24, fh = big ? 30 : 26;
		im.frame(new TextureFilm(sprite.texture, fw, fh).get(7));
		PointF p = DungeonTilemap.tileToWorld(at);
		im.x = p.x + (16 - fw) / 2f;
		im.y = p.y + 16 - fh;
		boolean left = (target % Dungeon.level.width()) < (at % Dungeon.level.width());
		if (left){ im.scale.x = -1; im.x += fw; }
		GameScene.groupOverFog(new FadeOut(im, 0.5f));
		CellEmitter.get(at).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 6);
	}

	//잠깐 보였다가 스러지는 그림
	private static class FadeOut extends com.watabou.noosa.Group {
		private final com.watabou.noosa.Image im;
		private final float time;
		private float t = 0;
		FadeOut( com.watabou.noosa.Image im, float time ){ this.im = im; this.time = time; add(im); }
		@Override public void update(){
			super.update();
			t += Game.elapsed;
			im.alpha(Math.max(0, 1f - t / time));
			if (t >= time) killAndErase();
		}
	}

	//기습 피격 연출: 그림자 쪽에서 긁는 검은 가지 자국 한 줄(핏빛 테두리) + 그림자 넋 + 흔들림 1픽셀
	private void clawFx( Char target, int from ){
		if (target.sprite == null || target.sprite.parent == null) return;
		PointF t = target.sprite.destinationCenter();
		PointF k = DungeonTilemap.tileCenterToWorld(from);
		float dir = (float)Math.atan2(t.y - k.y, t.x - k.x);
		if (k.x == t.x && k.y == t.y) dir = (float)(Math.PI / 4);
		float sweep = 1.4f;
		float start = dir + (float)Math.PI - sweep / 2f;
		com.watabou.noosa.Group fx = new com.watabou.noosa.Group();
		GameScene.groupOverFog(fx);
		PointF c = new PointF(t.x + (float)Math.cos(dir) * 7f, t.y + (float)Math.sin(dir) * 7f);
		new com.shatteredpixel.shatteredpixeldungeon.effects.SlashTrail(10f, 3f, start, sweep, 0.04f, 0.09f, 0.28f)
				.trailColor(0x1A0A0E).solid().show(fx, c);
		new com.shatteredpixel.shatteredpixeldungeon.effects.SlashTrail(11.2f, 1f, start, sweep, 0.04f, 0.09f, 0.28f)
				.trailColor(0xC8404E).show(fx, c);
		target.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle.UP, 8);
		PixelScene.shake(1, 0.15f);
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("maze_ambush_cd", ambushCd);
		bundle.put("maze_ambush_cell", ambushCell);
		bundle.put("maze_countered", countered);
		bundle.put("maze_repair_pos", repairPos);
		bundle.put("maze_repair_left", repairLeft);
		bundle.put("maze_home", home);
		bundle.put("maze_repair_cd", repairCd);
		bundle.put("maze_travel_to", travelTo);
		bundle.put("maze_hops_left", hopsLeft);
		bundle.put("maze_returning", returning);
		bundle.put("maze_stage", stage);
		bundle.put("maze_fruit", new int[]{ fruitCell, fruitLeft, fruitRegrow, fruitCharges, eating });
		bundle.put("maze_eat_vuln", eatVulnerable);
		skills.store(bundle);
		bundle.put("maze_petal_bolt_cd", petalBoltCd);
		bundle.put("maze_petal_rain_cd", petalRainCd);
		bundle.put("maze_petal_bolt_from", petalBoltFrom);
		if (petalBolt != null) bundle.put("maze_petal_bolt", petalBolt);
		if (petalRain != null) bundle.put("maze_petal_rain", petalRain);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("maze_ambush_cd")) ambushCd = bundle.getInt("maze_ambush_cd");
		if (bundle.contains("maze_ambush_cell")) ambushCell = bundle.getInt("maze_ambush_cell");
		countered = bundle.getBoolean("maze_countered");
		repairPos = bundle.contains("maze_repair_pos") ? bundle.getInt("maze_repair_pos") : -1;
		repairLeft = bundle.getInt("maze_repair_left");
		if (bundle.contains("maze_home")) home = bundle.getInt("maze_home");
		repairCd = bundle.getInt("maze_repair_cd");
		travelTo = bundle.contains("maze_travel_to") ? bundle.getInt("maze_travel_to") : -1;
		hopsLeft = bundle.getInt("maze_hops_left");
		returning = bundle.getBoolean("maze_returning");
		stage = bundle.contains("maze_stage") ? bundle.getInt("maze_stage") : 1;
		if (bundle.contains("maze_fruit")){ int[] fr = bundle.getIntArray("maze_fruit"); fruitCell = fr[0]; fruitLeft = fr[1]; fruitRegrow = fr[2]; fruitCharges = fr[3]; eating = fr[4]; }
		eatVulnerable = bundle.getBoolean("maze_eat_vuln");
		skills.restore(bundle);
		if (bundle.contains("maze_petal_bolt_cd")) petalBoltCd = bundle.getInt("maze_petal_bolt_cd");
		if (bundle.contains("maze_petal_rain_cd")) petalRainCd = bundle.getInt("maze_petal_rain_cd");
		if (bundle.contains("maze_petal_bolt_from")) petalBoltFrom = bundle.getInt("maze_petal_bolt_from");
		petalBolt = bundle.contains("maze_petal_bolt") ? bundle.getIntArray("maze_petal_bolt") : null;
		petalRain = bundle.contains("maze_petal_rain") ? bundle.getIntArray("maze_petal_rain") : null;
	}

	//(사용자) 1·2막에는 걷지 않는다 (다시 닫기·낙엽 걸음의 순간이동으로만 옮김). 3막에서만 걷는다 (O6)
	@Override protected boolean getCloser( int target ) { return stage >= 3 && super.getCloser(target); }
	@Override protected boolean getFurther( int target ) { return false; }

	@Override public int damageRoll() {
		int d = Random.NormalIntRange(MazeTower.O_MASTER_DMG_MIN, MazeTower.O_MASTER_DMG_MAX);
		if (buff(MasterFruit.class) != null) d = Math.round(d * MazeTower.O_FRUIT_DMG); //(사용자) 세계수의 열매를 먹은 동안
		lastRoll = d;
		return d;
	}

	//(사용자) 평타도 기술처럼: 피해의 O_SKILL_CHIP(최대 O_SKILL_CHIP_MAX)은 방어가 높아도 박힌다 (방어로 줄어든 피해가 그보다 작으면 그만큼으로 올림)
	private int lastRoll = 0;
	@Override
	public int attackProc( Char enemy, int damage ) {
		int d = super.attackProc(enemy, damage);
		if (enemy != null && enemy.alignment != Char.Alignment.ENEMY && lastRoll > 0) d = Math.max(d, Math.min(MazeTower.O_SKILL_CHIP_MAX, Math.round(lastRoll * MazeTower.O_SKILL_CHIP)));
		return d;
	}
	@Override public int attackSkill( Char target ) { return MazeTower.O_MASTER_ACC; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, MazeTower.O_MASTER_DR); }

	@Override
	public void die( Object cause ) {
		clearEchoFx();
		skills.cancelTide();
		if (fruitFx != null){ fruitFx.killAndErase(); fruitFx = null; }
		fruitCell = -1;
		skills.cancel();
		skills.cancelSpear();
		if (ambushCell != -1){ removeEyes(); ambushCell = -1; }
		GameScene.bossSlain();
		if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) && Statistics.spawnersAlive == 4){
			Badges.validateBossChallengeCompleted();
		} else {
			Statistics.qualifiedForBossChallengeBadge = false;
		}
		Statistics.bossScores[4] += 5000 + 1250 * Statistics.spawnersAlive;
		Badges.validateTakingTheMick(cause);
		Dungeon.level.mazeBossPos = pos; //보스가 쓰러진 자리 (역행 중 흔적)
		super.die(cause);
		yell(Messages.get(this, "defeated"));
		if (Dungeon.level instanceof MasterBossLevel) ((MasterBossLevel) Dungeon.level).onMasterDeath();
		com.shatteredpixel.shatteredpixeldungeon.MazeTrial.onBossDefeated();
	}

	public static class MasterSprite extends MobSprite {
		private Animation close;

		//(O4) 다시 닫기 자세 (끝나면 가만히)
		public void repairPose( boolean on ){
			if (on) play(close); else idle();
		}

		public MasterSprite(){
			super();
			build(false);
			play(idle);
		}

		//(O5) 2막부터 등에서 가지가 크게 뻗은 그림 (sprites/maze_b25_master2.png 32×30 × 28, 칸 번호는 1막과 같고 18~27 막 전환,
		//원본 docs/sprites/boss25/make_master2_anim.ps1). 그림은 사용자 확인 뒤 켬(O_MASTER2_ART), 그 전에는 1막 그림 그대로
		private int shownStage = 1;
		//2막 그림은 위로 4픽셀 더 크다 (머리 위 창 자리를 맞춤)
		private float headY(){ return y + (shownStage >= 2 ? 4 : 0); }
		private Animation grow;

		public void setStage( int st, boolean animate ){
			//(O6) 3막은 가슴의 옹이가 갈라진 그림 (sprites/maze_b25_master3.png, 2막 그림과 칸 배치 같음, 원본 docs/sprites/boss25/make_master3_anim.ps1)
			int want = st >= 3 && MazeTower.O_MASTER3_ART ? 3 : st >= 2 && MazeTower.O_MASTER2_ART ? 2 : 1;
			if (want > shownStage){
				int from = shownStage;
				artStage = want;
				build(true);
				shownStage = want;
				if (animate && from < 2 && want == 2) play(grow); else play(idle);
				if (ch != null) place(ch.pos); //칸이 커졌으니 자리를 다시 맞춘다 (발밑 기준)
				if (!(animate && want == 3 && ch != null)) return;
			}
			if (animate && ch != null && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null)
				zap(ch.pos, new com.watabou.utils.Callback(){ @Override public void call(){ idle(); } });
		}
		private int artStage = 1;

		@Override
		public void onComplete( Animation anim ) {
			if (anim == grow){ idle(); return; }
			super.onComplete(anim);
		}

		private void build( boolean two ){
			texture(!two ? "sprites/maze_b25_master.png" : artStage >= 3 ? "sprites/maze_b25_master3.png" : "sprites/maze_b25_master2.png");
			TextureFilm f = two ? new TextureFilm(texture, 32, 30) : new TextureFilm(texture, 24, 26);
			idle = new Animation(4, true);
			idle.frames(f, 0, 0, 0, 1, 1, 1);
			run = new Animation(6, true);
			run.frames(f, 2, 3, 4, 5);
			attack = new Animation(8, false);
			attack.frames(f, 6, 6, 7, 7, 8, 0);
			zap = new Animation(6, false);
			zap.frames(f, 13, 14, 14, 14, 15, 0);
			die = new Animation(3, false);
			die.frames(f, 0, 9, 9, 10, 10, 11, 11, 12);
			close = new Animation(2, true);
			close.frames(f, 16, 17); //(O4) 다시 닫기: 지팡이를 앞에 꽂고 고개를 숙여 기를 모음
			grow = new Animation(8, false);
			if (two) grow.frames(f, 18, 18, 19, 19, 20, 21, 22, 23, 24, 25, 26, 27, 27); else grow.frames(f, 13, 14, 14, 15);
		}

		//불러온 뒤 꽃잎 판막 덤 기술의 경고를 다시 그린다
		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof TowerMaster){ setStage(((TowerMaster) ch).stage, false); ((TowerMaster) ch).restoreFx(); if (((TowerMaster) ch).closing()) repairPose(true); }
		}

		// ===== (O3, 사용자) 여제 판의 머리 위 창 (20층 여제 그림과 같은 EM_FROST_OVER / EM_FIRE_OVER) =====
		private com.watabou.noosa.MovieClip spear;
		private int spearShown = 0;
		private com.shatteredpixel.shatteredpixeldungeon.effects.SpearAimFx aim;
		private boolean wasArmed = false;

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof TowerMaster) || parent == null) return;
			TowerMaster mm = (TowerMaster) ch;
			int want = mm.isAlive() ? mm.skills.spearType : 0;
			if (want != spearShown){
				if (spear != null){ spear.killAndErase(); spear = null; }
				spearShown = want;
				if (want != 0){
					spear = com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.clip(want == 1 ? com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_FROST_OVER : com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_FIRE_OVER, 6, true);
					parent.add(spear);
				}
			}
			boolean armed = spear != null && mm.skills.spearArmed() && visible;
			if (spear != null){
				com.watabou.utils.PointF c = center();
				spear.origin.set(10, 3.5f);
				spear.x = c.x - 10;
				spear.y = headY() - 6;
				spear.visible = visible;
				if (armed){
					//조준하는 동안: 창이 주인공 쪽을 겨눈다
					com.watabou.utils.PointF hc = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.sprite.center();
					spear.scale.x = 1;
					spear.angle = (float)(Math.atan2(hc.y - (headY() - 2.5f), hc.x - c.x) * 180 / Math.PI);
				} else {
					boolean heroLeft = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null && com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.sprite != null
							&& com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.sprite.center().x < c.x;
					spear.scale.x = heroLeft ? -1 : 1;
					spear.angle = 0;
				}
			}
			updateAim(mm, armed);
		}

		//날아오기 2턴 전부터: 창끝에서 주인공 쪽 점선 조준선 (실제 창과 같은 경로, 매 프레임), 주인공에게 닿으면 발밑 고리 (SpearAimFx)
		private void updateAim( TowerMaster mm, boolean armed ){
			if (!armed){
				if (aim != null) aim.hide();
				wasArmed = false;
				return;
			}
			if (aim == null || aim.parent == null){
				aim = new com.shatteredpixel.shatteredpixeldungeon.effects.SpearAimFx();
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupOverFog(aim);
			}
			int color = mm.skills.spearType == 1 ? 0xA8DCFF : 0xFF9A3C;
			com.watabou.utils.PointF c = center();
			com.watabou.utils.PointF mid = new com.watabou.utils.PointF(c.x, headY() - 2.5f);
			float rad = (float)(spear.angle * Math.PI / 180);
			com.watabou.utils.PointF tip = new com.watabou.utils.PointF(mid.x + (float)Math.cos(rad) * 9, mid.y + (float)Math.sin(rad) * 9);
			if (!wasArmed){
				wasArmed = true;
				new com.shatteredpixel.shatteredpixeldungeon.effects.Flare(4, 8).color(color, true).show(parent, tip, 0.45f);
			}
			com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica b = new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(mm.pos, com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.pos,
					com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_SOLID | com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.STOP_TARGET);
			int end = b.collisionPos;
			boolean blocked = com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.solid[end];
			com.watabou.utils.PointF to;
			com.watabou.utils.PointF foot = null;
			if (blocked){
				int before = b.dist >= 1 ? b.path.get(b.dist - 1) : mm.pos;
				com.watabou.utils.PointF p1 = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(before), p2 = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(end);
				to = new com.watabou.utils.PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f);
			} else {
				to = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.sprite.center();
				com.watabou.utils.PointF hc = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.pos);
				foot = new com.watabou.utils.PointF(hc.x, hc.y + 6);
				if (end != com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.pos) blocked = true;
			}
			aim.set(tip, to, blocked, foot, color);
		}

		@Override
		public void kill() {
			if (spear != null) spear.killAndErase();
			if (aim != null) aim.killAndErase();
			super.kill();
		}

		@Override
		public void die() {
			if (spear != null){ spear.killAndErase(); spear = null; }
			if (aim != null) aim.hide();
			super.die();
		}
	}

	//보스 체력 바 밑 창 아이콘: 남은 턴 숫자 (20층 여제의 SpearCountdown과 같은 글·그림)
	public static class SpearCountdown extends MazeEmpress.SpearCountdown {
		private MasterSkills sk(){ return target instanceof TowerMaster ? ((TowerMaster) target).skills : null; }
		@Override public boolean act(){ MasterSkills s = sk(); if (s == null || s.spearType == 0) detach(); spend(TICK); return true; }
		@Override public int icon(){ MasterSkills s = sk(); if (s == null || s.spearType == 0) return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE;
			return s.spearType == 1 ? com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.FROST : com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.FIRE; }
		@Override public float iconFadePercent(){ MasterSkills s = sk(); if (s == null) return 0; return Math.max(0, Math.min(1, 1f - Math.max(0, s.spearTimer) / (float) Math.max(1, s.spearMax))); }
		@Override public String iconTextDisplay(){ MasterSkills s = sk(); return s == null ? "" : Integer.toString(Math.max(0, s.spearTimer)); }
		@Override public String name(){ MasterSkills s = sk(); return Messages.get(MazeEmpress.SpearCountdown.class, s != null && s.spearType == 2 ? "name_fire" : "name_frost"); }
		@Override public String desc(){ MasterSkills s = sk(); return Messages.get(MazeEmpress.SpearCountdown.class, "desc", s == null ? 0 : Math.max(0, s.spearTimer)); }
	}
}
