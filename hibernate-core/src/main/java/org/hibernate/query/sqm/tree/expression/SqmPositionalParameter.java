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
			SqmExpressible<T> expressibleType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, expressibleType, nodeBuilder );
		this.position = position;
	}

	@Override
	public SqmPositionalParameter<T> copy(SqmCopyContext context) {
		final SqmPositionalParameter<T> existing = context.getCopy( this );
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
	public Integer getPosition() {
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
	public int compareTo(SqmParameter anotherParameter) {
		return anotherParameter instanceof SqmPositionalParameter<?> positionalParameter
				? getPosition().compareTo( positionalParameter.getPosition() )
				: 1;
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmPositionalParameter<?> that
			&& position == that.position;
	}

	@Override
	public int hashCode() {
		return position;
	}
}
