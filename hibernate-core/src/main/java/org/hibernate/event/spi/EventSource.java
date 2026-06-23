/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TransactionCompletionCallbacks;
import org.hibernate.engine.spi.TransactionCompletionCallbacksImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public interface EventSource extends SessionImplementor {

	/**
	 * Get the ActionQueue for this session
	 */
	@Nonnull
	org.hibernate.action.queue.spi.ActionQueue getActionQueue();

	/**
	 * Instantiate an entity instance, using either an interceptor,
	 * or the given persister
	 */
	@Nonnull
	Object instantiate(@Nonnull EntityPersister persister, @Nullable Object id) throws HibernateException;

	/**
	 * Obtain the best estimate of the entity name of the given entity
	 * instance, which is not involved in an association, by also
	 * considering information held in the proxy, and whether the object
	 * is already associated with this session.
	 */
	@Nonnull
	String bestGuessEntityName(@Nonnull Object object, @Nullable EntityEntry entry);

	/**
	 * Force an immediate flush
	 */
	void forceFlush(@Nonnull EntityEntry e) throws HibernateException;
	/**
	 * Force an immediate flush
	 */
	void forceFlush(@Nonnull EntityKey e) throws HibernateException;

	/**
	 * Cascade merge an entity instance
	 */
	void merge(@Nonnull String entityName, @Nonnull Object object, @Nonnull MergeContext copiedAlready) throws HibernateException;

	/**
	 * Cascade persist an entity instance
	 */
	void persist(@Nonnull String entityName, @Nonnull Object object, @Nonnull PersistContext createdAlready) throws HibernateException;

	/**
	 * Cascade persist an entity instance during the flush process
	 */
	void persistOnFlush(@Nonnull String entityName, @Nonnull Object object, @Nonnull PersistContext copiedAlready);

	/**
	 * Cascade refresh an entity instance
	 */
	void refresh(@Nonnull String entityName, @Nonnull Object object, @Nonnull RefreshContext refreshedAlready) throws HibernateException;

	/**
	 * Cascade delete an entity instance
	 */
	void delete(@Nonnull String entityName, @Nonnull Object child, boolean isCascadeDeleteEnabled, @Nonnull DeleteContext transientEntities);

	/**
	 * A specialized type of deletion for orphan removal that must occur prior to queued inserts and updates.
	 */
	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.
	//       This should be removed once action/task ordering is improved.
	void removeOrphanBeforeUpdates(@Nonnull String entityName, @Nonnull Object child);

	@Override
	@Nonnull
	default TransactionCompletionCallbacks getTransactionCompletionCallbacks() {
		return getActionQueue();
	}

	@Override
	@Nonnull
	default TransactionCompletionCallbacksImplementor getTransactionCompletionCallbacksImplementor() {
		return getActionQueue().getTransactionCompletionCallbacks();
	}
}
