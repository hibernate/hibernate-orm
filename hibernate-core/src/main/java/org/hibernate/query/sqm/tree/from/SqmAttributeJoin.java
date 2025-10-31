/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

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
	default SqmJoin<O, T> on(@Nullable JpaExpression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	default SqmJoin<O, T> on(@Nullable Expression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	default SqmJoin<O, T> on(JpaPredicate @Nullable... restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	@Override
	default SqmJoin<O, T> on(Predicate @Nullable... restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(Class<S> treatJavaType, @Nullable String alias);

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias);

	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch);
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(Class<S> treatTarget, @Nullable String alias, boolean fetch);

	@Override
	SqmAttributeJoin<O, T> copy(SqmCopyContext context);

}
