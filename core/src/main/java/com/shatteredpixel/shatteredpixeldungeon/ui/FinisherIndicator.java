/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;

//Maze Tower: 검투사 필살기 버튼. 원작은 행동 버튼(ActionIndicator)이 하나뿐이라
//무기 스킬 버튼과 필살기 버튼이 겹친다. 그래서 원작 ActionIndicator를 본떠 버튼을 하나 더 둔다.
//화면 오른쪽 아래, 무기 스킬 버튼 위에 놓인다 (GameScene.layoutTags).
public class FinisherIndicator extends Tag {

	Visual primaryVis;
	Visual secondVis;

	public static ActionIndicator.Action action;
	public static FinisherIndicator instance;

	public FinisherIndicator() {
		super( 0 );
		instance = this;
		setSize( SIZE, SIZE );
		visible = false;
	}

	@Override
	public void destroy() {
		super.destroy();
		instance = null;
	}

	@Override
	protected synchronized void layout() {
		super.layout();

		if (primaryVis != null){
			if (!flipped)   primaryVis.x = x + (SIZE - primaryVis.width()) / 2f + 1;
			else            primaryVis.x = x + width - (SIZE + primaryVis.width()) / 2f - 1;
			primaryVis.y = y + (height - primaryVis.height()) / 2f;
			PixelScene.align(primaryVis);
			if (secondVis != null){
				if (secondVis.width() > 16) secondVis.x = primaryVis.center().x - secondVis.width()/2f;
				else                        secondVis.x = primaryVis.center().x + 8 - secondVis.width();
				if (secondVis instanceof BitmapText){
					secondVis.y = primaryVis.center().y + 8 - ((BitmapText) secondVis).baseLine();
				} else {
					secondVis.y = primaryVis.center().y + 8 - secondVis.height();
				}
				PixelScene.align(secondVis);
			}
		}
	}

	private boolean needsRefresh = false;

	@Override
	public void update() {
		super.update();

		synchronized (FinisherIndicator.class) {
			if (!visible && action != null) {
				visible = true;
				needsRefresh = true;
				flash();
			} else {
				visible = action != null;
			}

			if (needsRefresh) {
				if (primaryVis != null) {
					primaryVis.destroy();
					primaryVis.killAndErase();
					primaryVis = null;
				}
				if (secondVis != null) {
					secondVis.destroy();
					secondVis.killAndErase();
					secondVis = null;
				}
				if (action != null) {
					primaryVis = action.primaryVisual();
					add(primaryVis);
					secondVis = action.secondaryVisual();
					if (secondVis != null) add(secondVis);
					setColor(action.indicatorColor());
				}
				layout();
				needsRefresh = false;
			}

			float a = Dungeon.hero.ready ? 1f : 0.5f;
			if (primaryVis != null) primaryVis.alpha(a);
			if (secondVis != null) secondVis.alpha(a);
		}
	}

	@Override
	protected void onClick() {
		super.onClick();
		if (action != null && Dungeon.hero.ready) {
			action.doAction();
		}
	}

	@Override
	protected String hoverText() {
		String text = (action == null ? null : action.actionName());
		return text != null ? Messages.titleCase(text) : null;
	}

	public static void setAction(ActionIndicator.Action action){
		synchronized (FinisherIndicator.class) {
			FinisherIndicator.action = action;
			refresh();
		}
	}

	public static void clearAction(ActionIndicator.Action action){
		synchronized (FinisherIndicator.class) {
			if (action == null || FinisherIndicator.action == action) {
				FinisherIndicator.action = null;
			}
		}
	}

	public static void refresh(){
		synchronized (FinisherIndicator.class) {
			if (instance != null) instance.needsRefresh = true;
		}
	}
}
