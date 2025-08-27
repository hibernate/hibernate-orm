/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.graph.RootGraph;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * Integration version of the {@link RootGraph} contract.
 *
 * @author Steve Ebersole
 *
 * @see SubGraphImplementor
 */
public interface RootGraphImplementor<J> extends RootGraph<J>, GraphImplementor<J> {

	boolean appliesTo(EntityDomainType<?> entityType);

	@Override
	RootGraphImplementor<J> makeCopy(boolean mutable);

	@Override @Deprecated(forRemoval = true)
	RootGraphImplementor<J> makeRootGraph(String name, boolean mutable);

	@Override @Deprecated(forRemoval = true)
	SubGraphImplementor<J> makeSubGraph(boolean mutable);

	/**
	 * Make an immutable copy of this entity graph, using the given name.
	 *
	 * @param name The name to apply to the immutable copy
	 *
	 * @return The immutable copy
	 */
	RootGraphImplementor<J> makeImmutableCopy(String name);
}
