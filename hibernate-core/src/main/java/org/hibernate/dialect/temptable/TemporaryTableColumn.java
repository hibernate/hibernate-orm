/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.temptable;

import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.JdbcMapping;

/**
 * A column in a IdTable.  As these columns mirror the entity id columns, we know a few things about it inherently,
 * such as being non-nullable
 *
 * @author Steve Ebersole
 */
public class TemporaryTableColumn {
	private final TemporaryTable containingTable;
	private final String columnName;
	private final JdbcMapping jdbcMapping;
	private final String sqlTypeName;
	private final Size size;
	private final boolean nullable;
	private final boolean primaryKey;

	public TemporaryTableColumn(
			TemporaryTable containingTable,
			String columnName,
			JdbcMapping jdbcMapping,
			String sqlTypeName,
			Size size,
			boolean nullable) {
		this( containingTable, columnName, jdbcMapping, sqlTypeName, size, nullable, false );
	}

	public TemporaryTableColumn(
			TemporaryTable containingTable,
			String columnName,
			JdbcMapping jdbcMapping,
			String sqlTypeName,
			Size size,
			boolean nullable,
			boolean primaryKey) {
		this.containingTable = containingTable;
		this.columnName = columnName;
		this.jdbcMapping = jdbcMapping;
		this.sqlTypeName = sqlTypeName;
		this.size = size;
		this.nullable = nullable;
		this.primaryKey = primaryKey;
	}

	public TemporaryTable getContainingTable() {
		return containingTable;
	}

	public String getColumnName() {
		return columnName;
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	public String getDefaultValue() {
		return null;
	}

	public String getSqlTypeDefinition() {
		return sqlTypeName;
	}

	public Size getSize() {
		return size;
	}

	public boolean isNullable() {
		return nullable;
	}

	public boolean isPrimaryKey() {
		return primaryKey;
	}
}
