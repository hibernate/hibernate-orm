/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PersistentObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.pretty.MessageHelper;

/**
 * Defines the default refresh event listener used by hibernate for refreshing entities
 * in response to generated refresh events.
 *
 * @author Steve Ebersole
 */
public class DefaultRefreshEventListener implements RefreshEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultRefreshEventListener.class );

	public void onRefresh(RefreshEvent event) throws HibernateException {
		onRefresh( event, new IdentityHashMap( 10 ) );
	}

	/**
	 * Handle the given refresh event.
	 *
	 * @param event The refresh event to be handled.
	 */
	public void onRefresh(RefreshEvent event, Map refreshedAlready) {

		final EventSource source = event.getSession();
		boolean isTransient;
		if ( event.getEntityName() != null ) {
			isTransient = !source.contains( event.getEntityName(), event.getObject() );
		}
		else {
			isTransient = !source.contains( event.getObject() );
		}
		if ( source.getPersistenceContext().reassociateIfUninitializedProxy( event.getObject() ) ) {
			if ( isTransient ) {
				source.setReadOnly( event.getObject(), source.isDefaultReadOnly() );
			}
			return;
		}

		final Object object = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );

		if ( refreshedAlready.containsKey( object ) ) {
			LOG.trace( "Already refreshed" );
			return;
		}

		final EntityEntry e = source.getPersistenceContext().getEntry( object );
		final EntityTypeDescriptor entityDescriptor;
		final Object id;

		if ( e == null ) {
			entityDescriptor = source.getEntityDescriptor(
					event.getEntityName(),
					object
			); //refresh() does not pass an entityName
			id = entityDescriptor.getIdentifier( object, event.getSession() );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Refreshing transient {0}", MessageHelper.infoString(
						entityDescriptor,
						id,
						source.getFactory()
				)
				);
			}
			final EntityKey key = source.generateEntityKey( id, entityDescriptor );
			if ( source.getPersistenceContext().getEntry( key ) != null ) {
				throw new PersistentObjectException(
						"attempted to refresh transient instance when persistent instance was already associated with the Session: " +
								MessageHelper.infoString( entityDescriptor, id, source.getFactory() )
				);
			}
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Refreshing ", MessageHelper.infoString(
						e.getDescriptor(),
						e.getId(),
						source.getFactory()
				)
				);
			}
			if ( !e.isExistsInDatabase() ) {
				throw new UnresolvableObjectException(
						e.getId(),
						"this instance does not yet exist as a row in the database"
				);
			}

			entityDescriptor = e.getDescriptor();
			id = e.getId();
		}

		// cascade the refresh prior to refreshing this entity
		refreshedAlready.put( object, object );
		Cascade.cascade(
				CascadingActions.REFRESH,
				CascadePoint.BEFORE_REFRESH,
				source,
				entityDescriptor,
				object,
				refreshedAlready
		);

		if ( e != null ) {
			final EntityKey key = source.generateEntityKey( id, entityDescriptor );
			source.getPersistenceContext().removeEntity( key );
			if ( entityDescriptor.getHierarchy().getMutabilityPlan().isMutable() ) {
				new EvictVisitor( source, object ).process( object, entityDescriptor );
			}
		}

		if ( entityDescriptor.canWriteToCache() ) {
			Object previousVersion = null;
			if ( entityDescriptor.isVersionPropertyGenerated() ) {
				// we need to grab the version value from the entity, otherwise
				// we have issues with generated-version entities that may have
				// multiple actions queued during the same flush
				previousVersion = entityDescriptor.getVersion( object );
			}
			final EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
			final Object ck = cacheAccess.generateCacheKey(
					id,
					entityDescriptor.getHierarchy(),
					source.getFactory(),
					source.getTenantIdentifier()
			);
			final SoftLock lock = cacheAccess.lockItem( source, ck, previousVersion );
			source.getActionQueue().registerProcess( (success, session) -> cacheAccess.unlockItem( session, ck, lock ) );
			cacheAccess.remove( source, ck );
		}

		evictCachedCollections( entityDescriptor, id, source );

		final InternalFetchProfileType previouslyEnabledInternalFetchProfileType =
				source.getLoadQueryInfluencers().getEnabledInternalFetchProfileType();
		source.getLoadQueryInfluencers().setEnabledInternalFetchProfileType( InternalFetchProfileType.REFRESH );


		// Handle the requested lock-mode (if one) in relation to the entry's (if one) current lock-mode

		LockOptions lockOptionsToUse = event.getLockOptions();

		final LockMode requestedLockMode = lockOptionsToUse.getLockMode();
		LockMode postRefreshLockMode = null;

		if ( e != null ) {
			final LockMode currentLockMode = e.getLockMode();
			if ( currentLockMode.greaterThan( requestedLockMode ) ) {
				// the requested lock-mode is less restrictive than the current one
				//		- pass along the current lock-mode (after accounting for WRITE)
				lockOptionsToUse = LockOptions.copy( event.getLockOptions(), new LockOptions() );
				if ( currentLockMode == LockMode.WRITE ||
						currentLockMode == LockMode.PESSIMISTIC_WRITE ||
						currentLockMode == LockMode.PESSIMISTIC_READ ) {
					// our transaction should already hold the exclusive lock on
					// the underlying row - so READ should be sufficient.
					//
					// in fact, this really holds true for any current lock-mode that indicates we
					// hold an exclusive lock on the underlying row - but we *need* to handle
					// WRITE specially because the Loader/Locker mechanism does not allow for WRITE
					// locks
					lockOptionsToUse.setLockMode( LockMode.READ );

					// and prepare to reset the entry lock-mode to the previous lock mode after
					// the refresh completes
					postRefreshLockMode = currentLockMode;
				}
				else {
					lockOptionsToUse.setLockMode( currentLockMode );
				}
			}
		}

		final Object result = entityDescriptor.getSingleIdLoader().load( id, lockOptionsToUse, source );

		if ( result != null ) {
			// apply `postRefreshLockMode`, if needed
			if ( postRefreshLockMode != null ) {
				// if we get here, there was a previous entry and we need to re-set its lock-mode
				//		- however, the refresh operation actually creates a new entry, so get it
				source.getPersistenceContext().getEntry( result ).setLockMode( postRefreshLockMode );
			}

			// Keep the same read-only/modifiable setting for the entity that it had before refreshing;
			// If it was transient, then set it to the default for the source.
			if ( !entityDescriptor.getHierarchy().getMutabilityPlan().isMutable() ) {
				// this is probably redundant; it should already be read-only
				source.setReadOnly( result, true );
			}
			else {
				source.setReadOnly( result, ( e == null ? source.isDefaultReadOnly() : e.isReadOnly() ) );
			}
		}

		UnresolvableObjectException.throwIfNull( result, id, entityDescriptor.getEntityName() );
	}

	private void evictCachedCollections(EntityTypeDescriptor entityDescriptor, Object id, EventSource source) {
		evictCachedCollections( entityDescriptor.getPersistentAttributes(), id, source );
	}

	@SuppressWarnings("unchecked")
	private void evictCachedCollections(List<PersistentAttributeDescriptor> persistentAttributes, Object id, EventSource source)
			throws HibernateException {
		for ( PersistentAttributeDescriptor attribute : persistentAttributes ) {
			if ( PluralPersistentAttribute.class.isInstance( attribute ) ) {
				final PersistentCollectionDescriptor collectionDescriptor = ( (PluralPersistentAttribute) attribute ).getPersistentCollectionDescriptor();

				if ( collectionDescriptor.hasCache() ) {
					final CollectionDataAccess cache = collectionDescriptor.getCacheAccess();
					final Object ck = cache.generateCacheKey(
							id,
							collectionDescriptor,
							source.getFactory(),
							source.getTenantIdentifier()
					);
					final SoftLock lock = cache.lockItem( source, ck, null );
					source.getActionQueue().registerProcess( (success, session) -> cache.unlockItem( session, ck, lock ) );
					cache.remove( source, ck );
				}
			}
			else if ( EmbeddedValuedNavigable.class.isInstance( attribute ) ) {
				EmbeddedValuedNavigable composite = (EmbeddedValuedNavigable) attribute;
				evictCachedCollections( composite.getEmbeddedDescriptor().getPersistentAttributes(), id, source );
			}
		}
	}
}
