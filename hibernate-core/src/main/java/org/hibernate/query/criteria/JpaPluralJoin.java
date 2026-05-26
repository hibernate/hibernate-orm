/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.BooleanExpression;
import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.PluralJoin;

/**
 * Specialization of {@link JpaJoin} for {@link java.util.Set} typed attribute joins
 *
 * @author Steve Ebersole
 */
public interface JpaPluralJoin<O, C, E> extends JpaJoin<O, E>, PluralJoin<O, C, E> {
	@Override
	@NonNull
	PluralPersistentAttribute<? super O, C, E> getAttribute();

	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nullable JpaExpression<Boolean> restriction);

	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nonnull Expression<Boolean> restriction);

	@Override
	@Nonnull
	JpaPluralJoin<O, ? extends C, E> on(JpaPredicate @Nullable... restrictions);

	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nonnull BooleanExpression... restrictions);

	@Nonnull
	@Override
	JpaPluralJoin<O, ? extends C, E> on(@Nonnull List<? extends Expression<Boolean>> restrictions);

	@Override
	<S extends E> JpaTreatedJoin<O, E, S> treatAs(Class<S> treatAsType);

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	default <S extends E> JpaPluralJoin<O, ?, S> treat(@Nonnull Class<S> treatAsType) {
		return (JpaPluralJoin<O, ?, S>) treatAs( treatAsType );
	}

	@Override
	<S extends E> JpaTreatedJoin<O, E, S> treatAs(EntityDomainType<S> treatAsType);
}
