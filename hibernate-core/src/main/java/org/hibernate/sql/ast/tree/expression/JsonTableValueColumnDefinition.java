/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @since 7.0
 */
public record JsonTableValueColumnDefinition(
		String name,
		CastTarget type,
		@Nullable String jsonPath,
		@Nullable JsonValueErrorBehavior errorBehavior,
		@Nullable JsonValueEmptyBehavior emptyBehavior
) implements JsonTableColumnDefinition {

}
