/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ComparableExecutable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.service.spi.EventListenerGroups;

import org.hibernate.persister.collection.CollectionPersister;

import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Any action relating to insert/update/delete of a collection
 *
 * @author Gavin King
 */
public abstract class CollectionAction implements ComparableExecutable {

	private transient CollectionPersister persister;
	private transient EventSource session;
	private final PersistentCollection<?> collection;

	private final Object key;
	private final String collectionRole;

	protected CollectionAction(
			final CollectionPersister persister,
			final PersistentCollection<?> collection,
			final Object key,
			final EventSource session) {
		this.persister = persister;
		this.session = session;
		this.key = key;
		this.collectionRole = persister.getRole();
		this.collection = collection;
	}

	protected PersistentCollection<?> getCollection() {
		return collection;
	}

	/**
	 * Reconnect to session after deserialization...
	 *
	 * @param session The session being deserialized
	 */
	public void afterDeserialize(EventSource session) {
		if ( this.session != null || this.persister != null ) {
			throw new IllegalStateException( "already attached to a session." );
		}
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			this.session = session;
			this.persister = session.getFactory().getMappingMetamodel().getCollectionDescriptor( collectionRole );
		}
	}

	@Override
	public final void beforeExecutions() throws CacheException {
		// We need to obtain the lock before any actions are executed, since this may be an inverse="true"
		// bidirectional association, and it is one of the earlier entity actions which actually updates
		// the database. This action is responsible for second-level cache invalidation only.
		if ( persister.hasCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			final SoftLock lock = cache.lockItem( session, cacheKey, null );
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
	public String[] getPropertySpaces() {
		return persister.getCollectionSpaces();
	}

	protected final CollectionPersister getPersister() {
		return persister;
	}

	protected final Object getKey() {
		return key instanceof DelayedPostInsertIdentifier
				? session.getPersistenceContextInternal().getEntry( collection.getOwner() ).getId()
				: key;
	}

	@Override
	public String getPrimarySortClassifier() {
		return collectionRole;
	}

	@Override
	public Object getSecondarySortIndex() {
		return key;
	}

	protected final EventSource getSession() {
		return session;
	}

	protected final void evict() throws CacheException {
		if ( persister.hasCache() ) {
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.remove( session, cacheKey);
		}
	}

	@Override
	public String toString() {
		return unqualify( getClass().getName() )
				+ infoString( collectionRole, key );
	}

	@Override
	public int compareTo(ComparableExecutable executable) {
		// sort first by role name
		final int roleComparison = collectionRole.compareTo( executable.getPrimarySortClassifier() );
		return roleComparison != 0
				? roleComparison
				//then by fk
				: persister.getAttributeMapping().getKeyDescriptor()
						.compare( key, executable.getSecondarySortIndex() );
	}

	private static class CacheCleanupProcess implements AfterTransactionCompletionProcess {
		private final Object key;
		private final CollectionPersister persister;
		private final SoftLock lock;

		private CacheCleanupProcess(Object key, CollectionPersister persister, SoftLock lock) {
			this.key = key;
			this.persister = persister;
			this.lock = lock;
		}

		@Override
		public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
			final var cache = persister.getCacheAccessStrategy();
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.unlockItem( session, cacheKey, lock );
		}
	}

	protected EventSource eventSource() {
		return getSession();
	}

	/**
	 * Convenience method for all subclasses.
	 * @return the {@link EventListenerGroups} instance from the {@code SessionFactory}.
	 */
	protected EventListenerGroups getEventListenerGroups() {
		return session.getFactory().getEventListenerGroups();
	}

}
