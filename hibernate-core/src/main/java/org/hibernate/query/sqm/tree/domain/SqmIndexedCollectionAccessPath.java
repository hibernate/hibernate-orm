/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


/**
 * @author Steve Ebersole
 */
public class SqmIndexedCollectionAccessPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final SqmExpression<?> selectorExpression;

	public SqmIndexedCollectionAccessPath(
			NavigablePath navigablePath,
			SqmAttributeJoin<?, ?> pluralDomainPath,
			SqmExpression<?> selectorExpression) {
		//noinspection unchecked
		super(
				navigablePath,
				( (SqmPluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource() )
						.getElementPathSource(),
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.selectorExpression = selectorExpression;
	}

	@Override
	public @NonNull SqmAttributeJoin<?, ?> getLhs() {
		return (SqmAttributeJoin<?, ?>) castNonNull( super.getLhs() );
	}

	@Override
	public SqmIndexedCollectionAccessPath<T> copy(SqmCopyContext context) {
		final SqmIndexedCollectionAccessPath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmAttributeJoin<?, ?> lhsCopy = getLhs().copy( context );
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
		//noinspection unchecked
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
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) {
		if ( getReferencedPathSource().getPathType() instanceof EntityDomainType ) {
			return getTreatedPath( treatTarget );
		}

		throw new UnsupportedOperationException(  );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		getLhs().getLhs().appendHqlString( hql, context );
		hql.append( '.' );
		hql.append( getLhs().getReferencedPathSource().getPathName() );
		hql.append( '[' );
		selectorExpression.appendHqlString( hql, context );
		hql.append( ']' );
	}

	// No need for a custom equals/hashCode or isCompatible/cacheHashCode, because the LHS is a SqmJoin
	// which is checked for deep equality/compatibility through SqmFromClause. The NavigablePath equality check is
	// enough to determine "syntactic" equality for expressions and predicates
}
