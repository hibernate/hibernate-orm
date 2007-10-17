/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.hql.ast.tree;

import antlr.collections.AST;

import org.hibernate.type.Type;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.util.StringHelper;
import org.hibernate.HibernateException;

/**
 * AbstractNullnessCheckNode implementation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNullnessCheckNode extends UnaryLogicOperatorNode {
	public void initialize() {
		// TODO : this really needs to be delayed unitl after we definitively know the operand node type;
		// where this is currently a problem is parameters for which where we cannot unequivocally
		// resolve an expected type
		Type operandType = extractDataType( getOperand() );
		if ( operandType == null ) {
			return;
		}
		SessionFactoryImplementor sessionFactory = getSessionFactoryHelper().getFactory();
		int operandColumnSpan = operandType.getColumnSpan( sessionFactory );
		if ( operandColumnSpan > 1 ) {
			mutateRowValueConstructorSyntax( operandColumnSpan );
		}
	}

	protected abstract int getExpansionConnectorType();
	protected abstract String getExpansionConnectorText();

	private void mutateRowValueConstructorSyntax(int operandColumnSpan) {
		final int comparisonType = getType();
		final String comparisonText = getText();

		final int expansionConnectorType = getExpansionConnectorType();
		final String expansionConnectorText = getExpansionConnectorText();

		setType( expansionConnectorType );
		setText( expansionConnectorText );

		String[] mutationTexts = extractMutationTexts( getOperand(), operandColumnSpan );

		AST container = this;
		for ( int i = operandColumnSpan - 1; i > 0; i-- ) {
			if ( i == 1 ) {
				AST op1 = getASTFactory().create( comparisonType, comparisonText );
				AST operand1 = getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, mutationTexts[0] );
				op1.setFirstChild( operand1 );
				container.setFirstChild( op1 );
				AST op2 = getASTFactory().create( comparisonType, comparisonText );
				AST operand2 = getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, mutationTexts[1] );
				op2.setFirstChild( operand2 );
				op1.setNextSibling( op2 );
			}
			else {
				AST op = getASTFactory().create( comparisonType, comparisonText );
				AST operand = getASTFactory().create( HqlSqlTokenTypes.SQL_TOKEN, mutationTexts[i] );
				op.setFirstChild( operand );
				AST newContainer = getASTFactory().create( expansionConnectorType, expansionConnectorText );
				container.setFirstChild( newContainer );
				newContainer.setNextSibling( op );
				container = newContainer;
			}
		}
	}

	protected Type extractDataType(Node operand) {
		Type type = null;
		if ( operand instanceof SqlNode ) {
			type = ( ( SqlNode ) operand ).getDataType();
		}
		if ( type == null && operand instanceof ExpectedTypeAwareNode ) {
			type = ( ( ExpectedTypeAwareNode ) operand ).getExpectedType();
		}
		return type;
	}

	private static String[] extractMutationTexts(Node operand, int count) {
		if ( operand instanceof ParameterNode ) {
			String[] rtn = new String[count];
			for ( int i = 0; i < count; i++ ) {
				rtn[i] = "?";
			}
			return rtn;
		}
		else if ( operand.getType() == HqlSqlTokenTypes.VECTOR_EXPR ) {
			String[] rtn = new String[ operand.getNumberOfChildren() ];
			int x = 0;
			AST node = operand.getFirstChild();
			while ( node != null ) {
				rtn[ x++ ] = node.getText();
				node = node.getNextSibling();
			}
			return rtn;
		}
		else if ( operand instanceof SqlNode ) {
			String nodeText = operand.getText();
			if ( nodeText.startsWith( "(" ) ) {
				nodeText = nodeText.substring( 1 );
			}
			if ( nodeText.endsWith( ")" ) ) {
				nodeText = nodeText.substring( 0, nodeText.length() - 1 );
			}
			String[] splits = StringHelper.split( ", ", nodeText );
			if ( count != splits.length ) {
				throw new HibernateException( "SqlNode's text did not reference expected number of columns" );
			}
			return splits;
		}
		else {
			throw new HibernateException( "dont know how to extract row value elements from node : " + operand );
		}
	}
}
