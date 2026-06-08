/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.Incubating;
import org.hibernate.collection.spi.PersistentCollection;

/// Read-only view of collection processing state and actions planned during the current flush.
///
/// A tracker is associated with a single flush cycle. It exposes state that used to be stored as
/// transient flags on {@link CollectionEntry}, allowing code that runs during or immediately after
/// flush action execution to answer questions about reachability, processing, and queued logical
/// collection actions without mutating the entry itself.
///
/// Implementations are expected to identify collections by instance identity, matching the way
/// persistent collections are tracked by the persistence context.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public interface CollectionFlushActionTracker {
	/// Was the collection found reachable from a flushed entity during the current flush?
	///
	/// @param collection The collection instance
	///
	/// @return {@code true} if the collection was marked reachable
	boolean wasCollectionReached(PersistentCollection<?> collection);

	/// Was the collection already processed by collection reachability handling during the current flush?
	///
	/// @param collection The collection instance
	///
	/// @return {@code true} if the collection was marked processed
	boolean wasCollectionProcessed(PersistentCollection<?> collection);

	/// Was any logical collection action queued for the collection during the current flush?
	///
	/// This includes recreate, remove, and update actions, independent of whether a later optimization
	/// suppresses the physical action.
	///
	/// @param collection The collection instance
	///
	/// @return {@code true} if at least one logical collection action was queued
	boolean hasQueuedCollectionAction(PersistentCollection<?> collection);

	/// Was a logical collection remove action queued for the collection during the current flush?
	///
	/// @param collection The collection instance
	///
	/// @return {@code true} if a remove action was queued
	boolean hasQueuedCollectionRemove(PersistentCollection<?> collection);
}
