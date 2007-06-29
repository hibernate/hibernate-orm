// $Id: ImpliedFromElement.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

/**
 * Represents a FROM element implied by a path expression or a collection reference.
 *
 * @author josh Feb 10, 2005 12:31:03 AM
 */
public class ImpliedFromElement extends FromElement {
	/**
	 * True if this from element was implied from a path in the FROM clause, but not
	 * explicitly declard in the from clause.
	 */
	private boolean impliedInFromClause = false;

	/**
	 * True if this implied from element should be included in the projection list.
	 */
	private boolean inProjectionList = false;

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
		StringBuffer buf = new StringBuffer();
		buf.append( "ImpliedFromElement{" );
		appendDisplayText( buf );
		buf.append( "}" );
		return buf.toString();
	}
}
