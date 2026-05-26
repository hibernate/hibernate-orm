/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ListJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.List} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaListJoin<O, T> extends JpaPluralJoin<O, List<T>, T>, ListJoin<O, T> {
	@Override
	@Nonnull
	JpaListJoin<O, T> on(@Nullable JpaExpression<Boolean> restriction);

	@Nonnull
	@Override
	JpaListJoin<O, T> on(@Nonnull Expression<Boolean> restriction);

	@Override
	@Nonnull
	JpaListJoin<O, T> on(@Nullable JpaPredicate... restrictions);

	@Nonnull
	@Override
	JpaListJoin<O, T> on(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	JpaListJoin<O, T> on(@Nonnull List<? extends Expression<Boolean>> restrictions);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(Class<S> treatAsType);

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	default <S extends T> JpaListJoin<O, S> treat(@Nonnull Class<S> treatAsType) {
		return (JpaListJoin<O, S>) treatAs( treatAsType );
	}

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(EntityDomainType<S> treatAsType);
}
