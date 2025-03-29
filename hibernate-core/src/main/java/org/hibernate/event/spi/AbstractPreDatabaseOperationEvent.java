/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Represents an operation that is about to be executed by the database.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPreDatabaseOperationEvent extends AbstractDatabaseOperationEvent {

	/**
	 * Constructs an event containing the pertinent information.
	 *
	 * @param source The session from which the event originated.
	 * @param entity The entity to be involved in the database operation.
	 * @param id The entity id to be involved in the database operation.
	 * @param persister The entity's persister.
	 */
	public AbstractPreDatabaseOperationEvent(
			EventSource source,
			Object entity,
			Object id,
			EntityPersister persister) {
		super( source, entity, id, persister );
	}
}
