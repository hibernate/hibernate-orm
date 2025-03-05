/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public interface EventSource extends SessionImplementor {

	/**
	 * Get the ActionQueue for this session
	 */
	ActionQueue getActionQueue();

	/**
	 * Instantiate an entity instance, using either an interceptor,
	 * or the given persister
	 */
	Object instantiate(EntityPersister persister, Object id) throws HibernateException;

	/**
	 * Force an immediate flush
	 */
	void forceFlush(EntityEntry e) throws HibernateException;
	/**
	 * Force an immediate flush
	 */
	void forceFlush(EntityKey e) throws HibernateException;

	/**
	 * Cascade merge an entity instance
	 */
	void merge(String entityName, Object object, MergeContext copiedAlready) throws HibernateException;

	/**
	 * Cascade persist an entity instance
	 */
	void persist(String entityName, Object object, PersistContext createdAlready) throws HibernateException;

	/**
	 * Cascade persist an entity instance during the flush process
	 */
	void persistOnFlush(String entityName, Object object, PersistContext copiedAlready);

	/**
	 * Cascade refresh an entity instance
	 */
	void refresh(String entityName, Object object, RefreshContext refreshedAlready) throws HibernateException;

	/**
	 * Cascade delete an entity instance
	 */
	void delete(String entityName, Object child, boolean isCascadeDeleteEnabled, DeleteContext transientEntities);

	/**
	 * A specialized type of deletion for orphan removal that must occur prior to queued inserts and updates.
	 */
	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.
	//       This should be removed once action/task ordering is improved.
	void removeOrphanBeforeUpdates(String entityName, Object child);

	/**
	 * Attempts to load the entity from the second-level cache.
	 *
	 * @param persister The persister for the entity being requested for load
	 * @param entityKey The entity key
	 * @param instanceToLoad The instance that is being initialized, or null
	 * @param lockMode The lock mode
	 *
	 * @return The entity from the second-level cache, or null.
	 *
	 * @since 7.0
	 */
	@Incubating
	Object loadFromSecondLevelCache(EntityPersister persister, EntityKey entityKey, Object instanceToLoad, LockMode lockMode);
}
