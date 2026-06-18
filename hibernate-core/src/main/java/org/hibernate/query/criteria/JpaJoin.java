/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;

import java.util.List;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;

/**
 * Consolidates the {@link Join} and {@link Fetch} hierarchies since that is how we implement them.
 * This allows us to treat them polymorphically.
*
* @author Steve Ebersole
*/
public interface JpaJoin<L, R> extends JpaFrom<L,R>, Join<L,R> {

	/**
	 * Return the joined attribute.
	 */
	@Override
	@Nullable
	PersistentAttribute<? super L, ?> getAttribute();

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	JpaJoin<L, R> on(@Nullable JpaExpression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaJoin<L, R> on(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	JpaJoin<L, R> on(@Nullable JpaPredicate... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaJoin<L, R> on(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaJoin<L, R> on(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends R> JpaTreatedJoin<L,R,S> treatAs(@Nonnull Class<S> treatAsType);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Nonnull
	@Override
	default <S extends R> JpaJoin<L, S> treat(@Nonnull Class<S> treatAsType) {
		return treatAs( treatAsType );
	}

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends R> JpaTreatedJoin<L,R,S> treatAs(@Nonnull EntityDomainType<S> treatAsType);
}
