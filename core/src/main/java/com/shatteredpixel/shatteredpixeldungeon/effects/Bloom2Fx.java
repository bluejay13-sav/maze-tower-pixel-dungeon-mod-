/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

//Maze Tower: 적 머리 위 표시. 그림은 fx2_sheet.png (좌표는 docs/sprites/talent2/README.md).
//(2026-11-24) 예전 2차 특성을 지우면서 그 연출도 지웠다. 지금은 결투가의 결투 상대 표식(예전 표식 그림)과
//박힌 화살 그림만 남았다.
public class Bloom2Fx {

	//머리 위 표식 1~4개 (x, y, w, h)
	private static final int[][] MARKS = {{0,181,5,5},{7,181,10,5},{19,181,11,9},{32,181,16,9}};

	// =====================================================================
	// 머리 위 표식 (1·2개 가로, 3개 세모, 4개 사다리꼴)
	// =====================================================================

	public static class MarkOverhead extends Image {
		private final CharSprite sprite;
		private int stacks = -1;

		public MarkOverhead(CharSprite sprite){
			super(Assets.Effects.MAZE_FX2);
			this.sprite = sprite;
		}

		public void stacks(int n){
			n = Math.max(1, Math.min(4, n));
			if (n == stacks) return;
			stacks = n;
			int[] r = MARKS[n - 1];
			frame(r[0], r[1], r[2], r[3]);
		}

		@Override
		public void update() {
			super.update();
			if (sprite == null || sprite.parent == null){
				killAndErase();
				return;
			}
			visible = sprite.visible && stacks > 0;
			x = sprite.x + (sprite.width() - width()) / 2f;
			y = sprite.y - height() - 1;
			PixelScene.align(this);
		}
	}

	public static MarkOverhead markOverhead(CharSprite s){
		if (s == null || s.parent == null) return null;
		MarkOverhead m = new MarkOverhead(s);
		GameScene.effect(m); //벽·기둥에 가리지 않게 벽 층보다 위(효과 층)에
		return m;
	}

	// =====================================================================
	// (D18) 박힌 화살: 적 몸통에 쏜 방향 그대로 꽂혀 은은하게 빛나는 화살 (날아가는 화살 그림을 작게)
	// =====================================================================

	public static class EmbeddedArrowFx extends com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite {
		private final Char ch;
		private float t = 0;

		public EmbeddedArrowFx(Char ch, float angle){
			super(com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites.ARROW_SHOT, null);
			this.ch = ch;
			scale.set(0.7f);
			origin.set(width / 2f, height / 2f);
			this.angle = angle;
		}

		@Override
		public void update() {
			super.update();
			CharSprite sprite = ch.sprite;
			if (sprite == null || sprite.parent == null || !ch.isAlive()){
				killAndErase();
				return;
			}
			t += Game.elapsed;
			float glow = 0.15f + 0.15f * (float)Math.sin(t * 3f);
			tint(0.62f, 1f, 0.5f, glow); //새잎빛으로 숨 쉬듯
			visible = sprite.visible;
			x = sprite.center().x - width / 2f;
			y = sprite.center().y - height / 2f;
		}
	}

	public static EmbeddedArrowFx embeddedArrow(Char ch, float angle){
		if (ch == null || ch.sprite == null || ch.sprite.parent == null) return null;
		EmbeddedArrowFx a = new EmbeddedArrowFx(ch, angle);
		GameScene.effect(a);
		return a;
	}
}
