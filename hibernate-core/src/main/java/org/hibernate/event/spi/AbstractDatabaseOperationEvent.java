/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.annotation.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;

/**
 * Base for events which denote database operations.
 *
 * @see AbstractPreDatabaseOperationEvent
 * @see AbstractPostDatabaseOperationEvent
 *
 * @author Gavin King
 *
 * @since 7
 */
public abstract class AbstractDatabaseOperationEvent extends AbstractEvent {
	private final Object entity;
	private final Object id;
	private final EntityPersister persister;

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param source The session from which the event originated.
	 * @param entity The entity to be involved in the database operation.
	 * @param id The entity id to be involved in the database operation.
	 * @param persister The entity's persister.
	 */
	public AbstractDatabaseOperationEvent(
			@Nonnull SharedSessionContractImplementor source,
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull EntityPersister persister) {
		super( source );
		this.entity = entity;
		this.id = id;
		this.persister = persister;
	}

	/**
	 * Retrieves the entity involved in the database operation.
	 */
	public @Nonnull Object getEntity() {
		return entity;
	}

	/**
	 * The id to be used in the database operation.
	 */
	public @Nullable Object getId() {
		return id;
	}

	/**
	 * The persister for the entity.
	 */
	public @Nonnull EntityPersister getPersister() {
		return persister;
	}
}
