package org.hibernate.hql.ast.tree;

/**
 * Contract for nodes representing binary operators.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
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
