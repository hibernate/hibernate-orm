/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Represents a reference to an entity and its associated {@link EntityEntry}.
 */
public interface EntityEntryRef {

	/**
	 * The entity
	 *
	 * @return The entity
	 */
	Object getEntity();

	/**
	 * The associated EntityEntry
	 *
	 * @return The EntityEntry associated with the entity in this context
	 */
	EntityEntry getEntityEntry();

}
