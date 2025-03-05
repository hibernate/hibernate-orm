/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.Incubating;
import org.hibernate.query.criteria.JpaWindowFrame;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Marco Belladelli
 */
@Incubating
public class SqmWindowFrame extends AbstractSqmNode implements JpaWindowFrame {
	private final FrameKind kind;
	private final SqmExpression<?> expression;

	public SqmWindowFrame(NodeBuilder nodeBuilder, FrameKind kind) {
		this( nodeBuilder, kind, null );
	}

	public SqmWindowFrame(NodeBuilder nodeBuilder, FrameKind kind, SqmExpression<?> expression) {
		super( nodeBuilder );
		this.kind = kind;
		this.expression = expression;
	}

	@Override
	public FrameKind getKind() {
		return kind;
	}

	@Override
	public SqmExpression<?> getExpression() {
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
}
