/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.metamodel.EntityType;

/**
 * Extends the JPA-defined {@link EntityGraph} with additional operations.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see SubGraph
 * @see org.hibernate.Session#createEntityGraph(Class)
 * @see org.hibernate.Session#createEntityGraph(String)
 * @see org.hibernate.Session#createEntityGraph(Class, String)
 * @see org.hibernate.SessionFactory#findEntityGraphByName(String)
 * @see org.hibernate.SessionFactory#createGraphForDynamicEntity(String)
 * @see EntityGraphs#createGraph(EntityType)
 * @see EntityGraphs#createGraphForDynamicEntity(EntityType)
 */
public interface RootGraph<J> extends Graph<J>, EntityGraph<J> {

	/**
	 * Make a copy of this root graph, with the given mutability.
	 * <p>
	 * If this graph is immutable, and the argument is {@code false},
	 * simply return this instance.
	 */
	@Override
	@Nonnull
	RootGraph<J> makeCopy(boolean mutable);

	/**
	 * Make a copy of this graph node, with the given mutability
	 * and the given name.
	 * <p>
	 * If this graph is immutable, and the argument is {@code false},
	 * simply return this instance.
	 */
	@Nonnull
	RootGraph<J> makeCopy(boolean mutable, String name);

}
