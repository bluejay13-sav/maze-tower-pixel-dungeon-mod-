/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;

public class WndChooseSubclass extends Window {
	
	private static final int WIDTH		= 130;
	private static final float GAP		= 2;
	
	public WndChooseSubclass(final TengusMask tome, final Hero hero ) {
		
		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( tome.image(), null ) );
		titlebar.label( tome.name() );
		titlebar.setRect( 0, 0, WIDTH-16, 0 );
		add( titlebar );

		IconButton random = new IconButton(Icons.SHUFFLE.get()){
			@Override
			protected void onClick() {
				super.onClick();
				GameScene.show(new WndOptions(Icons.SHUFFLE.get(),
						Messages.get(WndChooseSubclass.class, "random_title"),
						Messages.get(WndChooseSubclass.class, "random_sure"),
						Messages.get(WndChooseSubclass.class, "yes"),
						Messages.get(WndChooseSubclass.class, "no")){
					@Override
					protected void onSelect(int index) {
						super.onSelect(index);
						if (index == 0){
							WndChooseSubclass.this.hide();
							HeroSubClass cls = Random.oneOf(paths(hero)); //Maze Tower
							tome.choose(cls);
							GameScene.show(new WndInfoSubclass(hero.heroClass, cls));
						}
					}
				});
			}

			@Override
			public void update() {
				if (Statistics.qualifiedForRandomVictoryBadge){
					icon.tint(1, 1, 1, (float)Math.abs(Math.cos(1.5f*Math.PI* Game.timeTotal)/2f));
				}
				super.update();
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndChooseSubclass.class, "random_title");
			}
		};
		random.setRect(WIDTH-16, 0, 16, 16);
		add(random);

		RenderedTextBlock message = PixelScene.renderTextBlock( 6 );
		message.text( Messages.get(this, MazeTower.ORIGINAL_SUBCLASS ? "message" : "maze_message"), WIDTH ); //Maze Tower
		message.setPos( titlebar.left(), titlebar.bottom() + GAP );
		add( message );

		float pos = message.bottom() + 3*GAP;

		//Maze Tower (S1): 3차가 넷까지 늘어 화면에 다 들어가지 않으면, 고르기 단추들을 스크롤 목록에 넣는다
		final java.util.ArrayList<SubclassButton> btns = new java.util.ArrayList<>();
		final java.util.ArrayList<InfoButton> infos = new java.util.ArrayList<>();
		com.watabou.noosa.ui.Component list = new com.watabou.noosa.ui.Component();
		float lp = 0;
		for (HeroSubClass subCls : paths(hero)){ //Maze Tower
			SubclassButton btnCls = new SubclassButton( tome, subCls );
			btnCls.leftJustify = true;
			btnCls.multiline = true;
			btnCls.setSize(WIDTH-20, btnCls.reqHeight()+2);
			btnCls.setRect( 0, lp, WIDTH-20, btnCls.reqHeight()+2);
			list.add( btnCls );
			btns.add( btnCls );

			InfoButton clsInfo = new InfoButton( subCls );
			clsInfo.setRect(WIDTH-20, btnCls.top() + (btnCls.height()-20)/2, 20, 20);
			list.add(clsInfo);
			infos.add( clsInfo );

			lp = btnCls.bottom() + GAP;
		}
		float listH = lp;
		float avail = PixelScene.uiCamera.height * 0.9f - pos - 18 - 2*GAP - 12;
		if (listH <= avail){
			//다 들어가면 원작처럼 창에 바로 놓는다
			for (SubclassButton b : btns){ b.setPos( b.left(), b.top() + pos ); add( b ); }
			for (InfoButton b : infos){ b.setPos( b.left(), b.top() + pos ); add( b ); }
			pos += listH;
		} else {
			list.setSize( WIDTH, listH );
			com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane pane = new com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane( list ){
				@Override
				public void onClick( float x, float y ) {
					for (InfoButton b : infos) if (b.inside( x, y )) { b.click(); return; }
					for (SubclassButton b : btns) if (b.inside( x, y )) { b.click(); return; }
				}
			};
			add( pane );
			pane.setRect( 0, pos, WIDTH, avail );
			pos += avail + GAP;
		}

		RedButton btnCancel = new RedButton( Messages.get(this, "cancel") ) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnCancel.setRect( 0, pos, WIDTH, 18 );
		add( btnCancel );
		
		resize( WIDTH, (int)btnCancel.bottom() );
	}

	//Maze Tower (S1): 3차 고르기 단추 (스크롤 목록 안에서도 누를 수 있게 click()을 열어 둔다)
	private class SubclassButton extends RedButton {
		private final TengusMask tome;
		private final HeroSubClass subCls;
		SubclassButton( TengusMask tome, HeroSubClass subCls ){
			super( subCls.shortDesc(), 6 );
			this.tome = tome;
			this.subCls = subCls;
		}
		void click(){ onClick(); }
		@Override
		protected void onClick() {
			GameScene.show(new WndOptions(new HeroIcon(subCls),
					Messages.titleCase(subCls.title()),
					Messages.get(WndChooseSubclass.this, "are_you_sure"),
					Messages.get(WndChooseSubclass.this, "yes"),
					Messages.get(WndChooseSubclass.this, "no")){
				@Override
				protected void onSelect(int index) {
					hide();
					if (index == 0 && WndChooseSubclass.this.parent != null){
						WndChooseSubclass.this.hide();
						tome.choose( subCls );
						Statistics.qualifiedForRandomVictoryBadge = false;
					}
				}
			});
		}
	}

	private static class InfoButton extends IconButton {
		private final HeroSubClass subCls;
		InfoButton( HeroSubClass subCls ){
			super( Icons.get(Icons.INFO) );
			this.subCls = subCls;
		}
		void click(){ onClick(); }
		@Override
		protected void onClick() {
			GameScene.show(new WndInfoSubclass(Dungeon.hero.heroClass, subCls));
		}
	}

	//Maze Tower: 만개의 핵으로 고를 수 있는 3차 전직만 보인다
	private static HeroSubClass[] paths(Hero hero){
		return MazeTower.ORIGINAL_SUBCLASS ? hero.heroClass.subClasses() : MazeTower.fullBloomPaths(hero.heroClass);
	}
}
