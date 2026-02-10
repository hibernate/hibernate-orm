/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Internal;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * An event that occurs before a collection is recreated
 *
 * @author Gail Badner
 */
public class PreCollectionRecreateEvent extends AbstractCollectionEvent {

	@Internal
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

	@Internal
	public PreCollectionRecreateEvent(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object id,
			String entityName,
			Object loadedOwner) {
		super( collectionPersister, collection, entityName, loadedOwner, id );
	}
}
