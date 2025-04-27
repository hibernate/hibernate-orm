/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.type.BasicType;

import java.util.Objects;

public class AsWrapperSqmExpression<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<?> expression;

	AsWrapperSqmExpression(SqmExpressible<T> type, SqmExpression<?> expression) {
		super( type, expression.nodeBuilder() );
		this.expression = expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAsWrapperExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		hql.append( "wrap(" );
		expression.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( getNodeType().getReturnedClassName() );
		hql.append( ")" );
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		return expression.as( type );
	}

	@Override
	public SqmExpression<T> copy(SqmCopyContext context) {
		return new AsWrapperSqmExpression<>( getExpressible(), expression.copy( context ) );
	}

	public SqmExpression<?> getExpression() {
		return expression;
	}

	@Override
	public BasicType<T> getNodeType() {
		return (BasicType<T>) super.getNodeType();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof AsWrapperSqmExpression<?> that
			&& Objects.equals( this.expression, that.expression );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( expression );
	}
}
