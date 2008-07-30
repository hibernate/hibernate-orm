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
