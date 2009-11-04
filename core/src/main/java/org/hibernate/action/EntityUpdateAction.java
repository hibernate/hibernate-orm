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
package org.hibernate.action;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.TypeFactory;

public final class EntityUpdateAction extends EntityAction {
	private final Object[] state;
	private final Object[] previousState;
	private final Object previousVersion;
	private final int[] dirtyFields;
	private final boolean hasDirtyCollection;
	private final Object rowId;
	private Object nextVersion;
	private Object cacheEntry;
	private SoftLock lock;

	public EntityUpdateAction(
	        final Serializable id,
	        final Object[] state,
	        final int[] dirtyProperties,
	        final boolean hasDirtyCollection,
	        final Object[] previousState,
	        final Object previousVersion,
	        final Object nextVersion,
	        final Object instance,
	        final Object rowId,
	        final EntityPersister persister,
	        final SessionImplementor session) throws HibernateException {
		super( session, id, instance, persister );
		this.state = state;
		this.previousState = previousState;
		this.previousVersion = previousVersion;
		this.nextVersion = nextVersion;
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.rowId = rowId;
	}

	public void execute() throws HibernateException {
		Serializable id = getId();
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();

		boolean veto = preUpdate();

		final SessionFactoryImplementor factory = getSession().getFactory();
		Object previousVersion = this.previousVersion;
		if ( persister.isVersionPropertyGenerated() ) {
			// we need to grab the version value from the entity, otherwise
			// we have issues with generated-version entities that may have
			// multiple actions queued during the same flush
			previousVersion = persister.getVersion( instance, session.getEntityMode() );
		}
		
		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = new CacheKey( 
					id, 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					session.getEntityMode(), 
					session.getFactory() 
			);
			lock = persister.getCacheAccessStrategy().lockItem( ck, previousVersion );
		}
		else {
			ck = null;
		}

		if ( !veto ) {
			persister.update( 
					id, 
					state, 
					dirtyFields, 
					hasDirtyCollection, 
					previousState, 
					previousVersion, 
					instance, 
					rowId, 
					session 
			);
		}

		EntityEntry entry = getSession().getPersistenceContext().getEntry( instance );
		if ( entry == null ) {
			throw new AssertionFailure( "possible nonthreadsafe access to session" );
		}
		
		if ( entry.getStatus()==Status.MANAGED || persister.isVersionPropertyGenerated() ) {
			// get the updated snapshot of the entity state by cloning current state;
			// it is safe to copy in place, since by this time no-one else (should have)
			// has a reference  to the array
			TypeFactory.deepCopy(
					state,
					persister.getPropertyTypes(),
					persister.getPropertyCheckability(),
					state,
					session
			);
			if ( persister.hasUpdateGeneratedProperties() ) {
				// this entity defines proeprty generation, so process those generated
				// values...
				persister.processUpdateGeneratedProperties( id, instance, state, session );
				if ( persister.isVersionPropertyGenerated() ) {
					nextVersion = Versioning.getVersion( state, persister );
				}
			}
			// have the entity entry doAfterTransactionCompletion post-update processing, passing it the
			// update state and the new version (if one).
			entry.postUpdate( instance, state, nextVersion );
		}

		if ( persister.hasCache() ) {
			if ( persister.isCacheInvalidationRequired() || entry.getStatus()!=Status.MANAGED ) {
				persister.getCacheAccessStrategy().remove( ck );
			}
			else {
				//TODO: inefficient if that cache is just going to ignore the updated state!
				CacheEntry ce = new CacheEntry(
						state, 
						persister, 
						persister.hasUninitializedLazyProperties( instance, session.getEntityMode() ), 
						nextVersion,
						getSession(),
						instance
				);
				cacheEntry = persister.getCacheEntryStructure().structure( ce );
				boolean put = persister.getCacheAccessStrategy().update( ck, cacheEntry, nextVersion, previousVersion );
				if ( put && factory.getStatistics().isStatisticsEnabled() ) {
					factory.getStatisticsImplementor().secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
				}
			}
		}

		postUpdate();

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatisticsImplementor()
					.updateEntity( getPersister().getEntityName() );
		}
	}

	private void postUpdate() {
		PostUpdateEventListener[] postListeners = getSession().getListeners()
				.getPostUpdateEventListeners();
		if (postListeners.length>0) {
			PostUpdateEvent postEvent = new PostUpdateEvent( 
					getInstance(), 
					getId(), 
					state, 
					previousState, 
					getPersister(),
					(EventSource) getSession() 
				);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostUpdate(postEvent);
			}
		}
	}

	private void postCommitUpdate() {
		PostUpdateEventListener[] postListeners = getSession().getListeners()
				.getPostCommitUpdateEventListeners();
		if (postListeners.length>0) {
			PostUpdateEvent postEvent = new PostUpdateEvent( 
					getInstance(), 
					getId(), 
					state, 
					previousState, 
					getPersister(),
					(EventSource) getSession()
				);
			for ( int i = 0; i < postListeners.length; i++ ) {
				postListeners[i].onPostUpdate(postEvent);
			}
		}
	}

	private boolean preUpdate() {
		PreUpdateEventListener[] preListeners = getSession().getListeners()
				.getPreUpdateEventListeners();
		boolean veto = false;
		if (preListeners.length>0) {
			PreUpdateEvent preEvent = new PreUpdateEvent( 
					getInstance(), 
					getId(), 
					state, 
					previousState, 
					getPersister(),
					(EventSource)getSession()
				);
			for ( int i = 0; i < preListeners.length; i++ ) {
				veto = preListeners[i].onPreUpdate(preEvent) || veto;
			}
		}
		return veto;
	}

	protected boolean hasPostCommitEventListeners() {
		return getSession().getListeners().getPostCommitUpdateEventListeners().length>0;
	}

	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws CacheException {
		EntityPersister persister = getPersister();
		if ( persister.hasCache() ) {
			
			final CacheKey ck = new CacheKey( 
					getId(), 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					getSession().getEntityMode(), 
					getSession().getFactory() 
				);
			
			if ( success && cacheEntry!=null /*!persister.isCacheInvalidationRequired()*/ ) {
				boolean put = persister.getCacheAccessStrategy().afterUpdate( ck, cacheEntry, nextVersion, previousVersion, lock );
				
				if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
					getSession().getFactory().getStatisticsImplementor().secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
				}
			}
			else {
				persister.getCacheAccessStrategy().unlockItem( ck, lock );
			}
		}
		postCommitUpdate();
	}

}







