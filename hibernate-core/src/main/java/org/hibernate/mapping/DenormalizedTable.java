/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.relational.naming.spi.QualifiedPhysicalName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedList;

/**
 * @author Gavin King
 */
public class DenormalizedTable extends PhysicalTable {

	private final PhysicalTable includedTable;
	private List<Column> reorderedColumns;

	public DenormalizedTable(
			String contributor,
			Namespace namespace,
			PhysicalName physicalTableName,
			boolean isAbstract,
			PhysicalTable includedTable) {
		super( contributor, namespace, physicalTableName, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			String contributor,
			QualifiedPhysicalName name,
			boolean isAbstract,
			PhysicalTable includedTable) {
		super( contributor, name, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public void createDenormalizedForeignKeys(MetadataBuildingContext context) {
		if ( includedTable instanceof DenormalizedTable denormalizedTable ) {
			denormalizedTable.createDenormalizedForeignKeys( context );
		}
		for ( var foreignKey : includedTable.getForeignKeyCollection() ) {
			final var referencedClass =
					foreignKey.resolveReferencedClass( context.getMetadataCollector() );
			// the ForeignKeys created in the first pass did not have their referenced table initialized
			if ( foreignKey.getReferencedTable() == null ) {
				foreignKey.setReferencedTable( referencedClass.getTable() );
			}

			createForeignKey(
					null,
					foreignKey.getColumns(),
					foreignKey.getReferencedEntityName(),
					foreignKey.getKeyDefinition(),
					foreignKey.getOptions(),
					foreignKey.getReferencedColumns().isEmpty() ? null : foreignKey.getReferencedColumns()
			);
		}
	}

	@Override
	public Column getColumn(Column column) {
		final var superColumn = super.getColumn( column );
		return superColumn != null ? superColumn : includedTable.getColumn(column);
	}

	public Column getColumn(PhysicalName name) {
		final var superColumn = super.getColumn( name );
		return superColumn != null ? superColumn : includedTable.getColumn(name);
	}

	@Override
	public Collection<Column> getColumns() {
		if ( reorderedColumns != null ) {
			return reorderedColumns;
		}
		return new JoinedList<>( new ArrayList<>( includedTable.getColumns() ), new ArrayList<>( super.getColumns() ) );
	}

	@Override
	public boolean containsColumn(Column column) {
		return super.containsColumn( column ) || includedTable.containsColumn( column );
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}

	public PhysicalTable getIncludedTable() {
		return includedTable;
	}

	@Internal
	@Override
	public void reorderColumns(List<Column> columns) {
		assert includedTable.getColumns().size() + super.getColumns().size() == columns.size()
				&& columns.containsAll( includedTable.getColumns() )
				&& columns.containsAll( super.getColumns() )
				&& reorderedColumns == null;
		this.reorderedColumns = columns;
	}
}
