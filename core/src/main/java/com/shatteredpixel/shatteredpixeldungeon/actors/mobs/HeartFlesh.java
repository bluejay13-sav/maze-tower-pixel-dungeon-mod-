/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Bundle;

//Maze Tower: 25층 1페이즈 살점 (지시서 1122b 2절). 짐승이 찾아가 3턴 동안 먹고 최대 체력 5% 회복한다.
//오염: 유독 가스 물약(독)·마비 가스 물약(마비)·서리 물약/서리 마법봉/혹한(냉기)·부식 마법봉(부식)·타락 마법봉(타락 마법봉이 보스에게 거는 디버프).
//  오염된 살점을 먹으면 회복하지 않고 그 상태이상에 걸린다. 여러 번 오염시키면 마지막 것만 남는다.
//  살점은 피해를 받지 않는 물건이다(상태이상은 붙지 않고 오염으로만 바뀐다).
public class HeartFlesh extends NPC {

	public static final int CLEAN = 0, POISON = 1, PARALYSIS = 2, CHILL = 3, CORRODE = 4, CORRUPT = 5;

	{
		spriteClass = FleshSprite.class;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	public int type = CLEAN;
	public float corrodeLeft = 2f;    //부식: 원작 부식 가스가 거는 값 그대로
	public float corrodeDmg = 1f;
	public int corruptLvl = 0;        //타락: 쏜 마법봉의 레벨 (원작 디버프 지속 6 + 레벨 × 3)
	public boolean eaten = false;     //짐승이 먹는 중
	public boolean dirty = false;     //(2026-11-22) 한 번이라도 오염됐으면 true. 먹어도 회복하지 않는다

	public boolean contaminated(){ return dirty || type != CLEAN; }

	public static HeartFlesh at( int cell ){
		Char c = Actor.findChar(cell);
		return c instanceof HeartFlesh ? (HeartFlesh) c : null;
	}

	@Override
	protected boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public boolean interact( Char c ) { return true; }

	@Override
	public boolean isInvulnerable( Class effect ) { return true; }

	@Override
	public void damage( int dmg, Object src ) {
		//유독 가스: 원작 가스는 상태이상이 아니라 피해를 주므로 여기서 오염으로 받는다
		if (src instanceof ToxicGas) contaminate(POISON);
		else if (isFire(src)) burnAway();
	}

	//(결정, 2026-11-22) 불(화염 물약·불 마법봉·불 함정·횃불 등)에 닿으면 살점이 타서 재로 흩어진다(짐승이 먹을 수 없게 됨).
	//그림은 원작 시트(maze_b7_flesh_fx.png)의 4줄(타오름)·6줄(재로 흩어짐).
	private static boolean isFire( Object src ){
		if (src == null) return false;
		if (src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire
				|| src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning) return true;
		String n = src instanceof Class ? ((Class<?>) src).getSimpleName() : src.getClass().getSimpleName();
		return n.contains("Fire") || n.contains("Burning") || n.contains("Inferno") || n.contains("Flame");
	}

	private boolean burning = false;

	public void burnAway(){
		if (burning) return;
		burning = true;
		if (sprite instanceof FleshSprite) ((FleshSprite) sprite).burn();
		removeFromLevel();
	}

	@Override
	public boolean add( Buff buff ) {
		if (buff instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning){ burnAway(); return false; }
		if (buff instanceof Paralysis) contaminate(PARALYSIS);
		else if (buff instanceof Chill || buff instanceof Frost || buff instanceof Frostbite) contaminate(CHILL);
		else if (buff instanceof Poison) contaminate(POISON);
		else if (buff instanceof Corrosion){
			Bundle b = new Bundle();
			buff.storeInBundle(b);
			corrodeLeft = Math.max(1f, b.getFloat("left"));
			corrodeDmg = Math.max(1f, b.getFloat("damage"));
			contaminate(CORRODE);
		}
		return false; //살점에는 상태이상이 붙지 않는다
	}

	//타락 마법봉 (WandOfCorruption.onZap에서 부른다)
	public void contaminateCorrupt( int wandLvl ){
		corruptLvl = wandLvl;
		contaminate(CORRUPT);
	}

	public void contaminate( int t ){
		if (eaten && type == t) return;
		boolean changed = type != t;
		type = t;
		if (t != CLEAN) dirty = true;
		if (changed && sprite instanceof FleshSprite) ((FleshSprite) sprite).contaminate(t);
		//(2026-11-22) 오염되는 순간 살점 위에 "오염됨!" 글자
		if (changed && t != CLEAN && sprite != null && sprite.parent != null){
			sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "contaminated"));
		}
	}

	//짐승이 다 먹었다 / 2페이즈 시작 때 터진다
	public void vanish( boolean explode ){
		if (sprite instanceof FleshSprite){
			if (explode) ((FleshSprite) sprite).explode();
			else ((FleshSprite) sprite).eatenAway();
		}
		removeFromLevel();
	}

	//(2026-11-22) 2페이즈 진입: 부풀어 밝아지다(약 1.4초) 터진다. 맵에서는 바로 빠진다
	public void swellAndBurst(){
		if (sprite instanceof FleshSprite) ((FleshSprite) sprite).swellExplode();
		removeFromLevel();
	}

	//먹히는 중 (짐승이 한 턴 먹을 때마다): 살점이 조금씩 작아지고 핏방울
	public void bitten( int left ){
		if (sprite instanceof FleshSprite) ((FleshSprite) sprite).bite(left);
	}

	//맵에서 뺀다. (2026-11-22) 체력을 0으로 해서 영웅이 겨눴던 체력 막대가 남지 않게
	private void removeFromLevel(){
		HP = 0;
		if (sprite != null) sprite.visible = false;
		Dungeon.level.mobs.remove(this);
		Actor.remove(this);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc");
		if (type != CLEAN) d += "\n\n" + Messages.get(this, "contam_" + type);
		return d;
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put("type", type);
		bundle.put("dirty", dirty);
		bundle.put("corrode_left", corrodeLeft);
		bundle.put("corrode_dmg", corrodeDmg);
		bundle.put("corrupt_lvl", corruptLvl);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		type = bundle.getInt("type");
		dirty = bundle.getBoolean("dirty") || type != CLEAN;
		corrodeLeft = bundle.getFloat("corrode_left");
		corrodeDmg = bundle.getFloat("corrode_dmg");
		corruptLvl = bundle.getInt("corrupt_lvl");
	}

	// =====================================================================
	// 그림: maze_b7_flesh_contaminated.png (16×16 × 6: 일반·독·마비·냉기·부식·타락, 정지 그림)
	//       오염되는 순간 maze_b7_flesh_contam_anim.png (16×16 × 6단계 × 5줄, 바깥 테두리부터 안쪽으로 번짐, 약 0.6초)
	//       먹히는 동안·터질 때 maze_b7_flesh_fx.png (24×24 × 12, 1줄 먹힘, 6줄 부풀어 터짐)
	// =====================================================================
	public static class FleshSprite extends MobSprite {

		private static final String TEX = "sprites/maze_b7_flesh_contaminated.png";
		private static final String ANIM = "sprites/maze_b7_flesh_contam_anim.png";
		private static final String FX = "sprites/maze_b7_flesh_fx.png";

		private int shown = -1;
		private Image spread;   //오염이 번지는 그림
		private float spreadT = -1;
		private int spreadType;

		public FleshSprite(){
			super();
			texture(TEX);
			TextureFilm film = new TextureFilm(texture, 16, 16);
			idle = new MovieClip.Animation(1, true);
			idle.frames(film, 0);
			run = idle.clone();
			attack = idle.clone();
			die = idle.clone();
			play(idle);
		}

		@Override
		public void link( Char ch ) {
			super.link(ch);
			if (ch instanceof HeartFlesh) showType(((HeartFlesh) ch).type);
		}

		private void showType( int t ){
			if (shown == t) return;
			shown = t;
			TextureFilm film = new TextureFilm(texture, 16, 16);
			idle = new MovieClip.Animation(1, true);
			idle.frames(film, Math.max(0, Math.min(5, t)));
			play(idle, true);
		}

		public void contaminate( int t ){
			if (t == HeartFlesh.CLEAN){ showType(t); return; }
			spreadType = t;
			spreadT = 0;
			if (spread == null && parent != null){
				spread = new Image(ANIM);
				parent.add(spread);
			}
		}

		@Override
		public void update() {
			super.update();
			if (spread != null){
				spread.x = x + (width - 16) / 2f;
				spread.y = y + (height - 16) / 2f;
				spread.visible = visible;
				if (spreadT >= 0){
					spreadT += Game.elapsed;
					int step = Math.min(5, (int)(spreadT / (MazeTower.B25_CONTAM_TIME / 6f)));
					spread.frame(step * 16, (spreadType - 1) * 16, 16, 16);
					alpha(0f);
					if (spreadT >= MazeTower.B25_CONTAM_TIME){
						spreadT = -1;
						spread.killAndErase();
						spread = null;
						showType(spreadType);
						alpha(1f);
					}
				}
			}
		}

		//fx 시트의 row줄 from~to칸을 살점 자리에서 한 번 재생하고 살점 그림은 지운다. then이 있으면 이어서 다른 줄
		private void playFx( int row, int from, int to, float delay, final int[] then ){
			if (parent == null){ killAndErase(); return; }
			final float px = x + (width - 24) / 2f, py = y + (height - 24) / 2f;
			final com.watabou.noosa.Group g = parent;
			clip(g, px, py, row, from, to, delay, then);
			if (spread != null){ spread.killAndErase(); spread = null; }
			killAndErase();
		}

		private static void clip( final com.watabou.noosa.Group g, final float px, final float py, int row, int from, int to, float delay, final int[] then ){
			final MovieClip c = new MovieClip(FX);
			TextureFilm film = new TextureFilm(c.texture, 24, 24);
			Object[] f = new Object[to - from + 1];
			for (int i = from; i <= to; i++) f[i - from] = row * 12 + i;
			MovieClip.Animation a = new MovieClip.Animation(Math.round(1 / delay), false).frames(film, f);
			c.listener = new MovieClip.Listener() {
				@Override
				public void onComplete( MovieClip.Animation anim ) {
					c.killAndErase();
					//then[4] == 1: 부푼 뒤 터짐 (살점 조각·피가 사방으로)
					if (then != null && then.length > 4 && then[4] == 1) chunks(g, px + 12, py + 13);
					if (then != null) clip(g, px, py, then[0], then[1], then[2], then[3] / 1000f, null);
				}
			};
			c.play(a);
			c.x = px; c.y = py;
			g.add(c);
		}

		//(2026-11-22) 먹히는 3턴 동안 조금씩 작아지고 핏방울. 다 먹으면 남은 부분만 빠르게 부서진다 (예전엔 0.8초 남아 있는 것처럼 보였음)
		public void bite( int left ){
			float k = 1f - 0.15f * (MazeTower.B25_EAT_TURNS - left);
			scale.set(Math.max(0.55f, k));
			if (parent != null){
				com.watabou.noosa.particles.Emitter e = new com.watabou.noosa.particles.Emitter();
				e.pos(x + 4, y + 4, width - 8, height - 8);
				parent.add(e);
				e.burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle.BURST, 4);
			}
		}

		public void eatenAway(){ playFx(1, 7, 11, 0.04f, null); }
		//(2026-11-22) 터짐: 7줄의 6·7칸(가운데 점이 있는 고리)이 눈이 떠지는 것처럼 보여서 쓰지 않는다.
		//  부풀기(0~5칸) → 코드로 살점 조각·피가 사방으로 튐 → 흩어진 핏방울(8~11칸)
		public void explode(){ playFx(6, 3, 5, 0.06f, new int[]{6, 8, 11, 60, 1}); }
		public void swellExplode(){ playFx(6, 0, 5, 0.23f, new int[]{6, 8, 11, 60, 1}); }

		private static final int[] CHUNK_COLORS = { 0xFFC0304A, 0xFFE0607A, 0xFF8A1A30, 0xFFF08CA0, 0xFF6A1024 };

		//살점 조각이 사방으로 튀어 포물선을 그리며 떨어지고 흐려진다 + 피 입자
		private static void chunks( final com.watabou.noosa.Group g, final float cx, final float cy ){
			int n = 9;
			for (int i = 0; i < n; i++){
				int sz = com.watabou.utils.Random.Int(3) == 0 ? 3 : 2;
				final com.watabou.noosa.ColorBlock b = new com.watabou.noosa.ColorBlock(sz, sz, CHUNK_COLORS[com.watabou.utils.Random.Int(CHUNK_COLORS.length)]);
				b.x = cx - sz / 2f; b.y = cy - sz / 2f;
				g.add(b);
				double a = Math.PI * 2 * (i + com.watabou.utils.Random.Float(0.6f)) / n;
				final float sp = com.watabou.utils.Random.Float(38f, 62f);
				final float vx0 = (float)Math.cos(a) * sp;
				final float vy0 = (float)Math.sin(a) * sp * 0.7f - com.watabou.utils.Random.Float(20f, 40f);
				final float life = com.watabou.utils.Random.Float(0.35f, 0.5f);
				g.add(new com.watabou.noosa.Gizmo(){
					float t = 0, vx = vx0, vy = vy0;
					@Override public void update(){
						float dt = Game.elapsed;
						t += dt;
						vy += 220f * dt;
						b.x += vx * dt; b.y += vy * dt;
						b.alpha(Math.max(0f, 1f - t / life));
						if (t >= life){ b.killAndErase(); killAndErase(); }
					}
				});
			}
			com.watabou.noosa.particles.Emitter e = new com.watabou.noosa.particles.Emitter();
			e.pos(cx - 4, cy - 4, 8, 8);
			g.add(e);
			e.burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle.BURST, 12);
		}
		//불에 탐: 4줄(타오름) → 6줄(재로 흩어짐)
		public void burn(){ playFx(3, 0, 11, 0.05f, new int[]{5, 0, 11, 60}); }
	}
}
