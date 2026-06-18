/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Models a reference to a part of the application's domain model as part of an SQM tree.
 *
 * This correlates roughly to the JPA Criteria notion of Path, hence the name.
 *
 * @author Steve Ebersole
 */
public interface SqmPath<T> extends SqmExpression<T>, SemanticPathPart, JpaPath<T> {

	/**
	 * Returns the NavigablePath.
	 */
	NavigablePath getNavigablePath();

	/**
	 * The path source that this path refers to (and that most likely
	 * created it).
	 *
	 * @see SqmPathSource#createSqmPath
	 */
	SqmPathSource<T> getReferencedPathSource();

	/**
	 * Retrieve the explicit alias, if one.  May return null
	 */
	@Nullable String getExplicitAlias();

	/**
	 * Set the explicit alias for this path
	 */
	void setExplicitAlias(@Nullable String explicitAlias);

	/**
	 * Get the left-hand side of this path - may be null, indicating a
	 * root, cross-join or entity-join
	 */
	@Nullable SqmPath<?> getLhs();

	/**
	 * Returns an immutable List of reusable paths
	 */
	List<SqmPath<?>> getReusablePaths();

	/**
	 * Visit each reusable path relative to this path
	 */
	void visitReusablePaths(Consumer<SqmPath<?>> consumer);

	/**
	 * Register a reusable path relative to this path
	 */
	void registerReusablePath(SqmPath<?> path);

	@Nullable SqmPath<?> getReusablePath(String name);

	/**
	 * This node's type is its "referenced path source"
	 */
	@Override
	SqmBindableType<T> getNodeType();

	@Override
	default void applyInferableType(@Nullable SqmBindableType<?> type) {
		// do nothing
	}

	@Nullable
	@Override
	default JavaType<T> getJavaTypeDescriptor() {
		return getNodeType().getExpressibleJavaType();
	}

	@Nonnull
	@Override
	<S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull Class<S> treatJavaType);

	@Nonnull
	@Override
	<S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget);

	@Nonnull
	<S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias);

	@Nonnull
	<S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias);

	@Nonnull
	<S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch);

	@Nonnull
	<S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch);

	@Nonnull
	default SqmRoot<?> findRoot() {
		final var lhs = getLhs();
		if ( lhs != null ) {
			return lhs.findRoot();
		}

		throw new ParsingException( "Could not find root" );
	}

	SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState);

	@Override
	default SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Index operator applied to non-plural path '" + getNavigablePath() + "'" );
	}

	/**
	 * Get this path's actual resolved model, i.e. the concrete type for generic attributes.
	 */
	SqmPathSource<T> getResolvedModel();

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Nonnull
	@Override
	<Y> SqmPath<Y> get(@Nonnull SingularAttribute<? super T, Y> attribute);

	@Nonnull
	@Override
	<E, C extends Collection<E>> SqmPluralPath<C,E> get(@Nonnull PluralAttribute<? super T, C, E> collection);

	@Nonnull
	@Override
	<K, V, M extends Map<K, V>> SqmPluralPath<M,V> get(@Nonnull MapAttribute<? super T, K, V> map);

	@Nonnull
	@Override
	SqmExpression<Class<? extends T>> type();

	@Nonnull
	@Override
	<Y> SqmPath<Y> get(@Nonnull String attributeName);

	/**
	 * Same as {@link #get(String)}, but if {@code includeSubtypes} is set to {@code true}
	 * and this path is polymorphic, also try finding subtype attributes.
	 *
	 * @see SqmPathSource#findSubPathSource(String, boolean)
	 */
	default SqmPath<?> get(String attributeName, boolean includeSubtypes) {
		return get( attributeName );
	}

	@Override
	SqmPath<T> copy(SqmCopyContext context);
}
