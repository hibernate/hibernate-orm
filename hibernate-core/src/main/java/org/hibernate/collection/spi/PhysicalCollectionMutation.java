/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

/// The immutable physical realization selected for a semantic collection mutation.
///
/// The hierarchy is deliberately exhaustive. Each subtype carries exactly the
/// row facts required by that physical strategy.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public sealed interface PhysicalCollectionMutation {
	/// The semantic mutation owns no collection-table mutation.
	record NoWork() implements PhysicalCollectionMutation {
	}

	/// Remove all rows belonging to the loaded collection endpoint.
	record RemoveAll(@Nonnull RemovalMode removalMode) implements PhysicalCollectionMutation {
	}

	/// Insert every retained current row for a logical create.
	record CreateAll(@Nonnull FrozenCollectionRows currentRows) implements PhysicalCollectionMutation {
	}

	/// Apply a semantics-specific immutable row-change set.
	record RowChanges(@Nonnull CollectionChangeSet changes) implements PhysicalCollectionMutation {
	}

	/// Physically replace all rows while retaining update lifecycle semantics.
	record RemoveAllAndCreateAll(
			@Nonnull RemovalMode removalMode,
			@Nonnull FrozenCollectionRows currentRows) implements PhysicalCollectionMutation {
	}

	enum RemovalMode {
		EXECUTE,
		SKIP
	}

	static NoWork noWork() {
		return NoWorkHolder.INSTANCE;
	}

	final class NoWorkHolder {
		private static final NoWork INSTANCE = new NoWork();

		private NoWorkHolder() {
		}
	}
}
