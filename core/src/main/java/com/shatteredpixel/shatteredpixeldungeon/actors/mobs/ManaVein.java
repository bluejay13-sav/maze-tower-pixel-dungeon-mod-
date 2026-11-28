/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ManaVapor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.BossEffects;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//Maze Tower: 광맥 (15층 뿌리를 갉는 것 보스방의 광맥 기둥, 11~14층 살아 있는 광맥의 바탕). 부서지면 마력 증기를 뿜는다. 마력 증기에는 다치지 않는다.
//주인공도 때려서 부술 수 있다. 15층 보스전 중에는 깨질 때마다 다른 자리에 하나씩 다시 자란다(GnawerBossLevel.onVeinBroken).
//그림: core/src/main/assets/sprites/maze_p15_vein.png (17x17 x 4, 바위 속 맥동하는 푸른 마석 줄기, 원본 docs/sprites/props/p15_vein.png ← docs/sprites/mobs/j_draft/make_vein.ps1)
public class ManaVein extends Mob {

	{
		spriteClass = VeinSprite.class;

		HP = HT = MazeTower.VEIN_HP;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		state = PASSIVE;
		alignment = Alignment.NEUTRAL;

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		properties.add(Property.STATIC);

		immunities.add(ManaVapor.class); //(사용자) 광맥은 마력 증기에 다치지 않는다
		immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SpitVapor.class);
	}

	public boolean mazeEaten = false; //Maze Tower (J8): 새끼 굴벌레가 다 갉아먹음 (증기 없이 사라짐)
	public int gnawStage = 0;         //Maze Tower (J8): 굴벌레에게 갉힌 단계 0~3 (그림이 위 오른쪽부터 뜯겨 나감, 저장됨)

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( "maze_gnaw_stage", gnawStage );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		gnawStage = bundle.getInt( "maze_gnaw_stage" );
	}

	@Override
	protected boolean act() {
		if (sprite != null){
			sprite.hideAlert();
			sprite.hideLost();
		}
		spend(TICK);
		return true;
	}

	@Override public boolean heroShouldInteract() { return false; }
	@Override public int damageRoll() { return 0; }
	@Override public int attackSkill( Char target ) { return 0; }
	@Override public int defenseSkill( Char enemy ) { return 0; }
	@Override public int drRoll() { return 0; }
	@Override public boolean add( Buff buff ) { return false; }
	@Override public void beckon( int cell ) { }
	@Override public boolean reset() { return true; }

	@Override
	public void die( Object cause ) {
		//Maze Tower (J8): 새끼 굴벌레가 다 갉아먹은 광맥은 증기 없이 부스러진다
		if (mazeEaten){
			if (Dungeon.level.heroFOV[pos]) com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(pos).burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.ROCK), 4);
			//Maze Tower (K4): 15층 벌레가 다 먹은 광맥도 다른 자리에 하나 다시 자란다
			if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel) ((com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel) Dungeon.level).onVeinBroken(pos);
			super.die(cause);
			return;
		}
		//마력 증기 분출 (그 칸에서 원작 독가스처럼 퍼진다)
		GameScene.add(Blob.seed(pos, MazeTower.VAPOR_AMOUNT, ManaVapor.class));
		//Maze Tower (K1, 사용자): 15층 보스전 중에는 광맥이 깨질 때마다 다른 자리에 하나씩 다시 자란다
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel) ((com.shatteredpixel.shatteredpixeldungeon.levels.GnawerBossLevel) Dungeon.level).onVeinBroken(pos);
		if (Dungeon.level.heroFOV[pos]){
			BossEffects.atCellBottom(BossEffects.Fx.VEIN_BREAK, pos, 10, false);
			Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		}
		super.die(cause);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", HP, HT);
	}

	public static class VeinSprite extends MobSprite {
		public VeinSprite(){
			super();
			texture("sprites/maze_p15_vein.png");
			TextureFilm f = new TextureFilm(texture, 17, 17);
			//Maze Tower: 바위 속 푸른 마석 줄기 (한 칸, (사용자) 주변 빛무리 없이 틈 사이로만 은은히 새는 빛, 줄기 몇 칸만 반 단계 밝아지며 초당 1칸으로 옮겨 감 — 4칸. 빛무리는 VEIN_GLOW로 꺼 둠)
			this.film = f;
			idle = stageAnim(0);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			//부서지면 곧바로 빠르게 사라진다 (MazeTower.PROP_FADE_TIME)
			die = new Animation(20, false);
			die.frames(f, 0);
			play(idle);
		}

		@Override
		protected float fadeTime() {
			return com.shatteredpixel.shatteredpixeldungeon.MazeTower.PROP_FADE_TIME;
		}

		//Maze Tower: 광맥에서 은은히 새어 나오는 옅은 푸른 빛무리 (원작 감옥 횃불의 Halo와 같은 방식, 광맥 뒤에 그림)
		private com.watabou.noosa.Halo glow;

		//그림 칸: 갉힌 단계(0~3)마다 4칸씩. (사용자) 초당 1칸: 줄기 몇 칸만 반 단계 밝아지며 아주 천천히 옮겨 감
		//Maze Tower: 땅에 박힌 지형 같은 몹이라 원작처럼 무작위 방향으로 뒤집지 않는다 (놓인 뒤 좌우가 한 번 바뀌어 보이던 것)
		@Override
		public void turnTo( int from, int to ) { flipHorizontal = false; }

		private TextureFilm film;
		private int shownStage = 0;

		private Animation stageAnim( int stage ){
			Animation a = new Animation(1, true);
			int o = stage * 4;
			a.frames(film, o, o + 1, o + 2, o + 3);
			return a;
		}

		//Maze Tower (K4): 15층 벌레가 물어뜯을 때 광맥이 좌우로 한 번 떨린다
		private float joltT = 0;
		public void jolt(){ joltT = 0.3f; }

		@Override
		public void update() {
			super.update();
			if (joltT > 0 && ch != null){
				joltT = Math.max(0, joltT - com.watabou.noosa.Game.elapsed);
				com.watabou.utils.PointF p = worldToCamera(ch.pos);
				x = p.x + (joltT > 0 ? ((int)(joltT * 20) % 2 == 0 ? 1 : -1) : 0);
			}
			//Maze Tower (J8): 굴벌레에게 갉힌 단계가 바뀌면 그 단계 그림으로
			if (ch instanceof ManaVein && ch.isAlive()){
				int st = Math.max(0, Math.min(3, ((ManaVein) ch).gnawStage));
				if (st != shownStage){
					shownStage = st;
					idle = stageAnim(st);
					play(idle);
				}
			}
			if (com.shatteredpixel.shatteredpixeldungeon.MazeTower.VEIN_GLOW && glow == null && parent != null && ch != null && ch.isAlive()){
				glow = new com.watabou.noosa.Halo(com.shatteredpixel.shatteredpixeldungeon.MazeTower.VEIN_GLOW_RADIUS, 0x5AB4FF, com.shatteredpixel.shatteredpixeldungeon.MazeTower.VEIN_GLOW_BRIGHT);
				parent.addToBack(glow);
			}
			if (glow != null){
				com.watabou.utils.PointF c = center();
				glow.point(c.x, c.y + 2);
				glow.visible = visible;
			}
		}

		@Override
		public void die() {
			if (film != null){ die = new Animation(20, false); die.frames(film, shownStage * 4); } //갉히던 모습 그대로 사라진다
			if (glow != null){ glow.killAndErase(); glow = null; }
			super.die();
		}

		@Override
		public void kill() {
			if (glow != null){ glow.killAndErase(); glow = null; }
			super.kill();
		}
	}
}
