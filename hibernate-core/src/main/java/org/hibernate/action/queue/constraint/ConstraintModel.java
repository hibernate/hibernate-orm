/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.constraint;

import org.hibernate.action.queue.fk.ForeignKey;

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
		Map<String, List<UniqueConstraint>> uniqueConstraintsByTable) implements Serializable {

	/**
	 * Get unique constraints for a specific table
	 */
	public List<UniqueConstraint> getUniqueConstraintsForTable(String tableName) {
		return uniqueConstraintsByTable.getOrDefault(tableName, List.of());
	}
}
