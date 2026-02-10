/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Internal;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs after a collection is recreated
 *
 * @author Gail Badner
 */
public class PostCollectionRecreateEvent extends AbstractCollectionEvent {

	@Internal
	public PostCollectionRecreateEvent(
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

	@Internal
	public PostCollectionRecreateEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object loadedOwner) {
		super( collectionPersister, collection, entityName, loadedOwner, id );
	}
}
