/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//Maze Tower: 망령 왕 1→2단계 "어둠이 찾아옴" 연출 (명세 22번, 2026-11-21 결정).
//  1. 화면 가장자리부터 어두워지고, 밝은 원이 왕 쪽으로 좁혀져 왕이 마지막으로 남는다 (가장자리는 부드럽게)
//  2. 완전히 깜깜해진다
//  3. 깜깜한 가운데 왕의 푸른 눈이 한 번 선명하게 빛났다가 사라진다
//     (결정) 기습 전 푸른 눈 동작(fx20w_eyes)을 더 밝게 만든 그림(maze_fx20w_eyes_bright.png)을 25%가 아니라 또렷하게 쓴다
//  4. 끝나면 onDone (그때 실제 어둠 상태가 된다). 화면 흔들림은 없다.
//안개 위 층(GameScene.effectOverFog)에 그려 다른 모든 칸을 덮는다.
public class WraithDarkIntro extends Group {

	private static final float HOLE_TEX = 128f;  //maze_fx20w_dark_hole.png 크기
	private static final float HOLE_MID = 42f;   //그림 안에서 부드러운 가장자리의 가운데 반지름
	private static final float BIG = 4000f;

	private static final int[] EYE_FRAMES = { 0, 1, 2, 3, 2, 1 };
	private static final float[] EYE_TIMES = { 0.09f, 0.09f, 0.14f, 0.26f, 0.14f, 0.16f };

	private final CharSprite king;
	private final Callback onDone;
	private final PointF center;
	private final PointF eyeAt;

	private final Image hole;
	private final ColorBlock top, bottom, left, right;
	private final Image eyes;
	private final TextureFilm eyeFilm;

	private final float startR;
	private float time = 0;
	private boolean done = false;

	public WraithDarkIntro( CharSprite king, Callback onDone ){
		this.king = king;
		this.onDone = onDone;
		center = king.center();
		//왕의 해골 눈 자리 (서 있는 그림 22×25 기준 가로 11.5, 세로 6). 왼쪽을 보면 좌우 반전
		float ex = king.flipHorizontal ? king.width - 11.5f : 11.5f;
		eyeAt = new PointF(king.x + ex, king.y + 6f);

		SmartTexture tx = TextureCache.get("sprites/maze_fx20w_dark_hole.png");
		tx.filter(Texture.LINEAR, Texture.LINEAR);
		hole = new Image(tx);
		add(hole);
		top = new ColorBlock(1, 1, 0xFF000000);
		bottom = new ColorBlock(1, 1, 0xFF000000);
		left = new ColorBlock(1, 1, 0xFF000000);
		right = new ColorBlock(1, 1, 0xFF000000);
		add(top); add(bottom); add(left); add(right);

		eyes = new Image("sprites/maze_fx20w_eyes_bright.png");
		eyeFilm = new TextureFilm(eyes.texture, 16, 16);
		eyes.frame(eyeFilm.get(0));
		eyes.x = eyeAt.x - 8f;
		eyes.y = eyeAt.y - 8f;
		eyes.visible = false;
		add(eyes);

		//처음 원은 화면 전체를 덮을 만큼 크게 (왕에서 화면 대각선 길이만큼)
		Camera c = Camera.main;
		float w = c.width / c.zoom, h = c.height / c.zoom;
		startR = (float)Math.sqrt(w * w + h * h) + 40f;

		layoutHole(startR);
	}

	private void layoutHole( float r ){
		float s = Math.max(r, 0.01f) / HOLE_MID;
		hole.scale.set(s);
		float size = HOLE_TEX * s;
		hole.x = center.x - size / 2f;
		hole.y = center.y - size / 2f;
		//원이 작아지면 바깥을 검은 판으로 덮는다
		top.size(BIG * 2, BIG);       top.x = hole.x - BIG;    top.y = hole.y - BIG;
		bottom.size(BIG * 2, BIG);    bottom.x = hole.x - BIG; bottom.y = hole.y + size;
		left.size(BIG, size);         left.x = hole.x - BIG;   left.y = hole.y;
		right.size(BIG, size);        right.x = hole.x + size; right.y = hole.y;
	}

	@Override
	public void update() {
		super.update();
		if (done) return;
		time += Game.elapsed;

		float shrink = MazeTower.WK_DARK_SHRINK_TIME;
		float black = MazeTower.WK_DARK_BLACK_TIME;
		if (time < shrink){
			//가장자리부터 천천히, 끝으로 갈수록 빨리 좁혀진다
			float p = time / shrink;
			p = p * p * (3 - 2 * p);
			layoutHole(startR + (MazeTower.WK_DARK_LAST_R - startR) * p);
		} else if (time < shrink + black){
			//왕까지 어둠에 잠긴다 (앞 절반 동안 원이 닫힘)
			float p = Math.min(1f, (time - shrink) / (black * 0.5f));
			layoutHole(MazeTower.WK_DARK_LAST_R * (1f - p));
		} else {
			layoutHole(0);
			float t = time - shrink - black;
			int i = 0;
			while (i < EYE_TIMES.length && t >= EYE_TIMES[i]){
				t -= EYE_TIMES[i];
				i++;
			}
			if (i < EYE_TIMES.length){
				eyes.visible = true;
				eyes.frame(eyeFilm.get(EYE_FRAMES[i]));
			} else {
				eyes.visible = false;
				done = true;
				killAndErase();
				if (onDone != null) onDone.call();
			}
		}
	}

	//저장·불러오기 등으로 연출이 끊길 때를 위해: 그림 없이 바로 끝
	public static boolean canShow( CharSprite king ){
		return king != null && king.parent != null && Camera.main != null;
	}
}
