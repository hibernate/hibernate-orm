/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

import static org.hibernate.engine.internal.CacheHelper.usingCache;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Any action relating to insert/update/delete of a collection
 *
 * @author Gavin King
 */
public abstract class CollectionAction implements ComparableExecutable {

	@Nonnull
	private transient CollectionPersister persister;
	@Nonnull
	private transient EventSource session;
	@Nullable
	private final PersistentCollection<?> collection;

	@Nullable
	private final Object key;
	@Nonnull
	private final String collectionRole;

	protected CollectionAction(
			final @Nonnull CollectionPersister persister,
			final @Nullable PersistentCollection<?> collection,
			final @Nullable Object key,
			final @Nonnull EventSource session) {
		assert persister != null;
		assert session != null;
		this.persister = persister;
		this.session = session;
		this.key = key;
		this.collectionRole = persister.getRole();
		this.collection = collection;
	}

	/**
	 * collection accessor
	 *
	 * @return The collection
	 */
	@Nullable
	public PersistentCollection<?> getCollection() {
		return collection;
	}

	/**
	 * collection key accessor
	 *
	 * @return The collection key
	 */
	@Nullable
	public Object getKey() {
		if ( key instanceof DelayedPostInsertIdentifier ) {
			assert collection != null;
			return getSession().getPersistenceContextInternal().getEntry( collection.getOwner() ).getId();
		}
		return key;
	}

	/**
	 * Reconnect to session after deserialization...
	 *
	 * @param session The session being deserialized
	 */
	public void afterDeserialize(@Nullable EventSource session) {
//		if ( this.session != null || this.persister != null ) {
//			throw new IllegalStateException( "already attached to a session." );
//		}
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
		final var persister = getPersister();
		usingCache( persister, cache -> {
			final var session = getSession();
			assert key != null;
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			final var lock = cache.lockItem( session, cacheKey, null );
			afterTransactionProcess = new CacheCleanupProcess( key, persister, lock );
		} );
	}

	@Override
	@Nullable
	public BeforeTransactionCompletionProcess getBeforeTransactionCompletionProcess() {
		return null;
	}

	@Nullable
	private AfterTransactionCompletionProcess afterTransactionProcess;

	@Override
	@Nullable
	public AfterTransactionCompletionProcess getAfterTransactionCompletionProcess() {
		return afterTransactionProcess;
	}

	@Override
	@Nonnull
	public String[] getPropertySpaces() {
		return getPersister().getCollectionSpaces();
	}

	@Nonnull
	public final CollectionPersister getPersister() {
		return persister;
	}

	@Override
	@Nonnull
	public String getPrimarySortClassifier() {
		return collectionRole;
	}

	@Override
	@Nullable
	public Object getSecondarySortIndex() {
		return key;
	}

	@Nonnull
	protected final EventSource getSession() {
		return session;
	}

	public final void evict() throws CacheException {
		final var persister = getPersister();
		usingCache( persister, cache -> {
			final var session = getSession();
			assert key != null;
			final Object cacheKey = cache.generateCacheKey(
					key,
					persister,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			cache.remove( session, cacheKey );
		} );
	}

	@Override
	public @Nonnull String toString() {
		return unqualify( getClass().getName() )
				+ infoString( collectionRole, key );
	}

	@Override
	public int compareTo(@Nonnull ComparableExecutable executable) {
		// sort first by role name
		final int roleComparison = collectionRole.compareTo( executable.getPrimarySortClassifier() );
		return roleComparison != 0
				? roleComparison
				//then by fk
				: getPersister().getAttributeMapping().getKeyDescriptor()
						.compare( key, executable.getSecondarySortIndex() );
	}

	private static class CacheCleanupProcess implements AfterTransactionCompletionProcess {
		@Nonnull private final Object key;
		@Nonnull private final CollectionPersister persister;
		@Nullable private final SoftLock lock;

		private CacheCleanupProcess(
				@Nonnull Object key,
				@Nonnull CollectionPersister persister,
				@Nullable SoftLock lock) {
			this.key = key;
			this.persister = persister;
			this.lock = lock;
		}

		@Override
		public void doAfterTransactionCompletion(boolean success, @Nonnull SharedSessionContractImplementor session) {
			usingCache( persister, cache -> {
				final Object cacheKey = cache.generateCacheKey(
						key,
						persister,
						session.getFactory(),
						session.getTenantIdentifier()
				);
				cache.unlockItem( session, cacheKey, lock );
			} );
		}
	}

	@Nonnull
	protected EventSource eventSource() {
		return getSession();
	}

	/**
	 * Convenience method for all subclasses.
	 * @return the {@link EventListenerGroups} instance from the {@code SessionFactory}.
	 */
	@Nonnull
	protected EventListenerGroups getEventListenerGroups() {
		return session.getFactory().getEventListenerGroups();
	}

}
