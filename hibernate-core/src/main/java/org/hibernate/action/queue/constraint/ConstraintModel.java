/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import java.io.Serializable;
import java.util.List;
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
		return uniqueConstraintsByTable.getOrDefault(tableName, List.of());
	}

	/**
	 * Check if a table has cyclic foreign key relationships (bidirectional FKs).
	 * Useful for determining if DELETE operations need ordinal-based grouping.
	 */
	public boolean hasTableCyclicForeignKeys(String tableName) {
		return tablesWithCyclicForeignKeys.contains(tableName);
	}
}
