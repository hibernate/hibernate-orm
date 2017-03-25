/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionAction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.boot.Metadata;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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

	/**
	 * Exposed for use in testing
	 */
	public static boolean PROPAGATE_EXCEPTION = false;

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
		if ( !sessionFactory.getSessionFactoryOptions().isAutoEvictCollectionCache() ) {
			// feature is disabled
			return;
		}
		if ( !sessionFactory.getSessionFactoryOptions().isSecondLevelCacheEnabled() ) {
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

			Set<String> collectionRoles = factory.getMetamodel().getCollectionRolesByEntityParticipant( persister.getEntityName() );
			if ( collectionRoles == null || collectionRoles.isEmpty() ) {
				return;
			}
			for ( String role : collectionRoles ) {
				final CollectionPersister collectionPersister = factory.getMetamodel().collectionPersister( role );
				if ( !collectionPersister.hasCache() ) {
					// ignore collection if no caching is used
					continue;
				}
				// this is the property this OneToMany relation is mapped by
				String mappedBy = collectionPersister.getMappedByProperty();
				if ( !collectionPersister.isManyToMany() &&
						mappedBy != null && !mappedBy.isEmpty() ) {
					int i = persister.getEntityMetamodel().getPropertyIndex( mappedBy );
					Serializable oldId = null;
					if ( oldState != null ) {
						// in case of updating an entity we perhaps have to decache 2 entity collections, this is the
						// old one
						oldId = getIdentifier( session, oldState[i] );
					}
					Object ref = persister.getPropertyValue( entity, i );
					Serializable id = getIdentifier( session, ref );

					// only evict if the related entity has changed
					if ( ( id != null && !id.equals( oldId ) ) || ( oldId != null && !oldId.equals( id ) ) ) {
						if ( id != null ) {
							evict( id, collectionPersister, session );
						}
						if ( oldId != null ) {
							evict( oldId, collectionPersister, session );
						}
					}
				}
				else {
					LOG.debug( "Evict CollectionRegion " + role );
					final SoftLock softLock = collectionPersister.getCacheAccessStrategy().lockRegion();
					session.getActionQueue().registerProcess( (success, session1) -> {
						collectionPersister.getCacheAccessStrategy().unlockRegion( softLock );
					} );
				}
			}
		}
		catch ( Exception e ) {
			if ( PROPAGATE_EXCEPTION ) {
				throw new IllegalStateException( e );
			}
			// don't let decaching influence other logic
			LOG.error( "", e );
		}
	}

	private Serializable getIdentifier(EventSource session, Object obj) {
		Serializable id = null;
		if ( obj != null ) {
			id = session.getContextEntityIdentifier( obj );
			if ( id == null ) {
				id = session.getSessionFactory().getMetamodel().entityPersister( obj.getClass() ).getIdentifier( obj, session );
			}
		}
		return id;
	}

	private void evict(Serializable id, CollectionPersister collectionPersister, EventSource session) {
		if ( LOG.isDebugEnabled() ) {
			LOG.debug( "Evict CollectionRegion " + collectionPersister.getRole() + " for id " + id );
		}
		AfterTransactionCompletionProcess afterTransactionProcess = new CollectionEvictCacheAction(
				collectionPersister,
				null,
				id,
				session
		).lockCache();
		session.getActionQueue().registerProcess( afterTransactionProcess );
	}

	//execute the same process as invalidation with collection operations
	private static final class CollectionEvictCacheAction extends CollectionAction {
		protected CollectionEvictCacheAction(
				CollectionPersister persister,
				PersistentCollection collection,
				Serializable key,
				SharedSessionContractImplementor session) {
			super( persister, collection, key, session );
		}

		@Override
		public void execute() throws HibernateException {
		}

		public AfterTransactionCompletionProcess lockCache() {
			beforeExecutions();
			return getAfterTransactionCompletionProcess();
		}
	}
}
