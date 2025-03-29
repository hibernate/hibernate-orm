/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.List} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaListJoin<O, T> extends JpaPluralJoin<O, List<T>, T>, ListJoin<O, T> {
	@Override
	JpaListJoin<O, T> on(JpaExpression<Boolean> restriction);

	@Override
	JpaListJoin<O, T> on(Expression<Boolean> restriction);

	@Override
	JpaListJoin<O, T> on(JpaPredicate... restrictions);

	@Override
	JpaListJoin<O, T> on(Predicate... restrictions);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(EntityDomainType<S> treatAsType);
}
