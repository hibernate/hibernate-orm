/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmEmbeddedValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof EmbeddableDomainType;
	}

	@SuppressWarnings("unused")
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof EmbeddableDomainType;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathSource<?> subPathSource = getReferencedPathSource().findSubPathSource( name );
		if ( subPathSource == null ) {
			throw UnknownPathException.unknownSubPath( this, name );
		}

		prepareForSubNavigableReference( subPathSource, isTerminal, creationState );

		return subPathSource.createSqmPath( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableValuedPath( this );
	}

	private boolean dereferenced;

	@Override
	public void prepareForSubNavigableReference(
			SqmPathSource subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		if ( dereferenced ) {
			// nothing to do
			return;
		}

		log.tracef(
				"`SqmEmbeddedValuedSimplePath#prepareForSubNavigableReference` : %s -> %s",
				getNavigablePath().getFullPath(),
				subNavigable.getPathName()
		);

		final SqmPathRegistry pathRegistry = creationState.getProcessingStateStack().getCurrent().getPathRegistry();

		final SqmFrom fromByPath = pathRegistry.findFromByPath( getNavigablePath() );

		if ( fromByPath == null ) {
			getLhs().prepareForSubNavigableReference( getReferencedPathSource(), false, creationState );

			final SqmFrom<?,?> lhsFrom = pathRegistry.findFromByPath( getLhs().getNavigablePath() );

			if ( getReferencedPathSource() instanceof SqmJoinable ) {
				final SqmAttributeJoin sqmJoin = ( (SqmJoinable) getReferencedPathSource() ).createSqmJoin(
						lhsFrom,
						SqmJoinType.INNER,
						null,
						false,
						creationState
				);
				pathRegistry.register( sqmJoin );
				//noinspection unchecked
				lhsFrom.addSqmJoin( sqmJoin );
			}
		}

		dereferenced = true;
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		return null;
	}

	//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new CompositeResultImpl( getNavigablePath(), getReferencedNavigable(), resultVariable, creationState );
//	}
}
