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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;

/**
 * Adapter that wraps {@link RevengStrategy} so the caller works with
 * metadata objects ({@link TableMetadata}, {@link RawForeignKeyInfo}).
 * For strategy methods that require {@code org.hibernate.mapping.*} types,
 * the adapter constructs the mapping objects internally, calls the
 * delegate strategy, and returns the result.
 *
 * @author Koen Aers
 */
public class RevengStrategyAdapter {

	private final RevengStrategy delegate;

	public RevengStrategyAdapter(RevengStrategy delegate) {
		this.delegate = delegate;
	}

	// ---- Adapted methods (construct mapping objects internally) ----

	/**
	 * Checks whether the given table is a many-to-many join table.
	 * Builds a mapping {@link Table} with columns, {@link PrimaryKey},
	 * and {@link ForeignKey} collection from the metadata and outgoing FKs.
	 */
	public boolean isManyToManyTable(TableMetadata tableMetadata, List<RawForeignKeyInfo> outgoingFks) {
		Table table = buildMappingTable(tableMetadata, outgoingFks);
		return delegate.isManyToManyTable(table);
	}

	/**
	 * Checks whether the given foreign key represents a one-to-one relationship.
	 * Builds a mapping {@link ForeignKey} with column list and a reference
	 * to a {@link Table} that has a {@link PrimaryKey}.
	 */
	public boolean isOneToOne(RawForeignKeyInfo fkInfo, TableMetadata fkTableMetadata) {
		Table fkTable = buildMappingTableWithPrimaryKey(fkTableMetadata);
		ForeignKey foreignKey = buildMappingForeignKey(fkInfo, fkTable);
		return delegate.isOneToOne(foreignKey);
	}

	/**
	 * Checks whether the given FK should be excluded as a collection (OneToMany).
	 */
	public boolean excludeForeignKeyAsCollection(RawForeignKeyInfo fkInfo) {
		return delegate.excludeForeignKeyAsCollection(
			fkInfo.fkName(),
			createFkTableIdentifier(fkInfo),
			buildColumnList(fkInfo.fkColumnName()),
			createReferencedTableIdentifier(fkInfo),
			buildColumnList(fkInfo.pkColumnName()));
	}

	/**
	 * Checks whether the given FK should be excluded as a many-to-one.
	 */
	public boolean excludeForeignKeyAsManytoOne(RawForeignKeyInfo fkInfo) {
		return delegate.excludeForeignKeyAsManytoOne(
			fkInfo.fkName(),
			createFkTableIdentifier(fkInfo),
			buildColumnList(fkInfo.fkColumnName()),
			createReferencedTableIdentifier(fkInfo),
			buildColumnList(fkInfo.pkColumnName()));
	}

	/**
	 * Gets the entity property name for a many-to-one relationship.
	 */
	public String foreignKeyToEntityName(RawForeignKeyInfo fkInfo, boolean uniqueReference) {
		return delegate.foreignKeyToEntityName(
			fkInfo.fkName(),
			createFkTableIdentifier(fkInfo),
			buildColumnList(fkInfo.fkColumnName()),
			createReferencedTableIdentifier(fkInfo),
			buildColumnList(fkInfo.pkColumnName()),
			uniqueReference);
	}

	/**
	 * Gets the collection property name for a one-to-many relationship.
	 */
	public String foreignKeyToCollectionName(RawForeignKeyInfo fkInfo, boolean uniqueReference) {
		return delegate.foreignKeyToCollectionName(
			fkInfo.fkName(),
			createFkTableIdentifier(fkInfo),
			buildColumnList(fkInfo.fkColumnName()),
			createReferencedTableIdentifier(fkInfo),
			buildColumnList(fkInfo.pkColumnName()),
			uniqueReference);
	}

	/**
	 * Gets the inverse entity property name for a one-to-one relationship.
	 */
	public String foreignKeyToInverseEntityName(RawForeignKeyInfo fkInfo, boolean uniqueReference) {
		return delegate.foreignKeyToInverseEntityName(
			fkInfo.fkName(),
			createFkTableIdentifier(fkInfo),
			buildColumnList(fkInfo.fkColumnName()),
			createReferencedTableIdentifier(fkInfo),
			buildColumnList(fkInfo.pkColumnName()),
			uniqueReference);
	}

	// ---- Internal helpers ----

	private Table buildMappingTable(TableMetadata metadata, List<RawForeignKeyInfo> outgoingFks) {
		Table table = createBaseTable(metadata);
		addColumns(table, metadata);
		addPrimaryKey(table, metadata);
		addForeignKeys(table, outgoingFks);
		return table;
	}

