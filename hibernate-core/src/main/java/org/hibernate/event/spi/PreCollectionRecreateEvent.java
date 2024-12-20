/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs before a collection is recreated
 *
 * @author Gail Badner
 */
public class PreCollectionRecreateEvent extends AbstractCollectionEvent {

	public PreCollectionRecreateEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			EventSource source) {
		super(
				collectionPersister,
				collection,
				source,
				collection.getOwner(),
				getOwnerIdOrNull( collection.getOwner(), source )
		);
	}
}
