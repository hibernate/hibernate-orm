/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;

/**
 * A string concatenation.
 *
 * @author Steve Ebersole
 */
public class ConcatExpression extends AbstractExpression<String> {
	private Expression<String> first;
	private Expression<String> second;

	public ConcatExpression(
			Expression<String> expression1,
			Expression<String> expression2,
			CriteriaNodeBuilder builder) {
		super( String.class, builder );
		this.first = expression1;
		this.second = expression2;
	}

	public Expression<String> getFirstExpression() {
		return first;
	}

	public Expression<String> getSecondExpression() {
		return second;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitConcatExpression( this );
	}
}
