// $Id: InitializeableNode.java 7460 2005-07-12 20:27:29Z steveebersole $

package org.hibernate.hql.ast.tree;

/**
 * An interface for initializeable AST nodes.
 */
public interface InitializeableNode {
	/**
	 * Initializes the node with the parameter.
	 *
	 * @param param the initialization parameter.
	 */
	void initialize(Object param);
}
