/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import java.util.List;

/**
 * @since 7.0
 */
public class JsonTableColumnsClause implements SqlAstNode {

	private final List<JsonTableColumnDefinition> columnDefinitions;

	public JsonTableColumnsClause(List<JsonTableColumnDefinition> columnDefinitions) {
		this.columnDefinitions = columnDefinitions;
	}

	public List<JsonTableColumnDefinition> getColumnDefinitions() {
		return columnDefinitions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonPathPassingClause doesn't support walking");
	}

}
