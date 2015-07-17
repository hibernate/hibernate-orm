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
package org.hibernate.hql.internal.ast.tree;

import antlr.collections.AST;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.type.Type;

/**
 * Base class for nodes dealing 'is null' and 'is not null' operators.
 * <p/>
 * todo : a good deal of this is copied from BinaryLogicOperatorNode; look at consolidating these code fragments
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNullnessCheckNode extends UnaryLogicOperatorNode {

	/**
	 * {@inheritDoc}
	 */
	@Override
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

		String[] mutationTexts = BinaryLogicOperatorNode.extractMutationTexts( getOperand(), operandColumnSpan );

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
		Type type = null;
		if ( operand instanceof SqlNode ) {
			type = ( ( SqlNode ) operand ).getDataType();
		}
		if ( type == null && operand instanceof ExpectedTypeAwareNode ) {
			type = ( ( ExpectedTypeAwareNode ) operand ).getExpectedType();
		}
		return type;
	}
}
