/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;


/**
 * Contract for nodes representing binary operators.
 *
 * @author Steve Ebersole
 */
public interface BinaryOperatorNode extends OperatorNode {
	/**
	 * Retrieves the left-hand operand of the operator.
	 *
	 * @return The left-hand operand
	 */
	public Node getLeftHandOperand();

	/**
	 * Retrieves the right-hand operand of the operator.
	 *
	 * @return The right-hand operand
	 */
	public Node getRightHandOperand();
}
