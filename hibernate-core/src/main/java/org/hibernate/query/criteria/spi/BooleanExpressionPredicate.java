/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.criteria.Expression;

/**
 * Defines a {@link PredicateImplementor} used to wrap a Boolean-valued
 * {@link ExpressionImplementor}.
 * 
 * @author Steve Ebersole
 */
public class BooleanExpressionPredicate extends AbstractSimplePredicate {
	private final Expression<Boolean> expression;

	public BooleanExpressionPredicate(Expression<Boolean> expression, CriteriaNodeBuilder criteriaBuilder) {
		super( criteriaBuilder );
		this.expression = expression;
	}

	/**
	 * Get the boolean expression defining the predicate.
	 * 
	 * @return The underlying boolean expression.
	 */
	public Expression<Boolean> getExpression() {
		return expression;
	}

	@Override
	public <R> R accept(JpaCriteriaVisitor visitor) {
		return visitor.visitBooleanExpressionPredicate( this );
	}
}
