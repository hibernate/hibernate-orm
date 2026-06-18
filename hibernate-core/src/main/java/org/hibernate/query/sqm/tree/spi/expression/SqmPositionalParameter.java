/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;

/**
 * Models a positional parameter expression
 *
 * @author Steve Ebersole
 */
public class SqmPositionalParameter<T> extends AbstractSqmParameter<T> {
	private final int position;

	public SqmPositionalParameter(
			int position,
			boolean canBeMultiValued,
			NodeBuilder nodeBuilder) {
		this( position, canBeMultiValued, null, nodeBuilder );
	}

	public SqmPositionalParameter(
			int position,
			boolean canBeMultiValued,
			@Nullable SqmBindableType<T> expressibleType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, expressibleType, nodeBuilder );
		this.position = position;
	}

	@Override
	public SqmPositionalParameter<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmPositionalParameter<T> expression = context.registerCopy(
				this,
				new SqmPositionalParameter<>(
						position,
						allowMultiValuedBinding(),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public @Nonnull Integer getPosition() {
		return position;
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmPositionalParameter<>( getPosition(), allowMultiValuedBinding(), this.getNodeType(), nodeBuilder() );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitPositionalParameterExpression( this );
	}

	@Override
	public String toString() {
		return "SqmPositionalParameter(" + getPosition() + ")";
	}

	@Override
	public String asLoggableText() {
		return "?" + getPosition();
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( '?' );
		hql.append( getPosition() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmPositionalParameter<?> that
			&& position == that.position;
	}

	@Override
	public int hashCode() {
		return position;
	}

	@Override
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
