// $Id: ResolvableNode.java 7460 2005-07-12 20:27:29Z steveebersole $
package org.hibernate.hql.ast.tree;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * The contract for expression sub-trees that can resolve themselves.
 *
 * @author josh Sep 25, 2004 11:27:36 AM
 */
public interface ResolvableNode {
	/**
	 * Does the work of resolving an identifier or a dot
	 */
	void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) throws SemanticException;

	/**
	 * Does the work of resolving an identifier or a dot, but without a parent node
	 */
	void resolve(boolean generateJoin, boolean implicitJoin, String classAlias) throws SemanticException;

	/**
	 * Does the work of resolving an identifier or a dot, but without a parent node or alias
	 */
	void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException;

	/**
	 * Does the work of resolving inside of the scope of a function call
	 */
	void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException;

	/**
	 * Does the work of resolving an an index [].
	 */
	void resolveIndex(AST parent) throws SemanticException;

}
