/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.EventSource;

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
