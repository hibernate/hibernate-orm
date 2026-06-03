/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.SqmTreatedJoin;

/**
 * @author Steve Ebersole
 */
public interface SqmTreatedAttributeJoin<L,R,R1 extends R> extends SqmAttributeJoin<L,R1>, SqmTreatedJoin<L,R,R1> {
	@Nonnull
	@Override
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(@Nonnull Class<S> treatJavaType);

	@Override
	@Nonnull
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Nonnull
	@Override
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(@Nonnull EntityDomainType<S> treatTarget);

	@Override
	@Nonnull
	<S extends R1> SqmTreatedAttributeJoin<L,R1,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);

	@Override
	@Nonnull
	SqmTreatedAttributeJoin<L,R,R1> on(@Nullable JpaExpression<Boolean> restriction);

	@Nonnull
	@Override
	SqmTreatedAttributeJoin<L,R,R1> on(@Nonnull Expression<Boolean> restriction);

	@Override
	@Nonnull
	SqmTreatedAttributeJoin<L,R,R1> on(@Nullable JpaPredicate... restrictions);

	@Override
	@Nonnull
	SqmTreatedAttributeJoin<L,R,R1> copy(SqmCopyContext context);
}
