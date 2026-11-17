/*
 * Maze Tower (가칭 미궁의 마탑)
 * Shattered Pixel Dungeon 기반 아종. GPLv3.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

//Maze Tower: 검 스킬의 베기 궤적.
//가운데가 두껍고 양끝이 얇은 초승달 모양 띠를 빠르게 그린 뒤(draw) 서서히 사라진다(fade).
//원작 CircleArc의 그리기 방식을 따르되, 부채꼴이 아니라 가는 띠를 그린다.
public class SlashTrail extends Visual {

	private static final int SEGMENTS = 32; //동그라미 베기도 매끄럽게

	private final float radius;
	private final float thickness;
	private final float startAngle; //라디안
	private final float sweepAngle; //라디안, 음수면 반대 방향으로 그린다

	private float delay;
	private final float drawTime;
	private final float fadeTime;
	private float time = 0;
	private Runnable onStart;
	private boolean started = false;

	private final SmartTexture texture;
	private final FloatBuffer verticesBuffer;
	private final ShortBuffer indices;
	private final float[] vertex = new float[4];
	private int nIndices = 0;

	public SlashTrail( float radius, float thickness, float startAngle, float sweepAngle,
	                   float delay, float drawTime, float fadeTime ){
		super(0, 0, 0, 0);
		this.radius = radius;
		this.thickness = thickness;
		this.startAngle = startAngle;
		this.sweepAngle = sweepAngle;
		this.delay = delay;
		this.drawTime = drawTime;
		this.fadeTime = fadeTime;

		texture = TextureCache.createSolid(0xFFFFFFFF);

		verticesBuffer = ByteBuffer.allocateDirect((SEGMENTS + 1) * 2 * 4 * (Float.SIZE / 8))
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		indices = ByteBuffer.allocateDirect(SEGMENTS * 6 * Short.SIZE / 8)
				.order(ByteOrder.nativeOrder()).asShortBuffer();

		visible = false;
	}

	public SlashTrail trailColor( int color ){
		hardlight(color);
		return this;
	}

	//true: 빛처럼 밝게 더해지는 궤적(기본). false: 어두운 색도 그대로 보이는 일반 궤적(예: 그림자검)
	private boolean glow = true;

	//동그라미(고리) 베기: 두께가 거의 일정하다
	private boolean ring = false;

	public SlashTrail ring(){
		ring = true;
		return this;
	}

	public SlashTrail solid(){
		glow = false;
		return this;
	}

	//궤적이 그려지기 시작할 때 한 번 실행 (예: 대상 번쩍임)
	public SlashTrail onStart( Runnable r ){
		onStart = r;
		return this;
	}

	//궤적이 그려지는 끝을 따라 입자를 뿌린다 (예: 반월베기 은색 가루)
	private Emitter.Factory sparkle;
	private int sparkleDone = 0;

	private int sparkleCount = 2;

	public SlashTrail sparkle( Emitter.Factory factory, int perSegment ){
		sparkleCount = perSegment;
		sparkle = factory;
		return this;
	}

	private void emitSparkles( float progress ){
		if (sparkle == null) return;
		int upto = Math.round(SEGMENTS * progress);
		for (; sparkleDone < upto; sparkleDone++){
			if (sparkleDone % 2 != 0) continue; //구간이 32개로 늘어 가루 양이 두 배가 되지 않게 한 칸 걸러 뿌린다
			float a = startAngle + sweepAngle * (sparkleDone / (float)SEGMENTS);
			Emitter e = GameScene.emitter();
			if (e == null) return;
			e.pos(x + (float)Math.cos(a) * radius, y + (float)Math.sin(a) * radius);
			e.burst(sparkle, sparkleCount);
		}
	}

	public SlashTrail show( Group parent, PointF center ){
		point(center);
		parent.add(this);
		return this;
	}

	//progress: 0~1, 띠가 시작점에서 어디까지 그려졌는지
	private void build( float progress ){
		((Buffer)verticesBuffer).position(0);
		((Buffer)indices).position(0);

		int segs = Math.max(1, Math.round(SEGMENTS * progress));
		for (int i = 0; i <= segs; i++){
			float t = i / (float)SEGMENTS;  //띠 전체에서의 위치
			float a = startAngle + sweepAngle * t;
			//가운데가 두껍고 양끝이 얇다
			//기본: 가운데가 두껍고 양끝이 얇은 초승달. 고리(ring): 시작 부분만 살짝 가늘고 나머지는 같은 두께
			float th = ring
					? thickness * Math.min(1f, 0.25f + t * 4f)
					: thickness * (float)Math.sin(Math.PI * t);
			float cos = (float)Math.cos(a), sin = (float)Math.sin(a);

			vertex[0] = cos * radius;
			vertex[1] = sin * radius;
			vertex[2] = 0.5f; vertex[3] = 0.5f;
			verticesBuffer.put(vertex);

			vertex[0] = cos * (radius - th);
			vertex[1] = sin * (radius - th);
			verticesBuffer.put(vertex);
		}
		nIndices = 0;
		for (int i = 0; i < segs; i++){
			short o0 = (short)(i * 2), i0 = (short)(i * 2 + 1);
			short o1 = (short)(i * 2 + 2), i1 = (short)(i * 2 + 3);
			indices.put(o0); indices.put(i0); indices.put(o1);
			indices.put(i0); indices.put(i1); indices.put(o1);
			nIndices += 6;
		}
		((Buffer)verticesBuffer).position(0);
		((Buffer)indices).position(0);
	}

	@Override
	public void update() {
		super.update();

		if (delay > 0){
			delay -= Game.elapsed;
			return;
		}
		if (!started){
			started = true;
			visible = true;
			if (onStart != null) onStart.run();
		}

		time += Game.elapsed;
		if (time < drawTime){
			build(time / drawTime);
			emitSparkles(time / drawTime);
			alpha(1f);
		} else if (time < drawTime + fadeTime){
			build(1f);
			emitSparkles(1f);
			alpha(1f - (time - drawTime) / fadeTime);
		} else {
			killAndErase();
		}
	}

	@Override
	public void draw() {
		super.draw();
		if (nIndices == 0) return;

		if (glow) Blending.setLightMode();

		NoosaScript script = NoosaScript.get();
		texture.bind();
		script.uModel.valueM4( matrix );
		script.lighting( rm, gm, bm, am, ra, ga, ba, aa );
		script.camera( camera );
		script.drawElements( verticesBuffer, indices, nIndices );

		if (glow) Blending.setNormalMode();
	}
}
