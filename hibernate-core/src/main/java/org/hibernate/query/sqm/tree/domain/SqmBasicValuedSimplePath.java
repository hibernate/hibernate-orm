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
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmBasicValuedSimplePath<T>
		extends AbstractSqmSimplePath<T>
		implements SqmExpressible<T> {
	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SqmBasicValuedSimplePath<T> copy(SqmCopyContext context) {
		final SqmBasicValuedSimplePath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath<?> lhsCopy = getLhs().copy( context );
		final SqmBasicValuedSimplePath<T> path = context.registerCopy(
				this,
				new SqmBasicValuedSimplePath<>(
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SemanticPathPart

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException(
				String.format(
						"Could not interpret attribute '%s' of basic-valued path '%s'",
						name, getNavigablePath()
				)
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmPath

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return super.getReferencedPathSource();
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		return getReferencedPathSource();
	}

	@Override
	public BasicJavaType<T> getJavaTypeDescriptor() {
		return (BasicJavaType<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public Class<T> getBindableJavaType() {
		return getJavaType();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Visitation

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBasicValuedPath( this );
	}
}
