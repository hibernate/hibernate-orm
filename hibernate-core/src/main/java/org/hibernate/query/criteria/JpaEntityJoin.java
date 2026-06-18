/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedEntityJoin;

/**
 * @author Steve Ebersole
 */
public interface JpaEntityJoin<L,R> extends JpaJoin<L,R> {
	/**
	 * Return the joined entity model.
	 */
	@Nonnull
	@Override
	EntityDomainType<R> getModel();

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull Class<S> treatAsType);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatAsType);
}
