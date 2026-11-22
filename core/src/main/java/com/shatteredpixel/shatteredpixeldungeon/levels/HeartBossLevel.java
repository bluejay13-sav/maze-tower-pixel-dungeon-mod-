/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.HeartBeast;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Maze Tower: 25층 최종보스(심장을 진 짐승) 보스방. 원작 요그-드제와 방(HallsBossLevel)을 대신한다 (MazeBosses.ENABLED일 때).
//안쪽 17×13 직사각형 방(B25_ROOM_W/H), 아래쪽 문 → 짧은 통로 → 입구. 윗벽 가운데가 출구(보스를 쓰러뜨리면 열림).
//주인공이 방 안으로 들어서면 문이 닫히고 짐승이 방 가운데에 나온다.
public class HeartBossLevel extends Level {

	{
		color1 = 0x801500;
		color2 = 0xa68521;
		//(결정, 2026-11-22) 25층 보스 맵은 전체가 밝혀져 있다 (updateFieldOfView)
	}

	//테두리 1칸 + 벽 1칸 + 방 + 벽 + 통로 4칸 + 테두리
	public static final int ROOM_L = 2, ROOM_T = 2;
	public static final int ROOM_R = ROOM_L + MazeTower.B25_ROOM_W - 1;
	public static final int ROOM_B = ROOM_T + MazeTower.B25_ROOM_H - 1;
	private static final int WIDTH  = MazeTower.B25_ROOM_W + 4;
	private static final int HEIGHT = MazeTower.B25_ROOM_H + 4 + 4;
	private static final int MID_X  = ROOM_L + MazeTower.B25_ROOM_W / 2;

	public int doorCell(){ return MID_X + (ROOM_B + 1) * width(); }
	public int bossCell(){ return MID_X + (ROOM_T + MazeTower.B25_ROOM_H / 2) * width(); }

	public boolean inRoom( int cell ){
		int x = cell % width(), y = cell / width();
		return x >= ROOM_L && x <= ROOM_R && y >= ROOM_T && y <= ROOM_B;
	}

	@Override
	public void playLevelMusic() {
		if (locked && BossHealthBar.isAssigned()){
			Music.INSTANCE.play(BossHealthBar.isBleeding() ? Assets.Music.HALLS_BOSS_FINALE : Assets.Music.HALLS_BOSS, true);
		} else if (map[exit()] != Terrain.EXIT || Statistics.amuletObtained){
			Music.INSTANCE.end();
		} else {
			Music.INSTANCE.playTracks(HallsLevel.HALLS_TRACK_LIST, HallsLevel.HALLS_TRACK_CHANCES, false);
		}
	}

	@Override
	public String tilesTex() { return Assets.Environment.TILES_HALLS; }

	@Override
	public String waterTex() { return Assets.Environment.WATER_HALLS; }

	@Override
	protected boolean build() {
		setSize(WIDTH, HEIGHT);
		Painter.fill(this, 0, 0, WIDTH, HEIGHT, Terrain.WALL);
		Painter.fill(this, ROOM_L, ROOM_T, MazeTower.B25_ROOM_W, MazeTower.B25_ROOM_H, Terrain.EMPTY);
		//통로 (가로 3칸)
		Painter.fill(this, MID_X - 1, ROOM_B + 2, 3, 3, Terrain.EMPTY);
		map[doorCell()] = Terrain.EMPTY;

		int entrance = MID_X + (ROOM_B + 4) * width();
		map[entrance] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this, entrance, LevelTransition.Type.REGULAR_ENTRANCE));

		int exitCell = MID_X + (ROOM_T - 1) * width();
		map[exitCell] = Terrain.WALL_DECO; //보스를 쓰러뜨리기 전에는 막힌 벽
		LevelTransition exit = new LevelTransition(this, exitCell, LevelTransition.Type.REGULAR_EXIT);
		transitions.add(exit);

		//바닥 장식 조금
		for (int i = 0; i < length(); i++){
			if (map[i] == Terrain.EMPTY && Random.Int(6) == 0) map[i] = Terrain.EMPTY_DECO;
		}
		return true;
	}

	//(결정, 2026-11-22) 25층은 보스 맵 전체가 늘 밝혀져 있다 (영웅이 모든 칸을 본다)
	@Override
	public void updateFieldOfView( Char c, boolean[] fieldOfView ) {
		super.updateFieldOfView(c, fieldOfView);
		if (c == Dungeon.hero){
			java.util.Arrays.fill(fieldOfView, true);
		}
	}

	@Override
	protected void createMobs() {}

	@Override
	public Actor addRespawner() { return null; }

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int pos;
				do {
					pos = randomRespawnCell(null);
				} while (pos == entrance());
				for (Item i : bonesItems) {
					drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = entrance() + i;
			if (passable[cell] && Actor.findChar(cell) == null) candidates.add(cell);
		}
		return candidates.isEmpty() ? -1 : Random.element(candidates);
	}

	@Override
	public void occupyCell( Char ch ) {
		if (!locked && map[exit()] != Terrain.EXIT && ch == Dungeon.hero && inRoom(ch.pos)) {
			seal();
		}
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MimicHero.onOccupy(ch); //사냥꾼 의태의 혈관 덫
		super.occupyCell( ch );
	}

	@Override
	public void seal() {
		super.seal();
		Statistics.qualifiedForBossChallengeBadge = true;
		set( doorCell(), Terrain.WALL );
		GameScene.updateMap( doorCell() );
		CellEmitter.get( doorCell() ).burst( ShadowParticle.UP, 15 );
		Dungeon.observe();

		HeartBeast boss = new HeartBeast();
		boss.pos = bossCell();
		Char there = Actor.findChar(boss.pos);
		if (there != null){
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8){
				if (Actor.findChar(boss.pos + i) == null && passable[boss.pos + i]) candidates.add(boss.pos + i);
			}
			if (!candidates.isEmpty()){
				int to = Random.element(candidates);
				Actor.add(new Pushing(there, there.pos, to));
				there.pos = to;
			}
		}
		GameScene.add( boss );
		boss.beginFight();
	}

	@Override
	public void unseal() {
		super.unseal();
		set( doorCell(), Terrain.EMPTY );
		GameScene.updateMap( doorCell() );
		set( exit(), Terrain.EXIT );
		GameScene.updateMap( exit() );
		CellEmitter.get(exit()).burst(ShadowParticle.UP, 60);
		Dungeon.observe();
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut(5f, new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.play(Assets.Music.THEME_FINALE, true);
					}
				});
			}
		});
	}

	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.REGULAR_ENTRANCE
				&& hero.belongings.getItem(Amulet.class) != null
				&& hero.buff(AscensionChallenge.class) == null) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndOptions( new ItemSprite(ItemSpriteSheet.AMULET),
							Messages.get(Amulet.class, "ascent_title"),
							Messages.get(Amulet.class, "ascent_desc"),
							Messages.get(Amulet.class, "ascent_yes"),
							Messages.get(Amulet.class, "ascent_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0){
								Buff.affect(hero, AscensionChallenge.class);
								Statistics.highestAscent = 25;
								HeartBossLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;
		}
		return super.activateTransition(hero, transition);
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.WATER: return Messages.get(HallsLevel.class, "water_name");
			default: return super.tileName( tile );
		}
	}

	@Override
	public Group addVisuals () {
		super.addVisuals();
		HallsLevel.addHallsVisuals( this, visuals );
		return visuals;
	}
}
