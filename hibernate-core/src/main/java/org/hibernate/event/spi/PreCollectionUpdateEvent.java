/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.Internal;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An event that occurs before a collection is updated
 *
 * @author Gail Badner
 */
public class PreCollectionUpdateEvent extends AbstractCollectionEvent {

	@Internal
	public PreCollectionUpdateEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nonnull EventSource source) {
		super(
				collectionPersister,
				collection,
				source,
				getLoadedOwnerOrNull( collection, source ),
				getLoadedOwnerIdOrNull( collection, source )
		);
	}

	@Internal
	public PreCollectionUpdateEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nullable Object id,
			@Nonnull String entityName,
			@Nullable Object loadedOwner) {
		super( collectionPersister, collection, entityName, loadedOwner, id );
	}
}
