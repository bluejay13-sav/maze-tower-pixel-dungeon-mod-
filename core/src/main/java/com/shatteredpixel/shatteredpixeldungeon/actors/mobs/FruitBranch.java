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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower (N8, 사용자): 열매 맺는 가지 (23~24층 고정 몹, 다치지 않음). 25층 세계수의 열매 다툼을 미리 배운다.
//주인공이 층 어디에 있든 N8_GROW_TURNS(30)턴마다 가지 옆 빈 칸에 세계수의 열매가 맺혀 N8_RIPEN(8)턴 동안 익는다(그림이 커지고, 익으면 빛남).
//익으면 먼저 그 칸에 들어선 쪽이 먹는다. 가까운 깨어 있는 적 하나는 싸움을 멈추고 먹으러 가고(먹는 1턴은 무방비), 먹으면 N8_TURNS턴 동안 주는 피해 +10%·받는 피해 -10%.
//주인공이 먹으면 체력 N8_HEAL 회복 + N8_TURNS턴 동안 주는 피해 +10%·받는 피해 -10%. 열매가 사라지면 다시 N8_GROW_TURNS턴을 센다.
public class FruitBranch extends Mob {

	{
		spriteClass = BranchSprite.class;

		HP = HT = 100;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
		alignment = Alignment.NEUTRAL;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	public int fruitCell = -1;
	public int fruitLeft = 0;   //익기까지 남은 턴 (0이면 익음)
	private int growIn = MazeTower.N8_GROW_TURNS;
	private int eater = -1;     //먹으러 가는 적의 Actor id

	@Override public boolean isInvulnerable( Class effect ) { return true; }
	@Override public boolean heroShouldInteract() { return false; }
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }
	@Override protected boolean getCloser( int target ) { return false; }
	@Override protected boolean getFurther( int target ) { return false; }
	@Override protected boolean canAttack( Char enemy ) { return false; }
	@Override public boolean add( Buff buff ) { return false; }
	@Override public void damage( int dmg, Object src ) { }

	public boolean ripe(){ return fruitCell != -1 && fruitLeft <= 0; }

