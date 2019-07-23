/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import java.io.Serializable;

import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.action.spi.Executable;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Any action relating to insert/update/delete of a collection
 *
 * @author Gavin King
 */
public abstract class CollectionAction implements Executable, Serializable, Comparable {
	private transient CollectionPersister persister;
	private transient SharedSessionContractImplementor session;
	private final PersistentCollection collection;

	private final Serializable key;
	private final String collectionRole;

	protected CollectionAction(
			final CollectionPersister persister,
			final PersistentCollection collection, 
			final Serializable key, 
			final SharedSessionContractImplementor session) {
		this.persister = persister;
		this.session = session;
		this.key = key;
		this.collectionRole = persister.getRole();
		this.collection = collection;
	}

	protected PersistentCollection getCollection() {
		return collection;
	}

	/**
	 * Reconnect to session after deserialization...
	 *
	 * @param session The session being deserialized
	 */
	public void afterDeserialize(SharedSessionContractImplementor session) {
		if ( this.session != null || this.persister != null ) {
			throw new IllegalStateException( "already attached to a session." );
		}
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			this.session = session;
			this.persister = session.getFactory().getMetamodel().collectionPersister( collectionRole );
		}
	}

	@Override
	public final void beforeExecutions() throws CacheException {
		// we need to obtain the lock before any actions are executed, since this may be an inverse="true"
		// bidirectional association and it is one of the earlier entity actions which actually updates
		// the database (this action is responsible for second-level cache invalidation only)
		if ( persister.hasCache() ) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			final SoftLock lock = cache.lockItem( session, ck, null );
			// the old behavior used key as opposed to getKey()
			afterTransactionProcess = new CacheCleanupProcess( key, persister, lock );
		}
	}

	@Override
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	private AfterTransactionCompletionProcess afterTransactionProcess;

	@Override
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return afterTransactionProcess;
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return persister.getCollectionSpaces();
	}

	protected final CollectionPersister getPersister() {
		return persister;
	}

	protected final Serializable getKey() {
		Serializable finalKey = key;
		if ( key instanceof DelayedPostInsertIdentifier ) {
			// need to look it up from the persistence-context
			finalKey = session.getPersistenceContextInternal().getEntry( collection.getOwner() ).getId();
			if ( finalKey == key ) {
				// we may be screwed here since the collection action is about to execute
				// and we do not know the final owner key value
			}
		}
		return finalKey;
	}

	protected final SharedSessionContractImplementor getSession() {
		return session;
	}

	protected final void evict() throws CacheException {
		if ( persister.hasCache() ) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					key, 
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.remove( session, ck);
		}
	}

	@Override
	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + MessageHelper.infoString( collectionRole, key );
	}

	@Override
	public int compareTo(Object other) {
		final CollectionAction action = (CollectionAction) other;

		// sort first by role name
		final int roleComparison = collectionRole.compareTo( action.collectionRole );
		if ( roleComparison != 0 ) {
			return roleComparison;
		}
		else {
			//then by fk
			return persister.getKeyType().compare( key, action.key );
		}
	}

	private static class CacheCleanupProcess implements AfterTransactionCompletionProcess {
		private final Serializable key;
		private final CollectionPersister persister;
		private final SoftLock lock;

		private CacheCleanupProcess(Serializable key, CollectionPersister persister, SoftLock lock) {
			this.key = key;
			this.persister = persister;
			this.lock = lock;
		}

		@Override
		public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
			final CollectionDataAccess cache = persister.getCacheAccessStrategy();
			final Object ck = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.unlockItem( session, ck, lock );
		}
	}

	protected <T> EventListenerGroup<T> listenerGroup(EventType<T> eventType) {
		return getSession()
				.getFactory()
				.getServiceRegistry()
				.getService( EventListenerRegistry.class )
				.getEventListenerGroup( eventType );
	}

	protected EventSource eventSource() {
		return (EventSource) getSession();
	}
}
