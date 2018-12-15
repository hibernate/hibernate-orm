/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * A string concatenation.
 *
 * @author Steve Ebersole
 */
public class ConcatExpression extends AbstractExpression<String> {
	private ExpressionImplementor<String> first;
	private ExpressionImplementor<String> second;

	public ConcatExpression(
			ExpressionImplementor<String> expression1,
			ExpressionImplementor<String> expression2,
			CriteriaNodeBuilder builder) {
		super( String.class, builder );
		this.first = expression1;
		this.second = expression2;
	}

	public ExpressionImplementor<String> getFirstExpression() {
		return first;
	}

	public ExpressionImplementor<String> getSecondExpression() {
		return second;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitConcatExpression( this );
	}
}
