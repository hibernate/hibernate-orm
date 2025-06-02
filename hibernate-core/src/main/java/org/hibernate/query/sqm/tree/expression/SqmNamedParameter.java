/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

/**
 * Represents a named query parameter in the SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmNamedParameter<T> extends AbstractSqmParameter<T> {
	private final String name;

	public SqmNamedParameter(String name, boolean canBeMultiValued, NodeBuilder nodeBuilder) {
		this( name, canBeMultiValued, null, nodeBuilder );
	}

	public SqmNamedParameter(
			String name,
			boolean canBeMultiValued,
			SqmBindableType<T> inherentType,
			NodeBuilder nodeBuilder) {
		super( canBeMultiValued, inherentType, nodeBuilder );
		this.name = name;
	}

	@Override
	public SqmNamedParameter<T> copy(SqmCopyContext context) {
		final SqmNamedParameter<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNamedParameter<T> expression = context.registerCopy(
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

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNamedParameterExpression( this );
	}

	@Override
	public String asLoggableText() {
		return ":" + getName();
	}

	@Override
	public String toString() {
		return "SqmNamedParameter(" + getName() + ")";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SqmParameter<T> copy() {
		return new SqmNamedParameter<>( getName(), allowMultiValuedBinding(), this.getNodeType(), nodeBuilder() );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( ':' );
		hql.append( getName() );
	}

	@Override
	public int compareTo(SqmParameter anotherParameter) {
		return anotherParameter instanceof SqmNamedParameter<?> sqmNamedParameter
				? getName().compareTo( sqmNamedParameter.getName() )
				: -1;
	}
}
