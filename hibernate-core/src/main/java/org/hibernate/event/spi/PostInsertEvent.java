/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import jakarta.annotation.Nonnull;

/**
 * Occurs after inserting an item in the datastore
 *
 * @author Gavin King
 */
public class PostInsertEvent extends AbstractPostDatabaseOperationEvent {
	private final Object[] state;

	public PostInsertEvent(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nonnull EntityPersister persister,
			@Nonnull SharedSessionContractImplementor source) {
		super( source, entity, id, persister );
		this.state = state;
	}

	@Nonnull
	public Object[] getState() {
		return state;
	}
}
