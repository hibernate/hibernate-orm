/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import jakarta.annotation.Nullable;

/**
 * @since 7.0
 */
public record XmlTableValueColumnDefinition(
		String name,
		CastTarget type,
		@Nullable String xpath,
		@Nullable Expression defaultExpression
) implements XmlTableColumnDefinition {

}
