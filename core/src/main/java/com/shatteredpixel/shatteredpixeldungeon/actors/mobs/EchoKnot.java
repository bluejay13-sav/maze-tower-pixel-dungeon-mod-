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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.BlackLeaves;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.BArray;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (N7, 사용자): 메아리 매듭 (22~24층 고정 몹). 탑의 주인이 지난 수문장의 힘을 뿌리에 묶어 둔 매듭.
//움직이지도 공격하지도 않는다. 매듭마다 메아리 하나를 가지고, N7_LINK_RANGE칸 안·시선이 닿는 가장 가까운 적 하나와 뿌리 실로 이어져 그 힘을 빌려준다:
//  수액: 물리 피해 -50%, 불타는 동안은 그 감소가 꺼지고 화상 피해를 두 배로 받음 / 강화 마탄: 20층 여제의 강화 마탄(직선 경고 1턴 → 벽까지 꿰뚫음)을 처음 2턴 뒤, 그 뒤 4턴마다
//  광맥: 마석 껍질 방어막(N7_VEIN_SHIELD, N7_VEIN_REFILL턴마다 다시 채움), 그 적이 쓰러지면 껍질이 터져 둘레 1칸의 모두가 피해
//이어진 적이 쓰러지거나 멀어지면 다음 적과 이어진다. 매듭을 끊으면 메아리가 사라지고 매듭도 그냥 사라진다((사용자) 새잎은 남기지 않음). 25층 "근원을 끊으면 메아리가 사라진다"를 미리 배운다.
public class EchoKnot extends Mob {

	public static final int SAP = 0, BOLT = 1, VEIN = 2;

	{
		spriteClass = KnotSprite.class;

		HP = HT = MazeTower.N7_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	public int kind = SAP;
	private int linked = -1; //이어진 적의 Actor id

	@Override public int defenseSkill( Char enemy ) { return 0; } //반드시 맞는다
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }
	@Override protected boolean getCloser( int target ) { return false; }
	@Override protected boolean getFurther( int target ) { return false; }
	@Override protected boolean canAttack( Char enemy ) { return false; }

	@Override
	public boolean add( Buff buff ) {
		return false; //상태이상을 받지 않는다
	}

	@Override
	protected boolean act() {
		if (sprite != null){ sprite.hideAlert(); sprite.hideLost(); }
		relink();
		spend(TICK);
		return true;
	}

	public Mob linkedMob(){
		Actor a = linked == -1 ? null : Actor.findById(linked);
		return a instanceof Mob && ((Mob) a).isAlive() ? (Mob) a : null;
	}

	private boolean canLink( Mob m ){
		if (m == null || m == this || !m.isAlive() || m.alignment != Alignment.ENEMY) return false;
		if (m instanceof EchoKnot || m instanceof ResonanceCrystal || m instanceof ManaVein || m instanceof DemonSpawner) return false;
		if (Dungeon.level.distance(pos, m.pos) > MazeTower.N7_LINK_RANGE) return false;
		return new Ballistica(pos, m.pos, Ballistica.PROJECTILE).collisionPos == m.pos;
	}

	//가장 가까운 적 하나와 이어진다 (이어진 적이 그대로 닿으면 그대로)
	private void relink(){
		Mob cur = linkedMob();
		if (cur != null && !canLink(cur)){ unlink(cur); cur = null; }
		if (cur == null){
			Mob best = null;
			for (Mob m : Dungeon.level.mobs){
				if (!canLink(m)) continue;
				//다른 매듭이 이미 메아리를 준 적은 고르지 않는다 (메아리는 한 적에 하나)
				if (m.buff(Echo.class) != null) continue;
				if (best == null || Dungeon.level.distance(pos, m.pos) < Dungeon.level.distance(pos, best.pos)) best = m;
			}
			if (best != null){
				linked = best.id();
				Echo e = echoClass() == VeinShell.class ? new VeinShell() : echoClass() == BoltEcho.class ? new BoltEcho() : new SapEcho();
				e.knot = id();
				e.attachTo(best);
				if (e instanceof BoltEcho) ((BoltEcho) e).cd = MazeTower.N7_BOLT_FIRST;
				if (Dungeon.level.heroFOV[best.pos]) CellEmitter.center(best.pos).burst(Speck.factory(Speck.LIGHT), 4);
				cur = best;
			} else linked = -1;
		}
		if (cur != null){
			VeinShell v = cur.buff(VeinShell.class);
			if (v != null && v.knot == id()) v.refill();
		}
		if (sprite instanceof KnotSprite) ((KnotSprite) sprite).syncLink();
	}

