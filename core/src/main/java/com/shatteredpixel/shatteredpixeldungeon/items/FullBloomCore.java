/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloomPetalParticle;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MazeItemSprites;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChooseSubclass;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;

//Maze Tower: 만개의 핵 (3차 전직). 10층 보스(텐구)가 떨어뜨린다.
//사용하면 원작 텐구의 가면처럼 서브클래스 선택 창이 열린다. 지금은 전사 검투사 하나만 보인다.
public class FullBloomCore extends TengusMask {

	private static final String AC_USE = "WEAR"; //원작 텐구의 가면 행동을 그대로 쓴다

	{
		image = MazeItemSprites.FULLBLOOM_CORE;
	}

	//바닥(또는 가방)에 있을 때 벚꽃잎이 나선을 따라 나무를 휘감는다
	@Override
	public Emitter emitter() {
		return BloomPetalParticle.emitter();
	}

	@Override
	public void execute( Hero hero, String action ) {
		if (action.equals( AC_USE )) {
			if (hero.subClass != HeroSubClass.NONE){
				GLog.w(Messages.get(this, "already"));
				return;
			}
			if (MazeTower.fullBloomPaths(hero.heroClass).length == 0){
				GLog.w(Messages.get(this, "not_ready"));
				return;
			}
			curUser = hero;
			GameScene.show( new WndChooseSubclass( this, hero ) );
			return;
		}
		super.execute( hero, action );
	}

	@Override
	public void choose( HeroSubClass way ) {

		detach( curUser.belongings.backpack );
		Catalog.countUse( getClass() );

		curUser.spend( Actor.TICK );
		curUser.busy();

		curUser.subClass = way;
		Talent.initSubclassTalents(curUser);
		//만개 각성: 분홍 꽃잎 (2차 특성을 각성시킨다)
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.BloomTree.get(curUser).petalsGranted += MazeTower.BLOOM_PETALS;

		curUser.sprite.operate( curUser.pos );
		Sample.INSTANCE.play( Assets.Sounds.MASTERY );

		//(결정, 2026-11-21) 꽃이 피는 연출은 삭제 (특성 창에 가려 보이지 않음)

		GLog.p( Messages.get(this, "used", Messages.titleCase(way.title())) );
		StatusPane.talentBlink = 10f;
		WndHero.lastIdx = 1;
	}
}
