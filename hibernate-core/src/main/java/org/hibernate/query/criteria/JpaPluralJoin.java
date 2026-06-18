/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import java.util.List;

import jakarta.annotation.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.PluralJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaPluralJoin<O, C, E> extends JpaJoin<O, E>, PluralJoin<O, C, E> {
	/**
	 * Return the joined plural attribute.
	 */
	@Override
	@Nonnull
	PluralPersistentAttribute<? super O, C, E> getAttribute();

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nullable JpaExpression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Override
	@Nonnull
	JpaPluralJoin<O, ? extends C, E> on(@Nullable JpaPredicate... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends E> JpaTreatedJoin<O, E, S> treatAs(@Nonnull Class<S> treatAsType);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	default <S extends E> JpaPluralJoin<O, ?, S> treat(@Nonnull Class<S> treatAsType) {
		return (JpaPluralJoin<O, ?, S>) treatAs( treatAsType );
	}

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends E> JpaTreatedJoin<O, E, S> treatAs(@Nonnull EntityDomainType<S> treatAsType);
}
