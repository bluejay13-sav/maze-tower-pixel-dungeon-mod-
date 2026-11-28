/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.MazeTower;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Maze Tower: 2차(개화) 특성 (구현 지시서 maze_tower_impl_spec_1124.md 부록 2).
// - 전사 21개(공용 15 + 검 전용 6), 사냥꾼 21개(공용 16 + 활 전용 5).
//   특성마다 소속 풀(Pool), 구분(Scope: 공용/검 전용/활 전용), 갈래(Branch: 공격/형태/생존/유틸)를 둔다.
//   효과는 C·D 단계에서 하나씩 넣는다. 섬광 보법(FLASH_STEP)은 예전 특성을 전사 풀로 옮긴 것이라 효과가 이미 있다.
// - (결정, 2026-11-24) 예전 30개는 지웠다. 마법사·도적은 2차 특성이 아직 없다(나중에 추가).
//아이콘 번호는 talent2_icons.png (16×16, 가로 8칸) 순서. 31번은 분홍 꽃잎. 새 특성은 그림이 올 때까지 갈래별 임시 아이콘.
public enum BloomTalent {

	//===== 전사: 공용 15 + 검 전용 6 =====
	W_RIPOSTE       (Pool.WARRIOR, Scope.COMMON, Branch.ATTACK),   //받아치기
	W_OPENING       (Pool.WARRIOR, Scope.COMMON, Branch.ATTACK),   //허점 찌르기
	W_HONE          (Pool.WARRIOR, Scope.COMMON, Branch.ATTACK),   //벼림
	W_SPREAD        (Pool.WARRIOR, Scope.COMMON, Branch.ATTACK),   //번지는 반응
	W_ECHO          (Pool.WARRIOR, Scope.COMMON, Branch.FORM),     //메아리
	W_ONE_MORE      (Pool.WARRIOR, Scope.COMMON, Branch.FORM),     //한 번 더
	W_HEAVY         (Pool.WARRIOR, Scope.COMMON, Branch.FORM),     //무거운 일격
	W_HEATED_EDGE   (Pool.WARRIOR, Scope.COMMON, Branch.FORM),     //달군 날
	W_BREATHER      (Pool.WARRIOR, Scope.COMMON, Branch.SURVIVAL), //숨 고르기
	W_LAST_LAYER    (Pool.WARRIOR, Scope.COMMON, Branch.SURVIVAL), //마지막 겹
	W_ONE_ON_ONE    (Pool.WARRIOR, Scope.COMMON, Branch.SURVIVAL), //일대일
	W_HOLD_BREATH   (Pool.WARRIOR, Scope.COMMON, Branch.SURVIVAL), //버티는 숨
	W_OVERFLOW_CUP  (Pool.WARRIOR, Scope.COMMON, Branch.UTIL),     //넘치는 잔
	W_FRICTION      (Pool.WARRIOR, Scope.COMMON, Branch.UTIL),     //마찰
	W_LIGHT_FEET    (Pool.WARRIOR, Scope.COMMON, Branch.UTIL),     //가벼운 발
	W_CRESCENT      (Pool.WARRIOR, Scope.SWORD,  Branch.FORM),     //초승달
	FLASH_STEP      (17, Pool.WARRIOR, Scope.SWORD, Branch.FORM),  //섬광 보법 (예전 특성을 옮김, 아이콘도 그대로)
	W_LONG_EDGE     (Pool.WARRIOR, Scope.SWORD,  Branch.ATTACK),   //긴 칼날
	W_BLOODY_EDGE   (Pool.WARRIOR, Scope.SWORD,  Branch.ATTACK),   //피 묻은 날
	W_DEFLECT       (Pool.WARRIOR, Scope.SWORD,  Branch.SURVIVAL), //받아넘기기
	W_QUICKDRAW     (Pool.WARRIOR, Scope.SWORD,  Branch.UTIL),     //발도

