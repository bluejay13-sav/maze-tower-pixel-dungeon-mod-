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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.RatKing;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.sewerboss.SewerBossEntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.tiles.custom.Carpet;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

public class RatKingRoom extends SecretRoom {
	
	@Override
	public boolean canConnect(Room r) {
		//never connects at the entrance
		return !(r instanceof SewerBossEntranceRoom) && super.canConnect(r);
	}

	@Override
	public boolean canPlaceWater(Point p) {
		return false;
	}

	@Override
	public boolean canPlaceGrass(Point p) {
		return false;
	}

	@Override
	public boolean canPlaceTrap(Point p) {
		return false;
	}

	//force internal size to 5x5 for consistency of layout
	@Override
	public int minWidth() { return 7; }
	public int maxWidth() { return 7; }
	//Maze Tower (사용자): 현자의 서재는 안쪽 5×3 (벽 포함 7×5)
	public int minHeight() { return com.shatteredpixel.shatteredpixeldungeon.MazeTower.M_SAGE ? 5 : 7; }
	public int maxHeight() { return com.shatteredpixel.shatteredpixeldungeon.MazeTower.M_SAGE ? 5 : 7; }

	public void paint(Level level ) {
		if (com.shatteredpixel.shatteredpixeldungeon.MazeTower.M_SAGE){ paintSageStudy( level ); return; }

		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		Painter.fill( level, this, 2, Terrain.EMPTY_SP );
		
		Door entrance = entrance();
		entrance.set( Door.Type.HIDDEN );
		int door = level.pointToCell(entrance);

		int center = level.pointToCell(center());
		int w = level.width();

		int[] statuePositions = new int[]{
				center - 2 - 2*w,
				center - 2*w,
				center + 2 - 2*w,
				center + 2,
				center + 2 + 2*w,
				center + 2*w,
				center - 2 + 2*w,
				center -2,
		};

		for (int cell : statuePositions){
			if (level.distance(door, cell) >= 2) {
				Painter.set(level, cell, Terrain.CUSTOM_DECO);
			}
		}

		Carpet c = new Carpet();
		c.setRect(left+2, top+2, width()-4, height()-4);
		level.customTiles.add(c);

		RatKingRoomDeco deco = new RatKingRoomDeco();
		deco.setRect(left+1, top+1, width()-2, height()-2);
		level.customTiles.add(deco);

		RatKingStatues statues = new RatKingStatues();
		statues.setRect(left+1, top, width()-2, height());
		level.customTerrain.add(statues);

		StatueOverhang overhang = new StatueOverhang();
		overhang.setRect(left+1, top, width()-2, height());
		level.customWalls.add(overhang);

		//Maze Tower (M1): 쥐 왕 대신 숨어 사는 현자
		if (com.shatteredpixel.shatteredpixeldungeon.MazeTower.M_SAGE){
			com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sage sage = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sage();
			sage.pos = center;
			level.mobs.add( sage );
		} else {
		RatKing king = new RatKing();
		king.pos = center;
		level.mobs.add( king );
		}

		for (Point p : getPoints()){
			int cell = level.pointToCell(p);
			if (cell != center && (level.map[cell] == Terrain.EMPTY || level.map[cell] == Terrain.EMPTY_SP)){
				level.drop( new Gold( Random.IntRange( 5, 20 ) ), cell );
			}
		}
	}

