/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.spi;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
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
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.spi.expression.SqmBooleanExpression;

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

	@Nonnull
	@Override
	default <Y> SqmPath<Y> get(@Nonnull String attributeName) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default <Y> SqmPath<Y> get(@Nonnull SingularAttribute<? super T, Y> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default <E, C extends Collection<E>> SqmPluralPath<C, E> get(@Nonnull PluralAttribute<? super T, C, E> collection) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default <K, V, M extends Map<K, V>> SqmPluralPath<M, V> get(@Nonnull MapAttribute<? super T, K, V> map) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default SqmBooleanExpression get(@Nonnull BooleanAttribute<? super T> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default <C extends Comparable<? super C>> ComparableExpression<C> get(@Nonnull ComparableAttribute<? super T, C> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default <A extends Temporal & Comparable<? super A>> TemporalExpression<A> get(@Nonnull TemporalAttribute<? super T, A> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default <N extends Number & Comparable<N>> NumericExpression<N> get(@Nonnull NumericAttribute<? super T, N> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}

	@Nonnull
	@Override
	default TextExpression get(@Nonnull TextAttribute<? super T> attribute) {
		throw new IllegalStateException( "Discriminator cannot be de-referenced" );
	}



	@Nonnull
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(@Nonnull Class treatJavaType) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@Nonnull
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	default SqmTreatedPath treatAs(@Nonnull EntityDomainType treatTarget) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Nonnull
	default SqmTreatedPath treatAs(@Nonnull Class treatJavaType, @Nullable String alias) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Nonnull
	default SqmTreatedPath treatAs(@Nonnull EntityDomainType treatTarget, @Nullable String alias) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Nonnull
	default SqmTreatedPath treatAs(@Nonnull Class treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	@Nonnull
	default SqmTreatedPath treatAs(@Nonnull EntityDomainType treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedMappingException( "Cannot apply TREAT operator to discriminator path" );
	}
}