	//===== 사냥꾼: 공용 16 + 활 전용 5 =====
	H_FIRST_STRIKE  (Pool.HUNTRESS, Scope.COMMON, Branch.ATTACK),   //첫 일격
	H_WEAK_POINT    (Pool.HUNTRESS, Scope.COMMON, Branch.ATTACK),   //약점
	H_OPEN_WOUND    (Pool.HUNTRESS, Scope.COMMON, Branch.ATTACK),   //벌어진 상처
	H_HIT_AND_RUN   (Pool.HUNTRESS, Scope.COMMON, Branch.ATTACK),   //치고 빠지기
	H_FAR_TARGET    (Pool.HUNTRESS, Scope.COMMON, Branch.ATTACK),   //먼 과녁
	H_CLEAN_KILL    (Pool.HUNTRESS, Scope.COMMON, Branch.ATTACK),   //값진 일격
	H_RICOCHET      (Pool.HUNTRESS, Scope.COMMON, Branch.FORM),     //튀는 일격
	H_SPROUT_TRAIL  (Pool.HUNTRESS, Scope.COMMON, Branch.FORM),     //새잎 자국
	H_BINDING_VINE  (Pool.HUNTRESS, Scope.COMMON, Branch.FORM),     //휘감는 덩굴
	H_GRASS_BODY    (Pool.HUNTRESS, Scope.COMMON, Branch.SURVIVAL), //풀숲의 몸
	H_BREAKFALL     (Pool.HUNTRESS, Scope.COMMON, Branch.SURVIVAL), //낙법
	H_HERB_PICK     (Pool.HUNTRESS, Scope.COMMON, Branch.SURVIVAL), //약초 캐기
	H_SMOKE_FEATHER (Pool.HUNTRESS, Scope.COMMON, Branch.SURVIVAL), //연막 깃
	H_TAUT_BREATH   (Pool.HUNTRESS, Scope.COMMON, Branch.SURVIVAL), //팽팽한 숨
	H_SET_TRAP      (Pool.HUNTRESS, Scope.COMMON, Branch.UTIL),     //덫 놓기
	H_SPIRIT_DANCE  (Pool.HUNTRESS, Scope.COMMON, Branch.UTIL),     //정령의 춤
	H_PIN           (Pool.HUNTRESS, Scope.BOW,    Branch.ATTACK),   //못 박기
	H_EMBED_ARROW   (Pool.HUNTRESS, Scope.BOW,    Branch.FORM),     //박힌 화살
	H_ARROW_PICKUP  (Pool.HUNTRESS, Scope.BOW,    Branch.UTIL),     //화살 줍기
	H_ARROW_FIELD   (Pool.REMOVED,  Scope.BOW,    Branch.UTIL),     //화살밭 (결정, 2026-11-24: 지움)
	H_BOW_BASH      (Pool.HUNTRESS, Scope.BOW,    Branch.SURVIVAL); //활대 치기

	//분홍 꽃잎 아이콘 번호 (talent2_icons.png)
	public static final int PETAL_ICON = 31;

	//어느 직업이 고를 수 있는지
	public enum Pool { WARRIOR, HUNTRESS, REMOVED } //REMOVED: 지운 특성 (예전 저장 파일·아이콘 번호 때문에 자리만 남김, 어느 직업도 고르지 못함)

	//구분: 공용 / 검 전용 / 활 전용
	public enum Scope {
		COMMON, SWORD, BOW;

		public String title(){
			return Messages.get(BloomTalent.class, "scope_" + name().toLowerCase());
		}
	}

	//갈래: 공격 / 형태 / 생존 / 유틸
	public enum Branch {
		ATTACK, FORM, SURVIVAL, UTIL;

		public String title(){
			return Messages.get(BloomTalent.class, "branch_" + name().toLowerCase());
		}

		//새 특성 그림이 올 때까지 갈래별로 빌려 쓰는 아이콘 (talent2_icons.png 번호)
		int tempIcon(){
			switch (this){
				case ATTACK:   return 1;
				case FORM:     return 9;
				case SURVIVAL: return 28;
				default:       return 30;
			}
		}
	}

	private final int icon;
	public final Pool pool;
	public final Scope scope;
	public final Branch branch;

	//그림이 없어 갈래별 임시 아이콘
	BloomTalent(Pool pool, Scope scope, Branch branch){
		this(branch.tempIcon(), pool, scope, branch);
	}

	BloomTalent(int icon, Pool pool, Scope scope, Branch branch){
		this.icon = icon;
		this.pool = pool;
		this.scope = scope;
		this.branch = branch;
	}

	public int icon(){
		return icon;
	}

	public boolean inPool( Pool p ){
		return pool == p;
	}

	//영웅이 고르는 풀. 2차 특성이 없는 직업(마법사·도적)은 null
	public static Pool poolOf( Hero hero ){
		if (hero == null) return null;
		switch (hero.heroClass){
			case WARRIOR:  return Pool.WARRIOR;
			case HUNTRESS: return Pool.HUNTRESS;
			default:       return null;
		}
	}

