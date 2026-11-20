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

	//Maze Tower: 기습 중(푸른 눈)과 어둠 속 왕에게는 체력바를 띄우지 않는다
	public static boolean hideHealth( Char c ){
		if (c instanceof WraithEyes) return true;
		return c instanceof WraithKing && ((WraithKing) c).isDark();
	}

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
		//(결정) 밝은 상태에서는 원거리 공격: 영혼 화살이 닿는 곳이면 떨어져서도 쏜다
		if (super.canAttack(enemy)) return true;
		return new com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica(pos, enemy.pos,
				com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica.MAGIC_BOLT).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		//(결정) 기본 공격은 붙어 있든 떨어져 있든 마법 공격(영혼 화살)
		boolean visible = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];
		if (visible && sprite instanceof WraithKingSprite){
			((WraithKingSprite) sprite).shootSoul(enemy.pos);
			return false;
		}
		soulBolt();
		return true;
	}

	//영혼 화살이 닿았다 (그림에서 부른다)
	public void onSoulBoltHit(){
		soulBolt();
		next();
	}

	private void soulBolt(){
		spend(attackDelay());
		Char e = enemy;
		if (e == null) return;
		if (hit(this, e, true)){ //마법 공격
			int dmg = damageRoll();
			//(결정) 기본 공격은 마법: 피해 -50%, 마법이라 방어력을 무시한다
			dmg = Math.round(dmg * MazeTower.WK_BASIC_MULTI);
			e.damage(dmg, this);
			if (e == Dungeon.hero && !e.isAlive()) Dungeon.fail(this);
		} else if (e.sprite != null){
			e.sprite.showStatus(CharSprite.NEUTRAL, e.defenseVerb());
		}
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
		if (phase == 3) return actCrawl();

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
			//(결정, 바뀜) 주인공이 먹으면 구슬만 땅에서 사라진다 (신속 없음)
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
		//(결정, 바뀜) 벽에 붙은 칸에도 생길 수 있다. 처음 생길 때 주인공 둘레 2칸 안에는 생기지 않고,
		//등불끼리는 서로 WK_LAMP_GAP칸 넘게 떨어진다(가까운 3칸 안에는 생기지 않음). 왕좌 칸 제외.
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (c == l.mazeThrone() || Actor.findChar(c) != null) continue;
			if (Dungeon.level.distance(c, Dungeon.hero.pos) <= MazeTower.WK_LAMP_HERO_GAP) continue;
			cells.add(c);
		}
		ArrayList<Integer> placed = new ArrayList<>();
		for (int i = 0; i < MazeTower.WK_LAMPS && !cells.isEmpty(); i++){
			int c = cells.remove(Random.Int(cells.size()));
			boolean tooClose = false;
			for (int p : placed) if (Dungeon.level.distance(p, c) < MazeTower.WK_LAMP_GAP) tooClose = true;
			if (tooClose){ i--; continue; }
			placed.add(c);
			MemoryLamp lamp = new MemoryLamp();
			lamp.pos = c;
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
	public static void noise( int cell, int priority ){
		if (Dungeon.level == null) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof WraithGuard) ((WraithGuard) m).hear(cell, priority);
		}
	}

	// ----- 졸트라크 -----

	private void startZoltraak(){
		//(결정) 주인공 쪽으로 조준한다: 8방향(대각선 포함) 중 가장 가까운 방향
		int dx = Dungeon.hero.pos % Dungeon.level.width() - pos % Dungeon.level.width();
		int dy = Dungeon.hero.pos / Dungeon.level.width() - pos / Dungeon.level.width();
		double a = Math.atan2(dy, dx);
		zoltDir = (int)Math.floorMod(Math.round(a / (Math.PI / 4)), 8L);
		zoltCharge = MazeTower.WK_ZOLT_CHARGE;
		if (sprite != null) sprite.turnTo(pos, Dungeon.hero.pos);
		if (sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).raiseStaff(); //지팡이를 들어 구슬이 빛남
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
				MovieClip w = BossEffects.atCell(BossEffects.Fx.WK_ZOLT_WARN, c, 4, true);
				//(결정) 어둠 속에서도 범위 경고가 전부 보인다 (안개 위로 옮김)
				if (w.parent != null) w.parent.erase(w);
				GameScene.effectOverFog(w);
				zoltWarn.add(w);
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

	//8방향: 0 오른쪽, 1 오른쪽 아래, 2 아래, 3 왼쪽 아래, 4 왼쪽, 5 왼쪽 위, 6 위, 7 오른쪽 위
	private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
	private static final int[] DY = {0, 1, 1, 1, 0, -1, -1, -1};

	//폭 3칸, 벽에 막힐 때까지. lane[i] = 각 칸이 몇 번째 줄(-1, 0, 1)인지
	private ArrayList<Integer> zoltCells(){
		return zoltCells(null);
	}

	//옆 줄이 가운데 줄에서 떨어진 칸 (가로·세로는 직각 방향, 대각선은 가로 한 칸 / 세로 한 칸 옆의 나란한 대각선)
	private int[] laneOffset( int k ){
		int dx = DX[zoltDir], dy = DY[zoltDir];
		if (k == 0) return new int[]{0, 0};
		//대각선: 가로·세로로 |k|칸 옆의 나란한 대각선 ((결정) 대각선은 양쪽으로 한 줄씩 더, k = -2 ~ 2)
		if (dx != 0 && dy != 0) return k < 0 ? new int[]{dx * -k, 0} : new int[]{0, dy * k};
		return new int[]{-dy * k, dx * k};
	}

	private ArrayList<Integer> zoltCells( ArrayList<Integer> lanes ){
		ArrayList<Integer> cells = new ArrayList<>();
		int w = Dungeon.level.width();
		int x0 = pos % w, y0 = pos / w;
		int dx = DX[zoltDir], dy = DY[zoltDir];
		int reach = (DX[zoltDir] != 0 && DY[zoltDir] != 0) ? 2 : 1; //(결정) 대각선 광선은 한 칸 더 넓게
		for (int k = -reach; k <= reach; k++){
			int[] o = laneOffset(k);
			for (int i = (k == 0 ? 1 : 0); i < 40; i++){
				int x = x0 + o[0] + dx * i, y = y0 + o[1] + dy * i;
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
		zoltCd = MazeTower.WK_ZOLT_INTERVAL - (isDark() ? MazeTower.WK_ZOLT_DARK_CUT : 0); //(결정) 어둠 속에서는 주기가 2턴 짧다
		spend(TICK);
		return true;
	}

	private void fireZoltraak(){
		clearZoltFx();
		if (sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).raiseStaff();
		ArrayList<Integer> lanes = new ArrayList<>();
		ArrayList<Integer> cells = zoltCells(lanes);
		boolean seen = false;
		boolean diag = DX[zoltDir] != 0 && DY[zoltDir] != 0;
		for (int i = 0; i < cells.size(); i++){
			int c = cells.get(i);
			//어둠 속에서도 광선은 보인다. 가로·세로는 칸마다 이어 붙이고, 대각선은 아래에서 줄마다 한 번에 그린다
			if (!diag) beamVisual(c, lanes.get(i));
			if (Dungeon.level.heroFOV[c]) seen = true;
		}
		if (diag){
			//(결정) 대각선은 칸마다 돌린 조각을 붙이면 이음매가 깨져 보여서, 줄마다 처음 칸부터 끝 칸까지 하나로 길게 늘여 그린다
			for (int k = -2; k <= 2; k++){
				int first = -1, last = -1;
				for (int i = 0; i < cells.size(); i++){
					if (lanes.get(i) != k) continue;
					if (first == -1) first = cells.get(i);
					last = cells.get(i);
				}
				if (first != -1) diagBeamVisual(first, last, k);
			}
		}
		for (int c : cells){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this) continue;
			//왕의 기술은 주인공에게 적대적인 몹(근위병 등)과 등불을 다치게 하지 않는다
			if (ch.alignment == Alignment.ENEMY || ch instanceof MemoryLamp) continue;
			int dmg = Math.round(Random.NormalIntRange(MazeTower.WK_ZOLT_MIN, MazeTower.WK_ZOLT_MAX) * power());
			//(결정) 방어력의 70%를 무시한다 (30%만 적용)
			dmg = Math.max(0, dmg - Math.round(ch.drRoll() * (1f - MazeTower.WK_ZOLT_ARMOR_IGNORE)));
			ch.damage(dmg, this);
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "zolt_kill"));
			}
		}
		if (seen) PixelScene.shake(2, 0.4f);
		Sample.INSTANCE.play(Assets.Sounds.RAY);
	}

	//대각선 한 줄을 처음 칸 ~ 끝 칸까지 하나로 늘여 그린다 (양 끝을 반 칸씩 더 늘려 칸 끝까지 닿게)
	private void diagBeamVisual( int first, int last, int lane ){
		PointF a = DungeonTilemap.tileCenterToWorld(first);
		PointF b = DungeonTilemap.tileCenterToWorld(last);
		float dx = DX[zoltDir], dy = DY[zoltDir];
		float half = DungeonTilemap.SIZE / 2f;
		a = new PointF(a.x - dx * half, a.y - dy * half);
		b = new PointF(b.x + dx * half, b.y + dy * half);
		float len = (float)Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y));
		MovieClip c = BossEffects.clip(edgeLane(lane) ? BossEffects.Fx.WK_ZOLT_EDGE : BossEffects.Fx.WK_ZOLT_CENTER, 15, false);
		//그림 왼쪽 가운데를 a에 두고 광선 방향으로 돌려 길이만큼 늘인다
		c.origin.set(0, 8);
		c.x = a.x;
		c.y = a.y - 8;
		float angle = zoltDir * 45f;
		c.angle = angle;
		c.scale.x = len / 16f;
		//나란한 줄 사이가 비지 않게 두께를 조금 늘린다 (대각선 줄 간격 약 11.3픽셀)
		c.scale.y = 0.8f;
		if (edgeLane(lane)){
			double r = Math.toRadians(angle);
			int[] o = laneOffset(lane);
			double dot = o[0] * Math.sin(r) + o[1] * -Math.cos(r);
			if (dot < 0) c.scale.y = -0.8f;
		}
		GameScene.effectOverFog(c); //(결정) 어둠 속에서도 광선 전체가 보인다 (안개 위)
	}

	//가장 바깥 줄인지 (가로·세로는 ±1, 넓힌 대각선은 ±2). 안쪽 줄은 가운데 줄 그림을 쓴다
	private boolean edgeLane( int lane ){
		int reach = (DX[zoltDir] != 0 && DY[zoltDir] != 0) ? 2 : 1;
		return Math.abs(lane) == reach;
	}

	//가로 그림을 광선 방향으로 돌린다(대각선은 45도 + 길게). 바깥 두 줄은 그림의 위쪽이 바깥을 향하게, 반대쪽 줄은 뒤집는다
	private void beamVisual( int cell, int lane ){
		boolean diag = DX[zoltDir] != 0 && DY[zoltDir] != 0;
		MovieClip c = BossEffects.clip(edgeLane(lane) ? BossEffects.Fx.WK_ZOLT_EDGE : BossEffects.Fx.WK_ZOLT_CENTER, 15, false);
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		c.origin.set(8, 8);
		c.x = p.x - 8;
		c.y = p.y - 8;
		float angle = zoltDir * 45f;
		c.angle = angle;
		if (diag) c.scale.x = 1.42f;
		if (edgeLane(lane)){
			//돌린 뒤 그림의 위쪽 방향 = (sin, -cos). 이 줄이 그쪽에 있으면 그대로, 아니면 뒤집는다
			double r = Math.toRadians(angle);
			int[] o = laneOffset(lane);
			double dot = o[0] * Math.sin(r) + o[1] * -Math.cos(r);
			if (dot < 0) c.scale.y = -1;
		}
		GameScene.effectOverFog(c); //(결정) 어둠 속에서도 광선 전체가 보인다 (안개 위)
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
		//(결정) 눈을 쳐서 넘어가는 피해는 원래의 10%만 (기습 중 받는 피해 90% 감소, WK_COUNTER_*)
		damage(Math.max(1, Math.round(dmg * Random.Float(MazeTower.WK_COUNTER_MIN, MazeTower.WK_COUNTER_MAX))), src);
	}

	private boolean actAmbush(){
		int cell = ambushCell;
		boolean wasCountered = countered;
		removeEyes();
		ambushCell = -1;
		countered = false;
		if (wasCountered){
			if (Dungeon.level.heroFOV[cell]) GLog.p(Messages.get(this, "ambush_blocked"));
		} else if (Dungeon.hero.isAlive()){
			//(결정) 눈을 치지 않았으면 주인공이 어느 쪽으로 움직였든 반드시 덮친다:
			//주인공 바로 옆 칸(눈이 있던 칸이 그대로 옆이면 그 칸)으로 순간이동해 방어력 50%를 무시하는 일격
			//(결정, 바뀜) 왕은 언제나 눈이 있던 칸에서 나타난다 (주인공이 움직였어도 그 칸 그대로). 그 칸이 막혔을 때만 주인공 옆 칸
			int at = -1;
			if (Actor.findChar(cell) == null && Dungeon.level.passable[cell]) at = cell;
			if (at == -1){
				ArrayList<Integer> opts = new ArrayList<>();
				for (int n : PathFinder.NEIGHBOURS8){
					int c = Dungeon.hero.pos + n;
					if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null) opts.add(c);
				}
				if (!opts.isEmpty()) at = Random.element(opts);
			}
			if (at != -1){
				pos = at;
				Dungeon.level.occupyCell(this);
				if (sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).strike(at, Dungeon.hero.pos);
			}
			ambushStrike(Dungeon.hero);
			if (!Dungeon.hero.isAlive()){
				Dungeon.fail(this);
			}
			if (at != -1) vanish(false);
		}
		spend(TICK);
		return true;
	}

	//기습 일격: 반드시 맞고, 방어력을 50%(WK_AMBUSH_ARMOR_IGNORE)만 적용한다
	private void ambushStrike( Char target ){
		int dmg = Math.round(damageRoll() * MazeTower.WK_AMBUSH_DMG); //(결정) 기습 피해 +30%
		int armor = Math.round(target.drRoll() * (1f - MazeTower.WK_AMBUSH_ARMOR_IGNORE));
		dmg = Math.max(0, dmg - armor);
		if (target.sprite != null){
			target.sprite.flash();
			com.shatteredpixel.shatteredpixeldungeon.effects.Wound.hit(target);
		}
		Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
		target.damage(dmg, this);
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
			//쓰러짐 동작과 함께 머리 위 왕관이 깨지고, 끝나면 기어다니는 모습
			if (sprite instanceof WraithKingSprite) ((WraithKingSprite) sprite).collapse();
		}
		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		yell(Messages.get(this, "phase3"));
	}

	//(결정) 3단계: 기어다니는 왕은 주인공에게서 도망친다 (이동 속도 70%).
	//주인공에게서 더 멀어지는 칸이 없고 주인공이 바로 옆에 있을 때만 할퀴기
	private boolean actCrawl(){
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()){
			spend(TICK);
			return true;
		}
		int cur = Dungeon.level.distance(pos, hero.pos);
		int best = -1, bestDist = cur;
		if (!rooted){
			for (int n : PathFinder.NEIGHBOURS8){
				int c = pos + n;
				if (c < 0 || c >= Dungeon.level.length()) continue;
				if (!Dungeon.level.passable[c] || Dungeon.level.avoid[c] || Actor.findChar(c) != null) continue;
				int d = Dungeon.level.distance(c, hero.pos);
				if (d > bestDist || (d == bestDist && best != -1 && Random.Int(2) == 0)){
					if (d > cur){ bestDist = d; best = c; }
				}
			}
		}
		if (best != -1){
			int oldPos = pos;
			move(best);
			spend(1f / speed());
			return moveSprite(oldPos, pos);
		}
		if (Dungeon.level.adjacent(pos, hero.pos) && hero.invisible <= 0){
			enemy = hero;
			if (sprite != null && (sprite.visible || hero.sprite.visible)){
				sprite.attack(hero.pos);
				return false; //공격 동작이 끝나면 onAttackComplete
			}
			attack(hero);
			spend(attackDelay());
			return true;
		}
		spend(TICK);
		return true;
	}

	@Override
	public float speed() {
		float s = super.speed();
		return phase == 3 ? s * MazeTower.WK_CRAWL_SPEED : s;
	}

	// ===== 피해 =====

	@Override
	public void damage( int dmg, Object src ) {
		//3단계: 받는 평타·스킬 피해 +50%
		if (phase == 3 && src instanceof Char){
			dmg = Math.round(dmg * MazeTower.WK_PHASE3_TAKEN);
		}
		//(결정) 단계가 넘어가는 피해는 다음 단계 최대 체력보다 1% 낮은 체력에서 멈춘다 (한 번에 여러 단계를 건너뛰거나 즉사하지 않게)
		float thr = phase == 1 ? MazeTower.WK_PHASE2_HP : phase == 2 ? MazeTower.WK_PHASE3_HP : 0f;
		dmg = MazeBoss20.capPhaseDamage(this, dmg, thr);
		int pre = HP;
		super.damage(dmg, src);
		MazeBoss20.settlePhaseHP(this, pre, thr);
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
		//(결정) 한 대 맞을 때마다 순간이동 (화상·독 같은 지속 피해는 제외). 기어다니는 3단계는 순간이동하지 못한다
		if (phase != 3 && isAlive() && pre != HP && !(src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff)
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
		//(결정) 1단계: 영혼 구슬(떨어진 것·떨어질 예고 포함)에서 2칸 이상 4칸 이내인 칸으로. 그런 칸이 없으면 방 안 아무 칸
		if (phase == 1){
			ArrayList<Integer> orbs = new ArrayList<>();
			for (int c = 0; c < Dungeon.level.length(); c++){
				if (SoulOrb.at(c) || SoulOrb.Warn.at(c)) orbs.add(c);
			}
			ArrayList<Integer> near = new ArrayList<>();
			for (int c : cells){
				int d = Integer.MAX_VALUE;
				for (int o : orbs) d = Math.min(d, Dungeon.level.distance(c, o));
				if (d >= MazeTower.WK_TP_ORB_MIN && d <= MazeTower.WK_TP_ORB_MAX) near.add(c);
			}
			if (!near.isEmpty()) cells = near;
		}
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

		//(결정) 서 있는 시트의 9~12는 사망이 아니라 3단계로 바뀔 때 한 번 재생하는 '쓰러짐'
		private boolean collapsing = false;

		public WraithKingSprite(){
			super();
			setAttack(MazeBossSprites.apply(this, MazeBossSprites.Look.WRAITHKING));
			play(idle);
		}

		//(결정) 공격 6~8: 서 있는 모습은 지팡이를 들어 구슬이 빛나고(인간을 죽이는 마법 기 모으기·발사도 이 동작), 기어다니는 모습은 할퀴기
		private void setAttack( com.watabou.noosa.TextureFilm film ){
			attack = new Animation(12, false);
			attack.frames(film, 6, 7, 8);
			zap = attack.clone();
		}

		//인간을 죽이는 마법 기 모으기·발사: 지팡이를 들어 구슬이 빛나는 동작 (공격 6~8)
		public void raiseStaff(){
			if (!crawling && !collapsing && curAnim != die) play(zap);
		}

		//3단계로 바뀔 때: 쓰러짐(서 있는 시트 9~12)을 한 번 재생하고, 9번과 함께 머리 위에서 왕관이 깨진다. 끝나면 기어다니는 시트
		public void collapse(){
			if (crawling || collapsing) return;
			collapsing = true;
			reveal = 0;
			hidden = false;
			alpha(1f);
			play(die);
			BossEffects.at(BossEffects.Fx.WK_CROWN_BREAK, x + width / 2f, y + 2, 8, false);
		}

		public void crawl(){
			collapsing = false;
			if (crawling) return;
			crawling = true;
			//게임 사본은 칸마다 양옆에 투명 1픽셀을 두어(24×25) 옆 프레임 픽셀이 번져 보이지 않게 했다 (원본 docs는 22×25)
			setAttack(MazeBossSprites.applyFile(this, "sprites/maze_b5_wraithking_crawl_sheet.png", 24, MazeBossSprites.Look.WRAITHKING.h));
			play(idle);
		}

		@Override
		public void play( Animation anim ) {
			//쓰러지는 중에 다른 동작(이동·공격)이 오면 쓰러짐을 끝내고 기어다니는 시트의 같은 동작으로
			if (collapsing && anim != die){
				Animation old = anim;
				Animation oldRun = run, oldAtk = attack, oldZap = zap;
				crawl();
				if (old == oldRun) anim = run;
				else if (old == oldAtk) anim = attack;
				else if (old == oldZap) anim = zap;
				else anim = idle;
			}
			super.play(anim);
		}

		@Override
		public void die() {
			//실제 사망은 기어다니는 시트의 9~12
			if (!crawling) crawl();
			super.die();
		}

		private float reveal = 0;
		private boolean hidden = false;

		//원거리 공격: 지팡이를 휘둘러 푸른 영혼 화살 (원작 그림자 마탄)
		public void shootSoul( final int cell ){
			turnTo(ch.pos, cell);
			play(zap);
			com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.boltFromChar(parent,
					com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile.SHADOW, this, cell,
					new com.watabou.utils.Callback() {
						@Override
						public void call() {
							((WraithKing) ch).onSoulBoltHit();
						}
					});
			com.watabou.noosa.audio.Sample.INSTANCE.play(Assets.Sounds.ZAP);
		}

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
			//쓰러짐이 끝나면 기어다니는 모습 (MobSprite가 사라지게 하지 않도록 먼저 가로챈다)
			if (collapsing && anim == die){
				crawl();
				return;
			}
			if (anim == zap){
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
