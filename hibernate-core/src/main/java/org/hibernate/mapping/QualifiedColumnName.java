package org.hibernate.mapping;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Represents a fully qualified column name for uniqueness checks.
 */
public record QualifiedColumnName(
		Identifier catalogName,
		Identifier schemaName,
		Identifier tableName,
		Identifier columnName
) {}
