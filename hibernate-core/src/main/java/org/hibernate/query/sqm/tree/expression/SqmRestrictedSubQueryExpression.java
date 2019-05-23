/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier appplied to a subquery as
 * part of a comparison.
 *
 * @author Steve Ebersole
 */
public class SqmRestrictedSubQueryExpression<T> extends AbstractSqmExpression<T> {
	public enum Modifier {
		ALL,
		ANY,
		SOME,
	}

	private final SqmSubQuery<T> subQuery;
	private final Modifier modifier;

	public SqmRestrictedSubQueryExpression(
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

	public SqmRestrictedSubQueryExpression(
			SqmSubQuery<T> subQuery,
			Modifier modifier,
			SqmExpressable<T> resultType,
			NodeBuilder builder) {
		super( resultType, builder );
		this.subQuery = subQuery;
		this.modifier = modifier;
	}

	public Modifier getModifier() {
		return modifier;
	}

	public SqmSubQuery<T> getSubQuery() {
		return subQuery;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRestrictedSubQueryExpression( this );
	}
}
