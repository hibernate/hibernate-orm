/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
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

	EvictVisitor(EventSource session) {
		super(session);
	}

	@Override
	Object processCollection(Object collection, CollectionType type) throws HibernateException {
		if (collection != null) {
			evictCollection(collection, type);
		}

		return null;
	}
	public void evictCollection(Object value, CollectionType type) {
		final Object pc;
		if ( type.hasHolder() ) {
			pc = getSession().getPersistenceContext().removeCollectionHolder(value);
		}
		else if ( value instanceof PersistentCollection ) {
			pc = value;
		}
		else {
			return; //EARLY EXIT!
		}

		PersistentCollection collection = (PersistentCollection) pc;
		if ( collection.unsetSession( getSession() ) ) {
			evictCollection(collection);
		}
	}

	private void evictCollection(PersistentCollection collection) {
		CollectionEntry ce = (CollectionEntry) getSession().getPersistenceContext().getCollectionEntries().remove(collection);
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting collection: %s",
					MessageHelper.collectionInfoString( ce.getLoadedPersister(),
							collection,
							ce.getLoadedKey(),
							getSession() ) );
		}
		if (ce.getLoadedPersister() != null && ce.getLoadedPersister().getBatchSize() > 1) {
			getSession().getPersistenceContext().getBatchFetchQueue().removeBatchLoadableCollection(ce);
		}
		if ( ce.getLoadedPersister() != null && ce.getLoadedKey() != null ) {
			//TODO: is this 100% correct?
			getSession().getPersistenceContext().getCollectionsByKey().remove(
					new CollectionKey( ce.getLoadedPersister(), ce.getLoadedKey() )
			);
		}
	}
}
