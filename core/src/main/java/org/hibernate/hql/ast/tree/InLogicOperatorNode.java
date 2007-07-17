package org.hibernate.hql.ast.tree;

import antlr.SemanticException;
import antlr.collections.AST;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class InLogicOperatorNode extends BinaryLogicOperatorNode implements BinaryOperatorNode {

	public Node getInList() {
		return getRightHandOperand();
	}

	public void initialize() throws SemanticException {
		Node lhs = getLeftHandOperand();
		if ( lhs == null ) {
			throw new SemanticException( "left-hand operand of in operator was null" );
		}
		Node inList = getInList();
		if ( inList == null ) {
			throw new SemanticException( "right-hand operand of in operator was null" );
		}

		// for expected parameter type injection, we expect that the lhs represents
		// some form of property ref and that the children of the in-list represent
		// one-or-more params.
		if ( SqlNode.class.isAssignableFrom( lhs.getClass() ) ) {
			Type lhsType = ( ( SqlNode ) lhs ).getDataType();
			AST inListChild = inList.getFirstChild();
			while ( inListChild != null ) {
				if ( ExpectedTypeAwareNode.class.isAssignableFrom( inListChild.getClass() ) ) {
					( ( ExpectedTypeAwareNode ) inListChild ).setExpectedType( lhsType );
				}
				inListChild = inListChild.getNextSibling();
			}
		}
	}
}
