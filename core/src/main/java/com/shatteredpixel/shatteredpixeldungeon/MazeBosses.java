/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

//Maze Tower: 마탑 보스 (24단계, 명세 19번 + 기획 문서 "마탑 보스 외형과 패턴 초안").
//  5층 썩은 수액 덩어리 (원작 구)     10층 그림자 간수 (원작 텐구)
//  15층 마도 골렘 또는 수정 거인 (원작 DM-300, 판마다 무작위, 던전 시드를 따름)
//  20층 망령 왕 또는 마도시의 여제 (원작 드워프 왕, 판마다 무작위, 지금은 외형·이름만)
//  25층 탑의 주인 (원작 요그, 지금은 외형·이름만)
public class MazeBosses {

	//마탑 보스 외형·패턴을 쓸지 (false면 원작 보스 그대로)
	public static final boolean ENABLED = true;

	//15층: 수정 거인이 나오는 판인지 (아니면 마도 골렘). 같은 시드면 같은 보스.
	//치트로 바꾼 보스 (저장되지 않음, 시험용). null이면 시드대로
	public static Boolean force15 = null;
	public static Boolean force20 = null;

	public static boolean crystalGiant(){
		if (ENABLED && force15 != null) return force15;
		return ENABLED && new java.util.Random(Dungeon.seed ^ 0x15C0FFEEL).nextBoolean();
	}

	//20층: 마도시의 여제가 나오는 판인지 (아니면 망령 왕). 같은 시드면 같은 보스.
	public static boolean empress(){
		if (ENABLED && force20 != null) return force20;
		return ENABLED && new java.util.Random(Dungeon.seed ^ 0x20E3B2E5L).nextBoolean();
	}

	//보스 이름·설명 문자열 키 ("name" / "desc"). 마탑 보스가 아니면 null (원작 글)
	//대사(yell)는 원작 그대로 두고 27단계 글 바꾸기 때 다시 쓴다.
	public static String textKey( Object boss, String what ){
		if (!ENABLED) return null;
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo
				|| boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu
				|| boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa){
			return "maze_" + what;
		}
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300){
			return (crystalGiant() ? "maze_crystal_" : "maze_golem_") + what;
		}
		//수정 거인 판의 기둥은 수정 기둥
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Pylon && crystalGiant()){
			return "maze_crystal_" + what;
		}
		if (boss instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing){
			return (empress() ? "maze_empress_" : "maze_wraith_") + what;
		}
		return null;
	}

	//15층 보스방 그림: 그 판의 보스(마도 골렘 / 수정 거인)에 맞춘 마탑 그림
	public static String cavesBossTex(){
		if (!ENABLED) return Assets.Environment.CAVES_BOSS;
		return crystalGiant() ? Assets.Environment.MAZE_CAVES_BOSS_CRYSTAL : Assets.Environment.MAZE_CAVES_BOSS_GOLEM;
	}

	//20층 보스방 그림: 그 판의 보스(망령 왕 / 마도시의 여제)에 맞춘 마탑 그림
	public static String cityBossTex(){
		if (!ENABLED) return Assets.Environment.CITY_BOSS;
		return empress() ? Assets.Environment.MAZE_CITY_BOSS_EMPRESS : Assets.Environment.MAZE_CITY_BOSS_WRAITH;
	}

	//(결정, 2026-11-22 사용자) 보스전 중인지: 보스층이 잠겨 보스와 싸우는 동안. 이때는 이동 속도를 빠르게 하는 효과가 듣지 않는다(Hero.speed)
	public static boolean bossFight(){
		return ENABLED && MazeTower.BOSS_NO_SPEEDUP && Dungeon.level != null && Dungeon.bossLevel() && Dungeon.level.locked;
	}

	//(결정, 2026-11-22 사용자) 보스전 동안 주인공 상태 칸에 "속도 봉인" 아이콘 (원작 신속 아이콘을 회색으로). 보스전이 끝나면 사라진다
	public static void checkSpeedLock( com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero h ){
		if (h == null) return;
		if (bossFight()){
			if (h.buff(SpeedLock.class) == null) com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(h, SpeedLock.class);
		} else {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.detach(h, SpeedLock.class);
		}
	}

	public static class SpeedLock extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff {
		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		@Override
		public boolean act() {
			if (!bossFight()) detach();
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.HASTE;
		}

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ) {
			icon.hardlight(0.45f, 0.45f, 0.5f);
		}

		@Override
		public String name() {
			return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "name");
		}

		@Override
		public String desc() {
			return com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "desc");
		}
	}
}
