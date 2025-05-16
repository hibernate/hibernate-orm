/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.TreatException;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;

/**
 * Reference to the key-side (as opposed to the target-side) of the
 * foreign-key of a to-one association.
 *
 * @author Steve Ebersole
 */
public class SqmFkExpression<T> extends AbstractSqmPath<T> {
	public SqmFkExpression(SqmPath<?> toOnePath) {
		this( toOnePath.getNavigablePath().append( ForeignKeyDescriptor.PART_NAME ), toOnePath );
	}

	@SuppressWarnings("unchecked")
	private SqmFkExpression(
			NavigablePath navigablePath,
			SqmPath<?> toOnePath) {
		super(
				navigablePath,
				(SqmPathSource<T>) pathDomainType( toOnePath ).getIdentifierDescriptor(),
				toOnePath,
				toOnePath.nodeBuilder()
		);
	}

	private static IdentifiableDomainType<?> pathDomainType(SqmPath<?> toOnePath) {
		final DomainType<?> domainType = toOnePath.getReferencedPathSource().getPathType();
		if ( domainType instanceof IdentifiableDomainType<?> identifiableDomainType ) {
			return identifiableDomainType;
		}
		else {
			throw new IllegalArgumentException( "Invalid path provided to 'fk()' function: " + toOnePath.getNavigablePath() );
		}
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFkExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "fk(" );
		getLhs().appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmFkExpression<?> that
			&& Objects.equals( this.getExplicitAlias(), that.getExplicitAlias() )
			&& Objects.equals( this.getLhs(), that.getLhs() );
	}

	@Override
	public int hashCode() {
		return getLhs().hashCode();
	}

	@Override
	public SqmFkExpression<T> copy(SqmCopyContext context) {
		final SqmFkExpression<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmEntityValuedSimplePath<?> lhsCopy = (SqmEntityValuedSimplePath<?>) getLhs().copy( context );
		return context.registerCopy(
				this,
				new SqmFkExpression<>( getNavigablePathCopy( lhsCopy ), lhsCopy )
		);
	}

	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) {
		throw new TreatException( "Fk paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(EntityDomainType<S> treatTarget) {
		throw new TreatException( "Fk paths cannot be TREAT-ed" );
	}

	@Override
	public SqmPath<?> resolvePathPart(String name, boolean isTerminal, SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}
}
