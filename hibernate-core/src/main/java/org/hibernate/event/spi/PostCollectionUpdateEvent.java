/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Internal;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs after a collection is updated
 *
 * @author Gail Badner
 */
public class PostCollectionUpdateEvent extends AbstractCollectionEvent {

	@Internal
	public PostCollectionUpdateEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			EventSource source) {
		super(
				collectionPersister,
				collection,
				source,
				getLoadedOwnerOrNull( collection, source ),
				getLoadedOwnerIdOrNull( collection, source )
		);
	}

	@Internal
	public PostCollectionUpdateEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object loadedOwner) {
		super( collectionPersister, collection, entityName, loadedOwner, id );
	}
}
