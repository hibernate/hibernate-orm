/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Allows the collection cache to be automatically evicted if an element is inserted/removed/updated *without* properly
 * managing both sides of the association (ie, the ManyToOne collection is changed w/o properly managing the OneToMany).
 * 
 * For this functionality to be used, {@link org.hibernate.cfg.AvailableSettings#AUTO_EVICT_COLLECTION_CACHE} must be
 * enabled.  For performance reasons, it's disabled by default.
 * 
 * @author Andreas Berger
 */
public class CollectionCacheInvalidator
		implements Integrator, PostInsertEventListener, PostDeleteEventListener, PostUpdateEventListener {
	private static final Logger LOG = Logger.getLogger( CollectionCacheInvalidator.class.getName() );

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		integrate( serviceRegistry, sessionFactory );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		evictCache( event.getEntity(), event.getPersister(), event.getSession(), null );
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return true;
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		evictCache( event.getEntity(), event.getPersister(), event.getSession(), null );
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		evictCache( event.getEntity(), event.getPersister(), event.getSession(), event.getOldState() );
	}

	private void integrate(SessionFactoryServiceRegistry serviceRegistry, SessionFactoryImplementor sessionFactory) {
		if ( !sessionFactory.getSettings().isAutoEvictCollectionCache() ) {
			// feature is disabled
			return;
		}
		if ( !sessionFactory.getSettings().isSecondLevelCacheEnabled() ) {
			// Nothing to do, if caching is disabled
			return;
		}
		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		eventListenerRegistry.appendListeners( EventType.POST_INSERT, this );
		eventListenerRegistry.appendListeners( EventType.POST_DELETE, this );
		eventListenerRegistry.appendListeners( EventType.POST_UPDATE, this );
	}

	private void evictCache(Object entity, EntityPersister persister, EventSource session, Object[] oldState) {
		try {
			SessionFactoryImplementor factory = persister.getFactory();

			Set<String> collectionRoles = factory.getCollectionRolesByEntityParticipant( persister.getEntityName() );
			if ( collectionRoles == null || collectionRoles.isEmpty() ) {
				return;
			}
			for ( String role : collectionRoles ) {
				CollectionPersister collectionPersister = factory.getCollectionPersister( role );
				if ( !collectionPersister.hasCache() ) {
					// ignore collection if no caching is used
					continue;
				}
				// this is the property this OneToMany relation is mapped by
				String mappedBy = collectionPersister.getMappedByProperty();
				if ( mappedBy != null ) {
					int i = persister.getEntityMetamodel().getPropertyIndex( mappedBy );
					Serializable oldId = null;
					if ( oldState != null ) {
						// in case of updating an entity we perhaps have to decache 2 entity collections, this is the
						// old one
						oldId = session.getIdentifier( oldState[i] );
					}
					Object ref = persister.getPropertyValue( entity, i );
					Serializable id = null;
					if ( ref != null ) {
						id = session.getIdentifier( ref );
					}
					// only evict if the related entity has changed
					if ( id != null && !id.equals( oldId ) ) {
						evict( id, collectionPersister, session );
						if ( oldId != null ) {
							evict( oldId, collectionPersister, session );
						}
					}
				}
				else {
					LOG.debug( "Evict CollectionRegion " + role );
					collectionPersister.getCacheAccessStrategy().evictAll();
				}
			}
		}
		catch ( Exception e ) {
			// don't let decaching influence other logic
			LOG.error( "", e );
		}
	}

	private void evict(Serializable id, CollectionPersister collectionPersister, EventSource session) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( "Evict CollectionRegion " + collectionPersister.getRole() + " for id " + id );
		}
		CollectionRegionAccessStrategy cache = collectionPersister.getCacheAccessStrategy();
		Object key = cache.generateCacheKey(
				id,
				collectionPersister,
				session.getFactory(),
				session.getTenantIdentifier()
		);
		cache.evict( key );
	}
}
