/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SageQuest;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.SageBrooch;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Maze Tower (M): 현자의 방 퀘스트 (지시서 M, 부록 1의 5절).
// M1 5층 비밀방(원작 쥐 왕의 방 자리)의 숨어 사는 현자가 왕과 여제 중 하나를 고르게 하고 시든 세계수 잎 브로치를 준다 → 그 판의 20층 보스 고정.
// M2 받지 않고 6층에 들어서면 시드의 20층 보스로 고정된다(따로 알리지 않음). 그 뒤 현자에게 가면 그 보스 쪽만 제시된다.
// M3 망령 왕 3단계에서 브로치를 지닌 채 공격하지 않고 두면 잎 다섯 장이 차고, 다 차면 왕이 브로치를 알아보고 결실의 핵을 내놓고 흩어진다(WraithKing).
// M4 브로치의 능력은 자리만. (T5) 20층 보스를 넘긴 뒤 결실의 핵을 쓰지 않고 현자에게 건네면 히든 펫(망령 왕 → 마지막 근위, 여제 → 길든 주문). 역행으로 5층 이하에 닿으면 escapes 값만 저장한다.
public class MazeSage {

	public static SageQuest quest(){
		Hero hero = Dungeon.hero;
		if (hero == null) return null;
		SageQuest q = hero.buff( SageQuest.class );
		if (q == null) q = Buff.affect( hero, SageQuest.class );
		return q;
	}

	//고정된 20층 보스: null 아직, true 여제, false 망령 왕 (MazeBosses.empress가 먼저 본다)
	public static Boolean fixedEmpress(){
		Hero hero = Dungeon.hero;
		if (!MazeTower.M_SAGE || hero == null) return null;
		SageQuest q = hero.buff( SageQuest.class );
		if (q == null || q.fixed == -1) return null;
		return q.fixed == 1;
	}

	//시드가 정한 20층 보스 (현자 퀘스트를 쓰지 않을 때와 같음)
	public static boolean seedEmpress(){
		return new java.util.Random( Dungeon.seed ^ 0x20E3B2E5L ).nextBoolean();
	}

	//층에 들어설 때마다 (Dungeon.switchLevel)
	public static void onArrive( int depth ){
		if (!MazeTower.M_SAGE || Dungeon.hero == null) return;
		SageQuest q = quest();
		//(M2) 받지 않고 15층(M_FIX_DEPTH)에 들어서면 시드의 보스로 고정
		if (depth >= MazeTower.M_FIX_DEPTH && q.fixed == -1 && MazeBosses.force20 == null){
			q.fixed = seedEmpress() ? 1 : 0;
		}
		//(M4) 역행으로 5층 이하에 닿으면 현자가 함께 탈출한다는 값만
		if (MazeRetreat.active() && depth <= 5 && q.choice != -1) q.escapes = true;
	}

	//현자에게서 브로치를 받는다
	public static void take( boolean empress ){
		SageQuest q = quest();
		q.choice = empress ? 1 : 0;
		q.fixed = q.choice;
		SageBrooch b = new SageBrooch();
		b.empress = empress;
		if (!b.collect()) Dungeon.level.drop( b, Dungeon.hero.pos ).sprite.drop();
	}

	//브로치를 지니고 있는지 (그 보스의 것)
	public static boolean carries( boolean empress ){
		Hero hero = Dungeon.hero;
		if (hero == null) return false;
		SageBrooch b = hero.belongings.getItem( SageBrooch.class );
		return b != null && b.empress == empress;
	}

	//20층 보스가 쓰러질 때 (MazeBoss20.afterDeath): 브로치를 지니고 있었으면 전해 준 것으로 친다
	public static void onBoss20Defeated( boolean empress ){
		if (!MazeTower.M_SAGE || Dungeon.hero == null) return;
		SageQuest q = quest();
		q.bossDone = true;
		q.killed = empress ? 1 : 0;
		if (carries( empress )) q.delivered = true;
	}

