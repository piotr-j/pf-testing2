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
import com.badlogic.gdx.ai.utils.ArithmeticUtils;
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

	private Vector2 v2_1 = new Vector2();
	private Vector2 v2_2 = new Vector2();
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
			if (!agent.getLinearVelocity().isZero(agent.getZeroLinearSpeedThreshold()) && ai.path != null) {
				float orientation = vectorToAngle(agent.getLinearVelocity());
				orientation = Step.of(orientation * MathUtils.radDeg).angle;
				agent.targetOrientation = orientation * MathUtils.degRad;
				Gdx.app.log(TAG, "target " + (orientation));
			}

			if (!MathUtils.isZero(agent.getAngularVelocity()) || !MathUtils.isZero(steeringOutput.angular)) {
				agent.setOrientation(agent.getOrientation() + (agent.getAngularVelocity() * world.delta));
				agent.setAngularVelocity(agent.getAngularVelocity() * 0.98f + steeringOutput.angular * world.delta);
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
			shapes.setColor(Color.FIREBRICK);
			float size = (agent.clearance)/2f-.3f;
			shapes.rect(tf.x - size, tf.y -size,
				size, size,
				size * 2, size * 2,
				1, 1,
				agent.getOrientation() * MathUtils.radDeg
			);
		}

		v2_1.set(0, 1).rotateRad(agent.getOrientation()).limit(.3f);
		v2_2.set(1, 1).rotateRad(agent.targetOrientation).limit(.5f);

		switch (agent.clearance) {
		case 1: {
			shapes.setColor(Color.LIGHT_GRAY);
		}break;
		case 2: {
			shapes.setColor(Color.DARK_GRAY);
		}break;
		case 3: {
			shapes.setColor(Color.BLACK);
		}break;
		}
		float size = (agent.clearance)/2f-.4f;
		shapes.rect(tf.x - size, tf.y -size,
			size, size,
			size * 2, size * 2,
			1, 1,
			agent.getOrientation() * MathUtils.radDeg
		);

		shapes.setColor(Color.MAGENTA);
		shapes.rectLine(tf.x, tf.y, tf.x + v2_2.x, tf.y + v2_2.y, .15f);