	private Table buildMappingTableWithPrimaryKey(TableMetadata metadata) {
		Table table = createBaseTable(metadata);
		addColumns(table, metadata);
		addPrimaryKey(table, metadata);
		return table;
	}

	private Table createBaseTable(TableMetadata metadata) {
		Table table = new Table("Hibernate Tools");
		table.setAbstract(false);
		table.setName(metadata.getTableName());
		table.setSchema(metadata.getSchema());
		table.setCatalog(metadata.getCatalog());
		return table;
	}

	private void addColumns(Table table, TableMetadata metadata) {
		for (ColumnMetadata colMeta : metadata.getColumns()) {
			Column column = new Column();
			column.setName(colMeta.getColumnName());
			column.setNullable(colMeta.isNullable());
			column.setLength(colMeta.getLength());
			column.setPrecision(colMeta.getPrecision());
			column.setScale(colMeta.getScale());
			table.addColumn(column);
		}
	}

	private void addPrimaryKey(Table table, TableMetadata metadata) {
		List<ColumnMetadata> pkColumns = new ArrayList<>();
		for (ColumnMetadata colMeta : metadata.getColumns()) {
			if (colMeta.isPrimaryKey()) {
				pkColumns.add(colMeta);
			}
		}
		if (!pkColumns.isEmpty()) {
			PrimaryKey pk = new PrimaryKey(table);
			for (ColumnMetadata pkCol : pkColumns) {
				Column column = table.getColumn(new Column(pkCol.getColumnName()));
				if (column != null) {
					pk.addColumn(column);
				}
			}
			table.setPrimaryKey(pk);
		}
	}

	private void addForeignKeys(Table table, List<RawForeignKeyInfo> outgoingFks) {
		if (outgoingFks == null) {
			return;
		}
		for (RawForeignKeyInfo fkInfo : outgoingFks) {
			List<Column> fkColumns = new ArrayList<>();
			Column fkColumn = table.getColumn(new Column(fkInfo.fkColumnName()));
			if (fkColumn != null) {
				fkColumns.add(fkColumn);
			} else {
				fkColumns.add(new Column(fkInfo.fkColumnName()));
			}

			Table referencedTable = new Table("Hibernate Tools");
			referencedTable.setName(fkInfo.referencedTableName());
			referencedTable.setSchema(fkInfo.referencedSchema());
			referencedTable.setCatalog(fkInfo.referencedCatalog());

			List<Column> refColumns = new ArrayList<>();
			refColumns.add(new Column(fkInfo.pkColumnName()));

			ForeignKey fk = table.createForeignKey(
				fkInfo.fkName(),
				fkColumns,
				fkInfo.referencedTableName(),
				null,
				null,
				refColumns);
			fk.setReferencedTable(referencedTable);
		}
	}

	private ForeignKey buildMappingForeignKey(RawForeignKeyInfo fkInfo, Table fkTable) {
		List<Column> fkColumns = new ArrayList<>();
		Column fkColumn = fkTable.getColumn(new Column(fkInfo.fkColumnName()));
		if (fkColumn != null) {
			fkColumns.add(fkColumn);
		} else {
			fkColumns.add(new Column(fkInfo.fkColumnName()));
		}

		Table referencedTable = new Table("Hibernate Tools");
		referencedTable.setName(fkInfo.referencedTableName());
		referencedTable.setSchema(fkInfo.referencedSchema());
		referencedTable.setCatalog(fkInfo.referencedCatalog());

		List<Column> refColumns = new ArrayList<>();
		refColumns.add(new Column(fkInfo.pkColumnName()));

		ForeignKey fk = fkTable.createForeignKey(
			fkInfo.fkName(),
			fkColumns,
			fkInfo.referencedTableName(),
			null,
			null,
			refColumns);
		fk.setReferencedTable(referencedTable);

		return fk;
	}

	private List<Column> buildColumnList(String columnName) {
		List<Column> columns = new ArrayList<>();
		columns.add(new Column(columnName));
		return columns;
	}

	private TableIdentifier createFkTableIdentifier(RawForeignKeyInfo fkInfo) {
		return TableIdentifier.create(
			fkInfo.fkTableCatalog(),
			fkInfo.fkTableSchema(),
			fkInfo.fkTableName());
	}

	private TableIdentifier createReferencedTableIdentifier(RawForeignKeyInfo fkInfo) {
		return TableIdentifier.create(
			fkInfo.referencedCatalog(),
			fkInfo.referencedSchema(),
			fkInfo.referencedTableName());
	}
}