	//(B2) 이 특성이 지금 작동하는지. 공용은 언제나, 검 전용은 검 7종, 활 전용은 활 7종을 들고 있을 때만.
	//무기를 바꾸면 꺼지고 다시 들면 켜진다 (각성도 함께). 선택지에는 무기와 상관없이 나온다 (결정, 2026-11-24).
	public boolean activeFor( Hero hero ){
		if (hero == null) return false;
		switch (scope){
			case SWORD: return holdsSword(hero);
			case BOW:   return com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortbow.equipped(hero) != null;
			default:    return true;
		}
	}

	//검 전용 특성이 말하는 "검": 검 스킬이 있는 검 7종 (명세 2번)
	public static boolean holdsSword( Hero hero ){
		com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon w = hero.belongings.weapon();
		return w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Katana
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RunicBlade
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword     //Maze Tower (W1): 5티어 대검
				|| w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ResonanceSword; //Maze Tower (W2): 5티어 공명검
	}

	//선택지를 뽑을 때 서로 달라야 하는 묶음 (갈래)
	public Object pickGroup(){
		return branch;
	}

	public String title(){
		return Messages.get(this, name() + ".title");
	}

	//구분·갈래 + 효과 + 레벨이 올리는 것 + 각성
	public String desc(){
		return Messages.get(BloomTalent.class, "header_new", scope.title(), branch.title())
				+ "\n\n" + Messages.get(this, name() + ".desc")
				+ "\n\n" + Messages.get(BloomTalent.class, "levelup_label") + " " + Messages.get(this, name() + ".levelup", levelValues())
				+ "\n\n" + Messages.get(BloomTalent.class, "awaken_label") + " " + Messages.get(this, name() + ".awaken");
	}

