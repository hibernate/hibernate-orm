/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier applied to a subquery as
 * part of a comparison.
 *
 * @author Steve Ebersole
 */
public class SqmModifiedSubQueryExpression<T> extends AbstractSqmExpression<T> {
	public enum Modifier {
		ALL,
		ANY,
		SOME,
	}

	private final SqmSubQuery<T> subQuery;
	private final Modifier modifier;

	public SqmModifiedSubQueryExpression(
			SqmSubQuery<T> subquery,
			Modifier modifier,
			NodeBuilder builder) {
		this (
				subquery,
				modifier,
				subquery.getNodeType(),
				builder
		);
	}

	public SqmModifiedSubQueryExpression(
			SqmSubQuery<T> subQuery,
			Modifier modifier,
			SqmExpressible<T> resultType,
			NodeBuilder builder) {
		super( resultType, builder );
		this.subQuery = subQuery;
		this.modifier = modifier;
	}

	@Override
	public SqmModifiedSubQueryExpression<T> copy(SqmCopyContext context) {
		final SqmModifiedSubQueryExpression<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmModifiedSubQueryExpression<T> expression = context.registerCopy(
				this,
				new SqmModifiedSubQueryExpression<>(
						subQuery.copy( context ),
						modifier,
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public SqmSubQuery<T> getSubQuery() {
		return subQuery;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitModifiedSubQueryExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder hql) {
		hql.append( modifier );
		hql.append( " (" );
		subQuery.appendHqlString( hql );
		hql.append( ')' );
	}
}
