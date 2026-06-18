/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.BooleanExpression;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.MapJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Map} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaMapJoin<O,K,V> extends JpaPluralJoin<O, Map<K, V>, V>, MapJoin<O,K,V> {

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaMapJoin<O, K, V> on(@Nullable JpaExpression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaMapJoin<O, K, V> on(@Nonnull Expression<Boolean> restriction);

	/**
	 * Set the join restriction.
	 */
	@Override
	@Nonnull
	JpaMapJoin<O, K, V> on(@Nullable JpaPredicate... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaMapJoin<O, K, V> on(@Nonnull BooleanExpression... restrictions);

	/**
	 * Set the join restriction.
	 */
	@Nonnull
	@Override
	JpaMapJoin<O, K, V> on(@Nonnull List<? extends Expression<Boolean>> restrictions);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends V> JpaTreatedJoin<O, V, S> treatAs(@Nonnull Class<S> treatAsType);

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	default <S extends V> JpaMapJoin<O, K, S> treat(@Nonnull Class<S> treatAsType) {
		return (JpaMapJoin<O, K, S>) treatAs( treatAsType );
	}

	/**
	 * Downcast this join to the specified subtype.
	 */
	@Override
	@Nonnull
	<S extends V> JpaTreatedJoin<O, V, S> treatAs(@Nonnull EntityDomainType<S> treatJavaType);
}
