/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

/// Kind of table mutation
///
/// @author Steve Ebersole
public enum MutationKind {
	// Insert a single row
	INSERT,
	// Update a single row
	UPDATE,
	// Update a single row's order/index column only (no FK changes)
	// Used for indexed collections where only position changed, not entity identity
	UPDATE_ORDER,
	// Delete a single row
	DELETE,
	// No-op operation (callback carrier)
	NO_OP;

	public boolean canSkipTables() {
		return switch(this) {
			case INSERT, UPDATE, UPDATE_ORDER -> true;
			case DELETE -> false;
			case NO_OP -> true;  // No SQL execution
		};
	}
}
