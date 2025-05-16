/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * A named expression. Used when the name of the expression matters
 * e.g. in XML generation.
 *
 * @since 7.0
 */
@Incubating
public class SqmNamedExpression<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> expression;
	private final String name;

	public SqmNamedExpression(SqmExpression<T> expression, String name) {
		super( expression.getExpressible(), expression.nodeBuilder() );
		this.expression = expression;
		this.name = name;
	}

	@Override
	public SqmNamedExpression<T> copy(SqmCopyContext context) {
		final SqmNamedExpression<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmNamedExpression<T> expression = context.registerCopy(
				this,
				new SqmNamedExpression<>( this.expression.copy( context ), name )
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmExpression<T> getExpression() {
		return expression;
	}

	public String getName() {
		return name;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitNamedExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		expression.appendHqlString( hql, context );
		hql.append( " as " );
		hql.append( name );
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmNamedExpression<?> that
			&& Objects.equals( this.name, that.name )
			&& Objects.equals( this.expression, that.expression );
	}

	@Override
	public int hashCode() {
		return Objects.hash( expression, name );
	}
}
