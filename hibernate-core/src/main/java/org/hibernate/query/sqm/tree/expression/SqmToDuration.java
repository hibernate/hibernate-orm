/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;


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
		super( nodeBuilder.resolveExpressible( type ), nodeBuilder );
		this.magnitude = magnitude;
		this.unit = unit;
	}

	@Override
	public SqmToDuration<T> copy(SqmCopyContext context) {
		final var existing = context.getCopy( this );
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
	public @NonNull SqmBindableType<T> getNodeType() {
		return castNonNull( super.getNodeType() );
	}

	@Override
	public <R> R accept(SemanticQueryWalker<R> walker) {
		return walker.visitToDuration( this );
	}

	@Override
	public String asLoggableText() {
		return magnitude.asLoggableText() + " " + unit.getUnit();
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		magnitude.appendHqlString( hql, context );
		hql.append( ' ' );
		hql.append( unit.getUnit() );
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmToDuration<?> that
			&& magnitude.equals( that.magnitude )
			&& unit.equals( that.unit );
	}

	@Override
	public int hashCode() {
		int result = magnitude.hashCode();
		result = 31 * result + unit.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmToDuration<?> that
			&& magnitude.isCompatible( that.magnitude )
			&& unit.isCompatible( that.unit );
	}

	@Override
	public int cacheHashCode() {
		int result = magnitude.cacheHashCode();
		result = 31 * result + unit.cacheHashCode();
		return result;
	}
}
