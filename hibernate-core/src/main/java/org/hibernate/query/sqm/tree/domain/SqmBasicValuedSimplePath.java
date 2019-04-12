/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmBasicValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			BasicValuedNavigable<T> referencedNavigable,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		this( navigablePath, referencedNavigable, lhs, null, nodeBuilder );
	}

	public SqmBasicValuedSimplePath(
			NavigablePath navigablePath,
			BasicValuedNavigable referencedNavigable,
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
		throw new SemanticException( "Basic-valued path [" + getNavigablePath() + "] cannot be de-referenced : " + name );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitBasicValuedPath( this );
	}

	@Override
	public BasicValuedNavigable<T> getReferencedNavigable() {
		return (BasicValuedNavigable<T>) super.getReferencedNavigable();
	}

	@Override
	public BasicValuedNavigable<T> getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public <S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new UnsupportedOperationException( "Basic-value cannot be treated (downcast)" );
	}
}
