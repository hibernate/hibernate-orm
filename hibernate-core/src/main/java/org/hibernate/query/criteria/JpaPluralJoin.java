/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.PluralJoin;
import jakarta.persistence.criteria.Predicate;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaPluralJoin<O, C, E> extends JpaJoin<O, E>, PluralJoin<O, C, E> {
	@Override
	@NonNull PluralPersistentAttribute<? super O, C, E> getAttribute();

	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nullable JpaExpression<Boolean> restriction);

	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nullable Expression<Boolean> restriction);

	@Override
	JpaPluralJoin<O, ? extends C, E> on(JpaPredicate @Nullable... restrictions);

	@Override
	JpaPluralJoin<O, ? extends C, E> on(Predicate @Nullable... restrictions);

	@Override
	<S extends E> JpaTreatedJoin<O, E, S> treatAs(Class<S> treatAsType);

	@Override
	<S extends E> JpaTreatedJoin<O, E, S> treatAs(EntityDomainType<S> treatAsType);
}
