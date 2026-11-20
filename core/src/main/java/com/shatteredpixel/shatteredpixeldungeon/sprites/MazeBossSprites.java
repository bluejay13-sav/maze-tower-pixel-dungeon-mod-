/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.MazeBosses;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

//Maze Tower: 마탑 보스 그림 (명세 19번). 그림은 core/src/main/assets/sprites/maze_b*_sheet.png.
//보스 그림은 오른쪽을 본다(수액 덩어리 제외). 왼쪽은 원작처럼 좌우 반전.
//동작 시트는 13프레임을 가로로 이은 것: 대기 0–1, 이동 2–5, 공격 6–8, 사망 9–12.
//원작 보스 그림 클래스(GooSprite, TenguSprite, DM300Sprite, KingSprite, YogSprite)의 생성자에서
//이 클래스로 그림을 바꾼다. 원작 특수 동작(구의 부풀기, DM-300 돌진 등)은 공격 프레임으로 대신한다.
public class MazeBossSprites {

	public static final int[] IDLE   = {0, 1};
	public static final int[] MOVE   = {2, 3, 4, 5};
	//공격 마지막 칸(8번)은 하얗게 빛나는 그림이라 눈이 아파 쓰지 않는다 (6, 7을 보여 주고 대기 모습으로)
	public static final int[] ATTACK = {6, 7, 7, 0};
	public static final int[] DIE    = {9, 10, 11, 12};

	public enum Look {
		SAP        ("sprites/maze_b1_sap_sheet.png",        26, 18), //5층 썩은 수액 덩어리
		WARDEN     ("sprites/maze_b2_warden_sheet.png",     24, 24), //10층 그림자 간수
		GOLEM      ("sprites/maze_b3_golem_sheet.png",      31, 26), //15층 마도 골렘
		CRYSTAL    ("sprites/maze_b4_crystal_sheet.png",    27, 28), //15층 수정 거인
		WRAITHKING ("sprites/maze_b5_wraithking_sheet.png", 22, 25), //20층 망령 왕
		EMPRESS    ("sprites/maze_b6_empress_sheet.png",    22, 23), //20층 마도시의 여제
		MASTER     ("sprites/maze_b7_master_sheet.png",     32, 27); //25층 탑의 주인

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
		return applyFile(s, look.file, look.w, look.h);
	}

	//같은 13프레임 순서의 다른 시트로 바꾼다 (수액 덩어리 쪼그라든 모습, 망령 왕 기어다니는 모습 등)
	public static TextureFilm applyFile( CharSprite s, String file, int w, int h ){
		s.texture(file);
		TextureFilm film = new TextureFilm(s.texture, w, h);
		s.idle   = anim(film, 3, true,  IDLE);
		s.run    = anim(film, 10, true, MOVE);
		s.attack = anim(film, 15, false, ATTACK);
		s.zap    = s.attack.clone();
		s.die    = anim(film, 8, false, DIE);
		return film;
	}

	// ===== 25층 탑의 주인: 변신 단계 (명세 19번) =====
	//phase 0 = 처음(보석 6개 모두 빛남, 날개 없음). 1~6 = 보석이 하나씩 검게 바랜 시트(p1~p6).
	//변신 기능은 아직 없으므로 지금은 0단계 모습만 쓴다. 변신을 넣을 때 setMasterPhase를 부른다.
	public static final int[] MASTER_FADE   = {13, 14, 15, 16}; //방금 변신한 직업 보석이 바랜다
	public static final int[] MASTER_AWAKEN = {17, 18, 19, 20}; //p6 시트: 각성 (붉은 눈 → 날개)

	public static String masterSheet( int phase ){
		return phase <= 0 ? Look.MASTER.file : "sprites/maze_b7_master_p" + Math.min(phase, 6) + "_sheet.png";
	}

	//다음 단계 시트로 바꾸고 fade를 한 번 재생한 뒤 대기로 돌아간다. 6단계는 fade → awaken을 이어서 재생한다.
	public static void setMasterPhase( final CharSprite s, int phase ){
		s.texture(masterSheet(phase));
		final TextureFilm film = new TextureFilm(s.texture, Look.MASTER.w, Look.MASTER.h);
		s.idle   = anim(film, 3, true,  IDLE);
		s.run    = anim(film, 10, true, MOVE);
		s.attack = anim(film, 15, false, ATTACK);
		s.zap    = s.attack.clone();
		s.die    = anim(film, 8, false, DIE);
		if (phase <= 0){
			s.play(s.idle);
			return;
		}
		int[] frames = MASTER_FADE;
		if (phase >= 6){
			frames = new int[]{13, 14, 15, 16, 17, 18, 19, 20};
		}
		//한 번 재생하는 동작: CharSprite.onComplete가 모르는 동작이므로 끝나면 직접 대기로 돌린다
		final MovieClip.Animation change = anim(film, 6, false, frames);
		final MovieClip.Listener old = s.listener;
		s.listener = new MovieClip.Listener() {
			@Override
			public void onComplete( MovieClip.Animation a ) {
				if (a == change){
					s.listener = old;
					s.idle();
				} else if (old != null){
					old.onComplete(a);
				}
			}
		};
		s.play(change);
	}

	//전직의 핵을 지닌 보스 몸 주위의 벚꽃잎 (5·10·20층 모두 희미하게). CharSprite.link에서 부른다
	public static void attachPetals( CharSprite s, com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ){
		if (!MazeBosses.ENABLED || s.parent == null) return;
		int level;
		if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo)            level = com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle.FAINT;
		else if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu)     level = com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle.FAINT; //(결정) 10층도 5층과 같은 정도로
		else if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.WraithKing || ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MazeEmpress) level = com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle.FAINT; //20층 마탑 보스
		else if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing) level = com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle.FAINT; //(결정) 20층도 5층과 같은 정도로
		else return;
		s.parent.add(new com.shatteredpixel.shatteredpixeldungeon.effects.particles.BossPetalParticle.Petals(s, level));
	}

	public static MovieClip.Animation anim( TextureFilm film, int fps, boolean loop, int... frames ){
		Object[] idx = new Object[frames.length];
		for (int i = 0; i < frames.length; i++) idx[i] = frames[i];
		return new MovieClip.Animation(fps, loop).frames(film, idx);
	}
}
