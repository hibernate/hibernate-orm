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
 * Represents a {@code pre-insert} event, which occurs just prior to
 * performing the insert of an entity into the database.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PreInsertEvent extends AbstractPreDatabaseOperationEvent {
	private final Object[] state;

	/**
	 * Constructs an event containing the pertinent information.
	 * @param entity The entity to be inserted.
	 * @param id The id to use in the insertion.
	 * @param state The state to be inserted.
	 * @param persister The entity's persister.
	 * @param source The session from which the event originated.
	 */
	public PreInsertEvent(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.state = state;
	}

	/**
	 * Getter for property 'state'.  These are the values to be inserted.
	 *
	 * @return Value for property 'state'.
	 */
	@Nonnull
	public Object[] getState() {
		return state;
	}
}
