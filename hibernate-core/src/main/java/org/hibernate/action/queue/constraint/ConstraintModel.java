/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Complete model of database constraints (foreign keys and unique constraints)
 * needed for ActionQueue graph building and scheduling.
 *
 * @author Steve Ebersole
 */
public record ConstraintModel(
		List<ForeignKey> foreignKeys,
		List<UniqueConstraint> uniqueConstraints,
		Map<String, List<UniqueConstraint>> uniqueConstraintsByTable,
		Map<String, List<ForeignKey>> inboundForeignKeysByTable,
		Map<String, List<ForeignKey>> outboundForeignKeysByTable,
		java.util.Set<String> tablesWithCyclicForeignKeys,
		java.util.Set<String> selfReferentialTables) implements Serializable {

	/**
	 * Get unique constraints for a specific table
	 */
	public List<UniqueConstraint> getUniqueConstraintsForTable(String tableName) {
		final List<UniqueConstraint> constraints = uniqueConstraintsByTable.get( normalizeTableExpression( tableName ) );
		if ( constraints != null ) {
			return constraints;
		}

		for ( Map.Entry<String, List<UniqueConstraint>> entry : uniqueConstraintsByTable.entrySet() ) {
			if ( tableNamesMatch( entry.getKey(), tableName ) ) {
				return entry.getValue();
			}
		}

		return List.of();
	}

	/**
	 * Check if a table has cyclic foreign key relationships (bidirectional FKs).
	 * Useful for determining if DELETE operations need ordinal-based grouping.
	 */
	public boolean hasTableCyclicForeignKeys(String tableName) {
		final String normalizedTableName = normalizeTableExpression( tableName );
		if ( tablesWithCyclicForeignKeys.contains( normalizedTableName ) ) {
			return true;
		}

		for ( String cyclicTable : tablesWithCyclicForeignKeys ) {
			if ( tableNamesMatch( cyclicTable, tableName ) ) {
				return true;
			}
		}

		return false;
	}

	public boolean hasSelfReferentialTable(String tableName) {
		final String normalizedTableName = normalizeTableExpression( tableName );
		if ( selfReferentialTables.contains( normalizedTableName ) ) {
			return true;
		}

		for ( String selfReferentialTable : selfReferentialTables ) {
			if ( tableNamesMatch( selfReferentialTable, tableName ) ) {
				return true;
			}
		}

		return false;
	}

	static boolean tableNamesMatch(String tableName1, String tableName2) {
		if ( tableName1 == null || tableName2 == null ) {
			return tableName1 == tableName2;
		}
		final String normalized1 = normalizeTableExpression( tableName1 );
		final String normalized2 = normalizeTableExpression( tableName2 );
		return normalized1.equals( normalized2 )
				|| normalized1.endsWith( "." + normalized2 )
				|| normalized2.endsWith( "." + normalized1 );
	}

	static String normalizeTableExpression(String tableExpression) {
		if ( tableExpression == null ) {
			return "";
		}
		final String[] parts = tableExpression.split( "\\." );
		for ( int i = 0; i < parts.length; i++ ) {
			parts[i] = normalizeIdentifier( parts[i] );
		}
		return String.join( ".", parts );
	}

	static String normalizeIdentifier(String identifier) {
		if ( identifier == null ) {
			return "";
		}
		final String trimmed = identifier.trim();
		if ( trimmed.length() > 1 ) {
			final char first = trimmed.charAt( 0 );
			final char last = trimmed.charAt( trimmed.length() - 1 );
			if ( ( first == '`' && last == '`' )
					|| ( first == '"' && last == '"' )
					|| ( first == '[' && last == ']' ) ) {
				return trimmed.substring( 1, trimmed.length() - 1 ).toLowerCase( Locale.ROOT );
			}
		}
		return trimmed.toLowerCase( Locale.ROOT );
	}
}
