package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.FollowPath;
import com.badlogic.gdx.ai.steer.utils.Path;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
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

	@Wire Map map;
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

	private Vector2 v2 = new Vector2();
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

		// TODO handle size
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		if (ai.path != null) {
			LinePath<Vector2> path = (LinePath<Vector2>)ai.path;
			Array<LinePath.Segment<Vector2>> segments = path.getSegments();
			float step = 1f /(segments.size-1);
			float c = 0;
			for (int i = 0; i < segments.size; i++) {
				shapes.setColor(c, c, c, 1);
				LinePath.Segment<Vector2> segment = segments.get(i);
				shapes.rectLine(segment.getBegin().x, segment.getBegin().y, segment.getEnd().x, segment.getEnd().y, .075f);
				c += step;
			}
		}
		if (selectedId == entityId) {
			shapes.setColor(Color.DARK_GRAY);
			shapes.circle(tf.x, tf.y, .4f, 16);
		}
		shapes.setColor(Color.LIGHT_GRAY);
		shapes.circle(tf.x, tf.y, .3f, 16);
		v2.set(1, 0).rotateRad(agent.getOrientation()).limit(.3f);
		shapes.setColor(Color.DARK_GRAY);
		shapes.rectLine(tf.x, tf.y, tf.x + v2.x, tf.y + v2.y, .1f);
		shapes.end();
		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(Color.LIGHT_GRAY);
		shapes.rect(tf.gx - .5f, tf.gy - .5f, agent.width, agent.height);

		if (ai.behavior != null) {
			drawDebug(ai.behavior);
		}
		shapes.end();
	}

	private void drawDebug (SteeringBehavior<Vector2> behavior) {
		if (behavior instanceof FollowPath) {
			FollowPath<Vector2, LinePath.LinePathParam> fp = (FollowPath<Vector2, LinePath.LinePathParam>)behavior;
			shapes.setColor(Color.CYAN);
			Vector2 tp = fp.getInternalTargetPosition();
			shapes.circle(tp.x, tp.y, .2f, 16);
		} else {
			Gdx.app.log(TAG, "Not supported behaviour type " + behavior.getClass());
		}
	}

	@Override protected void end () {

	}

	int selectedId = -1;
	@Override protected void touchDownLeft (float x, float y) {
		selectedId = agentAt(x, y);
	}

	@Override protected void touchDownRight (float x, float y) {
		if (selectedId == -1) return;
		// go to spot?
		// find path
		Transform tf = mTransform.get(selectedId);
		pf.findPath(tf.gx, tf.gy, Map.grid(x), Map.grid(y), new Pathfinding.PFCallback() {
			@Override public void found (Pathfinding.NodePath path) {
				Agent agent = mAgent.get(selectedId);
				AI ai = mAI.get(selectedId);
				ai.path = convertPath(path);
				FollowPath<Vector2, LinePath.LinePathParam> followPath = new FollowPath<>(agent, ai.path);
				followPath
					.setTimeToTarget(0.15f)
					.setPathOffset(.3f)
					.setPredictionTime(.2f)
					.setArrivalTolerance(0.01f)
					.setArriveEnabled(true)
					.setDecelerationRadius(.66f);
				ai.behavior = followPath;
			}

			@Override public void notFound () {
				AI ai = mAI.get(selectedId);
				ai.path = null;
			}
		});
		// follow path
	}

	private void trySpawnAt (int x, int y) {
		Map.Node at = map.at(x, y);
		if (at == null || at.type == Map.WL) return;
		int agentId = world.create();
		Transform tf = mTransform.create(agentId);
		tf.xy(x, y);
		Agent agent = mAgent.create(agentId);
		agent.setMaxAngularAcceleration(90 * MathUtils.degreesToRadians);
		agent.setMaxAngularSpeed(45 * MathUtils.degreesToRadians);
		agent.setMaxLinearAcceleration(20);
		agent.setMaxLinearSpeed(2);
		agent.boundingRadius = .4f;
		agent.getPosition().set(tf.x, tf.y);

		AI ai = mAI.create(agentId);
	}

	private Path<Vector2, LinePath.LinePathParam> convertPath (Pathfinding.NodePath path) {
		Array<Vector2> wayPoints = new Array<>();
		for (int i = 0; i < path.getCount(); i++) {
			Map.Node node = path.get(i);
			wayPoints.add(new Vector2(node.x, node.y));
		}
		return new LinePath<>(wayPoints, true);
	}

	private Circle c = new Circle();
	int agentAt(float x, float y) {
		IntBag entityIds = getEntityIds();
		int[] data = entityIds.getData();
		for (int i = 0; i < entityIds.size(); i++) {
			Transform tf = mTransform.get(data[i]);
			c.set(tf.x, tf.y, .4f);
			if (c.contains(x, y)) {
				return data[i];
			}
		}
		return -1;
	}

	@Override public boolean keyDown (int keycode) {
		int x = Map.grid(tmp.x);
		int y = Map.grid(tmp.y);
		switch (keycode) {
		case Input.Keys.Q: {
			trySpawnAt(x, y);
		}
		break;
		}
		return false;
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
