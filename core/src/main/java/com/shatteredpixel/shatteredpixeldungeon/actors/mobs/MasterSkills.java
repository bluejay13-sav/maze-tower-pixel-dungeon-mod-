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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SapFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GooSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (O3): 25층 탑의 주인 1막 기술 (기획 docs/boss25_heart_plan.md). 한 턴에 기술 하나, 모두 1턴 전에 예고(예고 = 실제: 경고한 칸 그대로 터진다).
//몸의 기술: 뿌리 창(주인공을 지나는 한 줄 바닥 금 → 검은 뿌리가 솟구침), 가지 부채(주인공이 3칸 안이면 앞쪽 부채꼴 → 지팡이로 바닥을 짚어 검은 가지 + 밀침).
//메아리 기술 (그 판막이 깨지지 않았을 때만, 판막을 깨면 사라진다): 수액 → 수액 내려치기 / 등불 → 끌어내기 / 망령 왕 판 → 인간을 죽이는 마법 / 여제 판 → 서리창·화염창.
//예고 그림은 그 보스 때와 같은 것을 쓴다(사슬 조준선, 졸트라크 범위, 땅울림 금 등). 솟구치는 뿌리·가지 그림은 확인을 받은 뒤 넣는다(지금은 원작 효과).
public class MasterSkills {

	public static final int NONE = 0, ROOT_SPEAR = 1, FAN = 2, SLAM = 3, PULL = 4, ZOLT = 5;
	//(O5) 2막 "걷는 나무": 가지 채찍(뿌리 창을 대신함), 낙엽 걸음, 낙엽 바람
	public static final int WHIP = 6, LEAFWALK = 7, WIND = 8;

	private final TowerMaster m;

	//예고한 기술 (다음 주인 턴에 그대로 터진다)
	public int pending = NONE;
	//(사용자) 예고한 기술이 터지기까지 더 기다릴 주인 턴 (수액 내려치기는 경고 2턴: 그동안 주인은 다른 기술 없이 힘을 모은다)
	public int warnLeft = 0;
	private int[] cells = null;   //맞는 칸 (뿌리 창·부채·내려치기·졸트라크) 또는 날아갈 줄 (끌어내기·창)
	private int dir = 0;          //뿌리 창: 금 방향(0 가로 1 세로 2 "\" 3 "/"), 졸트라크: 8방향
	private int[] lanes = null;   //졸트라크: 칸마다 줄 번호
	private int origin = -1;      //예고할 때 주인의 칸

	//쿨타임 (주인 턴마다 1씩 줄어든다)
	private int cdSpear = 2, cdFan = 0, cdSlam = 3, cdPull = 4, cdZolt = 5, cdElem = 5;
	private int cdWalk = 1, cdWind = 3; //(O5) 2막

	private Group fx;

	public MasterSkills( TowerMaster m ){ this.m = m; }

	public void tick(){
		cdSpear--; cdFan--; cdSlam--; cdPull--; cdZolt--; cdElem--;
		if (m.stage >= 2){ cdWalk--; cdWind--; }
		if (m.stage >= 3 && tideBand < 0) cdTide--;
	}

	// ===================== 새 기술 고르기 =====================

	//우선순위: 가지 부채(가까우면) > (2막) 낙엽 걸음(바스락 소리가 났으면) > 메아리 기술(판막이 살아 있으면) > (2막) 낙엽 바람 > 뿌리 창 (2막은 가지 채찍)
	public boolean tryStart( MasterBossLevel l, Hero h ){
		if (tideBand >= 0) return false; //(O6) 해일이 밀려오는 동안 새 기술 없음 (걷기·평타는 함)
		if (h.invisible > 0) return false; //은신 존중
		if (cdFan <= 0 && l.distance(m.pos, h.pos) <= MazeTower.O_FAN_RANGE && startFan(l, h)){ cdFan = MazeTower.O_FAN_CD; return true; }
		if (m.stage >= 2 && cdWalk <= 0 && startLeafWalk(l, h)){ cdWalk = MazeTower.O_LEAFWALK_CD; return true; }
		if (m.stage >= 3 && cdTide <= 0 && startTide(l)) return true;
		if (m.stage >= 2 && echoStart(l, h)) return true;
		if (m.stage >= 2 && cdWind <= 0 && startWind(l, h)){ cdWind = MazeTower.O_WIND_CD; return true; }
		if (m.stage >= 2){
			if (cdSpear <= 0 && startWhip(l, h)){ cdSpear = MazeTower.O_WHIP_CD; return true; }
			return false;
		}
		if (echoStart(l, h)) return true;
		if (cdSpear <= 0 && startSpear(l, h)){ cdSpear = MazeTower.O_ROOT_SPEAR_CD; return true; }
		return false;
	}

	//메아리 기술 (그 판막이 살아 있을 때만)
	private boolean echoStart( MasterBossLevel l, Hero h ){
		if (cdSlam <= 0 && alive(l, HeartValve.SAP) && startSlam(l, h)){ cdSlam = MazeTower.O_SLAM_CD; return true; }
		if (cdPull <= 0 && alive(l, HeartValve.LANTERN) && startPull(l, h)){ cdPull = MazeTower.O_ECHO_CD; return true; }
		if (cdZolt <= 0 && alive(l, HeartValve.DARK) && startZolt(l, h)){ cdZolt = MazeTower.O_ECHO_CD; return true; }
		if (cdElem <= 0 && alive(l, HeartValve.PETAL) && startElem(l, h)){ cdElem = MazeTower.O_ECHO_CD + spearMax; return true; } //쿨타임은 창을 던진 뒤부터
		return false;
	}

	// ===================== (O5) 2막 =====================

	//낙엽 경고 칸 (빛바랜 낙엽빛)
	private void leafWarn( ArrayList<Integer> list ){
		fx = com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx.CellWarn.show(list, new float[]{ MazeTower.O_LEAF_WARN_ALPHA }, MazeTower.O_LEAF_WARN_COLOR, 0);
	}

	// ----- 가지 채찍 (뿌리 창을 대신함): 주인에게서 주인공을 지나 벽까지 곧은 줄 → 주인 쪽부터 검은 가지, 그 칸에 검은 낙엽 -----
	private boolean startWhip( MasterBossLevel l, Hero h ){
		ArrayList<Integer> line = whipLine(l, h.pos);
		if (!line.contains(h.pos)) return false;
		set(WHIP, line);
		if (m.sprite != null) m.sprite.turnTo(m.pos, h.pos);
		if (l.heroFOV[h.pos]) GLog.w(Messages.get(TowerMaster.class, "whip_warn"));
		Sample.INSTANCE.play(Assets.Sounds.GRASS, 0.6f, 0.7f);
		showFx();
		return true;
	}

	private ArrayList<Integer> whipLine( MasterBossLevel l, int to ){
		ArrayList<Integer> out = new ArrayList<>();
		Ballistica b = new Ballistica(m.pos, to, Ballistica.STOP_SOLID);
		for (int c : b.path){
			if (c == m.pos) continue;
			if (!l.isRoomFloor(c)) break; //벽·줄기에서 멈춤
			out.add(c);
		}
		return out;
	}

