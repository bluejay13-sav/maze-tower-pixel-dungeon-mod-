/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.watabou.utils.Bundle;

//Maze Tower (M): 현자의 방 퀘스트 기록. 주인공에게 붙는 보이지 않는 상태(층을 옮겨도, 저장해도 남음). 효과는 없고 값만 담는다(MazeSage).
public class SageQuest extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	public int choice = -1;        //현자에게서 받은 브로치의 상대: -1 아직, 0 망령 왕, 1 마도시의 여제
	public int fixed = -1;         //고정된 20층 보스: -1 아직, 0 망령 왕, 1 여제 (브로치를 받거나 6층에 들어서면 정해짐)
	public boolean delivered = false; //브로치를 지닌 채 20층 보스를 넘겼는지 (현자에게 돌아갈 보상의 자리)
	public boolean recognized = false;//망령 왕이 브로치를 알아보고 스스로 흩어졌는지
	public boolean escapes = false;   //역행으로 5층 이하에 닿았는지 (현자가 함께 탈출하는 엔딩의 자리, 값만)
	public boolean bossDone = false;  //20층 보스를 넘겼는지
	public int killed = -1;           //(T5) 쓰러뜨린 20층 보스: -1 아직, 0 망령 왕(브로치를 알아보고 흩어진 것 포함), 1 여제 → 현자에게서 받는 히든 펫
	public boolean reported = false;  //(사용자) 20층 보스를 본 뒤 현자에게 돌아와 보고했는지 → 브로치가 강화됨 (역행 중 16~19층에 적이 다시 생기지 않음)

	@Override
	public boolean act() {
		spend( TICK * 1000 ); //아무것도 하지 않는다
		return true;
	}

	private static final String CHOICE = "choice", FIXED = "fixed", BOSS_DONE = "boss_done", DELIVERED = "delivered", RECOGNIZED = "recognized", ESCAPES = "escapes";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CHOICE, choice ); bundle.put( FIXED, fixed );
		bundle.put( DELIVERED, delivered ); bundle.put( RECOGNIZED, recognized ); bundle.put( ESCAPES, escapes ); bundle.put( BOSS_DONE, bossDone );
		bundle.put( "killed", killed );
		bundle.put( "reported", reported );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		choice = bundle.getInt( CHOICE ); fixed = bundle.getInt( FIXED );
		delivered = bundle.getBoolean( DELIVERED ); recognized = bundle.getBoolean( RECOGNIZED ); escapes = bundle.getBoolean( ESCAPES ); bossDone = bundle.getBoolean( BOSS_DONE );
		killed = bundle.contains( "killed" ) ? bundle.getInt( "killed" ) : (bossDone ? fixed : -1);
		reported = bundle.getBoolean( "reported" );
	}
}
