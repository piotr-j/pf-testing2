package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mygdx.game.components.Renderable;
import com.mygdx.game.components.Transform;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class TransformRenderer extends IteratingSystem {
	private static final String TAG = TransformRenderer.class.getSimpleName();
	protected ComponentMapper<Transform> mTransform;

	@Wire OrthographicCamera camera;
	@Wire ShapeRenderer shapes;

	public TransformRenderer () {
		super(Aspect.all(Transform.class));
	}

	@Override protected void begin () {
		shapes.setProjectionMatrix(camera.combined);
		shapes.begin(ShapeRenderer.ShapeType.Line);
	}

	@Override protected void process (int entityId) {
		Transform tf = mTransform.get(entityId);
		shapes.setColor(Color.CYAN);
		shapes.rect(tf.gx - .5f, tf.gy - .5f, 1, 1);
		shapes.setColor(Color.GREEN);
		shapes.circle(tf.x, tf.y, .4f, 16);
	}

	@Override protected void end () {
		shapes.end();
	}

}
