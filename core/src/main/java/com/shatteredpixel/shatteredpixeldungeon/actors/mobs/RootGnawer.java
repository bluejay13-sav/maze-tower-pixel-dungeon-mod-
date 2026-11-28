/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GroundTremor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (K2~K4): 15층 보스 "뿌리를 갉는 것". 마석을 먹고 방 하나만큼 커진 광산 벌레 (샌드웜 모티브).
//땅속(UNDER): 보스층 벽 속 대기 칸(GnawerBossLevel.WAIT_CELL)에 있고 그림은 보이지 않는다. 겨누거나 때릴 수 없다(무적).
//땅속에서 기술 하나를 고른다(한 턴에 하나): 첫 행동은 솟구치기, 그 뒤 갉기(K_GNAW_CD마다) > 땅속 질주(K_DASH_CD마다) > 솟구치기.
//(K2) 솟구치기(WARN → ABOVE): 주인공 칸 둘레에 둥근 경고 + 남은 턴 숫자 → 그 안의 모두에게 피해(경고 칸 = 맞는 칸), 벌레는 경고 가운데로 솟는다.
//  그 뒤 K_ABOVE_TURNS턴 머리가 땅 위에 있다(딜 구간, 옆 칸이면 문다). 솟은 직후 K_FRESH_TURNS턴은 받는 피해 ×K_FRESH_MULTI.
//(K3) 땅속 질주(DASH): 주인공을 지나는 가로·세로 한 줄(방 끝에서 끝까지)에 땅울림과 같은 금(예고) → 다음 턴에 그 줄을 지나가며 줄 위의 모두에게 피해 + 옆으로 한 칸 밀침.
//  지나간 줄은 굴 자국(GnawerBossLevel.trail)으로 남는다. 물을 지나면 젖는다.
//(K4) 광맥 갉기(GNAW): 광맥 기둥 왼쪽이나 오른쪽 칸에서 솟아 K_GNAW_TURNS턴 갉는다(광맥이 한 턴에 한 단계씩 뜯겨 나가고 몸이 더 굽음, 남은 턴 숫자).
//  갉는 동안 다른 기술은 없다. 머리가 최대 체력의 K_GNAW_BREAK만큼 피해를 받거나, 밀쳐지거나(밀리지는 않고 버티는 동작), 이식 마법봉에 맞거나, 그 광맥이 먼저 부서지면 끊긴다(그 뒤 딜 구간).
//  다 먹으면 등에 마석 껍질 한 겹(받는 피해 -K_SHELL_REDUCE, 최대 K_SHELL_MAX, 아이콘) → 마력 토사(SPIT): 주인공 쪽 부채꼴 경고 → 다음 턴 피해 + 마력 증기.
//마력 증기·마력독에 면역. 어떤 밀침에도 밀리지 않는다. 쓰러지면 원작 DM-300처럼 배지·점수·금속 조각, 보스층이 열린다.
//그림: sprites/maze_b3_gnawer.png (24x24 x 23, 줄 0 껍질 / 줄 1 탈피한 속살, 원본 docs/sprites/mobs/k_draft/make_gnawer.ps1)
public class RootGnawer extends Mob implements com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar.PhaseMarks {

	//Maze Tower (사용자): 보스 체력 바 페이즈 눈금 — 탈피(K_MOLT_AT)
	@Override public float[] phaseMarks(){ return new float[]{ MazeTower.K_MOLT_AT }; }
	@Override public int turnsLeft(){ return -1; }

	{
		spriteClass = GnawerSprite.class;

		HP = HT = MazeTower.K_HP;
		EXP = 30;
		defenseSkill = MazeTower.K_DEFENSE;

		state = HUNTING;
		alignment = Alignment.ENEMY;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);

