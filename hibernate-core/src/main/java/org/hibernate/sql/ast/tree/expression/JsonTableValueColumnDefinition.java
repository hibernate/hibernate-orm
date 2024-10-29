/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
