/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import jakarta.persistence.EntityGraph;

/**
 * Extends the JPA-defined {@link EntityGraph} with additional operations.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see SubGraph
 */
public interface RootGraph<J> extends Graph<J>, EntityGraph<J> {

	@Override
	RootGraph<J> makeCopy(boolean mutable);

	@Override @Deprecated(forRemoval = true)
	RootGraph<J> makeRootGraph(String name, boolean mutable);

	@Override @Deprecated(forRemoval = true)
	SubGraph<J> makeSubGraph(boolean mutable);

	/**
	 * @deprecated Planned for removal in JPA 4
	 */
	@Override @Deprecated(forRemoval = true)
	default <T1> SubGraph<? extends T1> addSubclassSubgraph(Class<? extends T1> type) {
		throw new UnsupportedOperationException("This operation will be removed in JPA 4");
	}
}
