/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.ColumnContainer;

/// Binding-time correspondence between a table-local logical column name and
/// the physical `Column` produced for the compatibility mapping model.
///
/// This is the new-pipeline owner for the fact historically stored in
/// `InFlightMetadataCollector` column-name bindings.  The collector still gets
/// a compatibility projection, but binders should ask this correspondence first
/// when they need to recover logical source names from physical columns.
///
/// @since 9.0
/// @author Steve Ebersole
public class ColumnNameCorrespondence {
	private final Database database;
	private final Map<Column, Map<Column, LogicalName>> selectedReferences = new IdentityHashMap<>();

	/// Retain the selected reference alias alongside the actual local/target pair.
	public void registerReferenceName(Column local, Column target, LogicalName selected) {
		selectedReferences.computeIfAbsent( local, ignored -> new IdentityHashMap<>() ).putIfAbsent( target, selected );
	}

	public LogicalName findReferenceName(Column local, Column target) {
		final var references = selectedReferences.get( local );
		return references == null ? null : references.get( target );
	}

	private final Map<Column, LogicalName> declarationNames = new IdentityHashMap<>();
	private final Map<ColumnContainer, TableColumnNames> tableColumnNames = new IdentityHashMap<>();

	public ColumnNameCorrespondence(Database database) {
		this.database = database;
	}

	/// Retain a declaration without projecting an aggregate member onto the owner table.
	public void registerDeclarationName(Column column, LogicalName name) {
		declarationNames.putIfAbsent( column, name );
	}

	public void register(ColumnContainer table, LogicalName logicalName, Column physicalColumn) {
		if ( table == null || logicalName == null || physicalColumn == null ) {
			return;
		}
		declarationNames.putIfAbsent( physicalColumn, logicalName );
		tableColumnNames.computeIfAbsent( table, ignored -> new TableColumnNames() )
				.register( logicalName, physicalColumn );
	}

	/// A derived-identifier override becomes the selected declaration; old names remain aliases.
	public void registerDeclaration(ColumnContainer table, LogicalName logicalName, Column column) {
		register( table, logicalName, column );
		declarationNames.put( column, logicalName );
	}

	/// Recover the selected column declaration, never an arbitrary physical-name alias.
	public LogicalName findDeclarationName(ColumnContainer table, Column column) {
		final LogicalName declaration = declarationNames.get( column );
		if ( declaration != null ) {
			return declaration;
		}
		final var names = tableColumnNames.get( table );
		LogicalName candidate = null;
		if ( names != null ) {
			for ( var entry : names.physicalColumnByLogicalName.entrySet() ) {
				if ( entry.getValue().getPhysicalName().equals( column.getPhysicalName() ) ) {
					if ( candidate != null && !candidate.equals( entry.getKey() ) ) {
						throw new org.hibernate.MappingException( "Ambiguous logical column dependency: " + column.getName() );
					}
					candidate = entry.getKey();
				}
			}
		}
		return candidate;
	}

	/// Select the named alias while retaining its declaration's explicit/implicit origin.
	public LogicalName selectReferenceName(ColumnContainer table, Column column, LogicalName selected) {
		final var declaration = declarationNames.get( column );
		if ( declaration != null && declaration.equals( selected ) ) {
			return declaration;
		}
		final var names = tableColumnNames.get( table );
		if ( names != null ) {
			for ( var entry : names.physicalColumnByLogicalName.entrySet() ) {
				if ( entry.getKey().equals( selected ) && entry.getValue().getPhysicalName().equals( column.getPhysicalName() ) ) {
					return entry.getKey();
				}
			}
		}
		return selected;
	}

	public LogicalName findLogicalName(ColumnContainer table, Column physicalColumn) {
		if ( physicalColumn == null ) {
			return null;
		}
		return findLogicalName( table, physicalName( physicalColumn ) );
	}

	public LogicalName findLogicalName(ColumnContainer table, PhysicalName physicalName) {
		if ( table == null || physicalName == null ) {
			return null;
		}
		ColumnContainer currentTable = table;
		while ( currentTable != null ) {
			final TableColumnNames columnNames = tableColumnNames.get( currentTable );
			if ( columnNames != null ) {
				final LogicalName logicalName = columnNames.logicalByPhysicalName.get( physicalName );
				if ( logicalName != null ) {
					return logicalName;
				}
			}
			currentTable = currentTable instanceof DenormalizedTable denormalizedTable
					? denormalizedTable.getIncludedTable()
					: null;
		}
		return null;
	}

	public Column findPhysicalColumn(ColumnContainer table, LogicalName logicalName) {
		if ( table == null || logicalName == null ) {
			return null;
		}
		ColumnContainer currentTable = table;
		while ( currentTable != null ) {
			final TableColumnNames columnNames = tableColumnNames.get( currentTable );
			if ( columnNames != null ) {
				final Column physicalColumn = columnNames.physicalColumnByLogicalName.get( logicalName );
				if ( physicalColumn != null ) {
					return physicalColumn;
				}
			}
			currentTable = currentTable instanceof DenormalizedTable denormalizedTable
					? denormalizedTable.getIncludedTable()
					: null;
		}
		return null;
	}

	/// Matches a logical referenced name to a materialized target column without
	/// applying physical naming a second time. Synthetic columns without a
	/// registered correspondence retain their source-name matching behavior.
	public boolean matches(Column column, LogicalName logicalName) {
		if ( logicalName == null ) {
			return false;
		}
		final PhysicalName physicalName = physicalName( column );
		ColumnContainer table = column.getValue() == null ? null : column.getValue().getColumnContainer();
		boolean registered = false;
		while ( table != null ) {
			final TableColumnNames names = tableColumnNames.get( table );
			if ( names != null ) {
				for ( var entry : names.physicalColumnByLogicalName.entrySet() ) {
					if ( physicalName( entry.getValue() ).equals( physicalName ) ) {
						registered = true;
						if ( entry.getKey().equals( logicalName ) ) {
							return true;
						}
					}
				}
			}
			table = table instanceof DenormalizedTable denormalizedTable
					? denormalizedTable.getIncludedTable() : null;
		}
		// Unregistered synthetic columns still carry source names. Do not use
		// this fallback for a materialized column with a registered logical name.
		return !registered && new LogicalName( physicalName.getText(), physicalName.isQuoted(), false ).equals( logicalName );
	}

	private PhysicalName physicalName(Column column) {
		return column.getPhysicalName();
	}

	public void rebuildPhysicalIndex() {
		tableColumnNames.values().forEach( names -> {
			names.logicalByPhysicalName.clear();
			names.physicalColumnByLogicalName.forEach( (logical, column) ->
					names.logicalByPhysicalName.put( column.getPhysicalName(), logical ) );
		} );
	}

	private class TableColumnNames {
		private final Map<LogicalName, Column> physicalColumnByLogicalName = new HashMap<>();
		private final Map<PhysicalName, LogicalName> logicalByPhysicalName = new HashMap<>();

		private void register(LogicalName logicalName, Column physicalColumn) {
			physicalColumnByLogicalName.put( logicalName, physicalColumn );
			logicalByPhysicalName.put(
					physicalName( physicalColumn ),
					logicalName
			);
		}
	}
}
