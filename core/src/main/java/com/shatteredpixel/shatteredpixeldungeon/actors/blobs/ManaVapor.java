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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.HashSet;

//Maze Tower (J1): 마력 증기. 11~14층의 살아 있는 광맥·갉힌 채굴 기계와 15층 보스방의 광맥에서 난다. (사용자) 원작 독가스처럼 퍼지며 옅어진다.
//- 그 위에 있으면 원작 독가스와 같은 지속 피해, 주인공은 스킬을 쓰지 못한다. 벗어나도 J1_LINGER턴 동안 이어진다(Clogged, 아이콘).
//- 불(불 장판·불붙은 대상)이나 전격(전기 장판·전격 피해)이 닿으면 이어진 증기가 한 번에 터져 사라지고, 그 안의 모두가 피해를 받는다(주인공도).
//- 정화 물약으로 지우고, 정화의 면역(BlobImmunity) 중에는 영향이 없다. 보스(마도 골렘)·새끼 굴벌레는 면역.
//- 그림: 원작 맹독 구름과 같은 입자를 마나 푸른빛으로(Speck.MANA_VAPOR, 사용자 확인)
public class ManaVapor extends Blob {

	//전격 피해가 증기 칸의 대상에게 들어갔다 (ElementalReactions.onDamage): 다음 증기 차례에 터진다
	private static final HashSet<Integer> pendingIgnite = new HashSet<>();

	public static void igniteAt( int cell ){
		Blob b = Dungeon.level == null ? null : Dungeon.level.blobs.get(ManaVapor.class);
		Blob sb = Dungeon.level == null ? null : Dungeon.level.blobs.get(SpitVapor.class); //Maze Tower (K4)
		if (sb != null && sb.cur != null && cell >= 0 && cell < sb.cur.length && sb.cur[cell] > 0) pendingIgnite.add(cell);
		if (b != null && b.cur != null && cell >= 0 && cell < b.cur.length && b.cur[cell] > 0) pendingIgnite.add(cell);
	}

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		int w = l.width();

