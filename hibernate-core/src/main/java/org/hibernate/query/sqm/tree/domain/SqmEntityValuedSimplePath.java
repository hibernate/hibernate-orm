/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;

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
		final SqmEntityValuedSimplePath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final SqmEntityValuedSimplePath<T> path = context.registerCopy(
				this,
				new SqmEntityValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getReferencedPathSource(),
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
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityValuedPath( this );
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		//noinspection unchecked
		return (SqmPathSource<T>) getReferencedPathSource().getSqmPathType();
	}
// We can't expose that the type is a EntityDomainType because it could also be a MappedSuperclass
// Ideally, we would specify the return type to be IdentifiableDomainType, but that does not implement SqmPathSource yet
// and is hence incompatible with the return type of the super class
//	@Override
//	public EntityDomainType<T> getNodeType() {
//		//noinspection unchecked
//		return (EntityDomainType<T>) getReferencedPathSource().getSqmPathType();
//	}

	@Override
	public <S extends T> SqmTreatedSimplePath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		return (SqmTreatedSimplePath<T, S>) treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		return getTreatedPath( treatTarget );
	}
}
