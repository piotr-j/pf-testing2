package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.mygdx.game.components.AI;
import com.mygdx.game.components.Agent;
import com.mygdx.game.components.Transform;

/**
 * Created by EvilEntity on 24/02/2017.
 */
public class Agents extends IteratingInputSystem {
	private static final String TAG = Agents.class.getSimpleName();
	protected ComponentMapper<Transform> mTransform;
	protected ComponentMapper<AI> mAI;
	protected ComponentMapper<Agent> mAgent;

	@Wire Pathfinding pf;
	@Wire ShapeRenderer shapes;

	public Agents () {
		super(Aspect.all(Transform.class, AI.class, Agent.class));
	}

	@Override protected void initialize () {

	}

	@Override protected void begin () {
		shapes.setProjectionMatrix(camera.combined);
	}

	private final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<Vector2>(new Vector2());
	@Override protected void process (int entityId) {
		Transform tf = mTransform.get(entityId);
		AI ai = mAI.get(entityId);
		Agent agent = mAgent.get(entityId);

		if (ai.behavior != null) {

			// Calculate steering acceleration
			ai.behavior.calculateSteering(steeringOutput);

			agent.getPosition().mulAdd(agent.getLinearVelocity(), world.delta);
			agent.getLinearVelocity().mulAdd(steeringOutput.linear, world.delta).limit(agent.getMaxLinearSpeed());

			// If we haven't got any velocity, then we can do nothing.
			if (!agent.getLinearVelocity().isZero(agent.getZeroLinearSpeedThreshold())) {
				float newOrientation = vectorToAngle(agent.getLinearVelocity());
				agent.setAngularVelocity((newOrientation - agent.getOrientation()) * world.delta);
				agent.setOrientation(newOrientation);
			}
			tf.xy(agent.getPosition().x, agent.getPosition().y);
		}

		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.end();
		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.end();
	}

	@Override protected void end () {

	}

	public static float vectorToAngle (Vector2 vector) {
		return (float)Math.atan2(-vector.x, vector.y);
	}

	public static Vector2 angleToVector (Vector2 outVector, float angle) {
		outVector.x = -(float)Math.sin(angle);
		outVector.y = (float)Math.cos(angle);
		return outVector;
	}
}
