/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
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
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
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

		boolean isTransient = !source.contains( event.getObject() );
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
		final EntityDescriptor persister;
		final Serializable id;

		if ( e == null ) {
			persister = source.getEntityPersister(
					event.getEntityName(),
					object
			); //refresh() does not pass an entityName
			id = persister.getIdentifier( object, event.getSession() );
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Refreshing transient {0}", MessageHelper.infoString(
						persister,
						id,
						source.getFactory()
				)
				);
			}
			final EntityKey key = source.generateEntityKey( id, persister );
			if ( source.getPersistenceContext().getEntry( key ) != null ) {
				throw new PersistentObjectException(
						"attempted to refresh transient instance when persistent instance was already associated with the Session: " +
								MessageHelper.infoString( persister, id, source.getFactory() )
				);
			}
		}
		else {
			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Refreshing ", MessageHelper.infoString(
						e.getPersister(),
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

			persister = e.getPersister();
			id = e.getId();
		}

		// cascade the refresh prior to refreshing this entity
		refreshedAlready.put( object, object );
		Cascade.cascade(
				CascadingActions.REFRESH,
				CascadePoint.BEFORE_REFRESH,
				source,
				persister,
				object,
				refreshedAlready
		);

		if ( e != null ) {
			final EntityKey key = source.generateEntityKey( id, persister );
			source.getPersistenceContext().removeEntity( key );
			if ( persister.getHierarchy().isMutable() ) {
				new EvictVisitor( source ).process( object, persister );
			}
		}

		final EntityDataAccess cacheAccess = persister.getHierarchy().getEntityCacheAccess();

		if ( cacheAccess != null ) {
			Object previousVersion = null;
			if ( persister.isVersionPropertyGenerated() ) {
				// we need to grab the version value from the entity, otherwise
				// we have issues with generated-version entities that may have
				// multiple actions queued during the same flush
				previousVersion = persister.getVersion( object );
			}
			final Object ck = cacheAccess.generateCacheKey(
					id,
					persister.getHierarchy(),
					source.getFactory(),
					source.getTenantIdentifier()
			);
			final SoftLock lock = cacheAccess.lockItem( source, ck, previousVersion );
			source.getActionQueue().registerProcess(
					(success, session) -> cacheAccess.unlockItem( session, ck, lock )
			);
			cacheAccess.remove( source, ck );
		}

		evictCachedCollections( persister, id, source );

		final InternalFetchProfileType previouslyEnabledInternalFetchProfileType =
				source.getLoadQueryInfluencers().getEnabledInternalFetchProfileType();
		source.getLoadQueryInfluencers().setEnabledInternalFetchProfileType( InternalFetchProfileType.REFRESH );

		final Object result;
		try {
			result = persister.load( id, object, event.getLockOptions(), source );
		}
		finally {
			source.getLoadQueryInfluencers().setEnabledInternalFetchProfileType( previouslyEnabledInternalFetchProfileType );
		}

		// Keep the same read-only/modifiable setting for the entity that it had before refreshing;
		// If it was transient, then set it to the default for the source.
		if ( result != null ) {
			if ( !persister.getHierarchy().isMutable() ) {
				// this is probably redundant; it should already be read-only
				source.setReadOnly( result, true );
			}
			else {
				source.setReadOnly( result, ( e == null ? source.isDefaultReadOnly() : e.isReadOnly() ) );
			}
		}

		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
	}

	private void evictCachedCollections(EntityDescriptor entityDescriptor, Serializable id, EventSource source) {
		evictCachedCollections( entityDescriptor.getPersistentAttributes(), id, source );
	}

	@SuppressWarnings("unchecked")
	private void evictCachedCollections(List<PersistentAttribute> persistentAttributes, Serializable id, EventSource source)
			throws HibernateException {
		for ( PersistentAttribute attribute : persistentAttributes ) {
			if ( PluralPersistentAttribute.class.isInstance( attribute ) ) {
				final PersistentCollectionDescriptor collectionPersister = ( (PluralPersistentAttribute) attribute ).getPersistentCollectionDescriptor();
				final CollectionDataAccess cacheAccess = source.getFactory().getCache()
						.getCollectionRegionAccess( collectionPersister );
				if ( cacheAccess != null ) {
					final Object ck = cacheAccess.generateCacheKey(
						id,
						collectionPersister,
						source.getFactory(),
						source.getTenantIdentifier()
					);
					final SoftLock lock = cacheAccess.lockItem( source, ck, null );
					source.getActionQueue().registerProcess(
							(success, session) -> cacheAccess.unlockItem( session, ck, lock )
					);
					cacheAccess.remove( source, ck );
				}
			}
			else if ( EmbeddedValuedNavigable.class.isInstance( attribute ) ) {
				EmbeddedValuedNavigable composite = (EmbeddedValuedNavigable) attribute;
				evictCachedCollections( composite.getEmbeddedDescriptor().getPersistentAttributes(), id, source );
			}
		}
	}
}
