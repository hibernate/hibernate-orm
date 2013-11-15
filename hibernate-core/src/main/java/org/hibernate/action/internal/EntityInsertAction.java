/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.CacheKey;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityInsertAction extends AbstractEntityInsertAction {

	private Object version;
	private Object cacheEntry;

	public EntityInsertAction(
			Serializable id,
			Object[] state,
			Object instance,
			Object version,
			EntityPersister persister,
			boolean isVersionIncrementDisabled,
			SessionImplementor session) throws HibernateException {
		super( id, state, instance, isVersionIncrementDisabled, persister, session );
		this.version = version;
	}

	@Override
	public boolean isEarlyInsert() {
		return false;
	}

	@Override
	protected EntityKey getEntityKey() {
		return getSession().generateEntityKey( getId(), getPersister() );
	}

	@Override
	public void execute() throws HibernateException {
		nullifyTransientReferencesIfNotAlready();

		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();
		Serializable id = getId();

		boolean veto = preInsert();

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) {
			
			persister.insert( id, getState(), instance, session );
		
			EntityEntry entry = session.getPersistenceContext().getEntry( instance );
			if ( entry == null ) {
				throw new AssertionFailure( "possible non-threadsafe access to session" );
			}
			
			entry.postInsert( getState() );
	
			if ( persister.hasInsertGeneratedProperties() ) {
				persister.processInsertGeneratedProperties( id, instance, getState(), session );
				if ( persister.isVersionPropertyGenerated() ) {
					version = Versioning.getVersion( getState(), persister );
				}
				entry.postUpdate(instance, getState(), version);
			}

			getSession().getPersistenceContext().registerInsertedKey( getPersister(), getId() );
		}

		final SessionFactoryImplementor factory = getSession().getFactory();

		if ( isCachePutEnabled( persister, session ) ) {
			CacheEntry ce = persister.buildCacheEntry(
					instance,
					getState(),
					version,
					session
			);
			cacheEntry = persister.getCacheEntryStructure().structure(ce);
			final CacheKey ck = session.generateCacheKey( id, persister.getIdentifierType(), persister.getRootEntityName() );

			final boolean put = cacheInsert( persister, ck );

			if ( put && factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor().secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
			}
		}

		handleNaturalIdPostSaveNotifications(id);

		postInsert();

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatisticsImplementor().insertEntity( getPersister().getEntityName() );
		}

		markExecuted();
	}

	private boolean cacheInsert(EntityPersister persister, CacheKey ck) {
		try {
			getSession().getEventListenerManager().cachePutStart();
			return persister.getCacheAccessStrategy().insert( ck, cacheEntry, version );
		}
		finally {
			getSession().getEventListenerManager().cachePutEnd();
		}
	}

	private void postInsert() {
		EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				getId(),
				getState(),
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			listener.onPostInsert( event );
		}
	}

	private void postCommitInsert() {
		EventListenerGroup<PostInsertEventListener> listenerGroup = listenerGroup( EventType.POST_COMMIT_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return;
		}
		final PostInsertEvent event = new PostInsertEvent(
				getInstance(),
				getId(),
				getState(),
				getPersister(),
				eventSource()
		);
		for ( PostInsertEventListener listener : listenerGroup.listeners() ) {
			listener.onPostInsert( event );
		}
	}

	private boolean preInsert() {
		boolean veto = false;

		EventListenerGroup<PreInsertEventListener> listenerGroup = listenerGroup( EventType.PRE_INSERT );
		if ( listenerGroup.isEmpty() ) {
			return veto;
		}
		final PreInsertEvent event = new PreInsertEvent( getInstance(), getId(), getState(), getPersister(), eventSource() );
		for ( PreInsertEventListener listener : listenerGroup.listeners() ) {
			veto |= listener.onPreInsert( event );
		}
		return veto;
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SessionImplementor session) throws HibernateException {
		EntityPersister persister = getPersister();
		if ( success && isCachePutEnabled( persister, getSession() ) ) {
			final CacheKey ck = getSession().generateCacheKey( getId(), persister.getIdentifierType(), persister.getRootEntityName() );
			final boolean put = cacheAfterInsert( persister, ck );

			if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
				getSession().getFactory().getStatisticsImplementor()
						.secondLevelCachePut( getPersister().getCacheAccessStrategy().getRegion().getName() );
			}
		}
		postCommitInsert();
	}

	private boolean cacheAfterInsert(EntityPersister persister, CacheKey ck) {
		try {
			getSession().getEventListenerManager().cachePutStart();
			return persister.getCacheAccessStrategy().afterInsert( ck, cacheEntry, version );
		}
		finally {
			getSession().getEventListenerManager().cachePutEnd();
		}
	}

	@Override
	protected boolean hasPostCommitEventListeners() {
		return ! listenerGroup( EventType.POST_COMMIT_INSERT ).isEmpty();
	}
	
	private boolean isCachePutEnabled(EntityPersister persister, SessionImplementor session) {
		return persister.hasCache()
				&& !persister.isCacheInvalidationRequired()
				&& session.getCacheMode().isPutEnabled();
	}

}
