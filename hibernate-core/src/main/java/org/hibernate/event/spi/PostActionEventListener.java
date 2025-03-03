/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Andrea Boriero
 */
interface PostActionEventListener {

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
