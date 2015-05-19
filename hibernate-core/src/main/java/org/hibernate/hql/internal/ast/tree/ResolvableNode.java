/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
import antlr.SemanticException;
import antlr.collections.AST;

/**
 * The contract for expression sub-trees that can resolve themselves.
 *
 * @author josh
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
