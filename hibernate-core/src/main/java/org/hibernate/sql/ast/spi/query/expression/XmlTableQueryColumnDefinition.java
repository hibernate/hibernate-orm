package org.hibernate.sql.ast.spi.query.expression;

import jakarta.annotation.Nullable;
import org.hibernate.type.BasicType;

/**
 * @since 7.0
 */
public record XmlTableQueryColumnDefinition(
		String name,
		BasicType<String> type,
		@Nullable String xpath,
		@Nullable Expression defaultExpression
) implements XmlTableColumnDefinition {

}
