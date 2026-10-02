package org.hibernate.sql.ast.spi.query.expression;

/**
 * @since 7.0
 */
public record JsonTableOrdinalityColumnDefinition(
		String name
) implements JsonTableColumnDefinition {
}
