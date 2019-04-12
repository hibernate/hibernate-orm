/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.List;
import java.util.function.Consumer;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.criteria.JpaFrom;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.domain.SqmBagJoin;
import org.hibernate.query.sqm.tree.domain.SqmListJoin;
import org.hibernate.query.sqm.tree.domain.SqmMapJoin;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSetJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;

/**
 * Models a Bindable's inclusion in the {@code FROM} clause.
 *
 * @author Steve Ebersole
 */
public interface SqmFrom<O,T> extends SqmVisitableNode, SqmTypedNode, SqmPath<T>, JpaFrom<O, T> {
	/**
	 * The Navigable for an SqmFrom will always be a NavigableContainer
	 *
	 * {@inheritDoc}
	 */
	@Override
	NavigableContainer<T> getReferencedNavigable();

	boolean hasJoins();

	/**
	 * The joins associated with this SqmFrom
	 */
	List<SqmJoin<T,?>> getSqmJoins();

	/**
	 * Add an associated join
	 */
	void addSqmJoin(SqmJoin<T,?> join);

	/**
	 * Visit all associated joins
	 */
	void visitSqmJoins(Consumer<SqmJoin<T,?>> consumer);

	/**
	 * Details about how this SqmFrom is used in the query.
	 *
	 * @deprecated The SQM->SQL walker can already deduce
	 */
	@Remove
	@Deprecated
	UsageDetails getUsageDetails();

	@Override
	default void prepareForSubNavigableReference(
			Navigable subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		// nothing to do, already prepared
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA


	@Override
	SqmFrom<O, T> getCorrelationParent();

	@Override
	<A> SqmSingularJoin<T, A> join(SingularAttribute<? super T, A> attribute);

	@Override
	<A> SqmSingularJoin<T, A> join(SingularAttribute<? super T, A> attribute, JoinType jt);

	@Override
	<E> SqmBagJoin<T, E> join(CollectionAttribute<? super T, E> attribute);

	@Override
	<E> SqmBagJoin<T, E> join(CollectionAttribute<? super T, E> attribute, JoinType jt);

	@Override
	<E> SqmSetJoin<T, E> join(SetAttribute<? super T, E> set);

	@Override
	<E> SqmSetJoin<T, E> join(SetAttribute<? super T, E> set, JoinType jt);

	@Override
	<E> SqmListJoin<T, E> join(ListAttribute<? super T, E> list);

	@Override
	<E> SqmListJoin<T, E> join(ListAttribute<? super T, E> list, JoinType jt);

	@Override
	<K, V> SqmMapJoin<T, K, V> join(MapAttribute<? super T, K, V> map);

	@Override
	<K, V> SqmMapJoin<T, K, V> join(MapAttribute<? super T, K, V> map, JoinType jt);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName);

	@Override
	<X, Y> SqmAttributeJoin<X, Y> join(String attributeName, JoinType jt);

	@Override
	<X, Y> CollectionJoin<X, Y> joinCollection(String attributeName);

	@Override
	<X, Y> SqmBagJoin<X, Y> joinCollection(String attributeName, JoinType jt);

	@Override
	<X, Y> SetJoin<X, Y> joinSet(String attributeName);

	@Override
	<X, Y> SqmSetJoin<X, Y> joinSet(String attributeName, JoinType jt);

	@Override
	<X, Y> ListJoin<X, Y> joinList(String attributeName);

	@Override
	<X, Y> SqmListJoin<X, Y> joinList(String attributeName, JoinType jt);

	@Override
	<X, K, V> MapJoin<X, K, V> joinMap(String attributeName);

	@Override
	<X, K, V> SqmMapJoin<X, K, V> joinMap(String attributeName, JoinType jt);
}
