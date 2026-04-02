/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

/// Marker interface for objects that carry both an element and its snapshot position.
/// Used during collection update decomposition to provide the snapshot position for
/// DELETE operations on indexed collections.
///
/// This allows binding code to obtain the correct snapshot position when generating
/// WHERE clauses that restrict by index (e.g., `WHERE listOrder = ?`).
///
/// @author Steve Ebersole
public interface SnapshotPositioned {
	/// The element (entity or value) at this position.
	Object element();

	/// The position where this element existed in the snapshot.
	/// Used for WHERE clause restrictions in UPDATE/DELETE operations.
	int snapshotPosition();
}
