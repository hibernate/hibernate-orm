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
	// Delete a single row
	DELETE;

	public boolean canSkipTables() {
		return switch(this) {
			case INSERT, UPDATE -> true;
			case DELETE -> false;
		};
	}
}