		//1) 불·전격이 닿은 칸이 있으면 이어진 증기가 한 번에 터진다
		Blob fire = l.blobs.get(Fire.class), elec = l.blobs.get(Electricity.class);
		ArrayList<Integer> sparks = new ArrayList<>(pendingIgnite);
		pendingIgnite.clear();
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				int cell = i + j * w;
				if (cur[cell] <= 0) continue;
				boolean hot = (fire != null && fire.cur != null && fire.cur[cell] > 0)
						|| (elec != null && elec.cur != null && elec.cur[cell] > 0);
				Char ch = Actor.findChar(cell);
				if (ch != null && ch.buff(Burning.class) != null) hot = true;
				if (hot) sparks.add(cell);
			}
		}
		for (int c : sparks) if (cur[c] > 0) burst(c);

		//2) (사용자) 원작 독가스처럼 이웃 칸으로 퍼지며 옅어진다
		super.evolve();

		//3) 증기 안: 지속 피해와 스킬 막힘
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				int cell = i + j * w;
				if (cur[cell] <= 0) continue;

				Char ch = Actor.findChar(cell);
				if (ch != null && !ch.isImmune(getClass())){
					//(결정) 증기 안에서는 원작 독가스처럼 그 턴에만 피해(1 + 층/5)를 받고, 마력독에 걸린다.
					//  증기에서 얻은 마력독은 스킬만 막고(남은 턴을 J1_LINGER로 채움) 피해는 주지 않는다 — 나오면 피해는 멈춘다
					int dmg = 1 + Dungeon.scalingDepth() / 5;
					ch.damage(dmg, this);
					if (ch == Dungeon.hero && !ch.isAlive()){
						Dungeon.fail(this);
						GLog.n(Messages.get(this, "ondeath"));
					}
					if (ch.isAlive()) VaporBite.apply(ch, (int) MazeTower.J1_LINGER, 0);
				}
			}
		}
	}

	//이어진 증기 전체가 터진다 (8방향으로 이어진 칸)
	private void burst( int from ){
		Level l = Dungeon.level;
		ArrayList<Integer> region = new ArrayList<>();
		ArrayList<Integer> open = new ArrayList<>();
		HashSet<Integer> seen = new HashSet<>();
		open.add(from); seen.add(from);
		while (!open.isEmpty()){
			int c = open.remove(open.size() - 1);
			region.add(c);
			for (int n : PathFinder.NEIGHBOURS8){
				int d = c + n;
				if (d < 0 || d >= cur.length || seen.contains(d) || cur[d] <= 0) continue;
				seen.add(d);
				open.add(d);
			}
		}
		boolean seenByHero = false;
		for (int c : region){
			cur[c] = 0;
			if (l.heroFOV[c]){
				seenByHero = true;
				CellEmitter.center(c).burst(Speck.factory(Speck.BLUE_LIGHT), 3);
				CellEmitter.get(c).burst(Speck.factory(Speck.MANA_VAPOR), 3);
			}
		}
		int dmg = Math.round((MazeTower.REACTION_BASE_FLAT + MazeTower.REACTION_BASE_PER_DEPTH * Dungeon.scalingDepth()) * MazeTower.J1_BURST_MULTI);
		for (int c : region){
			Char ch = Actor.findChar(c);
			if (ch == null || !ch.isAlive() || ch.isImmune(getClass())) continue;
			ch.damage(dmg, new Burst());
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(Burst.class);
				GLog.n(Messages.get(this, "burst_kill"));
			}
		}
		if (seenByHero){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 1.2f);
			PixelScene.shake(1, 0.15f);
			GLog.w(Messages.get(this, "burst"));
		}
	}

	//터진 증기의 피해 출처 (사망 기록)
	public static class Burst {}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( Speck.factory( Speck.MANA_VAPOR ), 0.4f );
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc", MazeTower.J1_LINGER);
	}

	//스킬을 쓰지 못하는지 (증기 안이거나 나온 지 얼마 안 됨)
	public static boolean skillsBlocked( Char hero ){
		return hero != null && (hero.buff(Clogged.class) != null || hero.buff(VaporBite.class) != null);
	}

	//Maze Tower (J6): 수정 거미에게 물림. 원작 독 대신 마력 증기에 닿은 것처럼 턴마다 증기 피해를 받고 스킬을 쓰지 못한다
	public static class VaporBite extends Buff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		private int left = 0;      //스킬이 막히는 남은 턴
		private int biteLeft = 0;  //피해를 받는 남은 턴 (수정 거미에게 물렸을 때만)

		public static void apply( Char ch, int turns, int damageTurns ){
			VaporBite b = Buff.affect( ch, VaporBite.class );
			b.left = Math.max( b.left, turns );
			b.biteLeft = Math.max( b.biteLeft, damageTurns );
		}

		@Override
		public boolean act() {
			if (target.isAlive() && biteLeft > 0){
				int dmg = 1 + Dungeon.scalingDepth() / 5; //마력 증기 안과 같은 피해
				target.damage( dmg, this );
				biteLeft--;
				if (target == Dungeon.hero && !target.isAlive()){
					Dungeon.fail( this );
					GLog.n( Messages.get(this, "ondeath") );
				}
			}
			if (--left <= 0) detach();
			spend( TICK );
			return true;
		}

		@Override public int icon() { return BuffIndicator.WAND; }
		@Override public void tintIcon( Image icon ) { icon.hardlight(0.55f, 0.45f, 1f); }
		@Override public String iconTextDisplay() { return Integer.toString( left ); }
		@Override public String desc() {
			if (biteLeft > 0) return Messages.get(this, "desc", 1 + Dungeon.scalingDepth() / 5, left);
			return Messages.get(this, "desc_vapor", left);
		}

		private static final String LEFT = "left";
		private static final String BITE_LEFT = "bite_left";
		@Override public void storeInBundle( com.watabou.utils.Bundle bundle ) { super.storeInBundle( bundle ); bundle.put( LEFT, left ); bundle.put( BITE_LEFT, biteLeft ); }
		@Override public void restoreFromBundle( com.watabou.utils.Bundle bundle ) { super.restoreFromBundle( bundle ); left = bundle.getInt( LEFT ); biteLeft = bundle.contains( BITE_LEFT ) ? bundle.getInt( BITE_LEFT ) : left; }
	}

	//마력 증기에 막힘: 스킬을 쓰지 못한다
	public static class Clogged extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}
		@Override public int icon() { return BuffIndicator.WAND; }
		@Override public void tintIcon( Image icon ) { icon.hardlight(0.35f, 0.55f, 1f); }
		@Override public float iconFadePercent() { return Math.max(0, (MazeTower.J1_LINGER - visualcooldown()) / MazeTower.J1_LINGER); }
		@Override public String iconTextDisplay() { return Integer.toString((int) visualcooldown()); }
		@Override public String desc() { return Messages.get(this, "desc", dispTurns()); }
	}
}
