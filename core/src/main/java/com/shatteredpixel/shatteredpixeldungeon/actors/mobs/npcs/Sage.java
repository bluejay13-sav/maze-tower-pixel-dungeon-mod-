/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.MazeSage;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SageQuest;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SageSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;

import java.util.ArrayList;

//Maze Tower (M1): 숨어 사는 현자. 5층 보스층 비밀방(원작 쥐 왕의 방 자리)에 산다. 마도시의 현자였다.
//공격할 수 없다(피해·상태이상을 받지 않음). 20층 보스를 넘긴 뒤에는 말을 걸어도 아무 일도 없다(보상은 나중에).
//말을 걸면 망령 왕과 마도시의 여제 중 누구에게 브로치를 전할지 고르게 한다(이미 20층 보스가 고정됐으면 그쪽만, MazeSage).
//대사는 마지막 글 작업 때 다듬는다.
public class Sage extends NPC {

	{
		spriteClass = SageSprite.class;
		properties.add( Property.IMMOVABLE );
	}

	@Override
	public int defenseSkill( Char enemy ) { return INFINITE_EVASION; }

	@Override
	protected Char chooseEnemy() { return null; }

	@Override
	public void damage( int dmg, Object src ) { }

	@Override
	public boolean add( Buff buff ) { return false; }

	@Override
	public boolean reset() { return true; }

	//Maze Tower (사용자): 서재에서 평소 바라보는 쪽 (비밀문이 있는 쪽, 방 배치에 맞춤). 주인공이 보이면 주인공 쪽을 본다.
	public boolean mazeFaceLeft = false;
	private static final String FACE_LEFT = "maze_face_left";

	@Override
	public void storeInBundle( com.watabou.utils.Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( FACE_LEFT, mazeFaceLeft );
	}

	@Override
	public void restoreFromBundle( com.watabou.utils.Bundle bundle ) {
		super.restoreFromBundle( bundle );
		mazeFaceLeft = bundle.getBoolean( FACE_LEFT );
	}

	@Override
	public com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite sprite() {
		com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite s = super.sprite();
		s.flipHorizontal = mazeFaceLeft;
		return s;
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero != null && Dungeon.level.heroFOV[pos] && sprite != null) sprite.turnTo( pos, Dungeon.hero.pos );
		spend( TICK );
		return true;
	}

	//(사용자) 보고하는 창: 보고하면 브로치가 강화되고, 이어서 결실의 핵을 건넬 수 있으면 그 창을 연다
	public static void showReportDialog( final Sage sage ){
		final boolean empress = MazeSage.quest().killed == 1;
		final String name = Messages.titleCase( Messages.get( Sage.class, "name" ) );
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( new SageSprite(), name,
				Messages.get( Sage.class, empress ? "ask_report_empress" : "ask_report_wraith" ),
				Messages.get( Sage.class, "report" ), Messages.get( Sage.class, "later" ) ){
			@Override
			protected void onSelect( int index ) {
				if (index != 0) return;
				MazeSage.report();
				String line = Messages.get( Sage.class, empress ? "reported_empress" : "reported_wraith" );
				if (sage != null) sage.yell( line );
				else com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( "%s: \"%s\" ", name, line );
				if (MazeSage.canGiveCore()) showCoreDialog( sage );
			}
		} ) );
	}

	//(T5) 결실의 핵을 건네는 창. sage가 null이면 대사를 기록에만 남긴다.
	public static void showCoreDialog( final Sage sage ){
		final boolean empress = MazeSage.quest().killed == 1;
		final String name = Messages.titleCase( Messages.get( Sage.class, "name" ) );
		Game.runOnRenderThread( () -> GameScene.show( new WndOptions( new SageSprite(), name,
				Messages.get( Sage.class, empress ? "ask_core_empress" : "ask_core_wraith" ),
				Messages.get( Sage.class, "give_core" ), Messages.get( Sage.class, "later" ) ){
			@Override
			protected void onSelect( int index ) {
				if (index == 0 && MazeSage.giveCore()){
					String line = Messages.get( Sage.class, empress ? "core_given_empress" : "core_given_wraith" );
					if (sage != null) sage.yell( line );
					else com.shatteredpixel.shatteredpixeldungeon.utils.GLog.n( "%s: \"%s\" ", name, line );
				}
			}
		} ) );
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero) return true;
		final SageQuest q = MazeSage.quest();
		//20층 보스를 넘긴 뒤: (사용자) 브로치를 지닌 채 보스를 "보고" 왔으면 먼저 보고해 브로치를 강화하고,
		//(T5) 결실의 핵을 쓰지 않고 가져왔으면 건네고 히든 펫을 받는다. 그 밖에는 아무 일도 없다.
		if (q.bossDone){
			//(사용자) 퀘스트를 받지 않고 20층 보스를 먼저 넘긴 판은 현자와 아무 상호작용도 없다 (브로치를 받을 수도, 보고도, 핵도 없음)
			if (q.choice == -1) return true;
			if (MazeSage.canReport()) showReportDialog( this );
			else if (MazeSage.canGiveCore()) showCoreDialog( this );
			else yell( Messages.get( this, "idle" ) ); //(사용자) 할 일을 모두 마친 뒤
			return true;
		}
		if (q.choice == -1){
			final ArrayList<String> opts = new ArrayList<>();
			final ArrayList<Boolean> who = new ArrayList<>();
			if (q.fixed == -1 || q.fixed == 0){ opts.add( Messages.get( this, "to_wraith" ) ); who.add( false ); }
			if (q.fixed == -1 || q.fixed == 1){ opts.add( Messages.get( this, "to_empress" ) ); who.add( true ); }
			opts.add( Messages.get( this, "later" ) );
			Game.runOnRenderThread( () -> GameScene.show( new WndOptions( sprite(), Messages.titleCase( name() ),
					Messages.get( this, q.fixed == -1 ? "ask" : "ask_fixed" ), opts.toArray( new String[0] ) ){
				@Override
				protected void onSelect( int index ) {
					if (index < who.size()){
						MazeSage.take( who.get( index ) );
						yell( Messages.get( Sage.class, who.get( index ) ? "given_empress" : "given_wraith" ) );
					}
				}
			} ) );
		} else {
			yell( Messages.get( this, "waiting" ) );
		}
		return true;
	}
}
