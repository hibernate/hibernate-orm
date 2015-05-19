/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.expression;
import javax.persistence.criteria.Expression;

/**
 * Contract for operators with two operands.
 *
 * @author Steve Ebersole
 */
public interface BinaryOperatorExpression<T> extends Expression<T> {
	/**
	 * Get the right-hand operand.
	 *
	 * @return The right-hand operand.
	 */
	public Expression<?> getRightHandOperand();

	/**
	 * Get the left-hand operand.
	 *
	 * @return The left-hand operand.
	 */
	public Expression<?> getLeftHandOperand();
}
