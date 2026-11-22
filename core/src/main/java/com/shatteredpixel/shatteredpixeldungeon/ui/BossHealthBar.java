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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoMob;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;

public class BossHealthBar extends Component {

	private Image bar;

	private Image shieldHP;
	private Image hp;
	private Image Dot; //a visual darkening over HP and shield that shows total incoming DOT
	private BitmapText hpText;

	private Button bossInfo;
	private BuffIndicator buffs;

	private static Mob boss;

	private Image skull;
	private Emitter blood;

	private static String asset = Assets.Interfaces.BOSSHP;

	private static BossHealthBar instance;
	private static boolean bleeding;

	private boolean large;

	//Maze Tower (2026-11-22): 페이즈 눈금. 페이즈가 바뀌는 지점마다 금색 세로 눈금(위아래로 1px씩 삐져나옴, 맨 위 1px만 밝게),
	//이미 지난 눈금은 회색. turnsLeft() >= 0이면 체력바 대신 남은 턴 숫자 (25층 3페이즈)
	public interface PhaseMarks {
		float[] phaseMarks();
		int turnsLeft();
	}
	private final java.util.ArrayList<com.watabou.noosa.ColorBlock[]> marks = new java.util.ArrayList<>();

	private void updateMarks(){
		float[] fr = boss instanceof PhaseMarks ? ((PhaseMarks) boss).phaseMarks() : new float[0];
		if (fr == null) fr = new float[0];
		while (marks.size() < fr.length){
			com.watabou.noosa.ColorBlock body = new com.watabou.noosa.ColorBlock(1, 1, 0xFFD4A537);
			com.watabou.noosa.ColorBlock top = new com.watabou.noosa.ColorBlock(1, 1, 0xFFFFE58A);
			add(body); add(top);
			marks.add(new com.watabou.noosa.ColorBlock[]{body, top});
		}
		float fullW = large ? 96 : 47;
		float frac = boss.HP / (float)boss.HT;
		for (int i = 0; i < marks.size(); i++){
			com.watabou.noosa.ColorBlock[] m = marks.get(i);
			boolean show = i < fr.length && hp.visible;
			m[0].visible = m[1].visible = show;
			if (!show) continue;
			boolean passed = frac <= fr[i];
			float x = hp.x + Math.round(fullW * fr[i]);
			m[0].size(1, hp.height + 1);
			m[0].x = x; m[0].y = hp.y;
			m[1].size(1, 1);
			m[1].x = x; m[1].y = hp.y - 1;
			m[0].hardlight(passed ? 0x6E6E6E : 0xD4A537);
			m[1].hardlight(passed ? 0x9A9A9A : 0xFFE58A);
			bringToFront(m[0]); bringToFront(m[1]);
		}
	}

	public BossHealthBar() {
		super();
		visible = active = (boss != null);
		instance = this;
	}

	@Override
	public synchronized void destroy() {
		super.destroy();
		if (instance == this) instance = null;
		if (buffs != null) BuffIndicator.setBossInstance(null);
	}

	@Override
	protected void createChildren() {
		this.large = SPDSettings.interfaceSize() != 0;

		bar = large ? new Image(asset, 0, 16, 128, 30) : new Image(asset, 0, 0, 64, 16);
		add(bar);

		width = bar.width;
		height = bar.height;

		shieldHP = large ? new Image(asset, 0, 55, 96, 9) : new Image(asset, 71, 5, 47, 4);
		add(shieldHP);

		hp = large ? new Image(asset, 0, 46, 96, 9) : new Image(asset, 71, 0, 47, 4);
		add(hp);

		Dot = large ? new Image(asset, 0, 46, 96, 9) : new Image(asset, 71, 0, 47, 4);
		Dot.hardlight(0, 0, 0);
		Dot.alpha(0.25f);
		add(Dot);

		hpText = new BitmapText(PixelScene.pixelFont);
		hpText.alpha(0.6f);
		add(hpText);

		bossInfo = new Button(){
			@Override
			protected void onClick() {
				super.onClick();
				if (boss != null){
					GameScene.show(new WndInfoMob(boss));
				}
			}

			@Override
			protected String hoverText() {
				if (boss != null){
					return boss.name();
				}
				return super.hoverText();
			}
		};
		add(bossInfo);

		if (boss != null) {
			buffs = new BuffIndicator(boss, large);
			BuffIndicator.setBossInstance(buffs);
			add(buffs);
		}

		if (boss != null && large) {
			skull = boss.sprite();
		} else {
			skull = new Image(asset, 64, 0, 6, 6);
		}
		add(skull);

		blood = new Emitter();
		blood.pos(skull);
		blood.pour(BloodParticle.FACTORY, 0.3f);
		blood.autoKill = false;
		blood.on = false;
		add( blood );
	}

