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
 * Represents an operation successfully executed by the database.
 *
 * @author Gavin King
 *
 * @since 7
 */
public abstract class AbstractPostDatabaseOperationEvent extends AbstractDatabaseOperationEvent {

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param source The session from which the event originated.
	 * @param entity The entity to be involved in the database operation.
	 * @param id The entity id to be involved in the database operation.
	 * @param persister The entity's persister.
	 */
	public AbstractPostDatabaseOperationEvent(
			@Nonnull SharedSessionContractImplementor source,
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull EntityPersister persister) {
		super( source, entity, id, persister );
	}

}