	@Override
	protected boolean act() {
		if (sprite != null){ sprite.hideAlert(); sprite.hideLost(); }
		if (fruitCell == -1){
			if (--growIn <= 0) sprout();
		} else {
			if (fruitLeft > 0 && --fruitLeft == 0){
				if (Dungeon.level.heroFOV[fruitCell]){
					GLog.w(Messages.get(FruitBranch.class, "ripe"));
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.5f, 1.4f);
				}
			}
			if (ripe()){
				Char on = Actor.findChar(fruitCell);
				if (on != null) eat(on);
				else callEater();
			}
		}
		spend(TICK);
		return true;
	}

	//(사용자) 가지 상하좌우의 새잎 땅 칸 중 빈 칸에만 열매가 맺힌다 (열매는 한 번에 하나)
	private void sprout(){
		ArrayList<Integer> spots = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS4){
			int c = pos + n;
			if (c < 0 || c >= Dungeon.level.length() || !Dungeon.level.passable[c] || Dungeon.level.pit[c] || Actor.findChar(c) != null) continue;
			if (!com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.at(Dungeon.level, c)) continue;
			spots.add(c);
		}
		if (spots.isEmpty()){ growIn = 1; return; }
		fruitCell = Random.element(spots);
		fruitLeft = MazeTower.N8_RIPEN;
		eater = -1;
		if (Dungeon.level.heroFOV[fruitCell]){
			GLog.i(Messages.get(FruitBranch.class, "sprout", MazeTower.N8_RIPEN));
			CellEmitter.get(fruitCell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
		}
		if (sprite instanceof BranchSprite) ((BranchSprite) sprite).showFruit();
	}

	//익은 열매로 가까운 깨어 있는 적 하나가 싸움을 멈추고 간다
	private void callEater(){
		Actor a = eater == -1 ? null : Actor.findById(eater);
		Mob m = a instanceof Mob && ((Mob) a).isAlive() && ((Mob) a).alignment == Alignment.ENEMY && ((Mob) a).mazeSeekGaveUp != fruitCell ? (Mob) a : null;
		if (m == null){
			for (Mob o : Dungeon.level.mobs.toArray(new Mob[0])){
				if (!canEat(o) || o.mazeSeekGaveUp == fruitCell) continue; //(점검) 길이 막혀 포기한 적은 다시 부르지 않는다
				if (Dungeon.level.distance(o.pos, fruitCell) > MazeTower.N8_SEEK_RANGE) continue;
				if (m == null || Dungeon.level.distance(o.pos, fruitCell) < Dungeon.level.distance(m.pos, fruitCell)) m = o;
			}
			eater = m == null ? -1 : m.id();
		}
		if (m != null && m.state != m.SLEEPING && m.paralysed <= 0){ //싸움을 멈추고 열매로 걸어간다 (Mob.MAZE_SEEK)
			m.mazeSeekCell = fruitCell;
			m.state = m.MAZE_SEEK;
		}
	}

	private static boolean canEat( Mob o ){
		return o != null && o.isAlive() && o.alignment == Alignment.ENEMY && !o.properties().contains(Property.IMMOVABLE)
				&& o.state != o.SLEEPING && o.state != o.PASSIVE && !(o instanceof EchoKnot) && o.buff(GroveFruit.class) == null; //(점검) 잠든 석상 같은 가만히 있는 적은 부르지 않음
	}

	//칸에 들어섰을 때 (Level.occupyCell)
	public static void onEnterCell( Char ch ){
		if (Dungeon.level == null || !ch.isAlive()) return; //(점검) 쓰러진 대상은 먹지 않는다
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof FruitBranch && ((FruitBranch) m).ripe() && ((FruitBranch) m).fruitCell == ch.pos){
				((FruitBranch) m).eat(ch);
				return;
			}
		}
	}

	private void eat( Char ch ){
		int at = fruitCell;
		if (ch instanceof Hero){
			Hero h = (Hero) ch;
			int heal = Math.min(h.HT - h.HP, MazeTower.N8_HEAL);
			h.HP += heal;
			if (h.sprite != null && heal > 0) h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			Buff.prolong(h, HeroFruit.class, MazeTower.N8_TURNS);
			GLog.p(Messages.get(FruitBranch.class, "hero_eats"));
			Sample.INSTANCE.play(Assets.Sounds.DRINK, 1f, 1.2f);
			if (Dungeon.level.heroFOV[at]) CellEmitter.get(at).burst(LeafParticle.LEVEL_SPECIFIC, 8);
		} else if (ch instanceof Mob && ch.alignment == Alignment.ENEMY && !(ch instanceof EchoKnot)){
			Buff.prolong(ch, GroveFruit.class, MazeTower.N8_TURNS);
			Buff.prolong(ch, Paralysis.class, 1f); //먹는 1턴은 무방비 (반드시 맞음)
			if (Dungeon.level.heroFOV[at]){
				GLog.w(Messages.get(FruitBranch.class, "mob_eats", ch.name()));
				if (ch.sprite != null){
					ch.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(FruitBranch.class, "eating"));
					ch.sprite.emitter().burst(ShadowParticle.UP, 6);
				}
				Sample.INSTANCE.play(Assets.Sounds.EAT, 1f, 0.8f);
			}
		} else return;
		fruitCell = -1;
		fruitLeft = 0;
		if (eater != -1){ Actor a = Actor.findById(eater); if (a instanceof Mob && ((Mob) a).mazeSeekCell == at) ((Mob) a).mazeSeekCell = -1; }
		eater = -1;
		growIn = MazeTower.N8_GROW_TURNS;
		if (sprite instanceof BranchSprite) ((BranchSprite) sprite).showFruit();
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc", MazeTower.N8_GROW_TURNS, MazeTower.N8_RIPEN);
		if (fruitCell == -1) d += "\n\n" + Messages.get(this, "now_grow", Math.max(0, growIn));
		else if (fruitLeft > 0) d += "\n\n" + Messages.get(this, "now_ripen", fruitLeft);
		else d += "\n\n" + Messages.get(this, "now_ripe");
		return d;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("maze_branch", new int[]{ fruitCell, fruitLeft, growIn, eater });
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("maze_branch")){ int[] a = bundle.getIntArray("maze_branch"); fruitCell = a[0]; fruitLeft = a[1]; growIn = a[2]; eater = a[3]; }
	}

	// ===== 층을 만들 때 =====
	public static void place( Level level ){
		if (!MazeTower.N8_BRANCH || Dungeon.depth < 23 || Dungeon.depth > 24) return;
		Random.pushGenerator(Dungeon.seedCurDepth() ^ 0x5F7B1A93L);
		try {
			boolean[] near = new boolean[level.length()];
			for (int t : new int[]{ level.entrance(), level.exit() }){
				if (t < 0) continue;
				PathFinder.buildDistanceMap(t, BArray.not(level.solid, null), 4);
				for (int i = 0; i < near.length; i++) if (PathFinder.distance[i] != Integer.MAX_VALUE) near[i] = true;
			}
			int placed = 0;
			for (int tries = 0; tries < 1000 && placed < MazeTower.N8_BRANCHES; tries++){
				int c = Random.Int(level.length());
				if (near[c] || !LivingVein.openFloor(level, c)) continue;
				if (!clearAround(level, c)) continue; //(사용자) 벽과 붙은 자리에는 놓지 않는다 (상하좌우 4칸이 모두 새잎 땅이 되게)
				FruitBranch b = new FruitBranch();
				b.pos = c;
				level.mobs.add(b);
				//(사용자) 가지 상하좌우 4칸은 새잎 땅 (그 위에만 열매가 맺힌다, 검은 낙엽은 지움)
				for (int n : PathFinder.NEIGHBOURS4){
					int s = c + n;
					if (s < 0 || s >= level.length() || level.solid[s] || level.pit[s] || level.water[s]) continue;
					com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SproutGround.grow(level, s);
				}
				placed++;
			}
		} finally {
			Random.popGenerator();
		}
	}

	//둘레 8칸에 벽·구덩이가 없고, 상하좌우 4칸은 물도 아닌 바닥
	private static boolean clearAround( Level level, int c ){
		for (int n : PathFinder.NEIGHBOURS8){
			int d = c + n;
			if (d < 0 || d >= level.length() || level.solid[d] || level.pit[d] || !level.passable[d]) return false;
		}
		for (int n : PathFinder.NEIGHBOURS4) if (level.water[c + n]) return false;
		return true;
	}

	// ===== 먹은 효과 =====

	//적이 먹은 열매: 주는 피해 +10% (Char.attack), 받는 피해 -10% (Char.damage)
	public static class GroveFruit extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon(){ return BuffIndicator.HERB_HEALING; }
		@Override public void tintIcon( Image icon ){ icon.hardlight(0.55f, 0.9f, 0.4f); }
		@Override public float iconFadePercent(){ return Math.max(0, (MazeTower.N8_TURNS - visualcooldown()) / MazeTower.N8_TURNS); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round((MazeTower.N8_DMG - 1) * 100), Math.round((1 - MazeTower.N8_TAKEN) * 100), dispTurns()); }
	}

	//주인공이 먹은 열매: 주는 피해 +10% (적이 받는 피해, Char.damage), 받는 피해 -10% (Hero.damage)
	public static class HeroFruit extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = true; }
		@Override public int icon(){ return BuffIndicator.HERB_HEALING; }
		@Override public void tintIcon( Image icon ){ icon.hardlight(0.55f, 0.9f, 0.4f); }
		@Override public float iconFadePercent(){ return Math.max(0, (MazeTower.N8_TURNS - visualcooldown()) / MazeTower.N8_TURNS); }
		@Override public String desc(){ return Messages.get(this, "desc", Math.round((MazeTower.N8_DMG - 1) * 100), Math.round((1 - MazeTower.N8_TAKEN) * 100), dispTurns()); }
	}

	//Char.attack: 먹은 적이 주는 피해
	public static float dealtMulti( Char attacker ){
		return attacker.buff(GroveFruit.class) != null ? MazeTower.N8_DMG : 1f;
	}

	//Char.damage: 먹은 적이 받는 피해 감소 / 주인공이 먹었으면 적이 받는 피해 증가
	public static int takenModify( Char ch, int dmg, Object src ){
		if (dmg <= 0) return dmg;
		if (ch.buff(GroveFruit.class) != null) dmg = Math.round(dmg * MazeTower.N8_TAKEN);
		//(점검) 주인공이 준 피해만 (주인공·그 무기·마법봉·던진 것 등: 출처가 캐릭터면 주인공이어야 하고, 상태이상·장판·다른 몹은 아님)
		boolean byHero = src == Dungeon.hero || (src != null && !(src instanceof Char) && !(src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff) && !(src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob) && !(src instanceof Class) && !(src instanceof EchoKnot.ShellBurst));
		if (byHero && ch instanceof Mob && ch.alignment == Alignment.ENEMY && Dungeon.hero != null && Dungeon.hero.buff(HeroFruit.class) != null) dmg = Math.round(dmg * MazeTower.N8_DMG);
		return dmg;
	}

	// ===== 그림 =====

	//(임시) 세계수 어린 줄기의 첫 단계 그림을 가지로 빌려 씀 (새 그림은 사용자 확인 뒤). 열매는 25층 세계수의 열매 그림
	public static class BranchSprite extends MobSprite {
		private Group fruit;

		@Override public void turnTo( int from, int to ){ flipHorizontal = false; }

		public BranchSprite(){
			super();
			//(사용자 확인 뒤 N8_BRANCH_ART) sprites/maze_fruitbranch.png 16×24 × 4: 세계수의 어린 가지, 잎 끝·꽃심이 숨 쉼 (원본 docs/sprites/mobs/n_draft/make_n8_branch.ps1)
			if (com.shatteredpixel.shatteredpixeldungeon.MazeTower.N8_BRANCH_ART){
				texture("sprites/maze_fruitbranch.png");
				TextureFilm f2 = new TextureFilm(texture, 16, 24);
				idle = new Animation(2, true);
				idle.frames(f2, 0, 1, 2, 1);
				run = idle.clone(); attack = idle.clone(); zap = idle.clone(); die = idle.clone();
				play(idle);
				return;
			}
			texture("sprites/maze_b25_sapling.png");
			TextureFilm f = new TextureFilm(texture, 16, 32);
			idle = new Animation(1, true);
			idle.frames(f, 0);
			run = idle.clone(); attack = idle.clone(); zap = idle.clone(); die = idle.clone();
			play(idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			showFruit();
		}

		void showFruit(){
			if (fruit != null){ fruit.killAndErase(); fruit = null; }
			if (!(ch instanceof FruitBranch) || ((FruitBranch) ch).fruitCell == -1 || parent == null) return;
			fruit = new BranchFruit((FruitBranch) ch);
			GameScene.groupEffect(fruit);
		}

		@Override
		public void kill() {
			if (fruit != null) fruit.killAndErase();
			super.kill();
		}
	}

	//열매 그림 (sprites/maze_fruit_small.png 16×16 × 6: 0~3 익어 감, 4·5 다 익음), 본 칸에만
	public static class BranchFruit extends Group {
		private final FruitBranch b;
		private final Image img;
		private final TextureFilm film;
		private final com.watabou.noosa.Halo halo;
		private float t = 0;
		BranchFruit( FruitBranch b ){
			this.b = b;
			halo = new com.watabou.noosa.Halo(8, 0xC8F088, 0f);
			add(halo);
			img = new Image("sprites/maze_fruit_small.png"); //(사용자 확인) 25층 열매를 작게·초록빛은 같고 어둡게 (원본 docs/sprites/mobs/n_draft/make_n8_fruit.ps1)
			film = new TextureFilm(img.texture, 16, 16);
			img.frame(film.get(0));
			add(img);
		}
		@Override
		public void update() {
			super.update();
			t += Game.elapsed;
			if (b.fruitCell == -1 || !b.isAlive()){ killAndErase(); return; }
			PointF tl = DungeonTilemap.tileToWorld(b.fruitCell);
			float ripe = 1f - Math.max(0, b.fruitLeft) / (float) MazeTower.N8_RIPEN;
			int fr = b.ripe() ? ((int)(t * 2.5f) % 2 == 0 ? 4 : 5) : Math.min(3, (int)(ripe * 4));
			img.frame(film.get(fr));
			img.x = tl.x; img.y = tl.y;
			halo.point(tl.x + 8, tl.y + 6);
			halo.am = b.ripe() ? 0.15f + 0.08f * (float)Math.sin(t * 4f) : 0f; //큰 열매보다 은은하게
			boolean seen = Dungeon.level != null && Dungeon.level.heroFOV[b.fruitCell];
			img.visible = halo.visible = seen;
		}
	}
}
