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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ElementTile;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Freezing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Wet;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
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
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 20층 마도시의 여제 (명세 22번) — 여제의 원소 마법을 피하며 싸운다. 그림 maze_b6_empress_sheet.png.
//  공통: 기본공격 없음, 옥좌에 가만히. 원소 상태이상(화상·냉기·정전기)·원소 피해·원소 반응 피해를 받지 않는다.
//    단계가 바뀔 때와 피해를 연속으로 4번 받았을 때 순간이동(맨 끝자락·기둥·주인공 칸 제외).
//    주인공이 투명(은신)인 동안 주인공을 노리는 기술(강화 마탄·전기비·창)은 쓰지 않는다(쿨타임은 흐른다).
//  1단계(100~85%): 꽃잎의 보호(받는 피해 -50%), 강화 마탄(3턴), 전기비(7턴).
//  2단계(85~30%): + 서리창/화염창(15턴마다 번갈아, 머리 위 5턴 → 날아감, 벽에 막힘), 원소 감응(원소 타일).
//  3단계(30~0%): 받는 피해 +50%, 원소 폭풍(가로·세로 3등분을 번갈아, 5턴 경고 → 5턴 폭풍, 5턴 주기).
public class MazeEmpress extends Mob {

	{
		spriteClass = EmpressSprite.class;

		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? MazeTower.B20_HP_STRONG : MazeTower.B20_HP;
		EXP = MazeTower.B20_EXP;
		defenseSkill = MazeTower.B20_DEF;

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);

