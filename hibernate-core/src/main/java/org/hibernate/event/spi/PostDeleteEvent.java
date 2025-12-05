/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after deleting an item from the datastore
 *
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] deletedState;

	public PostDeleteEvent(
			Object entity,
			Object id,
			Object[] deletedState,
			EntityPersister persister,
			SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.deletedState = deletedState;
	}

	public Object[] getDeletedState() {
		return deletedState;
	}
}
