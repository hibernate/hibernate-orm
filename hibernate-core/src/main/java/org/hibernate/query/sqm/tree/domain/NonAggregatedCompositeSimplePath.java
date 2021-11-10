/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import jakarta.persistence.metamodel.EntityType;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.UnknownPathException;

/**
 * @author Andrea Boriero
 */
public class NonAggregatedCompositeSimplePath<T> extends SqmEntityValuedSimplePath<T> {

	public NonAggregatedCompositeSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof EntityType;
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

		return subPathSource.createSqmPath( this, getReferencedPathSource().getIntermediatePathSource( subPathSource ) );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNonAggregatedCompositeValuedPath( this );
	}


	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new PathException( "Non Aggregate composite paths cannot be TREAT-ed" );
	}
	
}
