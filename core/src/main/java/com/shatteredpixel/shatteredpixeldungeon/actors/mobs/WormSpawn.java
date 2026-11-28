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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Mana;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.TalentEffects;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (J8): 새끼 굴벌레 (13·14층). 15층 보스의 새끼.
//(1) 돌진: 주인공이 같은 직선(8방향) J8_CHARGE_RANGE칸 안에 있으면 그 줄(벽 앞까지)을 붉게 예고하고, 다음 턴에 그 줄을 끝까지 달린다.
//    줄 위의 주인공은 피해, 다른 적은 그냥 지나친다, 공명 수정은 부서진다. 벽에 박으면 한 박자(J8_WALL_STUN) 멈춘다.
//(2) 갉기: 주인공이 보는 살아 있는 광맥이 가까우면 가서 J8_GNAW_TURNS턴 갉는다(그동안 무방비). 다 먹으면 회복하고 껍질(방어막)이 생긴다.
//    갉는 동안 쓰러뜨리면 먹던 마석이 마나 구슬로 돌아온다. 주인공이 광맥을 볼 수 없게 되면 갉기를 멈춘다.
public class WormSpawn extends Mob {

	{
		spriteClass = WormSprite.class;

		HP = HT = MazeTower.J8_WORM_HP;
		defenseSkill = 12;

		EXP = 9;
		maxLvl = 17;

		immunities.add(ManaVapor.class);
		immunities.add(ManaVapor.VaporBite.class);
	}

	private int chargeCd = 3;
	private ArrayList<Integer> chargePath = null; //예고한 돌진 길 (처음 칸 = 굴벌레 바로 앞)
	private int gnawVein = -1;                    //갉는 광맥의 Actor id
	private int gnawTurns = 0;

	private ChargeWarn warnFx;

