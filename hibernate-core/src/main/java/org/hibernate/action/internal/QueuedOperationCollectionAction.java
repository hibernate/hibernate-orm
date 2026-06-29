/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * If a collection has queued ops, we still need to process them.
 * <p>
 * For example, {@link org.hibernate.persister.collection.OneToManyPersister}
 * needs to insert indexes for lists.  See HHH-8083.
 *
 * @see CollectionPersister#decompose(QueuedOperationCollectionAction, int, org.hibernate.engine.spi.SharedSessionContractImplementor).
 *
 * @author Brett Meyer
 */
public final class QueuedOperationCollectionAction extends CollectionAction {

	/**
	 * Constructs a CollectionUpdateAction
	 *  @param collection The collection to update
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param session The session
	 */
	public QueuedOperationCollectionAction(
			final @Nonnull PersistentCollection<?> collection,
			final @Nonnull CollectionPersister persister,
			final @Nonnull Object id,
			final @Nonnull EventSource session) {
		super( persister, collection, id, session );
		assert collection != null;
	}

	@Override
	@Nonnull
	public PersistentCollection<?> getCollection() {
		final var collection = super.getCollection();
		assert collection != null;
		return collection;
	}

	@Override
	public void execute() {
		// this QueuedOperationCollectionAction has to be executed before any other
		// CollectionAction involving the same collection.
		getPersister().processQueuedOps( getCollection(), getKey(), getSession() );
		afterQueuedOperationsProcessed();
	}

	public void afterQueuedOperationsProcessed() {
		// TODO: It would be nice if this could be done safely by CollectionPersister#processQueuedOps;
		//       Can't change the SPI to do this though.
		final var collection = (AbstractPersistentCollection<?>) getCollection();
		collection.clearOperationQueue();

			// The other CollectionAction types call CollectionEntry#afterAction, which
			// clears the dirty flag. We don't want to call CollectionEntry#afterAction unless
			// there is no other CollectionAction that will be executed on the same collection.
			final var collectionEntry =
					getSession().getPersistenceContextInternal()
							.getCollectionEntry( getCollection() );
			final var collectionFlushActionTracker =
					getSession().getPersistenceContextInternal()
							.getCollectionFlushActionTracker();
			if ( collectionFlushActionTracker == null
					|| !collectionFlushActionTracker.hasQueuedCollectionAction( getCollection() ) ) {
				collectionEntry.afterAction( getCollection() );
			}
		}
	}
