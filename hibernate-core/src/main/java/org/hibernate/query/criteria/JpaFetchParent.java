/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Set;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.FetchParent;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public interface JpaFetchParent<O,T> extends FetchParent<O,T> {
	@Override
	Set<Fetch<T, ?>> getFetches();

	@Override
	<Y> JpaFetch<T, Y> fetch(SingularAttribute<? super T, Y> attribute);

	@Override
	<Y> JpaFetch<T, Y> fetch(SingularAttribute<? super T, Y> attribute, JoinType jt);

	@Override
	<Y> JpaFetch<T, Y> fetch(PluralAttribute<? super T, ?, Y> attribute);

	@Override
	<Y> JpaFetch<T, Y> fetch(PluralAttribute<? super T, ?, Y> attribute, JoinType jt);

	@Override
	<X, Y> JpaFetch<X, Y> fetch(String attributeName);

	@Override
	<X, Y> JpaFetch<X, Y> fetch(String attributeName, JoinType jt);
}
