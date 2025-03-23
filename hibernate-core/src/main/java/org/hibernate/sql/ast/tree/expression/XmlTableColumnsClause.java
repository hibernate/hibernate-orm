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
public class XmlTableColumnsClause implements SqlAstNode {

	private final List<XmlTableColumnDefinition> columnDefinitions;

	public XmlTableColumnsClause(List<XmlTableColumnDefinition> columnDefinitions) {
		this.columnDefinitions = columnDefinitions;
	}

	public List<XmlTableColumnDefinition> getColumnDefinitions() {
		return columnDefinitions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("XmlTableColumnsClause doesn't support walking");
	}

}
