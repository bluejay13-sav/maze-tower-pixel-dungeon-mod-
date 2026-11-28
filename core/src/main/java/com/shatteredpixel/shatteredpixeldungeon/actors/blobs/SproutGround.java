/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.blobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.effects.TileFxLayer;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//Maze Tower (N1): 새잎 땅. 퍼지지도 사라지지도 않는다. 그 위에 서 있는 주인공은 마나가 턴마다 더 찬다(N1_SPROUT_MANA, "새잎" 아이콘).
//검은 낙엽이 쌓이지 않는다(그 칸의 낙엽을 지운다). 검은 뿌리 매듭(N7)을 끊은 자리에 생긴다.
public class SproutGround extends Blob {

	@Override
	protected void evolve() {
		Level l = Dungeon.level;
		int cell;
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j * l.width();
				off[cell] = cur[cell] > 0 ? cur[cell] : 0;
				volume += off[cell];
			}
		}
	}

	public static boolean at( int cell ){
		return Dungeon.level != null && at( Dungeon.level, cell );
	}

	public static boolean at( Level level, int cell ){
		SproutGround s = (SproutGround) level.blobs.get( SproutGround.class );
		return s != null && s.cur != null && cell >= 0 && cell < s.cur.length && s.cur[cell] > 0;
	}

	public static boolean boosts( Char ch ){
		return MazeTower.N1_FLOORS && ch != null && !ch.flying && at( ch.pos );
	}

	//한 칸을 새잎 땅으로 (벽·물·구덩이 제외), 그 칸의 검은 낙엽은 지운다
	public static void grow( Level level, int cell ){
		if (cell < 0 || cell >= level.length() || level.solid[cell] || level.pit[cell] || level.water[cell]) return;
		Blob.seed( cell, 1, SproutGround.class, level );
		BlackLeaves b = (BlackLeaves) level.blobs.get( BlackLeaves.class );
		if (b != null && b.cur != null) b.clear( cell );
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		TileFxLayer layer = new TileFxLayer( this, BossEffects.Fx.SPROUT_GROUND, 0, 1f ).connect( BossEffects.Fx.SPROUT_GROUND_CONN ).varied();
		//Maze Tower (O2, 사용자): 25층에서 줄기 둘레로 퍼질 때는 이미 자란 새싹이 그대로 있고 새 칸만 더해지게 (이웃과 상관없는 모양 + 서서히 나타남)
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.MasterBossLevel) layer.solo().fadeIn( com.shatteredpixel.shatteredpixeldungeon.MazeTower.O_SPROUT_FADE );
		emitter.add( layer );
	}

	@Override
	public String tileDesc() {
		return Messages.get( this, "desc", MazeTower.N1_SPROUT_MANA );
	}

	//새잎 땅 위에 서 있는 동안 (아이콘만)
	public static class Fresh extends FlavourBuff {
		{ type = buffType.POSITIVE; announced = false; }
		@Override public int icon() { return BuffIndicator.RECHARGING; }
		@Override public void tintIcon( Image icon ) { icon.hardlight( 0.55f, 0.9f, 0.4f ); }
		@Override public float iconFadePercent() { return 0; }
		@Override public String desc() { return Messages.get( this, "desc", MazeTower.N1_SPROUT_MANA ); }
	}
}
