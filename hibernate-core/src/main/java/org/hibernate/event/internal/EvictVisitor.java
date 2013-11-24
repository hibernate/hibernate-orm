/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;

import org.jboss.logging.Logger;

/**
 * Evict any collections referenced by the object from the session cache.
 * This will NOT pick up any collections that were dereferenced, so they
 * will be deleted (suboptimal but not exactly incorrect).
 *
 * @author Gavin King
 */
public class EvictVisitor extends AbstractVisitor {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, EvictVisitor.class.getName() );

	EvictVisitor(EventSource session) {
		super(session);
	}

	@Override
	Object processCollection(Object collection, CollectionType type)
		throws HibernateException {

		if (collection!=null) evictCollection(collection, type);

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
		if ( collection.unsetSession( getSession() ) ) evictCollection(collection);
	}

	private void evictCollection(PersistentCollection collection) {
		CollectionEntry ce = (CollectionEntry) getSession().getPersistenceContext().getCollectionEntries().remove(collection);
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Evicting collection: %s",
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
