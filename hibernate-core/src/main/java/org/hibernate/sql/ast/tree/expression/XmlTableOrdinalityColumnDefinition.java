/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

/**
 * @since 7.0
 */
public record XmlTableOrdinalityColumnDefinition(
		String name
) implements XmlTableColumnDefinition {
}
