package org.hibernate.mapping;

import org.hibernate.relational.naming.spi.PhysicalName;

/**
 * Represents a fully qualified column name for uniqueness checks.
 */
public record QualifiedColumnName(
		ColumnContainer columnContainer,
		PhysicalName columnName
) {}
