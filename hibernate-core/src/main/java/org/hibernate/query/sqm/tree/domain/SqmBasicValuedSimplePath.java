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
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmBasicValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedPathSource, lhs, null, nodeBuilder );
	}

	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Basic-valued path [" + getNavigablePath() + "] cannot be de-referenced : " + name );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBasicValuedPath( this );
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return super.getReferencedPathSource();
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		return getReferencedPathSource();
	}

	@Override
	public BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}
}
