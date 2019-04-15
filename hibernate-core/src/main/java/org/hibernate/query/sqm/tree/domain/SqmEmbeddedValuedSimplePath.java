/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.mapping.Join;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEmbeddedValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable<T> referencedNavigable,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, nodeBuilder );
	}

	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable<T> referencedNavigable,
			SqmPath lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		final Navigable subNavigable = getReferencedNavigable().findNavigable( name );
		if ( subNavigable == null ) {
			throw UnknownPathException.unknownSubPath( this, name );
		}

		prepareForSubNavigableReference( subNavigable, isTerminal, creationState );

		//noinspection unchecked
		return subNavigable.createSqmExpression(
				this,
				creationState
		);
	}

	@Override
	public EmbeddedValuedNavigable<T> getReferencedNavigable() {
		return (EmbeddedValuedNavigable<T>) super.getReferencedNavigable();
	}

	@Override
	public EmbeddedValuedNavigable<T> getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public EmbeddableJavaDescriptor<T> getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableValuedPath( this );
	}

	private boolean dereferenced;

	@Override
	public void prepareForSubNavigableReference(
			Navigable subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		if ( dereferenced ) {
			// nothing to do
			return;
		}

		log.tracef(
				"`SqmEmbeddedValuedSimplePath#prepareForSubNavigableReference` : %s -> %s",
				getNavigablePath().getFullPath(),
				subNavigable.getNavigableName()
		);

		final SqmPathRegistry pathRegistry = creationState.getProcessingStateStack().getCurrent().getPathRegistry();

		final SqmFrom fromByPath = pathRegistry.findFromByPath( getNavigablePath() );

		if ( fromByPath == null ) {
			getLhs().prepareForSubNavigableReference( getReferencedNavigable(), false, creationState );

			final SqmFrom lhsFrom = pathRegistry.findFromByPath( getLhs().getNavigablePath() );

			if ( getReferencedNavigable() instanceof Joinable ) {
				final SqmAttributeJoin sqmJoin = ( (Joinable) getReferencedNavigable() ).createSqmJoin(
						lhsFrom,
						SqmJoinType.INNER,
						null,
						false,
						creationState
				);
				pathRegistry.register( sqmJoin );
				lhsFrom.addSqmJoin( sqmJoin );
			}
		}

		dereferenced = true;
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException();
	}

	//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new CompositeResultImpl( getNavigablePath(), getReferencedNavigable(), resultVariable, creationState );
//	}
}
