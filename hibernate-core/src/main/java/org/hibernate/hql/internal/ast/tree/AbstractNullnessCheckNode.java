/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.type.Type;

import antlr.collections.AST;

/**
 * Base class for nodes dealing 'is null' and 'is not null' operators.
 * <p/>
 * todo : a good deal of this is copied from BinaryLogicOperatorNode; look at consolidating these code fragments
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNullnessCheckNode extends UnaryLogicOperatorNode {
	@Override
	public void initialize() {
		// TODO : this really needs to be delayed until after we definitively know the operand node type;
		// where this is currently a problem is parameters for which we cannot unequivocally
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

	/**
	 * When (if) we need to expand a row value constructor, what is the type of connector to use between the
	 * expansion fragments.
	 *
	 * @return The expansion connector type.
	 */
	protected abstract int getExpansionConnectorType();

	/**
	 * When (if) we need to expand a row value constructor, what is the text of the connector to use between the
	 * expansion fragments.
	 *
	 * @return The expansion connector text.
	 */
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

	private static Type extractDataType(Node operand) {
		if ( operand instanceof SqlNode ) {
			return ( (SqlNode) operand ).getDataType();
		}

		if ( operand instanceof ExpectedTypeAwareNode ) {
			return ( (ExpectedTypeAwareNode) operand ).getExpectedType();
		}

		return null;
	}

}
