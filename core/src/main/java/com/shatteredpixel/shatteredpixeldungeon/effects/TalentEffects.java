/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

//Maze Tower: 특성 발동 이펙트.
//(2026-11-24) 예전 2차 특성을 지우면서 그 연출도 지웠다. 지금은 마나 반환(사냥꾼 3차)과 섬광 보법 잔상만 있다.
public class TalentEffects {

	private static boolean visible( Char ch ){
		return ch != null && ch.sprite != null && ch.sprite.parent != null && ch.sprite.visible;
	}

	//섬광 보법: 떠난 칸에 빛 잔상
	public static void flashStep( Char hero, int from ){
		if (!visible(hero)) return;
		CellEmitter.get(from).burst(Speck.factory(Speck.LIGHT), 4);
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 1.4f);
	}

	//(C5) 메아리: 주인공 모습의 푸른 잔상이 대상 쪽으로 살짝 다가갔다가 흐려진다
	public static void echoGhost( Char hero, Char target ){
		if (!visible(hero) || !(hero.sprite instanceof com.watabou.noosa.MovieClip)) return;
		final com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite s = hero.sprite;
		if (s.frame() == null) return;
		final com.watabou.noosa.Image a = new com.watabou.noosa.Image(s.texture);
		a.frame(new com.watabou.utils.RectF(s.frame().left, s.frame().top, s.frame().right, s.frame().bottom));
		a.flipHorizontal = s.flipHorizontal;
		a.x = s.x; a.y = s.y;
		a.hardlight(0.45f, 0.70f, 1f);
		a.alpha(0.55f);
		float dx = 0, dy = 0;
		if (target != null && target.sprite != null){
			dx = Math.signum(target.sprite.x - s.x) * 3f;
			dy = Math.signum(target.sprite.y - s.y) * 3f;
		}
		final float fdx = dx, fdy = dy, sx = s.x, sy = s.y;
		s.parent.add(a);
		s.parent.add(new com.watabou.noosa.Gizmo(){
			final float total = 0.4f;
			float t = total;
			@Override public void update(){
				t -= com.watabou.noosa.Game.elapsed;
				float p = 1f - Math.max(0, t) / total;
				a.x = sx + fdx * p;
				a.y = sy + fdy * p;
				a.alpha(Math.max(0, 0.55f * (1f - p)));
				if (t <= 0){ a.killAndErase(); killAndErase(); }
			}
		});
	}

	//마나 반환: 처치한 적 자리에서 주인공에게 남빛 구슬이 천천히 날아든다 (fx4_06)
	public static void manaRefund( final Char hero, Char from, final int amount ){
		if (!visible(hero)) return;
		if (from != null && from.sprite != null && from.sprite.parent != null){
			PointF a = from.sprite.center(), b = hero.sprite.center();
			float time = Math.max(0.25f, (float)Math.hypot(b.x - a.x, b.y - a.y) / MazeTower.MANA_REFUND_BOLT_SPEED);
			MazeFx.fly(MazeFx.Fx.MANA_ORB, a, b, time, new Callback() {
				@Override
				public void call() {
					if (hero.sprite != null){
						hero.sprite.showStatus(0x4488FF, "+" + amount);
					}
				}
			});
		} else {
			hero.sprite.showStatus(0x4488FF, "+" + amount);
		}
	}
}
