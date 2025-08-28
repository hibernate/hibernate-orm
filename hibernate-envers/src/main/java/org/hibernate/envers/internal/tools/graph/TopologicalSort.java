/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.tools.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Topological sorting of a graph - based on DFS.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class TopologicalSort<R> {
	private List<R> sorted;
	private int time;

	private void process(Vertex<R> v) {
		if ( v.getStartTime() != 0 ) {
			// alread processed
			return;
		}

		v.setStartTime( time++ );

		for ( Vertex<R> n : v.getNeighbours() ) {
			process( n );
		}

		v.setEndTime( time++ );

		sorted.add( v.getRepresentation() );
	}

	public List<R> sort(Collection<Vertex<R>> vertices) {
		sorted = new ArrayList<>( vertices.size() );

		time = 1;

		for ( Vertex<R> v : vertices ) {
			if ( v.getEndTime() == 0 ) {
				process( v );
			}
		}

		Collections.reverse( sorted );

		return sorted;
	}
}
