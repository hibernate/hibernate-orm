/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.spi.NavigablePath;

import java.util.Collection;
import java.util.Map;

/**
 * API extension to the JPA {@link Path} contract
 *
 * @author Steve Ebersole
 */
public interface JpaPath<T> extends JpaExpression<T>, Path<T> {
	/**
	 * Get this path's NavigablePath
	 */
	NavigablePath getNavigablePath();

	/**
	 * The source (think "left hand side") of this path
	 */
	@Nullable
	JpaPath<?> getLhs();

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	<S extends T> JpaTreatedPath<T,S> treatAs(Class<S> treatJavaType);

	@Override
	@Nonnull
	default <S extends T> JpaPath<S> treat(@Nonnull Class<S> treatJavaType) {
		return treatAs( treatJavaType );
	}

	/**
	 * Support for JPA's explicit (TREAT) down-casting.
	 */
	@Nonnull
	<S extends T> JpaPath<S> treatAs(@Nonnull EntityDomainType<S> treatJavaType);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Nullable
	@Override
	default JpaPath<?> getParentPath() {
		return getLhs();
	}

	@Nonnull
	@Override
	<Y> JpaPath<Y> get(@Nonnull SingularAttribute<? super T, Y> attribute);

	@Nonnull
	@Override
	<E, C extends Collection<E>> JpaPluralExpression<C,E> get(@Nonnull PluralAttribute<? super T, C, E> collection);

	@Nonnull
	@Override
	<K, V, M extends Map<K, V>> JpaPluralExpression<M,V> get(@Nonnull MapAttribute<? super T, K, V> map);

	@Nonnull
	@Override
	JpaExpression<Class<? extends T>> type();

	@Nonnull
	@Override
	<Y> JpaPath<Y> get(@Nonnull String attributeName);
}
