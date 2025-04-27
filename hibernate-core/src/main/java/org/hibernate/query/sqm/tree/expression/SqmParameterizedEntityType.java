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
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import java.util.Objects;

/**
 * Entity type expression based on a parameter - `TYPE( :someParam )`
 *
 * @author Steve Ebersole
 */
public class SqmParameterizedEntityType<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final SqmParameter<T> discriminatorSource;

	public SqmExpression<T> getDiscriminatorSource() {
		return discriminatorSource;
	}

	public SqmParameterizedEntityType(SqmParameter<T> parameterExpression, NodeBuilder nodeBuilder) {
		super( SqmExpressionHelper.toSqmType( parameterExpression.getAnticipatedType(), nodeBuilder ), nodeBuilder );
		this.discriminatorSource = parameterExpression;
	}

	@Override
	public SqmParameterizedEntityType<T> copy(SqmCopyContext context) {
		final SqmParameterizedEntityType<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmParameterizedEntityType<T> expression = context.registerCopy(
				this,
				new SqmParameterizedEntityType<>(
						discriminatorSource.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(SqmExpressible<?> type) {
		setExpressibleType( type );
		discriminatorSource.applyInferableType( type );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "type(" );
		discriminatorSource.appendHqlString( hql, context );
		hql.append( ')' );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmParameterizedEntityType<?> that
			&& Objects.equals( discriminatorSource, that.discriminatorSource );
	}

	@Override
	public int hashCode() {
		return discriminatorSource.hashCode();
	}
}
