/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstNode;

/**
 * @since 7.0
 */
public sealed interface JsonTableColumnDefinition extends SqlAstNode
		permits JsonTableExistsColumnDefinition, JsonTableNestedColumnDefinition, JsonTableOrdinalityColumnDefinition, JsonTableQueryColumnDefinition, JsonTableValueColumnDefinition {

	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("JsonTableColumnDefinition doesn't support walking");
	}

}
