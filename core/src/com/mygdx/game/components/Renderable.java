package com.mygdx.game.components;

import com.artemis.Component;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class Renderable extends Component {
	public static final int SHAPE_RECT = 0;
	public static final int SHAPE_CIRCLE = 1;
	public int shape;
	public ShapeRenderer.ShapeType renderType = ShapeRenderer.ShapeType.Filled;
}
