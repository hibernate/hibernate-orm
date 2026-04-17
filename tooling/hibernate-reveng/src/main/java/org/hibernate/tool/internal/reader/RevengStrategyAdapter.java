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
package org.hibernate.tool.internal.reader;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;

/**
 * Adapter that wraps {@link RevengStrategy} so the caller works with
 * metadata objects ({@link TableDescriptor}, {@link RawForeignKeyInfo}).
 * For strategy methods that require {@code org.hibernate.mapping.*} types,
 * the adapter constructs the mapping objects internally, calls the
 * delegate strategy, and returns the result.
 *
 * @author Koen Aers
 */
public class RevengStrategyAdapter {

	private final RevengStrategy delegate;

	public static RevengStrategyAdapter create(RevengStrategy delegate) {
		return new RevengStrategyAdapter(delegate);
	}

	private RevengStrategyAdapter(RevengStrategy delegate) {
		this.delegate = delegate;
	}

	// ---- Adapted methods (construct mapping objects internally) ----

	/**
	 * Checks whether the given table is a many-to-many join table.
	 * Builds a mapping {@link Table} with columns, {@link PrimaryKey},
	 * and {@link ForeignKey} collection from the metadata and outgoing FKs.
	 */
	public boolean isManyToManyTable(TableDescriptor tableMetadata, List<RawForeignKeyInfo> outgoingFks) {
		Table table = buildMappingTable(tableMetadata, outgoingFks);
		return delegate.isManyToManyTable(table);
	}

	/**
	 * Checks whether the given foreign key represents a one-to-one relationship.
	 * Builds a mapping {@link ForeignKey} with column list and a reference
	 * to a {@link Table} that has a {@link PrimaryKey}.
	 */
	public boolean isOneToOne(RawForeignKeyInfo fkInfo, TableDescriptor fkTableDescriptor) {
		Table fkTable = buildMappingTable(fkTableDescriptor, null);
		ForeignKey foreignKey = buildMappingForeignKey(fkInfo, fkTable);
		return delegate.isOneToOne(foreignKey);
	}

	/**
	 * Checks whether the given composite foreign key represents a one-to-one
	 * relationship.  All FK columns are included so that the column-count
	 * comparison against the PK succeeds for composite keys.
	 */
	public boolean isOneToOne(java.util.List<RawForeignKeyInfo> fkColumns,
			TableDescriptor fkTableDescriptor) {
		if (fkColumns.size() == 1) {
			return isOneToOne(fkColumns.get(0), fkTableDescriptor);
		}
		Table fkTable = buildMappingTable(fkTableDescriptor, null);
		ForeignKey foreignKey = buildMappingCompositeForeignKey(fkColumns, fkTable);
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
		// Swap from/referenced: the legacy convention passes the referenced table
		// (the entity receiving the inverse field) as "from" and the FK table
		// (the entity the field points to) as "referenced"
		return delegate.foreignKeyToInverseEntityName(
			fkInfo.fkName(),
			createReferencedTableIdentifier(fkInfo),
			buildColumnList(fkInfo.pkColumnName()),
			createFkTableIdentifier(fkInfo),
			buildColumnList(fkInfo.fkColumnName()),
			uniqueReference);
	}

	/**
	 * Gets the property name for a many-to-many relationship.
	 * Builds mapping {@link ForeignKey} objects for both sides of the
	 * join table and calls {@code delegate.foreignKeyToManyToManyName()}.
	 */
	public String foreignKeyToManyToManyName(
			RawForeignKeyInfo fromFk,
			TableDescriptor joinTableDescriptor,
			List<RawForeignKeyInfo> joinTableFks,
			RawForeignKeyInfo toFk,
			boolean uniqueReference) {
		Table joinTable = buildMappingTable(joinTableDescriptor, joinTableFks);
		ForeignKey fromKey = buildMappingForeignKey(fromFk, joinTable);
		ForeignKey toKey = buildMappingForeignKey(toFk, joinTable);
		TableIdentifier middleTableId = TableIdentifier.create(
			joinTableDescriptor.getCatalog(),
			joinTableDescriptor.getSchema(),
			joinTableDescriptor.getTableName());
		return delegate.foreignKeyToManyToManyName(fromKey, middleTableId, toKey, uniqueReference);
	}

	/**
	 * Checks whether this FK's side of the join table is the inverse side
	 * of a collection (i.e. should use {@code mappedBy}).
	 */
	public boolean isForeignKeyCollectionInverse(
			RawForeignKeyInfo fkInfo,
			TableDescriptor joinTableDescriptor,
			List<RawForeignKeyInfo> joinTableFks) {
		Table joinTable = buildMappingTable(joinTableDescriptor, joinTableFks);
		return delegate.isForeignKeyCollectionInverse(
			fkInfo.fkName(),
			joinTable,
			buildColumnList(fkInfo.fkColumnName()),
			createReferencedTable(fkInfo),
			buildColumnList(fkInfo.pkColumnName()));
	}

	// ---- Internal helpers ----

	private Table buildMappingTable(TableDescriptor metadata, List<RawForeignKeyInfo> outgoingFks) {
		Table table = new Table("Hibernate Tools");
		table.setAbstract(false);
		table.setName(metadata.getTableName());
		table.setSchema(metadata.getSchema());
		table.setCatalog(metadata.getCatalog());
		addMappingColumns(table, metadata);
		setMappingPrimaryKey(table, metadata);
		addMappingForeignKeys(table, outgoingFks);
		return table;
	}

