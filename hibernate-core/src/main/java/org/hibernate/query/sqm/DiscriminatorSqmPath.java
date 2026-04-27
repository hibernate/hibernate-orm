/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import jakarta.persistence.criteria.ComparableExpression;
import jakarta.persistence.criteria.NumericExpression;
import jakarta.persistence.criteria.TemporalExpression;
import jakarta.persistence.criteria.TextExpression;
import jakarta.persistence.metamodel.BooleanAttribute;
import jakarta.persistence.metamodel.ComparableAttribute;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.NumericAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import jakarta.persistence.metamodel.TextAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.expression.SqmBooleanExpression;

import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Map;

/**
 * Commonality between entity and any discriminators
 *
 * @author Steve Ebersole
 */
public interface DiscriminatorSqmPath<T> extends SqmPath<T> {
	@Override
	default void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "type(" );
		getLhs().appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	default SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <Y> SqmPath<Y> get(String attributeName) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <Y> SqmPath<Y> get(SingularAttribute<? super T, Y> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <E, C extends Collection<E>> SqmPluralPath<C, E> get(PluralAttribute<? super T, C, E> collection) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <K, V, M extends Map<K, V>> SqmPluralPath<M, V> get(MapAttribute<? super T, K, V> map) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default SqmBooleanExpression get(BooleanAttribute<? super T> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <C extends Comparable<? super C>> ComparableExpression<C> get(ComparableAttribute<? super T, C> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <A extends Temporal & Comparable<? super A>> TemporalExpression<A> get(TemporalAttribute<? super T, A> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default <N extends Number & Comparable<N>> NumericExpression<N> get(NumericAttribute<? super T, N> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Override
	default TextExpression get(TextAttribute<? super T> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}



	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType, @Nullable String alias) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget, @Nullable String alias) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(Class treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(EntityDomainType treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}
}
