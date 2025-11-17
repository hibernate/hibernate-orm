/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs before a collection is removed
 *
 * @author Gail Badner
 */
public class PreCollectionRemoveEvent extends AbstractCollectionEvent {

	public PreCollectionRemoveEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			EventSource source,
			Object loadedOwner) {
		super(
				collectionPersister,
				collection,
				source,
				loadedOwner,
				getOwnerIdOrNull( loadedOwner, source )
		);
	}

	public PreCollectionRemoveEvent(
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object loadedOwner) {
		super( collection, entityName, loadedOwner, id );
	}
}
