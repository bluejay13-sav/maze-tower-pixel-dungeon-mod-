/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ChainedSoul;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

//Maze Tower (H6): 사슬 고리와 갇힌 영혼을 잇는 사슬 (원작 사슬 그림을 한 줄로 늘어놓는다, 움직이지 않음).
//고리나 영혼이 사라지거나 영혼이 풀려나면 사라진다.
public class SoulChain extends Group {

	private static final double A = 180 / Math.PI;

	private final int ringId, soulId;
	//Maze Tower (I7): 선고 사슬 (끌려 나온 영혼 → 주인공). 선고가 끝나면 사라진다
	private final boolean sentence;

	public SoulChain( int ringId, int soulId ){
		this(ringId, soulId, false);
	}

	public SoulChain( int fromId, int soulId, boolean sentence ){
		this.ringId = fromId;
		this.soulId = soulId;
		this.sentence = sentence;
	}

	@Override
	public void update() {
		super.update();
		Actor r = Actor.findById(ringId), s = Actor.findById(soulId);
		if (!(r instanceof Char) || !(s instanceof Char) || !((Char) r).isAlive() || !((Char) s).isAlive() || (s instanceof ChainedSoul && ((ChainedSoul) s).freed)
				|| ((Char) r).sprite == null || ((Char) s).sprite == null){
			killAndErase();
			return;
		}
		if (sentence){
			com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence sn =
					((Char) r).buff(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ShadowWarden.Sentence.class);
			if (sn == null || sn.soulId != soulId){ killAndErase(); return; }
		}
		Char ring = (Char) r, soul = (Char) s;
		visible = ring.sprite.visible || soul.sprite.visible;
		PointF from = ring.sprite.center(), to = soul.sprite.center();
		if (!sentence) from.y += 3; //고리 아래쪽에서
		float dx = to.x - from.x, dy = to.y - from.y;
		float dist = (float) Math.hypot(dx, dy);
		int n = Math.max(1, Math.round(dist / 5f));
		float angle = (float)(Math.atan2(dy, dx) * A) + 90f;
		while (length < n){
			Image link = new Image(Effects.get(Effects.Type.CHAIN));
			link.origin.set(link.width() / 2, link.height() / 2);
			add(link);
		}
		for (int i = 0; i < length; i++){
			Image link = (Image) members.get(i);
			link.visible = i < n;
			link.angle = angle;
			link.scale.set(0.8f);
			link.center(new PointF(from.x + dx * (i + 0.5f) / n, from.y + dy * (i + 0.5f) / n));
		}
	}
}
