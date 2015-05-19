/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.type.Type;

import antlr.SemanticException;

/**
 * Contract for nodes representing operators (logic or arithmetic).
 *
 * @author Steve Ebersole
 */
public interface OperatorNode {
	/**
	 * Called by the tree walker during hql-sql semantic analysis
	 * after the operator sub-tree is completely built.
	 */
	public abstract void initialize() throws SemanticException;

	/**
	 * Retrieves the data type for the overall operator expression.
	 *
	 * @return The expression's data type.
	 */
	public Type getDataType();
}
