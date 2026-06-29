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
 * An event that occurs after a collection is updated
 *
 * @author Gail Badner
 */
public class PostCollectionUpdateEvent extends AbstractCollectionEvent {

	@Internal
	public PostCollectionUpdateEvent(
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
	public PostCollectionUpdateEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nonnull EventSource source,
			@Nullable Object affectedOwner) {
		super(
				collectionPersister,
				collection,
				source,
				affectedOwner,
				getOwnerIdOrNull( affectedOwner, source )
		);
	}

	@Internal
	public PostCollectionUpdateEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nonnull EventSource source,
			@Nullable Object affectedOwner,
			@Nullable Object affectedOwnerId) {
		super(
				collectionPersister,
				collection,
				source,
				affectedOwner,
				affectedOwnerId
		);
	}

	@Internal
	public PostCollectionUpdateEvent(
			@Nonnull CollectionPersister collectionPersister,
			@Nonnull PersistentCollection<?> collection,
			@Nullable Object id,
			@Nonnull String entityName,
			@Nullable Object loadedOwner) {
		super( collectionPersister, collection, entityName, loadedOwner, id );
	}
}