	@Override public int damageRoll() { return Random.NormalIntRange(8, 16); }
	@Override public int attackSkill( Char target ) { return 20; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 4); }

	public boolean gnawing(){ return gnawVein != -1; }

	@Override
	public int defenseSkill( Char enemy ) {
		return gnawing() ? 0 : super.defenseSkill(enemy); //갉는 동안은 무방비
	}

	@Override
	protected boolean act() {
		//(고침) 원작 Mob.act보다 먼저 길을 찾으므로 시야를 여기서 먼저 갱신한다 (처음 행동할 때 시야가 비어 있어 게임이 튕겼다)
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) fieldOfView = new boolean[Dungeon.level.length()];
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		if (paralysed > 0 || buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost.class) != null){
			cancelCharge();
			return super.act();
		}

		//예고한 돌진을 마저 한다
		if (chargePath != null){
			return doCharge();
		}

		//갉는 중
		if (gnawing()){
			Char v = (Char) Actor.findById(gnawVein);
			if (!(v instanceof LivingVein) || !v.isAlive() || !Dungeon.level.adjacent(pos, v.pos) || !Dungeon.level.heroFOV[v.pos]){
				stopGnaw();
			} else {
				gnawTurns++;
				//(사용자) 광맥이 한 턴마다 한 단계씩 뜯겨 나간다 + 돌 부스러기·푸른 불꽃
				((LivingVein) v).gnawStage = Math.min(3, gnawTurns);
				if (Dungeon.level.heroFOV[v.pos]){
					com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(v.pos).burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.ROCK), 3);
					com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.center(v.pos).burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.BLUE_LIGHT), 1);
					Sample.INSTANCE.play(Assets.Sounds.MINE, 0.5f, 1.3f);
				}
				if (sprite instanceof WormSprite) ((WormSprite) sprite).gnaw(v.pos);
				if (gnawTurns >= MazeTower.J8_GNAW_TURNS) finishGnaw((LivingVein) v);
				spend(TICK);
				return true;
			}
		}

		if (chargeCd > 0) chargeCd--;

		//돌진 예고
		ArrayList<Integer> path = chargeLine();
		if (path != null){
			chargePath = path;
			warnFx = ChargeWarn.show(path);
			if (sprite != null){
				sprite.turnTo(pos, path.get(0));
				if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(WormSpawn.class, "charge"));
			}
			spend(TICK);
			return true;
		}

		//광맥 갉으러 가기
		LivingVein v = veinTarget();
		if (v != null){
			if (Dungeon.level.adjacent(pos, v.pos)){
				gnawVein = v.id();
				gnawTurns = 0;
				if (sprite instanceof WormSprite) ((WormSprite) sprite).gnaw(v.pos);
				spend(TICK);
				return true;
			}
			int oldPos = pos;
			if (getCloser(v.pos)){
				spend(1 / speed());
				return moveSprite(oldPos, pos);
			}
		}

		return super.act();
	}

	//주인공이 같은 직선(8방향) 위, 2~J8_CHARGE_RANGE칸 안, 사이에 벽이 없으면 그 줄(벽 앞까지)을 돌려준다
	private ArrayList<Integer> chargeLine(){
		if (chargeCd > 0 || state != HUNTING) return null;
		Hero h = Dungeon.hero;
		if (h == null || !h.isAlive() || h.invisible > 0 || fieldOfView == null || !fieldOfView[h.pos]) return null;
		Level l = Dungeon.level;
		int w = l.width();
		int dx = h.pos % w - pos % w, dy = h.pos / w - pos / w;
		int d = Math.max(Math.abs(dx), Math.abs(dy));
		if (d < 2 || d > MazeTower.J8_CHARGE_RANGE) return null;
		if (!(dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy))) return null;
		int step = Integer.signum(dx) + Integer.signum(dy) * w;
		ArrayList<Integer> path = new ArrayList<>();
		boolean sawHero = false;
		int c = pos;
		for (int i = 0; i < 40; i++){
			int n = c + step;
			if (n < 0 || n >= l.length() || l.solid[n] || !l.passable[n]) break;
			//대각선은 모서리를 끼고 지나가지 않는다
			if (Math.abs(dx) == Math.abs(dy) && (l.solid[c + Integer.signum(dx)] || l.solid[c + Integer.signum(dy) * w])) break;
			path.add(n);
			if (n == h.pos) sawHero = true;
			c = n;
		}
		return sawHero ? path : null;
	}

	private boolean doCharge(){
		ArrayList<Integer> path = chargePath;
		cancelCharge();
		chargeCd = MazeTower.J8_CHARGE_CD;
		Hero h = Dungeon.hero;
		//줄 위의 주인공은 피해, 공명 수정은 부서진다, 다른 적은 지나친다
		for (int c : path){
			Char ch = Actor.findChar(c);
			if (ch == h && h.isAlive()){
				h.damage(Random.NormalIntRange(MazeTower.J8_CHARGE_DMG_MIN, MazeTower.J8_CHARGE_DMG_MAX), this);
				if (h.sprite != null) h.sprite.flash();
				if (!h.isAlive()){
					Dungeon.fail(this);
					GLog.n(Messages.get(WormSpawn.class, "charge_kill"));
				}
			} else if (ch instanceof ResonanceCrystal && ch.isAlive()){
				ch.die(this);
			}
		}
		//멈추는 칸: 줄 끝에서부터 비어 있는 첫 칸
		int to = pos;
		for (int i = path.size() - 1; i >= 0; i--){
			int c = path.get(i);
			if (Actor.findChar(c) == null){ to = c; break; }
		}
		final int from = pos;
		final int dest = to;
		final boolean hitWall = to == path.get(path.size() - 1);
		if (dest != from){
			pos = dest;
			Dungeon.level.occupyCell(this);
			if (sprite != null && (sprite.visible || Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[dest])){
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 1.2f);
				//(고침) 돌진 그림(Pushing)은 이 턴이 끝난 뒤 따로 움직인다. 예전에는 그림이 끝나기를 기다리다 서로 막혀 게임이 멈췄다
				Actor.add(new Pushing(this, from, dest));
			} else if (sprite != null){
				sprite.place(dest);
			}
		}
		afterCharge(hitWall);
		spend(TICK);
		return true;
	}

	private void afterCharge( boolean hitWall ){
		if (hitWall && isAlive()){
			Buff.prolong(this, Paralysis.class, MazeTower.J8_WALL_STUN);
			if (Dungeon.level.heroFOV[pos]){
				PixelScene.shake(1, 0.1f);
				if (sprite != null) sprite.showStatus(CharSprite.NEUTRAL, Messages.get(WormSpawn.class, "stuck"));
			}
		}
	}

	private void cancelCharge(){
		chargePath = null;
		if (warnFx != null){ warnFx.fadeOut(); warnFx = null; }
	}

	//주인공이 보는, J8_GNAW_RANGE칸 안의 가장 가까운 살아 있는 광맥
	private LivingVein veinTarget(){
		if (Dungeon.level == null) return null;
		LivingVein best = null; int bd = 99;
		for (Mob m : Dungeon.level.mobs){
			if (!(m instanceof LivingVein) || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			int d = Dungeon.level.distance(pos, m.pos);
			if (d <= MazeTower.J8_GNAW_RANGE && d < bd){ bd = d; best = (LivingVein) m; }
		}
		return best;
	}

	private void stopGnaw(){
		gnawVein = -1;
		gnawTurns = 0;
		if (sprite instanceof WormSprite) ((WormSprite) sprite).stopGnaw();
	}

	private void finishGnaw( LivingVein v ){
		v.mazeEaten = true;
		v.die(this);
		stopGnaw();
		int heal = Math.round(HT * MazeTower.J8_GNAW_HEAL);
		HP = Math.min(HT, HP + heal);
		Shell s = Buff.affect(this, Shell.class);
		s.setShield(Math.max(s.shielding(), MazeTower.J8_GNAW_SHELL));
		if (sprite != null && sprite.visible){
			sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
		}
	}

	@Override
	public void die( Object cause ) {
		//갉는 동안 쓰러지면 먹던 마석이 마나 구슬로 돌아온다
		if (gnawing() && Dungeon.hero != null && Dungeon.hero.isAlive()){
			Mana m = Mana.get(Dungeon.hero);
			if (m != null){
				m.gain(MazeTower.J8_GNAW_MANA);
				if (sprite != null && sprite.visible) TalentEffects.manaRefund(Dungeon.hero, this, MazeTower.J8_GNAW_MANA);
			}
		}
		cancelCharge();
		super.die(cause);
	}

	@Override
	public void destroy() {
		cancelCharge();
		super.destroy();
	}

	@Override
	public String description() {
		String d = super.description();
		if (gnawing()) d += "\n\n" + Messages.get(WormSpawn.class, "desc_gnawing");
		return d;
	}

	//다 먹으면 생기는 껍질 (저절로 줄지 않는 방어막)
	public static class Shell extends ShieldBuff {
		@Override public String desc() { return Messages.get(this, "desc", shielding()); }
		{ type = buffType.POSITIVE; }
	}

	private static final String CHARGE_CD = "charge_cd";
	private static final String CHARGE_PATH = "charge_path";
	private static final String GNAW_VEIN = "gnaw_vein";
	private static final String GNAW_TURNS = "gnaw_turns";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE_CD, chargeCd);
		if (chargePath != null){
			int[] a = new int[chargePath.size()];
			for (int i = 0; i < a.length; i++) a[i] = chargePath.get(i);
			bundle.put(CHARGE_PATH, a);
		}
		bundle.put(GNAW_VEIN, gnawVein);
		bundle.put(GNAW_TURNS, gnawTurns);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		chargeCd = bundle.getInt(CHARGE_CD);
		if (bundle.contains(CHARGE_PATH)){
			chargePath = new ArrayList<>();
			for (int c : bundle.getIntArray(CHARGE_PATH)) chargePath.add(c);
		}
		gnawVein = bundle.contains(GNAW_VEIN) ? bundle.getInt(GNAW_VEIN) : -1;
		gnawTurns = bundle.getInt(GNAW_TURNS);
	}

	//불러온 뒤 남은 예고를 다시 그린다
	public void syncFx(){
		if (chargePath != null && warnFx == null){
			warnFx = ChargeWarn.show(chargePath);
		}
	}

	// ===== 놓기 =====

	//13·14층 일반 층마다 J8_WORMS_MIN~MAX마리 (트인 바닥, 입구·출구에서 떨어진 곳, 따로 된 난수)
	public static void place( Level level ){
		if (!MazeTower.J8_ENABLED || Dungeon.depth < 13 || Dungeon.depth > 14) return;
		Random.pushGenerator( Dungeon.seedCurDepth() ^ 0x5EED3A11L );
		try {
			int want = Random.IntRange(MazeTower.J8_WORMS_MIN, MazeTower.J8_WORMS_MAX);
			placeSome(level, want, true);
			placeSome(level, MazeTower.J8_CRYSTALS, false);
		} finally {
			Random.popGenerator();
		}
	}

	private static void placeSome( Level level, int want, boolean worm ){
		int placed = 0;
		for (int tries = 0; tries < 1000 && placed < want; tries++){
			int c = Random.Int(level.length());
			if (!LivingVein.openFloor(level, c)) continue;
			boolean bad = false;
			for (int t : new int[]{ level.entrance(), level.exit() }) if (t >= 0 && level.distance(t, c) < 6) bad = true;
			for (Mob m : level.mobs) if (m.pos == c || ((m instanceof WormSpawn || m instanceof ResonanceCrystal) && level.distance(m.pos, c) < 4)) bad = true;
			if (bad) continue;
			Mob m = worm ? new WormSpawn() : new ResonanceCrystal();
			m.pos = c;
			level.mobs.add(m);
			placed++;
		}
	}

	// ===== 그림 =====

	//sprites/maze_worm.png (16x16): 0·1 가만히, 2~5 기어감, 6·7 물기, 8~10 갉기(되풀이), 11~13 쓰러짐
	public static class WormSprite extends MobSprite {

		private Animation gnawAnim;

		public WormSprite(){
			super();
			texture("sprites/maze_worm.png");
			TextureFilm f = new TextureFilm(texture, 16, 16);
			idle = new Animation(3, true);
			idle.frames(f, 0, 0, 1, 0);
			run = new Animation(12, true);
			run.frames(f, 2, 3, 4, 5);
			attack = new Animation(14, false);
			attack.frames(f, 6, 7, 0);
			zap = attack.clone();
			gnawAnim = new Animation(8, true);
			gnawAnim.frames(f, 8, 9, 10); //(사용자) 들어 올림 → 깊게 물어뜯음(부스러기) → 비틀며 당김
			die = new Animation(10, false);
			die.frames(f, 11, 12, 13);
			play(idle);
		}

		public void gnaw( int veinCell ){
			if (ch != null) turnTo(ch.pos, veinCell);
			if (curAnim != gnawAnim) play(gnawAnim);
		}

		public void stopGnaw(){
			if (curAnim == gnawAnim) idle();
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof WormSpawn){
				((WormSpawn) ch).syncFx();
				if (((WormSpawn) ch).gnawing()) play(gnawAnim);
			}
		}

		@Override
		public int blood() {
			return 0xFF5AB4F0; //마디 사이의 푸른빛
		}
	}

	//(사용자) 돌진 경고: 은은하게. 칸마다 옅은 붉은 빛 + 줄의 바깥 테두리만 또렷하게, 이어진 칸 사이의 테두리는 아주 옅게. 천천히 숨 쉬듯
	public static class ChargeWarn extends com.watabou.noosa.Group {
		private static final int COLOR = 0xFF6A48;
		private final ArrayList<com.watabou.noosa.ColorBlock> fills = new ArrayList<>(), outer = new ArrayList<>(), inner = new ArrayList<>();
		private float time = 0, fade = 1f;
		private boolean fading = false;

		public static ChargeWarn show( ArrayList<Integer> path ){
			ChargeWarn w = new ChargeWarn(path);
			GameScene.groupEffect(w);
			return w;
		}

		public ChargeWarn( ArrayList<Integer> path ){
			int w = Dungeon.level.width();
			java.util.HashSet<Integer> set = new java.util.HashSet<>(path);
			for (int c : path){
				com.watabou.utils.PointF p = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.tileToWorld(c);
				com.watabou.noosa.ColorBlock f = new com.watabou.noosa.ColorBlock(16, 16, 0xFF000000 | COLOR);
				f.x = p.x; f.y = p.y; add(f); fills.add(f);
				//변: 위·아래·왼쪽·오른쪽. 그 방향의 이웃 칸도 줄에 있으면 안쪽 테두리(아주 옅게)
				int[][] sides = { {0,0,16,1, -w}, {0,15,16,1, w}, {0,0,1,16, -1}, {15,0,1,16, 1} };
				for (int[] s : sides){
					com.watabou.noosa.ColorBlock b = new com.watabou.noosa.ColorBlock(s[2], s[3], 0xFF000000 | COLOR);
					b.x = p.x + s[0]; b.y = p.y + s[1]; add(b);
					if (set.contains(c + s[4])) inner.add(b); else outer.add(b);
				}
			}
			update();
		}

		public void fadeOut(){ fading = true; }

		@Override
		public void update() {
			super.update();
			time += com.watabou.noosa.Game.elapsed;
			if (fading){ fade -= com.watabou.noosa.Game.elapsed / 0.25f; if (fade <= 0){ killAndErase(); return; } }
			float k = fade * (0.8f + 0.2f * (float)Math.sin(time * 3f));
			for (com.watabou.noosa.ColorBlock b : fills) b.alpha(0.12f * k);
			for (com.watabou.noosa.ColorBlock b : outer) b.alpha(0.45f * k);
			for (com.watabou.noosa.ColorBlock b : inner) b.alpha(0.08f * k);
		}
	}
}
