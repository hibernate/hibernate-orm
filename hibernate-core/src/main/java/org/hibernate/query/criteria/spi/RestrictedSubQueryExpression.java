/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.query.criteria.JpaSubQuery;

/**
 * Represents a {@link Modifier#ALL}, {@link Modifier#ANY}, {@link Modifier#SOME} modifier appplied to a subquery as
 * part of a comparison.
 *
 * @author Steve Ebersole
 */
public class RestrictedSubQueryExpression<Y> extends AbstractExpression<Y> {
	public enum Modifier {
		ALL,
		SOME,
		ANY;
	}

	private final JpaSubQuery<Y> subQuery;
	private final Modifier modifier;

	public RestrictedSubQueryExpression(
			Class<Y> javaType,
			JpaSubQuery<Y> subQuery,
			Modifier modifier,
			CriteriaNodeBuilder builder) {
		super( javaType, builder );
		this.subQuery = subQuery;
		this.modifier = modifier;
	}

	public RestrictedSubQueryExpression(
			JpaSubQuery<Y> subquery,
			Modifier modifier,
			CriteriaNodeBuilder builder) {
		this ( subquery.getResultType(), subquery, modifier, builder );
	}

	public Modifier getModifier() {
		return modifier;
	}

	public JpaSubQuery<Y> getSubQuery() {
		return subQuery;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.acceptRestrictedSubQueryExpression( this );
	}
}
