package org.hibernate.hql.ast.tree;

import org.hibernate.type.Type;
import org.hibernate.Hibernate;

/**
 * Represents a unary operator node.
 *
 * @author Steve Ebersole
 */
public class UnaryLogicOperatorNode extends HqlSqlWalkerNode implements UnaryOperatorNode {
	public Node getOperand() {
		return ( Node ) getFirstChild();
	}

	public void initialize() {
		// nothing to do; even if the operand is a parameter, no way we could
		// infer an appropriate expected type here
	}

	public Type getDataType() {
		// logic operators by definition resolve to booleans
		return Hibernate.BOOLEAN;
	}
}
