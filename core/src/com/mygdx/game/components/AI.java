package com.mygdx.game.components;

import com.artemis.Component;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.ai.steer.utils.Path;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.Agents;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class AI extends Component {
	public Agents.MyBlendedSteering steering;
	public Arrive<Vector2> arrive;

	public Path<Vector2, LinePath.LinePathParam> path;
}
