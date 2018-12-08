/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.io.Serializable;

/**
 * Models a <tt>BETWEEN</tt> {@link javax.persistence.criteria.Predicate}.
 *
 * @author Steve Ebersole
 */
public class BetweenPredicate<Y> extends AbstractSimplePredicate {
	private final ExpressionImplementor<? extends Y> expression;
	private final ExpressionImplementor<? extends Y> lowerBound;
	private final ExpressionImplementor<? extends Y> upperBound;

	public BetweenPredicate(
			ExpressionImplementor<? extends Y> expression,
			ExpressionImplementor<? extends Y> lowerBound,
			ExpressionImplementor<? extends Y> upperBound,
			CriteriaNodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public ExpressionImplementor<? extends Y> getExpression() {
		return expression;
	}

	public ExpressionImplementor<? extends Y> getLowerBound() {
		return lowerBound;
	}

	public ExpressionImplementor<? extends Y> getUpperBound() {
		return upperBound;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitBetweenPredicate( this );
	}
}
