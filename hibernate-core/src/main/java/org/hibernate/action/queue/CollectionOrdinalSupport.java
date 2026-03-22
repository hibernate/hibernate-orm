/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

/// Constants and utilities for calculating ordinals for [org.hibernate.action.queue.op.PlannedOperation]
/// related to collection actions.
///
/// Ordinals help determine the execution order of database operations during flush within graph building and
/// planning and discovered edges. This class provides a consistent, hierarchical scheme for assigning ordinals
///  across all collection mutation decomposers.
///
/// The ordinal scheme uses a two-level hierarchy:
/// <ol>
///   - **Collection Slot**: Each collection operation receives a unique ordinalBase from ActionQueue (0, 1, 2, ...)
/// 		based on the order it was added to the queue
///   - **Operation Type**: DELETE, UPDATE, INSERT, etc. have fixed offsets within the slot
/// </ol>
///
/// Formula: `ordinal = (collectionOrdinalBase * SLOT_SIZE) + operationTypeOffset`
///
/// Example (collection ordinalBase=5):
///
///   - All DELETEs: 5000
///   - All UPDATEs: 5100
///   - All INSERTs: 5200
///   - All WRITEINDEXes: 5300
///
/// This ensures:
///
///   - No ordinal collisions between operation types
///   - Correct ordering: DELETE &lt; UPDATE &lt; INSERT &lt; WRITEINDEX
///   - Works for both bundled (single operation per type) and non-bundled (multiple operations per type)
///   - Compatible with FlushCoordinator's grouping logic for self-referential tables
///
/// **Note**: Row ordering within a single operation type is handled by the graph's natural execution order,
/// not by ordinal values. All operations of the same type for a collection share the same ordinal.
///
/// **Limits**:
///
///   - Max collections per flush: Limited by SLOT_SIZE (1000 collections)
///
/// If these limits are insufficient, adjust SLOT_SIZE and offsets proportionally.
///
/// @author Steve Ebersole
public final class CollectionOrdinalSupport {

	/// Size of the ordinal range allocated to each collection.
	///
	/// This determines:
	///
	///   - How many collections can be processed in a single flush (up to 1000)
	///   - The grouping logic in FlushCoordinator (ordinal / SLOT_SIZE groups by collection)
	///
	/// Must be large enough to hold all operation types with distinct offsets:
	/// DELETE (0) + UPDATE (100) + INSERT (200) + WRITEINDEX (300).
	public static final int SLOT_SIZE = 1_000;

	/// Operation type slots within a collection's ordinal range.
	///
	/// Each slot has a specific offset that determines execution order:
	/// DELETE < UPDATE < INSERT < WRITEINDEX
	public enum Slot {
		/// DELETE operations remove old rows before inserting new ones
		/// (especially important for collections with unique constraints like `@OrderColumn`).
		DELETE(0),

		/// UPDATE operations execute after DELETEs but before INSERTs.
		UPDATE(100),

		/// INSERT operations execute after DELETEs to avoid unique constraint violations
		/// on collections with composite primary keys (e.g., entity_id + list index).
		INSERT(200),

		/// WRITEINDEX operations update the index/position column for indexed collections.
		/// These execute after INSERTs since they operate on already-inserted rows.
		WRITEINDEX(300);

		private final int offset;

		Slot(int offset) {
			this.offset = offset;
		}

		public int offset() {
			return offset;
		}
	}

	private CollectionOrdinalSupport() {
		// Utility class, no instances
	}

	/// Calculates the ordinal for a planned operation.
	///
	/// Formula: `ordinal = (collectionOrdinalBase * SLOT_SIZE) + slot.offset()`
	///
	/// @param collectionOrdinalBase the base ordinal for this collection (from ActionQueue, 0-based sequential)
	/// @param slot the operation type slot (DELETE, UPDATE, INSERT, or WRITEINDEX)
	/// @return the calculated ordinal
	public static int calculateOrdinal(int collectionOrdinalBase, Slot slot) {
		return (collectionOrdinalBase * SLOT_SIZE) + slot.offset();
	}

	/// Extracts the collection ordinal from a calculated ordinal.
	///
	/// Used by [FlushCoordinator] for grouping operations from self-referential tables.
	/// Operations from the same collection (same ordinalBase) are grouped together to avoid
	/// creating false dependency cycles.
	///
	/// @param ordinal a calculated ordinal from [#calculate]
	/// @return the collection ordinalBase that was used to calculate this ordinal
	public static int extractCollectionOrdinal(int ordinal) {
		return ordinal / SLOT_SIZE;
	}
}
