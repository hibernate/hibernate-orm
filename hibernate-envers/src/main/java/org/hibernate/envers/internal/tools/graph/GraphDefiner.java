/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
