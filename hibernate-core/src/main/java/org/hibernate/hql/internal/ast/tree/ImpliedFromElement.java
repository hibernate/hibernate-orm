/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
