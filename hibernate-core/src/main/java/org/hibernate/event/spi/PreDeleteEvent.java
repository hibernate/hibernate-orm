/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;


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
			Object entity,
			Object id,
			Object[] deletedState,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.deletedState = deletedState;
	}

	/**
	 * Getter for property 'deletedState'.  This is the entity state at the
	 * time of deletion (useful for optimistic locking and such).
	 *
	 * @return Value for property 'deletedState'.
	 */
	public Object[] getDeletedState() {
		return deletedState;
	}

}
