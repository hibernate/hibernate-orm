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
package org.hibernate.tool.internal.reveng.models.exporter.doc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.IndexMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Represents a database table for the table documentation templates.
 * <p>
 * When the original {@link TableMetadata} is available (i.e., the entity
 * was built from database metadata via {@code DynamicEntityBuilder}),
 * table info is extracted directly from it — preserving vendor-specific
 * SQL types, exact column definitions, and constraint names.
 * <p>
 * When no {@code TableMetadata} is available (e.g., hand-written entity
 * classes), table info is derived from JPA annotations on the
 * {@link ClassDetails} — producing vendor-independent documentation.
 * <p>
 * Templates access properties like {@code table.name},
 * {@code table.columns}, {@code table.hasPrimaryKey()},
 * {@code table.foreignKeys}, {@code table.uniqueKeys},
 * {@code table.indexes}.
 *
 * @author Koen Aers
 */
public class TableDocInfo {

	private final String name;
	private final String schema;
	private final String catalog;
	private final String comment;
	private final List<TableColumnDocInfo> columns;
	private final PrimaryKeyDocInfo primaryKey;
	private final Map<String, ForeignKeyDocInfo> foreignKeys;
	private final Map<String, UniqueKeyDocInfo> uniqueKeys;
	private final Map<String, IndexDocInfo> indexes;

	private TableDocInfo(String name, String schema, String catalog,
						  String comment,
						  List<TableColumnDocInfo> columns,
						  PrimaryKeyDocInfo primaryKey,
						  Map<String, ForeignKeyDocInfo> foreignKeys,
						  Map<String, UniqueKeyDocInfo> uniqueKeys,
						  Map<String, IndexDocInfo> indexes) {
		this.name = name;
		this.schema = schema;
		this.catalog = catalog;
		this.comment = comment;
		this.columns = columns;
		this.primaryKey = primaryKey;
		this.foreignKeys = foreignKeys;
		this.uniqueKeys = uniqueKeys;
		this.indexes = indexes;
	}

	public String getName() {
		return name;
	}

	public String getSchema() {
		return schema;
	}

	public String getCatalog() {
		return catalog;
	}

	public String getComment() {
		return comment != null ? comment : "";
	}

	public List<TableColumnDocInfo> getColumns() {
		return columns;
	}

	public boolean hasPrimaryKey() {
		return primaryKey != null;
	}

	public PrimaryKeyDocInfo getPrimaryKey() {
		return primaryKey;
	}

	public Map<String, ForeignKeyDocInfo> getForeignKeys() {
		return foreignKeys;
	}

	public Map<String, UniqueKeyDocInfo> getUniqueKeys() {
		return uniqueKeys;
	}

	public Map<String, IndexDocInfo> getIndexes() {
		return indexes;
	}

	/**
	 * Returns an iterator over the primary key columns.
	 */
	public Iterator<TableColumnDocInfo> getPrimaryKeyColumnIterator() {
		if (primaryKey != null) {
			return primaryKey.getColumns().iterator();
		}
		return List.<TableColumnDocInfo>of().iterator();
	}

	/**
	 * Adds a foreign key to this table. Called during the second pass
	 * when all tables have been built and can be cross-referenced.
	 */
	void addForeignKey(String fkName, ForeignKeyDocInfo fk) {
		foreignKeys.put(fkName, fk);
	}

	// ---- Factory methods ----

	/**
	 * Builds a {@link TableDocInfo} from the original {@link TableMetadata},
	 * preserving vendor-specific SQL types and database-level details.
	 */
	static TableDocInfo buildFromTableMetadata(TableMetadata tableMeta) {
		String tableName = tableMeta.getTableName();

		// Build columns and track primary key columns
		List<TableColumnDocInfo> allColumns = new ArrayList<>();
		List<TableColumnDocInfo> pkColumns = new ArrayList<>();
		Map<String, TableColumnDocInfo> columnsByName = new LinkedHashMap<>();

		for (ColumnMetadata col : tableMeta.getColumns()) {
			TableColumnDocInfo colInfo = new TableColumnDocInfo(
					col.getColumnName(),
					col.getJavaType().getName(),
					col.isNullable(),
					col.isUnique(),
					col.getComment() != null ? col.getComment() : "",
					col.getLength(),
					col.getPrecision(),
					col.getScale());
			allColumns.add(colInfo);
			columnsByName.put(col.getColumnName(), colInfo);
			if (col.isPrimaryKey()) {
				pkColumns.add(colInfo);
			}
		}

		// Add foreign key columns
		for (ForeignKeyMetadata fk : tableMeta.getForeignKeys()) {
			String fkColName = fk.getForeignKeyColumnName();
			if (!columnsByName.containsKey(fkColName)) {
				TableColumnDocInfo fkCol = new TableColumnDocInfo(
						fkColName,
						fk.getTargetEntityPackage() + "."
								+ fk.getTargetEntityClassName(),
						fk.isOptional(), false, "",
						255, 0, 0);
				allColumns.add(fkCol);
				columnsByName.put(fkColName, fkCol);
			}
		}

		// Primary key
		PrimaryKeyDocInfo pk = pkColumns.isEmpty()
				? null
				: new PrimaryKeyDocInfo("PK_" + tableName, pkColumns);

		// Indexes
		Map<String, IndexDocInfo> indexMap = new LinkedHashMap<>();
		for (IndexMetadata idx : tableMeta.getIndexes()) {
			List<TableColumnDocInfo> idxCols = resolveColumns(
					idx.getColumnNames(), columnsByName);
			indexMap.put(idx.getIndexName(),
					new IndexDocInfo(idx.getIndexName(), idxCols));
		}

		return new TableDocInfo(tableName, tableMeta.getSchema(),
				tableMeta.getCatalog(),
				tableMeta.getComment() != null ? tableMeta.getComment() : "",
				allColumns, pk, new LinkedHashMap<>(),
				new LinkedHashMap<>(), indexMap);
	}

