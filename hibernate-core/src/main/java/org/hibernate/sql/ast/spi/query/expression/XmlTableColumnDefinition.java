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
public sealed interface XmlTableColumnDefinition extends SqlAstNode
		permits XmlTableOrdinalityColumnDefinition, XmlTableQueryColumnDefinition, XmlTableValueColumnDefinition {

	@Override
	default void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException("XmlTableColumnDefinition doesn't support walking");
	}

}