	private boolean fireWhip(){
		ArrayList<Integer> line = list();
		if (!line.isEmpty()) tap(line.get(0));
		TimedHits hits = new TimedHits();
		for (int i = 0; i < line.size(); i++){
			final int c = line.get(i);
			float delay = MazeTower.O_FAN_TAP + i * MazeTower.O_WHIP_STAGGER;
			GameScene.effect(new com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx(c, com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx.ROOT, delay, false));
			hits.add(delay + 1f / MazeTower.O_SPIKE_FPS, new Runnable(){ @Override public void run(){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 1);
				Char ch = Actor.findChar(c);
				if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.O_WHIP_MIN, MazeTower.O_WHIP_MAX), 1f, "ondeath_whip");
				if (Dungeon.level instanceof MasterBossLevel) ((MasterBossLevel) Dungeon.level).layLeaves(c);
			}});
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 1.1f);
		PixelScene.shake(1, 0.12f);
		return hits.start();
	}

	// ----- 낙엽 걸음: 주인공이 낙엽을 밟아 바스락 소리를 낸 칸으로 (낙엽이 있는 칸으로만) 순간이동 → 둘레 8칸에 검은 가지 -----
	private boolean startLeafWalk( MasterBossLevel l, Hero h ){
		int t = l.rustleCell;
		if (t == -1 || !com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves.at(t)) return false;
		if (l.distance(m.pos, h.pos) < MazeTower.O_LEAFWALK_MINDIST || l.distance(m.pos, t) < 2) return false;
		ArrayList<Integer> list = new ArrayList<>();
		list.add(t);
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8) if (l.isRoomFloor(t + n)) list.add(t + n);
		l.rustleCell = -1; //이 소리는 들었다
		set(LEAFWALK, list);
		warnLeft = MazeTower.O_LEAFWALK_WARN - 1; //(사용자) 경고 2턴
		if (m.sprite != null) m.sprite.showStatus(MazeTower.O_LEAF_WARN_COLOR, Integer.toString(MazeTower.O_LEAFWALK_WARN));
		dir = t;
		if (m.sprite != null) m.sprite.turnTo(m.pos, t);
		l.leafBurst(m.pos, 6);
		l.leafBurst(t, 6);
		if (l.heroFOV[t]) GLog.w(Messages.get(TowerMaster.class, "leafwalk_warn"));
		Sample.INSTANCE.play(Assets.Sounds.GRASS, 0.8f, 0.6f);
		showFx();
		return true;
	}

	private boolean fireLeafWalk(){
		if (!(Dungeon.level instanceof MasterBossLevel)) return false;
		MasterBossLevel l = (MasterBossLevel) Dungeon.level;
		ArrayList<Integer> warned = list();
		int t = dir;
		//내려앉을 칸: 낙엽 칸 그대로 (주인공·주인 편이 서 있으면 한 칸 밀어냄, 판막·등불이면 경고 칸 안의 빈 칸)
		Char occ = Actor.findChar(t);
		int land = t;
		if (occ != null && occ != m){
			if (target(occ) && !occ.properties().contains(Char.Property.IMMOVABLE) && shove(l, occ, t)) land = t;
			else {
				land = -1;
				for (int c : warned) if (c != t && Actor.findChar(c) == null){ land = c; break; }
			}
		}
		if (land != -1 && land != m.pos) m.leafTeleport(l, land);
		//경고한 칸 그대로 (주인 칸 제외) 검은 가지가 솟는다
		TimedHits hits = new TimedHits();
		for (final int c : warned){
			if (c == m.pos) continue;
			float delay = 0.25f;
			GameScene.effect(new com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx(c, com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx.ROOT, delay, false));
			hits.add(delay + 1f / MazeTower.O_SPIKE_FPS, new Runnable(){ @Override public void run(){
				Char ch = Actor.findChar(c);
				if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.O_LEAFWALK_MIN, MazeTower.O_LEAFWALK_MAX), 1f, "ondeath_leafwalk");
			}});
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 0.9f);
		return hits.start();
	}

	//낙엽 걸음이 내려앉는 칸의 대상을 주인에게서 먼 쪽 빈 칸으로 한 칸 밀어낸다
	private boolean shove( MasterBossLevel l, final Char ch, int from ){
		int best = -1, bestD = -1;
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS8){
			int c = from + n;
			if (!l.isRoomFloor(c) || Actor.findChar(c) != null || l.pit[c]) continue;
			int d = l.distance(c, m.pos);
			if (d > bestD){ bestD = d; best = c; }
		}
		if (best == -1) return false;
		final int to = best;
		Actor.add(new Pushing(ch, from, to));
		ch.pos = to;
		Dungeon.level.occupyCell(ch);
		if (ch == Dungeon.hero){ Dungeon.observe(); GameScene.updateFog(); }
		return true;
	}

	// ----- 낙엽 바람: 방의 세로 또는 가로 절반(무작위) → 약한 피해 + 검은 낙엽 -----
	private boolean startWind( MasterBossLevel l, Hero h ){
		int axis = Random.Int(2), side = Random.Int(2);
		ArrayList<Integer> list = windCells(l, axis, side);
		if (list.isEmpty()) return false;
		set(WIND, list);
		dir = axis * 2 + side;
		zap(h.pos);
		if (l.heroFOV[h.pos]) GLog.w(Messages.get(TowerMaster.class, "wind_warn"));
		Sample.INSTANCE.play(Assets.Sounds.GRASS, 0.7f, 0.8f);
		showFx();
		return true;
	}

	//axis 0: 세로로 나눈 왼쪽(side 0)·오른쪽(1) 절반 / axis 1: 가로로 나눈 위(0)·아래(1) 절반 (줄기를 지나는 가운데 줄은 빼고)
	private static ArrayList<Integer> windCells( MasterBossLevel l, int axis, int side ){
		ArrayList<Integer> out = new ArrayList<>();
		int w = l.width(), sx = MasterBossLevel.SAPLING % w, sy = MasterBossLevel.SAPLING / w;
		for (int c = 0; c < l.length(); c++){
			if (!l.isRoomFloor(c)) continue;
			int v = axis == 0 ? c % w - sx : c / w - sy;
			if (side == 0 ? v < 0 : v > 0) out.add(c);
		}
		return out;
	}

	private boolean fireWind(){
		ArrayList<Integer> list = list();
		if (list.isEmpty() || !(Dungeon.level instanceof MasterBossLevel)) return false;
		final MasterBossLevel l = (MasterBossLevel) Dungeon.level;
		int w = l.width(), sx = MasterBossLevel.SAPLING % w, sy = MasterBossLevel.SAPLING / w;
		int axis = dir / 2;
		//바깥 가장자리에서 가운데 쪽으로 지나간다
		int far = 1;
		for (int c : list) far = Math.max(far, Math.abs(axis == 0 ? c % w - sx : c / w - sy));
		ArrayList<Integer> leafy = new ArrayList<>(list);
		java.util.Collections.shuffle(leafy, new java.util.Random(Random.Long()));
		final java.util.HashSet<Integer> lay = new java.util.HashSet<>(leafy.subList(0, Math.round(leafy.size() * MazeTower.O_WIND_LEAVES)));
		TimedHits hits = new TimedHits();
		for (final int c : list){
			int v = Math.abs(axis == 0 ? c % w - sx : c / w - sy);
			float delay = (far - v) / (float) far * MazeTower.O_WIND_SWEEP;
			hits.add(delay, new Runnable(){ @Override public void run(){
				l.leafBurst(c, 2);
				Char ch = Actor.findChar(c);
				if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.O_WIND_MIN, MazeTower.O_WIND_MAX), 0f, "ondeath_wind"); //(사용자) 방어 무시
				if (lay.contains(c)) l.layLeaves(c);
			}});
		}
		Sample.INSTANCE.play(Assets.Sounds.GRASS, 1f, 0.5f);
		Sample.INSTANCE.play(Assets.Sounds.PUFF, 0.6f, 0.7f);
		return hits.start();
	}

	private static boolean alive( MasterBossLevel l, int kind ){
		HeartValve v = l.valveOf(kind);
		return v != null && (!v.broken || v.absorbed); //(O6) 흡수한 판막의 메아리 기술은 끝까지 남는다
	}

	//메아리 기술: 그 판막이 번쩍이고 주인이 지팡이를 높이 든다
	private void echoCue( MasterBossLevel l, int kind ){
		HeartValve v = l.valveOf(kind);
		if (v != null && v.sprite != null) v.sprite.flash();
		zap(Dungeon.hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.6f, 1.2f);
	}

	private void zap( int cell ){
		if (m.sprite != null) m.sprite.zap(cell, new Callback(){ @Override public void call(){ if (m.sprite != null) m.sprite.idle(); } });
	}

	private void tap( int cell ){
		if (m.sprite != null) m.sprite.attack(cell, new Callback(){ @Override public void call(){ if (m.sprite != null) m.sprite.idle(); } });
	}

	// ----- 뿌리 창: 주인공을 지나는 한 줄(가로·세로·대각선 중 5칸 이상 이어지는 것) -----
	private static final int[][] AXES = {{1,0},{0,1},{1,1},{1,-1}};

	private boolean startSpear( MasterBossLevel l, Hero h ){
		ArrayList<Integer> opts = new ArrayList<>();
		for (int a = 0; a < 4; a++) if (spearLine(l, h.pos, a).size() >= MazeTower.O_ROOT_SPEAR_MINLEN) opts.add(a);
		if (opts.isEmpty()) return false;
		dir = Random.element(opts);
		ArrayList<Integer> line = spearLine(l, h.pos, dir);
		set(ROOT_SPEAR, line);
		if (l.heroFOV[h.pos]) GLog.w(Messages.get(TowerMaster.class, "spear_warn"));
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.4f, 0.8f);
		showFx();
		return true;
	}

	//주인공 칸에서 양쪽으로 방 바닥이 이어지는 데까지 (한쪽 끝부터 차례로)
	private static ArrayList<Integer> spearLine( MasterBossLevel l, int from, int a ){
		int w = l.width();
		int step = AXES[a][0] + AXES[a][1] * w;
		int c = from;
		while (ok(l, c - step, c)) c -= step;
		ArrayList<Integer> out = new ArrayList<>();
		while (true){
			out.add(c);
			if (!ok(l, c + step, c)) break;
			c += step;
		}
		return out;
	}

	private static boolean ok( MasterBossLevel l, int next, int cur ){
		return next >= 0 && next < l.length() && l.isRoomFloor(next) && l.adjacent(next, cur);
	}

	//뿌리가 그 칸에 솟는 순간 그 칸에 피해 (닿는 순간 적용). 다 솟을 때까지 주인의 차례가 끝나지 않는다
	private boolean fireSpear(){
		ArrayList<Integer> line = list();
		TimedHits hits = new TimedHits();
		for (int i = 0; i < line.size(); i++){
			final int c = line.get(i);
			float delay = i * MazeTower.O_SPEAR_STAGGER;
			//(사용자 확인) 땅을 뚫고 솟구치는 검은 뿌리: 줄 한쪽 끝부터 한 칸씩
			GameScene.effect(new com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx(c, com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx.ROOT, delay, false));
			hits.add(delay + 1f / MazeTower.O_SPIKE_FPS, new Runnable(){ @Override public void run(){
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 1);
				Char ch = Actor.findChar(c);
				if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.O_ROOT_SPEAR_MIN, MazeTower.O_ROOT_SPEAR_MAX), 1f, "ondeath_spear");
			}});
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		PixelScene.shake(1, 0.15f);
		return hits.start();
	}

	// ----- 가지 부채: 주인 앞쪽 부채꼴 (양옆 O_FAN_HALF°, O_FAN_RANGE칸) -----
	private boolean startFan( MasterBossLevel l, Hero h ){
		int facing = LanternCone.octantToward(m.pos, h.pos);
		ArrayList<Integer> list = new ArrayList<>();
		for (int c = 0; c < l.length(); c++){
			if (c == m.pos || !l.isRoomFloor(c)) continue;
			if (l.distance(c, m.pos) > MazeTower.O_FAN_RANGE) continue;
			int fdx = c % l.width() - m.pos % l.width(), fdy = c / l.width() - m.pos / l.width();
			if (fdx * fdx + fdy * fdy > MazeTower.O_FAN_R2) continue; //둥글게 (대각선일 때 먼 귀퉁이를 뺌)
			//(사용자) 양옆 O_FAN_HALF(40°)와 둥근 거리(O_FAN_R2)로: 정면은 멀어질수록 넓어지는 삼각형(1·3·5·7칸), 대각선은 먼 귀퉁이가 빠진 둥근 부채꼴
			//(양옆 45°에 거리만 보면 대각선이 정사각형, 둥근 거리로만 자르면 정면이 십자 모양이었다)
			if (!LanternCone.inCone(m.pos, c, facing, MazeTower.O_FAN_HALF, MazeTower.O_FAN_RANGE)) continue;
			list.add(c);
		}
		if (!list.contains(h.pos)) return false;
		dir = facing;
		set(FAN, list);
		if (m.sprite != null) m.sprite.turnTo(m.pos, h.pos);
		showFx();
		return true;
	}

	private boolean fireFan(){
		ArrayList<Integer> list = list();
		tap(Dungeon.hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 0.9f, 0.8f);
		//(사용자 확인) 휩쓰는 검은 가지: 지팡이로 바닥을 짚는 순간부터 부채꼴 한쪽 끝에서 다른 쪽 끝으로 (O_FAN_SWEEP초).
		//각도가 커지는 쪽으로 휩쓴다. 그 방향의 가로 성분이 왼쪽이면 그림을 좌우로 뒤집는다(가로 성분이 없으면 바라보는 쪽)
		int w0 = Dungeon.level.width();
		int fx = LanternCone.DIRS[dir][0], fy = LanternCone.DIRS[dir][1];
		int tanX = -fy;
		boolean flip = (tanX != 0 ? tanX : fx) < 0;
		TimedHits hits = new TimedHits();
		for (int c : list){
			int ddx = c % w0 - origin % w0, ddy = c / w0 - origin / w0;
			double ang = Math.toDegrees(Math.atan2(fx * ddy - fy * ddx, ddx * fx + ddy * fy));
			float delay = MazeTower.O_FAN_TAP + (float)((Math.max(-MazeTower.O_FAN_HALF, Math.min(MazeTower.O_FAN_HALF, ang)) + MazeTower.O_FAN_HALF) / (2.0 * MazeTower.O_FAN_HALF)) * MazeTower.O_FAN_SWEEP;
			GameScene.effect(new com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx(c, com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx.WHIP, delay, flip));
			//가지가 그 칸을 휩쓰는 순간 그 칸에 피해 (닿는 순간 적용)
			final int cc = c;
			hits.add(delay + 3f / MazeTower.O_WHIP_FPS, new Runnable(){ @Override public void run(){ fanHit(cc); }});
		}
		PixelScene.shake(1, 0.1f);
		return hits.start();
	}

	private void fanHit( int c ){
		Char ch = Actor.findChar(c);
		if (ch == null || !target(ch)) return;
		hurt(ch, Random.NormalIntRange(MazeTower.O_FAN_MIN, MazeTower.O_FAN_MAX), 1f, "ondeath_fan");
		if (ch.isAlive()) fanPush(ch);
	}

	//(사용자) 주인에게서 먼 쪽으로 O_FAN_PUSH칸 밀친다. 벽에 부딪히면 기절 O_FAN_WALL_STUN턴 + 피해 O_FAN_WALL_*
	//(다른 캐릭터·구덩이에 막히면 그 앞에서 멈추기만 한다. 원작 밀치기는 거리에 비례한 부딪힘이라 따로 만듦)
	private void fanPush( final Char ch ){
		if (ch.rooted || ch.properties().contains(Char.Property.IMMOVABLE)) return;
		final int w = Dungeon.level.width();
		int sx = Integer.signum(ch.pos % w - m.pos % w), sy = Integer.signum(ch.pos / w - m.pos / w);
		if (sx == 0 && sy == 0) return;
		int cur = ch.pos;
		boolean wall = false;
		for (int i = 0; i < MazeTower.O_FAN_PUSH; i++){
			int nx = cur % w + sx, ny = cur / w + sy;
			if (nx < 0 || ny < 0 || nx >= w || ny >= Dungeon.level.height()){ wall = true; break; }
			int next = nx + ny * w;
			if (Dungeon.level.solid[next]){ wall = true; break; }
			if (Actor.findChar(next) != null || Dungeon.level.pit[next]) break;
			cur = next;
		}
		final int to = cur;
		final boolean hitWall = wall;
		final int from = ch.pos;
		if (to == from){
			if (hitWall) wallHit(ch);
			return;
		}
		Actor.add(new Pushing(ch, from, to, new Callback() {
			@Override
			public void call() {
				if (ch.pos != from || Actor.findChar(to) != null){ if (ch.sprite != null) ch.sprite.place(ch.pos); return; }
				ch.pos = to;
				Dungeon.level.occupyCell(ch);
				if (hitWall) wallHit(ch);
				if (ch == Dungeon.hero){ Dungeon.observe(); GameScene.updateFog(); }
			}
		}));
	}

	private void wallHit( Char ch ){
		if (!ch.isAlive()) return;
		if (Dungeon.level.heroFOV[ch.pos]){
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.ROCK), 4);
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 0.9f);
			PixelScene.shake(1, 0.1f);
		}
		ch.damage(Random.NormalIntRange(MazeTower.O_FAN_WALL_MIN, MazeTower.O_FAN_WALL_MAX), new WandOfBlastWave.Knockback());
		if (ch.isAlive()){
			Buff.prolong(ch, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis.class, MazeTower.O_FAN_WALL_STUN);
		} else if (ch == Dungeon.hero){
			Dungeon.fail(m);
			GLog.n(Messages.get(TowerMaster.class, "ondeath_fan_wall"));
		}
	}

	//칸마다 정해진 때(초)에 피해를 주고, 다 끝나면 주인의 차례를 넘긴다 (뿌리 창·가지 부채)
	private class TimedHits extends Group {
		private final ArrayList<Float> times = new ArrayList<>();
		private final ArrayList<Runnable> acts = new ArrayList<>();
		private float t = 0;

		void add( float time, Runnable r ){ times.add(time); acts.add(r); }

		//반환: 주인의 차례를 기다려야 하면 true (화면이 없으면 바로 다 친다)
		boolean start(){
			if (acts.isEmpty()) return false;
			if (m.sprite == null || m.sprite.parent == null){
				for (Runnable r : acts) r.run();
				return false;
			}
			GameScene.groupEffect(this);
			return true;
		}

		@Override
		public void update() {
			super.update();
			t += com.watabou.noosa.Game.elapsed;
			for (int i = 0; i < times.size(); i++){
				if (times.get(i) > t) continue;
				Runnable r = acts.remove(i);
				times.remove(i--);
				r.run();
			}
			if (acts.isEmpty()){
				killAndErase();
				m.skillDone();
			}
		}
	}

	// ----- 수액 내려치기 (수액 판막): 주인공 둘레 O_SLAM_RADIUS칸 경고 → 피해 + 수액 웅덩이 -----
	private boolean startSlam( MasterBossLevel l, Hero h ){
		ArrayList<Integer> list = new ArrayList<>();
		for (int c = 0; c < l.length(); c++){
			if (l.isRoomFloor(c) && l.distance(c, h.pos) <= MazeTower.O_SLAM_RADIUS) list.add(c);
		}
		if (list.isEmpty()) return false;
		set(SLAM, list);
		warnLeft = MazeTower.O_SLAM_WARN - 1;
		if (m.sprite != null) m.sprite.showStatus(0xE0952A, Integer.toString(MazeTower.O_SLAM_WARN));
		echoCue(l, HeartValve.SAP);
		showFx();
		return true;
	}

	private void fireSlam(){
		ArrayList<Integer> list = list();
		tap(Dungeon.hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 0.8f);
		PixelScene.shake(1, 0.15f);
		for (int c : list){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(GooSprite.GooParticle.FACTORY, 4);
			Char ch = Actor.findChar(c);
			if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.O_SLAM_MIN, MazeTower.O_SLAM_MAX), 1f, "ondeath_slam");
		}
		//수액 웅덩이 (불이 이어진 웅덩이를 따라 번진다)
		ArrayList<Integer> free = new ArrayList<>(list);
		for (int i = 0; i < MazeTower.O_SLAM_PUDDLES && !free.isEmpty(); i++){
			int c = free.remove(Random.Int(free.size()));
			GameScene.add(Blob.seed(c, 1, SapFloor.class));
		}
	}

	// ----- 끌어내기 (등불 판막, 10층 간수와 같음): 금빛 사슬 조준선 → 줄 위의 첫 대상이 주인공이면 피해 + 주인 앞까지 끌려와 속박 -----
	private boolean startPull( MasterBossLevel l, Hero h ){
		if (l.distance(m.pos, h.pos) < MazeTower.I8_PULL_MIN_DIST) return false;
		Ballistica aim = new Ballistica(m.pos, h.pos, Ballistica.PROJECTILE);
		if (aim.collisionPos != h.pos) return false;
		Ballistica line = new Ballistica(m.pos, h.pos, Ballistica.STOP_SOLID);
		ArrayList<Integer> list = new ArrayList<>();
		for (int c : line.subPath(1, line.dist)){
			if (l.solid[c]) break;
			list.add(c);
		}
		if (list.isEmpty()) return false;
		set(PULL, list);
		echoCue(l, HeartValve.LANTERN);
		showFx();
		return true;
	}

	//끌어내기는 사슬이 닿을 때까지 주인의 차례가 끝나지 않는다 (true면 TowerMaster가 기다림)
	private boolean firePull(){
		ArrayList<Integer> list = list();
		int end = list.get(list.size() - 1);
		Char hit = null;
		int hitIdx = -1;
		for (int i = 0; i < list.size(); i++){
			Char ch = Actor.findChar(list.get(i));
			if (ch != null && ch != m){ hit = ch; hitIdx = i; end = list.get(i); break; }
		}
		Sample.INSTANCE.play(Assets.Sounds.CHAINS);
		if (hit != Dungeon.hero){
			if (m.sprite != null && m.sprite.parent != null){
				m.sprite.parent.add(new Chains(m.sprite.center(), com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(end),
						Effects.Type.CHAIN, null).speed(MazeTower.I8_CHAIN_SPEED));
			}
			return false;
		}
		final Hero h = Dungeon.hero;
		int to = h.pos;
		for (int i = 0; i < hitIdx; i++){
			int c = list.get(i);
			if (!Dungeon.level.solid[c] && !Dungeon.level.pit[c] && Actor.findChar(c) == null){ to = c; break; }
		}
		final int pullTo = to;
		h.interrupt();
		if (m.sprite == null || h.sprite == null || m.sprite.parent == null){
			pullHit(h, pullTo);
			return false;
		}
		m.sprite.parent.add(new Chains(m.sprite.center(), h.sprite.destinationCenter(), Effects.Type.CHAIN, new Callback() {
			@Override
			public void call() {
				pullHit(h, pullTo);
				m.skillDone();
			}
		}).speed(MazeTower.I8_CHAIN_SPEED));
		return true;
	}

	private void pullHit( final Hero h, final int pullTo ){
		if (!h.isAlive()) return;
		hurt(h, Random.NormalIntRange(MazeTower.I8_PULL_DMG_MIN, MazeTower.I8_PULL_DMG_MAX), 0f, "ondeath_pull");
		if (!h.isAlive()) return;
		if (pullTo != h.pos && h.sprite != null){
			Actor.add(new Pushing(h, h.pos, pullTo, new Callback() {
				@Override
				public void call() {
					h.pos = pullTo;
					h.sprite.place(pullTo);
					Dungeon.level.occupyCell(h);
					if (h.isAlive()) Buff.prolong(h, Roots.class, MazeTower.I8_PULL_ROOT);
					Dungeon.observe();
					GameScene.updateFog();
				}
			}));
		} else {
			Buff.prolong(h, Roots.class, MazeTower.I8_PULL_ROOT);
		}
	}

	// ----- 인간을 죽이는 마법 (망령 왕 판, 20층과 같음): 8방향 중 주인공 쪽, 폭 3(대각선 5) 범위 경고 → 광선 -----
	private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
	private static final int[] DY = {0, 1, 1, 1, 0, -1, -1, -1};

	private boolean startZolt( MasterBossLevel l, Hero h ){
		int w = l.width();
		double a = Math.atan2(h.pos / w - m.pos / w, h.pos % w - m.pos % w);
		dir = (int)Math.floorMod(Math.round(a / (Math.PI / 4)), 8L);
		ArrayList<Integer> ln = new ArrayList<>();
		ArrayList<Integer> list = zoltCells(m.pos, dir, ln);
		if (list.isEmpty()) return false;
		set(ZOLT, list);
		lanes = new int[ln.size()];
		for (int i = 0; i < ln.size(); i++) lanes[i] = ln.get(i);
		echoCue(l, HeartValve.DARK);
		if (l.heroFOV[m.pos]) GLog.n(Messages.get(TowerMaster.class, "zolt_warn"));
		showFx();
		return true;
	}

	//20층 망령 왕의 졸트라크 범위와 같은 계산 (WraithKing.zoltCells)
	private static ArrayList<Integer> zoltCells( int from, int d, ArrayList<Integer> lanes ){
		ArrayList<Integer> out = new ArrayList<>();
		int w = Dungeon.level.width();
		int x0 = from % w, y0 = from / w;
		int dx = DX[d], dy = DY[d];
		int reach = (dx != 0 && dy != 0) ? 2 : 1;
		for (int k = -reach; k <= reach; k++){
			int ox, oy;
			if (k == 0){ ox = 0; oy = 0; }
			else if (dx != 0 && dy != 0){ if (k < 0){ ox = dx * -k; oy = 0; } else { ox = 0; oy = dy * k; } }
			else { ox = -dy * k; oy = dx * k; }
			for (int i = 1; i < 40; i++){
				int x = x0 + ox + dx * i, y = y0 + oy + dy * i;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) break;
				int c = x + y * w;
				if (Dungeon.level.solid[c]) break;
				out.add(c);
				lanes.add(k);
			}
		}
		if (dx != 0 && dy != 0){
			java.util.HashSet<Integer> set = new java.util.HashSet<>(out);
			set.add(from);
			int[] pA = dx * dy < 0 ? new int[]{1, 0} : new int[]{-1, 0};
			int[] pB = {0, 1};
			for (int i = out.size() - 1; i >= 0; i--){
				int c = out.get(i);
				boolean p1 = set.contains(c + pA[0] + pA[1] * w) || set.contains(c + pB[0] + pB[1] * w);
				boolean n1 = set.contains(c - pA[0] - pA[1] * w) || set.contains(c - pB[0] - pB[1] * w);
				if (!p1 && !n1){ out.remove(i); lanes.remove(i); }
			}
		}
		return out;
	}

	private void fireZolt(){
		ArrayList<Integer> list = list();
		zap(Dungeon.hero.pos);
		GameScene.groupOverFog(new com.shatteredpixel.shatteredpixeldungeon.effects.ZoltBeamFx(list, origin, DX[dir], DY[dir]));
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		PixelScene.shake(1, 0.15f);
		for (int c : list){
			Char ch = Actor.findChar(c);
			if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.WK_ZOLT_MIN, MazeTower.WK_ZOLT_MAX), 1f - MazeTower.WK_ZOLT_ARMOR_IGNORE, "ondeath_zolt");
		}
	}

	// ----- 서리창·화염창 (여제 판, 번갈아, 20층 여제와 같음) -----
	//(사용자) 대기 없이 날아오지 않게: 창을 부르면 주인 머리 위에 창이 떠서 3~4턴(EM_SPEAR_DELAY_*) 센다(보스 체력 바 밑 아이콘 + 머리 위 숫자).
	//날아오기 2턴 전(EM_SPEAR_AIM_TURNS)부터 창이 주인공을 겨누고 점선 조준선·발밑 고리(SpearAimFx, MasterSprite). 0이 되면 던지고, 닿는 순간 피해 + 서리 혹한 / 화염 화상.
	//머리 위 창은 다른 기술과 함께 세어진다(던지는 턴이 그 턴의 기술). 여제 판막이 깨지면 사라진다. 주인공이 투명이면 던지지 않고 기다린다
	public int spearType = 0;   //0 없음, 1 서리, 2 화염
	public int spearTimer = 0;
	public int spearMax = 4;
	private int nextSpear = 1;

	private boolean startElem( MasterBossLevel l, Hero h ){
		if (spearType != 0) return false;
		spearType = nextSpear;
		nextSpear = nextSpear == 1 ? 2 : 1;
		spearTimer = Random.IntRange(MazeTower.EM_SPEAR_DELAY_MIN, MazeTower.EM_SPEAR_DELAY_MAX);
		spearMax = spearTimer;
		Buff.affect(m, TowerMaster.SpearCountdown.class);
		echoCue(l, HeartValve.PETAL);
		if (l.heroFOV[m.pos]) GLog.w(Messages.get(TowerMaster.class, spearType == 1 ? "frost_spear" : "fire_spear"));
		showSpearCount();
		return true;
	}

	//창이 다음 주인 턴에 날아가는 상태 (조준선을 그린다)
	public boolean spearArmed(){
		Hero h = Dungeon.hero;
		return spearType != 0 && spearTimer <= MazeTower.EM_SPEAR_AIM_TURNS && m.isAlive()
				&& h != null && h.isAlive() && h.invisible <= 0 && h.sprite != null;
	}

	//주인 턴마다 (tick 다음): 0이 되면 던진다. 반환: 던졌으면 true (그 턴의 기술)
	//(사용자) 남은 턴은 주인의 모든 턴마다 줄어든다 (다른 기술을 터뜨리거나 힘을 모으는 턴, 해일·수리·열매 턴에도). TowerMaster.act 맨 앞에서
	public void spearTick( MasterBossLevel l ){
		if (spearType == 0) return;
		if (!alive(l, HeartValve.PETAL)){ cancelSpear(); return; }
		if (spearTimer > 0) spearTimer--;
		if (spearTimer > 0 || Dungeon.hero.invisible > 0) showSpearCount();
	}

	//0이 되면 던진다 (그 턴의 기술). 다른 기술이 먼저 터진 턴이면 다음 비는 턴에
	public boolean spearTurn( MasterBossLevel l ){
		if (spearType == 0) return false;
		if (!alive(l, HeartValve.PETAL)){ cancelSpear(); return false; }
		if (spearTimer <= 0 && Dungeon.hero.invisible <= 0){
			throwSpear();
			return true;
		}
		return false;
	}

	public void cancelSpear(){
		spearType = 0;
		Buff.detach(m, TowerMaster.SpearCountdown.class);
	}

	//남은 턴을 머리 위에 (조준하는 동안은 창 옆에)
	private void showSpearCount(){
		if (spearType == 0 || m.sprite == null || !Dungeon.level.heroFOV[m.pos]) return;
		int left = Math.max(0, spearTimer);
		int color = spearType == 1 ? 0x88CCFF : 0xFF8844;
		if (spearArmed()){
			com.watabou.utils.PointF c = m.sprite.center();
			boolean heroLeft = Dungeon.hero.sprite != null && Dungeon.hero.sprite.center().x < c.x;
			com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.show(c.x + (heroLeft ? 12 : -12), m.sprite.y - 6, left > 0 ? Integer.toString(left) : "!", color);
			return;
		}
		m.sprite.showStatus(color, left > 0 ? Integer.toString(left) : "!");
	}

	//20층 여제의 창 던지기와 같음: 주인공 쪽으로 날아가 벽에 막히면 벽면에서 터지고, 주인공에게 닿는 순간 피해
	private void throwSpear(){
		final int type = spearType;
		cancelSpear();
		setMul(HeartValve.PETAL);
		Ballistica b = new Ballistica(m.pos, Dungeon.hero.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);
		int end = b.collisionPos;
		boolean blocked = Dungeon.level.solid[end];
		if (blocked) end = b.dist >= 1 ? b.path.get(b.dist - 1) : m.pos;
		final boolean hitHero = !blocked && end == Dungeon.hero.pos;
		com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx fly = type == 1 ? com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_FROST_FLY : com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_FIRE_FLY;
		final com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx hit = type == 1 ? com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_FROST_HIT : com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_FIRE_HIT;
		com.watabou.utils.PointF a = m.sprite != null ? new com.watabou.utils.PointF(m.sprite.center().x, m.sprite.y - 2) : com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(m.pos);
		com.watabou.utils.PointF z = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(end);
		if (blocked){
			com.watabou.utils.PointF o = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(m.pos);
			com.watabou.utils.PointF t = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(Dungeon.hero.pos);
			com.watabou.utils.PointF wl = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileToWorld(b.collisionPos);
			com.watabou.utils.PointF e = rayEnter(o, new com.watabou.utils.PointF(t.x - o.x, t.y - o.y), wl.x, wl.y, wl.x + 16, wl.y + 16);
			if (e != null) z = e;
			else { com.watabou.utils.PointF p1 = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(end), p2 = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileCenterToWorld(b.collisionPos); z = new com.watabou.utils.PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f); } //(점검) 여제처럼 벽면 가운데로
		}
		final com.watabou.utils.PointF hitAt = z;
		zap(Dungeon.hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);
		//(점검) 창이 닿는 순간 피해를 주되, 그동안 주인의 차례를 넘기지 않는다 (예전에는 차례가 먼저 넘어가 그림 쪽에서 피해가 들어갔다)
		final Runnable land = new Runnable(){ @Override public void run(){
			if (!hitHero) return;
			Hero h = Dungeon.hero;
			if (h == null || !h.isAlive()) return;
			hurt(h, Random.NormalIntRange(MazeTower.EM_SPEAR_MIN, MazeTower.EM_SPEAR_MAX), 0f, "ondeath_elem");
			if (!h.isAlive()) return;
			if (type == 1) Buff.affect(h, Frostbite.class, MazeTower.EM_SPEAR_FROSTBITE);
			else Buff.affect(h, Burning.class).reignite(h);
		}};
		spearWait = false;
		if (m.sprite == null || m.sprite.parent == null){ land.run(); return; }
		final com.watabou.noosa.MovieClip mc = com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.clip(fly, 12, true);
		mc.origin.set(fly.w - 3, fly.h / 2f);
		mc.x = a.x - (fly.w - 3);
		mc.y = a.y - fly.h / 2f;
		mc.angle = (float)(Math.atan2(z.y - a.y, z.x - a.x) * 180 / Math.PI);
		GameScene.effect(mc);
		if (mc.parent == null){ land.run(); return; }
		spearWait = true;
		mc.parent.add(new com.watabou.noosa.tweeners.PosTweener(mc, new com.watabou.utils.PointF(z.x - (fly.w - 3), z.y - fly.h / 2f), 0.25f){
			@Override
			protected void onComplete() {
				mc.killAndErase();
				com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.at(hit, hitAt.x, hitAt.y, 12, false);
				land.run(); //피해·상태이상은 창이 주인공에게 닿는 순간에
				m.skillDone();
			}
		});
	}

	//창을 던진 턴: 창이 닿을 때까지 주인의 차례를 기다리는지
	public boolean spearWait = false;

	//o에서 d 방향으로 뻗은 선이 네모에 처음 들어가는 점 (MazeEmpress.rayEnter와 같음)
	private static com.watabou.utils.PointF rayEnter( com.watabou.utils.PointF o, com.watabou.utils.PointF d, float minX, float minY, float maxX, float maxY ){
		float tmin = 0, tmax = Float.MAX_VALUE;
		float[] os = {o.x, o.y}, ds = {d.x, d.y}, mins = {minX, minY}, maxs = {maxX, maxY};
		for (int i = 0; i < 2; i++){
			if (Math.abs(ds[i]) < 1e-6f){
				if (os[i] < mins[i] || os[i] > maxs[i]) return null;
			} else {
				float t1 = (mins[i] - os[i]) / ds[i], t2 = (maxs[i] - os[i]) / ds[i];
				tmin = Math.max(tmin, Math.min(t1, t2));
				tmax = Math.min(tmax, Math.max(t1, t2));
			}
		}
		if (tmax < tmin) return null;
		return new com.watabou.utils.PointF(o.x + d.x * tmin, o.y + d.y * tmin);
	}


	// ===================== 예고한 기술 터뜨리기 =====================

	//예고한 기술을 터뜨린다. 반환: 연출(사슬)이 끝날 때까지 주인의 차례를 기다려야 하면 true
	//예고한 기술이 아직 기다리는 중이면 남은 턴을 하나 줄이고 true (주인은 그 턴 힘을 모은다)
	public boolean waiting(){
		if (pending == NONE || warnLeft <= 0) return false;
		if (m.sprite != null && Dungeon.level.heroFOV[m.pos]) m.sprite.showStatus(pending == LEAFWALK ? MazeTower.O_LEAF_WARN_COLOR : 0xE0952A, Integer.toString(warnLeft));
		warnLeft--;
		return true;
	}

	public boolean resolve(){
		int p = pending;
		clearFx();
		boolean wait = false;
		setMul(p == SLAM ? HeartValve.SAP : p == PULL ? HeartValve.LANTERN : p == ZOLT ? HeartValve.DARK : -1);
		switch (p){
			case ROOT_SPEAR: wait = fireSpear(); break;
			case FAN:        wait = fireFan(); break;
			case SLAM:       fireSlam(); break;
			case PULL:       wait = firePull(); break;
			case ZOLT:       fireZolt(); break;
			case WHIP:       wait = fireWhip(); break;
			case LEAFWALK:   wait = fireLeafWalk(); break;
			case WIND:       wait = fireWind(); break;
		}
		pending = NONE;
		warnLeft = 0;
		cells = null; lanes = null;
		return wait;
	}

	// ===================== (O6) 3막 =====================

	//이번 기술의 피해 배율: 흡수한 판막의 메아리 기술이면 O_ABSORB_MUL, 주인이 먹은 열매가 남아 있으면 O_FRUIT_MASTER_MUL (기술 하나마다 하나씩 씀)
	private float mul = 1f;

	private void setMul( int valveKind ){
		mul = 1f;
		if (valveKind >= 0 && Dungeon.level instanceof MasterBossLevel){
			HeartValve v = ((MasterBossLevel) Dungeon.level).valveOf(valveKind);
			if (v != null && v.absorbed) mul *= MazeTower.O_ABSORB_MUL;
		}
		if (m.buff(TowerMaster.MasterFruit.class) != null) mul *= MazeTower.O_FRUIT_DMG; //(사용자) 주인이 세계수의 열매를 먹은 동안
	}

	public void beginAct3(){
		cdTide = MazeTower.O_TIDE_FIRST;
		tideBand = -1;
	}

	// ----- 뿌리 해일: 가장자리에서 좁혀 오는 고리 셋 (줄기 둘레 O_TIDE_BANDS[2] 안은 늘 안전) -----
	//고리마다 O_TIDE_WARN턴 경고(붉은 칸) → 그 고리에 바깥 칸부터 검은 뿌리가 솟고, 같은 때 다음 고리 경고. 그동안 주인은 새 기술 없이 걷기·평타만
	private int cdTide = 99;
	public int tideBand = -1;   //경고 중인 고리 (0 바깥 ~ 2 안쪽), -1이면 해일 없음
	private int tideWarn = 0;
	private Group tideFx;

	private static ArrayList<Integer> tideCells( MasterBossLevel l, int band ){
		ArrayList<Integer> out = new ArrayList<>();
		int w = l.width(), sx = MasterBossLevel.SAPLING % w, sy = MasterBossLevel.SAPLING / w;
		float in = MazeTower.O_TIDE_BANDS[band], outR = band == 0 ? 99f : MazeTower.O_TIDE_BANDS[band - 1];
		for (int c = 0; c < l.length(); c++){
			if (!l.isRoomFloor(c)) continue;
			float dx = c % w - sx, dy = c / w - sy, d2 = dx * dx + dy * dy;
			if (d2 > in * in + 0.01f && d2 <= outR * outR + 0.01f) out.add(c);
		}
		return out;
	}

	private boolean startTide( MasterBossLevel l ){
		tideBand = 0;
		tideWarn = MazeTower.O_TIDE_WARN;
		zap(MasterBossLevel.SAPLING);
		GLog.w(Messages.get(TowerMaster.class, "tide_warn"));
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 0.6f);
		PixelScene.shake(1, 0.2f);
		showTide();
		return true;
	}

	public void showTide(){
		if (tideFx != null){ ((WormSpawn.ChargeWarn) tideFx).fadeOut(); tideFx = null; }
		if (tideBand < 0 || !(Dungeon.level instanceof MasterBossLevel)) return;
		tideFx = WormSpawn.ChargeWarn.show(tideCells((MasterBossLevel) Dungeon.level, tideBand));
	}

	//주인 턴마다: -1 해일 없음·아직 기다림 / 0 고리가 솟음 / 1 고리가 솟는 연출을 기다림
	public int tideTurn(){
		if (tideBand < 0 || !(Dungeon.level instanceof MasterBossLevel)) return -1;
		if (--tideWarn > 0) return -1;
		MasterBossLevel l = (MasterBossLevel) Dungeon.level;
		ArrayList<Integer> cells = tideCells(l, tideBand);
		setMul(-1);
		int w = l.width(), sx = MasterBossLevel.SAPLING % w, sy = MasterBossLevel.SAPLING / w;
		float far = MazeTower.O_TIDE_BANDS[0] + 2f;
		TimedHits hits = new TimedHits();
		for (final int c : cells){
			float dx = c % w - sx, dy = c / w - sy;
			float delay = Math.max(0f, (far - (float)Math.sqrt(dx * dx + dy * dy))) * 0.05f;
			GameScene.effect(new com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx(c, com.shatteredpixel.shatteredpixeldungeon.effects.MasterSpikeFx.ROOT, delay, false));
			hits.add(delay + 1f / MazeTower.O_SPIKE_FPS, new Runnable(){ @Override public void run(){
				if (Dungeon.level.heroFOV[c] && Random.Int(3) == 0) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 1);
				Char ch = Actor.findChar(c);
				if (ch != null) hurt(ch, Random.NormalIntRange(MazeTower.O_TIDE_MIN, MazeTower.O_TIDE_MAX), 1f, "ondeath_tide");
			}});
		}
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		PixelScene.shake(1, 0.15f);
		tideBand++;
		if (tideBand < MazeTower.O_TIDE_BANDS.length){
			tideWarn = MazeTower.O_TIDE_WARN;
		} else {
			tideBand = -1;
			cdTide = MazeTower.O_TIDE_CD;
		}
		showTide();
		return hits.start() ? 1 : 0;
	}

	public void cancelTide(){
		tideBand = -1;
		if (tideFx != null){ tideFx.killAndErase(); tideFx = null; }
	}

	// ===================== 도움 =====================

	private void set( int kind, ArrayList<Integer> list ){
		pending = kind;
		origin = m.pos;
		cells = new int[list.size()];
		for (int i = 0; i < list.size(); i++) cells[i] = list.get(i);
	}

	private ArrayList<Integer> list(){
		ArrayList<Integer> out = new ArrayList<>();
		if (cells != null) for (int c : cells) out.add(c);
		return out;
	}

	//주인의 기술이 다치게 하는 대상: 주인공과 그 편 (판막·등불·기습 눈·주인 편 몬스터는 아님)
	private boolean target( Char ch ){
		return ch != m && ch.isAlive() && ch.alignment != Char.Alignment.ENEMY
				&& !(ch instanceof HeartValve) && !(ch instanceof MemoryLamp) && !(ch instanceof WraithEyes);
	}

	//피해: armorFrac = 방어력을 얼마나 적용할지 (1 = 다, 0 = 무시)
	private void hurt( Char ch, int dmg, float armorFrac, String deathKey ){
		if (!target(ch)) return;
		dmg = Math.round(dmg * mul); //(O6) 흡수한 판막의 메아리 기술·주인이 먹은 열매
		if (armorFrac >= 1f){
			//(사용자) 방어를 전혀 무시하지 않는 기술은 피해의 일부(O_SKILL_CHIP, 최대 O_SKILL_CHIP_MAX)가 방어를 무시하고 박힌다: 방어력이 높아도 조금은 들어간다
			int chip = Math.min(MazeTower.O_SKILL_CHIP_MAX, Math.round(dmg * MazeTower.O_SKILL_CHIP));
			dmg = chip + Math.max(0, dmg - chip - ch.drRoll());
		} else if (armorFrac > 0) dmg = Math.max(0, dmg - Math.round(ch.drRoll() * armorFrac));
		ch.damage(dmg, m);
		if (ch.sprite != null) ch.sprite.flash();
		if (ch == Dungeon.hero && !ch.isAlive()){
			Dungeon.fail(m);
			GLog.n(Messages.get(TowerMaster.class, deathKey));
		}
	}

	// ===================== 예고 그림 =====================

	//예고 그림을 (다시) 만든다. 불러온 뒤에도 (TowerMaster.restoreFx)
	public void showFx(){
		clearFx();
		if (pending == NONE || cells == null) return;
		ArrayList<Integer> list = list();
		switch (pending){
			case ROOT_SPEAR: //13·14층 땅울림과 같은 금
				fx = new com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GroundTremor.CrackFx(list, dir);
				GameScene.groupEffect(fx);
				break;
			case FAN: //새끼 굴벌레 돌진과 같은 은은한 붉은 칸
			case WHIP:
				fx = WormSpawn.ChargeWarn.show(list);
				break;
			case LEAFWALK: //(O5) 빛바랜 낙엽빛 칸
			case WIND:
				leafWarn(list);
				break;
			case SLAM: //호박빛 칸
				fx = com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx.CellWarn.show(list, new float[]{ MazeTower.O_SLAM_WARN_ALPHA }, 0xE0952A, 0); //(사용자) 너무 진하지 않게
				break;
			case PULL: //10층 간수와 같은 금빛 사슬 조준선
				fx = new com.shatteredpixel.shatteredpixeldungeon.effects.WardenFx.ChainLine(origin, list.get(list.size() - 1), 0xE8C060);
				GameScene.groupOverFog(fx);
				break;
			case ZOLT: { //20층 망령 왕과 같은 범위 (회색빛 테두리)
				java.util.LinkedHashMap<Integer, Integer> lm = new java.util.LinkedHashMap<>();
				for (int i = 0; i < cells.length; i++) lm.put(cells[i], lanes != null && i < lanes.length ? lanes[i] : 0);
				fx = new com.shatteredpixel.shatteredpixeldungeon.effects.CellZoneFx(com.shatteredpixel.shatteredpixeldungeon.effects.CellZoneFx.Mode.WARN, lm, origin);
				GameScene.groupOverFog(fx);
				break;
			}
		}
	}

	public void clearFx(){
		if (fx != null){
			if (fx instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GroundTremor.CrackFx) ((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GroundTremor.CrackFx) fx).fadeOut();
			else if (fx instanceof WormSpawn.ChargeWarn) ((WormSpawn.ChargeWarn) fx).fadeOut();
			else if (fx instanceof com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx.CellWarn) ((com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx.CellWarn) fx).fadeOut();
			else fx.killAndErase();
			fx = null;
		}
	}

	public void cancel(){
		clearFx();
		pending = NONE;
		warnLeft = 0;
		cells = null; lanes = null;
	}

	// ===================== 저장 =====================

	public void store( Bundle b ){
		b.put("ms_pending", pending);
		b.put("ms_warn_left", warnLeft);
		if (cells != null) b.put("ms_cells", cells);
		if (lanes != null) b.put("ms_lanes", lanes);
		b.put("ms_dir", dir);
		b.put("ms_spear", new int[]{spearType, spearTimer, spearMax, nextSpear});
		b.put("ms_origin", origin);
		b.put("ms_cd", new int[]{cdSpear, cdFan, cdSlam, cdPull, cdZolt, cdElem, cdWalk, cdWind, cdTide, tideBand, tideWarn});
	}

	public void restore( Bundle b ){
		if (!b.contains("ms_pending")) return;
		pending = b.getInt("ms_pending");
		warnLeft = b.getInt("ms_warn_left");
		cells = b.contains("ms_cells") ? b.getIntArray("ms_cells") : null;
		lanes = b.contains("ms_lanes") ? b.getIntArray("ms_lanes") : null;
		dir = b.getInt("ms_dir");
		if (b.contains("ms_spear")){ int[] sp = b.getIntArray("ms_spear"); spearType = sp[0]; spearTimer = sp[1]; spearMax = sp[2]; nextSpear = sp[3] == 0 ? 1 : sp[3]; }
		origin = b.getInt("ms_origin");
		int[] cd = b.getIntArray("ms_cd");
		if (cd != null && cd.length >= 6){ cdSpear = cd[0]; cdFan = cd[1]; cdSlam = cd[2]; cdPull = cd[3]; cdZolt = cd[4]; cdElem = cd[5]; }
		if (cd != null && cd.length >= 8){ cdWalk = cd[6]; cdWind = cd[7]; }
		if (cd != null && cd.length >= 11){ cdTide = cd[8]; tideBand = cd[9]; tideWarn = cd[10]; }
	}
}
