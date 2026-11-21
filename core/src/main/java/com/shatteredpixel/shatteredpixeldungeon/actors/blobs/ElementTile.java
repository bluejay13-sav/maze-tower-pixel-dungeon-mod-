/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ElementalReactions;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbite;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.StaticCharge;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.HashMap;

//Maze Tower: 원소 감응 타일 (20층 마도시의 여제 2단계, 명세 22번). 칸 값: 1 붉은(화상) / 2 푸른(냉기) / 3 노란(정전기).
//주인공이 위에 있어도 상태이상은 걸리지 않지만, 그 위에서 여제의 원소 공격·폭풍에 맞거나
//반대 원소 상태로 들어서면 원소 반응이 터진다(MazeEmpress.tileReaction). 주인공이 밟고 지나간 타일은 사라진다.
//타일 지우기: 정화의 물약 = 범위 안 모두, 냉기(냉각 물약·서리 마법봉) = 붉은, 화염(화염 물약·화염 마법봉 등 불) = 푸른(김).
//노란 타일은 불에 닿으면 그 칸에서 폭발 반응이 일어나고 사라지지 않는다(정화의 물약으로만 지워짐).
//그림 maze_fx20e_tile_fire/frost/shock.png: 7프레임 × 48줄. 줄 = 모양 변형(0~2) × 16 + 이웃 연결값(위1 오른쪽2 아래4 왼쪽8).
//평소 0번 정지, 칸마다 무작위 간격(약 3~7초)으로 1~6번을 한 번 반짝.
public class ElementTile extends Blob {

	public static final int FIRE = 1, FROST = 2, SHOCK = 3;

