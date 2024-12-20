/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.type.BasicType;

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
	public void appendHqlString(StringBuilder sb) {
		sb.append( "wrap(" );
		expression.appendHqlString( sb );
		sb.append( " as " );
		sb.append( getNodeType().getReturnedClassName() );
		sb.append( ")" );
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
}