	private Class<? extends Echo> echoClass(){
		return kind == BOLT ? BoltEcho.class : kind == VEIN ? VeinShell.class : SapEcho.class;
	}

	private void unlink( Mob m ){
		if (m != null){
			for (Echo e : m.buffs(Echo.class)) if (e.knot == id()) e.detach();
		}
		linked = -1;
	}

	@Override
	public void die( Object cause ) {
		unlink(linkedMob());
		if (sprite instanceof KnotSprite) ((KnotSprite) sprite).syncLink();
		//(사용자) 끊으면 새잎을 남기지 않고 그냥 사라진다 (메아리만 사라짐)
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f, 0.8f);
			GLog.p(Messages.get(EchoKnot.class, "cut"));
		}
		super.die(cause);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "echo_" + kind);
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + kind);
	}

	private static final String KIND = "kind", LINKED = "linked";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(KIND, kind);
		bundle.put(LINKED, linked);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		kind = bundle.getInt(KIND);
		linked = bundle.getInt(LINKED);
	}

	// ===== 층을 만들 때 =====

	public static void place( Level level ){
		if (!MazeTower.N7_KNOT || Dungeon.depth < 22 || Dungeon.depth > 24) return;
		Random.pushGenerator(Dungeon.seedCurDepth() ^ 0x7E40C0A7L);
		try {
			boolean[] near = new boolean[level.length()];
			for (int t : new int[]{ level.entrance(), level.exit() }){
				if (t < 0) continue;
				PathFinder.buildDistanceMap(t, BArray.not(level.solid, null), 4);
				for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
			}
			int want = Random.IntRange(MazeTower.N7_KNOTS_MIN, MazeTower.N7_KNOTS_MAX);
			int placed = 0;
			for (int tries = 0; tries < 1000 && placed < want; tries++){
				int c = Random.Int(level.length());
				if (near[c] || !LivingVein.openFloor(level, c)) continue;
				boolean close = false;
				for (Mob m : level.mobs) if (m instanceof EchoKnot && level.distance(m.pos, c) < 6) close = true;
				if (close) continue;
				EchoKnot k = new EchoKnot();
				k.kind = Random.Int(3);
				k.pos = c;
				level.mobs.add(k);
				placed++;
			}
		} finally {
			Random.popGenerator();
		}
	}

	// ===== 메아리 (이어진 적에게 붙는 상태) =====

	public static abstract class Echo extends Buff {
		{ type = buffType.POSITIVE; announced = true; }
		public int knot = -1;
		@Override public boolean act(){
			Actor a = Actor.findById(knot);
			if (!(a instanceof EchoKnot) || !((EchoKnot) a).isAlive() || ((EchoKnot) a).linkedMob() != target) detach();
			spend(TICK);
			return true;
		}
		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put("knot", knot); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); knot = b.getInt("knot"); }
	}

	//수액: 물리 피해 -50%, 불타는 동안은 감소가 꺼지고 화상 피해 두 배 (Char.damage → modifyDamage)
	public static class SapEcho extends Echo {
		@Override public int icon(){ return BuffIndicator.ARMOR; }
		@Override public void tintIcon( Image icon ){ icon.hardlight(0.88f, 0.58f, 0.16f); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round(MazeTower.N7_SAP_REDUCE * 100)); }
	}

	//강화 마탄: 이어진 적이 20층 여제의 강화 마탄을 쓴다 (처음 N7_BOLT_FIRST턴 뒤, 그 뒤 N7_BOLT_CD턴마다, 1턴 직선 경고 → 벽까지 꿰뚫음, 방어 무시)
	public static class BoltEcho extends Echo {
		public int cd = 2;
		private int[] path = null;
		private com.shatteredpixel.shatteredpixeldungeon.effects.BoltWarnLine line;

		@Override public int icon(){ return BuffIndicator.WAND; }
		@Override public void tintIcon( Image icon ){ icon.hardlight(0.9f, 0.6f, 0.75f); }
		@Override public String iconTextDisplay(){ return path != null ? "!" : Integer.toString(Math.max(0, cd)); }
		@Override public String desc(){ return Messages.get(this, "desc", MazeTower.N7_BOLT_CD); }

		@Override
		public boolean act(){
			boolean ok = super.act();
			if (target == null || !target.isAlive() || target.buff(BoltEcho.class) != this){ clearLine(); return ok; } //(점검) 메아리가 떨어졌으면 쏘지 않음
			if (path != null){ fire(); return ok; }
			cd--;
			Mob m = target instanceof Mob ? (Mob) target : null;
			if (cd <= 0 && m != null && m.state == m.HUNTING && m.paralysed <= 0 && Dungeon.hero != null && Dungeon.hero.isAlive()
					&& Dungeon.hero.invisible <= 0 && m.fieldOfView != null && m.fieldOfView[Dungeon.hero.pos]
					&& Dungeon.level.distance(m.pos, Dungeon.hero.pos) >= 2){
				warn();
			}
			return ok;
		}

		private void warn(){
			Ballistica b = new Ballistica(target.pos, Dungeon.hero.pos, Ballistica.STOP_SOLID);
			ArrayList<Integer> cells = new ArrayList<>();
			for (int c : b.path){
				if (c == target.pos) continue;
				if (Dungeon.level.solid[c]) break;
				cells.add(c);
			}
			if (cells.isEmpty()) return;
			path = new int[cells.size()];
			for (int i = 0; i < path.length; i++) path[i] = cells.get(i);
			if (target.sprite != null) target.sprite.showStatus(0xC8A0E8, Messages.get(BoltEcho.class, "warn"));
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.6f, 1.3f);
			showLine();
		}

		public void showLine(){
			clearLine();
			if (path == null || target == null || Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null) return;
			PointF a = DungeonTilemap.tileCenterToWorld(target.pos);
			PointF b = DungeonTilemap.tileCenterToWorld(path[path.length - 1]);
			float dx = b.x - a.x, dy = b.y - a.y, len = (float)Math.sqrt(dx*dx + dy*dy);
			if (len > 0) b = new PointF(b.x + dx / len * DungeonTilemap.SIZE / 2f, b.y + dy / len * DungeonTilemap.SIZE / 2f);
			line = new com.shatteredpixel.shatteredpixeldungeon.effects.BoltWarnLine(a, b);
			Dungeon.hero.sprite.parent.addToBack(line);
		}

		private void clearLine(){
			if (line != null){ line.killAndErase(); line = null; }
		}

		private void fire(){
			clearLine();
			int[] p = path;
			path = null;
			cd = MazeTower.N7_BOLT_CD;
			int end = p[p.length - 1];
			if (target.sprite != null && target.sprite.parent != null){
				com.watabou.noosa.MovieClip m = com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.clip(com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects.Fx.EM_BOLT, 12, true);
				PointF a = DungeonTilemap.tileCenterToWorld(target.pos), b = DungeonTilemap.tileCenterToWorld(end);
				m.origin.set(8, 8);
				m.x = a.x - 8; m.y = a.y - 8;
				m.angle = (float)(Math.atan2(b.y - a.y, b.x - a.x) * 180 / Math.PI);
				GameScene.effect(m);
				final com.watabou.noosa.MovieClip mm = m;
				m.parent.add(new com.watabou.noosa.tweeners.PosTweener(m, new PointF(b.x - 8, b.y - 8), 0.04f * p.length){
					@Override protected void onComplete() { mm.killAndErase(); }
				});
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
			}
			for (int c : p){
				Char ch = Actor.findChar(c);
				if (ch == null || ch == target || ch.alignment == Alignment.ENEMY) continue;
				ch.damage(Random.NormalIntRange(MazeTower.N7_BOLT_MIN, MazeTower.N7_BOLT_MAX), this);
				if (ch.sprite != null) ch.sprite.flash();
				if (ch == Dungeon.hero && !ch.isAlive()){
					Dungeon.fail(this);
					GLog.n(Messages.get(BoltEcho.class, "ondeath"));
				}
			}
		}

		@Override public void detach(){ clearLine(); super.detach(); }
		@Override public void fx( boolean on ){ if (on && path != null) showLine(); }

		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put("cd", cd); if (path != null) b.put("path", path); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); cd = b.getInt("cd"); path = b.contains("path") ? b.getIntArray("path") : null; }
	}

	//광맥: 마석 껍질 방어막 (N7_VEIN_REFILL턴마다 다시 채움). 그 적이 쓰러지면 껍질이 터져 둘레 1칸의 모두가 피해 (Mob.die → onMobDie)
	public static class VeinShell extends Echo {
		private int refillIn = 0;
		void refill(){
			ShellShield s = target.buff(ShellShield.class);
			//(점검) 껍질이 다 깨져 사라졌으면 N7_VEIN_REFILL턴을 기다렸다가 다시 두른다 (예전에는 다음 턴에 바로 가득)
			if (s == null){ if (--refillIn > 0) return; s = new ShellShield(); s.attachTo(target); s.setShield(MazeTower.N7_VEIN_SHIELD); refillIn = MazeTower.N7_VEIN_REFILL; return; }
			if (--refillIn <= 0){ s.setShield(MazeTower.N7_VEIN_SHIELD); refillIn = MazeTower.N7_VEIN_REFILL; }
		}
		@Override public int icon(){ return BuffIndicator.ARMOR; }
		@Override public void tintIcon( Image icon ){ icon.hardlight(0.47f, 0.78f, 1f); }
		@Override public String desc(){ return Messages.get(this, "desc", MazeTower.N7_VEIN_SHIELD, MazeTower.N7_VEIN_REFILL); }
		@Override public void detach(){ if (target != null) Buff.detach(target, ShellShield.class); super.detach(); }
		@Override public void storeInBundle( Bundle b ){ super.storeInBundle(b); b.put("refill", refillIn); }
		@Override public void restoreFromBundle( Bundle b ){ super.restoreFromBundle(b); refillIn = b.getInt("refill"); }
	}

	public static class ShellShield extends ShieldBuff {
		{ type = buffType.POSITIVE; }
		@Override public String desc(){ return Messages.get(this, "desc", shielding()); }
	}

	//피해 출처 (사망 기록)
	public static class ShellBurst {}

	// ===== 원작 코드와 만나는 자리 =====

	//Char.damage 맨 앞: 수액 메아리
	public static int modifyDamage( Char ch, int dmg, Object src ){
		if (dmg <= 0 || ch.buff(SapEcho.class) == null) return dmg;
		boolean burning = ch.buff(Burning.class) != null;
		if (src instanceof Burning) return dmg * MazeTower.N7_SAP_FIRE_MULTI;
		if (src instanceof Char && !burning) return Math.round(dmg * (1f - MazeTower.N7_SAP_REDUCE));
		return dmg;
	}

	//Mob.die: 광맥 메아리를 받은 적이 쓰러지면 껍질이 터진다
	public static void onMobDie( Mob m ){
		VeinShell vs = m.buff(VeinShell.class);
		if (vs == null || Dungeon.level == null) return;
		vs.detach(); //(점검) 한 번만 터진다 (이웃 껍질이 서로 터뜨려도 되풀이되지 않게)
		BURSTING.add(m.id());
		if (Dungeon.level.heroFOV[m.pos]){
			com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.atCell(com.shatteredpixel.shatteredpixeldungeon.effects.MazeFx.Fx.MINE_BURST, m.pos);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER, 1f, 0.9f);
			PixelScene.shake(1, 0.15f);
			GLog.w(Messages.get(EchoKnot.class, "shell_burst"));
		}
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(m.pos + n);
			if (ch == null || !ch.isAlive() || BURSTING.contains(ch.id())) continue; //(점검) 쓰러지는 중인 적은 다시 치지 않음
			ch.damage(Random.NormalIntRange(MazeTower.N7_BURST_MIN, MazeTower.N7_BURST_MAX), new ShellBurst());
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(ShellBurst.class);
				GLog.n(Messages.get(EchoKnot.class, "ondeath_burst"));
			}
		}
		BURSTING.remove(m.id());
	}

	//(점검) 껍질이 터지는 중인 적 (서로 터뜨리는 사슬에서 같은 적이 두 번 쓰러지지 않게)
	private static final java.util.HashSet<Integer> BURSTING = new java.util.HashSet<>();

	// ===== 그림 (사용자 확인) sprites/maze_knot.png 16×16, 줄 = 메아리(0 수액 / 1 강화 마탄 / 2 광맥), 칸 0~3 속빛이 숨 쉼, 4~6 끊어짐 =====
	//원본 docs/sprites/mobs/n_draft/make_n7_knot.ps1, 움짤 anim_n7.html

	public static class KnotSprite extends com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite {
		private com.watabou.noosa.TextureFilm film;
		public KnotSprite(){
			super();
			texture("sprites/maze_knot.png");
			film = new com.watabou.noosa.TextureFilm(texture, 16, 16);
			setRow(0);
			play(idle);
		}
		private void setRow( int r ){
			int o = r * 7;
			idle = new Animation(3, true);
			idle.frames(film, o, o + 1, o + 2, o + 3);
			run = idle.clone(); attack = idle.clone(); zap = idle.clone();
			die = new Animation(6, false);
			die.frames(film, o + 4, o + 5); //(사용자) 새잎 없이 내려앉아 사라짐 (그림의 6칸째 새잎은 쓰지 않음)
		}
		private ResonanceCrystal.ThreadLine line;
		private int lineTo = -1;

		@Override public void turnTo( int from, int to ){ flipHorizontal = false; }

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof EchoKnot){
				int k = ((EchoKnot) ch).kind;
				setRow(k); play(idle);
				syncLink();
			}
		}

		void syncLink(){
			if (!(ch instanceof EchoKnot)) return;
			Mob m = ((EchoKnot) ch).linkedMob();
			int want = m == null || !ch.isAlive() ? -1 : m.id();
			if (want == lineTo && (line == null || line.parent != null)) return;
			if (line != null){ line.killAndErase(); line = null; }
			if (m != null && m.sprite == null) return; //(점검) 이어진 적의 그림이 아직 없으면 다음 프레임에 다시 (실이 영영 안 그려지던 것)
			lineTo = want;
			if (m == null || m.sprite == null) return;
			int k = ((EchoKnot) ch).kind;
			int col = k == SAP ? 0xFFE0952A : k == BOLT ? 0xFFC8A0E8 : 0xFF78C8FF;
			line = new ResonanceCrystal.ThreadLine(this, m.sprite, col & 0x88FFFFFF, col, 4f);
			GameScene.groupEffect(line);
		}

		@Override
		public void update() {
			super.update();
			if (ch instanceof EchoKnot) syncLink();
		}

		@Override
		public void kill() {
			if (line != null) line.killAndErase();
			line = null;
			super.kill();
		}
	}
}
