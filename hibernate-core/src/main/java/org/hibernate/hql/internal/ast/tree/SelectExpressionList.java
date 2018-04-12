/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTPrinter;
import org.hibernate.hql.internal.ast.util.TokenPrinters;

import antlr.collections.AST;

/**
 * Common behavior - a node that contains a list of select expressions.
 *
 * @author josh
 */
public abstract class SelectExpressionList extends HqlSqlWalkerNode {

	private List<Integer> parameterPositions = new ArrayList<Integer>();

	/**
	 * Returns an array of SelectExpressions gathered from the children of the given parent AST node.
	 *
	 * @return an array of SelectExpressions gathered from the children of the given parent AST node.
	 */
	public SelectExpression[] collectSelectExpressions() {
		// Get the first child to be considered.  Sub-classes may do this differently in order to skip nodes that
		// are not select expressions (e.g. DISTINCT).
		AST firstChild = getFirstSelectExpression();
		ArrayList<SelectExpression> list = new ArrayList<SelectExpression>();
		int p = 0;
		for ( AST n = firstChild; n != null; n = n.getNextSibling() ) {
			if ( n instanceof SelectExpression ) {
				list.add( (SelectExpression) n );
			}
			else if ( n instanceof ParameterNode ) {
				parameterPositions.add( p );
			}
			else {
				throw new IllegalStateException(
						"Unexpected AST: " + n.getClass().getName() + " "
								+ TokenPrinters.SQL_TOKEN_PRINTER.showAsString( n, "" )
				);
			}
			p++;
		}
		return list.toArray( new SelectExpression[list.size()] );
	}

	/**
	 * The total number of parameter projections of this expression.
	 *
	 * @return The number of parameters in this select clause.
	 */
	public int getTotalParameterCount() {
		return parameterPositions.size();
	}

	/**
	 * The position of parameters within the list of select expressions of this clause
	 *
	 * @return a list of positions representing the mapping from order of occurence to position
	 */
	public List<Integer> getParameterPositions() {
		return parameterPositions;
	}

	/**
	 * Returns the first select expression node that should be considered when building the array of select
	 * expressions.
	 *
	 * @return the first select expression node that should be considered when building the array of select
	 *         expressions
	 */
	protected abstract AST getFirstSelectExpression();

}
