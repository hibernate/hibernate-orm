package org.hibernate.hql.ast.tree;

/**
 * Contract for nodes representing unary operators.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface UnaryOperatorNode extends OperatorNode {
	/**
	 * Retrievs the node representing the operator's single operand.
	 * 
	 * @return The operator's single operand.
	 */
	public Node getOperand();
}
