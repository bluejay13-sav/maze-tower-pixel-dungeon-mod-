/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

//Maze Tower: 25층 심장을 진 짐승 그림 (docs/sprites/bosses/boss25/heartbeast/README_heartbeast.md).
//모든 칸 32×27. 가로 = 동작 프레임, 세로 = 심장 박동 상태(0 쉼, 1 두, 2 사이, 3 근). 대기(idle)만 가로 = 박동, 세로 = 옆/정면/뒷모습.
//박동은 실제 시간 기준(기본 48 bpm, 3페이즈 70 bpm). 한 세트를 16칸으로 나눠 두(1) → 사이(1) → 근(1) → 쉼(13).
//방향: 좌우·대각선 = 옆모습, 아래 = 정면, 위 = 뒷모습 (왼쪽은 좌우 반전).
//글리치: 실루엣 좌우 붉은/보라 잔상 + 가끔 가로 띠가 밀림 (평소 은은하게, 3페이즈 강하게, 심장이 뛰는 순간 더 자주).
public class HeartBeastSprite extends MobSprite {

	public static final int W = 32, H = 27;
	private static final String P = "sprites/maze_b7_heartbeast_";

	public enum Mode { IDLE, RUN, EAT, CHARGE, ROAR, DEATH }
	public enum Facing { SIDE, FRONT, BACK }

	private Mode mode = Mode.IDLE;
	private Facing facing = Facing.SIDE;
	private float modeTime = 0;
	private boolean modeLoop = true;
	private Callback modeDone;

	public float bpm = MazeTower.B25_BPM;
	private float beatClock = 0;
	public boolean strongGlitch = false;
	private float reveal = -1; //다시 만들어지는 중 (0 → 1, 위에서 아래로)

	private Image band;
	private String curTex = "";
	private final java.util.HashMap<String, TextureFilm> films = new java.util.HashMap<>();
	private float bandLeft = 0;

	public HeartBeastSprite(){
		super();
		texture(P + "idle.png");
		TextureFilm film = new TextureFilm(texture, W, H);
		idle = new MovieClip.Animation(1, true);
		idle.frames(film, 0);
		run = idle.clone();
		attack = idle.clone();
		zap = idle.clone();
		die = idle.clone();
		play(idle);
	}

	// ---------- 동작 ----------
	public void setMode( Mode m, boolean loop, Callback done ){
		mode = m;
		modeTime = 0;
		modeLoop = loop;
		modeDone = done;
	}

	public Mode mode(){ return mode; }

	@Override
	public void idle() {
		super.idle();
		if (mode != Mode.DEATH) setMode(Mode.IDLE, true, null);
	}

	@Override
	public void move( int from, int to ) {
		face(from, to);
		super.move(from, to);
		setMode(Mode.RUN, true, null);
	}

	@Override
	public void onComplete( Tweener tweener ) {
		super.onComplete(tweener);
		if (mode == Mode.RUN) setMode(Mode.IDLE, true, null);
	}

	@Override
	public void attack( int cell ) {
		face(ch.pos, cell);
		setMode(Mode.CHARGE, false, null);
		super.attack(cell);
	}

	@Override
	public void die() {
		//사망 연출은 HeartBeast가 deathSequence로 직접 부른다
	}

	//방향 고르기: 진행 방향 → 옆/정면/뒷모습, 왼쪽은 좌우 반전
	public void face( int from, int to ){
		PointF a = DungeonTilemap.tileCenterToWorld(from), b = DungeonTilemap.tileCenterToWorld(to);
		faceAngle((float)Math.toDegrees(Math.atan2(b.y - a.y, b.x - a.x)));
	}

	public void faceAngle( float deg ){
		float d = (deg % 360 + 360) % 360;
		if (d >= 55 && d <= 125) facing = Facing.FRONT;
		else if (d >= 235 && d <= 305) facing = Facing.BACK;
		else facing = Facing.SIDE;
		if (d > 90 && d < 270) flipHorizontal = true;
		else if (d < 90 || d > 270) flipHorizontal = false;
	}

	public void faceSide(){ facing = Facing.SIDE; }

	public void eat( int cell ){
		face(ch.pos, cell);
		facing = Facing.SIDE;
		setMode(Mode.EAT, true, null);
	}

	public void roar( Callback done ){
		facing = Facing.SIDE;
		setMode(Mode.ROAR, false, done);
	}

	//돌진 경고: 앞발 두 번 구르기 (charge 시트 앞부분 반복)
	public void stomp(){
		facing = Facing.SIDE;
		setMode(Mode.CHARGE, true, null);
	}

	public void run( float deg ){
		faceAngle(deg);
		setMode(Mode.RUN, true, null);
	}

