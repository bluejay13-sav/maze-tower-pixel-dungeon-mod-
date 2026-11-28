/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.PointF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

//Maze Tower (I8·I9): 10층 그림자 간수 2단계의 예고 그림.
//사슬 그물·끌어내기의 줄(ChainLine), 대봉인이 차오르는 바닥(SealFloor), 대봉인을 피할 안전지대(SafeZone).
//모두 안개 위에 그려 가려지지 않는다(GameScene.groupOverFog). 은은하게 숨 쉬듯 밝아졌다 어두워진다.
public class WardenFx {

	private static final int S = DungeonTilemap.SIZE;

	private static PointF center( int cell ){
		int w = Dungeon.level.width();
		return new PointF((cell % w + 0.5f) * S, (cell / w + 0.5f) * S);
	}

	//칸 가운데에서 칸 가운데까지 이어지는 가는 사슬 줄 (바깥 흐린 빛 + 가는 심 + 고리 마디 점)
	public static class ChainLine extends Group {

		private final ArrayList<ColorBlock> glow = new ArrayList<>(), core = new ArrayList<>(), links = new ArrayList<>();
		private float time = 0;

		public ChainLine( int fromCell, int toCell, int color ){
			this(center(fromCell), center(toCell), color);
		}

		public ChainLine( PointF a, PointF b, int color ){
			float dx = b.x - a.x, dy = b.y - a.y;
			float len = (float)Math.sqrt(dx*dx + dy*dy);
			float angle = (float)(Math.atan2(dy, dx) * 180 / Math.PI);
			float ux = dx / Math.max(len, 0.001f), uy = dy / Math.max(len, 0.001f);
			glow.add(bar(a, len + 2, 3f, angle, color));
			core.add(bar(a, len + 2, 1f, angle, color | 0x202020));
			//고리 마디: 4픽셀마다 조금 진한 점
			for (float s = 2; s < len; s += 4){
				ColorBlock d = new ColorBlock(1.5f, 1.5f, 0xFF000000 | darker(color));
				d.x = a.x + ux * s - 0.75f;
				d.y = a.y + uy * s - 0.75f;
				add(d);
				links.add(d);
			}
			update();
		}

		private ColorBlock bar( PointF a, float len, float t, float angle, int color ){
			ColorBlock c = new ColorBlock(len, t, 0xFF000000 | color);
			c.origin.set(0, 0.5f);
			c.x = a.x;
			c.y = a.y - 0.5f;
			c.angle = angle;
			add(c);
			return c;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float p = 0.75f + 0.25f * (float)Math.sin(time * 6f);
			for (ColorBlock c : glow) c.alpha(0.22f * p);
			for (ColorBlock c : core) c.alpha(0.75f * p);
			for (ColorBlock c : links) c.alpha(0.6f * p);
		}
	}

	private static int darker( int c ){
		int r = (c >> 16) & 0xFF, g = (c >> 8) & 0xFF, b = c & 0xFF;
		return ((r * 3 / 5) << 16) | ((g * 3 / 5) << 8) | (b * 3 / 5);
	}

	//대봉인이 차오르는 바닥: 칸마다 어두운 바탕 + 가운데 금빛 마름모 문양. 남은 턴이 줄수록 진해진다
	public static class SealFloor extends Group {

		private static final int BASE = 0x2A0F30, GLYPH = 0xD8B04A;
		private final ArrayList<ColorBlock> fills = new ArrayList<>(), glyphs = new ArrayList<>();
		private float progress = 0, time = 0;

		public SealFloor( Collection<Integer> cells ){
			int w = Dungeon.level.width();
			for (int c : cells){
				float x = (c % w) * S, y = (c / w) * S;
				ColorBlock f = new ColorBlock(S, S, 0xFF000000 | BASE);
				f.x = x; f.y = y;
				add(f); fills.add(f);
				//마름모 문양 (가로·세로 막대 두 개와 네 귀퉁이 점)
				glyph(x + S / 2f - 3, y + S / 2f - 0.5f, 6, 1);
				glyph(x + S / 2f - 0.5f, y + S / 2f - 3, 1, 6);
				glyph(x + 3, y + 3, 1, 1);
				glyph(x + S - 4, y + 3, 1, 1);
				glyph(x + 3, y + S - 4, 1, 1);
				glyph(x + S - 4, y + S - 4, 1, 1);
			}
			update();
		}

		private void glyph( float x, float y, float ww, float hh ){
			ColorBlock g = new ColorBlock(ww, hh, 0xFF000000 | GLYPH);
			g.x = x; g.y = y;
			add(g); glyphs.add(g);
		}

		//0 = 막 시작, 1 = 터지기 직전
		public void progress( float p ){
			progress = Math.max(0, Math.min(1, p));
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float pulse = 0.85f + 0.15f * (float)Math.sin(time * (4f + 6f * progress));
			for (ColorBlock f : fills) f.alpha((0.10f + 0.35f * progress) * pulse);
			for (ColorBlock g : glyphs) g.alpha((0.25f + 0.65f * progress) * pulse);
		}
	}

	//대봉인을 피할 안전지대: 새잎빛 옅은 바탕 + 바깥 테두리
	public static class SafeZone extends Group {

		private static final int COLOR = 0xA8E07C;
		private final ArrayList<ColorBlock> fills = new ArrayList<>(), edges = new ArrayList<>();
		private float time = 0;

		public SafeZone( Collection<Integer> cells ){
			HashSet<Integer> set = new HashSet<>(cells);
			int w = Dungeon.level.width();
			for (int c : cells){
				float x = (c % w) * S, y = (c / w) * S;
				ColorBlock f = new ColorBlock(S, S, 0xFF000000 | COLOR);
				f.x = x; f.y = y;
				add(f); fills.add(f);
				if (!set.contains(c - w)) edge(x, y, S, 1);
				if (!set.contains(c + w)) edge(x, y + S - 1, S, 1);
				if (c % w == 0 || !set.contains(c - 1)) edge(x, y, 1, S);
				if (c % w == w - 1 || !set.contains(c + 1)) edge(x + S - 1, y, 1, S);
			}
			update();
		}

		private void edge( float x, float y, float ww, float hh ){
			ColorBlock b = new ColorBlock(ww, hh, 0xFF000000 | COLOR);
			b.x = x; b.y = y;
			add(b); edges.add(b);
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			float p = 0.8f + 0.2f * (float)Math.sin(time * 3f);
			for (ColorBlock f : fills) f.alpha(0.10f * p);
			for (ColorBlock e : edges) e.alpha(0.55f * p);
		}
	}
}
