/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/// One ordered semantic change within a [CollectionDelta].
///
/// Snapshot and current positions are deliberately distinct. A position may be a
/// list index, map key, collection identifier, or an explicit deferred-position
/// handle supplied by a specialized producer.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public sealed interface CollectionChange {
	/// An element newly present at its current position.
	record Addition(Object element, @Nullable Object currentPosition) implements CollectionChange {
	}

	/// An element no longer present at its snapshot position.
	record Removal(Object element, @Nullable Object snapshotPosition) implements CollectionChange {
	}

	/// A value replaced while retaining its semantic row identity.
	record Replacement(
			Object snapshotValue,
			Object currentValue,
			@Nullable Object snapshotPosition,
			@Nullable Object currentPosition) implements CollectionChange {
	}

	/// An element retained while its semantic position changed.
	record PositionChange(
			Object element,
			@Nullable Object snapshotPosition,
			@Nullable Object currentPosition) implements CollectionChange {
	}

	/// A bulk clear whose individual persisted removals are not enumerated.
	record Clear() implements CollectionChange {
	}
}
