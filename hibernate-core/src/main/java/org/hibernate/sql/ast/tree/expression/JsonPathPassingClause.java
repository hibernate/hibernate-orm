/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.Map;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @since 7.0
 */
public class JsonPathPassingClause implements SqlAstNode {

	private final Map<String, Expression> passingExpressions;

	public JsonPathPassingClause(Map<String, Expression> passingExpressions) {
		this.passingExpressions = passingExpressions;
	}

	public Map<String, Expression> getPassingExpressions() {
		return passingExpressions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonPathPassingClause doesn't support walking");
	}

}