		//원소 상태이상·원소 피해·반응 피해를 받지 않는다
		immunities.add(Burning.class);
		immunities.add(Chill.class);
		immunities.add(Frost.class);
		immunities.add(Frostbite.class);
		immunities.add(StaticCharge.class);
		immunities.add(Fire.class);
		immunities.add(Freezing.class);
		immunities.add(Electricity.class);
		immunities.add(ElementalReactions.ReactionDamage.class);
	}

	private int phase = 1;
	private int hitStreak = 0, quietTurns = 0;
	private float lastHitTime = -100f;

	private int boltCd = 1;
	private int[] boltPath = null;     //예고한 강화 마탄 궤적 (다음 턴에 쏜다)
	private int boltFrom = -1;         //예고할 때 여제가 있던 칸 (순간이동해도 궤적은 그대로)
	private int rainCd = MazeTower.EM_RAIN_INTERVAL / 2;
	private int[] rainCells = null;    //예고한 전기비 칸

	public int spearType = 0;          //머리 위의 창: 0 없음, ElementTile.FROST / FIRE
	private int spearTimer = 0;
	private int spearMax = 4;          //이번 창이 처음 가진 대기 턴 (아이콘 채움용)
	private int spearCd = 0;
	private int nextSpear = ElementTile.FROST;

	private boolean sensingPending = false;
	private int tileTurns = 0;         //마지막 원소 감응에서 지난 턴

	//원소 폭풍: {축(0 가로 띠, 1 세로 띠), 띠(0~2), 원소, 남은 턴}
	private int[] stormZone = null, warnZone = null;
	private int lastAxis = -1;

	//화면 연출 (저장하지 않음)
	private final ArrayList<MovieClip> boltFx = new ArrayList<>(), rainFx = new ArrayList<>(),
			warnZoneFx = new ArrayList<>(), stormFx = new ArrayList<>();

	public static MazeEmpress find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof MazeEmpress && m.isAlive()) return (MazeEmpress) m;
		}
		return null;
	}

	public int phase(){ return phase; }

	private CityBossLevel level(){
		return Dungeon.level instanceof CityBossLevel ? (CityBossLevel) Dungeon.level : null;
	}

	@Override
	public String name() {
		return Messages.get(DwarfKing.class, "maze_empress_name");
	}

	@Override
	public String description() {
		return Messages.get(DwarfKing.class, "maze_empress_desc");
	}

	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return MazeTower.B20_ACC; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, MazeTower.B20_DR); }
	@Override protected boolean canAttack( Char enemy ) { return false; } //기본공격 없음
	@Override protected boolean getCloser( int target ) { return false; } //걷지 않는다
	@Override protected boolean getFurther( int target ) { return false; }

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
		if (Dungeon.hero.isAlive() && Dungeon.hero.invisible <= 0){ enemy = Dungeon.hero; enemySeen = true; }
		restoreFx();

		if (paralysed > 0){
			spend(TICK);
			return true;
		}


		boolean heroHidden = Dungeon.hero.invisible > 0;

		//(결정) 원소 감응은 모든 기술보다 먼저이고 0.5턴만 쓴다: 2단계 첫 감응은 턴 처음에,
		//그 뒤로는 기술을 쓸 때마다 원소 타일 수를 확인해 85칸(65%)보다 적으면 그 자리에서 0.5턴을 더 써서 다시 깐다.
		tileTurns++;
		if (phase >= 2 && sensingPending){
			castSensing();
			spend(MazeTower.EM_SENSING_TIME);
			return true;
		}

		boolean usedSkill = false;

		//지난 턴에 예고한 공격
		if (boltPath != null){ fireBolt(); usedSkill = true; }
		if (rainCells != null){ fireRain(); usedSkill = true; }

		//머리 위의 창: 3~4턴 뒤 날아간다 (주인공이 숨어 있으면 은신이 풀린 뒤 다음 턴에)
		if (spearType != 0){
			spearTimer--;
			if (spearTimer <= 0 && !heroHidden){ throwSpear(); usedSkill = true; }
			else showSpearCount();
		}

		if (phase == 3) stormTick();

		boltCd--; rainCd--; spearCd--;

		//한 턴에 새 기술 하나
		if (!heroHidden && boltCd <= 0 && boltPath == null){
			warnBolt();
			boltCd = MazeTower.EM_BOLT_INTERVAL;
			usedSkill = true;
		} else if (!heroHidden && rainCd <= 0 && rainCells == null){
			warnRain();
			rainCd = MazeTower.EM_RAIN_INTERVAL;
			usedSkill = true;
		} else if (phase >= 2 && spearType == 0 && spearCd <= 0){
			spearType = nextSpear;
			nextSpear = nextSpear == ElementTile.FROST ? ElementTile.FIRE : ElementTile.FROST;
			spearTimer = Random.IntRange(MazeTower.EM_SPEAR_DELAY_MIN, MazeTower.EM_SPEAR_DELAY_MAX); //(결정) 3~4턴 뒤
			spearMax = spearTimer;
			spearCd = MazeTower.EM_SPEAR_INTERVAL;
			Buff.affect(this, SpearCountdown.class);
			showSpearCount();
			if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, spearType == ElementTile.FROST ? "frost_spear" : "fire_spear"));
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.7f, 1.2f);
			usedSkill = true;
		}

		//(결정, 완화) 기술을 썼든 안 썼든 매 턴 확인: 원소 타일이 모자라면 0.5턴을 더 써서 깐다
		if (phase >= 2 && needSensing()){
			castSensing();
			spend(TICK + MazeTower.EM_SENSING_TIME);
			return true;
		}

		spend(TICK);
		return true;
	}

	// ----- 강화 마탄: 1턴 전 꿰뚫을 직선 궤적에 경고, 벽을 꿰뚫는다 -----

	private void warnBolt(){
		CityBossLevel l = level();
		if (l == null) return;
		Ballistica b = new Ballistica(pos, Dungeon.hero.pos, 0);
		ArrayList<Integer> path = new ArrayList<>();
		for (int c : b.path){
			if (c == pos) continue;
			if (CityBossLevel.rcOf(c) == null) break; //방(13×13)을 벗어나면 끝
			path.add(c);
		}
		if (path.isEmpty()) return;
		boltFrom = pos;
		boltPath = new int[path.size()];
		for (int i = 0; i < path.size(); i++) boltPath[i] = path.get(i);
		showBoltWarn();
	}

	private float boltAngle(){
		PointF a = DungeonTilemap.tileCenterToWorld(pos);
		PointF b = DungeonTilemap.tileCenterToWorld(boltPath[boltPath.length - 1]);
		return (float)(Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI);
	}

	//궤적 경고: 칸마다 그림을 돌려 붙이면 대각선에서 깨져서, 이어진 빛줄기를 코드로 그린다 (effects/BoltWarnLine)
	private com.shatteredpixel.shatteredpixeldungeon.effects.BoltWarnLine boltLine;

	private void clearBoltLine(){
		if (boltLine != null){
			boltLine.killAndErase();
			boltLine = null;
		}
	}

	private void showBoltWarn(){
		clear(boltFx);
		clearBoltLine();
		if (boltPath == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		PointF a = DungeonTilemap.tileCenterToWorld(boltFrom >= 0 ? boltFrom : pos);
		PointF b = DungeonTilemap.tileCenterToWorld(boltPath[boltPath.length - 1]);
		//마지막 칸 끝까지 닿도록 반 칸 더 늘린다
		float dx = b.x - a.x, dy = b.y - a.y, l = (float)Math.sqrt(dx*dx + dy*dy);
		if (l > 0){
			b = new PointF(b.x + dx / l * DungeonTilemap.SIZE / 2f, b.y + dy / l * DungeonTilemap.SIZE / 2f);
		}
		boltLine = new com.shatteredpixel.shatteredpixeldungeon.effects.BoltWarnLine(a, b);
		Dungeon.hero.sprite.parent.addToBack(boltLine); //캐릭터 아래 바닥 쪽에
	}

	private void fireBolt(){
		clear(boltFx);
		clearBoltLine();
		int[] path = boltPath;
		boltPath = null;
		//날아가는 강화 마탄
		if (sprite != null){
			int end = path[path.length - 1];
			MovieClip m = BossEffects.clip(BossEffects.Fx.EM_BOLT, 12, true);
			PointF a = DungeonTilemap.tileCenterToWorld((boltFrom >= 0 ? boltFrom : pos)), b = DungeonTilemap.tileCenterToWorld(end);
			m.origin.set(8, 8);
			m.x = a.x - 8; m.y = a.y - 8;
			m.angle = (float)(Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI);
			GameScene.effect(m);
			final MovieClip mm = m;
			m.parent.add(new PosTweener(m, new PointF(b.x - 8, b.y - 8), 0.04f * path.length){
				@Override
				protected void onComplete() {
					mm.killAndErase();
				}
			});
		}
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		for (int c : path){
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || ch.alignment == Alignment.ENEMY) continue;
			ch.damage(Random.NormalIntRange(MazeTower.EM_BOLT_MIN, MazeTower.EM_BOLT_MAX), this);
			if (ch.sprite != null) ch.sprite.flash();
			if (ch == Dungeon.hero && !ch.isAlive()) Dungeon.fail(this);
		}
	}

	// ----- 전기비: 1턴 전 솟을 칸에 경고. 전기 피해 + 정전기 3턴 -----

	private void warnRain(){
		CityBossLevel l = level();
		if (l == null) return;
		int h = Dungeon.hero.pos;
		int w = Dungeon.level.width();
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(h);
		//(결정) 주인공 둘레 6×6칸 안 (6은 짝수라 주인공이 가운데 2×2 중 한 칸에 오도록 창을 무작위로 놓는다)
		int left = h % w - (MazeTower.EM_RAIN_AREA / 2 - Random.Int(2));
		int top  = h / w - (MazeTower.EM_RAIN_AREA / 2 - Random.Int(2));
		ArrayList<Integer> near = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			int x = c % w, y = c / w;
			if (c != h && x >= left && x < left + MazeTower.EM_RAIN_AREA && y >= top && y < top + MazeTower.EM_RAIN_AREA) near.add(c);
		}
		while (cells.size() < MazeTower.EM_RAIN_CELLS && !near.isEmpty()){
			cells.add(near.remove(Random.Int(near.size())));
		}
		rainCells = new int[cells.size()];
		for (int i = 0; i < cells.size(); i++) rainCells[i] = cells.get(i);
		showRainWarn();
	}

	//경고: 명암을 섞은 노란 빛 고임 + 튀는 불꽃 (effects/CellWarnFx)
	private com.shatteredpixel.shatteredpixeldungeon.effects.CellWarnFx rainLook;

	private void showRainWarn(){
		clear(rainFx);
		clearRainLook();
		if (rainCells == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		ArrayList<Integer> list = new ArrayList<>();
		for (int c : rainCells) list.add(c);
		rainLook = new com.shatteredpixel.shatteredpixeldungeon.effects.CellWarnFx(list, 0x3A3208, 0xC9A52A, 0xFFF0A0, 0xFFE066);
		rainLook.attach(Dungeon.hero.sprite.parent);
	}

	private void clearRainLook(){
		if (rainLook != null){
			rainLook.kill();
			rainLook = null;
		}
	}

	private void fireRain(){
		clear(rainFx);
		clearRainLook();
		int[] cells = rainCells;
		rainCells = null;
		boolean seen = false;
		for (int c : cells){
			if (Dungeon.level.heroFOV[c]){
				seen = true;
				BossEffects.atCellBottom(BossEffects.Fx.EM_RAIN, c, 12, false);
			}
			Char ch = Actor.findChar(c);
			if (ch == null || ch == this || ch.alignment == Alignment.ENEMY) continue;
			ch.damage(Random.NormalIntRange(MazeTower.EM_RAIN_MIN, MazeTower.EM_RAIN_MAX), new Electricity());
			if (!ch.isAlive()){
				if (ch == Dungeon.hero) Dungeon.fail(this);
				continue;
			}
			Buff.affect(ch, StaticCharge.class, MazeTower.EM_RAIN_STATIC);
			tileReaction(ch, ElementTile.SHOCK);
		}
		if (seen) Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
	}

	// ----- 서리창·화염창 -----

	private void throwSpear(){
		final int type = spearType;
		spearType = 0;
		Buff.detach(this, SpearCountdown.class);
		Ballistica b = new Ballistica(pos, Dungeon.hero.pos, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET);
		int end = b.collisionPos;
		boolean blocked = Dungeon.level.solid[end];
		if (blocked){
			//벽 앞에서 멈춘다
			end = b.dist >= 1 ? b.path.get(b.dist - 1) : pos;
		}
		final int hitCell = end;
		final boolean hitHero = !blocked && end == Dungeon.hero.pos;

		BossEffects.Fx fly = type == ElementTile.FROST ? BossEffects.Fx.EM_FROST_FLY : BossEffects.Fx.EM_FIRE_FLY;
		final BossEffects.Fx hit = type == ElementTile.FROST ? BossEffects.Fx.EM_FROST_HIT : BossEffects.Fx.EM_FIRE_HIT;
		PointF a = sprite != null ? new PointF(sprite.center().x, sprite.y - 4) : DungeonTilemap.tileCenterToWorld(pos);
		PointF z = DungeonTilemap.tileCenterToWorld(hitCell);
		final MovieClip m = BossEffects.clip(fly, 12, true);
		m.origin.set(fly.w - 3, fly.h / 2f); //창끝 기준으로 돌린다
		m.x = a.x - (fly.w - 3);
		m.y = a.y - fly.h / 2f;
		m.angle = (float)(Math.atan2(z.y - a.y, z.x - a.x) * 180 / Math.PI);
		GameScene.effect(m);
		m.parent.add(new PosTweener(m, new PointF(z.x - (fly.w - 3), z.y - fly.h / 2f), 0.25f){
			@Override
			protected void onComplete() {
				m.killAndErase();
				BossEffects.atCell(hit, hitCell, 12, false);
			}
		});
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);

		if (hitHero){
			Char ch = Dungeon.hero;
			ch.damage(Random.NormalIntRange(MazeTower.EM_SPEAR_MIN, MazeTower.EM_SPEAR_MAX), this);
			if (!ch.isAlive()){
				Dungeon.fail(this);
				return;
			}
			if (type == ElementTile.FROST) Buff.affect(ch, Frostbite.class, MazeTower.EM_SPEAR_FROSTBITE);
			else                           Buff.affect(ch, Burning.class).reignite(ch);
			tileReaction(ch, type);
		}
	}

	//창이 날아오기까지 남은 턴을 여제 머리 위에 숫자로 (보스 체력 바 밑 아이콘에도 같은 숫자)
	private void showSpearCount(){
		if (spearType == 0 || sprite == null || !Dungeon.level.heroFOV[pos]) return;
		int left = Math.max(0, spearTimer);
		int color = spearType == ElementTile.FROST ? 0x88CCFF : 0xFF8844;
		sprite.showStatus(color, left > 0 ? Integer.toString(left) : "!");
	}

	//보스 체력 바 밑에 보이는 창 아이콘: 남은 턴을 숫자로 보여 준다
	public static class SpearCountdown extends Buff {

		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		private MazeEmpress empress(){
			return target instanceof MazeEmpress ? (MazeEmpress) target : null;
		}

		@Override
		public boolean act() {
			MazeEmpress e = empress();
			if (e == null || e.spearType == 0) detach();
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			MazeEmpress e = empress();
			if (e == null || e.spearType == 0) return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE;
			return e.spearType == ElementTile.FROST ? com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.FROST
					: com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.FIRE;
		}

		@Override
		public float iconFadePercent() {
			MazeEmpress e = empress();
			if (e == null) return 0;
			return Math.max(0, Math.min(1, 1f - Math.max(0, e.spearTimer) / (float) Math.max(1, e.spearMax)));
		}

		@Override
		public String iconTextDisplay() {
			MazeEmpress e = empress();
			return e == null ? "" : Integer.toString(Math.max(0, e.spearTimer));
		}

		@Override
		public String name() {
			MazeEmpress e = empress();
			return Messages.get(this, e != null && e.spearType == ElementTile.FIRE ? "name_fire" : "name_frost");
		}

		@Override
		public String desc() {
			MazeEmpress e = empress();
			return Messages.get(this, "desc", e == null ? 0 : Math.max(0, e.spearTimer));
		}
	}

	// ----- 원소 감응 -----

	private int tileableCells(){
		CityBossLevel l = level();
		return l == null ? 0 : l.roomFloorCells().size() - 1; //옥좌 칸 제외 (132)
	}

	private boolean needSensing(){
		if (sensingPending) return true;
		int n = ElementTile.count();
		return n < Math.round(tileableCells() * MazeTower.EM_TILE_REFILL); //(결정, 바뀜) 쿨타임 13턴 없이 기술을 쓸 때마다 확인
	}

	private void castSensing(){
		CityBossLevel l = level();
		if (l == null) return;
		sensingPending = false;
		tileTurns = 0;
		int goal = Math.round(tileableCells() * MazeTower.EM_TILE_FILL);
		ArrayList<Integer> empty = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			//빈칸에만 깐다. 주인공이 선 칸은 떠나는 순간 사라지므로 빼서 90%가 바로 깨지지 않게
			if (c == l.mazeThrone() || ElementTile.at(c) != 0 || c == Dungeon.hero.pos) continue;
			empty.add(c);
		}
		int n = ElementTile.count();
		while (n < goal && !empty.isEmpty()){
			int c = empty.remove(Random.Int(empty.size()));
			ElementTile.place(c, 1 + Random.Int(3));
			n++;
		}
		//스킬명을 외친다 (글은 27단계에서 다듬는다), 바닥 전체가 한 번 빛난다
		if (sprite != null) sprite.showStatus(0xFFDDEE, Messages.get(this, "sensing_shout"));
		GLog.n(Messages.get(this, "sensing"));
		for (int c : l.roomFloorCells()){
			if (Dungeon.level.heroFOV[c]) CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 1);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 1f, 1.5f);
	}

	//원소 공격이 원소 타일 위의 대상에게 닿으면 반응 (원소 타일 자체는 그대로)
	private void tileReaction( Char ch, int element ){
		if (!ch.isAlive()) return;
		ElementTile.react(ch, element, ElementTile.at(ch.pos));
		if (ch == Dungeon.hero && !ch.isAlive()) Dungeon.fail(this);
	}

	// ----- 원소 폭풍 -----

	private int[] newZone( int notElement ){
		int axis = lastAxis == -1 ? Random.Int(2) : 1 - lastAxis;
		lastAxis = axis;
		int el;
		do { el = 1 + Random.Int(3); } while (el == notElement);
		//(결정) 가운데 띠(가로·세로의 중앙)에는 생기지 않는다: 가장자리 띠(0 또는 2)만. 넓힌 띠라 가운데를 한 칸 살짝 침범한다
		return new int[]{axis, Random.Int(2) == 0 ? 0 : 2, el, MazeTower.EM_STORM_WARN};
	}

	private static boolean inBand( int band, int v ){
		//(결정, 바뀜) 3등분(0~3 / 4~8 / 9~12)보다 한 칸씩 넓게: 0~4 / 3~9 / 8~12 (이웃 띠와 한 칸 겹친다)
		if (band == 0) return v <= 4;
		if (band == 1) return v >= 3 && v <= 9;
		return v >= 8;
	}

	private ArrayList<Integer> zoneCells( int[] z ){
		ArrayList<Integer> cells = new ArrayList<>();
		CityBossLevel l = level();
		if (l == null || z == null) return cells;
		for (int c : l.roomFloorCells()){
			int[] rc = CityBossLevel.rcOf(c);
			if (inBand(z[1], z[0] == 0 ? rc[0] : rc[1])) cells.add(c);
		}
		return cells;
	}

	private void stormTick(){
		if (warnZone == null && stormZone == null){
			warnZone = newZone(0);
			warnZone[3] = MazeTower.EM_STORM_FIRST_WARN; //(결정) 처음 폭풍은 경고 3턴
			showZoneFx();
			return;
		}
		//폭풍
		if (stormZone != null){
			stormZone[3]--;
			ArrayList<Integer> cells = zoneCells(stormZone);
			Char h = Dungeon.hero;
			if (cells.contains(h.pos) && h.isAlive()){
				int el = stormZone[2];
				h.damage(Random.NormalIntRange(MazeTower.EM_STORM_MIN, MazeTower.EM_STORM_MAX),
						el == ElementTile.SHOCK ? new Electricity() : this);
				if (!h.isAlive()){
					Dungeon.fail(this);
				} else {
					if (el == ElementTile.FIRE)       Buff.affect(h, Burning.class).reignite(h);
					else if (el == ElementTile.FROST) Buff.affect(h, Chill.class, 2f);
					else                              Buff.affect(h, StaticCharge.class, 2f);
					//폭풍은 원소 타일과도 반응한다 (반응한 타일은 지워지지 않는다)
					tileReaction(h, el);
				}
			}
			if (stormZone[3] <= 0){
				stormZone = null;
				clear(stormFx);
				if (stormLook != null){ stormLook.kill(); stormLook = null; }
			}
		}
		//경고
		if (warnZone != null){
			warnZone[3]--;
			if (warnZone[3] <= 0){
				//경고가 끝나는 순간 폭풍이 시작되고, 같은 순간 다음 구역의 경고가 시작된다
				stormZone = warnZone;
				stormZone[3] = MazeTower.EM_STORM_TURNS;
				warnZone = newZone(stormZone[2]);
				showZoneFx();
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f);
			}
		}
	}

	//경고·폭풍 연출 (effects/StormZoneFx: 명암 있는 바닥 + 불씨·눈송이·번개를 코드로)
	private com.shatteredpixel.shatteredpixeldungeon.effects.StormZoneFx warnZoneLook, stormLook;

	private void clearZoneLooks(){
		if (warnZoneLook != null){ warnZoneLook.kill(); warnZoneLook = null; }
		if (stormLook != null){ stormLook.kill(); stormLook = null; }
	}

	private void showZoneFx(){
		clear(warnZoneFx);
		clear(stormFx);
		clearZoneLooks();
		if (Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
		if (warnZone != null){
			warnZoneLook = new com.shatteredpixel.shatteredpixeldungeon.effects.StormZoneFx(zoneCells(warnZone), warnZone[2], false);
			warnZoneLook.attach(Dungeon.hero.sprite.parent);
		}
		if (stormZone != null){
			stormLook = new com.shatteredpixel.shatteredpixeldungeon.effects.StormZoneFx(zoneCells(stormZone), stormZone[2], true);
			stormLook.attach(Dungeon.hero.sprite.parent);
		}
	}

	// ----- 순간이동 -----

	private void teleport(){
		CityBossLevel l = level();
		if (l == null) return;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : l.roomFloorCells()){
			if (l.roomEdge(c) || c == Dungeon.hero.pos || c == pos || Actor.findChar(c) != null) continue;
			cells.add(c);
		}
		if (cells.isEmpty()) return;
		if (Dungeon.level.heroFOV[pos]) BossEffects.atCell(BossEffects.Fx.EM_TELEPORT, pos, 12, false);
		pos = Random.element(cells);
		if (sprite != null) sprite.place(pos);
		Dungeon.level.occupyCell(this);
		if (Dungeon.level.heroFOV[pos]) BossEffects.atCell(BossEffects.Fx.EM_TELEPORT, pos, 12, false);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		hitStreak = 0;
		//예고한 공격의 위치는 그대로 두되, 궤적 그림은 새 자리에 맞춰 다시 그린다
		if (boltPath != null) showBoltWarn();
	}

	// ===== 피해 =====

	//원소 피해 (화염·냉기·전격·원소 반응)
	private static boolean elemental( Object src ){
		return src instanceof ElementalReactions.ReactionDamage
				|| src instanceof Burning || src instanceof Fire || src instanceof Freezing
				|| src instanceof Chill || src instanceof Frost || src instanceof Frostbite
				|| src instanceof Electricity || src instanceof StaticCharge
				|| ElementalReactions.isElectric(src)
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (elemental(src)){
			if (sprite != null && Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "immune"));
			return;
		}
		if (phase == 1) dmg = Math.round(dmg * MazeTower.EM_PETAL_TAKEN);   //꽃잎의 보호
		if (phase == 3) dmg = Math.round(dmg * MazeTower.EM_PHASE3_TAKEN);  //시든 꽃관
		int pre = HP;
		super.damage(dmg, src);
		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
			lock.addTime((pre - HP) / 3f);
		}
		if (!isAlive() || pre == HP) return;

		//연속 피격: 마지막으로 맞은 뒤 실제 게임 시간으로 2턴이 넘게 지났으면 처음부터 센다
		//(예전에는 여제가 행동한 횟수로 셌는데, 0.5턴 원소 감응도 한 번으로 세어져 연속 2대가 끊기곤 했다)
		if (Actor.now() - lastHitTime > MazeTower.EM_HIT_RESET_TURNS) hitStreak = 0;
		lastHitTime = Actor.now();
		hitStreak++;
		if (phase == 1 && HP <= HT * MazeTower.EM_PHASE2_HP){
			enterPhase2();
			if (HP <= HT * MazeTower.EM_PHASE3_HP) enterPhase3();
			teleport();
		} else if (phase == 2 && HP <= HT * MazeTower.EM_PHASE3_HP){
			enterPhase3();
			teleport();
		} else if (hitStreak >= MazeTower.EM_TELEPORT_HITS){
			teleport();
		}
	}

	private void enterPhase2(){
		phase = 2;
		sensingPending = true;
		spearCd = 0;
		yell(Messages.get(this, "phase2"));
	}

	private void enterPhase3(){
		phase = 3;
		BossHealthBar.bleed(true);
		yell(Messages.get(this, "phase3"));
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
		clear(boltFx); clear(rainFx); clear(warnZoneFx); clear(stormFx);
		clearRainLook();
		clearZoneLooks();
		clearBoltLine();
		boltPath = null; rainCells = null; spearType = 0; stormZone = null; warnZone = null;
		ElementTile.removeAll();
		Dungeon.level.mazeBossPos = pos; //보스가 쓰러진 자리 (역행 중 흔적)
		GameScene.bossSlain();
		super.die(cause);
		MazeBoss20.afterDeath(this);
		yell(Messages.get(this, "defeated"));
	}

	private static void clear( ArrayList<MovieClip> list ){
		for (MovieClip m : list) m.killAndErase();
		list.clear();
	}

	//불러온 뒤 화면 연출을 다시 만든다
	private void restoreFx(){
		if (boltPath != null && boltLine == null) showBoltWarn();
		if (rainCells != null && rainLook == null) showRainWarn();
		if ((warnZone != null && warnZoneLook == null) || (stormZone != null && stormLook == null)) showZoneFx();
		for (MovieClip m : boltFx)     m.visible = true;
	}

	// ===== 저장 =====

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("phase", phase);
		bundle.put("hit_streak", hitStreak);
		bundle.put("quiet", quietTurns);
		bundle.put("last_hit", lastHitTime);
		bundle.put("bolt_cd", boltCd);
		if (boltPath != null) bundle.put("bolt_path", boltPath);
		bundle.put("bolt_from", boltFrom);
		bundle.put("rain_cd", rainCd);
		if (rainCells != null) bundle.put("rain_cells", rainCells);
		bundle.put("spear_type", spearType);
		bundle.put("spear_timer", spearTimer);
		bundle.put("spear_max", spearMax);
		bundle.put("spear_cd", spearCd);
		bundle.put("next_spear", nextSpear);
		bundle.put("sensing_pending", sensingPending);
		bundle.put("tile_turns", tileTurns);
		if (stormZone != null) bundle.put("storm_zone", stormZone);
		if (warnZone != null) bundle.put("warn_zone", warnZone);
		bundle.put("last_axis", lastAxis);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt("phase");
		hitStreak = bundle.getInt("hit_streak");
		quietTurns = bundle.getInt("quiet");
		lastHitTime = bundle.contains("last_hit") ? bundle.getFloat("last_hit") : -100f;
		boltCd = bundle.getInt("bolt_cd");
		boltPath = bundle.contains("bolt_path") ? bundle.getIntArray("bolt_path") : null;
		boltFrom = bundle.contains("bolt_from") ? bundle.getInt("bolt_from") : -1;
		rainCd = bundle.getInt("rain_cd");
		rainCells = bundle.contains("rain_cells") ? bundle.getIntArray("rain_cells") : null;
		spearType = bundle.getInt("spear_type");
		spearTimer = bundle.getInt("spear_timer");
		spearMax = bundle.contains("spear_max") ? bundle.getInt("spear_max") : 4;
		spearCd = bundle.getInt("spear_cd");
		nextSpear = bundle.getInt("next_spear");
		if (nextSpear == 0) nextSpear = ElementTile.FROST;
		sensingPending = bundle.getBoolean("sensing_pending");
		tileTurns = bundle.getInt("tile_turns");
		stormZone = bundle.contains("storm_zone") ? bundle.getIntArray("storm_zone") : null;
		warnZone = bundle.contains("warn_zone") ? bundle.getIntArray("warn_zone") : null;
		lastAxis = bundle.getInt("last_axis");
		BossHealthBar.assignBoss(this);
		if (phase == 3) BossHealthBar.bleed(true);
	}

	// ===== 그림 =====

	//여제 그림 + 1단계 꽃잎의 보호(뒤 → 여제 → 앞) + 머리 위의 창
	public static class EmpressSprite extends MobSprite {

		private MovieClip petalBack, petalFront, spear;
		private int spearShown = 0;

		public EmpressSprite(){
			super();
			TextureFilm f = MazeBossSprites.apply(this, MazeBossSprites.Look.EMPRESS);
			idle = MazeBossSprites.anim(f, 1, true, MazeBossSprites.IDLE);
			idle.delay = 0.8f; //대기 2프레임은 한 프레임에 약 0.8초
			play(idle);
		}

		@Override
		public void update() {
			super.update();
			if (!(ch instanceof MazeEmpress) || parent == null) return;
			MazeEmpress e = (MazeEmpress) ch;
			boolean alive = e.isAlive();

			//꽃잎의 보호 (1단계)
			boolean guard = alive && e.phase == 1;
			if (guard && petalBack == null){
				petalBack = BossEffects.clip(BossEffects.Fx.EM_PETAL_BACK, 5, true);
				petalFront = BossEffects.clip(BossEffects.Fx.EM_PETAL_FRONT, 5, true);
				parent.addToBack(petalBack);
				parent.add(petalFront);
			} else if (!guard && petalBack != null){
				petalBack.killAndErase();
				petalFront.killAndErase();
				petalBack = petalFront = null;
			}
			if (petalBack != null){
				//오른쪽을 볼 때 여제 프레임보다 왼쪽 6·위쪽 6, 좌우 반전 때 왼쪽 4·위쪽 6
				float ox = flipHorizontal ? -4 : -6;
				petalBack.x = petalFront.x = x + ox;
				petalBack.y = petalFront.y = y - 6;
				petalBack.visible = petalFront.visible = visible;
			}

			//머리 위의 창
			int want = alive ? e.spearType : 0;
			if (want != spearShown){
				if (spear != null){ spear.killAndErase(); spear = null; }
				spearShown = want;
				if (want != 0){
					spear = BossEffects.clip(want == ElementTile.FROST ? BossEffects.Fx.EM_FROST_OVER : BossEffects.Fx.EM_FIRE_OVER, 6, true);
					parent.add(spear);
				}
			}
			if (spear != null){
				PointF c = center();
				spear.origin.set(10, 3.5f);
				//창끝(오른쪽)이 주인공 쪽을 향하게
				boolean heroLeft = Dungeon.hero != null && Dungeon.hero.sprite != null && Dungeon.hero.sprite.center().x < c.x;
				spear.scale.x = heroLeft ? -1 : 1;
				spear.x = c.x - 10;
				spear.y = y - 9;
				spear.visible = visible;
			}
		}

		@Override
		public void kill() {
			if (petalBack != null){ petalBack.killAndErase(); petalFront.killAndErase(); }
			if (spear != null) spear.killAndErase();
			super.kill();
		}
	}
}
