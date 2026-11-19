/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

//Maze Tower: 마탑 보스 그림 (명세 19번). 그림은 core/src/main/assets/sprites/maze_b*_sheet.png.
//동작 시트는 13프레임을 가로로 이은 것: 대기 0–1, 이동 2–5, 공격 6–8, 사망 9–12.
//원작 보스 그림 클래스(GooSprite, TenguSprite, DM300Sprite, KingSprite, YogSprite)의 생성자에서
//이 클래스로 그림을 바꾼다. 원작 특수 동작(구의 부풀기, DM-300 돌진 등)은 공격 프레임으로 대신한다.
public class MazeBossSprites {

	public static final int[] IDLE   = {0, 1};
	public static final int[] MOVE   = {2, 3, 4, 5};
	public static final int[] ATTACK = {6, 7, 8};
	public static final int[] DIE    = {9, 10, 11, 12};

	public enum Look {
		SAP        ("sprites/maze_b1_sap_sheet.png",        26, 18), //5층 썩은 수액 덩어리
		WARDEN     ("sprites/maze_b2_warden_sheet.png",     23, 24), //10층 그림자 간수
		GOLEM      ("sprites/maze_b3_golem_sheet.png",      31, 26), //15층 마도 골렘
		CRYSTAL    ("sprites/maze_b4_crystal_sheet.png",    27, 28), //15층 수정 거인
		WRAITHKING ("sprites/maze_b5_wraithking_sheet.png", 24, 25), //20층 망령 왕
		EMPRESS    ("sprites/maze_b6_empress_sheet.png",    24, 23), //20층 마도시의 여제
		MASTER     ("sprites/maze_b7_master_sheet.png",     32, 24); //25층 탑의 주인

		public final String file;
		public final int w, h;

		Look( String file, int w, int h ){
			this.file = file;
			this.w = w;
			this.h = h;
		}
	}

	public static Look look15(){ return MazeBosses.crystalGiant() ? Look.CRYSTAL : Look.GOLEM; }
	public static Look look20(){ return MazeBosses.empress() ? Look.EMPRESS : Look.WRAITHKING; }

	//그림을 바꾸고 기본 동작(대기·이동·공격·사망)을 채운다. 특수 동작에 쓸 수 있게 TextureFilm을 돌려준다.
	public static TextureFilm apply( CharSprite s, Look look ){
		s.texture(look.file);
		TextureFilm film = new TextureFilm(s.texture, look.w, look.h);
		s.idle   = anim(film, 3, true,  IDLE);
		s.run    = anim(film, 10, true, MOVE);
		s.attack = anim(film, 15, false, ATTACK);
		s.zap    = s.attack.clone();
		s.die    = anim(film, 8, false, DIE);
		return film;
	}

	public static MovieClip.Animation anim( TextureFilm film, int fps, boolean loop, int... frames ){
		Object[] idx = new Object[frames.length];
		for (int i = 0; i < frames.length; i++) idx[i] = frames[i];
		return new MovieClip.Animation(fps, loop).frames(film, idx);
	}
}
