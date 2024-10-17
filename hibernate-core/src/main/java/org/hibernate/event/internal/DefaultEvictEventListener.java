/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityHolder;
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
	 */
	@Override
	public void onEvict(EvictEvent event) throws HibernateException {
		final EventSource source = event.getSession();
		final PersistenceContext persistenceContext = source.getPersistenceContextInternal();
		final Object object = event.getObject();
		if ( object == null ) {
			throw new NullPointerException( "null passed to Session.evict()" );
		}
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( object );
		if ( lazyInitializer != null ) {
			final Object id = lazyInitializer.getInternalIdentifier();
			if ( id == null ) {
				throw new IllegalArgumentException( "Could not determine identifier of proxy passed to evict()" );
			}
			final EntityPersister persister = source.getFactory()
					.getMappingMetamodel()
					.getEntityDescriptor( lazyInitializer.getEntityName() );
			final EntityKey key = source.generateEntityKey( id, persister );
			final EntityHolder holder = persistenceContext.detachEntity( key );
			// if the entity has been evicted then its holder is null
			if ( holder != null && !lazyInitializer.isUninitialized() ) {
				final Object entity = holder.getEntity();
				if ( entity != null ) {
					EntityEntry entry = persistenceContext.removeEntry( entity );
					doEvict( entity, key, entry.getPersister(), event.getSession() );
				}
			}
			lazyInitializer.unsetSession();
		}
		else {
			EntityEntry entry = persistenceContext.getEntry( object );
			if ( entry != null ) {
				doEvict( object, entry.getEntityKey(), entry.getPersister(), source );
			}
			else {
				checkEntity( object, source );
			}
		}
	}

	/**
	 * Make sure the passed object is even an entity, and if not throw an exception.
	 * This is different to the legacy Hibernate behavior, but is what JPA 2.1
	 * requires with EntityManager.detach().
	 */
	private static void checkEntity(Object object, EventSource source) {
		String entityName = source.getSession().guessEntityName( object );
		if ( entityName != null ) {
			try {
				EntityPersister persister = source.getFactory().getMappingMetamodel()
						.getEntityDescriptor( entityName );
				if ( persister != null ) {
					return; //ALL GOOD
				}
			}
			catch (Exception ignore) {
			}
		}
		throw new IllegalArgumentException( "Non-entity object instance passed to evict: " + object);
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

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		if ( persister.hasNaturalIdentifier() ) {
			persistenceContext.getNaturalIdResolutions().handleEviction( key.getIdentifier(), object, persister );
		}

		// remove all collections for the entity from the session-level cache
		if ( persister.hasCollections() ) {
			new EvictVisitor( session, object ).process( object, persister );
		}

		// remove any snapshot, not really for memory management purposes, but
		// rather because it might now be stale, and there is no longer any
		// EntityEntry to take precedence
		// This is now handled by removeEntity()
		//session.getPersistenceContext().removeDatabaseSnapshot(key);
		
		persistenceContext.removeEntityHolder( key );
		persistenceContext.removeEntry( object );

		Cascade.cascade( CascadingActions.EVICT, CascadePoint.AFTER_EVICT, session, persister, object );
	}
}
