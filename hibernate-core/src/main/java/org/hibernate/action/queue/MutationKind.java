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
	DELETE,
	// Delete multiple rows by a foreign-key
	DELETE_BY_FK;

	public boolean canSkipTables() {
		return switch(this) {
			case INSERT, UPDATE -> true;
			case DELETE, DELETE_BY_FK -> false;
		};
	}
}
