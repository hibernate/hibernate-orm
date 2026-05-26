/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.domain.SqmTreatedEntityJoin;

/**
 * @author Steve Ebersole
 */
public interface JpaEntityJoin<L,R> extends JpaJoin<L,R> {
	@Nonnull
	@Override
	EntityDomainType<R> getModel();

	@Override
	@Nonnull
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull Class<S> treatAsType);

	@Override
	@Nonnull
	<S extends R> SqmTreatedEntityJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatAsType);
}