	//(사용자) 현자에게 보고할 수 있는지: 브로치를 지닌 채 20층 보스를 "보고" 왔을 때 —
	//망령 왕은 처치하지 않고 3단계에서 브로치를 알아보게 했을 때(recognized), 여제는 브로치를 지닌 채 처치했을 때(delivered).
	//결실의 핵을 건네는 것과는 따로다(핵을 건네지 않아도 보고할 수 있다).
	public static boolean canReport(){
		Hero hero = Dungeon.hero;
		if (!MazeTower.M_SAGE || hero == null) return false;
		SageQuest q = hero.buff( SageQuest.class );
		if (q == null || q.choice == -1 || !q.bossDone || q.reported) return false;
		if (!carries( q.killed == 1 )) return false;
		return (q.killed == 0 && q.recognized) || (q.killed == 1 && q.delivered);
	}

	//보고한다: 브로치가 강화된다 (역행 중 16~19층에 적이 다시 생기지 않는다)
	public static void report(){
		if (!canReport()) return;
		quest().reported = true;
		GLog.p( Messages.get( com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sage.class, "brooch_bloomed" ) );
		com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
	}

	//강화된 브로치를 지니고 역행 중일 때, 이 층(16~19층)에는 적이 다시 생기지 않는다 (원래 있던 적은 그대로)
	//(MazeRetreat.refillMobs: 처음 들어설 때 다시 깔기, MobSpawner.act: 시간이 지나며 생기는 적)
	public static boolean blocksRespawn( int depth ){
		Hero hero = Dungeon.hero;
		if (!MazeTower.M_SAGE || hero == null || !MazeRetreat.active()) return false;
		if (depth < MazeTower.M_NO_RESPAWN_FROM || depth > MazeTower.M_NO_RESPAWN_TO) return false;
		SageQuest q = hero.buff( SageQuest.class );
		return q != null && q.reported && hero.belongings.getItem( SageBrooch.class ) != null;
	}

	//역행으로 탈출할 때 현자가 함께 나왔는지 (퀘스트를 받고 역행 중 5층 이하에 닿음) → 탈출 엔딩 화면에 현자 (SurfaceScene)
	public static boolean escapedTogether(){
		Hero hero = Dungeon.hero;
		if (!MazeTower.M_SAGE || hero == null) return false;
		SageQuest q = hero.buff( SageQuest.class );
		return q != null && q.escapes;
	}

	//(T5, 지시서 claude_code_tasks_1126b_pets) 현자에게 결실의 핵을 건넬 수 있는지:
	//퀘스트를 받았고(브로치), 20층 보스를 쓰러뜨렸고, 아직 펫을 고르지 않았고, 핵을 지니고 있을 때
	public static boolean canGiveCore(){
		Hero hero = Dungeon.hero;
		if (!MazeTower.M_SAGE || hero == null || MazeTower.ORIGINAL_ARMOR_ABILITY) return false;
		SageQuest q = hero.buff( SageQuest.class );
		//(사용자) 망령 왕을 처치한 판은 보상이 없다: 왕 쪽은 3단계에서 브로치를 알아보게 했을 때만 (여제는 처치해도 됨)
		return q != null && q.choice != -1 && q.bossDone && q.killed != -1
				&& (q.killed != 0 || q.recognized)
				&& hero.armorAbility == null
				&& hero.belongings.getItem( com.shatteredpixel.shatteredpixeldungeon.items.FruitCore.class ) != null;
	}

	//핵을 건넨다: 핵이 사라지고, 쓰러뜨린 20층 보스에 따라 히든 펫이 그 판의 펫이 된다 (망령 왕 → 마지막 근위, 여제 → 길든 주문).
	//펫은 갑옷 충전으로 부르므로 결실의 핵을 쓸 때처럼 입은 방어구가 직업 방어구가 된다. 방어구를 입고 있어야 한다.
	public static boolean giveCore(){
		Hero hero = Dungeon.hero;
		if (!canGiveCore()) return false;
		if (hero.belongings.armor() == null){
			GLog.w( Messages.get( com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sage.class, "core_naked" ) );
			return false;
		}
		com.shatteredpixel.shatteredpixeldungeon.items.FruitCore core = hero.belongings.getItem( com.shatteredpixel.shatteredpixeldungeon.items.FruitCore.class );
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility pet = quest().killed == 1
				? new com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetSpell()
				: new com.shatteredpixel.shatteredpixeldungeon.actors.hero.pets.PetGuard();
		core.upgradeArmor( hero, hero.belongings.armor(), pet );
		return true;
	}
}
