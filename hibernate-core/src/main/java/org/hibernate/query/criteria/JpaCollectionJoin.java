/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.Collection;

import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Collection} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaCollectionJoin<O, T> extends JpaPluralJoin<O, Collection<T>, T>, CollectionJoin<O, T> {

	@Override
	JpaCollectionJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaCollectionJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaCollectionJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaCollectionJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(EntityDomainType<S> treatAsType);
}
