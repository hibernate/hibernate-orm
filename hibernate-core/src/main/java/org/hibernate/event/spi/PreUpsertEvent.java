/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;

/**
 * Represents a pre-upsert event, which occurs just prior to
 * performing the upsert of an entity in the database.
 *
 * @author Gavin King
 */
public class PreUpsertEvent extends AbstractPreDatabaseOperationEvent {
	private final Object[] state;

	/**
	 * Constructs an event containing the pertinent information.
	 * @param entity The entity to be updated.
	 * @param id The id of the entity to use for updating.
	 * @param state The state to be updated.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreUpsertEvent(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.state = state;
	}

	/**
	 * Retrieves the state to be used in the upsert.
	 *
	 * @return The current state.
	 */
	@Nonnull
	public Object[] getState() {
		return state;
	}
}
