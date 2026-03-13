/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.reader;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.TemporalType;

import org.hibernate.mapping.MetaAttribute;
import org.hibernate.tool.api.reveng.RevengDialect;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;

/**
 * Reads column metadata and primary key information for a table
 * from the database via {@link RevengDialect}. Populates the
 * given {@link TableMetadata} with {@link ColumnMetadata} entries,
 * marking primary key columns, version columns, temporal types,
 * and LOB types.
 *
 * @author Koen Aers
 */
class ColumnReader {

	private final RevengDialect dialect;
	private final RevengStrategy strategy;

	static ColumnReader create(RevengDialect dialect, RevengStrategy strategy) {
		return new ColumnReader(dialect, strategy);
	}

	private ColumnReader(RevengDialect dialect, RevengStrategy strategy) {
		this.dialect = dialect;
		this.strategy = strategy;
	}

	/**
	 * Reads columns and primary keys for the given table and populates
	 * the table metadata.
	 */
	void readColumns(TableMetadata tableMetadata, TableIdentifier tableId,
			String catalog, String schema) {
		Set<String> pkColumns = PrimaryKeyReader
				.create(dialect, strategy)
				.readPrimaryKeys(catalog, schema, tableMetadata.getTableName(), tableId);

		Iterator<Map<String, Object>> columnIterator = dialect.getColumns(
			catalog, schema, tableMetadata.getTableName(), null);
		try {
			while (columnIterator.hasNext()) {
				readColumn(columnIterator.next(), tableMetadata, tableId, pkColumns);
			}
		} finally {
			dialect.close(columnIterator);
		}
	}

	private void readColumn(Map<String, Object> colRow, TableMetadata tableMetadata,
			TableIdentifier tableId, Set<String> pkColumns) {
		RowInfo rowInfo = RowInfo.createFrom(colRow, pkColumns, dialect);

		if (!strategy.excludeColumn(tableId, rowInfo.columnName())) {
			tableMetadata.addColumn(createColumnMetadata(tableId, rowInfo));
		}
	}

	private ColumnMetadata createColumnMetadata(TableIdentifier tableId, RowInfo rowInfo) {
		String hibernateType = determineHibernateType(tableId, rowInfo);
		Class<?> javaClass = HibernateTypeToJavaClass.toJavaClass(hibernateType);
		String fieldName = strategy.columnToPropertyName(tableId, rowInfo.columnName());

		ColumnMetadata columnMetadata = new ColumnMetadata(rowInfo.columnName(), fieldName, javaClass)
			.hibernateTypeName(hibernateType)
			.nullable(rowInfo.nullable());

		if (JdbcToHibernateTypeHelper.typeHasLength(rowInfo.sqlType())) {
			columnMetadata.length(rowInfo.columnSize());
		}
		if (JdbcToHibernateTypeHelper.typeHasPrecision(rowInfo.sqlType())) {
			columnMetadata.precision(rowInfo.columnSize());
		}
		if (JdbcToHibernateTypeHelper.typeHasScale(rowInfo.sqlType())) {
			columnMetadata.scale(rowInfo.decimalDigits());
		}

		if (rowInfo.primaryKey()) {
			columnMetadata.primaryKey(true);
		}

		if (isVersionColumn(tableId, rowInfo.columnName())) {
			columnMetadata.version(true);
		}

		TemporalType temporalType = HibernateTypeToJavaClass.toTemporalType(hibernateType);
		if (temporalType != null) {
			columnMetadata.temporal(temporalType);
		}

		if (HibernateTypeToJavaClass.isLob(hibernateType)) {
			columnMetadata.lob(true);
		}

		if (rowInfo.comment() != null) {
			columnMetadata.comment(rowInfo.comment());
		}

		applyColumnMetaAttributes(columnMetadata, tableId, rowInfo.columnName());

		return columnMetadata;
	}

	private String determineHibernateType(TableIdentifier tableId, RowInfo rowInfo) {
		String hibernateType = strategy.columnToHibernateTypeName(
			tableId, rowInfo.columnName(), rowInfo.sqlType(), rowInfo.columnSize(), rowInfo.decimalDigits(),
			0, rowInfo.nullable(), rowInfo.primaryKey());
		if (hibernateType == null) {
			hibernateType = JdbcToHibernateTypeHelper.getPreferredHibernateType(
				rowInfo.sqlType(), rowInfo.columnSize(), rowInfo.decimalDigits(),
				0, rowInfo.nullable(), rowInfo.primaryKey());
		}
		return hibernateType;
	}

	private void applyColumnMetaAttributes(ColumnMetadata columnMetadata,
			TableIdentifier tableId, String columnName) {
		Map<String, MetaAttribute> metaMap = strategy.columnToMetaAttributes(tableId, columnName);
		if (metaMap != null) {
			for (Map.Entry<String, MetaAttribute> entry : metaMap.entrySet()) {
				MetaAttribute ma = entry.getValue();
				if (ma != null && ma.getValues() != null) {
					for (Object value : ma.getValues()) {
						columnMetadata.addMetaAttribute(entry.getKey(), value.toString());
					}
				}
			}
		}
	}

	private boolean isVersionColumn(TableIdentifier tableId, String columnName) {
		String optimisticLockColumn = strategy.getOptimisticLockColumnName(tableId);
		if (optimisticLockColumn != null) {
			return columnName.equals(optimisticLockColumn);
		}
		return strategy.useColumnForOptimisticLock(tableId, columnName);
	}

	private static String quote(String name, RevengDialect dialect) {
		if (name == null) return null;
		if (dialect.needQuote(name)) {
			if (name.length() > 1 && name.charAt(0) == '`'
					&& name.charAt(name.length() - 1) == '`') {
				return name;
			}
			return "`" + name + "`";
		}
		return name;
	}

	record RowInfo(String columnName, int sqlType, int columnSize, int decimalDigits,
			boolean nullable, boolean primaryKey, String comment) {
		static RowInfo createFrom(Map<String, Object> colRow, Set<String> pkColumns,
				RevengDialect dialect) {
			String columnName = quote((String) colRow.get("COLUMN_NAME"), dialect);
			int sqlType = (Integer) colRow.get("DATA_TYPE");
			int columnSize = colRow.get("COLUMN_SIZE") != null
				? (Integer) colRow.get("COLUMN_SIZE") : 0;
			int decimalDigits = colRow.get("DECIMAL_DIGITS") != null
				? (Integer) colRow.get("DECIMAL_DIGITS") : 0;
			int nullable = colRow.get("NULLABLE") != null
				? (Integer) colRow.get("NULLABLE") : 1;
			boolean isNullable = nullable != java.sql.DatabaseMetaData.columnNoNulls;
			String comment = (String) colRow.get("REMARKS");
			return new RowInfo(columnName, sqlType, columnSize, decimalDigits,
				isNullable, pkColumns.contains(columnName), comment);
		}
	}
}
