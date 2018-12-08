/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Set;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.criteria.JpaFetch;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractFrom<O,T>
		extends AbstractPath<T>
		implements FromImplementor<O,T>, PathSourceImplementor<T> {

	public static final JoinType DEFAULT_JOIN_TYPE = JoinType.INNER;

	private FromImplementor<O, T> correlationParent;

	private Set<JoinImplementor<T, ?>> joins;
	private Set<FetchImplementor<T, ?>> fetches;

	protected AbstractFrom(
			Navigable<T> navigable,
			PathSourceImplementor<?> pathSource,
			CriteriaNodeBuilder criteriaBuilder) {
		super( navigable, pathSource, criteriaBuilder );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Joins

	@Override
	@SuppressWarnings("unchecked")
	public Set<Join<T, ?>> getJoins() {
		return (Set) joins;
	}

	@Override
	public <Y> Join<T, Y> join(SingularAttribute<? super T, Y> attribute) {
		return null;
	}

	@Override
	public <Y> Join<T, Y> join(SingularAttribute<? super T, Y> attribute, JoinType jt) {
		return null;
	}

	@Override
	public <Y> CollectionJoin<T, Y> join(CollectionAttribute<? super T, Y> collection) {
		return null;
	}

	@Override
	public <Y> SetJoin<T, Y> join(SetAttribute<? super T, Y> set) {
		return null;
	}

	@Override
	public <Y> ListJoin<T, Y> join(ListAttribute<? super T, Y> list) {
		return null;
	}

	@Override
	public <K, V> MapJoin<T, K, V> join(MapAttribute<? super T, K, V> map) {
		return null;
	}

	@Override
	public <Y> CollectionJoin<T, Y> join(CollectionAttribute<? super T, Y> collection, JoinType jt) {
		return null;
	}

	@Override
	public <Y> SetJoin<T, Y> join(SetAttribute<? super T, Y> set, JoinType jt) {
		return null;
	}

	@Override
	public <Y> ListJoin<T, Y> join(ListAttribute<? super T, Y> list, JoinType jt) {
		return null;
	}

	@Override
	public <K, V> MapJoin<T, K, V> join(MapAttribute<? super T, K, V> map, JoinType jt) {
		return null;
	}

	@Override
	public <X, Y> Join<X, Y> join(String attributeName) {
		return null;
	}

	@Override
	public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName) {
		return null;
	}

	@Override
	public <X, Y> SetJoin<X, Y> joinSet(String attributeName) {
		return null;
	}

	@Override
	public <X, Y> ListJoin<X, Y> joinList(String attributeName) {
		return null;
	}

	@Override
	public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName) {
		return null;
	}

	@Override
	public <X, Y> Join<X, Y> join(String attributeName, JoinType jt) {
		return null;
	}

	@Override
	public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName, JoinType jt) {
		return null;
	}

	@Override
	public <X, Y> SetJoin<X, Y> joinSet(String attributeName, JoinType jt) {
		return null;
	}

	@Override
	public <X, Y> ListJoin<X, Y> joinList(String attributeName, JoinType jt) {
		return null;
	}

	@Override
	public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName, JoinType jt) {
		return null;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Fetches

	@Override
	public Set<Fetch<T, ?>> getFetches() {
		return null;
	}

	@Override
	public <Y> JpaFetch<T, Y> fetch(SingularAttribute<? super T, Y> attribute) {
		return null;
	}

	@Override
	public <Y> JpaFetch<T, Y> fetch(SingularAttribute<? super T, Y> attribute, JoinType jt) {
		return null;
	}

	@Override
	public <Y> JpaFetch<T, Y> fetch(PluralAttribute<? super T, ?, Y> attribute) {
		return null;
	}

	@Override
	public <Y> JpaFetch<T, Y> fetch(
			PluralAttribute<? super T, ?, Y> attribute, JoinType jt) {
		return null;
	}

	@Override
	public <X, Y> JpaFetch<X, Y> fetch(String attributeName) {
		return null;
	}

	@Override
	public <X, Y> JpaFetch<X, Y> fetch(String attributeName, JoinType jt) {
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Correlation

	@Override
	public FromImplementor<O, T> getCorrelationParent() {
		throw new IllegalStateException(
				String.format(
						"Criteria query From node [%s] is not part of a subquery correlation",
						getNavigablePath()
				)
		);
	}

	@Override
	public boolean isCorrelated() {
		return false;
	}
}
