/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.PersistentObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.RefreshEventListener;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

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
		final EntityPersister persister;
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
			if ( persister.hasCollections() ) {
				new EvictVisitor( source ).process( object, persister );
			}
		}

		if ( persister.hasCache() ) {
			final EntityRegionAccessStrategy cache = persister.getCacheAccessStrategy();
			Object ck = cache.generateCacheKey(
					id,
					persister,
					source.getFactory(),
					source.getTenantIdentifier()
			);
			cache.evict( ck );
		}

		evictCachedCollections( persister, id, source.getFactory() );

		String previousFetchProfile = source.getLoadQueryInfluencers().getInternalFetchProfile();
		source.getLoadQueryInfluencers().setInternalFetchProfile( "refresh" );
		Object result = persister.load( id, object, event.getLockOptions(), source );
		// Keep the same read-only/modifiable setting for the entity that it had before refreshing;
		// If it was transient, then set it to the default for the source.
		if ( result != null ) {
			if ( !persister.isMutable() ) {
				// this is probably redundant; it should already be read-only
				source.setReadOnly( result, true );
			}
			else {
				source.setReadOnly( result, ( e == null ? source.isDefaultReadOnly() : e.isReadOnly() ) );
			}
		}
		source.getLoadQueryInfluencers().setInternalFetchProfile( previousFetchProfile );

		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );

	}

	private void evictCachedCollections(EntityPersister persister, Serializable id, SessionFactoryImplementor factory) {
		evictCachedCollections( persister.getPropertyTypes(), id, factory );
	}

	private void evictCachedCollections(Type[] types, Serializable id, SessionFactoryImplementor factory)
			throws HibernateException {
		for ( Type type : types ) {
			if ( type.isCollectionType() ) {
				factory.getCache().evictCollection( ( (CollectionType) type ).getRole(), id );
			}
			else if ( type.isComponentType() ) {
				CompositeType actype = (CompositeType) type;
				evictCachedCollections( actype.getSubtypes(), id, factory );
			}
		}
	}
}
