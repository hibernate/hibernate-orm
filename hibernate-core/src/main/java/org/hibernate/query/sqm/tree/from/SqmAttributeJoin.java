/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.Internal;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaFetch;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.SqmPathSource;
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
	SqmFrom<?,O> getLhs();

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
	SqmPredicate getJoinPredicate();

	void setJoinPredicate(SqmPredicate predicate);

	@Override
	default SqmJoin<O, T> on(JpaExpression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	default SqmJoin<O, T> on(Expression<Boolean> restriction) {
		return SqmJoin.super.on( restriction );
	}

	@Override
	default SqmJoin<O, T> on(JpaPredicate... restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	@Override
	default SqmJoin<O, T> on(Predicate... restrictions) {
		return SqmJoin.super.on( restrictions );
	}

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(Class<S> treatJavaType);

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(Class<S> treatJavaType, String alias);

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget);

	@Override
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias);

	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget, String alias, boolean fetch);
	<S extends T> SqmTreatedAttributeJoin<O,T,S> treatAs(Class<S> treatTarget, String alias, boolean fetch);

}
