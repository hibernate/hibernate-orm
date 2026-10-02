package org.hibernate.sql.ast.spi.query.expression;

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
