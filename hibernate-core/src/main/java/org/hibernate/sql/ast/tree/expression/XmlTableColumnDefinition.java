/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @since 7.0
 */
public sealed interface XmlTableColumnDefinition extends SqlAstNode
		permits XmlTableOrdinalityColumnDefinition, XmlTableQueryColumnDefinition, XmlTableValueColumnDefinition {

	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("XmlTableColumnDefinition doesn't support walking");
	}

}
