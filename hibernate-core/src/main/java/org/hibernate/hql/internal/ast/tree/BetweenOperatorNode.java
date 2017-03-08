/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