		immunities.add(ManaVapor.class);
		immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SpitVapor.class);
		immunities.add(ManaVapor.VaporBite.class);
	}

	public enum Phase { UNDER, WARN, DASH, GNAW, SPIT, ABOVE, COLLAPSE, SWEEP, MULTI }
	public Phase phase = Phase.UNDER;
	private int timer = 1;            //지금 단계의 남은 턴
	private int fresh = 0;            //솟은 직후 받는 피해가 큰 남은 턴
	private int warnCenter = -1;
	private ArrayList<Integer> warnCells = new ArrayList<>();
	private int dashDir = 0;          //질주 방향: 0 가로, 1 세로
	private int gnawVein = -1;        //갉는 광맥 (actor id)
	private int gnawDmg = 0;          //갉는 동안 머리가 받은 피해
	private int shell = 0;            //마석 껍질 겹
	private int gnawCD = 0, dashCD = 0;
	private boolean started = false;  //첫 행동을 했는지
	//(K5·K6) 2단계
	private boolean phase2 = false, moltPending = false;
	private int collapseCD = 0, sweepCD = 0, multiCD = 0;
	private int sweepDir = 0;         //꼬리 휩쓸기 띠: 0 가로, 1 세로
	private int sweepLine = -1;       //띠 가운데 줄(가로면 y, 세로면 x)
	private int lastP2 = -1;          //바로 전에 쓴 2단계 기술 (0 굴 폭파, 1 굴착, 2 연속 솟구치기)
	private int digsSinceCollapse = 99; //(사용자) 지난 굴 폭파 뒤 굴착 횟수. 굴 폭파를 다시 쓰려면 2번 이상 (처음 한 번은 1단계 질주의 굴로 바로 쓸 수 있음)

	private WormSpawn.ChargeWarn warnFx;
	private GroundTremor.CrackFx crackFx;

	@Override public int damageRoll() { return Random.NormalIntRange(MazeTower.K_MELEE_MIN, MazeTower.K_MELEE_MAX); }
	@Override public int attackSkill( Char target ) { return MazeTower.K_ATTACK; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, MazeTower.K_ARMOR_MAX); }

	private GnawerBossLevel level(){
		return Dungeon.level instanceof GnawerBossLevel ? (GnawerBossLevel) Dungeon.level : null;
	}

	public boolean underground(){ return phase == Phase.UNDER || phase == Phase.WARN || phase == Phase.DASH || phase == Phase.COLLAPSE || phase == Phase.SWEEP || phase == Phase.MULTI; }
	public boolean phase2(){ return phase2; }

	private GnawerSprite gs(){ return sprite instanceof GnawerSprite ? (GnawerSprite) sprite : null; }

	private boolean heroHidden(){
		return Dungeon.hero == null || !Dungeon.hero.isAlive() || Dungeon.hero.invisible > 0;
	}

	@Override
	protected boolean act() {
		if (sprite != null){ sprite.hideAlert(); sprite.hideLost(); }
		if (gnawCD > 0) gnawCD--;
		if (dashCD > 0) dashCD--;
		if (collapseCD > 0) collapseCD--;
		if (sweepCD > 0) sweepCD--;
		if (multiCD > 0) multiCD--;
		if (moltPending){ return molt(); } //탈피 연출이 끝날 때까지 벌레의 차례가 끝나지 않는다
		switch (phase){
			case UNDER:
				//땅속으로 들어가는 동작을 보여 준 뒤에 대기 칸으로 옮긴다
				if (pos != GnawerBossLevel.WAIT_CELL) moveTo(GnawerBossLevel.WAIT_CELL);
				if (--timer <= 0) chooseSkill();
				break;
			case WARN:
				if (--timer > 0) showCount(warnCenter, timer);
				else emerge();
				break;
			case DASH:
				dashStrike();
				break;
			case GNAW:
				gnawTurn();
				break;
			case COLLAPSE:
				if (--timer > 0) showCollapseCount();
				else if (!collapseStrike()) return false; //다 무너지면 차례가 끝난다
				break;
			case SWEEP:
				if (--timer > 0) showSweepCount();
				else sweepStrike();
				break;
			case MULTI:
				multiTurn();
				break;
			case SPIT:
				if (!spitStrike()) return false; //토사가 땅에 닿으면 차례가 끝난다
				break;
			case ABOVE: default:
				if (fresh > 0) fresh--;
				if (--timer <= 0){ burrow(); break; }
				//옆 칸의 주인공을 문다 (머리는 움직이지 않는다)
				Char h = Dungeon.hero;
				if (!heroHidden() && Dungeon.level.adjacent(pos, h.pos)){
					enemy = h;
					return doAttack(h);
				}
				break;
		}
		spend(TICK);
		return true;
	}

	private void moveTo( int cell ){
		pos = cell;
		if (cell != GnawerBossLevel.WAIT_CELL) Dungeon.level.occupyCell(this);
		if (sprite != null) sprite.place(pos);
		Dungeon.observe();
		GameScene.updateFog();
	}

	//땅속에서 다음 기술: 첫 행동은 솟구치기, 그 뒤 갉기 > 땅속 질주 > 솟구치기
	private void chooseSkill(){
		//(사용자) 싸움의 첫 행동은 솟구치기 (방에 들어서자마자 갉지 않는다). 그다음부터 갉기 > 땅속 질주 > 솟구치기
		if (!started){
			if (startWarn()) started = true;
			else timer = 1;
			return;
		}
		//(K6, 사용자) 2단계: 굴 폭파 > 굴착 > 연속 솟구치기 중에서 고르되, 바로 전에 쓴 2단계 기술은 다시 고르지 않는다
		//(연속 솟구치기는 끝나는 데 쿨타임보다 오래 걸려 매번 그것만 나오던 문제)
		if (phase2){
			for (int s : new int[]{0, 1, 2}){
				if (s == lastP2) continue;
				if (s == 0 && collapseCD <= 0 && digsSinceCollapse >= MazeTower.K_DIGS_FOR_COLLAPSE && startCollapse()){ collapseCD = MazeTower.K_COLLAPSE_CD; lastP2 = 0; return; }
				if (s == 1 && sweepCD <= 0 && startSweep()){ sweepCD = MazeTower.K_SWEEP_CD; lastP2 = 1; return; }
				if (s == 2 && multiCD <= 0 && startMulti()){ multiCD = MazeTower.K_MULTI_CD; lastP2 = 2; return; }
			}
		}
		if (gnawCD <= 0 && startGnaw()){
			gnawCD = MazeTower.K_GNAW_CD;
			return;
		}
		if (dashCD <= 0 && startDash()){
			dashCD = MazeTower.K_DASH_CD;
			return;
		}
		if (!startWarn()) timer = 1; //주인공이 숨어 있으면 한 턴 뒤에 다시
	}

	// ===== (K2) 솟구치기 =====

	private boolean startWarn(){
		GnawerBossLevel lv = level();
		if (lv == null || heroHidden()) return false;
		warnCenter = Dungeon.hero.pos;
		warnCells.clear();
		int w = Dungeon.level.width(), r = MazeTower.K_EMERGE_RADIUS;
		for (int dy = -r; dy <= r; dy++){
			for (int dx = -r; dx <= r; dx++){
				int c = warnCenter + dx + dy * w;
				if (Math.abs(c % w - warnCenter % w) != Math.abs(dx)) continue;
				if (lv.isRoomFloor(c)) warnCells.add(c);
			}
		}
		phase = Phase.WARN;
		timer = MazeTower.K_EMERGE_WARN;
		showWarn();
		showCount(warnCenter, timer);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.6f, 0.7f);
		GLog.w(Messages.get(this, "rumble"));
		return true;
	}

	private void showWarn(){
		clearWarn();
		if (!warnCells.isEmpty()) warnFx = WormSpawn.ChargeWarn.show(warnCells);
	}

	private void clearWarn(){
		if (warnFx != null){ warnFx.fadeOut(); warnFx = null; }
		if (crackFx != null){ crackFx.fadeOut(); crackFx = null; }
	}

	//칸 위에 남은 턴 숫자
	private void showCount( int cell, int left ){
		if (cell < 0 || !Dungeon.level.heroFOV[cell]) return;
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		FloatingText.show(p.x, p.y - 6, Integer.toString(Math.max(1, left)), 0xFF8A66);
	}

	private void hurt( Char ch, int min, int max ){
		ch.damage(Math.max(0, Random.NormalIntRange(min, max) - ch.drRoll()), this);
		if (ch == Dungeon.hero && !ch.isAlive()){
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "ondeath"));
		}
	}

	private void emerge(){
		clearWarn();
		GnawerBossLevel lv = level();
		for (int c : warnCells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || !ch.isAlive()) continue;
			if (ch instanceof ManaVein){ ch.die(this); continue; } //(사용자) 솟구치는 자리의 광맥은 그냥 터진다 (마력 증기, 다른 자리에 다시 자람)
			hurt(ch, MazeTower.K_EMERGE_DMG_MIN, MazeTower.K_EMERGE_DMG_MAX);
		}
		//솟을 자리: 경고 가운데, 막혔으면 가장 가까운 빈 칸
		int dest = -1;
		if (lv != null){
			if (lv.freeFloor(warnCenter)) dest = warnCenter;
			int best = Integer.MAX_VALUE;
			if (dest < 0){
				for (int c = 0; c < Dungeon.level.length(); c++){
					if (!lv.freeFloor(c)) continue;
					int d = Dungeon.level.distance(c, warnCenter);
					if (d < best){ best = d; dest = c; }
				}
			}
		}
		warnCells.clear();
		warnCenter = -1;
		if (dest < 0){ phase = Phase.UNDER; timer = MazeTower.K_UNDER_TURNS; return; }
		surface(dest);
		phase = Phase.ABOVE;
		timer = MazeTower.K_ABOVE_TURNS;
		fresh = MazeTower.K_FRESH_TURNS;
	}

	//땅 위로 솟는다 (솟구침 동작)
	private void surface( int dest ){
		moveTo(dest);
		if (gs() != null) gs().emerge();
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(1, 0.15f);
		}
	}

	private void burrow(){
		if (Dungeon.level.heroFOV[pos]) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 4);
		phase = Phase.UNDER;
		timer = MazeTower.K_UNDER_TURNS;
		fresh = 0;
		if (gs() != null) gs().burrow();
		//대기 칸으로는 다음 차례에 옮긴다 (그동안은 무적, 동작만 보임)
	}

	// ===== (K3) 땅속 질주 =====

	private boolean startDash(){
		GnawerBossLevel lv = level();
		if (lv == null || heroHidden()) return false;
		int h = Dungeon.hero.pos, w = Dungeon.level.width();
		ArrayList<Integer> horiz = lineThrough(lv, h, 1), vert = lineThrough(lv, h, w);
		boolean useH = horiz.size() >= 5 && (vert.size() < 5 || Random.Int(2) == 0);
		if (!useH && vert.size() < 5) return false;
		warnCells.clear();
		warnCells.addAll(useH ? horiz : vert);
		dashDir = useH ? 0 : 1;
		phase = Phase.DASH;
		showCrack();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.6f, 0.6f);
		GLog.w(Messages.get(this, "dash_warn"));
		return true;
	}

	//주인공 칸을 지나는 한 줄 (방 바닥이 이어지는 데까지)
	private ArrayList<Integer> lineThrough( GnawerBossLevel lv, int c, int step ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int p = c;
		while (lv.isRoomFloor(p - step) && (step != 1 || (p - step) / w == c / w)) p -= step;
		while (lv.isRoomFloor(p) && (step != 1 || p / w == c / w)){ cells.add(p); p += step; }
		return cells;
	}

	private void showCrack(){
		clearWarn();
		if (warnCells.isEmpty()) return;
		crackFx = new GroundTremor.CrackFx(warnCells, dashDir);
		GameScene.groupFloor(crackFx);
	}

	private void dashStrike(){
		clearWarn();
		GnawerBossLevel lv = level();
		int w = Dungeon.level.width();
		int side = dashDir == 0 ? w : 1;
		boolean seen = false, water = false;
		for (int c : warnCells){
			if (Dungeon.level.heroFOV[c]) seen = true; //돌 부스러기는 굴 자국이 생길 때 (addTrail)
			if (Dungeon.level.water[c]) water = true;
			//굴 자국은 아래에서 줄을 따라 한 칸씩 생긴다 (addTrail)
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || !ch.isAlive() || ch instanceof ManaVein) continue;
			hurt(ch, MazeTower.K_DASH_DMG_MIN, MazeTower.K_DASH_DMG_MAX);
			//옆으로 한 칸 밀침 (빈 칸이 있는 쪽으로)
			if (ch.isAlive() && !ch.properties().contains(Property.IMMOVABLE)){
				ArrayList<Integer> opts = new ArrayList<>();
				for (int s : new int[]{side, -side}){
					int to = c + s;
					if (lv != null && lv.freeFloor(to) && !warnCells.contains(to)) opts.add(to);
				}
				if (!opts.isEmpty()){
					int to = Random.element(opts);
					Actor.add(new Pushing(ch, ch.pos, to));
					ch.pos = to;
					Dungeon.level.occupyCell(ch);
				}
			}
		}
		if (lv != null){
			lv.addTrail(warnCells, dashDir == 0 ? 1 : 2);
			GameScene.groupFloor(new CrestRun(new ArrayList<>(warnCells), dashDir, phase2 ? 1 : 0, MazeTower.K_TRAIL_SWEEP));
		}
		if (water) Buff.affect(this, Wet.class);
		if (seen){
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			PixelScene.shake(1, 0.2f);
		}
		warnCells.clear();
		phase = Phase.UNDER;
		timer = MazeTower.K_UNDER_TURNS;
	}

	// ===== (K4) 광맥 갉기 =====

	private ManaVein gnawTarget(){
		Actor a = Actor.findById(gnawVein);
		return a instanceof ManaVein && ((ManaVein) a).isAlive() ? (ManaVein) a : null;
	}

	private boolean startGnaw(){
		GnawerBossLevel lv = level();
		if (lv == null) return false;
		ArrayList<int[]> opts = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof ManaVein) || !m.isAlive()) continue;
			for (int s : new int[]{-1, 1}){
				int c = m.pos + s;
				if (lv.freeFloor(c)) opts.add(new int[]{m.id(), c});
			}
		}
		if (opts.isEmpty()) return false;
		int[] pick = Random.element(opts);
		gnawVein = pick[0];
		gnawDmg = 0;
		surface(pick[1]);
		ManaVein v = gnawTarget();
		if (sprite != null && v != null) sprite.turnTo(pos, v.pos);
		if (gs() != null) gs().gnawNext(v == null ? 0 : v.gnawStage);
		phase = Phase.GNAW;
		timer = MazeTower.K_GNAW_TURNS;
		if (v != null) showCount(v.pos, timer);
		GLog.w(Messages.get(this, "gnaw"));
		return true;
	}

	private void gnawTurn(){
		ManaVein v = gnawTarget();
		if (v == null){ interruptGnaw(); return; } //광맥이 먼저 부서졌다
		v.gnawStage = Math.min(3, v.gnawStage + 1);
		if (v.sprite instanceof ManaVein.VeinSprite) ((ManaVein.VeinSprite) v.sprite).jolt();
		if (Dungeon.level.heroFOV[v.pos]){
			CellEmitter.get(v.pos).burst(Speck.factory(Speck.ROCK), 3);
			CellEmitter.center(v.pos).burst(Speck.factory(Speck.BLUE_LIGHT), 2);
			Sample.INSTANCE.play(Assets.Sounds.MINE, 0.6f);
		}
		if (gs() != null) gs().gnaw(Math.min(2, v.gnawStage));
		if (--timer > 0){
			showCount(v.pos, timer);
			return;
		}
		//다 먹음: 광맥은 증기 없이 부스러지고(다른 자리에 하나 다시 자람), 등에 마석 껍질 한 겹, 마력 토사
		v.mazeEaten = true;
		v.die(this);
		if (shell < MazeTower.K_SHELL_MAX){
			shell++;
			Buff.affect(this, Shell.class).layers = shell;
			if (sprite != null) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "shell_up"));
		}
		gnawVein = -1;
		startSpit();
	}

	//갉기를 끊는다 (머리 피해·밀침·이식 마법봉·광맥이 먼저 부서짐). 그 뒤 딜 구간
	public void interruptGnaw(){
		if (phase != Phase.GNAW) return;
		gnawVein = -1;
		phase = Phase.ABOVE;
		timer = MazeTower.K_ABOVE_TURNS;
		fresh = MazeTower.K_FRESH_TURNS;
		if (sprite != null) sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "gnaw_broken"));
		if (gs() != null) gs().play(gs().idleAnim());
		GLog.p(Messages.get(this, "gnaw_broken_log"));
	}

	//(사용자) 밀치는 공격: 밀리지 않고 버티는 동작만, 갉기는 끊긴다
	public void onPushed( int toward ){
		if (underground()) return;
		if (gs() != null) gs().resist(pos, toward);
		if (Dungeon.level.heroFOV[pos]) CellEmitter.bottom(pos).burst(Speck.factory(Speck.ROCK), 4);
		interruptGnaw();
	}

	// ===== (K4) 마력 토사 =====

	private void startSpit(){
		GnawerBossLevel lv = level();
		warnCells.clear();
		int w = Dungeon.level.width();
		float dx, dy;
		if (!heroHidden()){
			dx = Dungeon.hero.pos % w - pos % w; dy = Dungeon.hero.pos / w - pos / w;
		} else {
			dx = sprite != null && sprite.flipHorizontal ? -1 : 1; dy = 0;
		}
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		if (len < 0.01f){ dx = 1; dy = 0; len = 1; }
		dx /= len; dy /= len;
		int r = MazeTower.K_SPIT_RANGE;
		for (int oy = -r; oy <= r; oy++){
			for (int ox = -r; ox <= r; ox++){
				if (ox == 0 && oy == 0) continue;
				float d = (float) Math.sqrt(ox * ox + oy * oy);
				if (d > r + 0.5f) continue;
				if ((ox * dx + oy * dy) / d < MazeTower.K_SPIT_COS) continue; //부채꼴 (양옆 약 28°)
				int x = pos % w + ox, y = pos / w + oy;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y * w;
				if (lv != null && lv.isRoomFloor(c)) warnCells.add(c);
			}
		}
		phase = Phase.SPIT;
		showWarn();
		if (sprite != null) sprite.turnTo(pos, pos + Math.round(dx * 2) + Math.round(dy * 2) * w);
		GLog.w(Messages.get(this, "spit_warn"));
	}

	//(사용자) 마력 증기는 뿜은 토사가 땅에 닿을 즈음(조각 절반이 떨어졌을 때)부터, 입에서 가까운 칸부터 먼 칸으로 조금씩 늦게(K_SPIT_WAVE초 간격) 생긴다.
	//그동안 벌레의 차례는 끝나지 않는다(동작이 보일 때)
	private boolean spitStrike(){
		clearWarn();
		if (gs() != null && sprite.visible){
			ArrayList<Integer> near = new ArrayList<>();
			for (int c : warnCells) if (Dungeon.level.trueDistance(pos, c) <= 3.3f) near.add(c);
			if (near.isEmpty()) near.addAll(warnCells);
			gs().spit(near, new com.watabou.utils.Callback() {
				@Override
				public void call() {
					Sample.INSTANCE.play(Assets.Sounds.GAS);
					GameScene.groupEffect(new VaporWave());
				}
			});
			return false;
		}
		for (int c : warnCells) seedVapor(c);
		Sample.INSTANCE.play(Assets.Sounds.GAS);
		finishSpit();
		return true;
	}

	private void seedVapor( int c ){
		if (Dungeon.level.heroFOV[c]) CellEmitter.center(c).burst(Speck.factory(Speck.MANA_VAPOR), 3);
		com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SpitVapor.spit(c, MazeTower.K_SPIT_VAPOR); //(사용자) 한 칸까지만 퍼짐
	}

	private void finishSpit(){
		warnCells.clear();
		phase = Phase.ABOVE;
		timer = MazeTower.K_ABOVE_TURNS;
		fresh = 0;
	}

	//증기가 입에서 가까운 칸부터 차례로 생긴다. 다 생기면 벌레의 차례가 끝난다
	private class VaporWave extends com.watabou.noosa.Group {
		private final ArrayList<Integer> cells = new ArrayList<>(warnCells);
		private float time = 0;
		VaporWave(){
			final int from = pos;
			java.util.Collections.sort(cells, (a, b) -> Float.compare(Dungeon.level.trueDistance(from, a), Dungeon.level.trueDistance(from, b)));
		}
		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float base = cells.isEmpty() ? 0 : Dungeon.level.trueDistance(pos, cells.get(0));
			while (!cells.isEmpty() && (Dungeon.level.trueDistance(pos, cells.get(0)) - base) * MazeTower.K_SPIT_WAVE <= time){
				seedVapor(cells.remove(0));
			}
			if (cells.isEmpty()){
				killAndErase();
				finishSpit();
				spend(TICK);
				next();
			}
		}
	}

	// ===== (K5) 탈피 =====

	//방 가운데 근처로 솟아 몸을 비틀며 껍질을 벗는다: 껍질 조각이 튀고 그림이 푸른 속살로. 마석 껍질 겹은 사라진다
	private boolean molt(){
		moltPending = false;
		phase2 = true;
		clearWarn();
		interruptQuietly();
		GnawerBossLevel lv = level();
		int center = 16 + 17 * Dungeon.level.width(), dest = -1, best = Integer.MAX_VALUE;
		if (lv != null){
			for (int c = 0; c < Dungeon.level.length(); c++){
				if (!lv.freeFloor(c) && c != pos) continue;
				int d = Dungeon.level.distance(c, center);
				if (d < best){ best = d; dest = c; }
			}
		}
		if (dest >= 0 && dest != pos) surface(dest);
		else if (gs() != null){ gs().visible = true; gs().emerge(); }
		shell = 0;
		Buff.detach(this, Shell.class);
		//(사용자) 천천히: 솟은 뒤 몸을 비틀고(약 1.7초) 그다음 번쩍이며 껍질이 튀고 속살로. 다른 동작이 끼어들어 끊기지 않게 그동안 차례를 붙든다
		boolean wait = gs() != null && sprite.visible;
		if (gs() != null) gs().molt(wait ? new com.watabou.utils.Callback() {
			@Override public void call() { spend(TICK); next(); }
		} : null);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		GLog.w(Messages.get(this, "molt"));
		phase = Phase.ABOVE;
		timer = MazeTower.K_ABOVE_TURNS;
		fresh = 0;
		collapseCD = 2; multiCD = 5; sweepCD = 3;
		if (wait) return false;
		spend(TICK);
		return true;
	}

	//탈피·연속 솟구치기를 시작할 때 갉기 등 하던 것을 조용히 그만둔다
	private void interruptQuietly(){
		gnawVein = -1;
		warnCells.clear();
		multiLeft = 0;
	}

	//껍질 조각이 사방으로 튀었다 떨어진다 (흙덩이 조각 그림 0·1번)
	public static class ShellBurstFx extends com.watabou.noosa.Group {
		private final ArrayList<float[]> pieces = new ArrayList<>(); //{시작 x, 시작 y, 목표 x, 목표 y, 걸리는 시간, 높이}
		private final ArrayList<Image> imgs = new ArrayList<>();
		private float time = 0;
		public ShellBurstFx( PointF from ){
			for (int i = 0; i < 18; i++){
				float a = Random.Float((float) Math.PI * 2), r = 10 + Random.Float(18);
				pieces.add(new float[]{ from.x, from.y - 2, from.x + (float) Math.cos(a) * r, from.y + 6 + (float) Math.sin(a) * r * 0.6f, MazeTower.K_MOLT_SHARD_TIME + Random.Float(0.3f), 8 + Random.Float(10) }); //(사용자) 천천히
				Image im = new Image("sprites/maze_fx_gnawer_clods.png", Random.Int(2) * 4, 0, 4, 4);
				add(im); imgs.add(im);
			}
			update();
		}
		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			boolean done = true;
			for (int i = 0; i < pieces.size(); i++){
				float[] p = pieces.get(i);
				float k = Math.min(1f, time / p[4]);
				Image im = imgs.get(i);
				im.x = Math.round(p[0] + (p[2] - p[0]) * k - 2);
				im.y = Math.round(p[1] + (p[3] - p[1]) * k - p[5] * 4 * k * (1 - k) - 2);
				if (time < p[4] + 0.7f){ done = false; im.alpha(time > p[4] ? 1f - (time - p[4]) / 0.7f : 1f); }
				else im.visible = false;
			}
			if (done) killAndErase();
		}
	}

	// ===== (K6) 굴 폭파 =====

	private boolean startCollapse(){
		GnawerBossLevel lv = level();
		if (lv == null) return false;
		int w = Dungeon.level.width(), r = MazeTower.K_COLLAPSE_RADIUS;
		java.util.HashSet<Integer> set = new java.util.HashSet<>();
		for (int c = 0; c < lv.trail.length; c++){
			if (lv.trail[c] == 0) continue;
			for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++){
				int n = c + dx + dy * w;
				if (Math.abs(n % w - c % w) != Math.abs(dx)) continue;
				if (lv.isRoomFloor(n)) set.add(n);
			}
		}
		if (set.isEmpty()) return false;
		warnCells.clear();
		warnCells.addAll(set);
		phase = Phase.COLLAPSE;
		timer = MazeTower.K_COLLAPSE_WARN;
		showWarn();
		showCollapseCount();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 0.6f);
		GLog.w(Messages.get(this, "collapse_warn"));
		return true;
	}

	//남은 턴 숫자: 경고 칸 중 주인공에게 가장 가까운 칸 위에
	private void showCollapseCount(){
		if (warnCells.isEmpty() || Dungeon.hero == null) return;
		int best = warnCells.get(0);
		for (int c : warnCells) if (Dungeon.level.distance(c, Dungeon.hero.pos) < Dungeon.level.distance(best, Dungeon.hero.pos)) best = c;
		showCount(best, timer);
	}

	//(사용자) 한꺼번에 터지지 않고 칸마다 무작위로 조금씩(0~K_COLLAPSE_SPREAD초) 늦게 무너진다. 무너지는 순간 그 칸의 대상이 맞는다(예고 = 실제).
	//다 무너질 때까지 벌레의 차례가 끝나지 않는다
	private boolean collapseStrike(){
		clearWarn();
		GLog.w(Messages.get(this, "collapse"));
		PixelScene.shake(1, 0.3f);
		GameScene.groupEffect(new CollapseWave());
		return false;
	}

	private void collapseCell( int c ){
		if (Dungeon.level.heroFOV[c]){
			//(사용자) 진짜 폭파하듯 촘촘하게: 원작 폭탄의 폭발·연기 입자 + 돌 부스러기
			CellEmitter.center(c).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle.FACTORY, 4); //(사용자) 4개
			CellEmitter.get(c).burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.SmokeParticle.FACTORY, 4);
			CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 6);
			if (Random.Int(3) == 0) Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.5f, 0.9f + Random.Float(0.3f));
			else Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.4f, 0.8f + Random.Float(0.4f));
		}
		Char ch = Actor.findChar(c);
		if (ch == null || ch == this || !ch.isAlive() || ch instanceof ManaVein) return;
		hurt(ch, MazeTower.K_COLLAPSE_DMG_MIN, MazeTower.K_COLLAPSE_DMG_MAX);
	}

	private class CollapseWave extends com.watabou.noosa.Group {
		private final ArrayList<float[]> cells = new ArrayList<>(); //{칸, 무너지는 때}
		private float time = 0;
		CollapseWave(){
			for (int c : warnCells) cells.add(new float[]{ c, Random.Float(MazeTower.K_COLLAPSE_SPREAD) });
		}
		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			for (int i = cells.size() - 1; i >= 0; i--){
				if (cells.get(i)[1] <= time) collapseCell((int) cells.remove(i)[0]);
			}
			if (cells.isEmpty()){
				killAndErase();
				GnawerBossLevel lv = level();
				if (lv != null){ java.util.Arrays.fill(lv.trail, 0); lv.refreshTrail(); }
				sweepCD = Math.max(sweepCD, MazeTower.K_DIG_GAP); //(사용자) 굴 폭파 바로 뒤에 굴착이 오지 않게
				digsSinceCollapse = 0;
				warnCells.clear();
				phase = Phase.UNDER;
				timer = MazeTower.K_UNDER_TURNS;
				spend(TICK);
				next();
			}
		}
	}

	// ===== (K6) 굴착 ((사용자) 예전 이름 꼬리 휩쓸기) =====

	//남은 턴 숫자: 띠 칸 중 주인공에게 가장 가까운 칸 위에
	private void showSweepCount(){
		if (warnCells.isEmpty() || Dungeon.hero == null) return;
		int best = warnCells.get(0);
		for (int c : warnCells) if (Dungeon.level.distance(c, Dungeon.hero.pos) < Dungeon.level.distance(best, Dungeon.hero.pos)) best = c;
		showCount(best, timer);
	}

	//주인공을 지나는 3칸 폭 띠(가로 또는 세로)가 방을 가로지른다
	private boolean startSweep(){
		GnawerBossLevel lv = level();
		if (lv == null || heroHidden()) return false;
		int w = Dungeon.level.width(), h = Dungeon.hero.pos;
		sweepDir = Random.Int(2);
		sweepLine = sweepDir == 0 ? h / w : h % w;
		warnCells.clear();
		for (int c = 0; c < Dungeon.level.length(); c++){
			if (!lv.isRoomFloor(c)) continue;
			int line = sweepDir == 0 ? c / w : c % w;
			if (Math.abs(line - sweepLine) <= 1) warnCells.add(c);
		}
		if (warnCells.isEmpty()) return false;
		phase = Phase.SWEEP;
		timer = MazeTower.K_SWEEP_WARN;
		showSweepCount();
		showWarn();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.6f, 0.6f);
		GLog.w(Messages.get(this, "sweep_warn"));
		return true;
	}

	private void sweepStrike(){
		clearWarn();
		GnawerBossLevel lv = level();
		int w = Dungeon.level.width();
		//땅이 띠를 따라 한쪽 끝에서 반대쪽 끝으로 들썩이고, (사용자) 지나간 띠는 굴 자국으로 남는다(굴 폭파를 다시 쓸 수 있게)
		if (lv != null){
			java.util.TreeMap<Integer, ArrayList<Integer>> lines = new java.util.TreeMap<>();
			for (int c : warnCells){ int ln = sweepDir == 0 ? c / w : c % w; if (!lines.containsKey(ln)) lines.put(ln, new ArrayList<>()); lines.get(ln).add(c); }
			for (ArrayList<Integer> l : lines.values()){
				java.util.Collections.sort(l, (a, b) -> sweepDir == 0 ? Integer.compare(a % w, b % w) : Integer.compare(a / w, b / w));
				lv.addTrail(l, sweepDir == 0 ? 1 : 2, MazeTower.K_DIG_FX_STEP, 0); //돌 부스러기는 SweepFx만 (칸마다 K_DIG_ROCKS)
				int mid = sweepDir == 0 ? l.get(0) / w : l.get(0) % w;
				if (mid == sweepLine) GameScene.groupFloor(new CrestRun(l, sweepDir, 1, MazeTower.K_DIG_FX_STEP)); //가운데 줄에만 몸
			}
		}
		GameScene.groupEffect(new SweepFx(new ArrayList<>(warnCells), sweepDir));
		collapseCD = Math.max(collapseCD, MazeTower.K_DIG_GAP); //(사용자) 굴착 바로 뒤에 굴 폭파가 오지 않게
		digsSinceCollapse++;
		for (int c : new ArrayList<>(warnCells)){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || !ch.isAlive() || ch instanceof ManaVein) continue;
			hurt(ch, MazeTower.K_SWEEP_DMG_MIN, MazeTower.K_SWEEP_DMG_MAX);
			if (!ch.isAlive() || ch.properties().contains(Property.IMMOVABLE)) continue;
			//띠 밖으로 밀친다 (가운데 줄이면 아무 쪽으로)
			int line = sweepDir == 0 ? c / w : c % w;
			int sgn = line > sweepLine ? 1 : line < sweepLine ? -1 : (Random.Int(2) == 0 ? 1 : -1);
			int step = (sweepDir == 0 ? w : 1) * sgn;
			int to = c;
			for (int i = 0; i < MazeTower.K_SWEEP_PUSH; i++){
				int n = to + step;
				if (lv == null || !lv.freeFloor(n)) break;
				to = n;
			}
			if (to != c){
				Actor.add(new Pushing(ch, ch.pos, to));
				ch.pos = to;
				Dungeon.level.occupyCell(ch);
			}
		}
		PixelScene.shake(1, 0.2f);
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		warnCells.clear();
		phase = Phase.UNDER;
		timer = MazeTower.K_UNDER_TURNS;
	}

	//띠의 칸마다 한쪽 끝부터 0.025초 간격으로 돌 부스러기 4개(K_DIG_ROCKS)가 튄다
	private static class SweepFx extends com.watabou.noosa.Group {
		private final ArrayList<Integer> cells;
		private final int dir;
		private final int start;
		private float time = 0;
		SweepFx( ArrayList<Integer> cells, int dir ){
			this.cells = cells;
			this.dir = dir;
			final int w = Dungeon.level.width();
			java.util.Collections.sort(cells, (a, b) -> dir == 0 ? Integer.compare(a % w, b % w) : Integer.compare(a / w, b / w));
			start = cells.isEmpty() ? 0 : along(cells.get(0));
		}
		private int along( int c ){ int w = Dungeon.level.width(); return dir == 0 ? c % w : c / w; }
		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			while (!cells.isEmpty() && (along(cells.get(0)) - start) * MazeTower.K_DIG_FX_STEP <= time){
				int c = cells.remove(0);
				if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), MazeTower.K_DIG_ROCKS);
			}
			if (cells.isEmpty()) killAndErase();
		}
	}

	// ===== (K6) 연속 솟구치기 =====

	//(사용자) 주인공을 조준해 세 번 연달아 솟는다: 주인공 칸에 둥근 경고 → 터지면 곧바로 그때 주인공 칸에 다음 경고(주인공이 숨으면 같은 자리).
	//세 번째가 터지면 그 자리에서 솟아 길게(K_MULTI_STAY) 머문다
	private int multiLeft = 0;
	private ArrayList<Integer> multiUsed = new ArrayList<>(); //(사용자) 이미 조준한 가운데 칸 (같은 곳을 다시 조준하지 않음)

	//주인공 칸을 조준하되, 이미 조준한 칸이면 주인공 옆의 다른 빈 바닥 칸을 가운데로 (원은 여전히 주인공을 덮는다)
	private int aimMulti(){
		int h = heroHidden() ? warnCenter : Dungeon.hero.pos;
		if (!multiUsed.contains(h)) return h;
		GnawerBossLevel lv = level();
		ArrayList<Integer> opts = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = h + n;
			if (lv != null && lv.isRoomFloor(c) && !multiUsed.contains(c)) opts.add(c);
		}
		return opts.isEmpty() ? h : Random.element(opts);
	}

	private boolean startMulti(){
		if (level() == null || heroHidden()) return false;
		multiLeft = 3;
		multiUsed.clear();
		phase = Phase.MULTI;
		placeMultiCircle(aimMulti());
		Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.7f, 0.6f);
		GLog.w(Messages.get(this, "multi_warn"));
		return true;
	}

	private void placeMultiCircle( int center ){
		multiUsed.add(center);
		warnCenter = center;
		warnCells.clear();
		warnCells.addAll(circleOf(center));
		timer = MazeTower.K_EMERGE_WARN;
		showWarn();
		showCount(warnCenter, timer);
	}

	private ArrayList<Integer> circleOf( int center ){
		ArrayList<Integer> cells = new ArrayList<>();
		GnawerBossLevel lv = level();
		int w = Dungeon.level.width(), r = MazeTower.K_EMERGE_RADIUS;
		for (int dy = -r; dy <= r; dy++) for (int dx = -r; dx <= r; dx++){
			int c = center + dx + dy * w;
			if (Math.abs(c % w - center % w) != Math.abs(dx)) continue;
			if (lv != null && lv.isRoomFloor(c)) cells.add(c);
		}
		return cells;
	}

	private void multiTurn(){
		if (--timer > 0){ showCount(warnCenter, timer); return; }
		clearWarn();
		for (int c : warnCells){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || !ch.isAlive()) continue;
			if (ch instanceof ManaVein){ ch.die(this); continue; } //(사용자) 솟구치는 자리의 광맥은 그냥 터진다 (마력 증기, 다른 자리에 다시 자람)
			hurt(ch, MazeTower.K_EMERGE_DMG_MIN, MazeTower.K_EMERGE_DMG_MAX);
		}
		if (Dungeon.level.heroFOV[warnCenter]){ Sample.INSTANCE.play(Assets.Sounds.ROCKS); PixelScene.shake(1, 0.15f); }
		if (--multiLeft > 0){
			placeMultiCircle(aimMulti());
			return;
		}
		//마지막 원에서 솟아 길게 머문다
		GnawerBossLevel lv = level();
		int dest = warnCenter;
		if (lv != null && !lv.freeFloor(dest)){
			int best = Integer.MAX_VALUE; dest = -1;
			for (int c = 0; c < Dungeon.level.length(); c++){
				if (!lv.freeFloor(c)) continue;
				int d = Dungeon.level.distance(c, warnCenter);
				if (d < best){ best = d; dest = c; }
			}
		}
		warnCells.clear();
		warnCenter = -1;
		if (dest < 0){ phase = Phase.UNDER; timer = MazeTower.K_UNDER_TURNS; return; }
		surface(dest);
		phase = Phase.ABOVE;
		timer = MazeTower.K_MULTI_STAY;
		fresh = MazeTower.K_FRESH_TURNS;
	}

	// ===== 받는 피해 =====

	@Override
	public boolean isInvulnerable( Class effect ) {
		return underground() || super.isInvulnerable(effect);
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (underground()) return;
		if (fresh > 0) dmg = Math.round(dmg * MazeTower.K_FRESH_MULTI);
		if (shell > 0) dmg = Math.round(dmg * (1f - MazeTower.K_SHELL_REDUCE * shell));
		//(K5) 탈피한 몸은 화상에 약하다
		if (phase2 && src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning) dmg = Math.round(dmg * MazeTower.K_MOLT_FIRE_MULTI);
		//(K5) 체력 50%에서 멈추고 다음 차례에 탈피 (한 번에 건너뛰지 않음)
		int line = Math.round(HT * MazeTower.K_MOLT_AT);
		if (!phase2 && HP - dmg < line){ dmg = Math.max(0, HP - line); moltPending = true; }
		int before = HP;
		super.damage(dmg, src);
		if (phase == Phase.GNAW && isAlive()){
			gnawDmg += Math.max(0, before - HP);
			if (gnawDmg >= Math.round(HT * MazeTower.K_GNAW_BREAK)) interruptGnaw();
		}
	}

	@Override protected boolean getCloser( int target ) { return false; }
	@Override protected boolean getFurther( int target ) { return false; }
	@Override public void beckon( int cell ) { }
	@Override public void aggro( Char ch ) { }

	@Override
	public void die( Object cause ) {
		clearWarn();
		super.die( cause );
		Dungeon.level.mazeBossPos = pos; //보스가 쓰러진 자리 (역행 중 흔적)
		GameScene.bossSlain();
		if (level() != null) level().onGnawerDeath();
		//Maze Tower (K8, 사용자): 원작 DM-300의 저주받은 쇳조각 대신 미스릴 파편 2~3개 (반반)
		int shards = Random.IntRange(MazeTower.K_MITHRIL_MIN, MazeTower.K_MITHRIL_MAX);
		for (int i = 0; i < shards; i++){
			int ofs;
			int tries = 0;
			do { ofs = PathFinder.NEIGHBOURS8[Random.Int(8)]; } while (!Dungeon.level.passable[pos + ofs] && ++tries < 30);
			Dungeon.level.drop( new com.shatteredpixel.shatteredpixeldungeon.items.quest.MithrilShard(), pos + ofs ).sprite.drop( pos );
		}
		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[2] += 3000;
		GLog.p(Messages.get(this, "defeated"));
	}

	@Override
	public String description() {
		String d = Messages.get(this, underground() ? "desc_under" : "desc");
		if (shell > 0) d += "\n\n" + Messages.get(this, "desc_shell", shell, Math.round(MazeTower.K_SHELL_REDUCE * shell * 100));
		return d;
	}

	//마석 껍질: 받는 피해 감소 (보스 체력 바 밑 아이콘, 겹 수)
	public static class Shell extends Buff {
		{ type = buffType.POSITIVE; }
		public int layers = 0;
		@Override public int icon() { return BuffIndicator.ARMOR; }
		@Override public void tintIcon( Image icon ) { icon.hardlight(0.45f, 0.75f, 1f); }
		@Override public String iconTextDisplay() { return Integer.toString(layers); }
		@Override public String name() { return Messages.get(this, "name"); }
		@Override public String desc() { return Messages.get(this, "desc", layers, Math.round(MazeTower.K_SHELL_REDUCE * layers * 100)); }
		@Override public void storeInBundle( Bundle b ) { super.storeInBundle(b); b.put("layers", layers); }
		@Override public void restoreFromBundle( Bundle b ) { super.restoreFromBundle(b); layers = b.getInt("layers"); }
	}

	// ===== 저장 =====

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("timer", timer);
		bundle.put("fresh", fresh);
		bundle.put("center", warnCenter);
		int[] a = new int[warnCells.size()];
		for (int i = 0; i < a.length; i++) a[i] = warnCells.get(i);
		bundle.put("cells", a);
		bundle.put("dash_dir", dashDir);
		bundle.put("gnaw_vein", gnawVein);
		bundle.put("gnaw_dmg", gnawDmg);
		bundle.put("shell", shell);
		bundle.put("gnaw_cd", gnawCD);
		bundle.put("dash_cd", dashCD);
		bundle.put("started", started);
		bundle.put("phase2", phase2);
		bundle.put("last_p2", lastP2);
		bundle.put("digs_since", digsSinceCollapse);
		bundle.put("molt_pending", moltPending);
		bundle.put("collapse_cd", collapseCD);
		bundle.put("sweep_cd", sweepCD);
		bundle.put("multi_cd", multiCD);
		bundle.put("sweep_dir", sweepDir);
		bundle.put("sweep_line", sweepLine);
		bundle.put("multi_left", multiLeft);
		int[] mu = new int[multiUsed.size()];
		for (int i = 0; i < mu.length; i++) mu[i] = multiUsed.get(i);
		bundle.put("multi_used", mu);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getEnum("phase", Phase.class);
		timer = bundle.getInt("timer");
		fresh = bundle.getInt("fresh");
		warnCenter = bundle.getInt("center");
		warnCells.clear();
		for (int c : bundle.getIntArray("cells")) warnCells.add(c);
		dashDir = bundle.getInt("dash_dir");
		gnawVein = bundle.contains("gnaw_vein") ? bundle.getInt("gnaw_vein") : -1;
		gnawDmg = bundle.getInt("gnaw_dmg");
		shell = bundle.getInt("shell");
		gnawCD = bundle.getInt("gnaw_cd");
		dashCD = bundle.getInt("dash_cd");
		started = bundle.getBoolean("started");
		phase2 = bundle.getBoolean("phase2");
		lastP2 = bundle.contains("last_p2") ? bundle.getInt("last_p2") : -1;
		digsSinceCollapse = bundle.contains("digs_since") ? bundle.getInt("digs_since") : 99;
		moltPending = bundle.getBoolean("molt_pending");
		collapseCD = bundle.getInt("collapse_cd");
		sweepCD = bundle.getInt("sweep_cd");
		multiCD = bundle.getInt("multi_cd");
		sweepDir = bundle.getInt("sweep_dir");
		sweepLine = bundle.getInt("sweep_line");
		multiLeft = bundle.getInt("multi_left");
		multiUsed.clear();
		if (bundle.contains("multi_used")) for (int c : bundle.getIntArray("multi_used")) multiUsed.add(c);
	}

	//불러온 뒤 경고 그림을 다시 그린다
	@Override
	protected void onAdd() {
		super.onAdd();
		if ((phase == Phase.WARN || phase == Phase.SPIT || phase == Phase.COLLAPSE || phase == Phase.SWEEP) && !warnCells.isEmpty()) showWarn();
		if (phase == Phase.MULTI && !warnCells.isEmpty()) showWarn();
		if (phase == Phase.DASH && !warnCells.isEmpty()) showCrack();
	}

	// ===== 그림 =====

	//(사용자) 굴을 지나가는 긴 몸: 굴 줄의 얕은 곳(봉우리, 3칸, 한 줄에 1~2군데)은 땅에 고정된 완만한 곡선이고, 굴을 파는 앞머리를 따라 몸(6칸)이 흘러 지나간다.
	//앞머리가 봉우리에 닿으면 등이 앞쪽부터 드러나고, 마디 홈이 앞으로 흐르고(4칸), 꼬리가 빠지면 뒤부터 사라진다. 판정 없는 연출.
	//그림: sprites/maze_gnawer_crest_h.png (48x24 x 4, 줄 0 껍질 / 1 속살), maze_gnawer_crest_v.png (24x48 x 4). 원본 docs/sprites/mobs/k_draft/make_crest.ps1
	public static class CrestRun extends com.watabou.noosa.Group {
		private static final int CREST = 48, BODY = 96;
		private static final float RING = 3.4f;
		private final ArrayList<Integer> cells;
		private final int dir, row;
		private final float step;
		private final int[] crestStart;   //봉우리가 시작하는 줄 위 거리(픽셀)
		private final Image[] imgs;
		private float time = 0;

		public CrestRun( ArrayList<Integer> cells, int dir, int row, float step ){
			this.cells = cells; this.dir = dir; this.row = row; this.step = step;
			int n = cells.size();
			ArrayList<Integer> starts = new ArrayList<>();
			int want = n >= 9 ? 2 : 1;
			for (int tries = 0; tries < 30 && starts.size() < want && n >= 4; tries++){
				int s = Random.IntRange(0, n - 3);
				boolean ok = true;
				for (int o : starts) if (Math.abs(o - s) < 4) ok = false;
				if (ok) starts.add(s);
			}
			crestStart = new int[starts.size()];
			imgs = new Image[starts.size()];
			for (int i = 0; i < crestStart.length; i++){
				crestStart[i] = starts.get(i) * 16;
				imgs[i] = new Image(dir == 0 ? "sprites/maze_gnawer_crest_h.png" : "sprites/maze_gnawer_crest_v.png");
				imgs[i].visible = false;
				add(imgs[i]);
			}
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float head = time / step * 16f, tail = head - BODY;
			int ph = (int) ((((head % RING) + RING) % RING) / RING * 4) % 4;
			if (cells.isEmpty() || tail > cells.size() * 16){ killAndErase(); return; }
			PointF origin = DungeonTilemap.tileToWorld(cells.get(0));
			for (int i = 0; i < imgs.length; i++){
				Image im = imgs[i];
				int cs = crestStart[i];
				float a = Math.max(cs, tail), b = Math.min(cs + CREST, head);
				int cellIdx = Math.min(cells.size() - 1, (cs + CREST / 2) / 16);
				if (b <= a || !Dungeon.level.heroFOV[cells.get(cellIdx)]){ im.visible = false; continue; }
				int sx = (int) (a - cs), w = Math.max(1, (int) (b - a));
				im.visible = true;
				if (dir == 0){
					im.frame(ph * CREST + sx, row * 24, w, 24);
					im.x = origin.x + (int) a;
					im.y = origin.y + 16 - 22;
				} else {
					im.frame(ph * 24, row * CREST + sx, 24, w);
					im.x = origin.x + 8 - 12;
					im.y = origin.y + (int) a;
				}
			}
		}
	}

	//(사용자) 마력 토사 이펙트: 흙덩이·푸른 마석 조각 20개(sprites/maze_fx_gnawer_clods.png 4x4 x 4)가 입에서 포물선을 그리며
	//앞쪽 칸들의 무작위 자리로 0.4초에 걸쳐 차례로 날아가고, 떨어진 자리에서 작게 흩어진다(sprites/maze_fx_gnawer_spit.png 16x16의 3~5칸)
	public static class SpitFx extends com.watabou.noosa.Group {
		private static final float START = 0.2f; //뿜는 칸이 나올 때까지
		private final float mx, my;
		private final ArrayList<float[]> pieces = new ArrayList<>(); //{종류, 목표 x, 목표 y, 시작, 걸리는 시간, 높이}
		private final ArrayList<Image> imgs = new ArrayList<>(), splats = new ArrayList<>();
		private float time = 0;

		private com.watabou.utils.Callback onLand;

		public SpitFx( float mx, float my, ArrayList<Integer> targets, com.watabou.utils.Callback onLand ){
			this.onLand = onLand;
			this.mx = mx; this.my = my;
			for (int i = 0; i < 20; i++){
				int c = targets.get(Random.Int(targets.size()));
				PointF p = DungeonTilemap.tileToWorld(c);
				pieces.add(new float[]{ Random.Int(4), p.x + 3 + Random.Float(10), p.y + 4 + Random.Float(9),
						START + Random.Float(0.35f), 0.38f + Random.Float(0.16f), 10 + Random.Float(10) });
				Image im = new Image("sprites/maze_fx_gnawer_clods.png", (int) pieces.get(i)[0] * 4, 0, 4, 4);
				im.visible = false;
				add(im); imgs.add(im);
				Image sp = new Image("sprites/maze_fx_gnawer_spit.png", 48, 0, 16, 16);
				sp.visible = false;
				add(sp); splats.add(sp);
			}
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			boolean done = true;
			int nowLanded = 0;
			for (int i = 0; i < pieces.size(); i++){
				float[] p = pieces.get(i);
				float k = (time - p[3]) / p[4];
				Image im = imgs.get(i), sp = splats.get(i);
				if (k < 0){ done = false; continue; }
				if (k < 1){
					done = false;
					im.visible = true;
					im.x = Math.round(mx + (p[1] - mx) * k - 2);
					im.y = Math.round(my + (p[2] - my) * k - p[5] * 4 * k * (1 - k) - 2);
					continue;
				}
				im.visible = false;
				nowLanded++;
				float after = time - p[3] - p[4];
				if (after < 0.3f){
					done = false;
					int fr = 3 + Math.min(2, (int) (after / 0.1f));
					sp.frame(fr * 16, 0, 16, 16);
					sp.x = p[1] - 8; sp.y = p[2] - 8;
					sp.visible = true;
				} else sp.visible = false;
			}
			//조각 절반이 땅에 닿으면 마력 증기 (한 번)
			if (onLand != null && (nowLanded >= pieces.size() / 2 || done)){ com.watabou.utils.Callback cb = onLand; onLand = null; cb.call(); }
			if (done) killAndErase();
		}
	}

	public static class GnawerSprite extends MobSprite {
		private static final int ROW = 26; //줄마다 칸 수 (23~25 토사 뱉기)
		private TextureFilm film;
		private Animation emergeAnim, burrowAnim, spitAnim, writheAnim;
		private boolean moltNext = false;

		//(K5) 탈피: 솟는 중이면 다 솟은 뒤, 아니면 바로 몸을 비튼다(writheAnim)
		private com.watabou.utils.Callback moltDone;
		public void molt( com.watabou.utils.Callback done ){
			moltDone = done;
			if (done == null){ setRow(1); return; } //보이지 않으면 그림만 바꾼다
			visible = true;
			if (curAnim == emergeAnim && !finished) moltNext = true;
			else play(writheAnim);
		}
		private Animation[] gnawAnims = new Animation[3];
		private Animation pendingGnaw;
		private float resistT = 0, resistDx = 0, resistDy = 0;

		public GnawerSprite(){
			super();
			texture("sprites/maze_b3_gnawer.png");
			film = new TextureFilm(texture, 24, 24);
			build(0);
			play(idle);
		}

		private int row = 0;

		//(K5) 탈피하면 푸른 속살 줄로
		public void setRow( int r ){
			if (r == row) return;
			row = r;
			Animation cur = curAnim;
			build(r);
			play(cur == null ? idle : idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof RootGnawer && ((RootGnawer) ch).phase2()) setRow(1);
		}

		private void build( int row ){
			int o = row * ROW;
			idle = new Animation(2, true);      idle.frames(film, o, o + 1);
			run = idle.clone();
			//(사용자) 샌드웜답게 모든 동작을 느리게
			attack = new Animation(6, false);   attack.frames(film, o + 2, o + 3, o + 4, o);
			zap = attack.clone();
			die = new Animation(4, false);      die.frames(film, o + 13, o + 14, o + 15, o + 16);
			emergeAnim = new Animation(MazeTower.K_EMERGE_FPS, false); emergeAnim.frames(film, o + 8, o + 9, o + 10, o + 11, o + 10, o + 12);
			spitAnim = new Animation(5, false); spitAnim.frames(film, o + 23, o + 24, o + 25, o + 25, o);
			writheAnim = new Animation(3, false); writheAnim.frames(film, o, o + 12, o + 11, o + 12, o);
			burrowAnim = new Animation(6, false); burrowAnim.frames(film, o + 12, o + 10, o + 9, o + 8);
			//갉기: 광맥이 0·1·2단계 갉혔을 때 (몸이 점점 더 굽음)
			int[][] g = { {5, 6, 7, 6}, {17, 18, 19, 18}, {20, 21, 22, 21} };
			for (int i = 0; i < 3; i++){
				gnawAnims[i] = new Animation(5, true);
				gnawAnims[i].frames(film, o + g[i][0], o + g[i][1], o + g[i][2], o + g[i][3]);
			}
		}

		public Animation idleAnim(){ return idle; }
		public Animation attackAnim(){ return attack; }

		//(사용자) 토사 뱉기: 젖히며 입 벌림 → 내밀며 뿜음 → 벌린 채. 뿜는 칸(0.2초 뒤)에 입에서 흙덩이·마석 조각이 포물선을 그리며 앞쪽 3칸쯤으로
		public void spit( java.util.ArrayList<Integer> targets, com.watabou.utils.Callback onLand ){
			play(spitAnim);
			float mx = x + (flipHorizontal ? 3 : 21), my = y + 8;
			com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupEffect(new SpitFx(mx, my, targets, onLand));
		}

		public void emerge(){
			visible = true;
			alpha(1f);
			play(emergeAnim);
		}

		//솟은 뒤 이어서 갉는다
		public void gnawNext( int stage ){
			pendingGnaw = gnawAnims[Math.max(0, Math.min(2, stage))];
		}

		public void gnaw( int stage ){
			pendingGnaw = null;
			Animation a = gnawAnims[Math.max(0, Math.min(2, stage))];
			if (curAnim != a) play(a);
		}

		public void burrow(){
			pendingGnaw = null;
			play(burrowAnim);
		}

		//(사용자) 밀리지 않고 버틴다: 밀린 쪽으로 몸이 한 번 휘청였다 제자리로
		public void resist( int from, int toward ){
			int w = Dungeon.level.width();
			float dx = toward % w - from % w, dy = toward / w - from / w;
			float l = (float) Math.sqrt(dx * dx + dy * dy);
			if (l < 0.01f){ dx = flipHorizontal ? 1 : -1; dy = 0; l = 1; }
			resistDx = dx / l; resistDy = dy / l;
			resistT = 0.35f;
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == emergeAnim){
				if (moltNext){ moltNext = false; play(writheAnim); return; }
				if (pendingGnaw != null){ play(pendingGnaw); pendingGnaw = null; }
				else play(idle);
				return;
			}
			if (anim == writheAnim){
				//다 비틀면: 번쩍이며 껍질 조각이 튀고 그림이 푸른 속살로
				setRow(1);
				flash();
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.groupEffect(new ShellBurstFx(center()));
				com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(1, 0.3f);
				play(idle);
				if (moltDone != null){ com.watabou.utils.Callback cb = moltDone; moltDone = null; cb.call(); }
				return;
			}
			if (anim == spitAnim){ play(idle); return; }
			if (anim == burrowAnim){
				visible = false;
				play(idle);
				return;
			}
			super.onComplete(anim);
		}

		@Override
		public void update() {
			super.update();
			if (resistT > 0 && ch != null){
				resistT = Math.max(0, resistT - Game.elapsed);
				//처음엔 밀린 쪽으로 2픽셀, 버티며 돌아옴
				float k = resistT > 0.25f ? (0.35f - resistT) / 0.1f : resistT / 0.25f;
				PointF p = worldToCamera(ch.pos);
				x = p.x + resistDx * 2f * k;
				y = p.y + resistDy * 2f * k;
				if (resistT == 0){ x = p.x; y = p.y; }
			}
			//땅속에 있는 동안은 그리지 않는다 (땅속으로 들어가는 동작 중에는 그림)
			if (ch instanceof RootGnawer && ((RootGnawer) ch).underground() && curAnim != burrowAnim) visible = false;
		}
	}
}
