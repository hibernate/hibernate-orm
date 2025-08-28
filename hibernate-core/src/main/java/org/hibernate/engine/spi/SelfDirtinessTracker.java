/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import org.hibernate.bytecode.enhance.spi.CollectionTracker;

/**
 * Contract for an entity to report that it tracks the dirtiness of its own state,
 * as opposed to needing Hibernate to perform state-diff dirty calculations.
 * <p>
 * Entity classes are free to implement this contract themselves.  This contract is
 * also introduced into the entity when using bytecode enhancement and requesting
 * that entities track their own dirtiness.
 *
 * @author Ståle W. Pedersen
 */
public interface SelfDirtinessTracker extends PrimeAmongSecondarySupertypes {
	/**
	 * Have any of the entity's persistent attributes changed?
	 *
	 * @return {@code true} indicates one or more persistent attributes have changed; {@code false}
	 * indicates none have changed.
	 */
	boolean $$_hibernate_hasDirtyAttributes();

	/**
	 * Retrieve the names of all the persistent attributes whose values have changed.
	 *
	 * @return An array of changed persistent attribute names
	 */
	String[] $$_hibernate_getDirtyAttributes();

	/**
	 * Adds persistent attribute to the set of values that have changed
	 */
	void $$_hibernate_trackChange(String attributes);

	/**
	 * Clear the stored dirty attributes
	 */
	void $$_hibernate_clearDirtyAttributes();

	/**
	 * Temporarily enable / disable dirty tracking
	 */
	void $$_hibernate_suspendDirtyTracking(boolean suspend);

	/**
	 * Get access to the CollectionTracker
	 */
	CollectionTracker $$_hibernate_getCollectionTracker();

	/**
	 * Special internal contract to optimize type checking
	 * @see PrimeAmongSecondarySupertypes
	 * @return this same instance
	 */
	@Override
	default SelfDirtinessTracker asSelfDirtinessTracker() {
		return this;
	}

}