	//Maze Tower (사용자): 숨은 마도시 학자의 서재 (안쪽 5×3, 가구 그림 maze_sage_room.png, 원본 docs/sprites/mobs/m_draft/make_sage_room.ps1).
	//방 가운데에 책상, 현자는 그 뒤(윗줄 가운데)에 선다. 윗줄: 단지·두루마리 책장 · (빈칸) · 현자 · 연금술 탁자 · 책장,
	//아랫줄: 시든 세계수 화분 · (빈칸) · (빈칸) · 두루마리 바구니 · 촛대. 비밀문이 윗벽에 나면 현자가 문을 막지 않게 위아래를 뒤집는다.
	//비밀문 바로 옆(거리 2 미만) 칸의 가구는 원작처럼 놓지 않는다.
	private void paintSageStudy( Level level ){
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		Painter.fill( level, this, 2, Terrain.EMPTY_SP );

		Door entrance = entrance();
		entrance.set( Door.Type.HIDDEN );
		int door = level.pointToCell(entrance);
		boolean flip = entrance.y == top;

		int center = level.pointToCell(center());
		int w = level.width();
		for (int dy = -1; dy <= 1; dy++){
			for (int dx = -2; dx <= 2; dx++){
				if (sageFurniture( dx, dy, flip ) < 0) continue;
				int cell = center + dx + dy*w;
				if (dx == 0 && dy == 0 || level.distance(door, cell) >= 2) {
					Painter.set(level, cell, Terrain.CUSTOM_DECO);
				}
			}
		}

		Carpet c = new Carpet();
		c.setRect(left+2, top+2, width()-4, height()-4);
		level.customTiles.add(c);

		RatKingRoomDeco deco = new RatKingRoomDeco();
		deco.setRect(left+1, top+1, width()-2, height()-2);
		deco.sageFlip = flip;
		level.customTiles.add(deco);

		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sage sage = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sage();
		sage.pos = center + (flip ? w : -w);
		sage.mazeFaceLeft = entrance.x < left + width()/2; //(사용자) 방 배치에 맞게 비밀문이 있는 쪽을 바라본다 (문이 가운데 위·아래면 오른쪽)
		level.mobs.add( sage );
		//(사용자) 현자의 방에는 금화가 나오지 않는다
	}

	//서재 가구 번호 (가운데에서 dx -2~2, dy -1~1, 없으면 -1): 0 책장, 1 단지·두루마리 책장, 2 책상, 3 연금술 탁자, 4 시든 세계수 화분, 5 두루마리 바구니, 6 촛대
	public static int sageFurniture( int dx, int dy, boolean flip ){
		if (flip) dy = -dy;
		if (dy == 0) return dx == 0 ? 2 : -1;
		if (dy < 0){
			switch (dx){ case -2: return 1; case 1: return 3; case 2: return 0; default: return -1; }
		}
		switch (dx){ case -2: return 4; case 1: return 5; case 2: return 6; default: return -1; }
	}

	//Maze Tower: 쥐 왕의 방을 왕자의 방으로 (maze_prince_room.png: 0 침대, 1 침대 2, 2·3 책상, 4 의자, 5 책, 6 상자, 7 등불).
	//가운데(쥐 왕 자리)는 침대, 석상 자리 8곳에는 가구를 놓는다. 석상의 솟은 부분·윗머리는 그리지 않는다.
	public static final boolean MAZE_PRINCE = true;

	//석상 자리(가운데에서 dx, dy = -2·0·2 칸)에 놓을 가구 번호
	public static int princeFurniture( int dx, int dy ){
		if (dx != 0 && dy != 0) return 7; //네 귀퉁이: 등불
		if (dy < 0) return 5;               //위: 책
		if (dx > 0) return 6;               //오른쪽: 상자
		if (dy > 0) return 4;               //아래: 의자
		return 1;                           //왼쪽: 침대 2
	}

	public static class RatKingRoomDeco extends CustomTilemap {

		{
			texture = MAZE_PRINCE ? Assets.Environment.MAZE_PRINCE_ROOM : Assets.Environment.RAT_KING_ROOM; //Maze Tower
		}

		//Maze Tower (사용자): 현자의 서재 (안쪽 5×3이라 tileH 3). 비밀문이 윗벽이면 위아래를 뒤집은 배치 (저장함)
		public boolean sageFlip = false;

		private boolean sageStudy(){
			return com.shatteredpixel.shatteredpixeldungeon.MazeTower.M_SAGE && tileH == 3;
		}

		private int sageAt( int tx, int ty ){
			return sageFurniture( tx - 2, ty - 1, sageFlip );
		}

		private static final String SAGE_FLIP = "maze_sage_flip";

		@Override
		public void storeInBundle( com.watabou.utils.Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( SAGE_FLIP, sageFlip );
		}

		@Override
		public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
			super.restoreFromBundle( bundle );
			sageFlip = bundle.getBoolean( SAGE_FLIP );
		}

