package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nullable;
import org.hibernate.type.BasicType;

/**
 * @since 7.0
 */
public record JsonTableQueryColumnDefinition(
		String name,
		BasicType<String> type,
		@Nullable String jsonPath,
		@Nullable JsonQueryWrapMode wrapMode,
		@Nullable JsonQueryErrorBehavior errorBehavior,
		@Nullable JsonQueryEmptyBehavior emptyBehavior
) implements JsonTableColumnDefinition {

}
