/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.BooleanExpression;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;

import java.util.Collection;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Collection} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaCollectionJoin<O, T> extends JpaPluralJoin<O, Collection<T>, T>, CollectionJoin<O, T> {

	@Override
	JpaCollectionJoin<O, T> on(@Nullable JpaExpression<Boolean> restriction);

	@Override
	JpaCollectionJoin<O, T> on(@Nullable Expression<Boolean> restriction);

	@Override
	JpaCollectionJoin<O, T> on(JpaPredicate @Nullable... restrictions);

	@Override
	JpaCollectionJoin<O, T> on(BooleanExpression... restrictions);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends T> JpaTreatedJoin<O,T, S> treatAs(EntityDomainType<S> treatAsType);
}
