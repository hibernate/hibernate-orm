/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

/**
 * An observer for detection of multiple entity representations for a persistent entity being merged.
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
	void entityCopyDetected(Object managedEntity, Object mergeEntity1, Object mergeEntity2, EventSource session);

	/**
	 * Called when the top-level merge operation is complete.
	 *
	 * @param session The session
	 */
	void topLevelMergeComplete(EventSource session);

	/**
	 * Called to clear any data stored in this EntityCopyObserver.
	 */
	void clear();
}
