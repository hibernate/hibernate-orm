/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after the datastore is updated
 *
 * @author Gavin King
 */
public class PostUpdateEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] state;
	private final Object[] oldState;
	// list of dirty properties as computed during a FlushEntityEvent
	private final int[] dirtyProperties;

	public PostUpdateEvent(
			Object entity,
			Object id,
			Object[] state,
			Object[] oldState,
			int[] dirtyProperties,
			EntityPersister persister,
			EventSource source) {
		super( source, entity, id, persister );
		this.state = state;
		this.oldState = oldState;
		this.dirtyProperties = dirtyProperties;
	}

	public Object[] getOldState() {
		return oldState;
	}

	public Object[] getState() {
		return state;
	}

	public int[] getDirtyProperties() {
		return dirtyProperties;
	}
}
