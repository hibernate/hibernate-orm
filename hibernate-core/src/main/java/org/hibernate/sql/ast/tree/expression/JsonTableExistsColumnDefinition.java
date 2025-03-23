/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.type.BasicType;

/**
 * @since 7.0
 */
public record JsonTableExistsColumnDefinition(
		String name,
		BasicType<Boolean> type,
		@Nullable String jsonPath,
		@Nullable JsonExistsErrorBehavior errorBehavior
) implements JsonTableColumnDefinition {
}
