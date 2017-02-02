/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EvictEvent;
import org.hibernate.event.spi.EvictEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
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
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultEvictEventListener.class );

	/**
	 * Handle the given evict event.
	 *
	 * @param event The evict event to be handled.
	 *
	 * @throws HibernateException
	 */
	public void onEvict(EvictEvent event) throws HibernateException {
		final Object object = event.getObject();
		if ( object == null ) {
			throw new NullPointerException( "null passed to Session.evict()" );
		}

		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContext();

		if ( object instanceof HibernateProxy ) {
			final LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			final Serializable id = li.getIdentifier();
			if ( id == null ) {
				throw new IllegalArgumentException( "Could not determine identifier of proxy passed to evict()" );
			}

			final EntityPersister persister = source.getFactory().getEntityPersister( li.getEntityName() );
			final EntityKey key = source.generateEntityKey( id, persister );
			persistenceContext.removeProxy( key );

			if ( !li.isUninitialized() ) {
				final Object entity = persistenceContext.removeEntity( key );
				if ( entity != null ) {
					EntityEntry e = persistenceContext.removeEntry( entity );
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
			else {
				// see if the passed object is even an entity, and if not throw an exception
				// 		this is different than legacy Hibernate behavior, but what JPA 2.1 is calling for
				//		with EntityManager.detach
				EntityPersister persister = null;
				final String entityName = persistenceContext.getSession().guessEntityName( object );
				if ( entityName != null ) {
					try {
						persister = persistenceContext.getSession().getFactory().getEntityPersister( entityName );
					}
					catch (Exception ignore) {
					}
				}
				if ( persister == null ) {
					throw new IllegalArgumentException( "Non-entity object instance passed to evict : " + object );
				}
			}
		}
	}

	protected void doEvict(
			final Object object,
			final EntityKey key,
			final EntityPersister persister,
			final EventSource session)
			throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Evicting {0}", MessageHelper.infoString( persister ) );
		}

		if ( persister.hasNaturalIdentifier() ) {
			session.getPersistenceContext().getNaturalIdHelper().handleEviction(
					object,
					persister,
					key.getIdentifier()
			);
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

		Cascade.cascade( CascadingActions.EVICT, CascadePoint.AFTER_EVICT, session, persister, object );
	}
}
