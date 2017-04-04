/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;


/**
 * Implementors will return additional display text, which will be used
 * by the ASTPrinter to display information (besides the node type and node
 * text).
 */
public interface DisplayableNode {
	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	String getDisplayText();
}
