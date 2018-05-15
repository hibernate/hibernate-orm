/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5;

import javax.persistence.EntityManagerFactory;

/**
 * Contract for something that can build an EntityManagerFactory.
 *
 * Used by EntityManagerFactoryScopeExtension to create the EntityManagerFactoryScope.
 *
 * Generally speaking, a test class would implement EntityManagerFactoryScopeContainer
 * and return the EntityManagerFactoryProducer to be used for those tests.  The
 * EntityManagerFactoryProducer is then used to build the EntityManagerFactoryScope
 * which is injected back into the EntityManagerFactoryScopeContainer.
 *
 * @see EntityManagerFactoryScopeExtension
 * @see EntityManagerFactoryScope
 * @see EntityManagerFactoryScopeContainer
 *
 * @author Chris Cranford
 */
public interface EntityManagerFactoryProducer {
	EntityManagerFactory produceEntityManagerFactory();
}
