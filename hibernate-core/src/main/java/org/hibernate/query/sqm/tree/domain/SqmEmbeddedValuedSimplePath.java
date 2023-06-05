/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmEmbeddedValuedSimplePath<T>
		extends AbstractSqmSimplePath<T>
		implements SqmExpressible<T> {
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof EmbeddableDomainType;
	}

	@SuppressWarnings("unused")
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof EmbeddableDomainType;
	}

	@Override
	public SqmEmbeddedValuedSimplePath<T> copy(SqmCopyContext context) {
		final SqmEmbeddedValuedSimplePath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final SqmEmbeddedValuedSimplePath<T> path = context.registerCopy(
				this,
				new SqmEmbeddedValuedSimplePath<>(
						getNavigablePathCopy( lhsCopy ),
						getReferencedPathSource(),
						lhsCopy,
						getExplicitAlias(),
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
		return walker.visitEmbeddableValuedPath( this );
	}


	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new PathException( "Embeddable paths cannot be TREAT-ed" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new PathException( "Embeddable paths cannot be TREAT-ed" );
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public Class<T> getBindableJavaType() {
		return getJavaType();
	}
}
