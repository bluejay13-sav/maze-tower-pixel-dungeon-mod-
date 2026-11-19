/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

//Maze Tower: 보스 스킬 이펙트 (명세 19번). 그림은 core/src/main/assets/sprites/maze_fx*.png.
//프레임을 가로로 이은 시트를 한 번(또는 반복) 재생한다.
public class BossEffects {

	public enum Fx {
		//5층 썩은 수액 덩어리
		SAP_CHARGE ("sprites/maze_fx5_charge.png",       24, 16, 4),
		SAP_SLAM   ("sprites/maze_fx5_slam.png",         48, 48, 5),
		SAP_PUDDLE ("sprites/maze_fx5_sap_puddle.png",   16, 16, 3),
		ROOT_SAP   ("sprites/maze_sap_puddle.png",       16, 16, 2), //뿌리 지하 수액 웅덩이 바닥
		REGION_SOUL      ("sprites/maze_fx_region_soul.png",      16, 24, 6), //봉인의 회랑 분위기
		REGION_MAGIC     ("sprites/maze_fx_region_magic.png",     16, 16, 8), //잊힌 마도시 분위기
		REGION_HEARTBEAT ("sprites/maze_fx_region_heartbeat.png", 48, 48, 8), //탑의 심장 분위기
		SAP_HEAL   ("sprites/maze_fx5_sap_heal.png",     16, 24, 4),
		//10층 그림자 간수
		TELEPORT   ("sprites/maze_fx10_teleport.png",    24, 24, 5),
		CHAIN_FLY  ("sprites/maze_fx10_chain_fly.png",   16,  8, 2),
		CHAIN_HIT  ("sprites/maze_fx10_chain_hit.png",   16, 16, 3),
		TALISMAN   ("sprites/maze_fx10_talisman_fly.png", 8, 10, 2),
		TALISMAN_BOOM("sprites/maze_fx10_talisman_boom.png", 32, 32, 5),
		BIND       ("sprites/maze_fx10_bind.png",        16, 16, 2),
		SOUL       ("sprites/maze_fx10_soul.png",        16, 24, 4),
		BLOOM_CHARGE("sprites/maze_fx10_bloom_charge.png", 32, 32, 4),
		BLOOM_BURST("sprites/maze_fx10_bloom_burst.png", 32, 32, 3),
		BEAM_H     ("sprites/maze_fx10_bloom_beam_h.png", 16, 16, 3),
		BEAM_D     ("sprites/maze_fx10_bloom_beam_d.png", 16, 16, 3),
		//15층 마도 골렘
		SLAM5      ("sprites/maze_fx15g_slam5x5.png",    80, 80, 5),
		VEIN_BREAK ("sprites/maze_fx15g_vein_break.png", 16, 24, 4),
		MANA_VAPOR ("sprites/maze_fx15g_mana_vapor.png", 16, 16, 3),
		STUN       ("sprites/maze_fx15g_stun.png",       16,  8, 2),
		//15층 수정 거인
		PILLAR_BEAM("sprites/maze_fx15c_pillar_beam.png",16, 16, 3),
		PILLAR_GLOW("sprites/maze_fx15c_pillar_glow.png",24, 24, 3),
		FROST_TILE ("sprites/maze_fx15c_frost_tile.png", 16, 16, 2),
		FROST_SHARD("sprites/maze_fx15c_frost_shard.png", 8,  8, 2),
		FROST_TRAIL("sprites/maze_fx15c_frost_trail.png",16, 16, 4),
		MELT       ("sprites/maze_fx15c_melt.png",       24, 24, 3),
		STEAM      ("sprites/maze_fx15c_steam.png",      32, 32, 5);

		public final String file;
		public final int w, h, frames;

		Fx( String file, int w, int h, int frames ){
			this.file = file;
			this.w = w;
			this.h = h;
			this.frames = frames;
		}
	}

	public static MovieClip clip( Fx fx, int fps, boolean loop ){
		final MovieClip c = new MovieClip(fx.file);
		TextureFilm film = new TextureFilm(c.texture, fx.w, fx.h);
		Object[] idx = new Object[fx.frames];
		for (int i = 0; i < fx.frames; i++) idx[i] = i;
		if (!loop){
			c.listener = new MovieClip.Listener() {
				@Override
				public void onComplete(MovieClip.Animation a) {
					c.killAndErase();
				}
			};
		}
		c.play(new MovieClip.Animation(fps, loop).frames(film, idx));
		return c;
	}

	//칸 가운데에 그림 가운데를 맞춘다 (3×3·5×5처럼 큰 그림도 그 칸을 중심으로)
	public static MovieClip atCell( Fx fx, int cell, int fps, boolean loop ){
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		return at(fx, p.x, p.y, fps, loop);
	}

	//칸 아래쪽에 그림 아래쪽을 맞춘다 (위로 솟는 이펙트)
	public static MovieClip atCellBottom( Fx fx, int cell, int fps, boolean loop ){
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		MovieClip c = clip(fx, fps, loop);
		c.x = p.x - fx.w / 2f;
		c.y = p.y + DungeonTilemap.SIZE / 2f - fx.h;
		GameScene.effect(c);
		return c;
	}

	public static MovieClip at( Fx fx, float cx, float cy, int fps, boolean loop ){
		MovieClip c = clip(fx, fps, loop);
		c.x = cx - fx.w / 2f;
		c.y = cy - fx.h / 2f;
		GameScene.effect(c);
		return c;
	}

	//캐릭터 몸 위에 겹친다
	public static MovieClip onChar( Fx fx, CharSprite s, int fps, boolean loop ){
		if (s == null) return null;
		PointF c = s.center();
		return at(fx, c.x, c.y, fps, loop);
	}

	public static boolean visible( int cell ){
		return Dungeon.level != null && cell >= 0 && cell < Dungeon.level.length() && Dungeon.level.heroFOV[cell];
	}
}
