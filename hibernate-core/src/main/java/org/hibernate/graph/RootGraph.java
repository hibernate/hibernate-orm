/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

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

	@Override
	RootGraph<J> makeCopy(boolean mutable);

	@Override @Deprecated(forRemoval = true)
	RootGraph<J> makeRootGraph(String name, boolean mutable);

	@Override @Deprecated(forRemoval = true)
	SubGraph<J> makeSubGraph(boolean mutable);
}
