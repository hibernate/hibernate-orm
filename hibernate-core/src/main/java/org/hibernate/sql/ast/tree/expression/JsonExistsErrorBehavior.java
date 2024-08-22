/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @since 7.0
 */
public enum JsonExistsErrorBehavior implements SqlAstNode {
	TRUE,
	FALSE,
	ERROR;

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonExistsErrorBehavior doesn't support walking");
	}

}
