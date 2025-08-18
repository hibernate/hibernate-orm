/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * If a collection is extra lazy and has queued ops, we still need to
 * process them.
 * <p>
 * For example, {@link org.hibernate.persister.collection.OneToManyPersister}
 * needs to insert indexes for lists.  See HHH-8083.
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
			final PersistentCollection<?> collection,
			final CollectionPersister persister,
			final Object id,
			final EventSource session) {
		super( persister, collection, id, session );
	}

	@Override
	public void execute() throws HibernateException {
		// this QueuedOperationCollectionAction has to be executed before any other
		// CollectionAction involving the same collection.

		getPersister().processQueuedOps( getCollection(), getKey(), getSession() );

		// TODO: It would be nice if this could be done safely by CollectionPersister#processQueuedOps;
		//       Can't change the SPI to do this though.
		final var collection = (AbstractPersistentCollection<?>) getCollection();
		collection.clearOperationQueue();

		// The other CollectionAction types call CollectionEntry#afterAction, which
		// clears the dirty flag. We don't want to call CollectionEntry#afterAction unless
		// there is no other CollectionAction that will be executed on the same collection.
		final var ce = getSession().getPersistenceContextInternal().getCollectionEntry( getCollection() );
		if ( !ce.isDoremove() && !ce.isDoupdate() && !ce.isDorecreate() ) {
			ce.afterAction( getCollection() );
		}
	}
}