	private void setMappingPrimaryKey(Table table, TableDescriptor metadata) {
		PrimaryKey pk = buildMappingPrimaryKey(table, metadata);
		if (pk != null) {
			table.setPrimaryKey(pk);
		}
	}

	private void addMappingForeignKeys(Table table, List<RawForeignKeyInfo> outgoingFks) {
		if (outgoingFks == null) {
			return;
		}
		// Group FK entries by FK name to handle composite FKs correctly.
		// Multiple RawForeignKeyInfo entries with the same FK name represent
		// multiple columns of a single logical foreign key.
		java.util.Map<String, List<RawForeignKeyInfo>> fksByName = new java.util.LinkedHashMap<>();
		for (RawForeignKeyInfo fkInfo : outgoingFks) {
			fksByName.computeIfAbsent(fkInfo.fkName(), k -> new ArrayList<>()).add(fkInfo);
		}
		for (java.util.Map.Entry<String, List<RawForeignKeyInfo>> entry : fksByName.entrySet()) {
			String fkName = entry.getKey();
			List<RawForeignKeyInfo> fkEntries = entry.getValue();
			RawForeignKeyInfo first = fkEntries.get(0);

			List<Column> fkColumns = new ArrayList<>();
			List<Column> refColumns = new ArrayList<>();
			for (RawForeignKeyInfo fkInfo : fkEntries) {
				Column fkColumn = table.getColumn(new Column(fkInfo.fkColumnName()));
				fkColumns.add(fkColumn != null ? fkColumn : new Column(fkInfo.fkColumnName()));
				refColumns.add(new Column(fkInfo.pkColumnName()));
			}

			ForeignKey fk = table.createForeignKey(
				fkName,
				fkColumns,
				first.referencedTableName(),
				null,
				null,
				refColumns);
			fk.setReferencedTable(createReferencedTable(first));
		}
	}

	private void addMappingColumns(Table table, TableDescriptor metadata) {
		for (ColumnDescriptor colMeta : metadata.getColumns()) {
			table.addColumn(buildMappingColumn(colMeta));
		}
	}

	private Column buildMappingColumn(ColumnDescriptor colMeta) {
		Column column = new Column();
		column.setName(colMeta.getColumnName());
		column.setNullable(colMeta.isNullable());
		column.setLength(colMeta.getLength());
		column.setPrecision(colMeta.getPrecision());
		column.setScale(colMeta.getScale());
		return column;
	}

	private PrimaryKey buildMappingPrimaryKey(Table table, TableDescriptor metadata) {
		List<ColumnDescriptor> pkColumns = new ArrayList<>();
		for (ColumnDescriptor colMeta : metadata.getColumns()) {
			if (colMeta.isPrimaryKey()) {
				pkColumns.add(colMeta);
			}
		}
		if (pkColumns.isEmpty()) {
			return null;
		}
		PrimaryKey pk = new PrimaryKey(table);
		for (ColumnDescriptor pkCol : pkColumns) {
			Column column = table.getColumn(new Column(pkCol.getColumnName()));
			if (column != null) {
				pk.addColumn(column);
			}
		}
		return pk;
	}

	private ForeignKey buildMappingForeignKey(RawForeignKeyInfo fkInfo, Table table) {
		List<Column> fkColumns = new ArrayList<>();
		Column fkColumn = table.getColumn(new Column(fkInfo.fkColumnName()));
		if (fkColumn != null) {
			fkColumns.add(fkColumn);
		} else {
			fkColumns.add(new Column(fkInfo.fkColumnName()));
		}
		List<Column> refColumns = new ArrayList<>();
		refColumns.add(new Column(fkInfo.pkColumnName()));
		ForeignKey fk = table.createForeignKey(
			fkInfo.fkName(),
			fkColumns,
			fkInfo.referencedTableName(),
			null,
			null,
			refColumns);
		fk.setReferencedTable(createReferencedTable(fkInfo));
		return fk;
	}

	private ForeignKey buildMappingCompositeForeignKey(
			java.util.List<RawForeignKeyInfo> fkInfos, Table table) {
		RawForeignKeyInfo first = fkInfos.get(0);
		List<Column> fkColumns = new ArrayList<>();
		List<Column> refColumns = new ArrayList<>();
		for (RawForeignKeyInfo fkInfo : fkInfos) {
			Column fkColumn = table.getColumn(new Column(fkInfo.fkColumnName()));
			fkColumns.add(fkColumn != null ? fkColumn : new Column(fkInfo.fkColumnName()));
			refColumns.add(new Column(fkInfo.pkColumnName()));
		}
		ForeignKey fk = table.createForeignKey(
			first.fkName(),
			fkColumns,
			first.referencedTableName(),
			null,
			null,
			refColumns);
		fk.setReferencedTable(createReferencedTable(first));
		return fk;
	}

	private Table createReferencedTable(RawForeignKeyInfo fkInfo) {
		Table referencedTable = new Table("Hibernate Tools");
		referencedTable.setName(fkInfo.referencedTableName());
		referencedTable.setSchema(fkInfo.referencedSchema());
		referencedTable.setCatalog(fkInfo.referencedCatalog());
		return referencedTable;
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
