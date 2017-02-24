package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.*;
import com.badlogic.gdx.ai.steer.proximities.RadiusProximity;
import com.badlogic.gdx.ai.steer.utils.Path;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
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

	Array<Agent> activeAgents = new Array<>();

	public Agents () {
		super(Aspect.all(Transform.class, AI.class, Agent.class));
	}

	@Override protected void initialize () {

	}

	@Override protected void begin () {
		shapes.setProjectionMatrix(camera.combined);
	}

	@Override protected void inserted (int entityId) {
		activeAgents.add(mAgent.get(entityId));
	}

	private Vector2 v2 = new Vector2();
	private final SteeringAcceleration<Vector2> steeringOutput = new SteeringAcceleration<Vector2>(new Vector2());
	@Override protected void process (int entityId) {
		Transform tf = mTransform.get(entityId);
		AI ai = mAI.get(entityId);
		Agent agent = mAgent.get(entityId);

		if (ai.steering != null) {

			// Calculate steering acceleration
			ai.steering.calculateSteering(steeringOutput);

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
			shapes.circle(tf.x, tf.y, agent.boundingRadius * 1.2f, 16);
		}
		shapes.setColor(Color.LIGHT_GRAY);
		shapes.circle(tf.x, tf.y, agent.boundingRadius, 16);
		v2.set(0, 1).rotateRad(agent.getOrientation()).limit(.3f);
		shapes.setColor(Color.DARK_GRAY);
		shapes.rectLine(tf.x, tf.y, tf.x + v2.x, tf.y + v2.y, .1f);
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(Color.LIGHT_GRAY);
		shapes.rect(tf.gx - .5f, tf.gy - .5f, agent.width, agent.height);

		if (ai.steering != null) {
			drawDebug(tf, ai.steering);
		}
		shapes.end();
	}

	@Override protected void removed (int entityId) {
		activeAgents.removeValue(mAgent.get(entityId), true);
	}

	private void drawDebug (Transform tf, SteeringBehavior<Vector2> behavior) {
		if (behavior instanceof MyBlendedSteering) {
			MyBlendedSteering blendedSteering = (MyBlendedSteering)behavior;
			for (int i = 0; i < blendedSteering.getCount(); i++) {
				drawDebug(tf, blendedSteering.get(i).getBehavior());
			}
		} else if (behavior instanceof FollowPath) {
			FollowPath<Vector2, LinePath.LinePathParam> fp = (FollowPath<Vector2, LinePath.LinePathParam>)behavior;
			shapes.setColor(Color.CYAN);
			Vector2 tp = fp.getInternalTargetPosition();
			shapes.circle(tp.x, tp.y, .2f, 16);
		} else if (behavior instanceof LookWhereYouAreGoing) {
			LookWhereYouAreGoing lwyag = (LookWhereYouAreGoing)behavior;

		} else if (behavior instanceof CollisionAvoidance) {
			CollisionAvoidance ca = (CollisionAvoidance)behavior;
			// ffs private things :/
			try {
				Field field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstNeighbor");
				field.setAccessible(true);
				Steerable<Vector2> firstNeighbor = (Steerable<Vector2>)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstMinSeparation");
				field.setAccessible(true);
				float firstMinSeparation = (Float)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstDistance");
				field.setAccessible(true);
				float firstDistance = (Float)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstRelativePosition");
				field.setAccessible(true);
				Vector2 firstRelativePosition = (Vector2)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "firstRelativeVelocity");
				field.setAccessible(true);
				Vector2 firstRelativeVelocity = (Vector2)field.get(ca);
				field = ClassReflection.getDeclaredField(CollisionAvoidance.class, "relativePosition");
				field.setAccessible(true);
				Vector2 relativePosition = (Vector2)field.get(ca);
				shapes.setColor(Color.RED);
				if (firstNeighbor != null) {
					Vector2 fp = firstNeighbor.getPosition();
					shapes.circle(fp.x, fp.y, .3f, 16);
					shapes.circle(fp.x, fp.y, .35f, 16);
					shapes.circle(fp.x, fp.y, .36f, 16);
				}

				shapes.setColor(Color.MAGENTA);
				v2.set(relativePosition).scl(.1f);
				shapes.line(tf.x, tf.y, tf.x + v2.x, tf.y + v2.y);

			} catch (ReflectionException e) {
				e.printStackTrace();
			}
			RadiusProximity proximity = (RadiusProximity)ca.getProximity();
			float radius = proximity.getRadius();
			shapes.setColor(Color.ORANGE);

			shapes.circle(tf.x, tf.y, radius, 32);

		} else if (behavior instanceof Arrive) {
			Arrive arrive = (Arrive)behavior;
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
				final Agent agent = mAgent.get(selectedId);
				final AI ai = mAI.get(selectedId);
				ai.path = convertPath(path);
				final MyFollowPath followPath = new MyFollowPath(agent, ai.path);
				followPath.setCallback(new MyFollowPath.Callback() {
					@Override public void arrived () {
						Gdx.app.log(TAG, "Arrived");
						ai.steering.remove(followPath);
						Location<Vector2> location = ai.arrive.getTarget();
						location.getPosition().set(agent.getPosition());
						location.setOrientation(agent.getOrientation());
						ai.steering.add(ai.arrive, 1);
					}
				});
				followPath
					.setTimeToTarget(0.15f)
					.setPathOffset(.3f)
					.setPredictionTime(.2f)
					.setArrivalTolerance(0.01f)
					.setArriveEnabled(true)
					.setDecelerationRadius(.66f);
				ai.steering.remove(ai.arrive);
				// NOTE if we interrupt running follow path we need to remove it
				MyBlendedSteering blendedSteering = ai.steering;
				for (int i = blendedSteering.getCount() -1; i >= 0; i--) {
					BlendedSteering.BehaviorAndWeight<Vector2> baw = blendedSteering.get(i);
					if (baw.getBehavior() instanceof MyFollowPath) {
						blendedSteering.remove(baw);
					}
				}
				ai.steering.setWeight(ai.avoidance, 5);
				ai.steering.add(followPath, 1);
			}

			@Override public void notFound () {
				AI ai = mAI.get(selectedId);
				ai.path = null;
			}
		});
		// follow path
	}

	private void trySpawnAt (int x, int y, float size) {
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
		agent.boundingRadius = size;
		agent.getPosition().set(tf.x, tf.y);

		AI ai = mAI.create(agentId);
		Location<Vector2> location = agent.newLocation();
		location.getPosition().set(agent.getPosition());
		location.setOrientation(agent.getOrientation());
		Arrive<Vector2> arrive = new Arrive<>(agent, location);
		arrive.setTimeToTarget(.15f);
		arrive.setArrivalTolerance(.01f);
		arrive.setDecelerationRadius(.66f);

		ai.arrive = arrive;

		MyBlendedSteering blendedSteering = new MyBlendedSteering(agent);

		// radius must be large enough when compared to agents bounding radiys
		CollisionAvoidance<Vector2> avoidance = new CollisionAvoidance<>(agent,
			new RadiusProximity<>(agent, activeAgents, size * 3f));
		ai.avoidance = avoidance;
		blendedSteering.add(avoidance, 5);
		LookWhereYouAreGoing<Vector2> lookWhereYouAreGoing = new LookWhereYouAreGoing<>(agent);
		blendedSteering.add(lookWhereYouAreGoing, 1);
		blendedSteering.add(arrive, 1);
		ai.steering = blendedSteering;
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
			trySpawnAt(x, y, .2f);
		} break;
		case Input.Keys.W: {
			trySpawnAt(x, y, .5f);
		}break;
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

	public static class MyFollowPath extends FollowPath<Vector2, LinePath.LinePathParam> {
		private Callback callback;
		public MyFollowPath (Steerable<Vector2> owner, Path<Vector2, LinePath.LinePathParam> path) {
			super(owner, path);
		}

		public MyFollowPath (Steerable<Vector2> owner, Path<Vector2, LinePath.LinePathParam> path, float pathOffset) {
			super(owner, path, pathOffset);
		}

		public MyFollowPath (Steerable<Vector2> owner, Path<Vector2, LinePath.LinePathParam> path, float pathOffset,
			float predictionTime) {
			super(owner, path, pathOffset, predictionTime);
		}

		@Override protected SteeringAcceleration<Vector2> calculateRealSteering (SteeringAcceleration<Vector2> steering) {
			steering = super.calculateRealSteering(steering);
			if (steering.isZero()) {
				if (callback != null) {
					callback.arrived();
				}
			}
			return steering;
		}

		public MyFollowPath setCallback (Callback callback) {
			this.callback = callback;
			return this;
		}

		public interface Callback {
			void arrived();
		}
	}

	public static class MyBlendedSteering extends BlendedSteering<Vector2> {

		/**
		 * Creates a {@code BlendedSteering} for the specified {@code owner}, {@code maxLinearAcceleration} and
		 * {@code maxAngularAcceleration}.
		 *
		 * @param owner the owner of this behavior.
		 */
		public MyBlendedSteering (Steerable<Vector2> owner) {
			super(owner);
		}

		public int getCount() {
			return list.size;
		}

		public void setWeight(SteeringBehavior behavior, float weight) {
			for (BehaviorAndWeight<Vector2> behaviorAndWeight : list) {
				if (behaviorAndWeight.getBehavior() == behavior) {
					behaviorAndWeight.setWeight(weight);
				}
			}
		}
	}
}
