// $Id: SqlFragment.java 8215 2005-09-23 16:51:56Z pgmjsd $
package org.hibernate.hql.ast.tree;

import org.hibernate.sql.JoinFragment;

/**
 * Represents an SQL fragment in the AST.
 *
 * @author josh Dec 5, 2004 9:01:52 AM
 */
public class SqlFragment extends Node {
	private JoinFragment joinFragment;
	private FromElement fromElement;

	public void setJoinFragment(JoinFragment joinFragment) {
		this.joinFragment = joinFragment;
	}

	public boolean hasFilterCondition() {
		return joinFragment.hasFilterCondition();
	}

	public void setFromElement(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	public FromElement getFromElement() {
		return fromElement;
	}
}
