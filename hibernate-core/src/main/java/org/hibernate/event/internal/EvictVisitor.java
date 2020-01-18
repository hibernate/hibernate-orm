/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;

/**
 * Evict any collections referenced by the object from the session cache.
 * This will NOT pick up any collections that were dereferenced, so they
 * will be deleted (suboptimal but not exactly incorrect).
 *
 * @author Gavin King
 */
public class EvictVisitor extends AbstractVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EvictVisitor.class );
	
	private Object owner;

	public EvictVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

	@Override
	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if (collection != null) {
			evictCollection(collection, type);
		}

		return null;
	}
	
	public void evictCollection(Object value, CollectionType type) {
		final PersistentCollection collection;
		final EventSource session = getSession();
		if ( type.hasHolder() ) {
			collection = session.getPersistenceContextInternal().removeCollectionHolder(value);
		}
		else if ( value instanceof PersistentCollection ) {
			collection = (PersistentCollection) value;
		}
		else if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			collection = (PersistentCollection) type.resolve( value, session, this.owner );
		}
		else {
			return; //EARLY EXIT!
		}

		if ( collection != null && collection.unsetSession(session) ) {
			evictCollection(collection);
		}
	}

	private void evictCollection(PersistentCollection collection) {
		final PersistenceContext persistenceContext = getSession().getPersistenceContextInternal();
		CollectionEntry ce = persistenceContext.removeCollectionEntry( collection );
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting collection: %s",
					MessageHelper.collectionInfoString( ce.getLoadedPersister(),
							collection,
							ce.getLoadedKey(),
							getSession() ) );
		}
		if (ce.getLoadedPersister() != null && ce.getLoadedPersister().getBatchSize() > 1) {
			persistenceContext.getBatchFetchQueue().removeBatchLoadableCollection(ce);
		}
		if ( ce.getLoadedPersister() != null && ce.getLoadedKey() != null ) {
			//TODO: is this 100% correct?
			persistenceContext.removeCollectionByKey( new CollectionKey( ce.getLoadedPersister(), ce.getLoadedKey() ) );
		}
	}
	
	@Override
	boolean includeEntityProperty(Object[] values, int i) {
		return true;
	}
}
