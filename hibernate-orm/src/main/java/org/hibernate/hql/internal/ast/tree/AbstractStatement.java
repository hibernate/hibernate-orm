/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
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
		StringBuilder buf = new StringBuilder();
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
