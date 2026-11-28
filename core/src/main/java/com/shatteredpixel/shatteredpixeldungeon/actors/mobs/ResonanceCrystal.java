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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.HashMap;

//Maze Tower (J8): 공명 수정 (13·14층 고정 몹). 벌레가 갉다 남긴 광맥 줄기에서 자란 키 큰 수정.
//움직이지도 공격하지도 않는다. J8_LINK_RANGE칸 안, 시선이 닿는 적 최대 J8_LINK_MAX마리와 푸른 실빛으로 이어져 방어막(J8_LINK_SHIELD)을 준다(깨지면 턴마다 다시 채움).
//부서지면 이어진 방어막이 한꺼번에 터져 그 적들이 J8_BREAK_STUN턴 기절한다. 새끼 굴벌레의 돌진 길에 있어도 부서진다. 반드시 맞는다.
public class ResonanceCrystal extends Mob {

	{
		spriteClass = CrystalSprite.class;

		HP = HT = MazeTower.J8_CRYSTAL_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
		alignment = Alignment.NEUTRAL;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	private final ArrayList<Integer> linked = new ArrayList<>(); //이어진 적의 Actor id

	@Override public int defenseSkill( Char enemy ) { return 0; } //반드시 맞는다
	@Override public boolean heroShouldInteract() { return false; } //누르면 원작처럼 평타로 친다
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

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

	//이어질 적을 다시 고르고 방어막을 채운다
	private void relink(){
		ArrayList<Mob> want = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m == this || !m.isAlive() || m.alignment != Alignment.ENEMY) continue;
			if (m instanceof ResonanceCrystal || m instanceof ManaVein) continue;
			if (Dungeon.level.distance(pos, m.pos) > MazeTower.J8_LINK_RANGE) continue;
			if (new Ballistica(pos, m.pos, Ballistica.PROJECTILE).collisionPos != m.pos) continue;
			want.add(m);
		}
		want.sort((a, b) -> Dungeon.level.distance(pos, a.pos) - Dungeon.level.distance(pos, b.pos));
		while (want.size() > MazeTower.J8_LINK_MAX) want.remove(want.size() - 1);

