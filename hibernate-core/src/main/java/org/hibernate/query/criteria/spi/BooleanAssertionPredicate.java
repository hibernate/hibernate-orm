/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Predicate to assert the explicit value of a boolean expression:<ul>
 * <li>x = true</li>
 * <li>x = false</li>
 * <li>x <> true</li>
 * <li>x <> false</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class BooleanAssertionPredicate extends AbstractSimplePredicate {
	private final ExpressionImplementor<Boolean> expression;
	private final Boolean assertedValue;

	public BooleanAssertionPredicate(
			ExpressionImplementor<Boolean> expression,
			Boolean assertedValue,
			CriteriaNodeBuilder builder) {
		super( builder );
		this.expression = expression;
		this.assertedValue = assertedValue;
	}

	public ExpressionImplementor<Boolean> getExpression() {
		return expression;
	}

	public Boolean getAssertedValue() {
		return assertedValue;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitBooleanAssertionPredicate( this );
	}
}
