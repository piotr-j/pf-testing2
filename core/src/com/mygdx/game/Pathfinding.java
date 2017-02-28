package com.mygdx.game;

import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.ai.pfa.*;
import com.badlogic.gdx.ai.sched.LoadBalancingScheduler;
import com.badlogic.gdx.ai.utils.Collision;
import com.badlogic.gdx.ai.utils.Ray;
import com.badlogic.gdx.ai.utils.RaycastCollisionDetector;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.mygdx.game.Map.Node;
import com.mygdx.game.pfa.*;

import java.util.Iterator;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class Pathfinding extends InputSystem implements Telegraph {
	private static final String TAG = Pathfinding.class.getSimpleName();
	final static int PF_REQUEST = 1;
	final static int PF_RESPONSE = 2;
	final static int PF_RESPONSE_DEBUG = 3;
	@Wire OrthographicCamera camera;
	@Wire ShapeRenderer shapes;
	@Wire Map map;


	NodePath activePath;

	boolean isActivePathSmoothed;
	ManhattanDistance heuristic;
	ClearanceAStarPathFinder<Node> pathFinder;
	PathSmoother<Node, Vector2> pathSmoother;

	Pool<MyPathFinderRequest> requestPool;

	LoadBalancingScheduler scheduler;

	boolean smooth = false;
	int debugClearance = 0;

	@Override protected void initialize () {
		while (true) {
			int x = MathUtils.random(1, Map.MAP_WIDTH-2);
			int y = MathUtils.random(1, Map.MAP_HEIGHT-2);
			Node at = map.at(x, y);
			if (at != null && at.type != Map.WL) {
				start.set(x, y);
				break;
			}
		}
		while (true) {
			int x = MathUtils.random(1, Map.MAP_WIDTH-2);
			int y = MathUtils.random(1, Map.MAP_HEIGHT-2);
			Node at = map.at(x, y);
			if (at != null && at.type != Map.WL) {
				end.set(x, y);
				break;
			}
		}

		activePath = new NodePath();
		heuristic = new ManhattanDistance();
		pathFinder = new ClearanceAStarPathFinder<>(map, true);
		pathSmoother = new PathSmoother<>(new CollisionDetector(map));

		requestPool = new Pool<MyPathFinderRequest>() {
			@Override
			protected MyPathFinderRequest newObject () {
				return new MyPathFinderRequest(pathSmoother);
			}
		};
		PathFinderQueue<Node> pathFinderQueue = new PathFinderQueue<Node>(pathFinder);
		MessageManager.getInstance().addListener(pathFinderQueue, PF_REQUEST);

		scheduler = new LoadBalancingScheduler(100);
		scheduler.add(pathFinderQueue, 1, 0);


	}

	Vector2 start = new Vector2();
	Vector2 end = new Vector2();
	@Override protected void processSystem () {
		scheduler.run(100000);

		if (debugClearance == 0) return;

		shapes.setProjectionMatrix(camera.combined);
		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(Color.GREEN);
		shapes.circle(start.x, start.y, .15f, 16);
		shapes.setColor(Color.RED);
		shapes.circle(end.x, end.y, .15f, 16);

		shapes.setColor(Color.CYAN);
		float offset = (debugClearance-1)/2f;
		for (int i = 0; i < activePath.getCount() -1; i++) {
			Node from = activePath.get(i);
			Node to = activePath.get(i + 1);
			shapes.line(from.x + offset, from.y + offset, to.x + offset, to.y + offset);
		}

		shapes.end();
	}

	@Override
	public boolean handleMessage (Telegram telegram) {
		switch (telegram.message) {
		case PF_RESPONSE_DEBUG: { // PathFinderQueue will call us directly, no need to register for this message
			MyPathFinderRequest pfr = (MyPathFinderRequest)telegram.extraInfo;
			if (PathFinderRequestControl.DEBUG) {
				@SuppressWarnings("unchecked") PathFinderQueue<Node> pfQueue = (PathFinderQueue<Node>)telegram.sender;
				System.out.println("pfQueue.size = " + pfQueue.size() + " executionFrames = " + pfr.executionFrames);
			}

			activePath.copy((NodePath)pfr.resultPath);

			isActivePathSmoothed = pfr.smoothEnabled;

			// Release the request
			requestPool.free(pfr);
		} break;
		case PF_RESPONSE: {// PathFinderQueue will call us directly, no need to register for this message
			MyPathFinderRequest pfr = (MyPathFinderRequest)telegram.extraInfo;
			if (PathFinderRequestControl.DEBUG) {
				@SuppressWarnings("unchecked") PathFinderQueue<Node> pfQueue = (PathFinderQueue<Node>)telegram.sender;
				System.out.println("pfQueue.size = " + pfQueue.size() + " executionFrames = " + pfr.executionFrames);
			}

			if (pfr.pathFound) {
				pfr.callback.found(new NodePath((NodePath)pfr.resultPath));
			} else {
				pfr.callback.notFound();
			}

			// Release the request
			requestPool.free(pfr);
		}break;
		}
		return true;
	}

	private void findPath () {
		Node from = map.at(start.x, start.y);
		Node to = map.at(end.x, end.y);
		if (from != null && from.type != Map.WL && to != null && to.type != Map.WL && from != to) {

			MyPathFinderRequest pfr = requestPool.obtain();
			pfr.startNode = from;
			pfr.endNode = to;
			pfr.heuristic = heuristic;
			pfr.responseMessageCode = PF_RESPONSE_DEBUG;
			pfr.smoothEnabled = smooth;
			pfr.setClearance(debugClearance);
			MessageManager.getInstance().dispatchMessage(this, PF_REQUEST, pfr);
		}
	}

	public void findPath(int sx, int sy, int ex, int ey, int clearance, PFCallback callback) {
		Node from = map.at(sx, sy);
		Node to = findTarget(ex, ey, clearance);

		if (from != null && from.type != Map.WL && to != null && to.type != Map.WL && from != to) {
			MyPathFinderRequest pfr = requestPool.obtain();
			pfr.startNode = from;
			pfr.endNode = to;
			pfr.heuristic = heuristic;
			pfr.responseMessageCode = PF_RESPONSE;
			pfr.callback = callback;
			pfr.smoothEnabled = smooth;
			pfr.setClearance(clearance);
			MessageManager.getInstance().dispatchMessage(this, PF_REQUEST, pfr);
		} else {
			callback.notFound();
		}
	}

	private Node findTarget (int tx, int ty, int clearance) {
		Node node = map.at(tx, ty);
		if (node != null && node.clearance >= clearance) return node;
		if (clearance > 1) {
			node = map.at(tx - 1, ty);
			if (node != null && node.clearance >= clearance) return node;
			node = map.at(tx, ty - 1);
			if (node != null && node.clearance >= clearance) return node;
			node = map.at(tx - 1, ty - 1);
			if (node != null && node.clearance >= clearance) return node;

			if (clearance > 2) {
				node = map.at(tx - 2, ty);
				if (node != null && node.clearance >= clearance) return node;
				node = map.at(tx, ty - 2);
				if (node != null && node.clearance >= clearance) return node;
				node = map.at(tx - 2, ty - 1);
				if (node != null && node.clearance >= clearance) return node;
				node = map.at(tx - 1, ty - 2);
				if (node != null && node.clearance >= clearance) return node;
				node = map.at(tx - 2, ty - 2);
				if (node != null && node.clearance >= clearance) return node;
			}
		}
		return null;
	}

	public interface PFCallback {
		void found(NodePath path);
		void notFound();
	}

	@Override protected void touchDownLeft (float x, float y) {
		if (debugClearance == 0) return;
		start.set(Map.grid(x), Map.grid(y));
		findPath();
	}

	@Override protected void touchDownRight (float x, float y) {
		if (debugClearance == 0) return;
		end.set(Map.grid(x), Map.grid(y));
		findPath();
	}

	@Override public boolean keyDown (int keycode) {
		switch (keycode) {
		case Input.Keys.F5: {
			debugClearance++;
			if (debugClearance > Map.MAX_CLEARANCE) {
				debugClearance = 0;
			}
			map.drawClearance = debugClearance;
			Gdx.app.log(TAG, "Debug clearance " + debugClearance);
			findPath();
		} break;
		case Input.Keys.F6: {
			debugClearance--;
			if (debugClearance < 0) {
				debugClearance = Map.MAX_CLEARANCE;
			}
			map.drawClearance = debugClearance;
			Gdx.app.log(TAG, "Debug clearance " + debugClearance);
			findPath();
		} break;
		case Input.Keys.F7: {
			smooth = !smooth;
			Gdx.app.log(TAG, "smooth " + smooth);
			findPath();
		} break;
		}
		return super.keyDown(keycode);
	}

	public static class ManhattanDistance implements Heuristic<Node> {
		@Override
		public float estimate (Node node, Node endNode) {
			return Math.abs(endNode.x - node.x) + Math.abs(endNode.y - node.y);
		}
	}

	public static class NodePath implements GraphPath<Node>, SmoothableGraphPath<Node, Vector2> {
		public final Array<Node> nodes = new Array<>();
		public int clearance;

		public NodePath () {}

		public NodePath (NodePath path) {
			copy(path);
		}

		@Override public void clear () {
			nodes.clear();
		}

		@Override public int getCount () {
			return nodes.size;
		}

		@Override public void add (Node node) {
			nodes.add(node);
		}

		@Override public Node get (int index) {
			return nodes.get(index);
		}

		@Override public void reverse () {
			nodes.reverse();
		}

		@Override public Iterator<Node> iterator () {
			return nodes.iterator();
		}

		private Vector2 tmp = new Vector2();
		@Override
		public Vector2 getNodePosition (int index) {
			Node node = nodes.get(index);
			return tmp.set(node.x, node.y);
		}

		@Override
		public void swapNodes (int index1, int index2) {
			nodes.set(index1, nodes.get(index2));
		}

		@Override
		public void truncatePath (int newLength) {
			nodes.truncate(newLength);
		}

		public void copy (NodePath other) {
			clearance = other.clearance;
			nodes.clear();
			nodes.addAll(other.nodes);
		}
	}

	protected static class MyPathFinderRequest extends ClearancePathFinderRequest<Node> implements Pool.Poolable {
		PathSmootherRequest<Node, Vector2> pathSmootherRequest;
		boolean smoothEnabled;
		boolean smoothFinished;
		private PFCallback callback;
		private PathSmoother<Node, Vector2> pathSmoother;

		public MyPathFinderRequest (PathSmoother<Node, Vector2> pathSmoother) {
			this.pathSmoother = pathSmoother;
			resultPath = new NodePath();
			pathSmootherRequest = new PathSmootherRequest<>();
		}

		public void setClearance(int clearance) {
			this.clearance = clearance;
			((NodePath)resultPath).clearance = clearance;
		}

		@Override
		public boolean initializeSearch (long timeToRun) {
			resultPath.clear();
			pathSmootherRequest.refresh((NodePath)resultPath);
			smoothFinished = false;
			return true;
		}

		@Override
		public boolean finalizeSearch (long timeToRun) {
			if (pathFound && smoothEnabled && !smoothFinished) {
				smoothFinished = pathSmoother.smoothPath(pathSmootherRequest, timeToRun);
				if (!smoothFinished) return false;
			}
			return true;
		}

		@Override
		public void reset () {
			startNode = null;
			endNode = null;
			heuristic = null;
			client = null;
			callback = null;
		}
	}

	public static class CollisionDetector implements RaycastCollisionDetector<Vector2> {
		Map map;

		public CollisionDetector (Map map) {
			this.map = map;
		}

		// See http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
		@Override
		public boolean collides (Ray<Vector2> ray) {
			int x0 = (int)ray.start.x;
			int y0 = (int)ray.start.y;
			int x1 = (int)ray.end.x;
			int y1 = (int)ray.end.y;

			int tmp;
			boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
			if (steep) {
				// Swap x0 and y0
				tmp = x0;
				x0 = y0;
				y0 = tmp;
				// Swap x1 and y1
				tmp = x1;
				x1 = y1;
				y1 = tmp;
			}
			if (x0 > x1) {
				// Swap x0 and x1
				tmp = x0;
				x0 = x1;
				x1 = tmp;
				// Swap y0 and y1
				tmp = y0;
				y0 = y1;
				y1 = tmp;
			}

			int deltax = x1 - x0;
			int deltay = Math.abs(y1 - y0);
			int error = 0;
			int y = y0;
			int ystep = (y0 < y1 ? 1 : -1);
			for (int x = x0; x <= x1; x++) {
				Node tile = steep ? map.at(y, x) : map.at(x, y);
				if (tile.type == Map.WL) return true; // We've hit a wall
				error += deltay;
				if (error + error >= deltax) {
					y += ystep;
					error -= deltax;
				}
			}

			return false;
		}

		@Override
		public boolean findCollision (Collision<Vector2> outputCollision, Ray<Vector2> inputRay) {
			throw new UnsupportedOperationException();
		}
	}
}
