/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * Evict any collections referenced by the object from the session cache.
 * This will NOT pick up any collections that were dereferenced, so they
 * will be deleted (suboptimal but not exactly incorrect).
 *
 * @author Gavin King
 */
public class EvictVisitor extends AbstractVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EvictVisitor.class );

	private final Object owner;

	public EvictVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

	@Override
	Object processCollection(Object entity, Object collection, CollectionType type) throws HibernateException {
		if ( collection != null ) {
			evictCollection( collection, type );
		}
		return null;
	}

	public void evictCollection(Object value, CollectionType type) {
		final EventSource session = getSession();
		final PersistentCollection<?> collection;
		if ( type.hasHolder() ) {
			collection = session.getPersistenceContextInternal().removeCollectionHolder( value );
		}
		else if ( value instanceof PersistentCollection<?> persistentCollection ) {
			collection = persistentCollection;
		}
		else if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			collection = (PersistentCollection<?>)
					type.getCollection( type.getKeyOfOwner( owner, session ), session, owner, false );
		}
		else {
			return; //EARLY EXIT!
		}

		if ( collection != null && collection.unsetSession( session ) ) {
			evictCollection( collection );
		}
	}

	private void evictCollection(PersistentCollection<?> collection) {
		final EventSource session = getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final CollectionEntry ce = persistenceContext.removeCollectionEntry( collection );
		final CollectionPersister persister = ce.getLoadedPersister();
		final Object loadedKey = ce.getLoadedKey();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting collection: %s",
					collectionInfoString( persister, collection, loadedKey, session )
			);
		}

		if ( persister != null ) {
			if ( session.getLoadQueryInfluencers().effectivelyBatchLoadable( persister ) ) {
				persistenceContext.getBatchFetchQueue().removeBatchLoadableCollection( ce );
			}
			if ( loadedKey != null ) {
				//TODO: is this 100% correct?
				persistenceContext.removeCollectionByKey( new CollectionKey( persister, loadedKey) );
			}
		}
	}

	@Override
	boolean includeEntityProperty(Object[] values, int i) {
		return true;
	}
}
