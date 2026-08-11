/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

/// The semantic difference information retained by a collection mutation
/// interpretation independently of its physical mutation strategy.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public sealed interface SemanticCollectionChange {
	/// No separate semantic difference was materialized.
	record None() implements SemanticCollectionChange {
	}

	/// An enumerable semantic difference relative to an identified baseline.
	record Delta(@Nonnull CollectionDelta delta) implements SemanticCollectionChange {
	}

	/// Prior collection contents are removed without claiming that their rows are enumerable.
	record BulkRemoval() implements SemanticCollectionChange {
	}

	static None none() {
		return NoneHolder.INSTANCE;
	}

	static BulkRemoval bulkRemoval() {
		return BulkRemovalHolder.INSTANCE;
	}

	final class NoneHolder {
		private static final None INSTANCE = new None();

		private NoneHolder() {
		}
	}

	final class BulkRemovalHolder {
		private static final BulkRemoval INSTANCE = new BulkRemoval();

		private BulkRemovalHolder() {
		}
	}
}
