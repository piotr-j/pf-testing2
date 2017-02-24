package com.mygdx.game.components;

import com.artemis.Component;
import com.mygdx.game.Map;

/**
 * Created by EvilEntity on 23/02/2017.
 */
public class Transform extends Component {
	public float x;
	public float y;
	public int gx;
	public int gy;

	public Transform xy (float x, float y) {
		this.x = x;
		this.y = y;
		gx = Map.grid(x);
		gy = Map.grid(y);
		return this;
	}
}
