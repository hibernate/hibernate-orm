/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaTreatedJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmTreatedJoin<L,R,R1 extends R> extends SqmTreatedFrom<L,R,R1>, JpaTreatedJoin<L,R,R1> {
	@Nonnull
	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(@Nonnull Class<S> treatJavaType);

	@Nonnull
	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(@Nonnull EntityDomainType<S> treatTarget);

	@Nonnull
	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Nonnull
	@Override
	<S extends R1> SqmTreatedJoin<L, R1, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);
}
