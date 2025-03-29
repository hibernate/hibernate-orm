/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmIndexedCollectionAccessPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final SqmExpression<?> selectorExpression;

	public SqmIndexedCollectionAccessPath(
			NavigablePath navigablePath,
			SqmPath<?> pluralDomainPath,
			SqmExpression<?> selectorExpression) {
		//noinspection unchecked
		super(
				navigablePath,
				( (PluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource() ).getElementPathSource(),
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.selectorExpression = selectorExpression;
	}

	@Override
	public SqmIndexedCollectionAccessPath<T> copy(SqmCopyContext context) {
		final SqmIndexedCollectionAccessPath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final SqmIndexedCollectionAccessPath<T> path = context.registerCopy(
				this,
				new SqmIndexedCollectionAccessPath<T>(
						getNavigablePathCopy( lhsCopy ),
						lhsCopy,
						selectorExpression.copy( context )
				)
		);
		copyTo( path, context );
		return path;
	}

	public SqmExpression<?> getSelectorExpression() {
		return selectorExpression;
	}

	public PluralPersistentAttribute<?, ?, T> getPluralAttribute() {
		return (PluralPersistentAttribute<?, ?, T>) getLhs().getReferencedPathSource();
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name, true );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitIndexedPluralAccessPath( this );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		if ( getReferencedPathSource().getSqmPathType() instanceof EntityDomainType ) {
			return getTreatedPath( treatTarget );
		}

		throw new UnsupportedOperationException(  );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		getLhs().appendHqlString( hql );
		hql.append( '[' );
		selectorExpression.appendHqlString( hql );
		hql.append( ']' );
	}
}
