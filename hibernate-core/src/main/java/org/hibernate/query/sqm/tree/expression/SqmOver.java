/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.List;
import java.util.Objects;

import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FrameMode;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Christian Beikov
 * @author Marco Belladelli
 */
public class SqmOver<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> expression;

	private final SqmWindow window;

	public SqmOver(
			SqmExpression<T> expression,
			SqmWindow window) {
		super( expression.getNodeType(), expression.nodeBuilder() );
		this.expression = expression;
		this.window = window;
	}

	public SqmOver(
			SqmExpression<T> expression,
			List<SqmExpression<?>> partitions,
			List<SqmSortSpecification> orderList,
			FrameMode mode,
			FrameKind startKind,
			SqmExpression<?> startExpression,
			FrameKind endKind,
			SqmExpression<?> endExpression,
			FrameExclusion exclusion) {
		this(
				expression,
				new SqmWindow(
						expression.nodeBuilder(),
						partitions,
						orderList,
						mode,
						startKind,
						startExpression,
						endKind,
						endExpression,
						exclusion
				)
		);
	}

	@Override
	public SqmOver<T> copy(SqmCopyContext context) {
		final SqmOver<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmOver<T> over = context.registerCopy(
				this,
				new SqmOver<>(
						expression.copy( context ),
						window.copy( context )
				)
		);
		copyTo( over, context );
		return over;
	}

	public SqmExpression<T> getExpression() {
		return expression;
	}

	public SqmWindow getWindow() {
		return window;
	}

	@Override
	public @Nullable SqmExpressible<T> getNodeType() {
		return expression.getNodeType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitOver( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		hql.append( " over (" );
		window.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmOver<?> sqmOver
			&& Objects.equals( expression, sqmOver.expression )
			&& Objects.equals( window, sqmOver.window );
	}

	@Override
	public int hashCode() {
		return Objects.hash( expression, window );
	}
}
