package org.hibernate.sql.ast.spi.query.expression;

/**
 * @since 7.0
 */
public record JsonTableNestedColumnDefinition(
		String jsonPath,
		JsonTableColumnsClause columns
) implements JsonTableColumnDefinition {

}
