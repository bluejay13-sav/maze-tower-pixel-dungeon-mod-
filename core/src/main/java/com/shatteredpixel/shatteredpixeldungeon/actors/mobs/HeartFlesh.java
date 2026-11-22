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
	}

	@Override
	public boolean add( Buff buff ) {
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
		if (changed && sprite instanceof FleshSprite) ((FleshSprite) sprite).contaminate(t);
	}

	//짐승이 다 먹었다 / 2페이즈 시작 때 터진다
	public void vanish( boolean explode ){
		if (sprite instanceof FleshSprite){
			if (explode) ((FleshSprite) sprite).explode();
			else ((FleshSprite) sprite).eatenAway();
		}
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
		bundle.put("corrode_left", corrodeLeft);
		bundle.put("corrode_dmg", corrodeDmg);
		bundle.put("corrupt_lvl", corruptLvl);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		type = bundle.getInt("type");
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

		private void playFx( int row, float delay ){
			final MovieClip c = new MovieClip(FX);
			TextureFilm film = new TextureFilm(c.texture, 24, 24);
			Object[] f = new Object[12];
			for (int i = 0; i < 12; i++) f[i] = row * 12 + i;
			MovieClip.Animation a = new MovieClip.Animation(Math.round(1 / delay), false).frames(film, f);
			c.listener = new MovieClip.Listener() {
				@Override
				public void onComplete( MovieClip.Animation anim ) { c.killAndErase(); }
			};
			c.play(a);
			c.x = x + (width - 24) / 2f;
			c.y = y + (height - 24) / 2f;
			if (parent != null) parent.add(c);
			if (spread != null){ spread.killAndErase(); spread = null; }
			killAndErase();
		}

		public void eatenAway(){ playFx(1, 0.07f); }
		public void explode(){ playFx(6, 0.06f); }
	}
}
