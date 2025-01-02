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
	@SuppressWarnings("unchecked") // The JPA method was defined with an incorrect generic signature
	default <T> SubGraph<? extends T> addSubclassSubgraph(Class<? extends T> type) {
		return (SubGraph<? extends T>) addTreatedSubgraph( (Class<? extends J>) type );
	}
}
