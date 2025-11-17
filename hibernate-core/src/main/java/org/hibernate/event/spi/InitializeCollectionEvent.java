/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;

/**
 * An event that occurs when a collection wants to be
 * initialized
 *
 * @author Gavin King
 */
public class InitializeCollectionEvent extends AbstractCollectionEvent {

	public InitializeCollectionEvent(PersistentCollection<?> collection, EventSource source ) {
		super(
				getLoadedCollectionPersister( collection, source ),
				collection,
				source,
				getLoadedOwnerOrNull( collection, source ),
				getLoadedOwnerIdOrNull( collection, source )
		);
	}
}
