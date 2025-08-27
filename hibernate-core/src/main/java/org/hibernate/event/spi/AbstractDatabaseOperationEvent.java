/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Abstract supertype of {@link AbstractPostDatabaseOperationEvent}
 * and {@link AbstractPostDatabaseOperationEvent}.
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
			EventSource source,
			Object entity,
			Object id,
			EntityPersister persister) {
		super( source );
		this.entity = entity;
		this.id = id;
		this.persister = persister;
	}

	/**
	 * Retrieves the entity involved in the database operation.
	 *
	 * @return The entity.
	 */
	public Object getEntity() {
		return entity;
	}

	/**
	 * The id to be used in the database operation.
	 *
	 * @return The id.
	 */
	public Object getId() {
		return id;
	}

	/**
	 * The persister for the entity.
	 *
	 * @return The entity persister.
	 */
	public EntityPersister getPersister() {
		return persister;
	}

	/**
	 * The factory which owns the persister for the entity.
	 *
	 * @return The factory
	 */
	@Override
	public SessionFactoryImplementor getFactory() {
		return persister.getFactory();
	}
}
