package com.mygdx.game.components;

import com.artemis.Component;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.Agents;

/**
 * Created by EvilEntity on 24/02/2017.
 */
public class Agent extends Component implements Steerable<Vector2>  {
	public Vector2 position = new Vector2();
	public Vector2 linearVelocity = new Vector2();
	public float angularVelocity;
	public float boundingRadius;
	public float rotation;
	public boolean tagged;

	public float maxLinearSpeed = 100;
	public float maxLinearAcceleration = 200;
	public float maxAngularSpeed = 5;
	public float maxAngularAcceleration = 10;

	public float width = 1;
	public float height = 1;

	@Override
	public Vector2 getPosition () {
		return position;
	}

	@Override
	public float getOrientation () {
		return rotation * MathUtils.degreesToRadians;
	}

	@Override
	public void setOrientation (float orientation) {
		rotation = orientation * MathUtils.radiansToDegrees;
	}

	@Override
	public Vector2 getLinearVelocity () {
		return linearVelocity;
	}

	@Override
	public float getAngularVelocity () {
		return angularVelocity;
	}

	public void setAngularVelocity (float angularVelocity) {
		this.angularVelocity = angularVelocity;
	}

	@Override
	public float getBoundingRadius () {
		return boundingRadius;
	}

	@Override
	public boolean isTagged () {
		return tagged;
	}

	@Override
	public void setTagged (boolean tagged) {
		this.tagged = tagged;
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

	@Override
	public float getMaxLinearSpeed () {
		return maxLinearSpeed;
	}

	@Override
	public void setMaxLinearSpeed (float maxLinearSpeed) {
		this.maxLinearSpeed = maxLinearSpeed;
	}

	@Override
	public float getMaxLinearAcceleration () {
		float scale = getLinearVelocity().len()/getMaxLinearSpeed();
		return maxLinearAcceleration * (.1f + scale * .9f);
	}

	@Override
	public void setMaxLinearAcceleration (float maxLinearAcceleration) {
		this.maxLinearAcceleration = maxLinearAcceleration;
	}

	@Override
	public float getMaxAngularSpeed () {
		return maxAngularSpeed;
	}

	@Override
	public void setMaxAngularSpeed (float maxAngularSpeed) {
		this.maxAngularSpeed = maxAngularSpeed;
	}

	@Override
	public float getMaxAngularAcceleration () {
		return maxAngularAcceleration;
	}

	@Override
	public void setMaxAngularAcceleration (float maxAngularAcceleration) {
		this.maxAngularAcceleration = maxAngularAcceleration;
	}

	@Override
	public float getZeroLinearSpeedThreshold () {
		return 0.001f;
	}

	@Override
	public void setZeroLinearSpeedThreshold (float value) {
		throw new UnsupportedOperationException();
	}
}
