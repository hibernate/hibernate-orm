/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Contract for instantiating entity values
 */
public interface EntityInstantiator extends Instantiator {
	/**
	 * Create an instance of managed entity
	 */
	Object instantiate(SessionFactoryImplementor sessionFactory);

	/**
	 * Can this entity be instantiated?
	 */
	default boolean canBeInstantiated() {
		return true;
	}
}
