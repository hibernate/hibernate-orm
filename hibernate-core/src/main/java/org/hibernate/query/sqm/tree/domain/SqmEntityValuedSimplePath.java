/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmEntityValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmEntityValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathSource referencedPathSource = getReferencedPathSource();
		final SqmPathSource subPathSource = referencedPathSource.findSubPathSource( name );

		prepareForSubNavigableReference( subPathSource, isTerminal, creationState );

		assert getLhs() == null || creationState.getProcessingStateStack()
				.getCurrent()
				.getPathRegistry()
				.findPath( getLhs().getNavigablePath() ) != null;

		//noinspection unchecked
		return subPathSource.createSqmPath( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityValuedPath( this );
	}

	private boolean dereferenced;

	@Override
	public void prepareForSubNavigableReference(
			SqmPathSource subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		if ( dereferenced ) {
			// nothing to do, already dereferenced
			return;
		}

		log.tracef(
				"`SqmEntityValuedSimplePath#prepareForSubNavigableReference` : %s -> %s",
				getNavigablePath().getFullPath(),
				subNavigable.getPathName()
		);

		SqmCreationHelper.resolveAsLhs( getLhs(), this, subNavigable, isSubReferenceTerminal, creationState );

		dereferenced = true;
	}

	@Override
	public EntityDomainType<T> getNodeType() {
		//noinspection unchecked
		return (EntityDomainType<T>) getReferencedPathSource().getSqmPathType();
	}

	@Override
	public <S extends T> SqmTreatedSimplePath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		return (SqmTreatedSimplePath<T, S>) treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		//noinspection unchecked
		return new SqmTreatedSimplePath( this, treatTarget, nodeBuilder() );
	}

}
