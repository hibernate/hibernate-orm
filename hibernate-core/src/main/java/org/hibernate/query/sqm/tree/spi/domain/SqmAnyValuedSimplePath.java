/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class SqmAnyValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmAnyValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		assert referencedPathSource.getPathType() instanceof AnyMappingDomainType;
	}

	@SuppressWarnings("unused")
	public SqmAnyValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			@Nullable String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );

		assert referencedPathSource.getPathType() instanceof AnyMappingDomainType;
	}

	@Override
	public SqmAnyValuedSimplePath<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final var lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmAnyValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						lhsCopy,
						getExplicitAlias(),
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException();
	}

	@Override
	@Nonnull
	public <S extends T> SqmTreatedPath<T, S> treatAs(@Nonnull EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Visitation

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAnyValuedValuedPath( this );
	}
}
