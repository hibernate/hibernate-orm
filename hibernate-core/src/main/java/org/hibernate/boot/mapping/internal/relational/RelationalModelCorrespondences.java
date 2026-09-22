/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.relational;

import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Collections;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Column;
import org.hibernate.relational.naming.spi.LogicalName;

import org.hibernate.boot.model.relational.Database;

/// Binding-time relational correspondence registry.
///
/// Correspondences capture pairings that are produced while materializing the
/// compatibility mapping model and later need to be queried without relying on
/// legacy collector side channels.
///
/// @since 9.0
/// @author Steve Ebersole
public class RelationalModelCorrespondences {
	private final ColumnNameCorrespondence columnNames;
	private final Map<Table, LogicalName> tableNames = new IdentityHashMap<>();
	private final Map<Table, LogicalName> tableCreationNames = new IdentityHashMap<>();
	private final Map<ForeignKey, Map<Column, LogicalName>> referencedNames = new IdentityHashMap<>();
	private final Map<ForeignKey, java.util.List<LogicalName>> foreignKeyTables = new IdentityHashMap<>();
	private final Set<ForeignKey> namedForeignKeys = Collections.newSetFromMap( new IdentityHashMap<>() );

	/// Capture owner-selected table aliases before the binder source is discarded.
	public void registerForeignKeyTables(ForeignKey key, LogicalName local, LogicalName target) {
		foreignKeyTables.putIfAbsent( key, java.util.List.of( local, target ) );
	}

	public LogicalName foreignKeyTableName(ForeignKey key, boolean target) {
		final var tables = foreignKeyTables.get( key );
		return tables == null ? null : tables.get( target ? 1 : 0 );
	}

	public void registerTableName(Table table, LogicalName name) {
		tableNames.putIfAbsent( table, name );
	}

	/// Retain the creation declaration for contributors without a later name binding.
	public void registerTableCreationName(Table table, LogicalName name) {
		tableCreationNames.putIfAbsent( table, name );
	}

	public LogicalName tableName(Table table) {
		final var name = tableNames.get( table );
		return name == null ? tableCreationNames.get( table ) : name;
	}

	public void registerReferenceName(ForeignKey key, Column column,
			LogicalName name) {
		referencedNames.computeIfAbsent( key, ignored -> new IdentityHashMap<>() ).put( column, name );
	}

	public LogicalName referenceName(ForeignKey key, Column column) {
		final var names = referencedNames.get( key );
		return names == null ? null : names.get( column );
	}

	public boolean isForeignKeyNamed(ForeignKey key) {
		return namedForeignKeys.contains( key );
	}
	public void markForeignKeyNamed(ForeignKey key) {
		namedForeignKeys.add( key );
	}


	public RelationalModelCorrespondences(Database database) {
		columnNames = new ColumnNameCorrespondence( database );
	}

	public ColumnNameCorrespondence columnNames() {
		return columnNames;
	}
}
