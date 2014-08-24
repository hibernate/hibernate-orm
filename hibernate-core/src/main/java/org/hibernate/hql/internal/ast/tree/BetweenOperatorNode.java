/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Contract for nodes representing logical BETWEEN (ternary) operators.
 *
 * @author Steve Ebersole
 */
public class BetweenOperatorNode extends SqlNode implements OperatorNode {

	public void initialize() throws SemanticException {
		final Node fixture = getFixtureOperand();
		if ( fixture == null ) {
			throw new SemanticException( "fixture operand of a between operator was null" );
		}

		final Node low = getLowOperand();
		if ( low == null ) {
			throw new SemanticException( "low operand of a between operator was null" );
		}

		final Node high = getHighOperand();
		if ( high == null ) {
			throw new SemanticException( "high operand of a between operator was null" );
		}

		Type expectedType = null;
		if ( fixture instanceof SqlNode ) {
			expectedType = ( (SqlNode) fixture ).getDataType();
		}
		if ( expectedType == null && low instanceof SqlNode ) {
			expectedType = ( (SqlNode) low ).getDataType();
		}
		if ( expectedType == null && high instanceof SqlNode ) {
			expectedType = ( (SqlNode) high ).getDataType();
		}

		if ( fixture instanceof ExpectedTypeAwareNode ) {
			( (ExpectedTypeAwareNode) fixture ).setExpectedType( expectedType );
		}
		if ( low instanceof ExpectedTypeAwareNode ) {
			( (ExpectedTypeAwareNode) low ).setExpectedType( expectedType );
		}
		if ( high instanceof ExpectedTypeAwareNode ) {
			( (ExpectedTypeAwareNode) high ).setExpectedType( expectedType );
		}
	}

	@Override
	public Type getDataType() {
		// logic operators by definition resolve to boolean.
		return StandardBasicTypes.BOOLEAN;
	}

	public Node getFixtureOperand() {
		return (Node) getFirstChild();
	}

	public Node getLowOperand() {
		return (Node) getFirstChild().getNextSibling();
	}

	public Node getHighOperand() {
		return (Node) getFirstChild().getNextSibling().getNextSibling();
	}

}
