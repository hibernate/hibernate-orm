/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;

import org.hibernate.hql.internal.ast.tree.Node;

import antlr.ASTFactory;

/**
 * User: Joshua Davis<br>
 * Date: Sep 23, 2005<br>
 * Time: 12:30:01 PM<br>
 */
public class HqlASTFactory extends ASTFactory {

	/**
	 * Returns the class for a given token type (a.k.a. AST node type).
	 *
	 * @param tokenType The token type.
	 * @return Class - The AST node class to instantiate.
	 */
	public Class getASTNodeType(int tokenType) {
		return Node.class;
	}
}
