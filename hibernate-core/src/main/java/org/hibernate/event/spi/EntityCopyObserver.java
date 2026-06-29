/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;


/**
 * An observer for detection of multiple entity representations for a persistent entity being merged.
 *
 * @see MergeContext
 *
 * @author Gail Badner
 */
public interface EntityCopyObserver {

	/**
	 * Called when more than one representation of the same persistent entity is being merged.
	 *
	 * @param managedEntity The managed entity in the persistence context (the merge result).
	 * @param mergeEntity1 A managed or detached entity being merged; must be non-null.
	 * @param mergeEntity2 A different managed or detached entity being merged; must be non-null.
	 * @param session The session.
	 */
	void entityCopyDetected(@Nonnull Object managedEntity, @Nonnull Object mergeEntity1, @Nonnull Object mergeEntity2, @Nonnull EventSource session);

	/**
	 * Called when the toplevel merge operation is complete.
	 *
	 * @param session The session
	 */
	void topLevelMergeComplete(@Nonnull EventSource session);

	/**
	 * Called to clear any data stored in this {@code EntityCopyObserver}.
	 */
	void clear();
}
