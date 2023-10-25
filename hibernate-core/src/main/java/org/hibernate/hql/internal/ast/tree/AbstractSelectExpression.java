/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.Arrays;

import org.hibernate.HibernateException;
import org.hibernate.hql.internal.antlr.HqlSqlTokenTypes;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Partial implementation of SelectExpression for all the nodes that aren't constructors.
 *
 * @author Joshua Davis
 */
public abstract class AbstractSelectExpression extends HqlSqlWalkerNode implements SelectExpression {
	
	private String alias;
	private int scalarColumnIndex = -1;
	
	public final void setAlias(String alias) {
		this.alias = alias;
	}
	
	public final String getAlias() {
		return alias;
	}

	public boolean isConstructor() {
		return false;
	}

	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	public FromElement getFromElement() {
		return null;
	}

	public boolean isScalar() throws SemanticException {
		// Default implementation:
		// If this node has a data type, and that data type is not an association, then this is scalar.
		Type type = getDataType();
		return type != null && !type.isAssociationType();	// Moved here from SelectClause [jsd]
	}

	public void setScalarColumn(int i) throws SemanticException {
		this.scalarColumnIndex = i;
		setScalarColumnText( i );
	}

	public int getScalarColumnIndex() {
		return scalarColumnIndex;
	}

	protected static String[] extractMutationTexts(Node operand, int count) {
		if ( operand instanceof ParameterNode ) {
			String[] rtn = new String[count];
			Arrays.fill( rtn, "?" );
			return rtn;
		}
		else if ( operand.getType() == HqlSqlTokenTypes.VECTOR_EXPR ) {
			String[] rtn = new String[operand.getNumberOfChildren()];
			int x = 0;
			AST node = operand.getFirstChild();
			while ( node != null ) {
				rtn[x++] = node.getText();
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
			String[] splits = StringHelper.split( ",", nodeText );
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
