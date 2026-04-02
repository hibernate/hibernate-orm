/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.List;

/// Represents the complete set of changes between a collection's snapshot state
/// and its current state. Used for planning collection update operations.
///
/// @author Steve Ebersole
public record CollectionChangeSet(
		List<Removal> removals,
		List<Addition> additions,
		List<Shift> shifts,
		List<ValueChange> valueChanges) {
	public static final CollectionChangeSet EMPTY = new CollectionChangeSet(List.of(), List.of(), List.of(), List.of());

	public boolean isEmpty() {
		return removals.isEmpty() && additions.isEmpty() && shifts.isEmpty() && valueChanges.isEmpty();
	}

	/// Represents an element that was removed from the collection.
	///
	/// @param element The removed element
	/// @param snapshotPosition The position where the element existed in the snapshot
	public record Removal(Object element, int snapshotPosition) implements SnapshotPositioned {}

	/// Represents an element that was added to the collection.
	///
	/// @param element The added element
	/// @param currentPosition The position where the element exists in the current collection
	public record Addition(Object element, int currentPosition) {}

	/// Represents an element that changed position within the collection.
	///
	/// @param element The shifted element
	/// @param snapshotPosition The position where the element existed in the snapshot
	/// @param currentPosition The position where the element exists in the current collection
	public record Shift(Object element, int snapshotPosition, int currentPosition) {}

	/// Represents an element whose value changed at the same position.
	/// Applicable to element collections where the element itself (not the FK) is stored.
	///
	/// @param oldValue The value in the snapshot
	/// @param newValue The value in the current collection
	/// @param position The position where the value changed
	public record ValueChange(Object oldValue, Object newValue, int position) {}
}