	//"레벨이 올리는 것"에 넣는 레벨별 수치 (MazeTower 상수에서 만든다. 효과가 있는 특성만)
	private Object[] levelValues(){
		switch (this){
			case W_RIPOSTE: return new Object[]{ percents(MazeTower.C1_RIPOSTE_DMG) };
			case W_OPENING: return new Object[]{ percents(MazeTower.C2_OPENING_DMG) };
			case W_SPREAD:  return new Object[]{ percents(MazeTower.C4_SPREAD_RATIO) };
			case W_ECHO:    return new Object[]{ percents(MazeTower.C5_ECHO_RATIO) };
			case W_ONE_MORE: return new Object[]{ percents(MazeTower.C6_ONE_MORE_TOTAL) };
			case W_HEAVY:   return new Object[]{ percents(MazeTower.C7_HEAVY_COLLIDE) };
			case W_HEATED_EDGE: return new Object[]{ numbers(MazeTower.C8_BURN_TURNS), numbers(MazeTower.C8_BURN_DMG) };
			case W_LAST_LAYER: return new Object[]{ percents(MazeTower.C10_LAYER_REDUCE) };
			case W_ONE_ON_ONE: return new Object[]{ percents(MazeTower.C11_DUEL_REDUCE) };
			case W_HOLD_BREATH: return new Object[]{ percents(MazeTower.C12_HOLD_SHIELD), Math.round(MazeTower.C12_HOLD_COOLDOWN) };
			case W_OVERFLOW_CUP: return new Object[]{ percents(MazeTower.C13_CUP_RATIO), percents(MazeTower.C13_CUP_CAP) };
			case W_FRICTION: return new Object[]{ numbers(MazeTower.C14_FRICTION_CHARGE) };
			case W_LIGHT_FEET: return new Object[]{ percents(MazeTower.C15_FLOOR_REDUCE) };
			case W_LONG_EDGE: return new Object[]{ percents(MazeTower.C18_LONG_EDGE_RATIO) };
			case W_BLOODY_EDGE: return new Object[]{ percents(MazeTower.C19_BLEED_CHANCE), percents(MazeTower.C19_BLEED_POWER) };
			case W_DEFLECT: return new Object[]{ percents(MazeTower.C20_DEFLECT_CHANCE) };
			case W_QUICKDRAW: return new Object[]{ percents(MazeTower.C21_QUICKDRAW_DMG) };
			case H_FIRST_STRIKE: return new Object[]{ percents(MazeTower.D1_FIRST_STRIKE_DMG) };
			case H_WEAK_POINT: return new Object[]{ percents(MazeTower.D2_CRIT_CHANCE) };
			case H_OPEN_WOUND: return new Object[]{ percents(MazeTower.D3_BURST_PER_STACK) };
			case H_HIT_AND_RUN: return new Object[]{ percents(MazeTower.D4_HIT_AND_RUN_DMG) };
			case H_FAR_TARGET: return new Object[]{ percents(MazeTower.D5_PER_TILE) };
			case H_CLEAN_KILL: {
				float[] f = new float[MazeTower.D6_CLEAN_MANA.length];
				for (int i = 0; i < f.length; i++) f[i] = MazeTower.D6_CLEAN_MANA[i];
				return new Object[]{ numbers(f) };
			}
			case H_RICOCHET: return new Object[]{ percents(MazeTower.D7_RICOCHET_RATIO) };
			case H_SPROUT_TRAIL: {
				float[] f = new float[MazeTower.D8_SPROUT_CELLS.length];
				for (int i = 0; i < f.length; i++) f[i] = MazeTower.D8_SPROUT_CELLS[i];
				return new Object[]{ numbers(f) };
			}
			case H_BINDING_VINE: return new Object[]{ numbers(MazeTower.D9_ROOT_TURNS) };
			case H_GRASS_BODY: return new Object[]{ percents(MazeTower.D10_GRASS_EVASION) };
			case H_BREAKFALL: return new Object[]{ percents(MazeTower.D11_REDUCE) };
			case H_HERB_PICK: return new Object[]{ percents(MazeTower.D12_HERB_CHANCE), percents(MazeTower.D12_HERB_HEAL) };
			case H_SMOKE_FEATHER: return new Object[]{ numbers(MazeTower.D13_INVIS_TURNS) };
			case H_TAUT_BREATH: return new Object[]{ percents(MazeTower.D14_TAUT_EVASION) };
			case H_PIN: return new Object[]{ numbers(MazeTower.D17_PIN_TURNS) };
			case H_EMBED_ARROW: return new Object[]{ percents(MazeTower.D18_BURST_DMG) };
			case H_ARROW_PICKUP: return new Object[]{ percents(MazeTower.D19_DROP_BONUS) };
			case H_BOW_BASH: return new Object[]{ percents(MazeTower.D21_SHOT_DMG) };
			case H_SET_TRAP: return new Object[]{ percents(MazeTower.D15_SNARE_DMG), numbers(MazeTower.D15_ROOT_TURNS) };
			case H_SPIRIT_DANCE: { int[] m = MazeTower.D16_MANA_PER_STEP; return new Object[]{ numbers(new float[]{ m[0], m[1], m[2] }) }; }
			case W_CRESCENT: return new Object[]{ percents(MazeTower.C16_CRESCENT_RATIO) };
			case FLASH_STEP: {
				StringBuilder s = new StringBuilder();
				for (int i = 0; i < MazeTower.C17_FLASH_RANGE.length; i++){
					if (i > 0) s.append(" / ");
					s.append(MazeTower.C17_FLASH_RANGE[i]);
				}
				return new Object[]{ s.toString() };
			}
			case W_BREATHER: return new Object[]{ percents(MazeTower.C9_BREATHER_PER_TURN), percents(MazeTower.C9_BREATHER_CAP) };
			case W_HONE: {
				float[] cap = new float[MazeTower.C3_HONE_PER_TURN.length];
				for (int i = 0; i < cap.length; i++) cap[i] = MazeTower.C3_HONE_PER_TURN[i] * MazeTower.C3_HONE_MAX_TURNS;
				return new Object[]{ percents(MazeTower.C3_HONE_PER_TURN), percents(cap) };
			}
			default:        return new Object[0];
		}
	}

	//{3, 4, 5} -> "3 / 4 / 5", {1, 1.25, 1.5} -> "1 / 1.25 / 1.5"
	private static String numbers(float[] v){
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < v.length; i++){
			if (i > 0) s.append(" / ");
			float x = Math.round(v[i] * 100) / 100f;
			s.append(x == (int)x ? Integer.toString((int)x) : Float.toString(x));
		}
		return s.toString();
	}

	//{0.3, 0.45, 0.6} -> "30% / 45% / 60%"
	private static String percents(float[] v){
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < v.length; i++){
			if (i > 0) s.append(" / ");
			s.append(Math.round(v[i] * 100)).append('%');
		}
		return s.toString();
	}

	//선택 창에서 보여 줄 짧은 설명
	public String shortDesc(){
		return Messages.get(this, name() + ".short");
	}
}
