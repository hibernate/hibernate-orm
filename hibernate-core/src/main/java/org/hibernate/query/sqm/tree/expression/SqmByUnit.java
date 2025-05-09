/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * @author Gavin King
 */
public class SqmByUnit extends AbstractSqmExpression<Long> {
	private final SqmDurationUnit<?> unit;
	private final SqmExpression<?> duration;

	public SqmByUnit(
			SqmDurationUnit<?> unit,
			SqmExpression<?> duration,
			SqmExpressible<Long> longType,
			NodeBuilder nodeBuilder) {
		super( longType, nodeBuilder );
		this.unit = unit;
		this.duration = duration;
	}

	@Override
	public SqmByUnit copy(SqmCopyContext context) {
		final SqmByUnit existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmByUnit expression = context.registerCopy(
				this,
				new SqmByUnit(
						unit.copy( context ),
						duration.copy( context ),
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmDurationUnit<?> getUnit() {
		return unit;
	}

	public SqmExpression<?> getDuration() {
		return duration;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitByUnit( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		duration.appendHqlString( hql, context );
		hql.append( " by " );
		hql.append( unit.getUnit() );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmByUnit that
			&& Objects.equals( this.unit, that.unit )
			&& Objects.equals( this.duration, that.duration );
	}

	@Override
	public int hashCode() {
		return Objects.hash( unit, duration );
	}
}
