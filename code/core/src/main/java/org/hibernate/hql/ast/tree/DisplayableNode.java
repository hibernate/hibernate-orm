// $Id: DisplayableNode.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

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
