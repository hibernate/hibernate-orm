/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Expression;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFetch;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.List;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public interface SqmAttributeJoin<O,T> extends SqmJoin<O,T>, JpaFetch<O,T>, JpaJoin<O,T> {
	@Override
	@NonNull PersistentAttribute<? super O, ?> getAttribute();

	@Override
	@NonNull SqmFrom<?,O> getLhs();

	@Override
	default boolean isImplicitlySelectable() {
		return !isFetched() && !isImplicitJoin();
	}

	@Override
	SqmPathSource<T> getReferencedPathSource();

	@Nullable
	@Override
	JavaType<T> getJavaTypeDescriptor();

	/**
	 * Is this a fetch join?
	 */
	boolean isFetched();

	/**
	 * Is this an implicit join inferred from a path expression?
	 */
	boolean isImplicitJoin();

	@Internal
	void clearFetched();

	@Override
	@Nullable SqmPredicate getJoinPredicate();

	void setJoinPredicate(@Nullable SqmPredicate predicate);

	@Override
	@Nonnull
	default SqmJoin<O, T> on(@Nullable JpaExpression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Nonnull
	@Override
	default SqmJoin<O, T> on(@Nonnull Expression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	@Nonnull
	default SqmJoin<O, T> on(@Nullable JpaPredicate... restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	@Nonnull
	@Override
	default SqmJoin<O, T> on(@Nonnull List<? extends Expression<Boolean>> restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	@Nonnull
	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(@Nonnull Class<S> treatJavaType);

	@Override
	@Nonnull
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Nonnull
	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget);

	@Override
	@Nonnull
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);

	@Nonnull
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch);

	@Nonnull
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(@Nonnull Class<S> treatTarget, @Nullable String alias, boolean fetch);

	@Override
	SqmAttributeJoin<O, T> copy(SqmCopyContext context);

}
