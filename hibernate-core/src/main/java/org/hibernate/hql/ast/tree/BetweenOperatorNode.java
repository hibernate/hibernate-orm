/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.ast.tree;

import org.hibernate.type.Type;
import org.hibernate.Hibernate;
import antlr.SemanticException;

/**
 * Contract for nodes representing logcial BETWEEN (ternary) operators.
 *
 * @author Steve Ebersole
 */
public class BetweenOperatorNode extends SqlNode implements OperatorNode {

	public void initialize() throws SemanticException {
		Node fixture = getFixtureOperand();
		if ( fixture == null ) {
			throw new SemanticException( "fixture operand of a between operator was null" );
		}
		Node low = getLowOperand();
		if ( low == null ) {
			throw new SemanticException( "low operand of a between operator was null" );
		}
		Node high = getHighOperand();
		if ( high == null ) {
			throw new SemanticException( "high operand of a between operator was null" );
		}
		check( fixture, low, high );
		check( low, high, fixture );
		check( high, fixture, low );
	}

	public Type getDataType() {
		// logic operators by definition resolve to boolean.
		return Hibernate.BOOLEAN;
	}

	public Node getFixtureOperand() {
		return ( Node ) getFirstChild();
	}

	public Node getLowOperand() {
		return ( Node ) getFirstChild().getNextSibling();
	}

	public Node getHighOperand() {
		return ( Node ) getFirstChild().getNextSibling().getNextSibling();
	}

	private void check(Node check, Node first, Node second) {
		if ( ExpectedTypeAwareNode.class.isAssignableFrom( check.getClass() ) ) {
			Type expectedType = null;
			if ( SqlNode.class.isAssignableFrom( first.getClass() ) ) {
				expectedType = ( ( SqlNode ) first ).getDataType();
			}
			if ( expectedType == null && SqlNode.class.isAssignableFrom( second.getClass() ) ) {
				expectedType = ( ( SqlNode ) second ).getDataType();
			}
			( ( ExpectedTypeAwareNode ) check ).setExpectedType( expectedType );
		}
	}
}
