package com.mygdx.game;

import com.artemis.annotations.Wire;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class Map extends InputSystem implements IndexedGraph<Map.Node> {
	public final static int MAP_WIDTH = 25;
	public final static int MAP_HEIGHT = 19;
	public final static int __ = 0; // EMPTY
	public final static int WL = 1; // WALL
	public final static int DR = 2; // DOOR
	private static int[] MAP = { // [y][x]
		WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL,
		WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, WL, __, WL, __, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, WL, __, __, WL, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, WL, __, WL, __, WL, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, __, __, WL, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, WL, WL, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL, __, WL, __, __, __, __, __, WL,
		WL, __, __, WL, __, __, WL, __, __, __, __, __, __, __, __, __, WL, WL, WL, __, __, __, __, __, WL,
		WL, WL, WL, WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, WL, __, __, __, __, __, __, __, __, __, __, __, __, WL, WL, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, WL, DR, WL, WL, __, __, WL, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, WL, __, __, WL, __, WL, __, WL, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, WL, __, __, WL, __, __, WL, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, WL, WL, DR, WL, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, WL,
		WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL, WL,
	};

	@Wire OrthographicCamera camera;
	@Wire ShapeRenderer shapes;

	Node[] nodes;

	@Override protected void initialize () {
		nodes = new Node[MAP_WIDTH * MAP_HEIGHT];
		for (int x = 0; x < MAP_WIDTH; x++) {
			for (int y = 0; y < MAP_HEIGHT; y++) {
				int index = index(x, y);
				nodes[index] = new Node(index, x, y, MAP[index(x, MAP_HEIGHT - y - 1)]);
			}
		}
		rebuildConnection();
	}

	boolean drawConnections = false;
	Vector2 v2 = new Vector2();
	@Override protected void processSystem () {
		shapes.setProjectionMatrix(camera.combined);
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		for (int index = 0; index < MAP_WIDTH * MAP_HEIGHT; index++) {
			Node node = nodes[index];
			switch (node.type) {
			case __: {
				shapes.setColor(.5f, .5f, .5f, 1);
				shapes.rect(node.x - .45f, node.y - .45f, .9f, .9f);
			} break;
			case DR: {
				shapes.setColor(.75f, .75f, .75f, 1);
				shapes.rect(node.x - .45f, node.y - .45f, .15f, .15f);
				shapes.rect(node.x + .3f, node.y - .45f, .15f, .15f);
				shapes.rect(node.x - .45f, node.y + .3f, .15f, .15f);
				shapes.rect(node.x + .3f, node.y + .3f, .15f, .15f);
			} break;
			case WL: {
				shapes.setColor(.1f, .1f, .1f, 1);
				shapes.rect(node.x - .45f, node.y - .45f, .9f, .9f);
			} break;
			default:{
				shapes.setColor(Color.MAGENTA);
				shapes.rect(node.x - .45f, node.y - .45f, .9f, .9f);
			}
			}
			shapes.setColor(Color.MAGENTA);
		}

		float x = gridFloor(tmp.x);
		float y = gridFloor(tmp.y);
		shapes.setColor(Color.RED);
		shapes.circle(tmp.x, tmp.y, .1f, 8);
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapes.setColor(1, 1, 0, .5f);
		shapes.rect(x, y, 1, 1);
		shapes.end();

		if (drawConnections) {
			shapes.begin(ShapeRenderer.ShapeType.Line);
			shapes.setColor(Color.ORANGE);
			for (int index = 0; index < MAP_WIDTH * MAP_HEIGHT; index++) {
				Node from = nodes[index];
				for (Connection<Node> connection : from.connections) {
					Node to = connection.getToNode();
					v2.set(to.x, to.y).sub(from.x, from.y).limit(.45f);
					shapes.line(from.x, from.y, from.x + v2.x, from.y + v2.y);
				}
			}
			shapes.end();
		}
	}

	@Override public boolean keyDown (int keycode) {
		int x = grid(tmp.x);
		int y = grid(tmp.y);
		switch (keycode) {
		case Input.Keys.NUM_1: {
			setType(x, y, __);
		} break;
		case Input.Keys.NUM_2: {
			setType(x, y, DR);
		} break;
		case Input.Keys.NUM_3: {
			setType(x, y, WL);
		} break;
		case Input.Keys.P: {
			print();
		} break;
		case Input.Keys.F1: {
			drawConnections = !drawConnections;
		} break;
		}
		return super.keyDown(keycode);
	}

	private void setType (int x, int y, int type) {
		Node at = at(x, y);
		if (at == null) return;
		at.type = type;
		rebuildConnection();
	}

	private void rebuildConnection () {
		for (int x = 0; x < MAP_WIDTH; x++) {
			for (int y = 0; y < MAP_HEIGHT; y++) {
				int index = index(x, y);
				Node from = nodes[index];
				from.connections.clear();
				if (from.type == WL) continue;
				if (notWall(x - 1, y)) {
					from.connections.add(new NodeConnection(from, at(x - 1, y)));
				}
				if (notWall(x + 1, y)) {
					from.connections.add(new NodeConnection(from, at(x + 1, y)));
				}
				if (notWall(x, y - 1)) {
					from.connections.add(new NodeConnection(from, at(x, y - 1)));
				}
				if (notWall(x, y + 1)) {
					from.connections.add(new NodeConnection(from, at(x, y + 1)));
				}
				if (notWall(x - 1, y - 1) && notWall(x - 1, y) && notWall(x, y - 1)) {
					from.connections.add(new NodeConnection(from, at(x - 1, y - 1)));
				}
				if (notWall(x - 1, y + 1) && notWall(x - 1, y) && notWall(x, y + 1)) {
					from.connections.add(new NodeConnection(from, at(x - 1, y + 1)));
				}
				if (notWall(x + 1, y - 1) && notWall(x + 1, y) && notWall(x, y - 1)) {
					from.connections.add(new NodeConnection(from, at(x + 1, y - 1)));
				}
				if (notWall(x + 1, y + 1) && notWall(x + 1, y) && notWall(x, y + 1)) {
					from.connections.add(new NodeConnection(from, at(x + 1, y + 1)));
				}
			}
		}
	}

	boolean notWall(int x, int y) {
		Node at = at(x, y);
		return at != null && at.type != WL;
	}

 	public int index(int x, int y) {
		if (x < 0 || y < 0 || x >= MAP_WIDTH || y >= MAP_HEIGHT)
			return -1;
		return x + y * MAP_WIDTH;
	}

	public int x(int index) {
		if (index < 0) return -1;
		return index % MAP_WIDTH;
	}

	public int y(int index) {
		if (index < 0) return -1;
		return index / MAP_WIDTH;
	}

	public Node at(float x, float y) {
		int index = index(grid(x), grid(y));
		if (index == -1) return null;
		return nodes[index];
	}

	@Override public int getIndex (Node node) {
		return node.index;
	}

	@Override public int getNodeCount () {
		return nodes.length;
	}

	@Override public Array<Connection<Node>> getConnections (Node fromNode) {
		return fromNode.connection();
	}

	public static class Node {
		public Array<Connection<Node>> connections = new Array<Connection<Node>>();
		public final int index;
		public final int x;
		public final int y;
		public int type;

		public Node (int index, int x, int y, int type) {
			this.index = index;
			this.x = x;
			this.y = y;
			this.type = type;
		}

		public Array<Connection<Node>> connection () {
			return connections;
		}
	}

	public static class NodeConnection implements Connection<Node> {
		Node from;
		Node to;
		float cost;
		public NodeConnection (Node from, Node to) {
			this.from = from;
			this.to = to;
			cost = 1.41f;
			if (to.x == from.x || to.y == from.y) {
				cost = 1;
			}
		}

		@Override public float getCost () {
			return cost;
		}

		@Override public Node getFromNode () {
			return from;
		}

		@Override public Node getToNode () {
			return to;
		}
	}

	private void print () {
		System.out.println("\tprivate static int[][] MAP = { // [y][x]");
		for (int x = MAP_WIDTH -1; x >= 0; x--) {
			System.out.print("\t\t{");
			for (int y = 0; y < MAP_HEIGHT; y++) {
				int type = at(x, y).type;
				switch (type) {
				case WL: {
					System.out.print("WL, ");
				}
				break;
				case __: {
					System.out.print("__, ");
				}
				break;
				}
			}
			System.out.println("},");
		}
		System.out.println("\t};");
	}

	public static int grid(float value) {
		return MathUtils.floor(value + .5f);
	}

	public static float gridFloor(float value) {
		return grid(value) - .5f;
	}
}
