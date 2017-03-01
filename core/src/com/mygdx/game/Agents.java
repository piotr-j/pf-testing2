package com.mygdx.game;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.steer.Limiter;
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
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.mygdx.game.components.AI;
import com.mygdx.game.components.Agent;
import com.mygdx.game.components.AgentLocation;
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

	boolean paused;
	float delta;
	float deltaScale = 1;
	@Override protected void begin () {
		shapes.setProjectionMatrix(camera.combined);
		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
			paused = !paused;
		}
		if (paused) {
			delta = 0;
		} else {
			delta = world.delta * deltaScale;
		}
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

			agent.getPosition().mulAdd(agent.getLinearVelocity(), delta);
			agent.getLinearVelocity().mulAdd(steeringOutput.linear, delta).limit(agent.getMaxLinearSpeed());

			// If we haven't got any velocity, then we can do nothing.
			if (!agent.getLinearVelocity().isZero(agent.getZeroLinearSpeedThreshold()) && ai.path != null) {
				float orientation = vectorToAngle(agent.getLinearVelocity());
				orientation = Step.of(orientation * MathUtils.radDeg).angle;
				agent.targetOrientation = orientation * MathUtils.degRad;
//				Gdx.app.log(TAG, "target " + (orientation));
			}

			if (!MathUtils.isZero(agent.getAngularVelocity()) || !MathUtils.isZero(steeringOutput.angular)) {
				agent.setOrientation(agent.getOrientation() + (agent.getAngularVelocity() * delta));
				agent.setAngularVelocity(agent.getAngularVelocity() * 0.98f + steeringOutput.angular * delta);
			}
			tf.xy(agent.getPosition().x, agent.getPosition().y);
		}

		if (agent.doorTimer > 0) {
			agent.doorTimer -= delta;
			if (agent.doorTimer < 0) agent.doorTimer = 0;
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
				if (i%2 == 0) {
					shapes.setColor(Color.MAGENTA);
				} else {
					shapes.setColor(Color.CYAN);
				}
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

//		shapes.setColor(Color.MAGENTA);
//		shapes.rectLine(tf.x, tf.y, tf.x + v2_2.x, tf.y + v2_2.y, .15f);
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
				if (relativePosition != null) {
					v2_1.set(relativePosition).scl(.1f);
					shapes.line(tf.x, tf.y, tf.x + v2_1.x, tf.y + v2_1.y);
				}
			} catch (ReflectionException e) {
				e.printStackTrace();
			}
			RadiusProximity proximity = (RadiusProximity)ca.getProximity();
			float radius = proximity.getRadius();
			shapes.setColor(Color.ORANGE);

			shapes.circle(tf.x, tf.y, radius, 32);

		} else if (behavior instanceof PriorityDoorArrive) {
			PriorityDoorArrive pfp = (PriorityDoorArrive)behavior;
			shapes.setColor(Color.GOLD);
			Vector2 tp = pfp.getInternalTargetPosition();
			shapes.circle(tp.x, tp.y, .15f, 16);

			shapes.setColor(Color.MAGENTA);
			Vector2 ap = pfp.getArriveTargetPosition();
			shapes.circle(ap.x, ap.y, .15f, 16);

		} else if (behavior instanceof Arrive) {
			Arrive arrive = (Arrive)behavior;
		} else if (behavior instanceof ReachOrientation) {
			ReachOrientation ro = (ReachOrientation)behavior;
		} else if (behavior instanceof MyArrive) {
			MyArrive arrive = (MyArrive)behavior;
		} else if (behavior instanceof PriorityBlendedSteering) {
			PriorityBlendedSteering pbs = (PriorityBlendedSteering)behavior;
			for (int i = 0; i < pbs.getCount(); i++) {
				drawDebug(tf, pbs.get(i).getBehavior());
			}
		} else if (behavior instanceof MyPrioritySteering) {
			MyPrioritySteering ps = (MyPrioritySteering)behavior;
			for (int i = 0; i < ps.getCount(); i++) {
				drawDebug(tf, ps.get(i));
			}
		} else if (behavior instanceof PrioritySteering) {
			PrioritySteering ps = (PrioritySteering)behavior;
		} else {
			Gdx.app.log(TAG, "Not supported behaviour type " + behavior.getClass());
		}
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
				ai.followPath.update(ai.path, path);
				ai.priorityPath.update(ai.path, path);
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
		if (at == null || at.type == Map.WL)
			return;
		IntBag entityIds = getEntityIds();
		for (int i = 0; i < entityIds.size(); i++) {
			Transform tf = mTransform.get(entityIds.get(i));
			if (tf.gx == x && tf.gy == y)
				return;
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

		PrioritySteering<Vector2> priorityIdle = new PrioritySteering<>(agent);

		MyBlendedSteering steeringIdle = new MyBlendedSteering(agent);

		// radius must be large enough when compared to agents bounding radiys
		CollisionAvoidance<Vector2> avoidance = new CollisionAvoidance<>(agent, new RadiusProximity<>(agent, activeAgents, .2f));
//		ai.avoidance = avoidance;
		priorityIdle.add(avoidance);

		ReachOrientation<Vector2> reachOrientation = new ReachOrientation<>(agent);
		reachOrientation.setTarget(ai.target);
		reachOrientation.setAlignTolerance(1 * MathUtils.degRad);
		reachOrientation.setDecelerationRadius(MathUtils.PI / 4);
		reachOrientation.setTimeToTarget(.15f);
		steeringIdle.add(reachOrientation, 1);

		Arrive<Vector2> arrive = new Arrive<>(agent, ai.target);
		arrive.setTimeToTarget(.15f);
		arrive.setArrivalTolerance(.01f);
		arrive.setDecelerationRadius(.66f);
		steeringIdle.add(arrive, 1);
		priorityIdle.add(steeringIdle);

		ai.steeringIdle = priorityIdle;

		// TODO we want custom priority steering, that will stop following path if blocked tile is encountered
		MyPrioritySteering<Vector2> priorityPath = new MyPrioritySteering<>(agent);

		PriorityBlendedSteering priorityBlendedPath = new PriorityBlendedSteering(agent);
		// TODO use follow path, with fairly large path offset so we are about 2 tiles ahead of actual position
		// if out target is at locked door, we want to arrive to previous waypoint until the door is open
		// hit the map to check type, store if door was hit, use agents timer for debug
		PriorityDoorArrive priorityDoorArrive = new PriorityDoorArrive(agent);
		priorityDoorArrive.setTimeToTarget(.15f);
		priorityDoorArrive.setArrivalTolerance(.01f);
		priorityDoorArrive.setDecelerationRadius(.66f);
		priorityDoorArrive.setPathOffset(.5f * size + .8f);
		priorityDoorArrive.setPredictionTime(0);
		priorityDoorArrive.setMap(map);
		priorityDoorArrive.setAlignTolerance(1 * MathUtils.degRad);
		priorityDoorArrive.setFaceDecelerationRadius(MathUtils.PI / 4);
		priorityDoorArrive.setFaceTimeToTarget(.15f);
		ai.priorityPath = priorityDoorArrive;
		priorityBlendedPath.add(priorityDoorArrive, 1);



		priorityPath.add(priorityBlendedPath);

		MyBlendedSteering blendedPath = new MyBlendedSteering(agent);
		blendedPath.add(avoidance, 1);

		LookWhereYouAreGoing<Vector2> lookWhereYouAreGoing = new LookWhereYouAreGoing<>(agent);
		lookWhereYouAreGoing.setAlignTolerance(1 * MathUtils.degRad);
		lookWhereYouAreGoing.setDecelerationRadius(MathUtils.PI / 4);
		lookWhereYouAreGoing.setTimeToTarget(.15f);
		blendedPath.add(lookWhereYouAreGoing, 1);

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
			.setPathOffset(.3f * size)
			.setPredictionTime(.2f)
			.setArrivalTolerance(0.01f)
			.setArriveEnabled(true)
			.setDecelerationRadius(.66f);

		ai.followPath = followPath;

		blendedPath.add(followPath, 1);
		priorityPath.add(blendedPath);
		ai.steeringPath = priorityPath;
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
		case Input.Keys.MINUS: {
			deltaScale = Math.max(deltaScale -.1f, 0);
			Gdx.app.log(TAG, "ds " + deltaScale);
		}break;
		case Input.Keys.EQUALS: {
			deltaScale = Math.min(deltaScale +.1f, 2);
			Gdx.app.log(TAG, "ds " + deltaScale);
		}break;
		case Input.Keys.NUM_0: {
			deltaScale = 1;
			Gdx.app.log(TAG, "ds " + deltaScale);
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
		private Pathfinding.NodePath nodes;

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

		public void update (Path<Vector2, LinePath.LinePathParam> path, Pathfinding.NodePath nodes) {
			this.path = path;
			this.nodes = nodes;
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

	public static class PriorityBlendedSteering extends BlendedSteering<Vector2> implements MyPrioritySteering.PriorityOverride {
		protected SteeringAcceleration<Vector2> steering;

		boolean override;
		public PriorityBlendedSteering (Steerable<Vector2> owner) {
			super(owner);
			this.steering = new SteeringAcceleration<Vector2>(newVector(owner));

		}

		public int getCount() {
			return list.size;
		}

		@Override protected SteeringAcceleration<Vector2> calculateRealSteering (SteeringAcceleration<Vector2> blendedSteering) {
			blendedSteering.setZero();
			override = false;
			// Go through all the behaviors
			int len = list.size;
			for (int i = 0; i < len; i++) {
				BehaviorAndWeight<Vector2> bw = list.get(i);
				SteeringBehavior<Vector2> behavior = bw.getBehavior();
				// Calculate the behavior's steering
				behavior.calculateSteering(steering);

				// Scale and add the steering to the accumulator
				blendedSteering.mulAdd(steering, bw.getWeight());
				if (behavior instanceof MyPrioritySteering.PriorityOverride) {
					override |= ((MyPrioritySteering.PriorityOverride)behavior).override();
				}
			}

			Limiter actualLimiter = getActualLimiter();

			// Crop the result
			blendedSteering.linear.limit(actualLimiter.getMaxLinearAcceleration());
			if (blendedSteering.angular > actualLimiter.getMaxAngularAcceleration())
				blendedSteering.angular = actualLimiter.getMaxAngularAcceleration();

			return blendedSteering;
		}

		public void setWeight(SteeringBehavior behavior, float weight) {
			for (BehaviorAndWeight<Vector2> behaviorAndWeight : list) {
				if (behaviorAndWeight.getBehavior() == behavior) {
					behaviorAndWeight.setWeight(weight);
				}
			}
		}

		@Override public boolean override () {
			return override;
		}
	}

	public static class MyPrioritySteering<T extends Vector<T>> extends SteeringBehavior<T> {

		/** The threshold of the steering acceleration magnitude below which a steering behavior is considered to have given no output. */
		protected float epsilon;

		/** The list of steering behaviors in priority order. The first item in the list is tried first, the subsequent entries are only
		 * considered if the first one does not return a result. */
		protected Array<SteeringBehavior<T>> behaviors = new Array<SteeringBehavior<T>>();

		/** The index of the behavior whose acceleration has been returned by the last evaluation of this priority steering. */
		protected int selectedBehaviorIndex;

		/** Creates a {@code PrioritySteering} behavior for the specified owner. The threshold is set to 0.001.
		 * @param owner the owner of this behavior */
		public MyPrioritySteering (Steerable<T> owner) {
			this(owner, 0.001f);
		}

		/** Creates a {@code PrioritySteering} behavior for the specified owner and threshold.
		 * @param owner the owner of this behavior
		 * @param epsilon the threshold of the steering acceleration magnitude below which a steering behavior is considered to have
		 *           given no output */
		public MyPrioritySteering (Steerable<T> owner, float epsilon) {
			super(owner);
			this.epsilon = epsilon;
		}

		/** Adds the specified behavior to the priority list.
		 * @param behavior the behavior to add
		 * @return this behavior for chaining. */
		public MyPrioritySteering<T> add (SteeringBehavior<T> behavior) {
			behaviors.add(behavior);
			return this;
		}

		@Override
		protected SteeringAcceleration<T> calculateRealSteering (SteeringAcceleration<T> steering) {
			// We'll need epsilon squared later.
			float epsilonSquared = epsilon * epsilon;

			// Go through the behaviors until one has a large enough acceleration
			int n = behaviors.size;
			selectedBehaviorIndex = -1;
			for (int i = 0; i < n; i++) {
				selectedBehaviorIndex = i;

				SteeringBehavior<T> behavior = behaviors.get(i);

				// Calculate the behavior's steering
				behavior.calculateSteering(steering);
				// this steering will be taken into account only if override is true
				if (behavior instanceof PriorityOverride) {
					if (((PriorityOverride)behavior).override()) {
						return steering;
					} else {
						continue;
					}
				}
				// If we're above the threshold return the current steering
				if (steering.calculateSquareMagnitude() > epsilonSquared) return steering;
			}

			// If we get here, it means that no behavior had a large enough acceleration,
			// so return the small acceleration from the final behavior or zero if there are
			// no behaviors in the list.
			return n > 0 ? steering : steering.setZero();
		}

		/** Returns the index of the behavior whose acceleration has been returned by the last evaluation of this priority steering; -1
		 * otherwise. */
		public int getSelectedBehaviorIndex () {
			return selectedBehaviorIndex;
		}

		/** Returns the threshold of the steering acceleration magnitude below which a steering behavior is considered to have given no
		 * output. */
		public float getEpsilon () {
			return epsilon;
		}

		/** Sets the threshold of the steering acceleration magnitude below which a steering behavior is considered to have given no
		 * output.
		 * @param epsilon the epsilon to set
		 * @return this behavior for chaining. */
		public MyPrioritySteering<T> setEpsilon (float epsilon) {
			this.epsilon = epsilon;
			return this;
		}

		public interface PriorityOverride {
			boolean override();
		}

		//
		// Setters overridden in order to fix the correct return type for chaining
		//

		@Override
		public MyPrioritySteering<T> setOwner (Steerable<T> owner) {
			this.owner = owner;
			return this;
		}

		@Override
		public MyPrioritySteering<T> setEnabled (boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/** Sets the limiter of this steering behavior. However, {@code PrioritySteering} needs no limiter at all as it simply returns
		 * the first non zero steering acceleration.
		 * @return this behavior for chaining. */
		@Override
		public MyPrioritySteering<T> setLimiter (Limiter limiter) {
			this.limiter = limiter;
			return this;
		}

		public int getCount() {
			return behaviors.size;
		}

		public SteeringBehavior<T> get (int i) {
			return behaviors.get(i);
		}
	}

	public class MyArrive<T extends Vector<T>> extends SteeringBehavior<T> implements MyPrioritySteering.PriorityOverride {

		/** The target to arrive to. */
		protected Location<T> target;

		/** The tolerance for arriving at the target. It lets the owner get near enough to the target without letting small errors keep
		 * it in motion. */
		protected float arrivalTolerance;

		/** The radius for beginning to slow down */
		protected float decelerationRadius;

		/** The time over which to achieve target speed */
		protected float timeToTarget = 0.1f;

		protected boolean override;

		/** Creates an {@code Arrive} behavior for the specified owner.
		 * @param owner the owner of this behavior */
		public MyArrive (Steerable<T> owner) {
			this(owner, null);
		}

		/** Creates an {@code Arrive} behavior for the specified owner and target.
		 * @param owner the owner of this behavior
		 * @param target the target of this behavior */
		public MyArrive (Steerable<T> owner, Location<T> target) {
			super(owner);
			this.target = target;
		}

		@Override
		protected SteeringAcceleration<T> calculateRealSteering (SteeringAcceleration<T> steering) {
			return arrive(steering, target.getPosition());
		}

		protected SteeringAcceleration<T> arrive (SteeringAcceleration<T> steering, T targetPosition) {

			if (!override) {
				steering.setZero();
				return steering;
			}

			// Get the direction and distance to the target
			T toTarget = steering.linear.set(targetPosition).sub(owner.getPosition());
			float distance = toTarget.len();

			// Check if we are there, return no steering
			if (distance <= arrivalTolerance) return steering.setZero();

			Limiter actualLimiter = getActualLimiter();
			// Go max speed
			float targetSpeed = actualLimiter.getMaxLinearSpeed();

			// If we are inside the slow down radius calculate a scaled speed
			if (distance <= decelerationRadius) targetSpeed *= distance / decelerationRadius;

			// Target velocity combines speed and direction
			T targetVelocity = toTarget.scl(targetSpeed / distance); // Optimized code for: toTarget.nor().scl(targetSpeed)

			// Acceleration tries to get to the target velocity without exceeding max acceleration
			// Notice that steering.linear and targetVelocity are the same vector
			targetVelocity.sub(owner.getLinearVelocity()).scl(1f / timeToTarget).limit(actualLimiter.getMaxLinearAcceleration());

			// No angular acceleration
			steering.angular = 0f;

			// Output the steering
			return steering;
		}

		/** Returns the target to arrive to. */
		public Location<T> getTarget () {
			return target;
		}

		/** Sets the target to arrive to.
		 * @return this behavior for chaining. */
		public MyArrive<T> setTarget (Location<T> target) {
			this.target = target;
			return this;
		}

		/** Returns the tolerance for arriving at the target. It lets the owner get near enough to the target without letting small
		 * errors keep it in motion. */
		public float getArrivalTolerance () {
			return arrivalTolerance;
		}

		/** Sets the tolerance for arriving at the target. It lets the owner get near enough to the target without letting small errors
		 * keep it in motion.
		 * @return this behavior for chaining. */
		public MyArrive<T> setArrivalTolerance (float arrivalTolerance) {
			this.arrivalTolerance = arrivalTolerance;
			return this;
		}

		/** Returns the radius for beginning to slow down. */
		public float getDecelerationRadius () {
			return decelerationRadius;
		}

		/** Sets the radius for beginning to slow down.
		 * @return this behavior for chaining. */
		public MyArrive<T> setDecelerationRadius (float decelerationRadius) {
			this.decelerationRadius = decelerationRadius;
			return this;
		}

		/** Returns the time over which to achieve target speed. */
		public float getTimeToTarget () {
			return timeToTarget;
		}

		/** Sets the time over which to achieve target speed.
		 * @return this behavior for chaining. */
		public MyArrive<T> setTimeToTarget (float timeToTarget) {
			this.timeToTarget = timeToTarget;
			return this;
		}

		//
		// Setters overridden in order to fix the correct return type for chaining
		//

		@Override
		public MyArrive<T> setOwner (Steerable<T> owner) {
			this.owner = owner;
			return this;
		}

		@Override
		public MyArrive<T> setEnabled (boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/** Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum linear speed and
		 * acceleration.
		 * @return this behavior for chaining. */
		@Override
		public MyArrive<T> setLimiter (Limiter limiter) {
			this.limiter = limiter;
			return this;
		}

		@Override public boolean override () {
			return override;
		}
	}

	public static class PriorityDoorArrive extends Arrive<Vector2> implements MyPrioritySteering.PriorityOverride {
		private static final LinePath<Vector2> dummy = new LinePath<>(new Array<>(new Vector2[]{new Vector2(), new Vector2(0, 1)}));

		/** The distance along the path to generate the target. Can be negative if the owner has to move along the reverse direction. */
		protected float pathOffset;

		/** The current position on the path */
		protected LinePath.LinePathParam pathParam;

		/** The flag indicating whether to use {@link Arrive} behavior to approach the end of an open path. It defaults to {@code true}. */
		protected boolean arriveEnabled;

		/** The time in the future to predict the owner's position. Set it to 0 for non-predictive path following. */
		protected float predictionTime;

		private Vector2 internalTargetPosition;

		private Vector2 arriveTargetPosition;

		protected boolean override;
		protected Map map;
		protected Agent owner;
		private LinePath<Vector2> path;

		protected Face<Vector2> face;
		protected AgentLocation faceLocation;

		public PriorityDoorArrive (Agent owner) {
			this(owner, dummy, 0);
		}
		/** Creates a non-predictive {@code FollowPath} behavior for the specified owner and path.
		 * @param owner the owner of this behavior
		 * @param path the path to be followed by the owner. */
		public PriorityDoorArrive (Agent owner, Path<Vector2, LinePath.LinePathParam> path) {
			this(owner, path, 0);
		}

		/** Creates a non-predictive {@code FollowPath} behavior for the specified owner, path and path offset.
		 * @param owner the owner of this behavior
		 * @param path the path to be followed by the owner
		 * @param pathOffset the distance along the path to generate the target. Can be negative if the owner is to move along the
		 *           reverse direction. */
		public PriorityDoorArrive (Agent owner, Path<Vector2, LinePath.LinePathParam> path, float pathOffset) {
			this(owner, path, pathOffset, 0);
		}

		/** Creates a {@code FollowPath} behavior for the specified owner, path, path offset, maximum linear acceleration and prediction
		 * time.
		 * @param owner the owner of this behavior
		 * @param path the path to be followed by the owner
		 * @param pathOffset the distance along the path to generate the target. Can be negative if the owner is to move along the
		 *           reverse direction.
		 * @param predictionTime the time in the future to predict the owner's position. Can be 0 for non-predictive path following. */
		public PriorityDoorArrive (Agent owner, Path<Vector2, LinePath.LinePathParam> path, float pathOffset, float predictionTime) {
			super(owner);
			this.path = (LinePath<Vector2>)path;
			this.pathParam = path.createParam();
			this.pathOffset = pathOffset;
			this.predictionTime = predictionTime;
			this.owner = owner;

			this.arriveEnabled = true;

			this.internalTargetPosition = newVector(owner);
			this.arriveTargetPosition = newVector(owner);

			face = new Face<>(owner);
			faceLocation = new AgentLocation();
			face.setTarget(faceLocation);
		}

		protected int findSegmentIndex (float targetDistance) {
			// NOTE only open paths
			if (targetDistance < 0) {
				// Clamp target distance to the min
				targetDistance = 0;
			} else if (targetDistance > path.getLength()) {
				// Clamp target distance to the max
				targetDistance = path.getLength();
			}

			// Walk through lines to see on which line we are
			Array<LinePath.Segment<Vector2>> segments = path.getSegments();
			int segmentId = -1;
			for (int i = 0; i < segments.size; i++) {
				LinePath.Segment<Vector2> segment = segments.get(i);
				if (segment.getCumulativeLength() >= targetDistance) {
					return i;
				}
			}
			return segmentId;
		}

		int lastAt = -1;
		@Override
		protected SteeringAcceleration<Vector2> calculateRealSteering (SteeringAcceleration<Vector2> steering) {

			// Predictive or non-predictive behavior?
			Vector2 location = (predictionTime == 0) ?
				// Use the current position of the owner
				owner.getPosition()
				:
				// Calculate the predicted future position of the owner. We're reusing steering.linear here.
				steering.linear.set(owner.getPosition()).mulAdd(owner.getLinearVelocity(), predictionTime);

			// Find the distance from the start of the path
			float distance = path.calculateDistance(location, pathParam);

			// Offset it
			float targetDistance = distance + pathOffset;

			// first we find a target thats ahead far enough
			path.calculateTargetPosition(internalTargetPosition, pathParam, targetDistance);

			Map.Node at = map.at(internalTargetPosition.x, internalTargetPosition.y);
			if (!override) {
				if (lastAt != at.index) {
					lastAt = at.index;
					Gdx.app.log(TAG, "At " + Map.typeToStr(at.type));
					if (at.type == Map.DR) {
						// if we hit a door, we find the segment that ends in the door
						Array<LinePath.Segment<Vector2>> segments = path.getSegments();
						int segmentIndex = findSegmentIndex(targetDistance);
						arriveTargetPosition.set(segments.get(segmentIndex).getBegin());
						faceLocation.getPosition().set(segments.get(segmentIndex).getEnd());
						// NOTE request the door to open
						owner.doorTimer = owner.clearance * 2;
						override = true;
					} else {
						override = false;
						arriveTargetPosition.set(0, 0);
					}
				}
			}
			if (override) {
				// NOTE arrive at the selected target while we wait for door to open
				if (owner.doorTimer > 0) {
					// we know face only adds angular, so we will reuse steering here
					face.calculateSteering(steering);
					// we need to store it, as arrive will override it
					float angular = steering.angular;
					arrive(steering, arriveTargetPosition);
					steering.angular = angular;
					return steering;
				} else {
					override = false;
				}
			}
			return steering.setZero();
		}

		/** Returns the path to follow */
		public Path<Vector2, LinePath.LinePathParam> getPath () {
			return path;
		}

		/** Sets the path followed by this behavior.
		 * @param path the path to set
		 * @return this behavior for chaining. */
		public PriorityDoorArrive setPath (Path<Vector2, LinePath.LinePathParam> path) {
			this.path = (LinePath<Vector2>)path;
			return this;
		}

		/** Returns the path offset. */
		public float getPathOffset () {
			return pathOffset;
		}

		/** Returns the flag indicating whether to use {@link Arrive} behavior to approach the end of an open path. */
		public boolean isArriveEnabled () {
			return arriveEnabled;
		}

		/** Returns the prediction time. */
		public float getPredictionTime () {
			return predictionTime;
		}

		/** Sets the prediction time. Set it to 0 for non-predictive path following.
		 * @param predictionTime the predictionTime to set
		 * @return this behavior for chaining. */
		public PriorityDoorArrive setPredictionTime (float predictionTime) {
			this.predictionTime = predictionTime;
			return this;
		}

		/** Sets the flag indicating whether to use {@link Arrive} behavior to approach the end of an open path. It defaults to
		 * {@code true}.
		 * @param arriveEnabled the flag value to set
		 * @return this behavior for chaining. */
		public PriorityDoorArrive setArriveEnabled (boolean arriveEnabled) {
			this.arriveEnabled = arriveEnabled;
			return this;
		}

		/** Sets the path offset to generate the target. Can be negative if the owner has to move along the reverse direction.
		 * @param pathOffset the pathOffset to set
		 * @return this behavior for chaining. */
		public PriorityDoorArrive setPathOffset (float pathOffset) {
			this.pathOffset = pathOffset;
			return this;
		}

		/** Returns the current path parameter. */
		public LinePath.LinePathParam getPathParam () {
			return pathParam;
		}

		/** Returns the current position of the internal target. This method is useful for debug purpose. */
		public Vector2 getInternalTargetPosition () {
			return internalTargetPosition;
		}

		public Vector2 getArriveTargetPosition () {
			return arriveTargetPosition;
		}

		//
		// Setters overridden in order to fix the correct return type for chaining
		//

		@Override
		public PriorityDoorArrive setOwner (Steerable<Vector2> owner) {
			super.setOwner(owner);
			this.owner = (Agent)owner;
			return this;
		}

		@Override
		public PriorityDoorArrive setEnabled (boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		/** Sets the limiter of this steering behavior. The given limiter must at least take care of the maximum linear speed and
		 * acceleration. However the maximum linear speed is not required for a closed path.
		 * @return this behavior for chaining. */
		@Override
		public PriorityDoorArrive setLimiter (Limiter limiter) {
			this.limiter = limiter;
			return this;
		}

		@Override
		public PriorityDoorArrive setTarget (Location<Vector2> target) {
			this.target = target;
			return this;
		}

		@Override
		public PriorityDoorArrive setArrivalTolerance (float arrivalTolerance) {
			this.arrivalTolerance = arrivalTolerance;
			return this;
		}

		@Override
		public PriorityDoorArrive setDecelerationRadius (float decelerationRadius) {
			this.decelerationRadius = decelerationRadius;
			return this;
		}

		public PriorityDoorArrive setFaceDecelerationRadius (float decelerationRadius) {
			face.setDecelerationRadius(decelerationRadius);
			return this;
		}

		@Override
		public PriorityDoorArrive setTimeToTarget (float timeToTarget) {
			this.timeToTarget = timeToTarget;
			return this;
		}

		public PriorityDoorArrive setFaceTimeToTarget (float timeToTarget) {
			face.setTimeToTarget(timeToTarget);
			return this;
		}

		public void setMap (Map map) {
			this.map = map;
		}

		@Override public boolean override () {
			return override;
		}

		public void update (Path<Vector2, LinePath.LinePathParam> path, Pathfinding.NodePath nodePath) {
			setPath(path);
		}

		public Face<Vector2> setAlignTolerance (float alignTolerance) {
			return face.setAlignTolerance(alignTolerance);
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
