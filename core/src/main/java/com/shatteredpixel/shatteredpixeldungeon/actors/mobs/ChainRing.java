/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PointF;

//Maze Tower (H6): 사슬 고리 (갇힌 영혼을 묶은 벽의 고리, 고정 몹).
//벽 바로 앞 칸에 놓이지만 벽면에 박힌 것처럼 그린다. 체력 H6_RING_HP, 반드시 맞는다.
//칠 때마다 쇠 소리가 나서 둘레 H6_NOISE_RANGE칸 안의 적이 그 자리로 모인다. 부서지면 영혼이 풀려난다. 경험치·전리품 없음.
public class ChainRing extends Mob {

	{
		spriteClass = RingSprite.class;

		HP = HT = MazeTower.H6_RING_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);
	}

	public int soulId = -1;
	//Maze Tower (I1): 벽면에 박힌 고리(true)인지, 바닥에 박힌 고리(false, 10층 대봉인실의 철창 앞)인지
	public boolean raised = true;

	@Override
	public int defenseSkill( Char enemy ) {
		return 0; //반드시 맞는다
	}

	//주인공이 누르면 원작처럼 평타로 친다
	@Override
	public boolean heroShouldInteract() {
		return false;
	}

	@Override
	protected boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage( dmg, src );
		//쇠 소리: 둘레의 적이 이 자리로 모인다
		Sample.INSTANCE.play( Assets.Sounds.CHAINS, 0.8f, 0.9f );
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Alignment.ENEMY && m != this && m.state != m.PASSIVE
					&& Dungeon.level.distance(m.pos, pos) <= MazeTower.H6_NOISE_RANGE){
				m.beckon(pos);
			}
		}
		if (isAlive() && sprite != null && sprite.visible) sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.WARNING, Messages.get(this, "clang"));
		if (isAlive() && sprite instanceof RingSprite) ((RingSprite) sprite).swing();
	}

	//(I7) 10층 대봉인실의 고리는 선고가 걸린 철창의 것만 끊을 수 있다
	@Override
	public boolean isInvulnerable( Class effect ) {
		if (!raised && Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel){
			return !((com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) Dungeon.level).sentenceRing(this);
		}
		return super.isInvulnerable(effect);
	}

	@Override
	public String description() {
		if (!raised && Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel){
			return Messages.get(this, ((com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) Dungeon.level).sentenceRing(this) ? "desc_sentence" : "desc_arena");
		}
		return super.description();
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		if (!raised && Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel){
			((com.shatteredpixel.shatteredpixeldungeon.levels.WardenBossLevel) Dungeon.level).onArenaRingBroken(this);
			return;
		}
		Actor a = Actor.findById(soulId);
		if (a instanceof ChainedSoul) ((ChainedSoul) a).free();
	}

	@Override
	public void rollToDropLoot() {
		//떨어뜨리는 것 없음
	}

	private static final String SOUL = "soul_id", RAISED = "raised";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(SOUL, soulId);
		bundle.put(RAISED, raised);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		soulId = bundle.getInt(SOUL);
		if (bundle.contains(RAISED)) raised = bundle.getBoolean(RAISED);
	}

	//벽면의 쇠고리 (16×16: 0 가만히, 1 맞아서 흔들림, 2~4 끊어짐). 바닥 칸보다 위(벽면)에 그린다
	public static class RingSprite extends MobSprite {

		private Animation hit;

		public RingSprite(){
			super();
			texture( Assets.Sprites.CHAIN_RING );
			TextureFilm frames = new TextureFilm( texture, 16, 16 );
			idle = new Animation( 1, true );
			idle.frames( frames, 0 );
			run = idle.clone();
			attack = idle.clone();
			hit = new Animation( 12, false );
			hit.frames( frames, 1, 0, 1, 0 );
			die = new Animation( 10, false );
			die.frames( frames, 2, 3, 4 );
			play( idle );
		}

		public void swing(){
			play( hit );
		}

		@Override
		public void onComplete( Animation anim ) {
			if (anim == hit){ idle(); return; }
			super.onComplete(anim);
		}

		//벽면에 박힌 것처럼 칸보다 위로 올려 그린다
		@Override
		public PointF worldToCamera( int cell ) {
			PointF p = super.worldToCamera(cell);
			if (!(ch instanceof ChainRing) || ((ChainRing) ch).raised) p.y -= MazeTower.H6_RING_RAISE;
			return p;
		}

		@Override
		public int blood() {
			return 0xFF8A8A96;
		}
	}
}
