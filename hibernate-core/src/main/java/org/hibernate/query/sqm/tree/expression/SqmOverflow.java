/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;

import java.util.Objects;

/**
 * @author Christian Beikov
 */
public class SqmOverflow<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> separatorExpression;
	private final SqmExpression<T> fillerExpression;
	private final boolean withCount;

	public SqmOverflow(SqmExpression<T> separatorExpression, SqmExpression<T> fillerExpression, boolean withCount) {
		super( separatorExpression.getNodeType(), separatorExpression.nodeBuilder() );
		this.separatorExpression = separatorExpression;
		this.fillerExpression = fillerExpression;
		this.withCount = withCount;
	}

	@Override
	public SqmOverflow<T> copy(SqmCopyContext context) {
		final SqmOverflow<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmOverflow<T> expression = context.registerCopy(
				this,
				new SqmOverflow<>(
						separatorExpression.copy( context ),
						fillerExpression == null ? null : fillerExpression.copy( context ),
						withCount
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmExpression<T> getSeparatorExpression() {
		return separatorExpression;
	}

	public SqmExpression<T> getFillerExpression() {
		return fillerExpression;
	}

	public boolean isWithCount() {
		return withCount;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitOverflow( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		separatorExpression.appendHqlString( hql, context );
		hql.append( " on overflow " );
		if ( fillerExpression == null ) {
			hql.append( "error" );
		}
		else {
			hql.append( "truncate " );
			fillerExpression.appendHqlString( hql, context );
			if ( withCount ) {
				hql.append( " with count" );
			}
			else {
				hql.append( " without count" );
			}
		}
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof SqmOverflow<?> that
			&& this.withCount == that.withCount
			&& Objects.equals( this.separatorExpression, that.separatorExpression )
			&& Objects.equals( this.fillerExpression, that.fillerExpression );
	}

	@Override
	public int hashCode() {
		return Objects.hash( separatorExpression, fillerExpression, withCount );
	}
}
