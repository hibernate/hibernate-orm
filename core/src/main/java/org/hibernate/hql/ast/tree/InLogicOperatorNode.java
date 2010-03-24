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

import java.util.ArrayList;
import java.util.List;

import antlr.SemanticException;
import antlr.collections.AST;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.HqlTokenTypes;
import org.hibernate.param.ParameterSpecification;
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
		SessionFactoryImplementor sessionFactory = getSessionFactoryHelper().getFactory();
		if ( sessionFactory.getDialect().supportsRowValueConstructorSyntaxInInList() )
			return;
        
        Type lhsType = extractDataType( lhs );
        if ( lhsType == null )
            return;
        int lhsColumnSpan = lhsType.getColumnSpan( sessionFactory );
        Node rhsNode = (Node) inList.getFirstChild();
        if ( !isNodeAcceptable( rhsNode ) )
            return;
        int rhsColumnSpan = 0;
        if ( rhsNode.getType() == HqlTokenTypes.VECTOR_EXPR ) {
            rhsColumnSpan = rhsNode.getNumberOfChildren();
        } else {
            Type rhsType = extractDataType( rhsNode );
            if ( rhsType == null )
                return;
            rhsColumnSpan = rhsType.getColumnSpan( sessionFactory );
        }
		if ( lhsColumnSpan > 1 && rhsColumnSpan > 1 ) {
			mutateRowValueConstructorSyntaxInInListSyntax( lhsColumnSpan, rhsColumnSpan );
		}
	}
	
	/**
	 * this is possible for parameter lists and explicit lists. It is completely unreasonable for sub-queries.
	 */
    private boolean isNodeAcceptable( Node rhsNode ) {
        return rhsNode instanceof LiteralNode
                || rhsNode instanceof ParameterNode
                || rhsNode.getType() == HqlTokenTypes.VECTOR_EXPR;
    }
    /**
     * Mutate the subtree relating to a row-value-constructor in "in" list to instead use
     * a series of ORen and ANDed predicates.  This allows multi-column type comparisons
     * and explicit row-value-constructor in "in" list syntax even on databases which do
     * not support row-value-constructor in "in" list.
     * <p/>
     * For example, here we'd mutate "... where (col1, col2) in ( ('val1', 'val2'), ('val3', 'val4') ) ..." to
     * "... where (col1 = 'val1' and col2 = 'val2') or (col1 = 'val3' and val2 = 'val4') ..."
     *
     * @param lhsColumnSpan The number of elements in the row value constructor list.
     */
    private void mutateRowValueConstructorSyntaxInInListSyntax(
            int lhsColumnSpan, int rhsColumnSpan ) {
        String[] lhsElementTexts = extractMutationTexts( getLeftHandOperand(),
                lhsColumnSpan );
        Node rhsNode = (Node) getInList().getFirstChild();

        ParameterSpecification lhsEmbeddedCompositeParameterSpecification = getLeftHandOperand() == null
                || ( !ParameterNode.class.isInstance( getLeftHandOperand() ) ) ? null
                : ( (ParameterNode) getLeftHandOperand() )
                        .getHqlParameterSpecification();
        /**
         * only one element in "in" cluster, e.g.
         * <code> where (a,b) in ( (1,2) ) </code> this will be mutated to
         * <code>where a=1 and b=2 </code>
         */
        if ( rhsNode != null && rhsNode.getNextSibling() == null ) {
            String[] rhsElementTexts = extractMutationTexts( rhsNode,
                    rhsColumnSpan );
            setType( HqlSqlTokenTypes.AND );
            setText( "AND" );
            ParameterSpecification rhsEmbeddedCompositeParameterSpecification = rhsNode == null
                    || ( !ParameterNode.class.isInstance( rhsNode ) ) ? null
                    : ( (ParameterNode) rhsNode )
                            .getHqlParameterSpecification();
            translate( lhsColumnSpan, HqlSqlTokenTypes.EQ, "=", lhsElementTexts,
                    rhsElementTexts,
                    lhsEmbeddedCompositeParameterSpecification,
                    rhsEmbeddedCompositeParameterSpecification, this );
        } else {
            List andElementsNodeList = new ArrayList();
            while ( rhsNode != null ) {
                String[] rhsElementTexts = extractMutationTexts( rhsNode,
                        rhsColumnSpan );
                AST and = getASTFactory().create( HqlSqlTokenTypes.AND, "AND" );
                ParameterSpecification rhsEmbeddedCompositeParameterSpecification = rhsNode == null
                        || ( !ParameterNode.class.isInstance( rhsNode ) ) ? null
                        : ( (ParameterNode) rhsNode )
                                .getHqlParameterSpecification();
                translate( lhsColumnSpan, HqlSqlTokenTypes.EQ, "=",
                        lhsElementTexts, rhsElementTexts,
                        lhsEmbeddedCompositeParameterSpecification,
                        rhsEmbeddedCompositeParameterSpecification, and );
                andElementsNodeList.add( and );
                rhsNode = (Node) rhsNode.getNextSibling();
            }
            setType( HqlSqlTokenTypes.OR );
            setText( "OR" );
            AST curNode = this;
            for ( int i = andElementsNodeList.size() - 1; i > 1; i-- ) {
                AST or = getASTFactory().create( HqlSqlTokenTypes.OR, "OR" );
                curNode.setFirstChild( or );
                curNode = or;
                AST and = (AST) andElementsNodeList.get( i );
                or.setNextSibling( and );
            }
            AST node0 = (AST) andElementsNodeList.get( 0 );
            AST node1 = (AST) andElementsNodeList.get( 1 );
            node0.setNextSibling( node1 );
            curNode.setFirstChild( node0 );
        }
    }
}