	//불에 닿아 이미 폭발한 노란 타일 (불이 꺼질 때까지 다시 터지지 않게, 저장하지 않음)
	private boolean[] overloaded;

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		Fire fire = (Fire) l.blobs.get(Fire.class);
		if (overloaded == null || overloaded.length != l.length()) overloaded = new boolean[l.length()];
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*l.width();
				off[cell] = cur[cell];
				boolean burning = fire != null && fire.volume > 0 && fire.cur[cell] > 0;
				if (off[cell] == FROST && burning){
					off[cell] = 0;
					steamFx(cell);
				} else if (off[cell] == SHOCK){
					if (burning && !overloaded[cell]){
						overloaded[cell] = true;
						ElementalReactions.explodeAt(cell);
					} else if (!burning){
						overloaded[cell] = false;
					}
				}
				volume += off[cell];
			}
		}
	}

	public static ElementTile get(){
		return Dungeon.level == null ? null : (ElementTile) Dungeon.level.blobs.get(ElementTile.class);
	}

	public static int at( int cell ){
		ElementTile t = get();
		if (t == null || t.cur == null || cell < 0 || cell >= t.cur.length) return 0;
		return t.cur[cell];
	}

	public static int count(){
		ElementTile t = get();
		if (t == null || t.cur == null) return 0;
		int n = 0;
		for (int v : t.cur) if (v > 0) n++;
		return n;
	}

	public static void place( int cell, int type ){
		if (at(cell) != 0) return;
		GameScene.add(Blob.seed(cell, type, ElementTile.class));
	}

	public static void remove( int cell ){
		ElementTile t = get();
		if (t != null && t.cur != null && cell >= 0 && cell < t.cur.length && t.cur[cell] > 0) t.clear(cell);
	}

	public static void removeAll(){
		ElementTile t = get();
		if (t != null && t.cur != null){
			for (int i = 0; i < t.cur.length; i++) if (t.cur[i] > 0) t.clear(i);
		}
	}

	private static void steamFx( int cell ){
		if (Dungeon.level.heroFOV[cell]){
			BossEffects.atCellBottom(BossEffects.Fx.EM_TILE_STEAM, cell, 10, false);
			Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.4f);
		}
	}

	// ===== 지우기 =====

	//밟고 지나간 타일은 사라진다 (CityBossLevel.occupyCell)
	public static void onHeroLeft( int cell ){
		remove(cell);
	}

	//냉기가 닿은 칸: 붉은 타일이 사라진다 (Freezing.freeze, 서리 마법봉)
	public static void onFrost( int cell ){
		if (at(cell) == FIRE){
			remove(cell);
			steamFx(cell);
		}
	}

	//정화의 물약 범위
	public static void onPurify( int cell ){
		remove(cell);
	}

	// ===== 주인공이 원소 상태로 타일에 들어섰다 (예: 정전기 상태로 붉은 타일) =====
	public static void onHeroEnter( Char hero ){
		int t = at(hero.pos);
		if (t == 0) return;
		int status = statusOf(hero);
		if (status != 0) react(hero, status, t);
	}

	//원소 attack에 맞을 때 반응할 수 있는, 대상이 지닌 원소 상태 (불 ↔ 전격·냉기). 없으면 0
	public static int reactingStatus( Char ch, int attack ){
		boolean burning = ch.buff(Burning.class) != null;
		if (attack == FIRE){
			if (ch.buff(StaticCharge.class) != null) return SHOCK;
			if (ch.buff(Chill.class) != null || ch.buff(Frostbite.class) != null) return FROST;
			return 0;
		}
		return burning ? FIRE : 0;
	}

	//대상이 지닌 원소 상태 (화상 = 불, 정전기 = 전격, 냉기·혹한 = 냉기, 없으면 0)
	public static int statusOf( Char ch ){
		if (ch.buff(Burning.class) != null) return FIRE;
		if (ch.buff(StaticCharge.class) != null) return SHOCK;
		if (ch.buff(Chill.class) != null || ch.buff(Frostbite.class) != null) return FROST;
		return 0;
	}

	//원소 attack이 타일 tile 위의 대상에게 닿았다. 반응이 일어나면 true
	public static boolean react( Char ch, int attack, int tile ){
		if (attack == 0 || tile == 0 || attack == tile) return false;
		boolean fireFrost = (attack == FIRE && tile == FROST) || (attack == FROST && tile == FIRE);
		boolean fireShock = (attack == FIRE && tile == SHOCK) || (attack == SHOCK && tile == FIRE);
		if (fireFrost){
			ElementalReactions.steamOn(ch, Random.NormalIntRange(MazeTower.EM_TILE_STEAM_MIN, MazeTower.EM_TILE_STEAM_MAX));
			return true;
		}
		if (fireShock){
			ElementalReactions.explodeAt(ch.pos);
			return true;
		}
		return false; //냉기와 전격은 반응하지 않는다
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.add(new Layer(this));
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

	// ===== 그림 =====

	public static class Layer extends Group {

		private static final String[] FILES = {
				null,
				"sprites/maze_fx20e_tile_fire.png",
				"sprites/maze_fx20e_tile_frost.png",
				"sprites/maze_fx20e_tile_shock.png"
		};
		private static final float FRAME = 0.1f;

		private final ElementTile blob;
		private final HashMap<Integer, Image> imgs = new HashMap<>();
		private final HashMap<Integer, Integer> types = new HashMap<>();
		private final HashMap<Integer, Float> nextFlash = new HashMap<>();
		private final HashMap<Integer, Float> flashStart = new HashMap<>();
		private final TextureFilm[] films = new TextureFilm[4];
		private float time = 0;

		public Layer( ElementTile blob ){
			this.blob = blob;
		}

		private TextureFilm film( int type, Image img ){
			if (films[type] == null) films[type] = new TextureFilm(img.texture, 16, 16);
			return films[type];
		}

		//칸마다 고정된 모양 변형 (0~2)
		private static int variant( int cell ){
			long h = (Dungeon.seed ^ 0x7E11E5L) + cell * 0x9E3779B97F4A7C15L;
			h ^= (h >>> 31);
			return (int) Math.floorMod(h, 3L);
		}

		@Override
		public synchronized void update() {
			super.update();
			if (blob.cur == null || Dungeon.level == null) return;
			time += Game.elapsed;
			int w = Dungeon.level.width();
			int[] cur = blob.cur;
			for (int i = 0; i < cur.length; i++){
				int t = cur[i];
				Image img = imgs.get(i);
				if (t > 0 && (img == null || types.get(i) != t)){
					if (img != null) img.killAndErase();
					img = new Image(FILES[t]);
					img.x = (i % w) * DungeonTilemap.SIZE;
					img.y = (i / w) * DungeonTilemap.SIZE;
					add(img);
					imgs.put(i, img);
					types.put(i, t);
					nextFlash.put(i, time + Random.Float(0.5f, 7f));
					flashStart.remove(i);
				} else if (t == 0 && img != null){
					img.killAndErase();
					imgs.remove(i);
					types.remove(i);
					nextFlash.remove(i);
					flashStart.remove(i);
					img = null;
				}
				if (img == null) continue;

				img.visible = Dungeon.level.heroFOV[i];

				//같은 원소 이웃과 이어 붙인다
				int mask = 0;
				if (i - w >= 0 && cur[i - w] == t)                    mask |= 1;
				if ((i % w) < w - 1 && cur[i + 1] == t)               mask |= 2;
				if (i + w < cur.length && cur[i + w] == t)            mask |= 4;
				if ((i % w) > 0 && cur[i - 1] == t)                   mask |= 8;
				int row = variant(i) * 16 + mask;

				//가끔 1~6번을 한 번 반짝 (한 프레임 0.1초)
				int frame = 0;
				Float fs = flashStart.get(i);
				if (fs == null && time >= nextFlash.get(i)){
					flashStart.put(i, time);
					fs = time;
				}
				if (fs != null){
					int f = 1 + (int)((time - fs) / FRAME);
					if (f > 6){
						flashStart.remove(i);
						nextFlash.put(i, time + Random.Float(3f, 7f));
					} else {
						frame = f;
					}
				}
				img.frame(film(t, img).get(row * 7 + frame));
			}
		}
	}
}
