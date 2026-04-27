/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import java.util.List;

import jakarta.persistence.criteria.BooleanExpression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ListJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.List} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaListJoin<O, T> extends JpaPluralJoin<O, List<T>, T>, ListJoin<O, T> {
	@Override
	JpaListJoin<O, T> on(@Nullable JpaExpression<Boolean> restriction);

	@Override
	JpaListJoin<O, T> on(@Nullable Expression<Boolean> restriction);

	@Override
	JpaListJoin<O, T> on(JpaPredicate @Nullable... restrictions);

	@Override
	JpaListJoin<O, T> on(BooleanExpression... restrictions);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(EntityDomainType<S> treatAsType);
}