	/**
	 * Builds a {@link TableDocInfo} from JPA annotations on a
	 * {@link ClassDetails} entity. Used as a fallback when no
	 * {@code TableMetadata} is available, producing vendor-independent
	 * documentation.
	 */
	static TableDocInfo buildFromClassDetails(ClassDetails classDetails) {
		Table tableAnn = classDetails.getDirectAnnotationUsage(Table.class);
		String tableName = tableAnn != null && !tableAnn.name().isEmpty()
				? tableAnn.name()
				: classDetails.getName();
		String schema = tableAnn != null && !tableAnn.schema().isEmpty()
				? tableAnn.schema()
				: null;
		String catalog = tableAnn != null && !tableAnn.catalog().isEmpty()
				? tableAnn.catalog()
				: null;

		// Build columns
		List<TableColumnDocInfo> allColumns = new ArrayList<>();
		List<TableColumnDocInfo> pkColumns = new ArrayList<>();
		Map<String, TableColumnDocInfo> columnsByName = new LinkedHashMap<>();

		for (FieldDetails field : classDetails.getFields()) {
			if (field.hasDirectAnnotationUsage(ManyToOne.class)
					|| field.hasDirectAnnotationUsage(OneToOne.class)) {
				JoinColumn jc =
						field.getDirectAnnotationUsage(JoinColumn.class);
				if (jc != null && !jc.name().isEmpty()) {
					TableColumnDocInfo col = new TableColumnDocInfo(
							jc.name(),
							field.getType().determineRawClass().getClassName(),
							jc.nullable(), jc.unique(), "",
							255, 0, 0);
					allColumns.add(col);
					columnsByName.put(col.getName(), col);
				}
				continue;
			}

			Column colAnn =
					field.getDirectAnnotationUsage(Column.class);
			String colName = colAnn != null && !colAnn.name().isEmpty()
					? colAnn.name()
					: field.getName();
			boolean nullable = colAnn == null || colAnn.nullable();
			boolean unique = colAnn != null && colAnn.unique();
			int length = colAnn != null ? colAnn.length() : 255;
			int precision = colAnn != null ? colAnn.precision() : 0;
			int scale = colAnn != null ? colAnn.scale() : 0;
			String javaType =
					field.getType().determineRawClass().getClassName();

			TableColumnDocInfo col = new TableColumnDocInfo(
					colName, javaType, nullable, unique, "",
					length, precision, scale);
			allColumns.add(col);
			columnsByName.put(col.getName(), col);

			if (field.hasDirectAnnotationUsage(Id.class)) {
				pkColumns.add(col);
			}
		}

		// Primary key
		PrimaryKeyDocInfo pk = pkColumns.isEmpty()
				? null
				: new PrimaryKeyDocInfo("PK_" + tableName, pkColumns);

		// Unique constraints from @Table
		Map<String, UniqueKeyDocInfo> uniqueKeyMap = new LinkedHashMap<>();
		if (tableAnn != null) {
			int ucIdx = 0;
			for (UniqueConstraint uc : tableAnn.uniqueConstraints()) {
				String ucName = uc.name() != null && !uc.name().isEmpty()
						? uc.name()
						: "UK_" + tableName + "_" + ucIdx;
				List<TableColumnDocInfo> ucCols = resolveColumns(
						List.of(uc.columnNames()), columnsByName);
				uniqueKeyMap.put(ucName,
						new UniqueKeyDocInfo(ucName, ucCols));
				ucIdx++;
			}
		}

		// Indexes from @Table
		Map<String, IndexDocInfo> indexMap = new LinkedHashMap<>();
		if (tableAnn != null) {
			int idxNum = 0;
			for (Index idx : tableAnn.indexes()) {
				String idxName = idx.name() != null && !idx.name().isEmpty()
						? idx.name()
						: "IDX_" + tableName + "_" + idxNum;
				List<String> colNames = new ArrayList<>();
				if (idx.columnList() != null && !idx.columnList().isEmpty()) {
					for (String colRef : idx.columnList().split(",")) {
						colNames.add(colRef.trim().split("\\s+")[0]);
					}
				}
				List<TableColumnDocInfo> idxCols = resolveColumns(
						colNames, columnsByName);
				indexMap.put(idxName, new IndexDocInfo(idxName, idxCols));
				idxNum++;
			}
		}

		return new TableDocInfo(tableName, schema, catalog, "",
				allColumns, pk, new LinkedHashMap<>(),
				uniqueKeyMap, indexMap);
	}

	private static List<TableColumnDocInfo> resolveColumns(
			List<String> colNames,
			Map<String, TableColumnDocInfo> columnsByName) {
		List<TableColumnDocInfo> result = new ArrayList<>();
		for (String colName : colNames) {
			TableColumnDocInfo col = columnsByName.get(colName);
			if (col != null) {
				result.add(col);
			}
			else {
				result.add(new TableColumnDocInfo(
						colName, "unknown", true, false, "",
						255, 0, 0));
			}
		}
		return result;
	}
}
