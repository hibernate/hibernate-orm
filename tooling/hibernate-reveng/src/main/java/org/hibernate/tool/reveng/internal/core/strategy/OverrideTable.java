/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.reveng.api.core.ForeignKeyDefinition;
import org.hibernate.tool.reveng.api.core.TableIdentifier;

/// Parse state for a table override, independent of the relational mapping model.
///
/// @author Steve Ebersole
final class OverrideTable {
	final TableIdentifier selector;
	final List<ForeignKeyDefinition> foreignKeys = new ArrayList<>();
	private final Map<String, String> columns = new HashMap<>();

	OverrideTable(String catalog, String schema, String name) {
		selector = TableIdentifier.create( catalog, schema, name );
	}

	TableIdentifier lookupKey() {
		return lookupKey( selector );
	}

	static TableIdentifier lookupKey(TableIdentifier selector) {
		return TableIdentifier.create( text( selector.getCatalog() ), text( selector.getSchema() ), text( selector.getName() ) );
	}

	static String text(String name) {
		final var identifier = Identifier.toIdentifier( name );
		return identifier == null ? null : identifier.getText();
	}

	void addColumn(String name) {
		final var existing = columns.get( canonical( name ) );
		if ( existing != null && sameColumn( name, existing ) ) {
			throw new MappingException( "Column " + text( name ) + " already exists in table " + lookupKey() );
		}
		columns.put( canonical( name ), name );
	}

	void addForeignKey(String name, TableIdentifier target, List<ForeignKeyDefinition.ColumnReference> references) {
		for ( int i = 0; i < foreignKeys.size(); i++ ) {
			final var previous = foreignKeys.get( i );
			if ( previous.referencedTable().getName().equals( target.getName() ) && sameColumns( previous.columns(), references ) ) {
				foreignKeys.set( i, new ForeignKeyDefinition( name == null ? previous.name() : name, selector, target, previous.columns() ) );
				return;
			}
		}
		foreignKeys.add( new ForeignKeyDefinition( name, selector, target, references ) );
	}

	private static boolean sameColumns(List<ForeignKeyDefinition.ColumnReference> first, List<ForeignKeyDefinition.ColumnReference> second) {
		if ( first.size() != second.size() ) {
			return false;
		}
		for ( int i = 0; i < first.size(); i++ ) {
			if ( !sameColumn( first.get( i ).column(), second.get( i ).column() )
					|| !sameColumn( first.get( i ).referencedColumn(), second.get( i ).referencedColumn() ) ) {
				return false;
			}
		}
		return true;
	}

	private static boolean sameColumn(String existing, String incoming) {
		final var first = Identifier.toIdentifier( existing );
		final var second = Identifier.toIdentifier( incoming );
		// Preserve the old Column-key hash/equality behavior, including mixed quoting.
		return first.getCanonicalName().equals( second.getCanonicalName() )
				&& (first.isQuoted() ? first.getText().equals( second.getText() )
						: first.getText().equalsIgnoreCase( second.getText() ));
	}

	private static String canonical(String name) {
		return Identifier.toIdentifier( name ).getCanonicalName();
	}
}
