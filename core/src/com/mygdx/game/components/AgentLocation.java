package com.mygdx.game.components;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.Agents;

/**
 * Created by EvilEntity on 24/02/2017.
 */
public class AgentLocation implements Location<Vector2> {

	Vector2 position = new Vector2();
	float orientation = 0;

	@Override
	public Vector2 getPosition () {
		return position;
	}

	@Override
	public float getOrientation () {
		return orientation;
	}

	@Override
	public void setOrientation (float orientation) {
		this.orientation = orientation;
	}

	@Override
	public Location<Vector2> newLocation () {
		return new AgentLocation();
	}

	@Override
	public float vectorToAngle (Vector2 vector) {
		return Agents.vectorToAngle(vector);
	}

	@Override
	public Vector2 angleToVector (Vector2 outVector, float angle) {
		return Agents.angleToVector(outVector, angle);
	}

}
