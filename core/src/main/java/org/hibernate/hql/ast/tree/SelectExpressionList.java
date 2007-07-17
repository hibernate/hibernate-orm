// $Id: SelectExpressionList.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import java.util.ArrayList;

import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ASTPrinter;

import antlr.collections.AST;

/**
 * Common behavior - a node that contains a list of select expressions.
 *
 * @author josh Nov 6, 2004 8:51:00 AM
 */
public abstract class SelectExpressionList extends HqlSqlWalkerNode {
	/**
	 * Returns an array of SelectExpressions gathered from the children of the given parent AST node.
	 *
	 * @return an array of SelectExpressions gathered from the children of the given parent AST node.
	 */
	public SelectExpression[] collectSelectExpressions() {
		// Get the first child to be considered.  Sub-classes may do this differently in order to skip nodes that
		// are not select expressions (e.g. DISTINCT).
		AST firstChild = getFirstSelectExpression();
		AST parent = this;
		ArrayList list = new ArrayList( parent.getNumberOfChildren() );
		for ( AST n = firstChild; n != null; n = n.getNextSibling() ) {
			if ( n instanceof SelectExpression ) {
				list.add( n );
			}
			else {
				throw new IllegalStateException( "Unexpected AST: " + n.getClass().getName() + " " + new ASTPrinter( SqlTokenTypes.class ).showAsString( n, "" ) );
			}
		}
		return ( SelectExpression[] ) list.toArray( new SelectExpression[list.size()] );
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
