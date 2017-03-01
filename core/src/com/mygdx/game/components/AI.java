package com.mygdx.game.components;

import com.artemis.Component;
import com.badlogic.gdx.ai.steer.SteeringBehavior;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.ai.steer.behaviors.CollisionAvoidance;
import com.badlogic.gdx.ai.steer.behaviors.PrioritySteering;
import com.badlogic.gdx.ai.steer.utils.Path;
import com.badlogic.gdx.ai.steer.utils.paths.LinePath;
import com.badlogic.gdx.math.Vector2;
import com.mygdx.game.Agents;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class AI extends Component {
	public AgentLocation target = new AgentLocation();
	public AgentLocation overrideTarget = new AgentLocation();
	public SteeringBehavior<Vector2> steering;

	public SteeringBehavior<Vector2> steeringPath;
	public SteeringBehavior<Vector2> steeringIdle;
//	public Arrive<Vector2> arrive;
//	public Agents.MyFollowPath followPath;

	public Path<Vector2, LinePath.LinePathParam> path;
	public Agents.MyFollowPath followPath;
	public Agents.PriorityFollowPath priorityPath;
//	public CollisionAvoidance<Vector2> avoidance;
}
