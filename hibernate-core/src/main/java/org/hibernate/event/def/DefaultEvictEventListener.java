/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.event.def;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.event.EventSource;
import org.hibernate.event.EvictEvent;
import org.hibernate.event.EvictEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Defines the default evict event listener used by hibernate for evicting entities
 * in response to generated flush events.  In particular, this implementation will
 * remove any hard references to the entity that are held by the infrastructure
 * (references held by application or other persistent instances are okay)
 *
 * @author Steve Ebersole
 */
public class DefaultEvictEventListener implements EvictEventListener {

	private static final Logger log = LoggerFactory.getLogger(DefaultEvictEventListener.class);

	/** 
	 * Handle the given evict event.
	 *
	 * @param event The evict event to be handled.
	 * @throws HibernateException
	 */
	public void onEvict(EvictEvent event) throws HibernateException {
		EventSource source = event.getSession();
		final Object object = event.getObject();
		final PersistenceContext persistenceContext = source.getPersistenceContext();

		if ( object instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			Serializable id = li.getIdentifier();
			EntityPersister persister = source.getFactory().getEntityPersister( li.getEntityName() );
			if ( id == null ) {
				throw new IllegalArgumentException("null identifier");
			}

			EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
			persistenceContext.removeProxy( key );

			if ( !li.isUninitialized() ) {
				final Object entity = persistenceContext.removeEntity( key );
				if ( entity != null ) {
					EntityEntry e = event.getSession().getPersistenceContext().removeEntry( entity );
					doEvict( entity, key, e.getPersister(), event.getSession() );
				}
			}
			li.unsetSession();
		}
		else {
			EntityEntry e = persistenceContext.removeEntry( object );
			if ( e != null ) {
				persistenceContext.removeEntity( e.getEntityKey() );
				doEvict( object, e.getEntityKey(), e.getPersister(), source );
			}
		}
	}

	protected void doEvict(
		final Object object, 
		final EntityKey key, 
		final EntityPersister persister,
		final EventSource session) 
	throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "evicting " + MessageHelper.infoString(persister) );
		}

		// remove all collections for the entity from the session-level cache
		if ( persister.hasCollections() ) {
			new EvictVisitor( session ).process( object, persister );
		}

		// remove any snapshot, not really for memory management purposes, but
		// rather because it might now be stale, and there is no longer any 
		// EntityEntry to take precedence
		// This is now handled by removeEntity()
		//session.getPersistenceContext().removeDatabaseSnapshot(key);

		new Cascade( CascadingAction.EVICT, Cascade.AFTER_EVICT, session )
				.cascade( persister, object );
	}
}
