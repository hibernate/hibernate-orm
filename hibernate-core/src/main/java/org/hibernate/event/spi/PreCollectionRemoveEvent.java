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
 * An event that occurs before a collection is removed
 *
 * @author Gail Badner
 */
public class PreCollectionRemoveEvent extends AbstractCollectionEvent {

	@Internal
	public PreCollectionRemoveEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nonnull EventSource source,
			@Nullable Object loadedOwner) {
		super(
				collectionPersister,
				collection,
				source,
				loadedOwner,
				getOwnerIdOrNull( loadedOwner, source )
		);
	}

	@Internal
	public PreCollectionRemoveEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nullable Object id,
			@Nonnull String entityName,
			@Nullable Object loadedOwner) {
		super( collectionPersister, collection, entityName, loadedOwner, id );
	}
}
