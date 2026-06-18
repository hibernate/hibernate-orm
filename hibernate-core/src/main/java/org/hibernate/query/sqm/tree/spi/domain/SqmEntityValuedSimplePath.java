/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;

/**
 * @author Steve Ebersole
 */
public class SqmEntityValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmEntityValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	@Override
	public SqmEntityValuedSimplePath<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final var path = context.registerCopy(
				this,
				new SqmEntityValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getModel(),
						lhsCopy,
						nodeBuilder()
				)
		);
		copyTo( path, context );
		return path;
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

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityValuedPath( this );
	}

	@Override
	public @Nonnull SqmBindableType<T> getNodeType() {
		return getReferencedPathSource().getPathType();
	}
// We can't expose that the type is a EntityDomainType because it could also be a MappedSuperclass
// Ideally, we would specify the return type to be IdentifiableDomainType, but that does not implement SqmPathSource yet
// and is hence incompatible with the return type of the super class
//	@Override
//	public EntityDomainType<T> getNodeType() {
//		//noinspection unchecked
//		return (EntityDomainType<T>) getReferencedPathSource().getSqmPathType();
//	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedEntityValuedSimplePath<T,S> treatAs(@Nonnull Class<S> treatJavaType) {
		return (SqmTreatedEntityValuedSimplePath<T, S>) treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}
}
