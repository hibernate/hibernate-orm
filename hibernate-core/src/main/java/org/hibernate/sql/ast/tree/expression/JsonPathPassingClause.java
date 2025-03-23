/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