	public void deathSequence( Callback done ){
		facing = Facing.SIDE;
		setMode(Mode.DEATH, false, done);
	}

	//의태 처치 뒤 위에서 아래로 다시 만들어진다
	//(2026-11-22) delay초 기다린 뒤(그동안 안 보임) 붉은 살점 줄무늬가 위에서 아래로 차오르며 만들어지고, 끝나면 발밑 고리
	public void rebuild( float delay ){
		reveal = 0;
		revealDelay = delay;
	}

	private float revealDelay = 0;
	private float dissolve = -1; //의태로 바뀌는 중 (0 → 1, 위에서부터 사라짐)
	private float baseY;

	public void dissolve(){
		dissolve = 0;
		baseY = y;
	}

	// ---------- 매 프레임 ----------
	private int heartRow(){
		float period = 60f / Math.max(1f, bpm);
		if (bpm <= 0) return 0;
		int slot = (int)((beatClock % period) / period * 16);
		switch (slot){
			case 0: return 1;
			case 1: return 2;
			case 2: return 3;
			default: return 0;
		}
	}

	public boolean beating(){ return heartRow() != 0; }

	@Override
	public void update() {
		super.update();
		beatClock += Game.elapsed;
		modeTime += Game.elapsed;

		String tex; int cols, col, row = heartRow(); float delay; int n;
		switch (mode){
			default:
			case IDLE:
				tex = "idle"; cols = 4; col = row; row = facing.ordinal(); n = 1; delay = 1; break;
			case RUN:
				tex = facing == Facing.FRONT ? "run_front" : facing == Facing.BACK ? "run_back" : "run_side";
				cols = 16; n = 16; delay = 0.045f; col = 0; break;
			case EAT:
				tex = "eat"; cols = 6; n = 6; delay = 0.14f; col = 0; break;
			case CHARGE:
				tex = "charge"; cols = 12; n = modeLoop ? 6 : 12; delay = 0.08f; col = 0; break;
			case ROAR:
				tex = "roar"; cols = 10; n = 10; delay = 0.11f; col = 0; break;
			case DEATH:
				tex = "death"; cols = 22; n = 22; delay = 0.16f; col = 0; row = 0; break;
		}
		if (mode != Mode.IDLE){
			int f = (int)(modeTime / delay);
			if (f >= n){
				if (modeLoop) f %= n;
				else {
					f = n - 1;
					if (modeDone != null){
						Callback c = modeDone;
						modeDone = null;
						c.call();
					}
				}
			}
			col = f;
		}
		if (!tex.equals(curTex)){ curTex = tex; texture(P + tex + ".png"); }
		TextureFilm film = films.get(tex);
		if (film == null){ film = new TextureFilm(texture, W, H); films.put(tex, film); }
		RectF r = film.get(row * cols + col);
		if (r != null){
			if (reveal >= 0){
				if (revealDelay > 0){
					revealDelay -= Game.elapsed;
					alpha(0f);
				} else {
					alpha(1f);
					reveal = Math.min(1f, reveal + Game.elapsed / 0.9f);
					r = new RectF(r.left, r.top, r.right, r.top + (r.bottom - r.top) * Math.max(0.04f, reveal));
					//줄무늬: 한 줄 걸러 붉게, 다 차면 원래 색
					if (reveal < 1f && ((int)(reveal * 20)) % 2 == 0) hardlight(1f, 0.25f, 0.3f); else resetColor();
					if (reveal >= 1){
						reveal = -1;
						resetColor();
						com.shatteredpixel.shatteredpixeldungeon.effects.Heart25Fx.footRing(this);
					}
				}
			}
			if (dissolve >= 0){
				dissolve = Math.min(1f, dissolve + Game.elapsed / 1.1f);
				float h = r.bottom - r.top;
				r = new RectF(r.left, r.top + h * Math.min(0.96f, dissolve), r.right, r.bottom);
				y = baseY + H * Math.min(0.96f, dissolve);
				if (dissolve >= 1){ visible = false; }
			}
			frame(r);
		}

		updateGlitch();

		//3페이즈: 박동마다 등의 심장이 하얗게 번쩍 + 작은 고리
		boolean b = beating() && heartRow() == 1;
		if (strongGlitch && b && !lastBeat && parent != null && visible){
			float hx = flipHorizontal ? x + W - 13 : x + 13, hy = y + 9;
			com.watabou.noosa.ColorBlock dot = new com.watabou.noosa.ColorBlock(2, 2, 0xFFFFFFFF);
			dot.x = hx - 1; dot.y = hy - 1;
			parent.add(dot);
			final com.watabou.noosa.ColorBlock d = dot;
			parent.add(new com.watabou.noosa.Gizmo(){
				float t = 0.12f;
				@Override public void update(){ t -= Game.elapsed; if (t <= 0){ d.killAndErase(); killAndErase(); } }
			});
			Emitter e = new Emitter();
			e.pos(hx - 2, hy - 2, 4, 4);
			parent.add(e);
			e.burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.LIGHT), 2);
		}
		lastBeat = b;
	}

	private boolean lastBeat = false;

	// ---------- 글리치 ----------
	private void updateGlitch(){
		if (parent == null) return;
		if (band == null){
			band = new Image(texture);
			band.visible = false;
			parent.add(band);
		}
		if (bandLeft > 0){
			bandLeft -= Game.elapsed;
			if (bandLeft <= 0) band.visible = false;
		}
		float chance = strongGlitch ? (beating() ? 0.45f : 0.18f) : (beating() ? 0.06f : 0.02f);
		if (visible && mode != Mode.DEATH && bandLeft <= 0 && Random.Float() < chance * Game.elapsed * 30f){
			int bh = strongGlitch ? Random.IntRange(2, 3) : 1;
			int by = Random.Int(H - bh);
			float tw = texture.width, th = texture.height;
			RectF f = frame;
			float px = f.left * tw, py = f.top * th;
			band.texture(texture);
			band.frame(new RectF(f.left, (py + by) / th, f.right, (py + by + bh) / th));
			band.flipHorizontal = flipHorizontal;
			band.x = x + (Random.Int(2) == 0 ? -1 : 1) * (strongGlitch ? Random.IntRange(2, 3) : 1);
			band.y = y + by;
			band.resetColor();
			if (strongGlitch && Random.Int(4) == 0) band.lightness(1f);
			band.visible = true;
			bandLeft = strongGlitch ? 0.09f : 0.06f;
			if (band.parent == parent) parent.bringToFront(band);
		}
	}

	@Override
	public void draw() {
		if (visible && mode != Mode.DEATH){
			float ca = strongGlitch ? (beating() ? 0.55f : 0.35f) : (beating() ? 0.27f : 0.11f);
			int off = strongGlitch ? (beating() ? 2 : 1) : 1;
			float ox = x;
			float ram = rm, gam = gm, bam = bm, aam = am;
			rm = 1f; gm = 0.15f; bm = 0.3f; am = aam * ca;
			x = ox - off; super.draw();
			rm = 0.55f; gm = 0.25f; bm = 1f;
			x = ox + off; super.draw();
			rm = ram; gm = gam; bm = bam; am = aam;
			x = ox;
		}
		super.draw();
	}

	@Override
	public void kill() {
		super.kill();
		if (band != null) band.killAndErase();
	}

	// ---------- 효과 ----------
	//(2026-11-22) 돌진 잔상: 지금 그림을 붉게 물들인 흐린 사본이 남아 빠르게 사라지고, 발치에 붉은 먼지
	public void afterimage(){
		if (parent == null || !visible || frame == null) return;
		final Image a = new Image(texture);
		a.frame(new RectF(frame.left, frame.top, frame.right, frame.bottom));
		a.flipHorizontal = flipHorizontal;
		a.x = x; a.y = y;
		a.hardlight(1f, 0.15f, 0.25f);
		a.alpha(0.42f);
		parent.addToBack(a);
		parent.add(new com.watabou.noosa.Gizmo(){
			float t = 0.22f;
			@Override public void update(){
				t -= Game.elapsed;
				a.alpha(Math.max(0, t / 0.22f * 0.42f));
				if (t <= 0){ a.killAndErase(); killAndErase(); }
			}
		});
		if (Random.Int(2) == 0){
			Emitter e = new Emitter();
			e.pos(x + 8, y + H - 4, 16, 3);
			parent.add(e);
			e.burst(BloodParticle.FACTORY, 2);
		}
	}

	public void petalBurst(){
		if (parent == null) return;
		Emitter e = new Emitter();
		e.pos(x + 8, y + 2, 16, 12);
		parent.add(e);
		e.burst(BossPetalParticle.FACTORIES[BossPetalParticle.VIVID], 30);
	}

	public void bleed( int n ){
		if (parent == null) return;
		Emitter e = new Emitter();
		e.pos(x + 6, y + 6, 20, 16);
		parent.add(e);
		e.burst(BloodParticle.BURST, n);
	}

	//texture 캐시 미리 올리기 (첫 프레임 끊김 방지)
	public static void preload(){
		for (String s : new String[]{"idle","run_side","run_front","run_back","eat","charge","roar","death","turn"}){
			TextureCache.get(P + s + ".png");
		}
	}
}
