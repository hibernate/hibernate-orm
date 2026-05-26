/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import jakarta.annotation.Nonnull;
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
	@Nonnull
	RootGraphImplementor<J> makeCopy(boolean mutable);

	@Override
	@Nonnull
	RootGraphImplementor<J> makeCopy(boolean mutable, String name);

}
