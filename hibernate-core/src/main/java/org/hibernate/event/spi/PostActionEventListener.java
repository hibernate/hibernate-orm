/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.SPI;
import org.hibernate.persister.entity.EntityPersister;


import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Shared contract for listeners notified after an entity insert, update,
/// delete, or upsert action.
///
/// Implement this contract through one of the specialized post-action listener
/// interfaces. Override [#requiresPostCommitHandling(EntityPersister)] only
/// when the listener needs Hibernate to register an after-transaction callback
/// for the affected entity.
///
/// @see PostInsertEventListener
/// @see PostUpdateEventListener
/// @see PostDeleteEventListener
/// @see PostUpsertEventListener
///
/// @author Andrea Boriero
@SPI({ USE, IMPLEMENT })
public interface PostActionEventListener {

	/**
	 * Does this listener require that after transaction hooks be registered?
	 *
	 * @param persister The persister for the entity in question.
	 *
	 * @return {@code true} if after transaction callbacks should be added.
	 */
	default boolean requiresPostCommitHandling(EntityPersister persister) {
		return false;
	}
}
