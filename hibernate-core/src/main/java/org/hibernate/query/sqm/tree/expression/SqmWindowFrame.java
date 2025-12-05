/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaWindowFrame;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;

import java.util.Objects;

/**
 * @author Marco Belladelli
 */
@Incubating
public class SqmWindowFrame extends AbstractSqmNode implements JpaWindowFrame {
	private final FrameKind kind;
	private final @Nullable SqmExpression<?> expression;

	public SqmWindowFrame(NodeBuilder nodeBuilder, FrameKind kind) {
		this( nodeBuilder, kind, null );
	}

	public SqmWindowFrame(NodeBuilder nodeBuilder, FrameKind kind, @Nullable SqmExpression<?> expression) {
		super( nodeBuilder );
		this.kind = kind;
		this.expression = expression;
	}

	@Override
	public FrameKind getKind() {
		return kind;
	}

	@Override
	public @Nullable SqmExpression<?> getExpression() {
		return expression;
	}

	@Override
	public SqmWindowFrame copy(SqmCopyContext context) {
		final SqmWindowFrame existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmWindowFrame(
						nodeBuilder(),
						kind,
						expression == null ? null : expression.copy( context )
				)
		);
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmWindowFrame that
			&& kind == that.kind
			&& Objects.equals( expression, that.expression );
	}

	@Override
	public int hashCode() {
		int result = kind.hashCode();
		result = 31 * result + Objects.hashCode( expression );
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmWindowFrame that
				&& kind == that.kind
				&& SqmCacheable.areCompatible( expression, that.expression );
	}

	@Override
	public int cacheHashCode() {
		int result = kind.hashCode();
		result = 31 * result + SqmCacheable.cacheHashCode( expression );
		return result;
	}
}
