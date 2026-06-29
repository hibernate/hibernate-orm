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
 * Represents a {@code pre-delete} event, which occurs just prior to
 * performing the deletion of an entity from the database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PreDeleteEvent extends AbstractPreDatabaseOperationEvent {

	private final Object[] deletedState;

	/**
	 * Constructs an event containing the pertinent information.
	 * @param entity The entity to be deleted.
	 * @param id The id to use in the deletion.
	 * @param deletedState The entity's state at deletion time.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreDeleteEvent(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object[] deletedState,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.deletedState = deletedState;
	}

	/**
	 * Getter for property 'deletedState'.  This is the entity state at the
	 * time of deletion (useful for optimistic locking and such).
	 *
	 * @return Value for property 'deletedState'.
	 */
	@Nullable
	public Object[] getDeletedState() {
		return deletedState;
	}

}
