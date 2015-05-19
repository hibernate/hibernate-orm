/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public final class GraphTopologicalSort {
	private GraphTopologicalSort() {
	}

	/**
	 * Sorts a graph topologically.
	 *
	 * @param definer Defines a graph (values and representations) to sort.
	 *
	 * @return Values of the graph, sorted topologically.
	 */
	public static <V, R> List<V> sort(GraphDefiner<V, R> definer) {
		final List<V> values = definer.getValues();
		final Map<R, Vertex<R>> vertices = new HashMap<R, Vertex<R>>();

		// Creating a vertex for each representation
		for ( V v : values ) {
			final R rep = definer.getRepresentation( v );
			vertices.put( rep, new Vertex<R>( rep ) );
		}

		// Connecting neighbourhooding vertices
		for ( V v : values ) {
			for ( V vn : definer.getNeighbours( v ) ) {
				vertices.get( definer.getRepresentation( v ) )
						.addNeighbour( vertices.get( definer.getRepresentation( vn ) ) );
			}
		}

		// Sorting the representations
		final List<R> sortedReps = new TopologicalSort<R>().sort( vertices.values() );

		// Transforming the sorted representations to sorted values
		final List<V> sortedValues = new ArrayList<V>( sortedReps.size() );
		for ( R rep : sortedReps ) {
			sortedValues.add( definer.getValue( rep ) );
		}

		return sortedValues;
	}
}
