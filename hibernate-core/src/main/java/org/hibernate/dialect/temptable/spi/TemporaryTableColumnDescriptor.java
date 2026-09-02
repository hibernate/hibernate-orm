/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable.spi;

import org.hibernate.SPI;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;

/// Read-only description of a temporary-table column.
///
/// Hibernate owns descriptor construction. A [TemporaryTableExporter] uses
/// these values to assemble DDL and must not retain the descriptor.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI
public interface TemporaryTableColumnDescriptor {
	/// The physical column name rendered in DDL and SQL.
	String getColumnName();

	/// The JDBC mapping used to bind values for this column.
	JdbcMapping getJdbcMapping();

	/// The base SQL type definition selected by the Dialect's type registry.
	String getSqlTypeDefinition();

	/// The column size used to expand type-definition placeholders.
	Size getSize();

	/// Whether DDL may declare this column nullable.
	boolean isNullable();

	/// Whether this column participates in the temporary table's primary key.
	boolean isPrimaryKey();
}
