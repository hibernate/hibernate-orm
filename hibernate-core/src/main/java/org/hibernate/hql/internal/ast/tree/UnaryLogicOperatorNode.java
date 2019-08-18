/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import antlr.SemanticException;

import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Represents a unary operator node.
 *
 * @author Steve Ebersole
 */
public class UnaryLogicOperatorNode extends AbstractSelectExpression implements UnaryOperatorNode {
	@Override
	public Node getOperand() {
		return (Node) getFirstChild();
	}

	@Override
	public void initialize() {
		// nothing to do; even if the operand is a parameter, no way we could
		// infer an appropriate expected type here
	}

	@Override
	public Type getDataType() {
		// logic operators by definition resolve to booleans
		return StandardBasicTypes.BOOLEAN;
	}

	@Override
	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}
}
