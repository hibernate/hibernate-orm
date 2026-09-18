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

import java.util.Objects;

/**
 * Represents a named query parameter in the SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmNamedParameter<T> extends AbstractSqmParameter<T> {
	private final String name;

	public SqmNamedParameter(@Nonnull String name, boolean canBeMultiValued, @Nonnull NodeBuilder nodeBuilder) {
		this( name, canBeMultiValued, null, nodeBuilder );
	}

	public SqmNamedParameter(
			@Nonnull String name,
			boolean canBeMultiValued,
			@Nullable SqmBindableType<T> inherentType,
			@Nonnull NodeBuilder nodeBuilder) {
		super( canBeMultiValued, inherentType, nodeBuilder );
		this.name = name;
	}

	@Nonnull
	@Override
	public SqmNamedParameter<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final var expression = context.registerCopy(
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

	@Nullable
	@Override
	public <X> X accept(@Nonnull SemanticQueryWalker<X> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Nonnull
	@Override
	public String toString() {
		return "SqmNamedParameter(" + getName() + ")";
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	@Override
	public SqmParameter<T> copy() {
		return new SqmNamedParameter<>( getName(), allowMultiValuedBinding(), getNodeType(), nodeBuilder() );
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
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
	public boolean isCompatible(@Nullable Object object) {
		return equals(  object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
