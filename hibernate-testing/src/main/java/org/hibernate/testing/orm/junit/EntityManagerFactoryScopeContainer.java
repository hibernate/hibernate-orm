/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
