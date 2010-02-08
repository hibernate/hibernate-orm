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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.PersistentObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.CacheKey;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.event.RefreshEvent;
import org.hibernate.event.RefreshEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.util.IdentityMap;

/**
 * Defines the default refresh event listener used by hibernate for refreshing entities
 * in response to generated refresh events.
 *
 * @author Steve Ebersole
 */
public class DefaultRefreshEventListener implements RefreshEventListener {

	private static final Logger log = LoggerFactory.getLogger(DefaultRefreshEventListener.class);
	
	public void onRefresh(RefreshEvent event) throws HibernateException {
		onRefresh( event, IdentityMap.instantiate(10) );
	}

	/** 
	 * Handle the given refresh event.
	 *
	 * @param event The refresh event to be handled.
	 */
	public void onRefresh(RefreshEvent event, Map refreshedAlready) {

		final EventSource source = event.getSession();
		
		boolean isTransient = ! source.contains( event.getObject() );
		if ( source.getPersistenceContext().reassociateIfUninitializedProxy( event.getObject() ) ) {
			if ( isTransient ) {
				source.setReadOnly( event.getObject(), source.isDefaultReadOnly() );
			}
			return;
		}

		final Object object = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );

		if ( refreshedAlready.containsKey(object) ) {
			log.trace("already refreshed");
			return;
		}

		final EntityEntry e = source.getPersistenceContext().getEntry( object );
		final EntityPersister persister;
		final Serializable id;
		
		if ( e == null ) {
			persister = source.getEntityPersister(null, object); //refresh() does not pass an entityName
			id = persister.getIdentifier( object, event.getSession() );
			if ( log.isTraceEnabled() ) {
				log.trace(
						"refreshing transient " +
						MessageHelper.infoString( persister, id, source.getFactory() )
					);
			}
			EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
			if ( source.getPersistenceContext().getEntry(key) != null ) {
				throw new PersistentObjectException(
						"attempted to refresh transient instance when persistent instance was already associated with the Session: " +
						MessageHelper.infoString(persister, id, source.getFactory() )
					);
			}
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.trace(
						"refreshing " +
						MessageHelper.infoString( e.getPersister(), e.getId(), source.getFactory()  )
					);
			}
			if ( !e.isExistsInDatabase() ) {
				throw new HibernateException( "this instance does not yet exist as a row in the database" );
			}

			persister = e.getPersister();
			id = e.getId();
		}

		// cascade the refresh prior to refreshing this entity
		refreshedAlready.put(object, object);
		new Cascade(CascadingAction.REFRESH, Cascade.BEFORE_REFRESH, source)
				.cascade( persister, object, refreshedAlready );

		if ( e != null ) {
			EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
			source.getPersistenceContext().removeEntity(key);
			if ( persister.hasCollections() ) new EvictVisitor( source ).process(object, persister);
		}

		if ( persister.hasCache() ) {
			final CacheKey ck = new CacheKey(
					id,
					persister.getIdentifierType(),
					persister.getRootEntityName(),
					source.getEntityMode(), 
					source.getFactory()
			);
			persister.getCacheAccessStrategy().evict( ck );
		}
		
		evictCachedCollections( persister, id, source.getFactory() );
		
		String previousFetchProfile = source.getFetchProfile();
		source.setFetchProfile("refresh");
		Object result = persister.load( id, object, event.getLockOptions(), source );
		// Keep the same read-only/modifiable setting for the entity that it had before refreshing;
		// If it was transient, then set it to the default for the source.
		if ( result != null ) {
			source.setReadOnly( result, ( e == null ? source.isDefaultReadOnly() : e.isReadOnly() ) );
		}
		source.setFetchProfile(previousFetchProfile);
		
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );

	}

	private void evictCachedCollections(EntityPersister persister, Serializable id, SessionFactoryImplementor factory) {
		evictCachedCollections( persister.getPropertyTypes(), id, factory );
	}

	private void evictCachedCollections(Type[] types, Serializable id, SessionFactoryImplementor factory)
	throws HibernateException {
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i].isCollectionType() ) {
				factory.evictCollection( ( (CollectionType) types[i] ).getRole(), id );
			}
			else if ( types[i].isComponentType() ) {
				AbstractComponentType actype = (AbstractComponentType) types[i];
				evictCachedCollections( actype.getSubtypes(), id, factory );
			}
		}
	}

}
