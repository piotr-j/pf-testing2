package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.mygdx.game.components.Renderable;
import com.mygdx.game.components.Transform;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class Shapes extends IteratingSystem {
	private static final String TAG = Shapes.class.getSimpleName();
	protected ComponentMapper<Renderable> mRenderable;

	@Wire OrthographicCamera camera;
	@Wire ShapeRenderer shapes;

	public Shapes () {
		super(Aspect.all(Transform.class, Renderable.class));
	}

	private ShapeRenderer.ShapeType current;
	@Override protected void begin () {
		shapes.setProjectionMatrix(camera.combined);
		current = ShapeRenderer.ShapeType.Filled;
		shapes.begin(current);
	}

	@Override protected void process (int entityId) {
		Renderable renderable = mRenderable.get(entityId);
		if (renderable.renderType != current) {
			shapes.end();
			current = renderable.renderType;
			shapes.begin(current);
		}

		switch (renderable.shape) {
		case Renderable.SHAPE_RECT: {

		} break;
		case Renderable.SHAPE_CIRCLE: {

		} break;
		}
	}

	@Override protected void end () {
		shapes.end();
	}

}