		//끊어진 적은 방어막을 거둔다
		for (int id : new ArrayList<>(linked)){
			Char ch = (Char) Actor.findById(id);
			if (!(ch instanceof Mob) || !want.contains(ch)){
				if (ch != null){ ResonanceShield s = ch.buff(ResonanceShield.class); if (s != null) s.detach(); }
				linked.remove((Integer) id);
			}
		}
		for (Mob m : want){
			if (!linked.contains(m.id())) linked.add(m.id());
			ResonanceShield s = m.buff(ResonanceShield.class);
			if (s == null){ s = new ResonanceShield(); s.attachTo(m); }
			if (s.shielding() < MazeTower.J8_LINK_SHIELD) s.setShield(MazeTower.J8_LINK_SHIELD);
		}
		if (sprite instanceof CrystalSprite) ((CrystalSprite) sprite).syncLinks(linked);
	}

	@Override
	public void die( Object cause ) {
		//이어진 방어막이 한꺼번에 터져 그 적들이 기절한다
		boolean seen = Dungeon.level.heroFOV[pos];
		for (int id : linked){
			Char ch = (Char) Actor.findById(id);
			if (ch == null || !ch.isAlive()) continue;
			ResonanceShield s = ch.buff(ResonanceShield.class);
			if (s != null) s.detach();
			Buff.prolong(ch, Paralysis.class, MazeTower.J8_BREAK_STUN);
			if (Dungeon.level.heroFOV[ch.pos]){
				CellEmitter.center(ch.pos).burst(Speck.factory(Speck.BLUE_LIGHT), 4);
				seen = true;
			}
		}
		linked.clear();
		if (sprite instanceof CrystalSprite) ((CrystalSprite) sprite).syncLinks(linked);
		if (seen){
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
			GLog.w(Messages.get(ResonanceCrystal.class, "shatter"));
		}
		super.die(cause);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", MazeTower.J8_LINK_RANGE, MazeTower.J8_LINK_SHIELD, (int) MazeTower.J8_BREAK_STUN);
	}

	//공명 수정이 준 방어막 (저절로 줄지 않음)
	public static class ResonanceShield extends ShieldBuff {
		@Override public String desc() { return Messages.get(this, "desc", shielding()); }
		{ type = buffType.POSITIVE; }
	}

	private static final String LINKED = "linked";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		int[] a = new int[linked.size()];
		for (int i = 0; i < a.length; i++) a[i] = linked.get(i);
		bundle.put(LINKED, a);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		linked.clear();
		if (bundle.contains(LINKED)) for (int id : bundle.getIntArray(LINKED)) linked.add(id);
	}

	// ===== 그림 =====

	//sprites/maze_rescrystal.png (16x16): 0~5 속빛이 아주 조금씩 오르내림(되풀이, 완전히 밝아지는 칸 없음), 6~8 부서짐. 이어진 적과는 푸른 실빛(ThreadLine)
	public static class CrystalSprite extends MobSprite {

		private final HashMap<Integer, ThreadLine> lines = new HashMap<>();

		//Maze Tower: 고정된 수정이라 원작처럼 무작위 방향으로 뒤집지 않는다
		@Override
		public void turnTo( int from, int to ) { flipHorizontal = false; }

		public CrystalSprite(){
			super();
			texture("sprites/maze_rescrystal.png");
			TextureFilm f = new TextureFilm(texture, 16, 16);
			idle = new Animation(3, true); //(사용자) 뚝뚝 끊기지 않게: 아주 작은 차이의 여섯 칸을 천천히
			idle.frames(f, 0, 1, 2, 3, 4, 5);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(12, false);
			die.frames(f, 6, 7, 8);
			play(idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof ResonanceCrystal) syncLinks(((ResonanceCrystal) ch).linked);
		}

		public void syncLinks( ArrayList<Integer> ids ){
			for (Integer id : new ArrayList<>(lines.keySet())){
				if (!ids.contains(id)){ ThreadLine t = lines.remove(id); if (t != null) t.killAndErase(); }
			}
			for (int id : ids){
				if (lines.containsKey(id)) continue;
				Char other = (Char) Actor.findById(id);
				if (other == null || other.sprite == null) continue;
				ThreadLine t = new ThreadLine(this, other.sprite);
				GameScene.groupEffect(t);
				lines.put(id, t);
			}
		}

		@Override
		public void kill() {
			for (ThreadLine t : lines.values()) t.killAndErase();
			lines.clear();
			super.kill();
		}
	}

	//(사용자) 수정과 적 사이의 실: 굵게 빛나는 선 대신 아주 옅은 실 한 줄 + 그 위를 작은 빛 알갱이가 수정에서 적 쪽으로 천천히 흐름.
	//  수정 윗부분에서 적 몸통 가운데로 매 프레임 다시 잇는다
	public static class ThreadLine extends Group {
		private static final float GAP = 5f, SPEED = 6f;
		private final CharSprite a, b;
		private final ColorBlock line;
		private final ArrayList<ColorBlock> dots = new ArrayList<>();
		private float time = 0;

		private int dotColor = 0xFFAAE2FF; private float startY = 6f; //Maze Tower (L5): 석상의 금빛 선은 색과 시작 높이가 다르다
		public ThreadLine( CharSprite a, CharSprite b ){
			this( a, b, 0xFF78C8FF, 0xFFAAE2FF, 6f );
		}
		public ThreadLine( CharSprite a, CharSprite b, int lineColor, int dotColor, float startY ){
			this.a = a; this.b = b; this.dotColor = dotColor; this.startY = startY;
			line = new ColorBlock(1, 1, lineColor); line.origin.set(0, 0.5f); add(line);
		}

		@Override
		public void update() {
			super.update();
			if (a.parent == null || b.parent == null){ killAndErase(); return; }
			visible = (a.visible || b.visible) && b.visible;
			if (!visible) return;
			time += Game.elapsed;
			PointF p = new PointF(a.x + a.width / 2f, a.y + startY), q = b.center();
			float dx = q.x - p.x, dy = q.y - p.y;
			float len = (float)Math.sqrt(dx*dx + dy*dy);
			line.size(len, 1);
			line.x = p.x; line.y = p.y - 0.5f;
			line.angle = (float)(Math.atan2(dy, dx) * 180 / Math.PI);
			line.alpha(0.16f);
			int n = Math.max(1, (int)(len / GAP));
			while (dots.size() < n){ ColorBlock d = new ColorBlock(1, 1, dotColor); add(d); dots.add(d); }
			for (int i = 0; i < dots.size(); i++){
				ColorBlock d = dots.get(i);
				if (i >= n){ d.visible = false; continue; }
				float t = ((i * GAP + time * SPEED) % len) / len;
				d.visible = true;
				d.x = p.x + dx * t - 0.5f; //(사용자) 뚝뚝 끊기지 않게 칸에 맞추지 않고 부드럽게 흐름
				d.y = p.y + dy * t - 0.5f;
				d.alpha(0.55f * (float)Math.sin(Math.PI * t));
			}
		}
	}
}
