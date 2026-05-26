/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Set;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public interface JpaFetchParent<O,T> extends FetchParent<O,T> {
	@Nonnull
	@Override
	Set<Fetch<T, ?>> getFetches();

	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull SingularAttribute<? super T, Y> attribute);

	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull SingularAttribute<? super T, Y> attribute, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull PluralAttribute<? super T, ?, Y> attribute);

	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull PluralAttribute<? super T, ?, Y> attribute, @Nonnull JoinType jt);

	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull String attributeName);

	@Nonnull
	@Override
	<Y> JpaFetch<T, Y> fetch(@Nonnull String attributeName, @Nonnull JoinType jt);
}
