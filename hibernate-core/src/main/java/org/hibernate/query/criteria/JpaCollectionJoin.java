/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import java.util.Collection;
import java.util.List;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Collection} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaCollectionJoin<O, T> extends JpaPluralJoin<O, Collection<T>, T>, CollectionJoin<O, T> {

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaCollectionJoin<O, T> on(@Nullable JpaExpression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaCollectionJoin<O, T> on(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaCollectionJoin<O, T> on(@Nullable JpaPredicate... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaCollectionJoin<O, T> on(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaCollectionJoin<O, T> on(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(@Nonnull Class<S> treatAsType);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	default <S extends T> JpaCollectionJoin<O, S> treat(@Nonnull Class<S> treatAsType) {
		return (JpaCollectionJoin<O, S>) treatAs( treatAsType );
	}

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(@Nonnull EntityDomainType<S> treatAsType);
}
