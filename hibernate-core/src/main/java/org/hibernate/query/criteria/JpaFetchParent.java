/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.Set;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.JoinType;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public interface JpaFetchParent<O,T> extends JpaPathSource<T>, FetchParent<O,T> {
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
