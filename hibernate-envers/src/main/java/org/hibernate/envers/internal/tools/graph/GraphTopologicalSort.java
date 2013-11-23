/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
