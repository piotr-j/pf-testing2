package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.BaseSystem;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public abstract class IteratingInputSystem extends IteratingSystem implements InputProcessor {
	@Wire OrthographicCamera camera;
	Vector3 tmp = new Vector3();


	public IteratingInputSystem (Aspect.Builder aspect) {
		super(aspect);
	}

	@Override public boolean keyDown (int keycode) {
		return false;
	}

	@Override public boolean keyUp (int keycode) {
		return false;
	}

	@Override public boolean keyTyped (char character) {
		return false;
	}

	@Override public boolean touchDown (int screenX, int screenY, int pointer, int button) {
		camera.unproject(tmp.set(screenX, screenY, 0));
		switch (button) {
		case Input.Buttons.LEFT: {
			touchDownLeft(tmp.x, tmp.y);
		} break;
		case Input.Buttons.MIDDLE: {
			touchDownMiddle(tmp.x, tmp.y);
		} break;
		case Input.Buttons.RIGHT: {
			touchDownRight(tmp.x, tmp.y);
		} break;
		}
		return false;
	}

	protected void touchDownLeft (float x, float y) {}
	protected void touchDownMiddle (float x, float y) {}
	protected void touchDownRight (float x, float y) {}

	@Override public boolean touchUp (int screenX, int screenY, int pointer, int button) {
		camera.unproject(tmp.set(screenX, screenY, 0));
		switch (button) {
		case Input.Buttons.LEFT: {
			touchUpLeft(tmp.x, tmp.y);
		} break;
		case Input.Buttons.MIDDLE: {
			touchUpMiddle(tmp.x, tmp.y);
		} break;
		case Input.Buttons.RIGHT: {
			touchUpRight(tmp.x, tmp.y);
		} break;
		}
		return true;
	}

	protected void touchUpLeft (float x, float y) {}
	protected void touchUpMiddle (float x, float y) {}
	protected void touchUpRight (float x, float y) {}

	@Override public boolean touchDragged (int screenX, int screenY, int pointer) {
		return false;
	}

	@Override public boolean mouseMoved (int screenX, int screenY) {
		camera.unproject(tmp.set(screenX, screenY, 0));
		return false;
	}

	@Override public boolean scrolled (int amount) {
		return false;
	}
}
