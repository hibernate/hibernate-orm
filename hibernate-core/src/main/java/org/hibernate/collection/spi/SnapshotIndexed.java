/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

/// Marker interface for objects that carry both an element and its snapshot position/key.
/// Used during collection update decomposition to provide the snapshot position/key for
/// DELETE operations on indexed collections.
///
/// This allows binding code to obtain the correct snapshot position (for Lists) or key (for Maps)
/// when generating WHERE clauses that restrict by index (e.g., `WHERE listOrder = ?` or `WHERE mapKey = ?`).
///
/// @param <K> The type of the index/key - Integer for Lists, the map key type for Maps
/// @author Steve Ebersole
public interface SnapshotIndexed<K> {
	/// The element (entity or value) at this position.
	Object element();

	/// The position/key where this element existed in the snapshot.
	/// - For Lists: the numeric position (0, 1, 2, ...)
	/// - For Maps: the actual map key
	/// Used for WHERE clause restrictions in UPDATE/DELETE operations.
	K snapshotIndex();
}
