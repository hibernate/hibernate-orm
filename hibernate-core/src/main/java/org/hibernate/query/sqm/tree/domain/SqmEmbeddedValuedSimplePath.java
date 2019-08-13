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
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;

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

		return subPathSource.createSqmPath( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEmbeddableValuedPath( this );
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
