/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after the datastore is updated via a SQL {@code merge}
 *
 * @author Gavin King
 */
public class PostUpsertEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] state;
	//list of dirty properties as computed by Hibernate during a FlushEntityEvent
	private final int[] dirtyProperties;

	public PostUpsertEvent(
			Object entity,
			Object id,
			Object[] state,
			int[] dirtyProperties,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.state = state;
		this.dirtyProperties = dirtyProperties;
	}

	public Object[] getState() {
		return state;
	}

	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
}
