/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SoulOrb;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Light;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeBossSprites;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 20층 망령 왕 (명세 22번) — 영혼을 누가 먼저 가져가느냐, 빛과 어둠.
//원작 드워프 왕 패턴은 쓰지 않는다. 그림 maze_b5_wraithking_sheet.png, 3단계 maze_b5_wraithking_crawl_sheet.png.
//  1단계(100~70%) 왕의 수확: 공격하지 않고 영혼 구슬을 주우러 다닌다. 4턴마다 2개(1턴 전 예고).
//    왕이 먼저 닿으면 영혼 +1, 주인공이 먼저 밟으면 사라지고 주인공 신속 2턴(갱신만).
//    영혼 1개당 공격력 +5%. 6개 이상: 어두워질 때마다 근위병 3마리. 12개 이상: 졸트라크 경고 없음. 18개 이상: 공격력 +100%.
//  2단계(70~25%) 어둠의 도시: 방이 어두워지고(주인공 시야 1칸) 기억의 등불 3개. 어둠 속 왕은 은신.
//    졸트라크(2턴 기 모으기 → 폭 3칸 광선, 15턴마다), 망령 근위병(7턴마다), 기습(어두운 동안 3턴마다, 푸른 눈 쪽을 치면 막고 왕이 피해).
//  3단계(25~0%) 깨진 왕관: 기어다니고, 어둠·졸트라크·새 근위병 없음. 받는 평타·스킬 피해 +50%.
//  공통: 한 턴에 기술 하나. 기를 모으는 동안 다른 기술 없음. 왕의 기술은 주인공에게 적대적인 몹을 다치게 하지 않는다.
//  주인공이 투명(은신)인 동안 주인공을 노리는 기술(졸트라크·기습)은 쓰지 않는다(쿨타임은 흐른다).
public class WraithKing extends Mob {

	{
		spriteClass = WraithKingSprite.class;

		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? MazeTower.B20_HP_STRONG : MazeTower.B20_HP;
		EXP = MazeTower.B20_EXP;
		defenseSkill = MazeTower.B20_DEF;

		properties.add(Property.BOSS);
		properties.add(Property.UNDEAD);
	}

	private int phase = 1;
	private int souls = 0;
	private int orbTimer = MazeTower.WK_ORB_INTERVAL;
	private int[] pendingOrbs = new int[0];

	private boolean dark = false;
	private boolean lampsPlaced = false;
	private int zoltCd = MazeTower.WK_ZOLT_INTERVAL;
	private int guardCd = MazeTower.WK_GUARD_INTERVAL;
	private int ambushCd = MazeTower.WK_AMBUSH_INTERVAL;

	private int zoltCharge = 0;   //남은 기 모으기 턴
	private int zoltDir = 0;      //0 오른쪽, 1 아래, 2 왼쪽, 3 위
	private int ambushCell = -1;  //기습하려고 숨어든 칸 (주인공 뒤)
	private boolean countered = false;

	//화면 연출 (저장하지 않음)
	private ArrayList<MovieClip> zoltWarn = new ArrayList<>();
	private MovieClip zoltChargeFx;

