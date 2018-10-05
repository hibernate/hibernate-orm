/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.EntityCopyObserver;
import org.hibernate.event.spi.EntityCopyObserverFactory;
import org.hibernate.event.spi.EventSource;

/**
 * An {@link org.hibernate.event.spi.EntityCopyObserver} implementation that allows multiple representations of
 * the same persistent entity to be merged.
 *
 * @author Gail Badner
 */
public final class EntityCopyAllowedObserver implements EntityCopyObserver {

	public static final String SHORT_NAME = "allow";
	private static final EntityCopyObserver INSTANCE = new EntityCopyAllowedObserver();

	//This implementation of EntityCopyObserver is stateless, so no need to create multiple copies:
	public static final EntityCopyObserverFactory FACTORY_OF_SELF = () -> INSTANCE;

	private EntityCopyAllowedObserver() {
		//Not to be constructed; use INSTANCE.
	}

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