		@Override
		public Tilemap create() {
			if (sageStudy()) texture = Assets.Environment.MAZE_SAGE_ROOM;
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			int i = 0;
			if (sageStudy()){
				for (int y = 0; y < tileH; y++){
					int cell = tileX + (tileY+y)*Dungeon.level.width();
					for (int x = 0; x < tileW; x++){
						data[i++] = Dungeon.level.map[cell++] == Terrain.CUSTOM_DECO ? sageAt( x, y ) : -1;
					}
				}
				v.map( data, tileW );
				return v;
			}
			for (int y = 0; y < tileH; y++){
				int cell = tileX + (tileY+y)*Dungeon.level.width();
				for (int x = 0; x < tileW; x++){
					if (MAZE_PRINCE && x == 2 && y == 2){
						data[i] = 0; //Maze Tower: 가운데 침대
					} else if (MAZE_PRINCE && Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
						data[i] = princeFurniture(x - 2, y - 2); //Maze Tower: 가구
					} else 					if (x == 2 && y == 2){
						//center, pillow
						data[i] = 3;
					} else if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
						//statue
						data[i] = 0;
					} else {
						data[i] = -1;
					}
					cell++;
					i++;
				}
			}
			v.map( data, tileW );
			return v;
		}

		@Override
		public Image image(int tileX, int tileY) {
			int cell = this.tileX+tileX + (this.tileY + tileY)*Dungeon.level.width();
			if (sageStudy() && Dungeon.level.map[cell] == Terrain.CUSTOM_DECO && sageAt(tileX, tileY) >= 0){ //Maze Tower: 서재 가구 살펴보기 그림
				Image img = new Image(Assets.Environment.MAZE_SAGE_ROOM);
				img.frame(sageAt(tileX, tileY) * 16, 0, 16, 16);
				return img;
			}
			if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
				//custom visual for rat king statue examine
				//TODO should make a method for this if we have to do it with any frequency
				Image img = new Image(texture);
				if (MAZE_PRINCE){ //Maze Tower: 살펴보기 그림도 그 자리의 가구
					img.frame(princeFurniture(tileX - 2, tileY - 2) * 16, 0, 16, 16);
					return img;
				}
				img.frame(64, 0, 16, 16);
				return img;
			} else {
				return super.image(tileX, tileY);
			}
		}

		@Override
		public String name(int tileX, int tileY) {
			int cell = this.tileX+tileX + (this.tileY + tileY)*Dungeon.level.width();
			if (sageStudy()){ //Maze Tower: 서재 가구 이름
				if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO && sageAt(tileX, tileY) >= 0) return Messages.get(this, "sage_name_" + sageAt(tileX, tileY));
				return super.name(tileX, tileY);
			}
			if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
				return Messages.get(this, "statue_name");
			} else if (tileX == 2 && tileY == 2){
				return Messages.get(this, "pillow_name");
			} else {
				return super.name(tileX, tileY);
			}
		}

		@Override
		public String desc(int tileX, int tileY) {
			int cell = this.tileX+tileX + (this.tileY + tileY)*Dungeon.level.width();
			if (sageStudy()){ //Maze Tower: 서재 가구 설명
				if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO && sageAt(tileX, tileY) >= 0) return Messages.get(this, "sage_desc_" + sageAt(tileX, tileY));
				return super.desc(tileX, tileY);
			}
			if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
				return Messages.get(this, "statue_desc");
			} else if (tileX == 2 && tileY == 2){
				return Messages.get(this, "pillow_desc");
			} else {
				return super.desc(tileX, tileY);
			}
		}
	}

	public static class RatKingStatues extends CustomTilemap {

		{
			texture = Assets.Environment.RAT_KING_ROOM;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			int i = 0;
			for (int y = 0; y < tileH; y++){
				int cell = tileX + (tileY+y)*Dungeon.level.width();
				for (int x = 0; x < tileW; x++){
					if (MAZE_PRINCE){
						data[i] = -1; //Maze Tower: 가구는 솟은 부분이 없다
					} else 					if (Dungeon.level.map[cell] == Terrain.CUSTOM_DECO){
						//statue
						data[i] = 1;
					} else {
						data[i] = -1;
					}
					cell++;
					i++;
				}
			}
			v.map( data, tileW );
			return v;
		}

	}

	public static class StatueOverhang extends CustomTilemap {

		{
			texture = Assets.Environment.RAT_KING_ROOM;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			int i = 0;
			for (int y = 0; y < tileH; y++){
				int cell = tileX + (tileY+y)*Dungeon.level.width();
				for (int x = 0; x < tileW; x++){
					if (MAZE_PRINCE){
						data[i] = -1; //Maze Tower: 가구는 윗머리가 없다
					} else 					if (Dungeon.level.map[cell + Dungeon.level.width()] == Terrain.CUSTOM_DECO){
						//statue overhang
						data[i] = 2;
					} else {
						data[i] = -1;
					}
					cell++;
					i++;
				}
			}
			v.map( data, tileW );
			return v;
		}

	}
}
