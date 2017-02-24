package com.mygdx.game;

import com.artemis.BaseSystem;
import com.artemis.World;
import com.artemis.WorldConfiguration;
import com.badlogic.gdx.*;
import com.badlogic.gdx.ai.GdxAI;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class MyGdxGame extends ApplicationAdapter {
	public final static float SCALE = 32f;
	public final static float INV_SCALE = 1.f/SCALE;
	public final static float WIDTH = 800 * INV_SCALE;
	public final static float HEIGHT = 600 * INV_SCALE;

	OrthographicCamera camera;
	ExtendViewport viewport;
	SpriteBatch batch;
	ShapeRenderer shapes;

	World world;

	@Override
	public void create () {
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(WIDTH, HEIGHT, camera);
		camera.translate(-.5f, -.5f);
		camera.update();
		batch = new SpriteBatch();
		shapes = new ShapeRenderer();

		WorldConfiguration config = new WorldConfiguration();
		config.register(batch);
		config.register(shapes);
		config.register(camera);

		config.setSystem(new Gods());
		config.setSystem(Map.class);
		config.setSystem(Pathfinding.class);
//		config.setSystem(TransformRenderer.class);
//		config.setSystem(Shapes.class);

		world = new World(config);

		InputMultiplexer multiplexer = new InputMultiplexer();
		for (BaseSystem baseSystem : world.getSystems()) {
			if (baseSystem instanceof InputSystem) {
				multiplexer.addProcessor((InputProcessor)baseSystem);
			}
		}
		Gdx.input.setInputProcessor(multiplexer);

	}

	@Override
	public void render () {
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glClearColor(.4f, .4f, .4f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		world.delta = Gdx.graphics.getDeltaTime();
		GdxAI.getTimepiece().update(world.delta);
		world.process();
	}

	@Override public void resize (int width, int height) {
		viewport.update(width, height, true);
		camera.translate(-.5f, -.5f);
		camera.update();
	}

	@Override
	public void dispose () {
		world.dispose();
		shapes.dispose();
		batch.dispose();
	}

	public static class Gods extends InputSystem {

		@Override protected void initialize () {

		}

		@Override protected void processSystem () {

		}
	}
}

