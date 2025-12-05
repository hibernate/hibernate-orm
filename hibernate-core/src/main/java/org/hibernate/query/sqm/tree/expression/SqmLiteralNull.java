/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull<T> extends SqmLiteral<T> {

	public SqmLiteralNull(NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this( null, nodeBuilder );
	}

	public SqmLiteralNull(@Nullable SqmBindableType<T> expressibleType, NodeBuilder nodeBuilder) {
		super( expressibleType, nodeBuilder );
	}

	@Override
	public SqmLiteralNull<T> copy(SqmCopyContext context) {
		final SqmLiteralNull<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralNull<T> expression = context.registerCopy(
				this,
				new SqmLiteralNull<>(
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
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
	public boolean isCompatible(Object object) {
		return equals( object );
	}

	@Override
	public int cacheHashCode() {
		return hashCode();
	}
}
