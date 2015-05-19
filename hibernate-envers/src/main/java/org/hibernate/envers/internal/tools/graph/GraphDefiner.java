/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.tools.graph;

import java.util.List;

/**
 * Defines a graph, where each vertex has a representation, which identifies uniquely a value.
 * Representations are comparable, values - not.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public interface GraphDefiner<V, R> {
	R getRepresentation(V v);

	V getValue(R r);

	List<V> getNeighbours(V v);

	List<V> getValues();
}
