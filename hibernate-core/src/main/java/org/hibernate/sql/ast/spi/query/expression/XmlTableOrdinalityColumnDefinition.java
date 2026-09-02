/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query.expression;

/**
 * @since 7.0
 */
public record XmlTableOrdinalityColumnDefinition(
		String name
) implements XmlTableColumnDefinition {
}
