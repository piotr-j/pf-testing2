package com.mygdx.game.pfa;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;

/**
 * Created by PiotrJ on 27/02/2017.
 */
public interface ClearanceIndexedGraph<N> extends IndexedGraph<N> {

	/** Returns the connections outgoing from the given node.
	 * @param fromNode the node whose outgoing connections will be returned
	 * @return the array of connections outgoing from the given node. */
	public Array<Connection<N>> getConnections (N fromNode, int clearance);
}