	public static WraithKing find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof WraithKing && m.isAlive()) return (WraithKing) m;
		}
		return null;
	}

	public boolean isDark(){
		return phase == 2 && dark;
	}

	//어둠 속에 숨어 있다 (그림을 숨긴다). 기습하려고 주인공 뒤에 숨어든 동안도 그림은 안 보이고 눈만 빛난다
	public boolean shrouded(){
		return isDark();
	}

	public int phase(){ return phase; }
	public int souls(){ return souls; }

	private CityBossLevel level(){
		return Dungeon.level instanceof CityBossLevel ? (CityBossLevel) Dungeon.level : null;
	}

	// ===== 수치 =====

	//영혼 강화: 1개당 +5%, 18개 이상이면 +100% 더
	public float power(){
		float p = 1f + souls * MazeTower.WK_SOUL_ATK;
		if (souls >= MazeTower.WK_SOUL_RAGE) p += MazeTower.WK_RAGE_ATK;
		return p;
	}

	@Override
	public int damageRoll() {
		return Math.round(Random.NormalIntRange(MazeTower.B20_DMG_MIN, MazeTower.B20_DMG_MAX) * power());
	}

	@Override
	public int attackSkill( Char target ) {
		return MazeTower.B20_ACC;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, MazeTower.B20_DR);
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (phase == 1 || isDark()) return false; //1단계는 공격하지 않고, 어둠 속에서는 기습만
		return super.canAttack(enemy);
	}

	@Override
	public String name() {
		return Messages.get(DwarfKing.class, "maze_wraith_name");
	}

	@Override
	public String description() {
		return Messages.get(DwarfKing.class, "maze_wraith_desc") + "\n\n" + Messages.get(this, "souls", souls);
	}

	// ===== 턴 =====

	@Override
	protected boolean act() {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss(this);
			if (phase == 3) BossHealthBar.bleed(true);
		}
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		//주인공을 알아챈 상태 (원작 AI를 쓰지 않는 턴에도 기습 판정이 되지 않게)
		if (Dungeon.hero.isAlive() && Dungeon.hero.invisible <= 0){ enemy = Dungeon.hero; enemySeen = true; }

		applyDarkness();
		restoreFx();

		if (paralysed > 0){
			return super.act();
		}

		if (phase == 1) return actHarvest();
		if (phase == 3) return super.act();

		//2단계
		zoltCd--; guardCd--;
		if (dark) ambushCd--;

		if (zoltCharge > 0) return actCharge();
		if (ambushCell != -1) return actAmbush();

		boolean heroHidden = Dungeon.hero.invisible > 0;
		if (zoltCd <= 0 && !heroHidden){
			startZoltraak();
			spend(TICK);
			return true;
		}
		if (dark && ambushCd <= 0 && !heroHidden){
			if (startAmbush()){
				spend(TICK);
				return true;
			}
		}
		if (guardCd <= 0){
			guardCd = MazeTower.WK_GUARD_INTERVAL;
			summonGuards(1);
			spend(TICK);
			return true;
		}
		if (dark){
			spend(TICK); //어둠 속에서는 숨어서 기다린다
			return true;
		}
		return super.act();
	}

	// ----- 1단계: 왕의 수확 -----

	private boolean actHarvest(){
		//지난 턴에 예고한 칸에 구슬이 떨어진다
		if (pendingOrbs.length > 0){
			for (int c : pendingOrbs){
				SoulOrb.remove(SoulOrb.Warn.class, c);
				GameScene.add(Blob.seed(c, 1, SoulOrb.class));
			}
			pendingOrbs = new int[0];
		}

		orbTimer--;
		if (orbTimer <= 0){
			orbTimer = MazeTower.WK_ORB_INTERVAL;
			warnOrbs();
		}

		//가장 가까운 구슬로
		int best = -1;
		SoulOrb orbs = (SoulOrb) Dungeon.level.blobs.get(SoulOrb.class);
		if (orbs != null && orbs.volume > 0){
			int bestDist = Integer.MAX_VALUE;
			for (int i = 0; i < Dungeon.level.length(); i++){
				if (!SoulOrb.at(i)) continue;
				int d = Dungeon.level.distance(pos, i);
				if (d < bestDist){ bestDist = d; best = i; }
			}
		}
		int oldPos = pos;
		if (best != -1 && getCloser(best)){
			spend(1f / speed());
			return moveSprite(oldPos, pos); //그림도 함께 움직인다 (빠뜨리면 그림만 제자리에 남는다)
		}
		spend(TICK);
		return true;
	}

	private void warnOrbs(){
		CityBossLevel l = level();
		if (l == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (l.roomEdge(c) || c == l.mazeThrone()) continue;
			if (Actor.findChar(c) != null || SoulOrb.at(c) || SoulOrb.Warn.at(c)) continue;
			if (Dungeon.level.distance(c, pos) <= 1) continue; //(결정) 왕이 있는 칸과 그 주변 1칸에는 떨어지지 않는다
			cells.add(c);
		}
		ArrayList<Integer> chosen = new ArrayList<>();
		for (int i = 0; i < MazeTower.WK_ORB_COUNT && !cells.isEmpty(); i++){
			int c = cells.remove(Random.Int(cells.size()));
			chosen.add(c);
			GameScene.add(Blob.seed(c, 1, SoulOrb.Warn.class));
		}
		pendingOrbs = new int[chosen.size()];
		for (int i = 0; i < chosen.size(); i++) pendingOrbs[i] = chosen.get(i);
	}

	//누군가 칸에 들어섰다 (CityBossLevel.occupyCell). 영혼 구슬을 먹는다
	public static void onOccupy( Char ch ){
		if (!SoulOrb.at(ch.pos)) return;
		if (ch == Dungeon.hero){
			SoulOrb.remove(SoulOrb.class, ch.pos);
			//(결정) 신속 2턴. 남아 있으면 2턴으로 갱신만 한다
			Haste h = ch.buff(Haste.class);
			if (h != null) h.detach();
			Buff.affect(ch, Haste.class, MazeTower.WK_ORB_HASTE);
			if (Dungeon.level.heroFOV[ch.pos]) BossEffects.atCell(BossEffects.Fx.WK_SOUL_ABSORB, ch.pos, 12, false);
			Sample.INSTANCE.play(Assets.Sounds.DRINK, 0.6f, 1.4f);
		} else if (ch instanceof WraithKing){
			SoulOrb.remove(SoulOrb.class, ch.pos);
			((WraithKing) ch).souls++;
			if (Dungeon.level.heroFOV[ch.pos]){
				BossEffects.atCell(BossEffects.Fx.WK_SOUL_ABSORB, ch.pos, 12, false);
				if (ch.sprite != null) ch.sprite.showStatus(0x66AAFF, Messages.get(WraithKing.class, "soul_status", ((WraithKing) ch).souls));
			}
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.5f, 1.3f);
		}
	}

	// ----- 2단계: 어둠 -----

	private void enterPhase2(){
		phase = 2;
		pendingOrbs = new int[0];
		SoulOrb.removeAll(SoulOrb.class);
		SoulOrb.removeAll(SoulOrb.Warn.class);
		yell(Messages.get(this, "phase2"));
		if (!lampsPlaced){
			lampsPlaced = true;
			placeLamps();
		}
		dark = true;
		onDarkStart();
		applyDarkness();
	}

	private void placeLamps(){
		CityBossLevel l = level();
		if (l == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (l.touchesWall(c) || c == l.mazeThrone() || Actor.findChar(c) != null) continue;
			cells.add(c);
		}
		for (int i = 0; i < MazeTower.WK_LAMPS && !cells.isEmpty(); i++){
			MemoryLamp lamp = new MemoryLamp();
			lamp.pos = cells.remove(Random.Int(cells.size()));
			GameScene.add(lamp);
		}
	}

	//방이 어두워질 때마다 (영혼 6개 이상이면 근위병 3마리)
	private void onDarkStart(){
		if (souls >= MazeTower.WK_SOUL_GUARDS) summonGuards(MazeTower.WK_DARK_GUARDS);
		GLog.n(Messages.get(this, "dark"));
	}

	public void onLampLit( MemoryLamp lamp ){
		if (phase != 2) return;
		dark = false;
		cancelAmbush();
		Buff.affect(this, Paralysis.class, MazeTower.WK_LAMP_STUN);
		applyDarkness();
		GLog.p(Messages.get(this, "lamp_lit"));
	}

	public void onLampBroken(){
		if (phase != 2) return;
		dark = true;
		onDarkStart();
		applyDarkness();
	}

	private int lastView = -1;

	//어둠: 주인공 시야 1칸, 왕은 은신. 밝아지면 원래대로
	private void applyDarkness(){
		Hero h = Dungeon.hero;
		int want;
		if (isDark()){
			want = MazeTower.WK_DARK_VIEW;
		} else {
			want = Dungeon.level.viewDistance;
			if (h.buff(Light.class) != null) want = Math.max(Light.DISTANCE, want);
		}
		invisible = isDark() ? 1 : 0; //어둠 속 왕은 늘 숨어 있다 (기습은 푸른 눈으로)
		if (h.viewDistance != want || lastView != want){
			h.viewDistance = want;
			lastView = want;
			Dungeon.observe();
			GameScene.updateFog();
		}
	}

	//근위병 소환 (등불 칸·주인공 근처 제외)
	private void summonGuards( int n ){
		CityBossLevel l = level();
		if (l == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (Actor.findChar(c) != null) continue;
			if (Dungeon.level.distance(c, Dungeon.hero.pos) < 2) continue;
			cells.add(c);
		}
		for (int i = 0; i < n && !cells.isEmpty(); i++){
			WraithGuard g = new WraithGuard();
			g.pos = cells.remove(Random.Int(cells.size()));
			g.target = Dungeon.hero.pos;
			GameScene.add(g, 1f);
			if (Dungeon.level.heroFOV[g.pos] && g.sprite != null){
				g.sprite.alpha(0);
				g.sprite.parent.add(new com.watabou.noosa.tweeners.AlphaTweener(g.sprite, 1, 0.3f));
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.6f, 0.8f);
	}

	//소리: 주인공이 움직이거나 무언가를 던지면 근위병이 그쪽으로
	public static void noise( int cell ){
		if (Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof WraithGuard) ((WraithGuard) m).hear(cell);
		}
	}

	// ----- 졸트라크 -----

	private void startZoltraak(){
		int dx = Dungeon.hero.pos % Dungeon.level.width() - pos % Dungeon.level.width();
		int dy = Dungeon.hero.pos / Dungeon.level.width() - pos / Dungeon.level.width();
		if (Math.abs(dx) >= Math.abs(dy)) zoltDir = dx >= 0 ? 0 : 2;
		else                             zoltDir = dy >= 0 ? 1 : 3;
		zoltCharge = MazeTower.WK_ZOLT_CHARGE;
		showZoltFx();
		if (Dungeon.level.heroFOV[pos]) GLog.n(Messages.get(this, "zolt_charge"));
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 0.7f);
	}

	private void showZoltFx(){
		clearZoltFx();
		if (Dungeon.level.heroFOV[pos] && sprite != null){
			zoltChargeFx = BossEffects.onChar(BossEffects.Fx.WK_ZOLT_CHARGE, sprite, 8, true);
		}
		//영혼 11개 이하면 범위 경고
		if (souls < MazeTower.WK_SOUL_NOWARN){
			for (int c : zoltCells()){
				zoltWarn.add(BossEffects.atCell(BossEffects.Fx.WK_ZOLT_WARN, c, 4, true));
			}
		}
	}

	private void clearZoltFx(){
		for (MovieClip c : zoltWarn) c.killAndErase();
		zoltWarn.clear();
		if (zoltChargeFx != null){
			zoltChargeFx.killAndErase();
			zoltChargeFx = null;
		}
	}

	private static final int[] DX = {1, 0, -1, 0};
	private static final int[] DY = {0, 1, 0, -1};

	//폭 3칸, 벽에 막힐 때까지. lane[i] = 각 칸이 몇 번째 줄(-1, 0, 1)인지
	private ArrayList<Integer> zoltCells(){
		return zoltCells(null);
	}

	private ArrayList<Integer> zoltCells( ArrayList<Integer> lanes ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int x0 = pos % w, y0 = pos / w;
		int dx = DX[zoltDir], dy = DY[zoltDir];
		int px = -dy, py = dx; //옆 방향
		for (int k = -1; k <= 1; k++){
			for (int i = (k == 0 ? 1 : 0); i < 40; i++){
				int x = x0 + px * k + dx * i, y = y0 + py * k + dy * i;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) break;
				int c = x + y * w;
				if (Dungeon.level.solid[c]) break;
				cells.add(c);
				if (lanes != null) lanes.add(k);
			}
		}
		return cells;
	}

	private boolean actCharge(){
		zoltCharge--;
		if (zoltCharge > 0){
			spend(TICK);
			return true;
		}
		fireZoltraak();
		zoltCd = MazeTower.WK_ZOLT_INTERVAL;
		spend(TICK);
		return true;
	}

	private void fireZoltraak(){
		clearZoltFx();
		ArrayList<Integer> lanes = new ArrayList<>();
		ArrayList<Integer> cells = zoltCells(lanes);
		boolean seen = false;
		for (int i = 0; i < cells.size(); i++){
			int c = cells.get(i);
			//어둠 속에서도 광선은 보인다
			beamVisual(c, lanes.get(i));
			if (Dungeon.level.heroFOV[c]) seen = true;
		}
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this) continue;
			//왕의 기술은 주인공에게 적대적인 몹(근위병 등)과 등불을 다치게 하지 않는다
			if (ch.alignment == Alignment.ENEMY || ch instanceof MemoryLamp) continue;
			int dmg = Math.round(Random.NormalIntRange(MazeTower.WK_ZOLT_MIN, MazeTower.WK_ZOLT_MAX) * power());
			ch.damage(dmg, this);
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "zolt_kill"));
			}
		}
		if (seen) PixelScene.shake(2, 0.4f);
		Sample.INSTANCE.play(Assets.Sounds.RAY);
	}

	//가로 그림을 세로 광선이면 90도 돌린다. 바깥 두 줄은 위쪽 줄 기준, 반대쪽 줄은 뒤집는다
	private void beamVisual( int cell, int lane ){
		boolean vertical = zoltDir == 1 || zoltDir == 3;
		MovieClip c = BossEffects.clip(lane == 0 ? BossEffects.Fx.WK_ZOLT_CENTER : BossEffects.Fx.WK_ZOLT_EDGE, 15, false);
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		c.origin.set(8, 8);
		c.x = p.x - 8;
		c.y = p.y - 8;
		if (vertical){
			c.angle = 90;
			//90도 돌리면 위쪽이 오른쪽이 된다: 왼쪽 줄(옆 방향 -x)은 뒤집는다
			int px = -DY[zoltDir];
			if (lane != 0 && lane * px < 0) c.scale.y = -1;
		} else {
			int py = DX[zoltDir];
			if (lane != 0 && lane * py > 0) c.scale.y = -1; //아래쪽 줄
		}
		GameScene.effect(c);
	}

	// ----- 기습 -----
	//(결정, 바뀜) 어둠 속 왕은 실제로 걸어다니지 않는다. 주인공 뒤 칸에 푸른 눈(WraithEyes)만 나타나고,
	//그 눈을 치면 기습이 취소되며 피해가 왕에게 들어간다. 치지 않으면 다음 왕 턴에 왕이 그 칸으로 순간이동해
	//주인공을 한 대 치고(그 순간만 모습이 보인다) 다른 곳으로 순간이동해 사라진다.

	private boolean startAmbush(){
		CityBossLevel l = level();
		if (l == null) return false;
		int hero = Dungeon.hero.pos;
		int behind = -1;
		int step = l.lastHeroStep();
		if (step != 0){
			int c = hero - step;
			if (validAmbushCell(l, c)) behind = c;
		}
		if (behind == -1){
			ArrayList<Integer> opts = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				if (validAmbushCell(l, hero + n)) opts.add(hero + n);
			}
			if (opts.isEmpty()){
				ambushCd = 1;
				return false;
			}
			behind = Random.element(opts);
		}
		ambushCd = MazeTower.WK_AMBUSH_INTERVAL;
		ambushCell = behind;
		countered = false;
		showEyes();
		return true;
	}

	private boolean validAmbushCell( CityBossLevel l, int c ){
		return c >= 0 && c < l.length() && l.roomFloor(c) && Actor.findChar(c) == null && l.passable[c];
	}

	private void showEyes(){
		if (findEyes() != null) return;
		if (Actor.findChar(ambushCell) != null) return;
		WraithEyes e = new WraithEyes();
		e.pos = ambushCell;
		GameScene.add(e);
	}

	private static WraithEyes findEyes(){
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof WraithEyes) return (WraithEyes) m;
		}
		return null;
	}

	private static void removeEyes(){
		WraithEyes e = findEyes();
		if (e != null) e.die(null);
	}

	//눈이 맞았다: 기습을 막고 피해를 숨은 왕에게
	public void counterHit( int dmg, Object src ){
		if (ambushCell == -1 || countered) return;
		countered = true;
		damage(dmg, src);
	}

	private boolean actAmbush(){
		int cell = ambushCell;
		boolean wasCountered = countered;
		removeEyes();
		ambushCell = -1;
		countered = false;
		if (wasCountered){
			if (Dungeon.level.heroFOV[cell]) GLog.p(Messages.get(this, "ambush_blocked"));
		} else if (Dungeon.level.adjacent(cell, Dungeon.hero.pos) && Actor.findChar(cell) == null && Dungeon.hero.isAlive()){
			//막지 못하면 그 칸으로 순간이동해 주인공을 덮친다 (반드시 맞는다)
			pos = cell;
			Dungeon.level.occupyCell(this);
			if (sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).strike(cell, Dungeon.hero.pos);
			attack(Dungeon.hero, 1f, 0f, Char.INFINITE_ACCURACY);
			if (!Dungeon.hero.isAlive()){
				Dungeon.fail(this);
			}
			vanish(false);
		}
		spend(TICK);
		return true;
	}

	private void cancelAmbush(){
		if (Dungeon.level != null) removeEyes();
		ambushCell = -1;
		countered = false;
	}

	//주인공에게서 떨어진 칸으로 숨는다. placeSprite가 false면 그림은 기습 연출이 끝난 뒤 옮겨진다
	private void vanish( boolean placeSprite ){
		CityBossLevel l = level();
		if (l == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (Actor.findChar(c) != null) continue;
			if (Dungeon.level.distance(c, Dungeon.hero.pos) < 3) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		pos = Random.element(cells);
		if (placeSprite && sprite != null) sprite.place(pos);
		Dungeon.level.occupyCell(this);
	}

	// ----- 3단계 -----

	private void enterPhase3(){
		if (phase == 1) enterPhase2();
		phase = 3;
		dark = false;
		zoltCharge = 0;
		clearZoltFx();
		cancelAmbush();
		applyDarkness();
		BossHealthBar.bleed(true);
		if (sprite != null){
			BossEffects.onChar(BossEffects.Fx.WK_CROWN_BREAK, sprite, 8, false);
			if (sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).crawl();
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		yell(Messages.get(this, "phase3"));
	}

	// ===== 피해 =====

	@Override
	public void damage( int dmg, Object src ) {
		//3단계: 받는 평타·스킬 피해 +50%
		if (phase == 3 && src instanceof Char){
			dmg = Math.round(dmg * MazeTower.WK_PHASE3_TAKEN);
		}
		int pre = HP;
		super.damage(dmg, src);
		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
			lock.addTime((pre - HP) / 3f);
		}
		if (!isAlive()) return;
		if (phase == 1 && HP <= HT * MazeTower.WK_PHASE2_HP){
			enterPhase2();
		}
		if (phase <= 2 && HP <= HT * MazeTower.WK_PHASE3_HP){
			enterPhase3();
		}
		//(결정) 한 대 맞을 때마다 순간이동 (화상·독 같은 지속 피해는 제외)
		if (isAlive() && pre != HP && !(src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff)
				&& !(src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob)){
			hitTeleport();
		}
	}

	private void hitTeleport(){
		CityBossLevel l = level();
		if (l == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (l.roomEdge(c) || Actor.findChar(c) != null) continue;
			if (Dungeon.level.distance(c, Dungeon.hero.pos) < 2) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		boolean seenFrom = Dungeon.level.heroFOV[pos] && !shrouded();
		int from = pos;
		pos = Random.element(cells);
		if (sprite != null) sprite.place(pos);
		Dungeon.level.occupyCell(this);
		if (seenFrom) BossEffects.atCell(BossEffects.Fx.TELEPORT, from, 12, false);
		if (Dungeon.level.heroFOV[pos] && !shrouded()) BossEffects.atCell(BossEffects.Fx.TELEPORT, pos, 12, false);
		Sample.INSTANCE.play(Assets.Sounds.PUFF, 0.6f);
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss(this);
			yell(Messages.get(this, "notice"));
		}
	}

	@Override
	public void die( Object cause ) {
		clearZoltFx();
		cancelAmbush();
		dark = false;
		phase = 3;
		applyDarkness();
		SoulOrb.removeAll(SoulOrb.class);
		SoulOrb.removeAll(SoulOrb.Warn.class);
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof WraithGuard) m.die(null);
		}
		Dungeon.level.mazeBossPos = pos; //보스가 쓰러진 자리 (역행 중 흔적)
		GameScene.bossSlain();
		super.die(cause);
		MazeBoss20.afterDeath(this);
		yell(Messages.get(this, "defeated"));
	}

	//불러온 뒤 화면 연출을 다시 만든다
	private void restoreFx(){
		if (zoltCharge > 0 && zoltChargeFx == null && zoltWarn.isEmpty()) showZoltFx();
		if (ambushCell != -1) showEyes();
		if (phase == 3 && sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).crawl();
	}

	// ===== 저장 =====

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("souls", souls);
		bundle.put("orb_timer", orbTimer);
		bundle.put("pending_orbs", pendingOrbs);
		bundle.put("dark", dark);
		bundle.put("lamps_placed", lampsPlaced);
		bundle.put("zolt_cd", zoltCd);
		bundle.put("guard_cd", guardCd);
		bundle.put("ambush_cd", ambushCd);
		bundle.put("zolt_charge", zoltCharge);
		bundle.put("zolt_dir", zoltDir);
		bundle.put("ambush_cell", ambushCell);
		bundle.put("countered", countered);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt("phase");
		souls = bundle.getInt("souls");
		orbTimer = bundle.getInt("orb_timer");
		pendingOrbs = bundle.getIntArray("pending_orbs");
		if (pendingOrbs == null) pendingOrbs = new int[0];
		dark = bundle.getBoolean("dark");
		lampsPlaced = bundle.getBoolean("lamps_placed");
		zoltCd = bundle.getInt("zolt_cd");
		guardCd = bundle.getInt("guard_cd");
		ambushCd = bundle.getInt("ambush_cd");
		zoltCharge = bundle.getInt("zolt_charge");
		zoltDir = bundle.getInt("zolt_dir");
		ambushCell = bundle.getInt("ambush_cell");
		countered = bundle.getBoolean("countered");
		BossHealthBar.assignBoss(this);
		if (phase == 3) BossHealthBar.bleed(true);
	}

	// ===== 그림 =====

	public static class WraithKingSprite extends MobSprite {

		private boolean crawling = false;

		public WraithKingSprite(){
			super();
			MazeBossSprites.apply(this, MazeBossSprites.Look.WRAITHKING);
			play(idle);
		}

		public void crawl(){
			if (crawling) return;
			crawling = true;
			boolean dying = curAnim == die;
			//게임 사본은 칸마다 양옆에 투명 1픽셀을 두어(24×25) 옆 프레임 픽셀이 번져 보이지 않게 했다 (원본 docs는 22×25)
			MazeBossSprites.applyFile(this, "sprites/maze_b5_wraithking_crawl_sheet.png", 24, MazeBossSprites.Look.WRAITHKING.h);
			if (!dying) play(idle);
		}

		private float reveal = 0;
		private boolean hidden = false;

		//어둠 속 기습: 그 칸에 잠깐 모습을 드러내 한 대 치고, 연출이 끝나면 숨은 자리로 옮긴다
		public void strike( int at, int target ){
			place(at);
			turnTo(at, target);
			reveal = 0.5f;
			alpha(1f);
			hidden = false;
			play(attack);
		}

		@Override
		public void onComplete( Animation anim ) {
			//기습 연출의 공격 동작은 원작 공격 마무리(onAttackComplete)를 부르지 않는다 (피해는 이미 들어감)
			if (anim == attack && reveal > 0){
				idle();
				return;
			}
			super.onComplete(anim);
		}

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof WraithKing) || !ch.isAlive()) return;
			if (reveal > 0){
				reveal -= com.watabou.noosa.Game.elapsed;
				if (reveal <= 0) place(ch.pos);
			}
			//어둠 속에 숨은 동안 그림을 숨긴다. 시야 계산이 visible을 다시 켜도 깜빡이지 않게 투명도로 숨긴다
			boolean hide = ((WraithKing) ch).shrouded() && reveal <= 0;
			if (hide != hidden){
				hidden = hide;
				alpha(hide ? 0f : 1f);
			}
		}
	}
}
