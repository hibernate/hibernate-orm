/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmAnyValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	@SuppressWarnings("WeakerAccess")
	public SqmAnyValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof AnyMappingDomainType;
	}

	@SuppressWarnings("unused")
	public SqmAnyValuedSimplePath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath lhs,
			String explicitAlias,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, explicitAlias, nodeBuilder );

		assert referencedPathSource.getSqmPathType() instanceof AnyMappingDomainType;
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return null;
	}
}
