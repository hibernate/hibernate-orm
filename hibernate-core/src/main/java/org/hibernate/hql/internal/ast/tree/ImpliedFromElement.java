/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

/**
 * Represents a FROM element implied by a path expression or a collection reference.
 *
 * @author josh
 */
public class ImpliedFromElement extends FromElement {
	/**
	 * True if this from element was implied from a path in the FROM clause, but not
	 * explicitly declard in the from clause.
	 */
	private boolean impliedInFromClause;

	/**
	 * True if this implied from element should be included in the projection list.
	 */
	private boolean inProjectionList;

	public boolean isImplied() {
		return true;
	}

	public void setImpliedInFromClause(boolean flag) {
		impliedInFromClause = flag;
	}

	public boolean isImpliedInFromClause() {
		return impliedInFromClause;
	}

	public void setInProjectionList(boolean inProjectionList) {
		this.inProjectionList = inProjectionList;
	}

	public boolean inProjectionList() {
		return inProjectionList && isFromOrJoinFragment();
	}

	public boolean isIncludeSubclasses() {
		return false;	// Never include subclasses for implied from elements.
	}

	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	public String getDisplayText() {
		StringBuilder buf = new StringBuilder();
		buf.append( "ImpliedFromElement{" );
		appendDisplayText( buf );
		buf.append( "}" );
		return buf.toString();
	}
}
