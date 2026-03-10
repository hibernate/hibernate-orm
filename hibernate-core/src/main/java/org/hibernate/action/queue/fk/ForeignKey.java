/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.fk;

import org.hibernate.metamodel.mapping.SelectableMappings;

import java.io.Serializable;

/// Describes a foreign-key in terms needed by graph creation and scheduling.
///
/// @param keyTable The table which holds the foreign-key column(s) - `orders`.
/// @param targetTable The table which the foreign-key targets - `customers`.
/// @param keyColumns The foreign-key column(s) - `orders.customer_fk`.
/// @param targetColumns The column(s) which the foreign-key targets or points to - `customers.id`.
/// @param targetType What type of constraint the foreign-key targets (primary key, unique key, or non-unique).
/// @param isAssociation Whether the foreign-key is mapping an association, as opposed to secondary or hierarchy tables.
/// @param nullable Whether the foreign-key is nullable - that is, all of its column(s) are nullable.
/// @param deferrable Whether the foreign-key is defined as deferrable in the database.  Currently not used.
///
/// @author Steve Ebersole
public record ForeignKey(
		String keyTable,
		String targetTable,
		SelectableMappings keyColumns,
		SelectableMappings targetColumns,
		TargetType targetType,
		boolean isAssociation,
		boolean nullable,
		boolean deferrable) implements Serializable {

	public enum TargetType {
		/**
		 * Foreign key targets a primary key
		 */
		PRIMARY_KEY,

		/**
		 * Foreign key targets a unique key (not primary key)
		 */
		UNIQUE_KEY,

		/**
		 * Foreign key targets non-unique column(s)
		 */
		NON_UNIQUE
	}

	/**
	 * Check if this foreign key targets a unique constraint (either primary key or unique key)
	 */
	public boolean targetsUniqueConstraint() {
		return targetType == TargetType.PRIMARY_KEY || targetType == TargetType.UNIQUE_KEY;
	}
}
