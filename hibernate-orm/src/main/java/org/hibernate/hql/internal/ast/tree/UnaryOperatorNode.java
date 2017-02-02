/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;


/**
 * Contract for nodes representing unary operators.
 *
 * @author Steve Ebersole
 */
public interface UnaryOperatorNode extends OperatorNode {
	/**
	 * Retrievs the node representing the operator's single operand.
	 * 
	 * @return The operator's single operand.
	 */
	public Node getOperand();
}
