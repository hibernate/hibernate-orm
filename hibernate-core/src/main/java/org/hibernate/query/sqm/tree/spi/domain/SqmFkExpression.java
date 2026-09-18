/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.domain;

import jakarta.annotation.Nullable;
import jakarta.annotation.Nonnull;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.TreatException;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * Reference to the key-side (as opposed to the target-side) of the
 * foreign-key of a to-one association.
 *
 * @author Steve Ebersole
 */
public class SqmFkExpression<T> extends AbstractSqmPath<T> {
	public SqmFkExpression(@Nonnull SqmPath<?> toOnePath) {
		this( toOnePath.getNavigablePath().append( ForeignKeyDescriptor.PART_NAME ), toOnePath );
	}

	@SuppressWarnings("unchecked")
	private SqmFkExpression(
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqmPath<?> toOnePath) {
		super(
				navigablePath,
				(SqmPathSource<T>)
						castNonNull( pathDomainType( toOnePath )
								.getIdentifierDescriptor() ),
				toOnePath,
				toOnePath.nodeBuilder()
		);
	}

	@Nonnull
	private static IdentifiableDomainType<?> pathDomainType(@Nonnull SqmPath<?> toOnePath) {
		if ( toOnePath.getReferencedPathSource().getPathType()
				instanceof IdentifiableDomainType<?> identifiableDomainType ) {
			return identifiableDomainType;
		}
		else {
			throw new IllegalArgumentException( "Invalid path provided to 'fk()' function: "
												+ toOnePath.getNavigablePath() );
		}
	}

	@Override
	public @Nonnull SqmPath<?> getLhs() {
		return castNonNull( super.getLhs() );
	}

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitFkExpression( this );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "fk(" );
		getLhs().appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Nonnull
	@Override
	public SqmFkExpression<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var lhsCopy = (SqmEntityValuedSimplePath<?>) getLhs().copy( context );
		return context.registerCopy(
				this,
				new SqmFkExpression<>( getNavigablePathCopy( lhsCopy ), lhsCopy )
		);
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull Class<S> treatJavaType) {
		throw new TreatException( "Fk paths cannot be TREAT-ed" );
	}

	@Nonnull
	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(@Nonnull EntityDomainType<S> treatTarget) {
		throw new TreatException( "Fk paths cannot be TREAT-ed" );
	}

	@Nonnull
	@Override
	public SqmPath<?> resolvePathPart(@Nonnull String name, boolean isTerminal, @Nonnull SqmCreationState creationState) {
		final var sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}
}