//		shapes.circle(tf.x, tf.y, size, 16);
		shapes.setColor(Color.DARK_GRAY);
		shapes.rectLine(tf.x, tf.y, tf.x + v2_1.x, tf.y + v2_1.y, .1f);


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
				v2_1.set(relativePosition).scl(.1f);
				shapes.line(tf.x, tf.y, tf.x + v2_1.x, tf.y + v2_1.y);

			} catch (ReflectionException e) {
				e.printStackTrace();
			}
			RadiusProximity proximity = (RadiusProximity)ca.getProximity();
			float radius = proximity.getRadius();
			shapes.setColor(Color.ORANGE);

			shapes.circle(tf.x, tf.y, radius, 32);

		} else if (behavior instanceof Arrive) {
			Arrive arrive = (Arrive)behavior;
		} else if (behavior instanceof ReachOrientation) {
			ReachOrientation ro = (ReachOrientation)behavior;
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
		final Agent agent = mAgent.get(selectedId);
		float offset = (agent.clearance-1f)/2f;
		pf.findPath(Map.grid(tf.gx - offset), Map.grid(tf.gy - offset), Map.grid(x - offset), Map.grid(y-offset), agent.clearance, new Pathfinding.PFCallback() {
			@Override public void found (Pathfinding.NodePath path) {
				final AI ai = mAI.get(selectedId);
				ai.path = convertPath(path);
				ai.followPath.update(ai.path);
				ai.steering = ai.steeringPath;
			}

			@Override public void notFound () {
				AI ai = mAI.get(selectedId);
				ai.path = null;
				Gdx.app.log(TAG, "path not found");
			}
		});
		// follow path
	}

	private void trySpawnAt (int x, int y, int size) {
		Map.Node at = map.at(x, y);
		if (at == null || at.type == Map.WL) return;
		IntBag entityIds = getEntityIds();
		for (int i = 0; i < entityIds.size(); i++) {
			Transform tf = mTransform.get(entityIds.get(i));
			if (tf.gx == x && tf.gy == y) return;
		}
		int agentId = world.create();
		Transform tf = mTransform.create(agentId);
		tf.xy(x, y);
		final Agent agent = mAgent.create(agentId);
		agent.setMaxAngularAcceleration(360 * MathUtils.degreesToRadians);
		agent.setMaxAngularSpeed(90 * MathUtils.degreesToRadians);
		agent.setMaxLinearAcceleration(20);
		agent.setMaxLinearSpeed(2);
		agent.boundingRadius = .3f;
		agent.getPosition().set(tf.x, tf.y);
		agent.clearance = size;

		final AI ai = mAI.create(agentId);
		ai.target.getPosition().set(agent.getPosition());
		ai.target.setOrientation(agent.getOrientation());

//		ai.arrive = arrive;

		MyBlendedSteering steeringIdle = new MyBlendedSteering(agent);

		// radius must be large enough when compared to agents bounding radiys
		CollisionAvoidance<Vector2> avoidance = new CollisionAvoidance<>(agent,
			new RadiusProximity<>(agent, activeAgents, .2f));
//		ai.avoidance = avoidance;
		steeringIdle.add(avoidance, 5);

		ReachOrientation<Vector2> reachOrientation = new ReachOrientation<>(agent);
		reachOrientation.setTarget(ai.target);
		reachOrientation.setAlignTolerance(1 * MathUtils.degRad);
		reachOrientation.setDecelerationRadius(MathUtils.PI/4);
		reachOrientation.setTimeToTarget(.15f);
		steeringIdle.add(reachOrientation, 1);

		Arrive<Vector2> arrive = new Arrive<>(agent, ai.target);
		arrive.setTimeToTarget(.15f);
		arrive.setArrivalTolerance(.01f);
		arrive.setDecelerationRadius(.66f);
		steeringIdle.add(arrive, 1);

		ai.steeringIdle = steeringIdle;

		MyBlendedSteering steeringPath = new MyBlendedSteering(agent);
		steeringPath.add(avoidance, 1);

		LookWhereYouAreGoing<Vector2> lookWhereYouAreGoing = new LookWhereYouAreGoing<>(agent);
		lookWhereYouAreGoing.setAlignTolerance(1 * MathUtils.degRad);
		lookWhereYouAreGoing.setDecelerationRadius(MathUtils.PI/4);
		lookWhereYouAreGoing.setTimeToTarget(.15f);
		steeringPath.add(lookWhereYouAreGoing, 1);

		final MyFollowPath followPath = new MyFollowPath(agent);
		followPath.setCallback(new MyFollowPath.Callback() {
			@Override public void arrived () {
				Gdx.app.log(TAG, "Arrived");
				Location<Vector2> location = ai.target;
				location.getPosition().set(agent.getPosition());
				location.setOrientation(agent.targetOrientation);
				ai.steering = ai.steeringIdle;
				ai.path = null;
			}
		});
		followPath
			.setTimeToTarget(0.15f)
			.setPathOffset(.3f)
			.setPredictionTime(.2f)
			.setArrivalTolerance(0.01f)
			.setArriveEnabled(true)
			.setDecelerationRadius(.66f);

		ai.followPath = followPath;

		steeringPath.add(followPath, 1);

		ai.steeringPath = steeringPath;


	}

	private Path<Vector2, LinePath.LinePathParam> convertPath (Pathfinding.NodePath path) {
		float offset = (path.clearance-1)/2f;
		Array<Vector2> wayPoints = new Array<>();
		for (int i = 0; i < path.getCount(); i++) {
			Map.Node node = path.get(i);
			wayPoints.add(new Vector2(node.x + offset, node.y + offset));
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
			trySpawnAt(x, y, 1);
		} break;
		case Input.Keys.W: {
			trySpawnAt(x, y, 2);
		}break;
		case Input.Keys.E: {
			trySpawnAt(x, y, 3);
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
		private static final LinePath<Vector2> dummy = new LinePath<>(new Array<>(new Vector2[]{new Vector2(), new Vector2(0, 1)}));
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

		public MyFollowPath (Agent agent) {
			super(agent, dummy);
		}

		@Override protected SteeringAcceleration<Vector2> calculateRealSteering (SteeringAcceleration<Vector2> steering) {
			steering = super.calculateRealSteering(steering);
			if (steering.isZero() && path.getEndPoint().epsilonEquals(owner.getPosition(), 0.01f)) {
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

		public void update (Path<Vector2, LinePath.LinePathParam> path) {
			this.path = path;
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

	// counterclockwise, 0 east, 90 north, -90 south
	public enum Step {E(0), SE(-45), S(-90), SW(-135), W(180), NW(135), N(90), NE(45);
		public final int angle;
		private static Step[] cache = new Step[360];
		static {
			Step[] values = values();
			for (int i = 0; i < 360; i++) {
				cache[i] = W;
				for (Step value : values) {
					if (Math.abs(i - value.angle - 180) <= 22.5f){
						cache[i] = value;
						break;
					}
				}
			}
		}
		Step (int angle) {
			this.angle = angle;
		}
		public static Step of(float angle) {
			return cache[((int)normalize(angle)) + 180];
		}
	}

	public static float normalize(float angle) {
		return angle - 360f * MathUtils.floor((angle + 180) / 360f);
	}
}
