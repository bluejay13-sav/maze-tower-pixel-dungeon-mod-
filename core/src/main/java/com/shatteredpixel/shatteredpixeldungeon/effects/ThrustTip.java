/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.Visual;
import com.watabou.utils.PointF;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

//Maze Tower: 찌르기 이펙트. 가느다란 강철 화살촉이 주인공 쪽에서 적을 꿰뚫고 지나가며 사라진다.
//화살촉(가늘고 긴 삼각형) + 뒤쪽의 아주 가는 자루로 이루어진 모양을 그린다.
//진한 강철색 몸체 위에 밝은 날(가운데 선)을 한 번 더 겹쳐 그려 쇠 느낌을 낸다.
public class ThrustTip extends Visual {

	private final SmartTexture texture;
	private final FloatBuffer vertices;
	private final ShortBuffer indices;
	private final int nIndices;

	private final PointF from, to;
	private final float travelTime, fadeTime;
	private float time = 0;
	private float delay;
	private Runnable onArrive;
	private boolean arrived = false;

	//length: 화살촉 길이, width: 화살촉 밑변 폭, shaft: 뒤쪽 자루 길이
	private ThrustTip( PointF from, PointF to, float length, float width, float shaft,
	                   float travelTime, float fadeTime, float delay ){
		super(0, 0, 0, 0);
		this.from = from;
		this.to = to;
		this.travelTime = travelTime;
		this.fadeTime = fadeTime;
		this.delay = delay;

		texture = TextureCache.createSolid(0xFFFFFFFF);

		//로컬 좌표: 끝이 +x를 향한다. (0,0)이 화살촉 끝.
		float[] v = {
				//화살촉 삼각형
				0, 0,                 0.5f, 0.5f,
				-length,  width/2f,   0.5f, 0.5f,
				-length, -width/2f,   0.5f, 0.5f,
				//자루 (폭 1)
				-length,        0.5f, 0.5f, 0.5f,
				-length-shaft,  0.5f, 0.5f, 0.5f,
				-length-shaft, -0.5f, 0.5f, 0.5f,
				-length,       -0.5f, 0.5f, 0.5f,
		};
		short[] idx = { 0,1,2,  3,4,5,  3,5,6 };

		vertices = ByteBuffer.allocateDirect(v.length * Float.SIZE / 8)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertices.put(v);
		((Buffer)vertices).position(0);
		indices = ByteBuffer.allocateDirect(idx.length * Short.SIZE / 8)
				.order(ByteOrder.nativeOrder()).asShortBuffer();
		indices.put(idx);
		((Buffer)indices).position(0);
		nIndices = idx.length;

		angle = (float)Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
		origin.set(0, 0);
		point(from);
		visible = false;
	}

	//강철색 몸체 + 밝은 날을 함께 날린다. onArrive는 끝이 목표에 닿을 때 한 번.
	public static void fire( Group parent, PointF from, PointF to, float delay, Runnable onArrive ){
		ThrustTip body = new ThrustTip(from, to, 9, 3.2f, 6, 0.09f, 0.18f, delay);
		body.hardlight(0x8E9AA6); //강철
		body.onArrive = onArrive;
		parent.add(body);

		ThrustTip edge = new ThrustTip(from, to, 8, 0.9f, 0, 0.09f, 0.14f, delay);
		edge.hardlight(0xF2F6FA); //날
		parent.add(edge);
	}

	@Override
	public void update() {
		super.update();

		if (delay > 0){
			delay -= Game.elapsed;
			return;
		}
		visible = true;

		time += Game.elapsed;
		if (time < travelTime){
			float p = time / travelTime;
			//처음엔 빠르고 끝에서 살짝 느려진다
			p = 1f - (1f - p) * (1f - p);
			x = from.x + (to.x - from.x) * p;
			y = from.y + (to.y - from.y) * p;
			alpha(1f);
		} else {
			x = to.x;
			y = to.y;
			if (!arrived){
				arrived = true;
				if (onArrive != null) onArrive.run();
			}
			float f = (time - travelTime) / fadeTime;
			if (f >= 1f){
				killAndErase();
			} else {
				alpha(1f - f);
			}
		}
	}

	@Override
	public void draw() {
		super.draw();

		Blending.setNormalMode();
		NoosaScript script = NoosaScript.get();
		texture.bind();
		script.uModel.valueM4( matrix );
		script.lighting( rm, gm, bm, am, ra, ga, ba, aa );
		script.camera( camera );
		script.drawElements( vertices, indices, nIndices );
	}
}
