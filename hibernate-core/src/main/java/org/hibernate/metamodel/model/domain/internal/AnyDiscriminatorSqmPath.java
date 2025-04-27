/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.query.sqm.DiscriminatorSqmPath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

import java.util.Objects;

public class AnyDiscriminatorSqmPath<T> extends AbstractSqmPath<T> implements DiscriminatorSqmPath<T> {

	protected AnyDiscriminatorSqmPath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedPathSource, lhs, nodeBuilder );
	}

	@Override
	public AnyDiscriminatorSqmPath<T> copy(SqmCopyContext context) {
		final AnyDiscriminatorSqmPath<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		//noinspection unchecked
		return context.registerCopy(
				this,
				(AnyDiscriminatorSqmPath<T>) getLhs().copy( context ).type()
		);
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAnyDiscriminatorTypeExpression( this ) ;
	}

	@Override
	public AnyDiscriminatorSqmPathSource<T> getExpressible() {
		return (AnyDiscriminatorSqmPathSource<T>) getNodeType();
	}


	@Override
	public boolean equals(Object object) {
		return object instanceof AnyDiscriminatorSqmPath<?> that
			&& Objects.equals( this.getLhs(), that.getLhs() );
	}

	@Override
	public int hashCode() {
		return getLhs().hashCode();
	}
}
