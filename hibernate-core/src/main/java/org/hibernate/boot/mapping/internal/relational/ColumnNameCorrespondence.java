/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;

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
	private final Map<Table, TableColumnNames> tableColumnNames = new IdentityHashMap<>();

	public ColumnNameCorrespondence(Database database) {
		this.database = database;
	}

	public void register(Table table, Identifier logicalName, Column physicalColumn) {
		if ( table == null || logicalName == null || physicalColumn == null ) {
			return;
		}
		tableColumnNames.computeIfAbsent( table, ignored -> new TableColumnNames() )
				.register( logicalName, physicalColumn );
	}

	public Identifier findLogicalName(Table table, Column physicalColumn) {
		if ( physicalColumn == null ) {
			return null;
		}
		return findLogicalName( table, physicalColumn.getNameIdentifier( database ) );
	}

	public Identifier findLogicalName(Table table, Identifier physicalName) {
		if ( table == null || physicalName == null ) {
			return null;
		}
		final String physicalNameText = physicalName.render( database.getDialect() );
		Table currentTable = table;
		while ( currentTable != null ) {
			final TableColumnNames columnNames = tableColumnNames.get( currentTable );
			if ( columnNames != null ) {
				final Identifier logicalName = columnNames.logicalByPhysicalName.get( physicalNameText );
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

	public Column findPhysicalColumn(Table table, Identifier logicalName) {
		if ( table == null || logicalName == null ) {
			return null;
		}
		Table currentTable = table;
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

	private class TableColumnNames {
		private final Map<Identifier, Column> physicalColumnByLogicalName = new HashMap<>();
		private final Map<String, Identifier> logicalByPhysicalName = new HashMap<>();

		private void register(Identifier logicalName, Column physicalColumn) {
			physicalColumnByLogicalName.put( logicalName, physicalColumn );
			logicalByPhysicalName.put(
					physicalColumn.getNameIdentifier( database ).render( database.getDialect() ),
					logicalName
			);
		}
	}
}
