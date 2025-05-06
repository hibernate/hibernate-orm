/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.monitor.spi.DiagnosticEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.stat.spi.StatisticsImplementor;

/**
 * The action for recreating a collection
 */
public final class CollectionRecreateAction extends CollectionAction {

	/**
	 * Constructs a CollectionRecreateAction
	 *  @param collection The collection being recreated
	 * @param persister The collection persister
	 * @param id The collection key
	 * @param session The session
	 */
	public CollectionRecreateAction(
			final PersistentCollection<?> collection,
			final CollectionPersister persister,
			final Object id,
			final EventSource session) {
		super( persister, collection, id, session );
	}

	@Override
	public void execute() throws HibernateException {
		// this method is called when a new non-null collection is persisted
		// or when an existing (non-null) collection is moved to a new owner
		final PersistentCollection<?> collection = getCollection();
		preRecreate();
		final SharedSessionContractImplementor session = getSession();
		final CollectionPersister persister = getPersister();
		final Object key = getKey();
		final EventMonitor eventMonitor = session.getEventMonitor();
		final DiagnosticEvent event = eventMonitor.beginCollectionRecreateEvent();
		boolean success = false;
		try {
			persister.recreate( collection, key, session );
			success = true;
		}
		finally {
			eventMonitor.completeCollectionRecreateEvent( event, key, persister.getRole(), success, session );
		}

		session.getPersistenceContextInternal().getCollectionEntry( collection ).afterAction( collection );
		evict();
		postRecreate();

		final StatisticsImplementor statistics = session.getFactory().getStatistics();
		if ( statistics.isStatisticsEnabled() ) {
			statistics.recreateCollection( persister.getRole() );
		}
	}

	private void preRecreate() {
		getEventListenerGroups().eventListenerGroup_PRE_COLLECTION_RECREATE
				.fireLazyEventOnEachListener( this::newPreCollectionRecreateEvent,
						PreCollectionRecreateEventListener::onPreRecreateCollection );
	}

	private PreCollectionRecreateEvent newPreCollectionRecreateEvent() {
		return new PreCollectionRecreateEvent( getPersister(), getCollection(), eventSource() );
	}

	private void postRecreate() {
		getEventListenerGroups().eventListenerGroup_POST_COLLECTION_RECREATE
				.fireLazyEventOnEachListener( this::newPostCollectionRecreateEvent,
						PostCollectionRecreateEventListener::onPostRecreateCollection );
	}

	private PostCollectionRecreateEvent newPostCollectionRecreateEvent() {
		return new PostCollectionRecreateEvent( getPersister(), getCollection(), eventSource() );
	}
}
