/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EventSource;

/**
 * An {@link org.hibernate.event.spi.EntityCopyObserver} implementation that allows multiple representations of
 * the same persistent entity to be merged.
 *
 * @author Gail Badner
 */
public class EntityCopyAllowedObserver implements EntityCopyObserver {

	public static final String SHORT_NAME = "allow";

	@Override
	public void entityCopyDetected(
			Object managedEntity,
			Object mergeEntity1,
			Object mergeEntity2,
			EventSource session) {
		// do nothing.
	}

	public void clear() {
		// do nothing.
	}


	@Override
	public void topLevelMergeComplete(EventSource session) {
		// do nothing.
	}
}
