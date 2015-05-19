/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;


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
