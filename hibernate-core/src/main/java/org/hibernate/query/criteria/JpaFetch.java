/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Set;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public interface JpaFetch<O,T> extends JpaFetchParent<O,T>, Fetch<O,T> {
	/**
	 * Return the fetches defined for this fetch parent.
	 */
	@Nonnull
	@Override
	Set<Fetch<T, ?>> getFetches();

	/**
	 * Create a fetch for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull SingularAttribute<? super T, Y> attribute);

	/**
	 * Create a fetch for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull SingularAttribute<? super T, Y> attribute, @Nonnull JoinType jt);

	/**
	 * Create a fetch for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull PluralAttribute<? super T, ?, Y> attribute);

	/**
	 * Create a fetch for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull PluralAttribute<? super T, ?, Y> attribute, @Nonnull JoinType jt);

	/**
	 * Create a fetch for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull String attributeName);

	/**
	 * Create a fetch for the given attribute.
	 */
	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull String attributeName, @Nonnull JoinType jt);

	/**
	 * Add a restriction to the fetch.
	 *
	 * @apiNote JPA does not allow restricting a fetch
	 */
	JpaJoin<O, T> on(JpaExpression<Boolean> restriction);

	/**
	 * Add a restriction to the fetch.
	 *
	 * @apiNote JPA does not allow restricting a fetch
	 */
	JpaJoin<O, T> on(JpaPredicate... restrictions);
}
