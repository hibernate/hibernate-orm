/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;


/**
 * Contract for instantiating entity values
 */
public interface EntityInstantiator extends Instantiator {
	/**
	 * Create an instance of managed entity
	 */
	Object instantiate();

	/**
	 * Can this entity be instantiated?
	 */
	default boolean canBeInstantiated() {
		return true;
	}
}
