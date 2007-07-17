// $Id: PathNode.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

/**
 * An AST node with a path property.  This path property will be the fully qualified name.
 *
 * @author josh Nov 7, 2004 10:56:49 AM
 */
public interface PathNode {
	/**
	 * Returns the full path name represented by the node.
	 *
	 * @return the full path name represented by the node.
	 */
	String getPath();
}
