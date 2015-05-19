/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.internal.antlr.HqlTokenTypes;
import org.hibernate.param.ParameterSpecification;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * @author Steve Ebersole
 */
public class InLogicOperatorNode extends BinaryLogicOperatorNode implements BinaryOperatorNode {
	public Node getInList() {
		return getRightHandOperand();
	}

	@Override
	public void initialize() throws SemanticException {
		final Node lhs = getLeftHandOperand();
		if ( lhs == null ) {
			throw new SemanticException( "left-hand operand of in operator was null" );
		}

		final Node inList = getInList();
		if ( inList == null ) {
			throw new SemanticException( "right-hand operand of in operator was null" );
		}

		// for expected parameter type injection, we expect that the lhs represents
		// some form of property ref and that the children of the in-list represent
		// one-or-more params.
		if ( SqlNode.class.isAssignableFrom( lhs.getClass() ) ) {
			Type lhsType = ( (SqlNode) lhs ).getDataType();
			AST inListChild = inList.getFirstChild();
			while ( inListChild != null ) {
				if ( ExpectedTypeAwareNode.class.isAssignableFrom( inListChild.getClass() ) ) {
					( (ExpectedTypeAwareNode) inListChild ).setExpectedType( lhsType );
				}

				// fix for HHH-9605
				if ( CollectionFunction.class.isInstance( inListChild )
						&& ExpectedTypeAwareNode.class.isInstance( lhs ) ) {
					final Type rhsType = ((CollectionFunction) inListChild).getDataType();
					((ExpectedTypeAwareNode) lhs).setExpectedType( rhsType );
				}

				inListChild = inListChild.getNextSibling();
			}
		}

		final SessionFactoryImplementor sessionFactory = getSessionFactoryHelper().getFactory();
		if ( sessionFactory.getDialect().supportsRowValueConstructorSyntaxInInList() ) {
			return;
		}

		final Type lhsType = extractDataType( lhs );
		if ( lhsType == null ) {
			return;
		}
		final int lhsColumnSpan = lhsType.getColumnSpan( sessionFactory );

		final Node rhsNode = (Node) inList.getFirstChild();
		if ( !isNodeAcceptable( rhsNode ) ) {
			return;
		}
		int rhsColumnSpan;
		if ( rhsNode == null ) {
			// early exit for empty IN list
			return;
		}
		else if ( rhsNode.getType() == HqlTokenTypes.VECTOR_EXPR ) {
			rhsColumnSpan = rhsNode.getNumberOfChildren();
		}
		else {
			final Type rhsType = extractDataType( rhsNode );
			if ( rhsType == null ) {
				return;
			}
			rhsColumnSpan = rhsType.getColumnSpan( sessionFactory );
		}

		if ( lhsColumnSpan > 1 && rhsColumnSpan > 1 ) {
			mutateRowValueConstructorSyntaxInInListSyntax( lhsColumnSpan, rhsColumnSpan );
		}
	}

	/**
	 * this is possible for parameter lists and explicit lists. It is completely unreasonable for sub-queries.
	 */
	private boolean isNodeAcceptable(Node rhsNode) {
		return rhsNode == null /* empty IN list */ || rhsNode instanceof LiteralNode
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
			int lhsColumnSpan, int rhsColumnSpan) {
		String[] lhsElementTexts = extractMutationTexts(
				getLeftHandOperand(),
				lhsColumnSpan
		);
		Node rhsNode = (Node) getInList().getFirstChild();

		ParameterSpecification lhsEmbeddedCompositeParameterSpecification = getLeftHandOperand() == null
				|| ( !ParameterNode.class.isInstance( getLeftHandOperand() ) ) ? null
				: ( (ParameterNode) getLeftHandOperand() )
				.getHqlParameterSpecification();

		final boolean negated = getType() == HqlSqlTokenTypes.NOT_IN;

		if ( rhsNode != null && rhsNode.getNextSibling() == null ) {
			/**
			 * only one element in the vector grouping.
			 * <code> where (a,b) in ( (1,2) ) </code> this will be mutated to
			 * <code>where a=1 and b=2 </code>
			 */
			String[] rhsElementTexts = extractMutationTexts( rhsNode, rhsColumnSpan );
			setType( negated ? HqlTokenTypes.OR : HqlSqlTokenTypes.AND );
			setText( negated ? "or" : "and" );
			ParameterSpecification rhsEmbeddedCompositeParameterSpecification =
					rhsNode == null || ( !ParameterNode.class.isInstance( rhsNode ) )
							? null
							: ( (ParameterNode) rhsNode ).getHqlParameterSpecification();
			translate(
					lhsColumnSpan,
					negated ? HqlSqlTokenTypes.NE : HqlSqlTokenTypes.EQ,
					negated ? "<>" : "=",
					lhsElementTexts,
					rhsElementTexts,
					lhsEmbeddedCompositeParameterSpecification,
					rhsEmbeddedCompositeParameterSpecification,
					this
			);
		}
		else {
			List andElementsNodeList = new ArrayList();
			while ( rhsNode != null ) {
				String[] rhsElementTexts = extractMutationTexts( rhsNode, rhsColumnSpan );
				AST group = getASTFactory().create(
						negated ? HqlSqlTokenTypes.OR : HqlSqlTokenTypes.AND,
						negated ? "or" : "and"
				);
				ParameterSpecification rhsEmbeddedCompositeParameterSpecification =
						rhsNode == null || ( !ParameterNode.class.isInstance( rhsNode ) )
								? null
								: ( (ParameterNode) rhsNode ).getHqlParameterSpecification();
				translate(
						lhsColumnSpan,
						negated ? HqlSqlTokenTypes.NE : HqlSqlTokenTypes.EQ,
						negated ? "<>" : "=",
						lhsElementTexts,
						rhsElementTexts,
						lhsEmbeddedCompositeParameterSpecification,
						rhsEmbeddedCompositeParameterSpecification,
						group
				);
				andElementsNodeList.add( group );
				rhsNode = (Node) rhsNode.getNextSibling();
			}
			setType( negated ? HqlSqlTokenTypes.AND : HqlSqlTokenTypes.OR );
			setText( negated ? "and" : "or" );
			AST curNode = this;
			for ( int i = andElementsNodeList.size() - 1; i > 1; i-- ) {
				AST group = getASTFactory().create(
						negated ? HqlSqlTokenTypes.AND : HqlSqlTokenTypes.OR,
						negated ? "and" : "or"
				);
				curNode.setFirstChild( group );
				curNode = group;
				AST and = (AST) andElementsNodeList.get( i );
				group.setNextSibling( and );
			}
			AST node0 = (AST) andElementsNodeList.get( 0 );
			AST node1 = (AST) andElementsNodeList.get( 1 );
			node0.setNextSibling( node1 );
			curNode.setFirstChild( node0 );
		}
	}
}
