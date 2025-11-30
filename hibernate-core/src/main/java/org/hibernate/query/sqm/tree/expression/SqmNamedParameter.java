/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * Represents a named query parameter in the SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmNamedParameter<T> extends AbstractSqmParameter<T> {
	private final String name;

	public SqmNamedParameter(String name, boolean canBeMultiValued, NodeBuilder nodeBuilder) {
		this( name, canBeMultiValued, null, nodeBuilder );
	}

	public SqmNamedParameter(
			String name,
			boolean canBeMultiValued,
			@Nullable SqmBindableType<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, inherentType, nodeBuilder );
		this.name = name;
	}

	@Override
	public SqmNamedParameter<T> copy(SqmCopyContext context) {
		final SqmNamedParameter<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNamedParameter<T> expression = context.registerCopy(
				this,
				new SqmNamedParameter<>(
						name,
						allowMultiValuedBinding(),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Override
	public String toString() {
		return "SqmNamedParameter(" + getName() + ")";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmNamedParameter<>( getName(), allowMultiValuedBinding(), getNodeType(), nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( ':' ).append( getName() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmNamedParameter<?> that
			&& Objects.equals( name, that.name );
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals(  object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
