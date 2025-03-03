/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;


/**
 * The keystone in EntityManagerFactoryScopeExtension support.
 *
 * This is how the extensions know how to build an EntityManagerFactory (scope)
 * and how to inject that EntityManagerFactory (scope) back into the test.
 *
 * @author Chris Cranford
 */
public interface EntityManagerFactoryScopeContainer {
	/**
	 * Callback to inject the EntityManagerFactoryScope into the container.
	 */
	void injectEntityManagerFactoryScope(EntityManagerFactoryScope scope);

	/**
	 * Obtain the {@link EntityManagerFactoryProducer}.  Quite often this is also
	 * implemented by the container itself.
	 */
	EntityManagerFactoryProducer getEntityManagerFactoryProducer();
}