	@Override
	protected void layout() {
		bar.x = x;
		bar.y = y;

		hp.x = shieldHP.x = Dot.x = bar.x+(large ? 30 : 15);
		hp.y = shieldHP.y = Dot.y = bar.y+(large ? 2 : 3);

		if (!large) hpText.scale.set(PixelScene.align(0.5f));
		hpText.x = hp.x + (large ? (96-hpText.width())/2f : 1);
		hpText.y = hp.y + (hp.height - (hpText.baseLine()+hpText.scale.y))/2f;
		hpText.y -= 0.001f; //prefer to be slightly higher
		PixelScene.align(hpText);

		bossInfo.setRect(x, y, bar.width, bar.height);

		if (buffs != null) {
			buffs.maxBuffs = 12;
			if (large) {
				//little extra width here for a 6th column
				buffs.setRect(hp.x+1, hp.y + 12, 102, 34);
			} else {
				buffs.setRect(hp.x, hp.y + 5, 47, 16);
			}
		}

		int paneSize = large ? 30 : 16;

		if (skull.height() > 24 || skull.width() > 24){
			skull.scale.set(skull.scale.x * PixelScene.align(24f/Math.max(skull.width(), skull.height())));
		}

		skull.x = bar.x + (paneSize - skull.width())/2f;
		skull.y = bar.y + (paneSize - skull.height())/2f;
	}

	@Override
	public void update() {
		super.update();
		if (boss != null){
			if (!boss.isAlive() || !Dungeon.level.mobs.contains(boss)){
				boss = null;
				visible = active = false;
				if (buffs != null) {
					BuffIndicator.setBossInstance(null);
					remove(buffs);
					buffs.destroy();
					buffs = null;
				}
			} else {

				int health = boss.HP;
				int shield = boss.shielding();
				int incomingDOT = boss.incomingDOT();
				int max = boss.HT;

				float healthPercent = health/(float)max;
				float shieldPercent = shield/(float)max;
				float DOTPercent    = incomingDOT/(float)max;

				if (healthPercent + shieldPercent > 1f){
					float excess = healthPercent + shieldPercent;
					healthPercent /= excess;
					shieldPercent /= excess;
					DOTPercent    /= excess;
				}

				hp.scale.x = healthPercent;
				shieldHP.scale.x = healthPercent + shieldPercent;
				Dot.scale.x = Math.min(DOTPercent, shieldHP.scale.x);
				Dot.x = shieldHP.x + shieldHP.width() - Dot.width();

				if (bleeding != blood.on){
					if (bleeding)   skull.tint( 0xcc0000, large ? 0.3f : 0.6f );
					else            skull.resetColor();
					bringToFront(blood);
					blood.pos(skull);
					blood.on = bleeding;
				}

				if (shield <= 0){
					hpText.text(health + "/" + max);
				} else {
					hpText.text(health + "+" + shield +  "/" + max);
				}
				//Maze Tower: 25층 3페이즈는 체력바 대신 남은 턴 (그림은 나중에, 지금은 숫자)
				int turns = boss instanceof PhaseMarks ? ((PhaseMarks) boss).turnsLeft() : -1;
				hp.visible = shieldHP.visible = Dot.visible = turns < 0;
				if (turns >= 0){
					hpText.text(turns + " / " + com.shatteredpixel.shatteredpixeldungeon.MazeTower.B25_P3_TURNS);
				}
				hpText.measure();
				hpText.x = hp.x + (large ? (96-hpText.width())/2f : 1);
				updateMarks();

			}
		}
	}

	public static void assignBoss(Mob boss){
		assignBoss(boss, false);
	}

	public static void assignBoss(Mob boss, boolean forceSpriteRefresh){
		if (BossHealthBar.boss == boss && instance != null) {
			//re-assign sprite if it has changed
			if (forceSpriteRefresh && instance.large){
				ShatteredPixelDungeon.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						instance.remove(instance.skull);
						instance.skull.destroy();
						instance.skull = boss.sprite();
						instance.add(instance.skull);
						instance.layout();
					}
				});
			}
			return;
		}
		BossHealthBar.boss = boss;
		bleed(false);
		if (instance != null) {
			ShatteredPixelDungeon.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					instance.visible = instance.active = true;
					if (boss != null){
						if (instance.large){
							if (instance.skull != null){
								instance.remove(instance.skull);
								instance.skull.destroy();
							}
							instance.skull = boss.sprite();
							instance.add(instance.skull);
						}
						if (instance.buffs != null){
							instance.remove(instance.buffs);
							instance.buffs.destroy();
						}
						instance.buffs = new BuffIndicator(boss, instance.large);
						BuffIndicator.setBossInstance(instance.buffs);
						instance.add(instance.buffs);
						instance.layout();
					}
				}
			});
		}
	}
	
	public static boolean isAssigned(){
		return boss != null && boss.isAlive() && Dungeon.level.mobs.contains(boss);
	}

	public static void bleed(boolean value){
		bleeding = value;
	}

	public static boolean isBleeding(){
		return isAssigned() && bleeding;
	}

}
