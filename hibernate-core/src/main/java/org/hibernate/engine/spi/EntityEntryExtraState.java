/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Navigation methods for extra state objects attached to {@link EntityEntry}.
 *
 * @author Emmanuel Bernard
 */
public interface EntityEntryExtraState {

	/**
	 * Attach additional state to the core state of {@link EntityEntry}
	 * <p>
	 * Implementations must delegate to the next state or add it as next state if last in line.
	 */
	void addExtraState(EntityEntryExtraState extraState);

	/**
	 * Retrieve additional state by class type or null if no extra state of that type is present.
	 * <p>
	 * Implementations must return self if they match or delegate discovery to the next state in line.
	 */
	<T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType);

	//a remove method is ugly to define and has not real use case that we found: left out
}
