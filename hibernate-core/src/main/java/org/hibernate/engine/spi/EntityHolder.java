/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.spi;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holder for an entry in the {@link PersistenceContext} for an {@link EntityKey}.
 *
 * @since 6.4
 */
@Incubating
public interface EntityHolder {
	EntityKey getEntityKey();
	EntityPersister getDescriptor();

	/**
	 * The entity object, or {@code null} if no entity object was registered yet.
	 */
	@Nullable Object getEntity();
	/**
	 * The proxy object, or {@code null} if no proxy object was registered yet.
	 */
	@Nullable Object getProxy();
	/**
	 * The entity initializer that claims to initialize the entity for this holder.
	 * Will be {@code null} if entity is initialized already or the entity holder is not claimed yet.
	 */
	@Nullable EntityInitializer<?> getEntityInitializer();

	/**
	 * The proxy if there is one and otherwise the entity.
	 */
	default @Nullable Object getManagedObject() {
		final Object proxy = getProxy();
		return proxy == null ? getEntity() : proxy;
	}

	@Nullable EntityEntry getEntityEntry();

	@Internal
	void setEntityEntry(@Nullable EntityEntry entry);

	/**
	 * Marks the entity holder as reloaded to potentially trigger follow-on locking.
	 *
	 * @param processingState The processing state within which this entity is reloaded.
	 */
	void markAsReloaded(JdbcValuesSourceProcessingState processingState);

	/**
	 * Whether the entity is already initialized
	 */
	boolean isInitialized();

	/**
	 * Whether the entity is already initialized or will be initialized through an initializer eventually.
	 */
	boolean isEventuallyInitialized();

	/**
	 * Whether the entity is detached.
	 */
	boolean isDetached();
}
