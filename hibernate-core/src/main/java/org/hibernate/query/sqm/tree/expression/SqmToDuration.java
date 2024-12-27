/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Gavin King
 */
public class SqmToDuration<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> magnitude;
	private final SqmDurationUnit<?> unit;

	public SqmToDuration(
			SqmExpression<?> magnitude,
			SqmDurationUnit<?> unit,
			ReturnableType<T> type,
			NodeBuilder nodeBuilder) {
		super( type, nodeBuilder );
		this.magnitude = magnitude;
		this.unit = unit;
	}

	@Override
	public SqmToDuration<T> copy(SqmCopyContext context) {
		final SqmToDuration<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmToDuration<T> expression = context.registerCopy(
				this,
				new SqmToDuration<>(
						magnitude.copy( context ),
						unit.copy( context ),
						(ReturnableType<T>) getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmExpression<?> getMagnitude() {
		return magnitude;
	}

	public SqmDurationUnit<?> getUnit() {
		return unit;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitToDuration( this );
	}

	@Override
	public String asLoggableText() {
		return magnitude.asLoggableText() + " " + unit.getUnit();
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		magnitude.appendHqlString( sb );
		sb.append( ' ' );
		sb.append( unit.getUnit() );
	}
}
