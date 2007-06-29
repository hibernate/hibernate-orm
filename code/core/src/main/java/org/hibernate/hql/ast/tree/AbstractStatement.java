// $Id: AbstractStatement.java 7486 2005-07-15 04:39:41Z oneovthafew $
package org.hibernate.hql.ast.tree;

import java.util.Iterator;

/**
 * Convenience implementation of Statement to centralize common functionality.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractStatement extends HqlSqlWalkerNode implements DisplayableNode, Statement {

	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();
		if ( getWalker().getQuerySpaces().size() > 0 ) {
			buf.append( " querySpaces (" );
			for ( Iterator iterator = getWalker().getQuerySpaces().iterator(); iterator.hasNext(); ) {
				buf.append( iterator.next() );
				if ( iterator.hasNext() ) {
					buf.append( "," );
				}
			}
			buf.append( ")" );
		}
		return buf.toString();
	}
}
