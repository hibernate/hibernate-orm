/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.internal;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionAction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
import org.jboss.logging.Logger;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * Allows the collection cache to be automatically evicted if an element is inserted/removed/updated *without* properly
 * managing both sides of the association (ie, the ManyToOne collection is changed w/o properly managing the OneToMany).
 * <p>
 * For this functionality to be used, {@value org.hibernate.cfg.AvailableSettings#AUTO_EVICT_COLLECTION_CACHE} must be
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
	public void integrate(
			Metadata metadata,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		integrate( sessionFactory );
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		if ( event.getSession() instanceof EventSource eventSource ) {
			evictCache( event.getEntity(), event.getPersister(), eventSource, null );
		}
	}

	@Override
	public boolean requiresPostCommitHandling(EntityPersister persister) {
		return true;
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		if ( event.getSession() instanceof EventSource eventSource ) {
			evictCache( event.getEntity(), event.getPersister(), eventSource, null );
		}
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		if ( event.getSession() instanceof EventSource eventSource ) {
			evictCache( event.getEntity(), event.getPersister(), eventSource, event.getOldState() );
		}
	}

	private void integrate(SessionFactoryImplementor sessionFactory) {
		final var options = sessionFactory.getSessionFactoryOptions();
		if ( options.isSecondLevelCacheEnabled()
				&& options.isAutoEvictCollectionCache() ) {
			final var eventListenerRegistry = sessionFactory.getEventListenerRegistry();
			eventListenerRegistry.appendListeners( EventType.POST_INSERT, this );
			eventListenerRegistry.appendListeners( EventType.POST_DELETE, this );
			eventListenerRegistry.appendListeners( EventType.POST_UPDATE, this );
		}
	}

	private void evictCache(Object entity, EntityPersister persister, EventSource session, Object[] oldState) {
		try {
			final var metamodel = persister.getFactory().getMappingMetamodel();
			final var roles = metamodel.getCollectionRolesByEntityParticipant( persister.getEntityName() );
			if ( !isEmpty( roles ) ) {
				for ( String role : roles ) {
					evictCollection( entity, persister, oldState, metamodel.getCollectionDescriptor( role ), session );
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

	private void evictCollection(
			Object entity,
			EntityPersister persister,
			Object[] oldState,
			CollectionPersister collectionPersister,
			EventSource session) {
		if ( collectionPersister.hasCache() ) { // ignore collection if no caching is used
			if ( isInverseOneToMany( collectionPersister ) ) {
				handleInverseOneToMany( entity, persister, oldState, collectionPersister, session );
			}
			else {
				if ( L2CACHE_LOGGER.isTraceEnabled() ) {
					L2CACHE_LOGGER.autoEvictingCollectionCacheByRole( collectionPersister.getRole() );
				}
				final var cacheAccessStrategy = collectionPersister.getCacheAccessStrategy();
				final var softLock = cacheAccessStrategy.lockRegion();
				session.getActionQueue()
						.registerCallback( (success, s) -> cacheAccessStrategy.unlockRegion( softLock ) );
			}
		}
	}

	private void handleInverseOneToMany(
			Object entity,
			EntityPersister persister,
			Object[] oldState,
			CollectionPersister collectionPersister,
			EventSource session) {
		// this is the property this OneToMany relation is mapped by
		final int propertyIndex = persister.getPropertyIndex( collectionPersister.getMappedByProperty() );
		// in case of updating an entity, we might need to decache two entity collections
		final Object oldId = oldState == null ? null : getIdentifier( session, oldState[propertyIndex] );
		final Object currentId = getIdentifier( session, persister.getValue( entity, propertyIndex ) );
		// only evict if the related entity has changed
		if ( (currentId != null || oldId != null)
				&& !collectionPersister.getKeyType().isEqual( oldId, currentId ) ) {
			if ( currentId != null ) {
				evict( currentId, collectionPersister, session );
			}
			if ( oldId != null ) {
				evict( oldId, collectionPersister, session );
			}
		}
	}

	private static boolean isInverseOneToMany(CollectionPersister collectionPersister) {
		return collectionPersister.isOneToMany()
			&& !isEmpty( collectionPersister.getMappedByProperty() );
	}

	private Object getIdentifier(SharedSessionContractImplementor session, Object object) {
		if ( object != null ) {
			final Object id = session.getContextEntityIdentifier( object );
			if ( id == null ) {
				return session.getFactory().getMappingMetamodel()
						.getEntityDescriptor( object.getClass() )
						.getIdentifier( object, session );
			}
			else {
				return id;
			}
		}
		else {
			return null;
		}
	}

	private void evict(Object id, CollectionPersister collectionPersister, EventSource session) {
		if ( L2CACHE_LOGGER.isTraceEnabled() ) {
			L2CACHE_LOGGER.autoEvictingCollectionCache(
					collectionInfoString( collectionPersister, id, collectionPersister.getFactory() ) );
		}
		final var evictCacheAction =
				new CollectionEvictCacheAction( collectionPersister, null, id, session );
		evictCacheAction.execute();
		session.getActionQueue().registerCallback( evictCacheAction.getAfterTransactionCompletionProcess() );
	}

	//execute the same process as invalidation with collection operations
	private static final class CollectionEvictCacheAction extends CollectionAction {
		CollectionEvictCacheAction(
				CollectionPersister persister,
				PersistentCollection<?> collection,
				Object key,
				EventSource session) {
			super( persister, collection, key, session );
		}

		@Override
		public void execute() throws HibernateException {
			beforeExecutions();
			evict();
		}
	}

}
