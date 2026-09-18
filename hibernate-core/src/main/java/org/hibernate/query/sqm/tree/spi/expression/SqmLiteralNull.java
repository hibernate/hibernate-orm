/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.expression;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.query.sqm.spi.SqmBindableType;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull<T> extends SqmLiteral<T> {
	private final @Nullable JavaType<T> declaredJavaType;

	public SqmLiteralNull(@Nonnull NodeBuilder nodeBuilder) {
		this( null, nodeBuilder );
	}

	public SqmLiteralNull(@Nullable SqmBindableType<T> expressibleType, @Nonnull NodeBuilder nodeBuilder) {
		this( expressibleType, null, nodeBuilder );
	}

	public SqmLiteralNull(
			@Nullable SqmBindableType<T> expressibleType,
			@Nullable JavaType<T> declaredJavaType,
			@Nonnull NodeBuilder nodeBuilder) {
		super( expressibleType, nodeBuilder );
		this.declaredJavaType = declaredJavaType;
	}

	@Override
	public @Nullable JavaType<T> getJavaTypeDescriptor() {
		return declaredJavaType == null ? super.getJavaTypeDescriptor() : declaredJavaType;
	}

	@Nonnull
	@Override
	public SqmLiteralNull<T> copy(@Nonnull SqmCopyContext context) {
		final var existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralNull<T> expression = context.registerCopy(
				this,
				new SqmLiteralNull<>(
						getNodeType(),
						declaredJavaType,
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Nonnull
	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	@Override
	public void appendHqlString(@Nonnull StringBuilder hql, @Nonnull SqmRenderContext context) {
		hql.append( "null" );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmLiteralNull;
	}

	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public boolean isCompatible(@Nullable Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
